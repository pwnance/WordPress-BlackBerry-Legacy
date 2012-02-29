//#preprocess
package com.wordpress.view;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import net.rim.blackberry.api.browser.URLEncodedPostData;
import net.rim.device.api.math.Fixed32;
import net.rim.device.api.system.ApplicationDescriptor;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.Characters;
import net.rim.device.api.system.CodeModuleGroup;
import net.rim.device.api.system.CodeModuleGroupManager;
import net.rim.device.api.system.Display;
import net.rim.device.api.system.EncodedImage;
import net.rim.device.api.system.GIFEncodedImage;
import net.rim.device.api.system.KeypadListener;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.DrawStyle;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.Ui;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.XYRect;
import net.rim.device.api.ui.component.BitmapField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.container.MainScreen;

import com.wordpress.bb.WordPressCore;
import com.wordpress.bb.WordPressInfo;
import com.wordpress.bb.WordPressResource;
import com.wordpress.controller.AccountsController;
import com.wordpress.controller.BaseController;
import com.wordpress.controller.FrontController;
import com.wordpress.controller.MainController;
import com.wordpress.controller.MediaLibrariesController;
import com.wordpress.controller.PagesController;
import com.wordpress.controller.PostsController;
import com.wordpress.controller.RecentCommentsController;
import com.wordpress.controller.StatsController;
import com.wordpress.io.BlogDAO;
import com.wordpress.io.CommentsDAO;
import com.wordpress.model.Blog;
import com.wordpress.model.BlogInfo;
import com.wordpress.model.Post;
import com.wordpress.model.Preferences;
import com.wordpress.task.StopConnTask;
import com.wordpress.utils.DataCollector;
import com.wordpress.utils.ImageManipulator;
import com.wordpress.utils.MultimediaUtils;
import com.wordpress.utils.StringUtils;
import com.wordpress.utils.Tools;
import com.wordpress.utils.log.Log;
import com.wordpress.utils.observer.Observable;
import com.wordpress.utils.observer.Observer;
import com.wordpress.view.component.AnimatedGIFField;
import com.wordpress.view.component.BlogSelectorField;
import com.wordpress.view.component.SelectorPopupScreen;
import com.wordpress.view.component.WelcomeField;
import com.wordpress.view.container.MainViewInternalFieldManager;
import com.wordpress.view.container.TableLayoutManager;
import com.wordpress.view.dialog.ConnectionInProgressView;

//#ifdef VER_4.7.0 | BlackBerrySDK5.0.0 | BlackBerrySDK6.0.0 | BlackBerrySDK7.0.0
import net.rim.device.api.ui.TouchGesture;
import net.rim.device.api.ui.TouchEvent;
//#endif

//#ifdef BlackBerrySDK6.0.0 | BlackBerrySDK7.0.0
import com.wordpress.quickphoto.QuickPhotoScreen;
import net.rim.device.api.ui.UiEngineInstance;
import net.rim.device.api.ui.TransitionContext;
//#endif

//#ifdef BlackBerrySDK7.0.0
import com.wordpress.view.reader.WPCOMReaderListView;
//#endif

import com.wordpress.xmlrpc.BlogConnResponse;
import com.wordpress.xmlrpc.BlogUpdateConn;

public class MainView extends BaseView {
	
    private MainController mainController = null;
    private BlogInfo currentBlog = null;
    private WelcomeField wft;
    
    private TableLayoutManager	blogSelectorRow;
    private BitmapField blogIconField;
    private BlogSelectorField blogSelectorField;
    private MainViewInternalFieldManager mainContentContainer;
    
    private TableLayoutManager actionsTable;
    final int actionsTableNumberOfRows = 3;
    final int actionsTableItemHorizontalPadding = 10; // Horizontal padding between items
	//Default icons used as blog's status
	private Bitmap imgImportant = Bitmap.getBitmapResource("important.png");
	private Bitmap wp_blue = Bitmap.getBitmapResource("wordpress-logo-100-blue.png");
	private Bitmap wp_grey = Bitmap.getBitmapResource("wordpress-logo-100-grey.png");
	private Bitmap pendingActivation = Bitmap.getBitmapResource("pending_activation.png"); //not used yet

	private final int mnuPosts = 100;
	private final int mnuPages = 110;
	private final int mnuComments = 120;
	private final int mnuMedia = 130;
	private final int mnuStats = 140;
	private final int mnuSettings = 150;
	private final int mnuRefresh = 160;
	private final int mnuDashboard = 170;
	private final int mnuReader = 180;
	private final int mnuNewPhoto = 190;

	public MainView(MainController mainController) {
		super( "WordPress", MainScreen.NO_VERTICAL_SCROLL | Manager.NO_HORIZONTAL_SCROLL | USE_ALL_HEIGHT);
		this.mainController = mainController;
     
		BlogInfo[] blogCaricati = mainController.getApplicationBlogs();
		if( blogCaricati.length > 0 ) {
			createTableAndSelector ( blogCaricati );
		} else {
			currentBlog = null;
			wft = new WelcomeField();
			wft.setToolbarHeight(titleField.getPreferredHeight());
			add(wft);
		}
        addMenuItem(_feedbackItem);
        addMenuItem(_bugReportItem);
        addMenuItem(_aboutItem);
		addMenuItem(_addBlogItem);
		addMenuItem(_appSettingsItem);
		addMenuItem(_accountItem);
		addMenuItem(_updateItem);
		
		mainController.bumpScreenViewStats("com/wordpress/view/MainView", "MainView Screen", "", null, "");
	}
	
