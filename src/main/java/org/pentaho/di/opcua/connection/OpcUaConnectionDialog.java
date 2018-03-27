/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2018 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.opcua.connection;

import org.apache.commons.lang.StringUtils;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.gui.WindowProperty;
import org.pentaho.di.ui.core.widget.PasswordTextVar;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

/**
 * Dialog that allows you to edit the settings of an OPC UA Connection
 *
 * @author Matt
 * @since 2017-12-14
 */

public class OpcUaConnectionDialog extends Dialog {
  private static Class<?> PKG = OpcUaConnectionDialog.class; // for i18n purposes, needed by Translator2!!
  private final VariableSpace space;

  private OpcUaConnection connection;

  private Shell shell;

  // Fields...
  private Text wName;
  private TextVar wUrl; // , wPort, wTenant, wNamespace, wUsername, wPassword;
  // private Button wSSL;

  private Button wOK, wCancel, wTest;

  Control lastControl;
  
  private PropsUI props;

  private int middle;
  private int margin;

  private boolean ok;
  
  public OpcUaConnectionDialog( Shell par, VariableSpace space, OpcUaConnection connection ) {
    super( par, SWT.NONE );
    this.connection = connection;
    this.space = space;
    props = PropsUI.getInstance();
    ok = false;
  }

  public boolean open() {
    Shell parent = getParent();
    shell = new Shell( parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN );
    props.setLook( shell );
    shell.setImage( GUIResource.getInstance().getImageSlave() );

    middle = props.getMiddlePct();
    margin = Const.MARGIN;

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = Const.FORM_MARGIN;
    formLayout.marginHeight = Const.FORM_MARGIN;

    shell.setText( BaseMessages.getString( PKG, "OpcUaConnectionDialog.Shell.Title" ) );
    shell.setLayout( formLayout );

    addFormWidgets();
    
    // Buttons
    wOK = new Button( shell, SWT.PUSH );
    wOK.setText( BaseMessages.getString( PKG, "System.Button.OK" ) );

    wTest = new Button( shell, SWT.PUSH );
    wTest.setText( BaseMessages.getString( PKG, "System.Button.Test" ) );

    wCancel = new Button( shell, SWT.PUSH );
    wCancel.setText( BaseMessages.getString( PKG, "System.Button.Cancel" ) );

    Button[] buttons = new Button[] { wOK, wTest, wCancel };
    BaseStepDialog.positionBottomButtons( shell, buttons, margin, lastControl );

    // Add listeners
    wOK.addListener( SWT.Selection, new Listener() {
      public void handleEvent( Event e ) {
        ok();
      }
    } );
    wTest.addListener( SWT.Selection, new Listener() {
      public void handleEvent( Event e ) {
        test();
      }
    } );
    wCancel.addListener( SWT.Selection, new Listener() {
      public void handleEvent( Event e ) {
        cancel();
      }
    } );

    SelectionAdapter selAdapter = new SelectionAdapter() {
      public void widgetDefaultSelected( SelectionEvent e ) {
        ok();
      }
    };
    wUrl.addSelectionListener( selAdapter );

    // Detect X or ALT-F4 or something that kills this window...
    shell.addShellListener( new ShellAdapter() {
      public void shellClosed( ShellEvent e ) {
        cancel();
      }
    } );

    getData();

    BaseStepDialog.setSize( shell );

    shell.open();
    Display display = parent.getDisplay();
    while ( !shell.isDisposed() ) {
      if ( !display.readAndDispatch() ) {
        display.sleep();
      }
    }
    return ok;
  }

