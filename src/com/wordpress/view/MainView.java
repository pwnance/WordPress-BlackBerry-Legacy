package com.wordpress.view;

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.ObjectListField;
import net.rim.device.api.ui.container.MainScreen;

import com.wordpress.bb.WordPressResource;
import com.wordpress.controller.BlogIOController;
import com.wordpress.controller.FrontController;
import com.wordpress.controller.MainController;

public class MainView extends MainScreen {
	
    private BlogIOController blogController = null;
    private MainController mainController=null;

    private ObjectListField listaBlog; 
    		
    //create a variable to store the ResourceBundle for localization support
    private static ResourceBundle _resources;
	    
    static {
        //retrieve a reference to the ResourceBundle for localization support
        _resources = ResourceBundle.getBundle(WordPressResource.BUNDLE_ID, WordPressResource.BUNDLE_NAME);
    }
	

	public MainView(MainController mainController) {
		super();
		this.mainController=mainController;
	
		blogController= BlogIOController.getIstance();
        //add a screen title
        LabelField title = new LabelField(_resources.getString(WordPressResource.TITLE_APPLICATION),
                        LabelField.ELLIPSIS | LabelField.USE_ALL_WIDTH);
        setTitle(title);
        setupUpBlogsView();
	
		addMenuItem(_aboutItem);
		addMenuItem(_addBlogItem);
		addMenuItem(_setupItem);
	}
	
	
	 public void setupUpBlogsView() {
		String[] blogCaricati= blogController.getBlogNames();

    	removeMenuItem(_refreshBlogItem);
    	removeMenuItem(_deleteBlogItem);
    	removeMenuItem(_showBlogItem);
		
        listaBlog = new ObjectListField();
        if (blogCaricati.length == 0){
        	blogCaricati= new String[1];
        	blogCaricati[0]="Setup your blog...";
        } else {
        	addMenuItem(_showBlogItem);
        	addMenuItem(_refreshBlogItem);
        	addMenuItem(_deleteBlogItem);
        }
    	listaBlog.set(blogCaricati);
        add(listaBlog);
	}
	 
	public void refreshBlogList(){	 
		if(listaBlog != null){
			this.delete(listaBlog);
			setupUpBlogsView();
		}
	 }

/*	
	// Handle trackball clicks.
	protected boolean navigationClick(int status, int time) {
		return true;
	}

	protected boolean keyChar(char c, int status, int time) {
		// Close this screen if escape is selected.
		if (c == Characters.ESCAPE) {
			return true;
		} else if (c == Characters.ENTER) {
			return true;
		}

		return super.keyChar(c, status, time);
	}
	
*/
	
    private MenuItem _showBlogItem = new MenuItem( _resources, WordPressResource.MENUITEM_SHOWBLOG, 130, 10) {
        public void run() {
            int selectedBlog = listaBlog.getSelectedIndex();
            mainController.showBlog(selectedBlog);
        }
    };
   

    //add blog menu item 
    private MenuItem _addBlogItem = new MenuItem( _resources, WordPressResource.MENUITEM_ADDBLOG, 140, 10) {
        public void run() {
        	FrontController.getIstance().showAddBlogsView();
        }
    };

    private MenuItem _refreshBlogItem = new MenuItem( _resources, WordPressResource.MENUITEM_REFRESHBLOG, 150, 10) {
        public void run() {
        	int selected = listaBlog.getSelectedIndex();
        	mainController.refreshBlog(selected);
        }
    };
    
    private MenuItem _deleteBlogItem = new MenuItem( _resources, WordPressResource.MENUITEM_DELETEBLOG, 200, 10) {
        public void run() {
            int selectedBlog = listaBlog.getSelectedIndex();
            mainController.deleteBlog(selectedBlog);
            refreshBlogList();
        }
    };
    
    private MenuItem _setupItem = new MenuItem( _resources, WordPressResource.MENUITEM_SETUP, 1000, 10) {
        public void run() {
        	FrontController.getIstance().showSetupView();
        }
    };

    private MenuItem _aboutItem = new MenuItem( _resources, WordPressResource.MENUITEM_ABOUT, 1010, 10) {
        public void run() {
        	FrontController.getIstance().showAboutView();
        }
    };
   
    //override onClose() to display a dialog box when the application is closed    
	public boolean onClose()   {
    	return mainController.exitApp();
    }
}