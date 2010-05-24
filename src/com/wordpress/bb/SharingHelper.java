//#preprocess
package com.wordpress.bb;

import java.util.Vector;

import javax.microedition.content.ActionNameMap;
import javax.microedition.content.ContentHandler;
import javax.microedition.content.ContentHandlerException;
import javax.microedition.content.ContentHandlerServer;
import javax.microedition.content.Invocation;
import javax.microedition.content.Registry;
import javax.microedition.content.RequestListener;

import net.rim.blackberry.api.menuitem.ApplicationMenuItem;
import net.rim.blackberry.api.menuitem.ApplicationMenuItemRepository;
import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.i18n.ResourceBundleFamily;
import net.rim.device.api.system.ApplicationDescriptor;
import net.rim.device.api.ui.Screen;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;

import com.wordpress.controller.FrontController;
import com.wordpress.io.BlogDAO;
import com.wordpress.model.AudioEntry;
import com.wordpress.model.Blog;
import com.wordpress.model.BlogInfo;
import com.wordpress.model.MediaEntry;
import com.wordpress.model.PhotoEntry;
import com.wordpress.model.Post;
import com.wordpress.model.VideoEntry;
import com.wordpress.utils.MultimediaUtils;
import com.wordpress.utils.log.Log;
import com.wordpress.view.MainView;
import com.wordpress.view.component.SelectorPopupScreen;

public class SharingHelper implements RequestListener{
	
	/* We are using a singleton here. 
	 * Keep in mind that you should remove manually the listener from the CHAPI server at application exist. 
	 * Otherwise, the CHAPI server retain a reference to this class ever, and on next startup you have already 
	 * a listener in the place. While we want add the listener not just after apps start, whitin the main screen view.
	 */
	
	private static SharingHelper instance;
	private ResourceBundleFamily _resources;
	
	/* used when sharing via CHAPI apis*/
	public final String CHAPI_CLASS_NAME = "com.wordpress.bb.WordPress";//WordPress.class.getName();
    public final static String CHAPI_ID = "com.wordpress.bb";
    public final static String[] CHAPI_MIME_TYPES = new String[] {
    	"video/x-msvideo", "video/quicktime", "video/mp4", "video/mpeg", "video/3gpp", "video/3gpp2",
    	"audio/mpeg", "audio/mp4", "audio/wav", "application/ogg",
    	"image/jpg", "image/jpeg", "image/bmp", "image/png", "image/gif"
    };
    public final static String[] CHAPI_SUFFIXES = new String[] {
    	"avi", "mov", "mp4", "m4v", "mpg", "3gp", "3g2",
    	"mp3", "m4a","wav", "ogg",
    	"jpg", "jpeg","bmp", "png", "gif"
    };
    
    private Invocation pending;
    private ContentHandlerServer server;
    
	public static SharingHelper getInstance() {
		if (instance == null) {
			instance = new SharingHelper();
		}
		return instance;
	}
    
	private SharingHelper() {
		Log.trace("SharingHelper constructor");
		 //retrieve a reference to the ResourceBundle for localization support
        _resources = ResourceBundle.getBundle(WordPressResource.BUNDLE_ID, WordPressResource.BUNDLE_NAME);
	};
	
	public void removeCHAPIListener() {
		try {
			server = Registry.getServer(CHAPI_CLASS_NAME);
			server.setListener(null);           //set the listener 
		} catch (ContentHandlerException e) {
			Log.error(e,"Error during SharingHelp init" );
		}
	}
	
    
	public void verifyRegistration() {
		Log.trace(">>> verifyRegistration");
		
		try
		{
		Registry registry = Registry.getRegistry(CHAPI_CLASS_NAME);
		ContentHandler registered = registry.forID(CHAPI_ID, true);

		if (registered != null)
		{
			return;
		}
		// Wasn't registered before, so do it now.
		String[] actions = new String[] { ContentHandler.ACTION_SEND };
		String[] actionNames = new String[] { _resources.getString(WordPressResource.MENUITEM_SHARE_TO_WORDPRESS) };
		ActionNameMap[] maps = new ActionNameMap[]{ new ActionNameMap(actions, actionNames, "en") };
			registry.register(CHAPI_CLASS_NAME, CHAPI_MIME_TYPES, CHAPI_SUFFIXES, actions, maps,
					CHAPI_ID, null);
		}
		catch (Exception e)
		{
			Log.error(e, "Could not register for " + CHAPI_ID);
		}
		
		
		ShareToWordPressMenuItem shareToWordPressMenuItem = new ShareToWordPressMenuItem(10000, 
				_resources.getString(WordPressResource.MENUITEM_SHARE_TO_WORDPRESS));

		
		//#ifndef IS_OS47_OR_ABOVE
		//add the global menu item
		ApplicationMenuItemRepository amir = ApplicationMenuItemRepository.getInstance();
		Log.trace(">>>Old menu items will be registered");
		amir.addMenuItem(ApplicationMenuItemRepository.MENUITEM_FILE_EXPLORER_BROWSE, shareToWordPressMenuItem, ApplicationDescriptor.currentApplicationDescriptor());
		amir.addMenuItem(ApplicationMenuItemRepository.MENUITEM_FILE_EXPLORER_ITEM, shareToWordPressMenuItem, ApplicationDescriptor.currentApplicationDescriptor());
		//amir.addMenuItem(ApplicationMenuItemRepository.MENUITEM_BROWSER, shareToWordPressMenuItem);
		//#endif
		
		Log.trace("<<< verifyRegistration");
	}
	
