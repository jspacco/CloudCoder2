package junit.org.cloudcoder.analysis.incremental.compiler;

import java.rmi.UnexpectedException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import org.cloudcoder.analysis.incremental.compiler.EditSequence;
import org.cloudcoder.analysis.incremental.compiler.LineEdit;
import org.cloudcoder.app.server.persist.Database;
import org.cloudcoder.app.server.persist.JDBCDatabaseConfig;
import org.cloudcoder.app.shared.model.ApplyChangeToTextDocument;
import org.cloudcoder.app.shared.model.Change;
import org.cloudcoder.app.shared.model.ChangeType;
import org.cloudcoder.app.shared.model.CompilationOutcome;
import org.cloudcoder.app.shared.model.CompilationResult;
import org.cloudcoder.app.shared.model.CompilerDiagnostic;
import org.cloudcoder.app.shared.model.TextDocument;
import org.cloudcoder.app.shared.model.User;
import org.cloudcoder.builder2.javacompiler.InMemoryJavaCompiler;
import org.junit.Test;

import static org.junit.Assert.*;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;


public class CompileAll
{
	private static class Tuple<E,E2> {
		private E e;
		private E2 e2;
		
		public Tuple(E e, E2 e2) {
			this.e = e;
			this.e2 = e2;
		}
		
		public E getFirst() {
			return e;
		}
		
		public E2 getSecond() {
			return e2;
		}
	}
    /*
     * 
     * Strategies:
     *      Compile every change at whatever granularity
     *      
     *      Compile every line (i.e. look-ahead to see 
     *          when we start editing a new line number)
     *      
     *      Reconstruct token sequence, compile token-by-token
     *      
     *      Some other choice of granularity?
     *      
     * 
     * Future work: use the Eclipse incremental compiler
     * 
     * Take the change sequence and reconstruct
     * tokens.  Probably have to find the spaces.
     * May want to compile after each edit to a line,
     * or work on consecutive edits within a line.
     * 
     * May need to recognize when an edit is part of the same token
     * and when it is a new token (i.e. state machine that can
     * look ahead by characters).
     * 
     * How to handle edits that are finer or courser than an edit?
     * i.e. what if an edit contains several tokens, or a single character
     * that doesn't make sense without the next character?  We can't key these
     * edits to the eventID.
     * 
     * Maybe use a BufferedReader to read from
     * a StringReader, and have a lookup of some kind
     * to find the location for the token to be added
     * or removed.
     * 
     * What format do we want? 
     */
    
    static {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        create();
    }
    
    public static void create() {
        // XXX this is a hack!
        // TODO figure out how to put this info into a config file
        JDBCDatabaseConfig.create(new JDBCDatabaseConfig.ConfigProperties() {
            @Override
            public String getUser() {
                return "root";
            }
            @Override
            public String getPasswd() {
                return "root";
            }
            @Override
            public String getDatabaseName() {
                return "cloudcoderdb";
            }
            @Override
            public String getHost() {
                return "localhost";
            }
            @Override
            public String getPortStr() {
                return ":8889";
            }
        });
    }
    
    static List<String> convert(String lines) {
        lines=lines.replaceAll("\r", "");
        List<String> result=new LinkedList<String>();
        for (String s : lines.split("\n")) {
            result.add(s);
        }
        return result;
    }
    
    @Test
    public void printLineByLine()
    throws Exception
    {
        Integer userID=3;
        Integer problemID=9;
        List<Change> changeList=lookupChanges(userID, problemID);
        EditSequence lineChanges=new EditSequence();
        lineChanges.parseChanges(changeList);
        
        TextDocument doc=new TextDocument();
        doc.setText("");
        
        ApplyChangeToTextDocument applicator=new ApplyChangeToTextDocument();
        
        for (LineEdit line : lineChanges) {
            // this variable is useless and exists to hold a breakpoint
            for (Change c : line) {
                applicator.apply(c, doc);
            }
            System.out.println(line);
            System.out.println(doc.getText());
        }
    }
    
    //@Test
    public void testReconstructLineLevelEdits()
    throws Exception
    {
        Integer userID=70;
        Integer problemID=16;
        List<Change> changeList=lookupChanges(userID, problemID);
        EditSequence lineChanges=new EditSequence();
        lineChanges.parseChanges(changeList);
        assertEquals(changeList.size(), lineChanges.getNumChanges());
        for (int i=0; i<changeList.size(); i++) {
            Change c1=changeList.get(i);
            Change c2=lineChanges.getAllChanges().get(i);
            assertEquals(c1, c2);
        }
        for (LineEdit ch : lineChanges) {
            System.out.println(ch);
        }
    }
    
