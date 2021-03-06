/*
 * Copyright 2011 JBoss Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.kie.workbench.common.widgets.client.popups.errors;

import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.constants.BackdropType;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import org.kie.workbench.common.widgets.client.popups.footers.ModalFooterOKButton;
import org.kie.workbench.common.widgets.client.resources.i18n.CommonConstants;

/**
 * A popup that can contain a list of items
 */
public class ErrorPopup extends Modal {

    interface PopupListWidgetBinder
            extends
            UiBinder<Widget, ErrorPopup> {

    }

    private static PopupListWidgetBinder uiBinder = GWT.create( PopupListWidgetBinder.class );

    @UiField
    protected Label message;

    private ErrorPopup() {
        setTitle( CommonConstants.INSTANCE.Error() );
        setMaxHeigth( ( Window.getClientHeight() * 0.75 ) + "px" );
        setBackdrop( BackdropType.STATIC );
        setKeyboard( true );
        setAnimation( true );
        setDynamicSafe( true );
        setHideOthers( false );

        add( uiBinder.createAndBindUi( this ) );
        add( new ModalFooterOKButton( new Command() {
            @Override
            public void execute() {
                hide();
            }
        } ) );
    }

    public void setMessage( final String message ) {
        this.message.setText( message );
    }

    public static void showMessage( String message ) {
        final ErrorPopup instance = new ErrorPopup();
        instance.setMessage( message );
        instance.show();
    }

}