	private void createTableAndSelector( BlogInfo[] blogCaricati ) {
		String choices[] = new String[ blogCaricati.length ];
		for (int i=0; i< blogCaricati.length; i++) {
			BlogInfo currentRow = (BlogInfo) blogCaricati[i];
			String blogName = currentRow.getName();
			choices[i]= blogName;
		}
		int iSetTo = 0;
		currentBlog = blogCaricati[ iSetTo ];
		
		blogSelectorRow = new TableLayoutManager(
				new int[] {
						TableLayoutManager.USE_PREFERRED_SIZE,
						TableLayoutManager.SPLIT_REMAINING_WIDTH
				}, 
				new int[] { 2, 2 },
				10,
				Manager.USE_ALL_WIDTH | Manager.FIELD_HCENTER
		);
		
		blogIconField = createBlogIconField( blogCaricati[iSetTo] );
		blogSelectorRow.add( blogIconField );
		blogSelectorField = new BlogSelectorField( choices, iSetTo, FOCUSABLE | USE_ALL_WIDTH );
		blogSelectorField.setFieldMaxHeight( getBlogIconSize() ); //set the field with the same height of the icon
		blogSelectorField.setChangeListener(new BlogSelectorChangeListener());
		blogSelectorRow.add( blogSelectorField );
		//#ifdef BlackBerrySDK4.5.0
		blogSelectorRow.setMargin(5, 0, 0, 0);
		blogIconField.setMargin(0, 0, 0, 5);
		blogSelectorField.setMargin(0, 5, 0, 0);
        //#else
        blogSelectorRow.setMargin(5, 5, 0, 5);
        //#endif
		
		actionsTable = new TableLayoutManager(
				new int[] {
						TableLayoutManager.SPLIT_REMAINING_WIDTH,
						TableLayoutManager.SPLIT_REMAINING_WIDTH,
						TableLayoutManager.SPLIT_REMAINING_WIDTH,
				}, 
				new int[] { 2, 2, 2 }, //not used in this configuration
				actionsTableItemHorizontalPadding,
				Manager.USE_ALL_WIDTH
		);
		actionsTable.setMargin(5, 5, 5, 5);
		
		//#ifdef BlackBerrySDK6.0.0 | BlackBerrySDK7.0.0
		if( MultimediaUtils.isPhotoCaptureSupported() )
			actionsTable.add( new ActionTableItem( mnuNewPhoto,  getItemLabel(mnuNewPhoto), mnuNewPhoto ) );
		//#endif		
		
		actionsTable.add( new ActionTableItem( mnuPosts, getItemLabel(mnuPosts), mnuPosts ) );
		actionsTable.add( new ActionTableItem( mnuComments, getItemLabel(mnuComments), mnuComments ) );
		actionsTable.add( new ActionTableItem( mnuPages, getItemLabel(mnuPages), mnuPages ) );
		actionsTable.add( new ActionTableItem( mnuStats,  getItemLabel(mnuStats), mnuStats ) );	
		actionsTable.add( new ActionTableItem( mnuSettings, getItemLabel(mnuSettings), mnuSettings ) );
		actionsTable.add( new ActionTableItem( mnuDashboard, getItemLabel(mnuDashboard), mnuDashboard ) );
		actionsTable.add( new ActionTableItem( mnuRefresh, getItemLabel(mnuRefresh), mnuRefresh ) );
		
		//#ifdef BlackBerrySDK7.0.0
		if ( currentBlog != null && currentBlog.isWPCOMBlog() )
			actionsTable.add( new ActionTableItem( mnuReader, getItemLabel(mnuReader), mnuReader ) );
		//#endif
		
		mainContentContainer = new MainViewInternalFieldManager(blogSelectorRow, actionsTable, true);
		add( mainContentContainer );
	}
	
	/**
	 * 
	 * This method returns the height of the Blog Icon we use in the top left of the screen. 
	 * It is also called when the app asks the blavatar to the Gravatar service during the update call.
	 * 
	 * Pearl 8220 - 240 x 320 pixels
	 * Curve 8300 Series, 8800 Series, 8700 Series - 320 x 240 pixels
	 * Curve 8350i - 320 x 240 pixels
	 * Curve 8900 - 480 x 360 pixels
	 * Bold  9000 Series - 480 x 320 pixels
	 * Bold  9900 Series - 640 x 480
	 * Tour  9600 Series - 480 x 360 pixels
	 * Storm 9500 Series - portrait view: 360 x 480 pixels,  landscape view: 480 x 360 pixels
	 * Torch 9800 360 x 480 (portrait) when held vertically.
	 * Torch 9810 - 640 x 480
	 * Torch2 9850/9860  - 800 x 480
	 * 
	 */
	public static int getBlogIconSize() {
		 int height = Display.getHeight();
		 if( height == 240 ) {
			 return 32;
		 } else if(  height == 320 ) { 
			 return 48;
		 } else if( height == 360 ) {
			 return 48;
		 } else if( height == 480 ) {
			 return 64;		 
		 } else if( height == 640 ){
			 return 72;
		 } else if( height > 640 ) { 
			 return 92;
		 }
		 return 32;
	}
	
	private String getItemLabel(int type) {
		switch (type) {
		case (mnuPosts):
			return _resources.getString(WordPressResource.BUTTON_POSTS);
		case (mnuPages):
			return _resources.getString(WordPressResource.BUTTON_PAGES);
		case (mnuComments):
			return _resources.getString(WordPressResource.BUTTON_COMMENTS);
		case (mnuMedia):
			return _resources.getString(WordPressResource.BUTTON_MEDIA);
		case (mnuStats):
			return _resources.getString(WordPressResource.BUTTON_STATS);
		case (mnuSettings):
			return _resources.getString(WordPressResource.BUTTON_SETTINGS);
		case (mnuRefresh):
			return _resources.getString(WordPressResource.BUTTON_REFRESH_BLOG);
		case (mnuDashboard):
			return _resources.getString(WordPressResource.MENUITEM_DASHBOARD);
		case (mnuReader):
			return _resources.getString(WordPressResource.MENUITEM_READER);
		case (mnuNewPhoto):
			return _resources.getString(WordPressResource.MENUITEM_QUICKPHOTO);
		default:
			return null;
		}
	}
	
