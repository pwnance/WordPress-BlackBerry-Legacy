package com.wordpress.view;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.HttpConnection;
import javax.microedition.io.InputConnection;

import net.rim.device.api.browser.field.BrowserContent;
import net.rim.device.api.browser.field.BrowserContentChangedEvent;
import net.rim.device.api.browser.field.Event;
import net.rim.device.api.browser.field.RedirectEvent;
import net.rim.device.api.browser.field.RenderingApplication;
import net.rim.device.api.browser.field.RenderingException;
import net.rim.device.api.browser.field.RenderingOptions;
import net.rim.device.api.browser.field.RenderingSession;
import net.rim.device.api.browser.field.RequestedResource;
import net.rim.device.api.browser.field.UrlRequestedEvent;
import net.rim.device.api.io.http.HttpHeaders;
import net.rim.device.api.system.Application;
import net.rim.device.api.system.Display;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.Status;

import com.wordpress.bb.WordPressResource;
import com.wordpress.controller.BaseController;
import com.wordpress.utils.SecondaryResourceFetchThread;
import com.wordpress.view.dialog.ConnectionInProgressView;

public class PostPreviewView  extends BaseView implements RenderingApplication {
	ConnectionInProgressView connectionProgressView=null;
	
	private static final String REFERER = "referer";   
	private RenderingSession _renderingSession;   
	private InputConnection  _currentConnection;
	private BrowserContent browserContent = null;
	
	public PostPreviewView(String html) {
		super(_resources.getString(WordPressResource.TITLE_POSTVIEW));
				
        
		_renderingSession = RenderingSession.getNewInstance();        
        // Enable javascript.
       // _renderingSession.getRenderingOptions().setProperty(RenderingOptions.CORE_OPTIONS_GUID,  RenderingOptions.JAVASCRIPT_ENABLED, true);
		
		_renderingSession.getRenderingOptions().setProperty(RenderingOptions.CORE_OPTIONS_GUID,  RenderingOptions.ENABLE_CSS, true);
        _renderingSession.getRenderingOptions().setProperty(RenderingOptions.CORE_OPTIONS_GUID,  RenderingOptions.CSS_MEDIA_TYPE, "screen");
        _renderingSession.getRenderingOptions().setProperty(RenderingOptions.CORE_OPTIONS_GUID,  RenderingOptions.ENABLE_IMAGE_SAVING, false);
        _renderingSession.getRenderingOptions().setProperty(RenderingOptions.CORE_OPTIONS_GUID,  RenderingOptions.ENABLE_AUDIO_SAVING, false);
         
        _renderingSession.getRenderingOptions().setProperty(RenderingOptions.CORE_OPTIONS_GUID,  RenderingOptions.ADD_IMAGE_ADDRESS_MENU_ITEM, false);
        _renderingSession.getRenderingOptions().setProperty(RenderingOptions.CORE_OPTIONS_GUID,  RenderingOptions.ADD_LINK_ADDRESS_MENU_ITEM, false);
        _renderingSession.getRenderingOptions().setProperty(RenderingOptions.CORE_OPTIONS_GUID,  RenderingOptions.ALLOW_POPUPS, false);
                        
        PrimaryResourceFetchThread thread = new PrimaryResourceFetchThread(html, null, this);
        thread.start();       
	}
	
   
	  public void processConnection(InputConnection connection, Event e) 
	    {
	        // Cancel previous request.
	        if (_currentConnection != null) 
	        {
	            try 
	            {
	                _currentConnection.close();
	            } 
	            catch (IOException e1) 
	            {
	            }
	        }
	        
	        _currentConnection = connection;
	        
	       
	        
	        try   
	        {
	            browserContent = _renderingSession.getBrowserContent(connection, "", this, e);
	            
	            if (browserContent != null) 
	            {
	                Field field = browserContent.getDisplayableContent();
	                
	                if (field != null) 
	                {
	                	field.setMargin(margins);
	                    synchronized (Application.getEventLock()) 
	                    {
	                        deleteAll();
	                        add(field);
	                    }
	                }
	                
	                browserContent.finishLoading();
	            }
	        } 
	        catch (RenderingException re) 
	        {
	        	System.out.println(re.getMessage());
	        } 
	        finally 
	        {
	           SecondaryResourceFetchThread.doneAddingImages();
	        }
	        
	    }    