	public void addCHAPIListener() {
		try {
			server = Registry.getServer(CHAPI_CLASS_NAME);
			pending = server.getRequest(false); //check if a request is already there
			server.setListener(this);           //set the listener 
		} catch (ContentHandlerException e) {
			Log.error(e,"Error during SharingHelp init" );
		}
	}
	
	public void invocationRequestNotify(ContentHandlerServer handler) {
		Log.trace(">>> invocationRequestNotify");
		pending = handler.getRequest(false);
		if(pending != null) {
				processRequest();
		}
		Log.trace("<<< invocationRequestNotify");
	}
	
	public void checkPendingRequest() {
		if(pending != null) {
			processRequest();
		}
	}
	
	private void processRequest() {
		Log.trace(">>> processRequest");
		String filename = null;
		String type = null;
		
		synchronized (this) {
			filename = pending.getURL();
			type = pending.getType();				
			 
		}
		server.finish(pending, Invocation.OK);
			
		Screen scr = UiApplication.getUiApplication().getActiveScreen();
		//There is a dialog displayed within the app. do nothing in this case
		if (scr instanceof Dialog ){
			return;
		}				
		Vector applicationBlogs = WordPressCore.getInstance().getApplicationBlogs();
		if(applicationBlogs.size() == 0) {
			Dialog.alert( _resources.getString(WordPressResource.MESSAGE_SHARE_TO_WORDPRESS_NO_BLOGS));			
			return;
		}		
		int selection = -1;
		if(applicationBlogs.size() == 1) {
			selection = 0;
		} else {
			//choose a blog from the blog list. Excluded blogs in loading phase.
			Vector tmpBlogs = new Vector();
			
			for (int i = 0; i < applicationBlogs.size(); i++) {
				BlogInfo currentBlog = (BlogInfo) applicationBlogs.elementAt(i);
				if (currentBlog.getState() != BlogInfo.STATE_LOADING)
					tmpBlogs.addElement(currentBlog.getName());
			}

			String[] blogNames = new String[tmpBlogs.size()];
			tmpBlogs.copyInto(blogNames);
			String title = _resources.getString(WordPressResource.TITLE_BLOG_SELECTOR_POPUP);
			SelectorPopupScreen selScr = new SelectorPopupScreen(title, blogNames);
			selScr.pickBlog();
			selection = selScr.getSelectedBlog();
		}

		if(selection != -1) {
			
			//back to main screen - draft post will be auto-saved			
			while ((scr = UiApplication.getUiApplication().getActiveScreen()) != null){
				if (scr instanceof MainView) {		

					break;
				} else {
					UiApplication.getUiApplication().popScreen(scr);
				}
			}
			
			try {
				Blog selectedBlog =  BlogDAO.getBlog((BlogInfo)applicationBlogs.elementAt(selection));
				Post post = new Post(selectedBlog);
				MediaEntry mediaObj = null;
				String imageExtensions[] = MultimediaUtils.getSupportedWordPressImageFormat();
				String videoExtensions[] = MultimediaUtils.getSupportedWordPressVideoFormat();
				String audioExtensions[] = MultimediaUtils.getSupportedWordPressAudioFormat();
				
				if(filename != null && filename.startsWith("file://")) {
				
					for (int i = 0; i < audioExtensions.length; i++) {
						if (filename.endsWith(audioExtensions[i])) {
							mediaObj = new AudioEntry();
							break;
						}
					}

					if(mediaObj == null)
						for (int i = 0; i < videoExtensions.length; i++) {
							if (filename.endsWith(videoExtensions[i])) {
								mediaObj = new VideoEntry();
								break;
							}
						}

					if(mediaObj == null)
						for (int i = 0; i < imageExtensions.length; i++) {
							if (filename.endsWith(imageExtensions[i])) {
								mediaObj = new PhotoEntry();
								break;
							}
						}
				
					mediaObj.setFilePath(filename);
					Vector mediaObjs = new Vector();
					mediaObjs.addElement(mediaObj);
					post.setMediaObjects(mediaObjs);
				} else if(filename != null && type != null && type.startsWith("text/")) {
					post.setBody(filename);
				}
				
				FrontController.getIstance().showPost(post, true);
				
			} catch (Exception e) {
				Log.error(e, "Error while loading selected blog");
				Dialog.alert("Error while loading selected Blog");
			}				
		} 
	}
	
	
	private class ShareToWordPressMenuItem extends ApplicationMenuItem {
		
		private final String text; //string we want to use as the label of the menuItem
		private Registry registry;

		ShareToWordPressMenuItem(int order, String text){
			super(order);
			this.text = text;
			this.registry = Registry.getRegistry(getClass().getName());
		}
		
		//Run is called when the menuItem is invoked. KEEP IN MIND - YOU ARE IN THE CONTEXT OF CALLER
		public Object run(final Object context){
			Log.trace("ShareToWordPressMenuItem - RUN");
			if(context != null ) {
			/*	Invocation requ = new Invocation();
				requ.setURL(context.toString());
				requ.setID(CHAPI_ID);
				registry.invoke(requ);*/
			}
			return null;
		}
		
		//toString should return the string we want to use as the label of the menuItem
		public String toString(){
			return text;
		}
	}
	
}