	private boolean tableOrMenuItemSelected(int selection) {
		if ( currentBlog == null ) return true;

		if (currentBlog.getState() == BlogInfo.STATE_LOADING || currentBlog.getState() == BlogInfo.STATE_ADDED_TO_QUEUE) {
			mainController.displayMessage(_resources.getString(WordPressResource.MESSAGE_LOADING_BLOGS));
			return true;
		} 

		Blog tmpblog = null;
		try {
			tmpblog = BlogDAO.getBlog(currentBlog);
		} catch (Exception e) {
			mainController.displayErrorAndWait(e, "Can't load blog data ");
			return true;
		}

		switch (selection) {
		case mnuPosts:
			final PostsController ctrl = new PostsController(tmpblog);
			ctrl.showView();
			break;
		case mnuPages:
			PagesController pctrl = new PagesController(tmpblog);
			pctrl.showView();
			break;
		case mnuComments:
			RecentCommentsController cctrl=new RecentCommentsController(tmpblog);
			cctrl.showView();
			break;
		case mnuStats:
			StatsController sctrl = new StatsController(tmpblog);
			sctrl.showView();
			break;
		case mnuSettings:
			FrontController.getIstance().showBlogOptions(tmpblog);
			break;
		case mnuRefresh:
			final BlogUpdateConn connection = new BlogUpdateConn (tmpblog);       
			ConnectionInProgressView connectionProgressView = new ConnectionInProgressView(
					_resources.getString(WordPressResource.CONNECTION_INPROGRESS));
			connection.addObserver(new RefreshBlogCallBack(connectionProgressView)); 
			connection.startConnWork(); //starts connection
			int choice = connectionProgressView.doModal();
			if(choice == Dialog.CANCEL) {
				WordPressCore.getInstance().getTasksRunner().enqueue(new StopConnTask(connection));
			}
			break;
		case mnuDashboard:
			String user = currentBlog.getUsername();
			String pass = currentBlog.getPassword();

			String cleanURL = currentBlog.getXmlRpcUrl().endsWith("/") ? currentBlog.getXmlRpcUrl() :  currentBlog.getXmlRpcUrl()+"/";
			String loginURL = StringUtils.replaceLast(cleanURL,"/xmlrpc.php/", "/wp-login.php");
			String dashboardURL = StringUtils.replaceLast(cleanURL,"/xmlrpc.php/", "/wp-admin/");

			//create the link
			URLEncodedPostData urlEncoder = new URLEncodedPostData("UTF-8", false);

			urlEncoder.append("redirect_to", dashboardURL);
			urlEncoder.append("log", user);
			urlEncoder.append("pwd", pass);

			Tools.openNativeBrowser(loginURL, "WordPress for BlackBerry App", null, urlEncoder);
			break;
		case mnuMedia:
			try {
				MediaLibrariesController mctrl = new MediaLibrariesController(BlogDAO.getBlog(currentBlog));
				mctrl.showView();
			} catch (Exception e) {
				mainController.displayError(e, "Cannot load the blog data");
			}
			break;
			//#ifdef BlackBerrySDK6.0.0 | BlackBerrySDK7.0.0
		case mnuNewPhoto:
			try {
				Post quikPost = new Post(BlogDAO.getBlog(currentBlog));
				QuickPhotoScreen quikScreen = new QuickPhotoScreen(quikPost);
				UiEngineInstance engine = Ui.getUiEngineInstance();
				TransitionContext transitionContextIn;
				transitionContextIn = new TransitionContext(TransitionContext.TRANSITION_SLIDE);
				transitionContextIn.setIntAttribute(TransitionContext.ATTR_DURATION, 750);
				transitionContextIn.setIntAttribute(TransitionContext.ATTR_DIRECTION, TransitionContext.DIRECTION_LEFT);   
				engine.setTransition(this, quikScreen, UiEngineInstance.TRIGGER_PUSH, transitionContextIn);
				UiApplication.getUiApplication().pushScreen(quikScreen);
			} catch (Exception e) {
				mainController.displayError(e, "Cannot load the blog data");
			}
			break;
			//#endif 
			//#ifdef BlackBerrySDK7.0.0
		case mnuReader:
			//load the first WP.COM available within the app
			Hashtable applicationAccounts = MainController.getIstance().getApplicationAccounts();
			Hashtable currentAccount = null;
			Enumeration k = applicationAccounts.keys();
			if (k.hasMoreElements()) {
				String key = (String) k.nextElement();
				currentAccount = (Hashtable)applicationAccounts.get(key);
			}
			String user2 = (String)currentAccount.get("username");
			String pass2 = (String)currentAccount.get("passwd");
			WPCOMReaderListView _browserScreen = new WPCOMReaderListView(user2, pass2);
			UiEngineInstance engine = Ui.getUiEngineInstance();
			TransitionContext transitionContextIn;
			transitionContextIn = new TransitionContext(TransitionContext.TRANSITION_SLIDE);
			transitionContextIn.setIntAttribute(TransitionContext.ATTR_DURATION, 750);
			transitionContextIn.setIntAttribute(TransitionContext.ATTR_DIRECTION, TransitionContext.DIRECTION_LEFT);   
			engine.setTransition(this, _browserScreen, UiEngineInstance.TRIGGER_PUSH, transitionContextIn);
			UiApplication.getUiApplication().pushScreen(_browserScreen); 
			break;
			//#endif

		default:
			break;
		}
		return true;
	}
	
	 private class BlogSelectorChangeListener implements FieldChangeListener {

