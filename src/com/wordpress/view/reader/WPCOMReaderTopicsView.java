//#preprocess

//#ifdef BlackBerrySDK7.0.0

package com.wordpress.view.reader;


import net.rim.device.api.browser.field2.BrowserField;
import net.rim.device.api.browser.field2.BrowserFieldConfig;
import net.rim.device.api.browser.field2.BrowserFieldListener;
import net.rim.device.api.browser.field2.BrowserFieldRequest;
import net.rim.device.api.system.Application;
import net.rim.device.api.system.Characters;
import net.rim.device.api.system.KeyListener;
import net.rim.device.api.ui.Screen;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.container.MainScreen;

import org.w3c.dom.Document;

import com.wordpress.bb.WordPressResource;
import com.wordpress.controller.BaseController;
import com.wordpress.utils.log.Log;
import com.wordpress.view.dialog.ConnectionInProgressView;


public class WPCOMReaderTopicsView extends WPCOMReaderBase
{
    private BrowserField _browserField;    
    
    private ConnectionInProgressView connectionProgressView = null;
	private BrowserFieldRequest request = null;
	private final String topicsContent;
	private final WPCOMReaderListView parent;

    
    public WPCOMReaderTopicsView(WPCOMReaderListView parent, BrowserFieldRequest request, String topicsContent)
    {    
    	super(_resources.getString(WordPressResource.MENUITEM_READER));
		this.parent = parent;
    	this.request = request;
		this.topicsContent = topicsContent;
    	addKeyListener(new BrowserFieldScreenKeyListener());        
        BrowserFieldConfig config = getReaderBrowserDefaultConfig();
        _browserField = new BrowserField(config);
        _browserField.addListener(new InnerBrowserListener());
        _browserField.getConfig().setProperty(BrowserFieldConfig.ERROR_HANDLER, new ReaderBrowserFieldErrorHandler(_browserField) );
        try {
			extendJavaScript(_browserField);
		} catch (Exception e) {
			Log.error(e, "Error while binding JS code to Native Code");
		}
        add(_browserField);
    }
 
    
    /**
     * @see Screen#onUiEngineAttached(boolean)     
     */
    protected void onUiEngineAttached(boolean attached)
    {
        if(attached)
        {
            try
            {
            	
            	this.setPreferredConnectionTypes(_browserField);
            	
            	if( topicsContent != null ) {
                
            		_browserField.displayContent(topicsContent, "");
                
            	} else {
                	
                	connectionProgressView = new ConnectionInProgressView(
                			_resources.getString(WordPressResource.CONNECTION_INPROGRESS));
                	connectionProgressView.setDialogClosedListener(new ConnectionDialogClosedListener());
                	connectionProgressView.show();
                	
                	_browserField.requestContent(request);
                	
                	int res = UiApplication.getUiApplication().invokeLater(new Runnable() {
                		public void run() {
                			if ( connectionProgressView.isDisplayed())
                				UiApplication.getUiApplication().popScreen(connectionProgressView);
                			connectionProgressView = null;
                		} //end run
                	}, 2000, false);
                	
                	if ( res == -1 ) { //timer failed, remove the dialog immediately
                		UiApplication.getUiApplication().invokeLater(new Runnable() {
                			public void run() {
                				if ( connectionProgressView.isDisplayed())
                					UiApplication.getUiApplication().popScreen(connectionProgressView);
                				connectionProgressView = null;
                			} //end run
                		});
                	}
                }
            }
            catch(Exception e)
            {                
                deleteAll();
                add(new LabelField("ERROR:\n\n"));
                add(new LabelField(e.getMessage()));
            }
        }
    }
    
    /**
     * A class to listen for BrowserField events
     */
    private class InnerBrowserListener extends BrowserFieldListener
    {
    	public void documentLoaded(BrowserField browserField, Document document) {
    		Log.debug("Topics View has loaded the following URL : " + browserField.getDocumentUrl() );
    		//the browser has loaded the login form and authenticated the user...
    		UiApplication.getUiApplication().invokeLater(new Runnable() {
    			public void run() {
    				try {
    					_browserField.getScriptEngine().executeScript("document.setSelectedTopic('"+parent.getCurrentTopic()+"')", null);
    				} catch (Exception e) {
    					Log.error(e, "Error while setting the selectedTopic on Topics view");
    				}
    			} //end run
    		});
    	}
    }

    /**
     * A KeyListener implementation
     */
    private class BrowserFieldScreenKeyListener implements KeyListener
    {
    	/**
    	 * @see KeyListener#keyChar(char, int, int)
    	 */
    	public boolean keyChar(final char key, int status, int time)
    	{            
    		if(key == Characters.ESCAPE)
    		{
    			Runnable previousRunnable = new Runnable()
    			{
    				public void run()
    				{
						synchronized(Application.getEventLock()) 
						{
							close();
						}
    				}
    			};
    			new Thread(previousRunnable).start();
    			return true;
    		}
    		return false;            
    	}

        
       /**
        * @see KeyListener#keyDown(int, int)
        */
        public boolean keyDown(int keycode, int time)
        {
            return false;
        }

        
       /**
        * @see KeyListener#keyRepeat(int, int)
        */
        public boolean keyRepeat(int keycode, int time)
        {
            return false;
        }

        
       /**
        * @see KeyListener#keyStatus(int, int)
        */
        public boolean keyStatus(int keycode, int time)
        {
            return false;
        }

        
       /**
        * @see KeyListener#keyUp(int, int)
        */
        public boolean keyUp(int keycode, int time)
        {
            return false;
        }
    }


	public BaseController getController() {
		// TODO Auto-generated method stub
		return null;
	}
    
	protected void executeNativeJaveCode(String methodName, Object[] formalParamenters, Class[] formalParametersType) {
		Log.debug("Calling the following method "+ methodName + " on " + this.getClass().getName());
		if( methodName.equalsIgnoreCase("selectTopic")) {
			parent.setNewTopicAndRefreshTheReader((String)formalParamenters[0], (String)formalParamenters[1]);
			close();
		} else if( methodName.equalsIgnoreCase("setTitle")) {
			final String title = (String)formalParamenters[0];
			UiApplication.getUiApplication().invokeLater(new Runnable() {
				public void run() {
					setTitleText(title);
				} //end run
			});
		} else {
			Log.debug("Method not found: " + methodName);
		}
    }
}

//#endif