	    /**
	     * @see net.rim.device.api.browser.RenderingApplication#eventOccurred(net.rim.device.api.browser.Event)
	     */
	    public Object eventOccurred(Event event) 
	    {
	        int eventId = event.getUID();

	        switch (eventId) 
	        {
	            case Event.EVENT_URL_REQUESTED : 
	            {
	                UrlRequestedEvent urlRequestedEvent = (UrlRequestedEvent) event;    
	                String absoluteUrl = urlRequestedEvent.getURL();
	    
	                HttpConnection conn = null;
	             /*   PrimaryResourceFetchThread thread = new PrimaryResourceFetchThread(urlRequestedEvent.getURL(),
	                                                                                         urlRequestedEvent.getHeaders(), 
	                                                                                         urlRequestedEvent.getPostData(),
	                                                                                         event, this);
	                thread.start();
	    */
	                break;

	            } 
	            case Event.EVENT_BROWSER_CONTENT_CHANGED: 
	            {                
	                // Browser field title might have changed update title.
	                BrowserContentChangedEvent browserContentChangedEvent = (BrowserContentChangedEvent) event; 
	            
	                if (browserContentChangedEvent.getSource() instanceof BrowserContent) 
	                { 
	                    BrowserContent browserField = (BrowserContent) browserContentChangedEvent.getSource(); 
	                    String newTitle = browserField.getTitle();
	                    if (newTitle != null) 
	                    {
	                        synchronized (UiApplication.getUiApplication().getAppEventLock()) 
	                        { 
	                            setTitle(newTitle);
	                        }                                               
	                    }                                       
	                }                   

	                break;                

	            } 
	            case Event.EVENT_REDIRECT : 
	            {
	                RedirectEvent e = (RedirectEvent) event;
	                String referrer = e.getSourceURL();
	                
	                switch (e.getType()) 
	                {  
	                    case RedirectEvent.TYPE_SINGLE_FRAME_REDIRECT :
	                        // Show redirect message.
	                        Application.getApplication().invokeAndWait(new Runnable() 
	                        {
	                            public void run() 
	                            {
	                                Status.show("You are being redirected to a different page...");
	                            }
	                        });
	                    
	                    break;
	                    
	                    case RedirectEvent.TYPE_JAVASCRIPT :
	                        break;
	                    
	                    case RedirectEvent.TYPE_META :
	                        // MSIE and Mozilla don't send a Referer for META Refresh.
	                        referrer = null;     
	                        break;
	                    
	                    case RedirectEvent.TYPE_300_REDIRECT :
	                        // MSIE, Mozilla, and Opera all send the original
	                        // request's Referer as the Referer for the new
	                        // request.
	                        Object eventSource = e.getSource();
	                        if (eventSource instanceof HttpConnection) 
	                        {
	                            referrer = ((HttpConnection)eventSource).getRequestProperty(REFERER);
	                        }
	                        
	                        break;
	                    }
	                    
	                    HttpHeaders requestHeaders = new HttpHeaders();
	                    requestHeaders.setProperty(REFERER, referrer);
	                 //   PrimaryResourceFetchThread thread = new PrimaryResourceFetchThread(e.getLocation(), requestHeaders,null, event, this);
	                   // thread.start();
	                    break;

	            } 
	            case Event.EVENT_CLOSE :
	                break;
	            
	            case Event.EVENT_SET_HEADER :        // No cache support.
	            case Event.EVENT_SET_HTTP_COOKIE :   // No cookie support.
	            case Event.EVENT_HISTORY :           // No history support.
	            case Event.EVENT_EXECUTING_SCRIPT :  // No progress bar is supported.
	            case Event.EVENT_FULL_WINDOW :       // No full window support.
	            case Event.EVENT_STOP :              // No stop loading support.
	            default :
	        }

	        return null;
	    }

	    /**
	     * @see net.rim.device.api.browser.RenderingApplication#getAvailableHeight(net.rim.device.api.browser.BrowserContent)
	     */
	    public int getAvailableHeight(BrowserContent browserField) 
	    {
	        // Field has full screen.
	        return Display.getHeight();
	    }

	    /**
	     * @see net.rim.device.api.browser.RenderingApplication#getAvailableWidth(net.rim.device.api.browser.BrowserContent)
	     */
	    public int getAvailableWidth(BrowserContent browserField) 
	    {
	        // Field has full screen.
	        return Display.getWidth();
	    }

	    /**
	     * @see net.rim.device.api.browser.RenderingApplication#getHistoryPosition(net.rim.device.api.browser.BrowserContent)
	     */
	    public int getHistoryPosition(BrowserContent browserField) 
	    {
	        // No history support.
	        return 0;
	    }
	    

	    /**
	     * @see net.rim.device.api.browser.RenderingApplication#getHTTPCookie(java.lang.String)
	     */
	    public String getHTTPCookie(String url) 
	    {
	        // No cookie support.
	        return null;
	    }