		 public void fieldChanged(Field field, int context) {
			 if ( context == 0 ) {

				 if( blogSelectorField.getChoices().length == 1 ) {
					 //One blog only, open the blog in the browser
					 String user = currentBlog.getUsername();
					 String pass = currentBlog.getPassword();
					 String cleanURL = currentBlog.getXmlRpcUrl().endsWith("/") ? currentBlog.getXmlRpcUrl() :  currentBlog.getXmlRpcUrl()+"/";
					 String loginURL = StringUtils.replaceLast(cleanURL,"/xmlrpc.php/", "/wp-login.php");
					 //create the link
					 URLEncodedPostData urlEncoder = new URLEncodedPostData("UTF-8", false);

					 urlEncoder.append("redirect_to", currentBlog.getBlogURL());
					 urlEncoder.append("log", user);
					 urlEncoder.append("pwd", pass);

					 Tools.openNativeBrowser(loginURL, "WordPress for BlackBerry App", null, urlEncoder);
				 } else {
					 final SelectorPopupScreen selScr = new SelectorPopupScreen( _resources.getString(WordPressResource.TITLE_BLOG_SELECTOR_POPUP), blogSelectorField.getChoices());
					 UiApplication.getUiApplication().invokeAndWait(new Runnable() {
						 public void run() {
							 selScr.pickItem();
						 }
					 });
					 int selectedIndex = selScr.getSelectedItem();
					 if ( selectedIndex != -1 ) {
						 currentBlog = mainController.getApplicationBlogs()[selectedIndex];
						 updateBlogIconField(currentBlog);
						 blogSelectorField.setSelectedIndex(selectedIndex);
						 actionsTable.invalidate();
					 }

				 }
				 
			 }
		 }
	 }
	
	 	 
	public void refreshMainView() {
		synchronized (this) {
			BlogInfo[] blogCaricati = mainController.getApplicationBlogs();
			if( blogCaricati.length > 0 ) {
				if( wft != null ) {
					delete(wft);
					createTableAndSelector(blogCaricati);
					wft = null;
				} else {
					if ( currentBlog == null )
						currentBlog = blogCaricati[0];
					final String choices[] = new String[ blogCaricati.length ];
					int iSetTo = 0;
					for (int i=0; i< blogCaricati.length; i++) {
						BlogInfo currentRow = (BlogInfo) blogCaricati[i];
						String blogName = currentRow.getName();
						choices[i]= blogName;
						if ( currentBlog != null && currentBlog.equals(currentRow) )
							iSetTo = i;
					}
					
					final int sel = iSetTo;
					UiApplication.getUiApplication().invokeLater(new Runnable() {
						public void run() {
							blogSelectorField.setChoices(choices);
							blogSelectorField.setSelectedIndex(sel);
							updateBlogIconField(currentBlog);
							actionsTable.invalidate();
						}
					});
				}
			} else {
				currentBlog = null;
				if( wft != null ) {
					//welcome screen already on the stack
				} else {
					wft = new WelcomeField();
					wft.setToolbarHeight(titleField.getPreferredHeight());
					if ( UiApplication.getUiApplication().isEventThread() ) {
						if ( blogSelectorRow.getManager() != null ) {
							delete(mainContentContainer);
							blogSelectorRow = null;
							actionsTable = null;
						}
						add(wft);
					} else {
						UiApplication.getUiApplication().invokeLater(new Runnable() {
							public void run() {
								if ( blogSelectorRow.getManager() != null ) {
									delete(mainContentContainer);
									blogSelectorRow = null;
									actionsTable = null;
								}
								add(wft);
							}
						});
					}
				}
			}
		}
	}
	
	private void updateBlogIconField(BlogInfo blogToUpdate){
		BitmapField blogIconField2 = createBlogIconField( blogToUpdate );
		//#ifdef BlackBerrySDK4.5.0
		blogIconField2.setMargin(0, 0, 0, 5);
        //#endif
		blogSelectorRow.replace(blogIconField, blogIconField2);
		blogIconField = blogIconField2;
		blogSelectorRow.invalidate();
	}
	
	 private BitmapField createBlogIconField( BlogInfo currentRow ){
		 int stato = currentRow.getState();
		 int maxIconWidth = getBlogIconSize();
		 Bitmap icon = null;
		 if(stato == BlogInfo.STATE_PENDING_ACTIVATION) {
			 icon = pendingActivation;
		 } else if ( stato == BlogInfo.STATE_LOADING || stato == BlogInfo.STATE_ADDED_TO_QUEUE ) { 
			GIFEncodedImage _theImage= (GIFEncodedImage)EncodedImage.getEncodedImageResource("loading-blog-gif.bin");
			AnimatedGIFField animatedBitmapField = new AnimatedGIFField(_theImage, Field.NON_FOCUSABLE | FIELD_HCENTER | FIELD_VCENTER ); 
			if( _theImage.getWidth() < maxIconWidth ) { //we know that the loading div is small
				int mar = ( maxIconWidth - _theImage.getWidth() ) / 2; 
				animatedBitmapField.setMargin(mar, mar, mar, mar);
			}
			return animatedBitmapField;
		 } else if (stato == BlogInfo.STATE_LOADED_WITH_ERROR ||  stato == BlogInfo.STATE_ERROR) {
			 icon = imgImportant;
		 } else if( stato == BlogInfo.STATE_LOADED ) {
			 if(currentRow.getBlogIcon() != null) {
				 try {
					 icon =  Bitmap.createBitmapFromPNG(currentRow.getBlogIcon(), 0, -1);
					// BitmapField  test = new BitmapField( icon, Field.NON_FOCUSABLE | FIELD_HCENTER | FIELD_VCENTER );
					// test.setBorder(BorderFactory.createRoundedBorder(new XYEdges(6,6,6,6)));
					// return test;
				 } catch (Exception e) {
					 Log.error("no valid shortcut ico found in the blog obj");
				 }
			 }
			 //still null there was an error during img generation process
			 if( icon == null) {
				 if(currentRow.isWPCOMBlog()) {
					 icon = wp_blue;
				 } else {
					 icon = wp_grey;
				 }
			 }
		 } 
		 
		 if( icon.getWidth() != maxIconWidth ) {
			// Calculate the new scale based on the region sizes
				// Scale / Zoom
				// 0.1 = 1000%
				// 0.5 = 200%
				// 1 = 100%
				// 2 = 50%
				// 4 = 25%
			int	resultantScaleX = Fixed32.div(Fixed32.toFP(maxIconWidth), Fixed32.toFP(icon.getWidth()));
			icon = ImageManipulator.scale(icon, resultantScaleX);
		 }
	 
		 return new BitmapField( icon, Field.NON_FOCUSABLE | FIELD_HCENTER | FIELD_VCENTER );
	 }
	 	 