  private void addFormWidgets() {
    
    // What's the name
    Label wlName = new Label( shell, SWT.RIGHT );
    props.setLook( wlName );
    wlName.setText( BaseMessages.getString( PKG, "OpcUaConnectionDialog.ServerName.Label" ) );
    FormData fdlName = new FormData();
    fdlName.top = new FormAttachment( 0, 0 );
    fdlName.left = new FormAttachment( 0, 0 ); // First one in the left top corner
    fdlName.right = new FormAttachment( middle, -margin );
    wlName.setLayoutData( fdlName );
    wName = new Text( shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wName );
    FormData fdName = new FormData();
    fdName.top = new FormAttachment( 0, 0 );
    fdName.left = new FormAttachment( middle, 0 ); // To the right of the label
    fdName.right = new FormAttachment( 95, 0 );
    wName.setLayoutData( fdName );
    lastControl = wName;
    
    // What's the hostname
    Label wlUrl = new Label( shell, SWT.RIGHT );
    props.setLook( wlUrl );
    wlUrl.setText( BaseMessages.getString( PKG, "HCPConnectionDialog.URL.Label" ) );
    FormData fdlUrl = new FormData();
    fdlUrl.top = new FormAttachment( lastControl, margin );
    fdlUrl.left = new FormAttachment( 0, 0 ); // First one in the left top corner
    fdlUrl.right = new FormAttachment( middle, -margin );
    wlUrl.setLayoutData( fdlUrl );
    wUrl = new TextVar( space, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wUrl );
    FormData fdUrl = new FormData();
    fdUrl.top = new FormAttachment( lastControl, margin * 2 );
    fdUrl.left = new FormAttachment( middle, 0 ); // To the right of the label
    fdUrl.right = new FormAttachment( 95, 0 );
    wUrl.setLayoutData( fdUrl );
    lastControl = wUrl;

    /*
    // What's the port?
    Label wlPort = new Label( shell, SWT.RIGHT );
    props.setLook( wlPort );
    wlPort.setText( BaseMessages.getString( PKG, "HCPConnectionDialog.Port.Label" ) );
    FormData fdlPort = new FormData();
    fdlPort.top = new FormAttachment( lastControl, margin );
    fdlPort.left = new FormAttachment( 0, 0 ); // First one in the left top corner
    fdlPort.right = new FormAttachment( middle, -margin );
    wlPort.setLayoutData( fdlPort );
    wPort = new TextVar( connection, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wPort );
    FormData fdPort = new FormData();
    fdPort.top = new FormAttachment( lastControl, margin );
    fdPort.left = new FormAttachment( middle, 0 ); // To the right of the label
    fdPort.right = new FormAttachment( 95, 0 );
    wPort.setLayoutData( fdPort );
    lastControl = wPort;
    
    // Tenant
    Label wlTenant = new Label( shell, SWT.RIGHT );
    wlTenant.setText( BaseMessages.getString( PKG, "HCPConnectionDialog.Tenant.Label" ) );
    props.setLook( wlTenant );
    FormData fdlTenant = new FormData();
    fdlTenant.top = new FormAttachment( lastControl, margin );
    fdlTenant.left = new FormAttachment( 0, 0 );
    fdlTenant.right = new FormAttachment( middle, -margin );
    wlTenant.setLayoutData( fdlTenant );
    wTenant = new TextVar( connection, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wTenant );
    FormData fdTenant = new FormData();
    fdTenant.top = new FormAttachment( lastControl, margin );
    fdTenant.left = new FormAttachment( middle, 0 );
    fdTenant.right = new FormAttachment( 95, 0 );
    wTenant.setLayoutData( fdTenant );
    lastControl = wTenant;

    // Namespace
    Label wlNamespace = new Label( shell, SWT.RIGHT );
    wlNamespace.setText( BaseMessages.getString( PKG, "HCPConnectionDialog.Namespace.Label" ) );
    props.setLook( wlNamespace );
    FormData fdlNamespace = new FormData();
    fdlNamespace.top = new FormAttachment( lastControl, margin );
    fdlNamespace.left = new FormAttachment( 0, 0 );
    fdlNamespace.right = new FormAttachment( middle, -margin );
    wlNamespace.setLayoutData( fdlNamespace );
    wNamespace = new TextVar( connection, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wNamespace );
    FormData fdNamespace = new FormData();
    fdNamespace.top = new FormAttachment( lastControl, margin );
    fdNamespace.left = new FormAttachment( middle, 0 );
    fdNamespace.right = new FormAttachment( 95, 0 );
    wNamespace.setLayoutData( fdNamespace );
    lastControl = wNamespace;

    // Username
    Label wlUsername = new Label( shell, SWT.RIGHT );
    wlUsername.setText( BaseMessages.getString( PKG, "HCPConnectionDialog.UserName.Label" ) );
    props.setLook( wlUsername );
    FormData fdlUsername = new FormData();
    fdlUsername.top = new FormAttachment( lastControl, margin );
    fdlUsername.left = new FormAttachment( 0, 0 );
    fdlUsername.right = new FormAttachment( middle, -margin );
    wlUsername.setLayoutData( fdlUsername );
    wUsername = new TextVar( connection, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wUsername );
    FormData fdUsername = new FormData();
    fdUsername.top = new FormAttachment( lastControl, margin );
    fdUsername.left = new FormAttachment( middle, 0 );
    fdUsername.right = new FormAttachment( 95, 0 );
    wUsername.setLayoutData( fdUsername );
    lastControl = wUsername;
    
    // Password
    Label wlPassword = new Label( shell, SWT.RIGHT );
    wlPassword.setText( BaseMessages.getString( PKG, "HCPConnectionDialog.Password.Label" ) );
    props.setLook( wlPassword );
    FormData fdlPassword = new FormData();
    fdlPassword.top = new FormAttachment( wUsername, margin );
    fdlPassword.left = new FormAttachment( 0, 0 );
    fdlPassword.right = new FormAttachment( middle, -margin );
    wlPassword.setLayoutData( fdlPassword );
    wPassword = new PasswordTextVar( connection, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wPassword );
    FormData fdPassword = new FormData();
    fdPassword.top = new FormAttachment( wUsername, margin );
    fdPassword.left = new FormAttachment( middle, 0 );
    fdPassword.right = new FormAttachment( 95, 0 );
    wPassword.setLayoutData( fdPassword );
    lastControl = wPassword;
    
    // Https
    Label wlSSL = new Label( shell, SWT.RIGHT );
    wlSSL.setText( BaseMessages.getString( PKG, "HCPConnectionDialog.UseSsl.Label" ) );
    props.setLook( wlSSL );
    FormData fd = new FormData();
    fd.top = new FormAttachment( lastControl, margin );
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( middle, -margin );
    wlSSL.setLayoutData( fd );
    wSSL = new Button( shell, SWT.CHECK );
    props.setLook( wSSL );
    FormData bfd = new FormData();
    bfd.top = new FormAttachment( lastControl, margin );
    bfd.left = new FormAttachment( middle, 0 );
    bfd.right = new FormAttachment( 95, 0 );
    wSSL.setLayoutData( bfd );
    lastControl = wSSL;

    */
  }

