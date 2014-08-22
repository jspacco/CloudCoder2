// CloudCoder - a web-based pedagogical programming environment
// Copyright (C) 2011-2014, Jaime Spacco <jspacco@knox.edu>
// Copyright (C) 2011-2014, David H. Hovemeyer <david.hovemeyer@gmail.com>
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.cloudcoder.app.client.view;

import java.util.List;
import java.util.Set;

import org.cloudcoder.app.client.model.StatusMessage;
import org.cloudcoder.app.client.rpc.RPC;
import org.cloudcoder.app.shared.dto.ShareExercisesResult;
import org.cloudcoder.app.shared.model.CloudCoderAuthenticationException;
import org.cloudcoder.app.shared.model.ICallback;
import org.cloudcoder.app.shared.model.Module;
import org.cloudcoder.app.shared.model.OperationResult;
import org.cloudcoder.app.shared.model.Problem;
import org.cloudcoder.app.shared.model.ProblemAndModule;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;

/**
 * @author jaimespacco
 *
 */
public class EditModuleDialogBox extends DialogBox
{
    private Set<ProblemAndModule> problems;
    private ICallback<ShareExercisesResult> resultCallback;
    private EditModuleDialogPanel panel;
    private ICallback<Set<ProblemAndModule>> editModuleCallback;
    
    public EditModuleDialogBox() {
        setTitle("Edit Module(s)");
        setGlassEnabled(true);

        this.panel = new EditModuleDialogPanel();
        add(panel);
        
        panel.setCancelButtonCallback(new Runnable() {
            @Override
            public void run() {
                hide();
            }
        });
        
        panel.setEditModuleButtonCallback(new Runnable() {
            @Override
            public void run() {
                if (problems!=null) {
                    for (ProblemAndModule p : problems){
                        p.getModule().setName(panel.getModuleNameString());
                    }
                }
                editModuleCallback.call(problems);
                hide();
            }
        });
    }
    
    /**
     * @param exercise the exercise to set
     */
    public void setExercise(Set<ProblemAndModule> problems) {
        this.problems = problems;
    }

    /**
     * Set the result callback that will receive the {@link OperationResult}
     * from attempting to share the exercise to the repository.
     * 
     * @param resultCallback the result callback
     */
    public void setResultCallback(ICallback<ShareExercisesResult> resultCallback) {
        this.resultCallback = resultCallback;
    }
    /**
     * @param iCallback
     */
    public void setEditModuleNameCallback(ICallback<Set<ProblemAndModule>> callback)
    {
        this.editModuleCallback=callback;
    }
}