	 //update the view of blog list entry
	 public synchronized void setBlogItemViewState(BlogInfo blogInfo) {
		 //if ( blogIconField == null) return;
		 if ( currentBlog != null && currentBlog.equals(blogInfo)) {
			 currentBlog = blogInfo;
			 final BitmapField blogIconField2 = createBlogIconField( blogInfo );
			//#ifdef BlackBerrySDK4.5.0
				blogIconField2.setMargin(0, 0, 0, 5);
	        //#endif
			 UiApplication.getUiApplication().invokeLater(new Runnable() {
				 public void run() {
					 blogSelectorRow.replace(blogIconField, blogIconField2);
					 blogIconField = blogIconField2;
					 blogSelectorRow.invalidate();
				 }
			 });
		 }
	 }

	 private MenuItem _showBlogPosts = new MenuItem( _resources, WordPressResource.BUTTON_POSTS, 1300, 900) {
		 public void run() {
			 if ( currentBlog == null ) return;
			 tableOrMenuItemSelected(mnuPosts);
		 }
	 };
	 private MenuItem _showBlogPages = new MenuItem( _resources, WordPressResource.BUTTON_PAGES, 1310, 900) {
		 public void run() {
			 if ( currentBlog == null ) return;
			 tableOrMenuItemSelected(mnuPages);
		 }
	 };
	 private MenuItem _showBlogComments = new MenuItem( _resources, WordPressResource.BUTTON_COMMENTS, 1320, 900) {
		 public void run() {
			 if ( currentBlog == null ) return;
			 tableOrMenuItemSelected(mnuStats);
		 }
	 };
	 private MenuItem _showBlogMedia = new MenuItem( _resources, WordPressResource.BUTTON_MEDIA, 1325, 900) {
		 public void run() {
			 if ( currentBlog == null ) return; 
			 tableOrMenuItemSelected(mnuMedia);
		 }
	 };
	 
	 private MenuItem _showBlogStats = new MenuItem( _resources, WordPressResource.BUTTON_STATS, 1330, 900) {
		 public void run() {
			 if ( currentBlog == null ) return; 
			 tableOrMenuItemSelected(mnuStats);
		 }
	 };
	 private MenuItem _showBlogSettings = new MenuItem( _resources, WordPressResource.BUTTON_SETTINGS, 1340, 900) {
		 public void run() {
			 if ( currentBlog == null ) return;
			 tableOrMenuItemSelected(mnuSettings);
		 }
	 };
	 private MenuItem _refreshBlog = new MenuItem( _resources, WordPressResource.BUTTON_REFRESH_BLOG, 1350, 900) {
		 public void run() {
			 if ( currentBlog == null ) return; 
			 tableOrMenuItemSelected(mnuRefresh);
		 }
	 };
	 private MenuItem _showBlogDashBoard = new MenuItem( _resources, WordPressResource.MENUITEM_DASHBOARD, 1360, 900) {
		 public void run() {
			 if ( currentBlog == null ) return; 
			 tableOrMenuItemSelected(mnuDashboard);
		 }
	 };
	 
	 //#ifdef BlackBerrySDK7.0.0
	 private MenuItem _mobileReaderMenuItem = new MenuItem( _resources, WordPressResource.MENUITEM_READER, 1370, 900) {
		 public void run() {
			 tableOrMenuItemSelected(mnuReader);
		 }
	 }; 
	 //#endif
	 
   
    //add blog menu item 
    private MenuItem _addBlogItem = new MenuItem( _resources, WordPressResource.MENUITEM_ADDBLOG, 100000, 1000) {
    	public void run() {
    		if(mainController.isLoadingBlogs()) {
    			mainController.displayMessage(_resources.getString(WordPressResource.MESSAGE_LOADING_BLOGS));
				return;
    		}
    		mainController.showWelcomeView();
    	}
    };
        
    private MenuItem _deleteBlogItem = new MenuItem( _resources, WordPressResource.MENUITEM_DELETE_BLOG, 100100, 1000) {
    	public void run() {
    		if ( currentBlog == null ) return;
    		if (currentBlog.getState() == BlogInfo.STATE_LOADING || currentBlog.getState() == BlogInfo.STATE_ADDED_TO_QUEUE) {
    			mainController.displayMessage(_resources.getString(WordPressResource.MESSAGE_LOADING_BLOGS));
    		} else {
    			mainController.deleteBlog(currentBlog);	       
    			currentBlog = null;
    			refreshMainView();
    		}
    	}
    };

    private MenuItem _notificationItem = new MenuItem( _resources, WordPressResource.MENUITEM_NOTIFICATIONS, 100200, 1000) {
        public void run() {
        	BlogInfo[] blogs = mainController.getApplicationBlogs();
        	FrontController.getIstance().showNotificationView(blogs);
        }
    };
   
    
    private MenuItem _appSettingsItem = new MenuItem( _resources, WordPressResource.MENUITEM_SETUP, 100300, 1000) {
        public void run() {
        	FrontController.getIstance().showSetupView();
        }
    };
    
    private MenuItem _accountItem = new MenuItem( _resources, WordPressResource.MENUITEM_ACCOUNTS, 100400, 1000) {
        public void run() {
        	AccountsController ctrl = new AccountsController();
    		ctrl.showView();	
        }
    };

    private MenuItem _updateItem = new MenuItem( _resources, WordPressResource.MENUITEM_CHECKUPDATE, 100500, 1000) {
    	public void run() {

    		try {
    			DataCollector dtc = new DataCollector();
    			int numBlogs = 0;
    			numBlogs = mainController.getApplicationBlogs().length;
    			dtc.checkForUpdate(numBlogs); //start data gathering here
    		} catch (Exception e) {
    			mainController.displayError(e, "Error while checking for new versions.");
    		}
    	}
    };
    