    static List<Change> lookupChanges(Integer userID, Integer problemID) {
        // look up the problem
        // Problem problem=Database.getInstance().getProblem(problemID);
        // Language lang=problem.getProblemType().getLanguage();
        
        // Get all of the changes for this problem
        // We need a fake user object here to match the API
        // of the getAllChangesNewerThan() method
        User user=new User();
        user.setId(userID);
        return Database.getInstance().getAllChangesNewerThan(user, problemID, -1);
    }
    
    @Test
    public void testPrintChanges()
    throws Exception
    {
        
        Integer userID=33;
        Integer problemID=3;
        
        
        List<Change> deltaList = lookupChanges(userID, problemID);
        
        ApplyChangeToTextDocument app=new ApplyChangeToTextDocument();
        TextDocument doc=new TextDocument();
        doc.setText("");
        System.out.println(doc);
        
        int i=1;
        int numRemove=0;
        int different=0;
        System.out.println(doc.getText());
        for (Change c : deltaList) {
            //if (i>=30) break;
            String before=doc.getText();
            app.apply(c, doc);
            String after=doc.getText();
            System.out.println(i+"\n"+c+"\nvvvvvvvvvvv after change vvvvvvvvvv(eventid: "+c.getEventId()+")");
            System.out.println(doc.getText());
            System.out.println("-------------------");
            if (c.getType()==ChangeType.FULL_TEXT && !before.equals(after)) {
                Patch p=DiffUtils.diff(convert(before), convert(after));
                if (p.getDeltas().size()==0) {
                    continue;
                }
                System.out.println("before is not the same as after:");
                for (Delta d: p.getDeltas()) {
                    System.out.println(d);
                }
                
                different++;
                System.out.println("-------------------");
            }
            
            
            if (c.getType()==ChangeType.REMOVE_TEXT) {
                numRemove++;
            }
            i++;
        }
        System.out.println("Num remove text: "+numRemove);
        System.out.println("Num different: "+different);
        
    }
    
