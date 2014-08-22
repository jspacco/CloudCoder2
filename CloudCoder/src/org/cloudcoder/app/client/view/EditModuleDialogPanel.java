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

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.TextBox;

/**
 * @author jaimespacco
 *
 */
public class EditModuleDialogPanel extends Composite
{
    private TextBox moduleTextBox;
    private Runnable cancelButtonCallback;
    private Runnable editModuleButtonCallback;
    private Label errorLabel;
    
    public EditModuleDialogPanel() {
        LayoutPanel layoutPanel = new LayoutPanel();
        initWidget(layoutPanel);
        layoutPanel.setSize("450px", "340px");
        
        Label dialogTitleLabel = new Label("Edit module for execise(s)");
        dialogTitleLabel.setStyleName("cc-dialogTitle");
        layoutPanel.add(dialogTitleLabel);
        layoutPanel.setWidgetLeftRight(dialogTitleLabel, 10.0, Unit.PX, 10.0, Unit.PX);
        layoutPanel.setWidgetTopHeight(dialogTitleLabel, 10.0, Unit.PX, 28.0, Unit.PX);
        
        HTML instructionTextLabel = new HTML("To edit the module for all of the selected exercises, change the text below and click \"Save eidts\" button.<br /><br />\n" +
                "Click the \"Cancel\" button if you do not want to edit the module for these exercises.", true);
        layoutPanel.add(instructionTextLabel);
        layoutPanel.setWidgetLeftRight(instructionTextLabel, 10.0, Unit.PX, 10.0, Unit.PX);
        layoutPanel.setWidgetTopHeight(instructionTextLabel, 44.0, Unit.PX, 100.0, Unit.PX);
        
        moduleTextBox = new TextBox();
        layoutPanel.add(moduleTextBox);
        layoutPanel.setWidgetLeftWidth(moduleTextBox, 132.0, Unit.PX, 161.0, Unit.PX);
        layoutPanel.setWidgetTopHeight(moduleTextBox, 180.0, Unit.PX, 31.0, Unit.PX);
        
        Button cancelButton = new Button("Cancel");
        cancelButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                handleCancelButtonClick();
            }
        });
        layoutPanel.add(cancelButton);
        layoutPanel.setWidgetLeftWidth(cancelButton, 100.0, Unit.PX, 120.0, Unit.PX);
        layoutPanel.setWidgetTopHeight(cancelButton, 267.0, Unit.PX, 27.0, Unit.PX);
        
        Button editModuleButton = new Button("Edit module");
        editModuleButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                handleEditModuleButtonClick();
            }
        });
        layoutPanel.add(editModuleButton);
        layoutPanel.setWidgetRightWidth(editModuleButton, 100.0, Unit.PX, 120.0, Unit.PX);
        layoutPanel.setWidgetTopHeight(editModuleButton, 267.0, Unit.PX, 27.0, Unit.PX);
        
        errorLabel = new Label("");
        errorLabel.setStyleName("cc-errorText");
        layoutPanel.add(errorLabel);
        layoutPanel.setWidgetLeftRight(errorLabel, 10.0, Unit.PX, 10.0, Unit.PX);
        layoutPanel.setWidgetTopHeight(errorLabel, 313.0, Unit.PX, 15.0, Unit.PX);
        
        
    }
    
    /**
     * Set a callback to run when the "Cancel" button is clicked.
     * 
     * @param cancelButtonCallback the cancelButtonCallback to set
     */
    public void setCancelButtonCallback(Runnable cancelButtonCallback) {
        this.cancelButtonCallback = cancelButtonCallback;
    }
    
    /**
     * Set a callback to run when the "Share exercise" button is clicked.
     * 
     * @param shareExerciseButtonCallback the shareExerciseButtonCallback to set
     */
    public void setEditModuleButtonCallback(
            Runnable editModuleButtonCallback) {
        this.editModuleButtonCallback = editModuleButtonCallback;
    }
    
    public String getModuleNameString() {
        return moduleTextBox.getText();
    }
    
    protected void handleEditModuleButtonClick() {
        if (editModuleButtonCallback!= null) {
            editModuleButtonCallback.run();
        }
    }

    protected void handleCancelButtonClick() {
        if (cancelButtonCallback != null) {
            cancelButtonCallback.run();
        }
    }
    /**
     * Set error message to display.
     * 
     * @param errorMessage error message
     */
    public void setErrorMessage(String errorMessage) {
        errorLabel.setText(errorMessage);
    }
}