    private MenuItem _feedbackItem = new MenuItem( _resources, WordPressResource.MENUITEM_FEEDBACK, 200200, 1000) {
        public void run() {
        	try {
        		 // Pull out the App World data from the CodeModuleGroup
                String myAppName = ApplicationDescriptor.currentApplicationDescriptor().getName();
                CodeModuleGroup group = CodeModuleGroupManager.load( myAppName );
                final String myContentId = group == null ? "" : group.getProperty( "RIM_APP_WORLD_ID" );

                //if App World data is null or empty string, put the id manually
                if(myContentId != null && !myContentId.trim().equalsIgnoreCase("")){
                	Tools.openAppWorld(myContentId);
                } else {
                	Tools.openAppWorld("5802"); //id of the App given by RIM
                }
                
			} catch (Exception e) {
				Log.error(e, "Problem invoking BlackBerry App World");
				mainController.displayError("Problem invoking BlackBerry App World");
			}
        }
    };
    
    private MenuItem _bugReportItem = new MenuItem( _resources, WordPressResource.MENUITEM_BUG_REPORT, 200300, 1000) {
    	public void run() {
    		int selection = -1;
    		String[] messages = _resources.getStringArray(WordPressResource.MESSAGES_ADD_BLOG);
    		String[] blogNames = new String[]{messages[1], messages[2]};
    		String title = _resources.getString(WordPressResource.MESSAGE_WORDPRESS_VERSION);
    		SelectorPopupScreen selScr = new SelectorPopupScreen(title, blogNames);
    		selScr.pickItem();
    		selection = selScr.getSelectedItem();
    		if(selection == 0) {
    			ContactSupportView view = new ContactSupportView(mainController, false);
    			UiApplication.getUiApplication().pushScreen(view);
    		} else if(selection == 1) {
    			ContactSupportView view = new ContactSupportView(mainController, true);
    			UiApplication.getUiApplication().pushScreen(view);
    		}
    	}
    };

    private MenuItem _aboutItem = new MenuItem( _resources, WordPressResource.MENUITEM_ABOUT, 200400, 1000) {
        public void run() {
        	FrontController.getIstance().showAboutView();
        }
    };
        
    /*
     * used when background on close is activated
     */
    private MenuItem _exitItem = new MenuItem( _resources, WordPressResource.MENUITEM_EXIT, 300000, 2000) {
        public void run() {
        	WordPressCore.getInstance().exitWordPress();
        }
    };
       
    
    public void paint(Graphics graphics)
	 {
		 graphics.setBackgroundColor(0xefebef);
		 graphics.clear();
		 super.paint(graphics);
	 }
    
    //Override the makeMenu method so we can add a custom menu item
    protected void makeMenu(Menu menu, int instance)
    {
    	
    	if ( instance == Menu.INSTANCE_CONTEXT ) return; // Do not show the popup menu on this screen

    	if( currentBlog != null ) {
    		menu.add(_showBlogPosts);
    		menu.add(_showBlogPages);
    		menu.add(_showBlogComments);
    		menu.add(_showBlogStats);
    		menu.add(_showBlogMedia);
    		menu.add(_refreshBlog);
    		menu.add(_showBlogSettings);
    		menu.add(_showBlogDashBoard);
    		menu.add(_notificationItem);
    		menu.add(_deleteBlogItem);
    		
        	//#ifdef BlackBerrySDK7.0.0
        	
        	//show the reader menu item when there are WP.com accounts
        	Hashtable applicationAccounts = MainController.getIstance().getApplicationAccounts();
        	if(applicationAccounts != null && applicationAccounts.size() > 0 && currentBlog.isWPCOMBlog() )
        		menu.add(_mobileReaderMenuItem);
        	
        	//#endif
    	}
    	
    	if(Preferences.getIstance().isBackgroundOnClose())
    		menu.add(_exitItem);
    	
    	//add the check for activation menu item if the blog is on pending state
    	/*if(blogListController != null) {
    		BlogInfo blogSelected = blogListController.getBlogSelected();
    		if(blogSelected.getState() == BlogInfo.STATE_PENDING_ACTIVATION)
    			menu.add(_activateBlogItem);
    	}*/
    	
        //Create the default menu.
        super.makeMenu(menu, instance);
    }
    
    //override onClose() to display a dialog box when the application 
    //menu close is selected or return btn is hitted    
	public boolean onClose() {
		Log.trace ("public boolean onClose()...");
    	return mainController.exitApp();
    }
 
	public BaseController getController() {
		return mainController;
	}
	
	
	private class RefreshBlogCallBack implements Observer {

		private ConnectionInProgressView connectionProgressView;

		public RefreshBlogCallBack(ConnectionInProgressView connectionProgressView) {
			super();
			this.connectionProgressView = connectionProgressView;
		}