    @Test
    public void testUserProblem() throws Exception
    {
        Connection conn=getConnection();
        conn.setAutoCommit(false);
        Tuple<Integer,Integer> pair=new Tuple<Integer,Integer>(33, 3);
        
        cleanRecords(conn, pair);
        // work should resume from last_line in e_status table
        boolean result = doWork(conn, pair);
        markPair(conn, pair, result);
    }
    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://localhost:8889/cloudcoderdb?user=root&password=root");
    }
    
    @Test
    public void workLoop()
    throws Exception
    {
    	// get the student problem pair
    	Connection conn = null;
    	try {
    	    conn=getConnection();
    		// connections start in auto-commit mode, we turn this off so we can use transactions
    		conn.setAutoCommit(false); // this will turn off auto-commit, and start a new transaction
    		// each call to commit() ends the current transaction and starts a new one
    		
    		// clear and populate the status table
    		// we don't need the clear, just use it for testing
    		//clearStatusTable(conn);
    		populateStatusTable(conn);
    		
    		boolean done = false;
    		while (!done) {
		    	Tuple<Integer, Integer> pair = getStudentProblemPair(conn);
		    	if (pair.getFirst() != -1)
		    	{
		    	    if (pair.getFirst()==3 && pair.getSecond()==9 ||
		    	            pair.getFirst()==33 && pair.getSecond()==3)
		    	    {
		    	        // skip user 3 problem 9, which was giving us problems
		    	        continue;
		    	    }
			    	// want to remove cleanRecords and have the work resume from where it left off
			    	cleanRecords(conn, pair);
		    		// work should resume from last_line in e_status table
			    	boolean result = doWork(conn, pair);
			    	markPair(conn, pair, result);
		    	}
		    	else
		    	{
		    		done = true;
		    	}
    		}
    	} catch (Exception e) {
    		throw e;
    	} finally {
    		if (conn != null)
    			conn.close();
    	}
    }
    
    public void clearStatusTable(Connection conn)
    throws Exception
    {
    	System.out.println("Clearing status table...");
    	
    	Statement stmt = conn.createStatement();
    	String query = "DELETE FROM e_status WHERE 1";
    	
    	stmt.executeUpdate(query);
    	
    	conn.commit();
    	
    	System.out.println("Status table cleared.");
    }
    
    public void populateStatusTable(Connection conn)
    throws Exception
    {
    	System.out.println("Populating status table...");
    	
    	int statuses = 0;
    		
		// select all user, problem pairs
		Statement stmt = conn.createStatement();
		String query = "SELECT DISTINCT user_id, problem_id FROM cc_events";
		
		// insert a status record for them
		ResultSet result = stmt.executeQuery(query);
		while (result.next()) {
			int userID = result.getInt("user_id");
			int problemID = result.getInt("problem_id");
			
			Statement stmt2 = conn.createStatement();
			query = "SELECT last_line FROM e_status " +
					"WHERE user_id = " + userID + " " +
					"AND problem_id = " + problemID + ";";
			
			ResultSet result2 = stmt2.executeQuery(query);
			if (result2.next()) {
				// record already exists
			} else {
				query = "INSERT INTO e_status (user_id, problem_id, last_line, done, in_progress) " +
						"VALUES (" + userID + "," + problemID + ", -1, 0, 0);";
				
				stmt2.executeUpdate(query);
				
				statuses++;
			}
			result2.close();
			stmt2.close();
		}
		
		conn.commit();
		
		System.out.println(statuses + " records added.");
    }
    
    private static Tuple<Integer, Integer> getStudentProblemPair(Connection conn)
    throws Exception
    {
    	Integer student = -1;
    	Integer problem = -1;
    	
    	// select all rows that are not in progress and not done
    	Statement stmt = conn.createStatement();
    	String query = "SELECT * FROM e_status WHERE done = 0 AND in_progress = 0";
    	
    	ResultSet result = stmt.executeQuery(query);
    	
    	if (result.next()) {
    		student = result.getInt("user_id");
    		problem = result.getInt("problem_id");
    	} else {
    		// no rows left, we must be done or almost done
    	}
    	result.close();
    	stmt.close();
    	
    	conn.commit();
    	
    	return new Tuple<Integer, Integer>(student, problem);
    }
    
    private static void cleanRecords(Connection conn, Tuple<Integer, Integer> pair)
    throws Exception
    {
    	int userID = pair.getFirst();
    	int problemID = pair.getSecond();
    	
    	System.out.println("Cleaning records for user: " + userID + " and problem: " + problemID + "...");
    	
    	// clear previous records for this userID, problemID pair
		Statement stmt = conn.createStatement();
		String query = 	"SELECT line_id FROM e_lines " + 
						"WHERE user_id = '" + userID + "' " +
						"AND problem_id = '" + problemID + "';";
		ResultSet result = stmt.executeQuery(query);
		
		while (result.next()) {
			int line_id = result.getInt("line_id");
			
			Statement stmt2 = conn.createStatement();
			query = "DELETE FROM e_lines 			WHERE line_id = '" + line_id + "';";
			stmt2.executeUpdate(query);
			stmt2.close();
			
			stmt2 = conn.createStatement();
			query = "DELETE FROM e_linestochanges 	WHERE line_id = '" + line_id + "';";
			stmt2.executeUpdate(query);
			stmt2.close();
			
			stmt2 = conn.createStatement();
			query = "DELETE FROM e_linestoerrors 	WHERE line_id = '" + line_id + "';";
			stmt2.executeUpdate(query);
			stmt2.close();
		}
		stmt.close();
		result.close();
		
		conn.commit();
		
		System.out.println("Cleaning complete");
    }
    
    private static boolean doWork(Connection conn, Tuple<Integer, Integer> pair)
    throws Exception
    {
    	int userID = pair.getFirst();
    	int problemID = pair.getSecond();
    	
    	System.out.println("Committing lines for user: " + userID + " and problem: " + problemID + "...");
    	
        List<Change> changeList = lookupChanges(userID, problemID);
        EditSequence lineChanges = new EditSequence();
        lineChanges.parseChanges(changeList);
        
    	TextDocument doc = new TextDocument();
    	doc.setText("");
    	
    	// set the user, problem pair as in progress
    	Statement stmt = conn.createStatement();
    	String query = "UPDATE e_status SET in_progress = 1 WHERE user_id = " + userID + " AND problem_id = " + problemID + ";";
    	
    	stmt.executeUpdate(query);
    	stmt.close();
    	
    	conn.commit();
        
    	int lastLine = 0;
        // this loop goes through each line change, compiling and updating the database
        for (LineEdit change : lineChanges) {
    		String text = doc.getText();
    		text = sanitizeString(text);
    		
    		change.apply(doc);

    		CompilationResult compResult = compile(text);
    		CompilationOutcome outcome = compResult.getOutcome();
    		
    		String compiledString = "0";
    		if (outcome == CompilationOutcome.SUCCESS)
    			compiledString = "1";
    		
    		// create a new line entry in the database
    		stmt = conn.createStatement();
    		query = 	"INSERT INTO e_lines (user_id, problem_id, text, compiled)" +
    						"VALUES ('" + userID + "','" + problemID + "','" + text + "','" + compiledString + "');";
    		stmt.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
    		
    		// get line_id
    		int line_id = -1;
    		ResultSet result = stmt.getGeneratedKeys();
    		if (result != null && result.next()) {
    			line_id = result.getInt(1);
    		} else {
    			conn.rollback();
    			throw new UnexpectedException("Could not get the generated line_id. Changes not committed.");
    		}
    		stmt.close();
    		
    		// put a row in linestochanges
    		for (Change c : change) {
    			int changeEventID = c.getEventId();
	    		stmt = conn.createStatement();
	    		query = "INSERT INTO e_linestochanges (line_id, event_id)" +
	    				"VALUES ('" + line_id + "','" + changeEventID + "');";
	    		stmt.executeUpdate(query);
	    		stmt.close();
    		}
    	
    		switch (outcome) {
    		case SUCCESS:
    			// successfully compiled
    			
    			// don't need to submit any errors here
    			break;
    		case FAILURE:
    			// did not compile
    			
    			// put a row for each error into e_linestoerrors
    			for (CompilerDiagnostic diag : compResult.getCompilerDiagnosticList()) {
    				stmt = conn.createStatement();
    				String message = diag.getMessage();
    				message = sanitizeString(message);
    						
    				query = "INSERT INTO e_linestoerrors (line_id, message, startLine, startColumn, endLine, endColumn)" +
    						"VALUES ('" + line_id + "','" + message + "','" + diag.getStartLine() + "','" + diag.getStartColumn() + "','" + diag.getEndLine() + "','" + diag.getEndColumn() + "');";
    				stmt.executeUpdate(query);
    				stmt.close();
    			}
	    		break;
    		case UNEXPECTED_COMPILER_ERROR:
    			// compiler error
    		case BUILDER_ERROR:
    			// error/bug with cloudcoder builder
    			
    			// not sure what we want to do here, just gonna rollback and throw an error
    			conn.rollback();
    			throw new UnexpectedException("Unexpected compiler error or builder error. Changes not committed.");
    		}
    		
    		// update the last_line
    		lastLine++;
    		stmt = conn.createStatement();
    		query = "UPDATE e_status SET last_line = " + lastLine + " WHERE user_id = " + userID + " AND problem_id = " + problemID + ";";
    		
    		stmt.executeUpdate(query);
    		stmt.close();
    		
    		// commit this line
    		conn.commit();
        } // end of foreach line loop
        
        // set the user, problem pair as done
        stmt = conn.createStatement();
        query = "UPDATE e_status SET done = 1, in_progress = 0 WHERE user_id = " + userID + " AND problem_id = " + problemID + ";";
        
        stmt.executeUpdate(query);
        stmt.close();
        
        conn.commit();
        
        System.out.println("Committed " + lineChanges.getNumLines() + " lines.");
    	return true;
    }
    
    private static void markPair(Connection conn, Tuple<Integer, Integer> pair, boolean done)
    {
    	// mark the pair as done or not done
    }
    
    /**
    public static void compileLineByLine(Integer userID, Integer problemID, boolean clearOldRecords) 
    throws Exception
    {    	
    	Connection conn = null;
    	Statement stmt = null;
    	ResultSet result = null;
    	// try to connect to database
    	try {
    		Class.forName("com.mysql.jdbc.Driver");
    		conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/cloudcoderdb?user=root&password=");
    		// connections start in auto-commit mode, we turn this off so we can use transactions
    		conn.setAutoCommit(false); // this will turn off auto-commit, and start a new transaction
    		// each call to commit() ends the current transaction and starts a new one
    		
    		System.out.println("Cleaning records for User: " + userID + ", Problem: " + problemID);
    		if (clearOldRecords) {
    			// clear previous records for this userID, problemID pair
    			stmt = conn.createStatement();
    			String query = 	"SELECT line_id FROM e_lines " + 
    							"WHERE user_id = '" + userID + "' " +
    							"AND problem_id = '" + problemID + "';";
    			result = stmt.executeQuery(query);
    			
    			while (result.next()) {
	    			int line_id = result.getInt(1);
	    			
	    			Statement stmt2 = conn.createStatement();
	    			query = "DELETE FROM e_lines 			WHERE line_id = '" + line_id + "';";
	    			stmt2.executeUpdate(query);
	    			stmt2.close();
	    			
	    			stmt2 = conn.createStatement();
	    			query = "DELETE FROM e_linestochanges 	WHERE line_id = '" + line_id + "';";
	    			stmt2.executeUpdate(query);
	    			stmt2.close();
	    			
	    			stmt2 = conn.createStatement();
	    			query = "DELETE FROM e_linestoerrors 	WHERE line_id = '" + line_id + "';";
	    			stmt2.executeUpdate(query);
	    			stmt2.close();
    			}
    			stmt.close();
    		}
    		System.out.println("Cleaning complete.");
    		
    		List<Change> deltaList = lookupChanges(userID, problemID);
        	
        	ApplyChangeToTextDocument app = new ApplyChangeToTextDocument();
        	TextDocument doc = new TextDocument();
        	doc.setText("");
        	
        	//
        	// THIS IS FOR EVERY CHANGE, WE WANT FOR EVERY LINE
        	// WRITE RESUME CODE
        	//
        	for (Change c : deltaList)
        	{
        		int changeEventID = c.getEventId();
        		System.out.println("Creating entries for UserID: " + userID + ", ProblemID: " + problemID + ", EventID: " + changeEventID);
        		
        		String text = doc.getText();
        		
        		app.apply(c, doc);

        		CompilationResult compResult = compile(text);
        		CompilationOutcome outcome = compResult.getOutcome();
        		
        		text = sanitizeString(text);
        		
        		// create a new line entry in the database
        		stmt = conn.createStatement();
        		String query = 	"INSERT INTO e_lines (user_id, problem_id, text, compiled)" +
        						"VALUES ('" + userID + "','" + problemID + "','" + text + "','" + compiledString + "');";
        		stmt.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
        		
        		// get line_id
        		int line_id = -1;
        		result = stmt.getGeneratedKeys();
        		if (result != null && result.next()) {
        			line_id = result.getInt(1);
        		} else {
        			conn.rollback();
        			throw new UnexpectedException("Could not get the generated line_id. Changes not committed.");
        		}
        		stmt.close();
        		
        		// put a row in linestochanges
        		stmt = conn.createStatement();
        		query = "INSERT INTO e_linestochanges (line_id, event_id)" +
        				"VALUES ('" + line_id + "','" + changeEventID + "');";
        		stmt.executeUpdate(query);
        		stmt.close();
        	
        		switch (outcome.name()) {
        		case "SUCCESS":
        			// successfully compiled
        			
        			// don't need to submit any errors here
        			break;
        		case "FAILURE":
        			// did not compile
        			
        			// put a row for each error into e_linestoerrors
        			for (CompilerDiagnostic diag : compResult.getCompilerDiagnosticList()) {
        				stmt = conn.createStatement();
        				String message = diag.getMessage();
        				message = sanitizeString(message);
        						
        				query = "INSERT INTO e_linestoerrors (line_id, message, startLine, startColumn, endLine, endColumn)" +
        						"VALUES ('" + line_id + "','" + message + "','" + diag.getStartLine() + "','" + diag.getStartColumn() + "','" + diag.getEndLine() + "','" + diag.getEndColumn() + "');";
        				stmt.executeUpdate(query);
        				stmt.close();
        			}
    	    		break;
        		case "UNEXPECTED_COMPILER_ERROR":
        			// compiler error
        		case "BUILDER_ERROR":
        			// error/bug with cloudcoder builder
        			
        			// not sure what we want to do here, just gonna rollback and throw an error
        			conn.rollback();
        			throw new UnexpectedException("Unexpected compiler error or builder error. Changes not committed.");
        		}
        		
        		conn.commit();
        		System.out.println("Finished.");
        	}
        	
        	// done with every change
        	conn.close();
    	} catch (Exception e) {
    		throw e;
    	} finally {
    		if (result != null) {
    			result.close();
    		}
    		if (stmt != null) {
    			stmt.close();
    		}
    		if (conn != null) {
    			conn.close();
    		}
    	}
    }
    
    @Test
    public void testLineByLineCompilation()
    throws Exception 
    {
    	Integer userID = 70;
    	Integer problemID = 16;
    	
    	compileLineByLine(userID, problemID, true);
    }
    **/
    
    private static CompilationResult compile(String text) {
    	String preappendText = "public class ProblemWrapper {";
    	String postappendText = "}";
    	String docString = preappendText + text + postappendText;
		
		InMemoryJavaCompiler compiler = new InMemoryJavaCompiler();
		compiler.addSourceFile("ProblemWrapper", docString);
		compiler.compile();
		
		return compiler.getCompileResult();
    }
    
    private static String sanitizeString(String text) {
    	text = text.replace("'", "\\'");
    	return text;
    }
    
}