  public void dispose() {
    props.setScreen( new WindowProperty( shell ) );
    shell.dispose();
  }

  public void getData() {
    wName.setText( Const.NVL( connection.getName(), "" ) );
    wUrl.setText( Const.NVL( connection.getUrl(), "" ) );

    /*
    wPort.setText( Const.NVL( connection.getPort(), "" ) );
    wTenant.setText( Const.NVL( connection.getTenant(), "" ) );
    wNamespace.setText( Const.NVL( connection.getNamespace(), "" ) );
    wUsername.setText( Const.NVL( connection.getUsername(), "" ) );
    wPassword.setText( Const.NVL( connection.getPassword(), "" ) );
    wSSL.setSelection( connection.isUseSSL() );
    */

    wName.setFocus();
  }

  private void cancel() {
    ok=false;
    dispose();
  }

  public void ok() {
    if (StringUtils.isEmpty(wName.getText())) {
      MessageBox box = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
      box.setText(BaseMessages.getString(PKG, "OpcUaConnectionDialog.NoNameDialog.Title"));
      box.setMessage(BaseMessages.getString(PKG, "OpcUaConnectionDialog.NoNameDialog.Message"));
      box.open();
      return;
    }
    getInfo( connection );
    ok = true;
    dispose();
  }

  // Get dialog info in securityService
  private void getInfo(OpcUaConnection connection) {
    connection.setName( wName.getText() );
    connection.setUrl( wUrl.getText() );

    /*
      connection.setPort( wPort.getText() );
      connection.setTenant( wTenant.getText() );
      connection.setNamespace( wNamespace.getText() );
      connection.setUsername( wUsername.getText() );
      connection.setPassword( wPassword.getText() );
      connection.setUseSSL(wSSL.getSelection() );
    */

  }

  public void test() {
    try {
      OpcUaConnection connection = new OpcUaConnection();
      getInfo(connection);
      connection.test(space, SecurityPolicy.None, new AnonymousProvider());
      MessageBox box = new MessageBox(shell, SWT.OK);
      box.setMessage("OK");
      box.setText("OK");
      box.open();
    } catch(Exception e) {
      new ErrorDialog(shell, "Error", "Error connecting to OPC UA server using URL '"+wUrl.getText()+"'", e);
    }
  }
}