		public void update(Observable observable, final Object object) {

			Log.trace(">>>Refreshing Blog Response");
			
			UiApplication.getUiApplication().invokeLater(new Runnable() {
				public void run() {
					if ( connectionProgressView.isDisplayed())
						UiApplication.getUiApplication().popScreen(connectionProgressView);
					connectionProgressView = null;
				} //end run
			});

			BlogConnResponse resp= (BlogConnResponse) object;

			if(resp.isStopped()){
				return;
			}
			
			Blog responseBlog = null;

			if( ! resp.isError() ) {
				try{
					responseBlog = (Blog) resp.getResponseObject(); 	
					responseBlog.setLoadingState(BlogInfo.STATE_LOADED);
					BlogDAO.updateBlog(responseBlog);							
					CommentsDAO.cleanGravatarCache(responseBlog);
				} catch (final Exception e) {
					mainController.displayErrorAndWait(e,"Error while storing the blog data");
				}
			} else {
				//something went wrong load the full blog info
				try {
					responseBlog = BlogDAO.getBlog(currentBlog);
				} catch (Exception e1) {
					mainController.displayErrorAndWait(e1,"Error while storing the blog data");
					return;
				}
				responseBlog.setLoadingState(BlogInfo.STATE_ERROR);
				final String respMessage = resp.getResponse();
				mainController.displayError(respMessage);
				try {
					BlogDAO.updateBlog(responseBlog);
				} catch (Exception e) {
					mainController.displayErrorAndWait(e,"Error while storing the blog data");	
				}
			}

			//update app blog
			WordPressCore wpCore = WordPressCore.getInstance();
			Vector applicationBlogs = wpCore.getApplicationBlogs();
			
			//update application blogs
			final BlogInfo currentBlogI = new BlogInfo(responseBlog);
			for(int count = 0; count < applicationBlogs.size(); ++count)
			{
				BlogInfo applicationBlogTmp = (BlogInfo)applicationBlogs.elementAt(count);
				if (applicationBlogTmp.equals(currentBlogI) )		
				{
					applicationBlogs.setElementAt(currentBlogI, count);
					break;
				}
			}
			
			currentBlog =  currentBlogI;
			//update the icon here and text here
			UiApplication.getUiApplication().invokeLater(new Runnable() {
				public void run() {
					refreshMainView();
				}
			});
		}//end update
	}
	
	private class ActionTableItem extends BitmapField {

		private final int bitmapType;
		private final String label;
		private final int menuIndex;
		private final int PADDING = 5;
		private final int VPADDING_IMAGE_TEXT = 0;

		private Bitmap bmp;
		Font myFont = null;

		private int fieldWidth = 0;
		private int fieldHeight = 0;
		private int bitmapWidth , bitmapHeight;
		private int labelAdvice;

		private boolean focusableFlag = true;
		
		public ActionTableItem(int bitmapType, String text, int menuIndex)
		{
			super(Bitmap.getBitmapResource("folder_yellow_open"), Field.FOCUSABLE | FIELD_HCENTER | FIELD_VCENTER | USE_ALL_WIDTH );
			this.bitmapType = bitmapType;
			this.label = text;
			this.menuIndex = menuIndex;
					
			myFont = Font.getDefault().derive(Font.PLAIN);
			labelAdvice = myFont.getAdvance(label);
		}
		
		public int getPreferredWidth() {
			return fieldWidth;
		}
		
		public int getPreferredHeight() {
			return fieldHeight;
		}
		
		public boolean isFocusable() {
			return focusableFlag;
		}
		
		protected void drawFocus( Graphics graphics, boolean on ) 
		{
			if (on) {
				int prevColor = graphics.getColor();
				try {
					XYRect rect = new XYRect();
					getFocusRect(rect);
					graphics.setColor(GUIFactory.BTN_COLOUR_BACKGROUND_FOCUS);
					graphics.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 20, 20); 
				} finally {
					graphics.setColor(prevColor);
				}
			} else {
				super.drawFocus(graphics, on);
			}
			paint(graphics);
		}

		protected void paint(Graphics graphics) {
			if( bitmapType == mnuReader &&  currentBlog != null && ! currentBlog.isWPCOMBlog() ) {
				focusableFlag = false;
				return;
			}
			
			focusableFlag = true;

			String currentLbl = label;
			if( bitmapType == mnuComments && currentBlog.getAwaitingModeration() > 0 ) {
				currentLbl = "(" + currentBlog.getAwaitingModeration() + ") " + currentLbl; 
				labelAdvice = myFont.getAdvance(currentLbl);
			}
			
			int availableWidthForChildFields = fieldWidth - ( 2 * PADDING ); //Do not use all the width available. see findBitmapSizeThatFits.
			//int availableHeightForChildFields = fieldHeight - ( 2 * PADDING );
			
			int imageHeight = getAvailableHeightForTheIcon();
			int textMaxHeight = getAvailableHeightForText();
			int totalHeightOfItems = imageHeight + VPADDING_IMAGE_TEXT + textMaxHeight; //ICON + PADDING + TEXT SPACE
								
			int xOffset = ( fieldWidth - bitmapWidth ) / 2 ;
			int yOffset = ( fieldHeight - totalHeightOfItems ) / 2;
			graphics.drawBitmap(xOffset, yOffset,  bitmapWidth, bitmapHeight, bmp, 0, 0);

			int prevColor = graphics.getColor();
			try {
				if ( isFocus() || graphics.isDrawingStyleSet( Graphics.DRAWSTYLE_FOCUS ) )
					graphics.setColor( Color.WHITE );
				else
					graphics.setColor(Color.BLACK);
				graphics.setFont(myFont);
				
				while ( availableWidthForChildFields < myFont.getAdvance(currentLbl) ) {
					myFont = myFont.derive( myFont.getStyle(),  myFont.getHeight() - 1 ); 
				}
				while (  myFont.getHeight() > textMaxHeight ) {
					myFont = myFont.derive( myFont.getStyle(),  myFont.getHeight() - 1 ); 
				}
				labelAdvice = myFont.getAdvance(currentLbl);
				
				xOffset = ( fieldWidth - labelAdvice ) / 2 ;		
				if ( xOffset < 0 ) xOffset = 0;
				yOffset =  yOffset + bitmapHeight + VPADDING_IMAGE_TEXT;
				
				graphics.drawText( currentLbl, xOffset, yOffset, DrawStyle.ELLIPSIS | DrawStyle.TOP, availableWidthForChildFields );

			} finally {
				graphics.setColor(prevColor);
			}
		}

		
		private int getAvailableHeightForText() {
			if ( fieldHeight == 0 ) return 0;
			
			int textMaxH = Math.min(fieldHeight, fieldWidth) - ( PADDING * 2 );
			textMaxH = textMaxH  / 3; // the text is 1/3 of the remaining space minus the V_PADDING
			return textMaxH - VPADDING_IMAGE_TEXT;
		}

		
		private int getAvailableHeightForTheIcon() {
			if ( fieldHeight == 0 ) return 0;
			
			int imageHeight = Math.min(fieldHeight, fieldWidth) - ( PADDING * 2 );
			imageHeight = ( imageHeight * 2 ) / 3; // the icon is 2/3 of the remaining space
			return imageHeight;
		}
		