	    /**
	     * @see net.rim.device.api.browser.RenderingApplication#getResource(net.rim.device.api.browser.RequestedResource,
	     *      net.rim.device.api.browser.BrowserContent)
	     */
	    public HttpConnection getResource( RequestedResource resource, BrowserContent referrer) 
	    {
	        if (resource == null) 
	        {
	            return null;
	        }

	        // Check if this is cache-only request.
	        if (resource.isCacheOnly()) 
	        {
	            // No cache support.
	            return null;
	        }

	        String url = resource.getUrl();

	        if (url == null) 
	        {
	            return null;
	        }

	        // If referrer is null we must return the connection.
	        if (referrer == null) 
	        {
	            HttpConnection connection = SecondaryResourceFetchThread.makeConnection(resource.getUrl(), resource.getRequestHeaders(), null);
	            
	            return connection;
	            
	        } 
	        else 
	        {
	            // If referrer is provided we can set up the connection on a separate thread.
	           SecondaryResourceFetchThread.enqueue(resource, referrer);
	        }

	        return null;
	    }

	    /**
	     * @see net.rim.device.api.browser.RenderingApplication#invokeRunnable(java.lang.Runnable)
	     */
	    public void invokeRunnable(Runnable runnable) 
	    {       
	        (new Thread(runnable)).start();
	    }    
	

	
	public BaseController getController() {
		return null;
	}
	
	protected void makeMenu(Menu menu, int instance)
    {
    	//menu.deleteAll();
		removeMenuWithLabel("Get", menu);	
		removeMenuWithLabel("Sel", menu);
		removeMenuWithLabel("Find", menu);
	
       	super.makeMenu(menu, instance);
    }
	
	private void removeMenuWithLabel(String labelToRemove, Menu menu) {
		int size = menu.getSize();
		for(int i=0; i < size; i++) {
		    MenuItem item = menu.getItem(i);
		    String label = item.toString();
		    if(label.startsWith(labelToRemove)) {
		    	menu.deleteItem(i);
		    	break;
	    	    }			
		}
	}
	
	private class PrimaryResourceFetchThread extends Thread 
	{
	    
	    private PostPreviewView _application;
	    private Event _event;
		private final String html;
	    
	    PrimaryResourceFetchThread(String html, Event event, PostPreviewView application) 
	    {
	        this.html = html;
			_application = application;
	        _event = event;
	    }

	    public void run() 
	    {
	    	HttpConnection connection = new HttpConnOnString(html);
	        _application.processConnection(connection, _event);        
	    }
	    
	}
	
	private class HttpConnOnString implements HttpConnection {
		private InputStream is = null;

		public HttpConnOnString(String html){
			is = new ByteArrayInputStream(html.getBytes());
		}
		
		public long getDate() throws IOException {
			return 0;
		}

		public long getExpiration() throws IOException {
			return 0;
		}

		public String getFile() {
			return null;
		}

		public String getHeaderField(String name) throws IOException {
			return null;
		}

		public String getHeaderField(int n) throws IOException {
			return null;
		}

		public long getHeaderFieldDate(String name, long def)
				throws IOException {
			return 0;
		}

		public int getHeaderFieldInt(String name, int def) throws IOException {
			return 0;
		}

		public String getHeaderFieldKey(int n) throws IOException {
			return null;
		}

		public String getHost() {
			return null;
		}

		public long getLastModified() throws IOException {
			return 0;
		}

		public int getPort() {
			return 0;
		}

		public String getProtocol() {
			return null;
		}

		public String getQuery() {
			return null;
		}

		public String getRef() {
			return null;
		}

		public String getRequestMethod() {
			return null;
		}

		public String getRequestProperty(String key) {
			return null;
		}

		public int getResponseCode() throws IOException {
			return 200;
		}

		public String getResponseMessage() throws IOException {
			return "OK";
		}

		public String getURL() {
			return "";
		}

		public void setRequestMethod(String method) throws IOException {
			
		}

		public void setRequestProperty(String key, String value)
				throws IOException {
			
		}

		public String getEncoding() {
			return "UTF-8";
		}

		public long getLength() {
			try {
				if (is != null )
					return is.available();
				else return 0;
			} catch (IOException e) {
			}
			return 0L;
		}

		public String getType() {
			return "text/html";
		}

		public DataInputStream openDataInputStream() throws IOException {
			return new DataInputStream(is);
		}

		public InputStream openInputStream() throws IOException {
			return is;
		}

		public void close() throws IOException {
			is.close();
			
		}

		public DataOutputStream openDataOutputStream() throws IOException {
			return null;
		}

		public OutputStream openOutputStream() throws IOException {
			return null;
		}
		
	}
	
    //override onClose() to stop all internet activity immediatly 
	public boolean onClose()   {
		SecondaryResourceFetchThread.stopAllActivity();
		return super.onClose();
    }
	
}