		protected void layout(int width, int height) {
			fieldWidth = width;
			fieldHeight = mainContentContainer.getHeightAvailableForTheGrid() / actionsTableNumberOfRows;
			int imageHeight = getAvailableHeightForTheIcon();
			bmp = this.getBitmapz(imageHeight);
			this.setBitmap(bmp); //just to make sure...
			bitmapWidth = bmp.getWidth();
			bitmapHeight = bmp.getHeight();
			super.layout(width, fieldHeight);
		}
				
		private Bitmap getBitmapz(int imageHeight) {
			int sizePrefix = 0;

			if ( imageHeight <=  64 )  
				sizePrefix = 64;
			else 
				sizePrefix = 96;
			Bitmap unscaledBitmap = null;
			String size = sizePrefix > 0 ? "_"+sizePrefix+".png" : ".png";
			switch ( bitmapType ) {
			case (mnuPosts):
				unscaledBitmap = Bitmap.getBitmapResource("dashboard_icon_posts"+size);
			break;
			case (mnuPages):
				unscaledBitmap = Bitmap.getBitmapResource("dashboard_icon_pages"+size);
			break;
			case (mnuComments):
				unscaledBitmap = Bitmap.getBitmapResource("dashboard_icon_comments"+size);
			break;
			case (mnuMedia):
				unscaledBitmap = Bitmap.getBitmapResource("dashboard_icon_posts"+size);
			break;
			case (mnuStats):
				unscaledBitmap = Bitmap.getBitmapResource("dashboard_icon_stats"+size);
			break;
			case (mnuSettings):
				unscaledBitmap = Bitmap.getBitmapResource("dashboard_icon_settings"+size);
			break;
			case (mnuRefresh):
				unscaledBitmap = Bitmap.getBitmapResource("dashboard_icon_refresh"+size);
			break;
			case (mnuDashboard):
				unscaledBitmap = Bitmap.getBitmapResource("dashboard_icon_dashboard"+size);
			break;
			case (mnuReader):
				unscaledBitmap = Bitmap.getBitmapResource("dashboard_icon_subs"+size);
			break;
			case (mnuNewPhoto):
				unscaledBitmap = Bitmap.getBitmapResource("dashboard_icon_photo"+size);
			break;
			default:
				break;
			}

			if( unscaledBitmap != null &&  unscaledBitmap.getHeight() !=  imageHeight ) {
				// Calculate the new scale based on the region sizes
				// Scale / Zoom
				// 0.1 = 1000%
				// 0.5 = 200%
				// 1 = 100%
				// 2 = 50%
				// 4 = 25%
				int	resultantScaleX = Fixed32.div(Fixed32.toFP( imageHeight ), Fixed32.toFP(unscaledBitmap.getHeight()));
				unscaledBitmap = ImageManipulator.scale(unscaledBitmap, resultantScaleX);
			}

			return unscaledBitmap;
		}

		/**
	     * Overrides default implementation.  Performs the show blog action if the 
	     * 4ways trackpad was clicked; otherwise, the default action occurs.
	     * 
	     * @see net.rim.device.api.ui.Screen#navigationClick(int,int)
	     */
		protected boolean navigationClick(int status, int time) {
			Log.trace(">>> navigationClick");
			
			if ((status & KeypadListener.STATUS_TRACKWHEEL) == KeypadListener.STATUS_TRACKWHEEL) {
				Log.trace("Input came from the trackwheel");
				// Input came from the trackwheel
				return super.navigationClick(status, time);
				
			} else if ((status & KeypadListener.STATUS_FOUR_WAY) == KeypadListener.STATUS_FOUR_WAY) {
				Log.trace("Input came from a four way navigation input device");
				return tableOrMenuItemSelected( menuIndex );
			}
			return super.navigationClick(status, time);
		}
		
	    /**
	     * Overrides default.  Enter key will take show blog action on selected blog.
	     *  
	     * @see net.rim.device.api.ui.Screen#keyChar(char,int,int)
	     * 
	     */
		protected boolean keyChar(char c, int status, int time) {
			Log.trace(">>> keyChar");
			// Close this screen if escape is selected.
			if (c == Characters.ENTER) {
				return tableOrMenuItemSelected( menuIndex );
			}
			
			return super.keyChar(c, status, time);
		}
		
		//#ifdef VER_4.7.0 | BlackBerrySDK5.0.0 | BlackBerrySDK6.0.0 | BlackBerrySDK7.0.0
		protected boolean touchEvent(TouchEvent message) {
							
	  		// Get the screen coordinates of the touch event
	        int x = message.getX(1);
	        int y = message.getY(1);
	        // Check to ensure point is within this field
	        if(x < 0 || y < 0 || x > getExtent().width || y > getExtent().height) {
	            return false;
	        }
			
			int eventCode = message.getEvent();

			if(WordPressInfo.isForcelessTouchClickSupported) {
				if (eventCode == TouchEvent.GESTURE) {
					TouchGesture gesture = message.getGesture();
					int gestureCode = gesture.getEvent();
					if (gestureCode == TouchGesture.TAP) {
						tableOrMenuItemSelected( menuIndex );
						return true;
					}
				} 
				return false;
			} else {
				if(eventCode == TouchEvent.CLICK) {
					tableOrMenuItemSelected( menuIndex );
					return true;
				}else if(eventCode == TouchEvent.DOWN) {
				} else if(eventCode == TouchEvent.UP) {
				} else if(eventCode == TouchEvent.UNCLICK) {
					return true; //consume the event: avoid context menu!!
				} else if(eventCode == TouchEvent.CANCEL) {
				}
				return false; 
				//return super.touchEvent(message);
			}
		}
		//#endif
	}	
}