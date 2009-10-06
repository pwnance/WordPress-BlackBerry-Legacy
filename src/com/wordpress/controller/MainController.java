package com.wordpress.controller;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.microedition.rms.RecordStoreException;

import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;

import com.wordpress.bb.WordPressCore;
import com.wordpress.bb.WordPressResource;
import com.wordpress.io.BlogDAO;
import com.wordpress.model.Blog;
import com.wordpress.model.BlogInfo;
import com.wordpress.task.TaskProgressListener;
import com.wordpress.utils.DataCollector;
import com.wordpress.utils.log.Log;
import com.wordpress.view.MainView;


public class MainController extends BaseController implements TaskProgressListener{
	
	private MainView view = null;
	private Timer timer = new Timer();
	
	public MainController() {
		super();
	}
	
	public void showView(){
		
		int numberOfBlog = 0; 
		
		//reset the state of blogs that are in loading or queue to loading error state
		//.... maybe app crash during adding blog
	   	 try {
			BlogInfo[] blogsList = getBlogsList();
			
			for (int i = 0; i < blogsList.length; i++) {
				BlogInfo blogInfo = blogsList[i];
				Blog blog = BlogDAO.getBlog(blogInfo);
			
				if (blog.getLoadingState() == BlogInfo.STATE_LOADING
						|| blog.getLoadingState() == BlogInfo.STATE_ADDED_TO_QUEUE) {
					blog.setLoadingState(BlogInfo.STATE_LOADED_WITH_ERROR);
					BlogDAO.updateBlog(blog);
				}
			}
			numberOfBlog = blogsList.length;  //get the number of blog
		} catch (Exception e) {
			Log.error(e, "Error while reading stored blog");
		}
		
		//stats and update stuff!
		try {
			DataCollector dtc = new DataCollector();
			dtc.collectData(numberOfBlog); //start data gathering here
		} catch (Exception e) {
			//don't propagate this Exception
		}
		timer.schedule(new CheckUpdateTask(), 24*60*60*1000, 24*60*60*1000); //24h check
		
		this.view=new MainView(this); //main view init here!.	
		UiApplication.getUiApplication().pushScreen(this.view);
	}
	

	private class CheckUpdateTask extends TimerTask {
		public void run() {
			try {
				BlogInfo[] blogsList = getBlogsList();
				DataCollector dtc = new DataCollector();
				dtc.collectData(blogsList.length); //start data gathering here
			} catch (Exception e) {
				Log.error(e, "Error while checking for a new version in background");
			} 			  
		}
	}
	
	
	public BlogInfo[] getBlogsList() {

		String[] listFiles = new String[0];
		BlogInfo[] blogs = new BlogInfo[0];

		try {
			listFiles = BlogDAO.getBlogsPath();
		} catch (Exception e1) {
			displayError(e1, "Error while loading your blogs index");
			return blogs;
		}

		Vector blogsVector = new Vector();
		for (int i = 0; i < listFiles.length; i++) {
			String currBlogPath = listFiles[i];

			try {
				BlogInfo currBlogInfo = BlogDAO.getBlogInfo(currBlogPath);
				blogsVector.addElement(currBlogInfo);
			} catch (Exception e) {
				displayError(e, "Error while loading blog: "+ currBlogPath);
			}
		}
		
		blogs = new BlogInfo[blogsVector.size()];
		for (int i = 0; i < blogs.length; i++) {
			blogs[i] = (BlogInfo) blogsVector.elementAt(i);
		}
		

		return blogs;
	}


	
	public void deleteBlog(BlogInfo selectedBlog) {
		if (selectedBlog.getState() == BlogInfo.STATE_LOADING || selectedBlog.getState() == BlogInfo.STATE_ADDED_TO_QUEUE) {
			displayMessage("Loading blog. Try later..");
		} else {
			try {
				BlogDAO.removeBlog(selectedBlog);
			} catch (IOException e) {
				displayError(e, "Error while deleting the blog");
			} catch (RecordStoreException e) {
				displayError(e, "Error while deleting the blog");
			}
		}
	}

	public void addBlogs() {
		AddBlogsController ctrl=new AddBlogsController(this);
		ctrl.showView();
	}
		
	public void showBlog(BlogInfo selectedBlog){

		if (selectedBlog.getState() == BlogInfo.STATE_LOADING || selectedBlog.getState() == BlogInfo.STATE_ADDED_TO_QUEUE) {
			//blog in caricamento
			displayMessage("Loading blog. Try later..");
		} else {
			
		Blog currentBlog=null;
	   	 try {
	   		 currentBlog=BlogDAO.getBlog(selectedBlog);
	   		 FrontController.getIstance().showBlog(currentBlog);
			} catch (Exception e) {
				displayError(e, "Show Blog Error");
			}
		}
	}
			
	public void refreshView() {
		view.refreshBlogList();
	}	
	
	// Utility routine to ask question about exit application
	public synchronized boolean exitApp() {
		
/*		boolean inLoadingState = AddBlogsMediator.getIstance().isInLoadingState();
		if( inLoadingState ) {
			displayMessage("There are blogs in loading... Wait until blogs are loaded");
			return false;
		}
	*/	
    	int result=this.askQuestion(_resources.getString(WordPressResource.MESSAGE_EXIT_APP));   
    	if(Dialog.YES==result) {
    		timer.cancel();
    		WordPressCore.getInstance().exitWordPress();
    		return true;
    	} else {
    		return false;
    	}
	}

	//listener for the adding blogs task
	public void taskComplete(Object obj) {
		taskUpdate(obj);		
	}
	
	//listener for the adding blogs task
	public void taskUpdate(Object obj) {
		synchronized (view) {
			Blog loadedBlog = (Blog)obj;
			String blogName = loadedBlog.getName();
			String blogXmlRpcUrl=loadedBlog.getXmlRpcUrl();
			String blogId= loadedBlog.getId();
			int blogLoadingState = loadedBlog.getLoadingState();
			BlogInfo blogI = new BlogInfo(blogId, blogName,blogXmlRpcUrl,blogLoadingState);
			view.setBlogItemViewState(blogI); 
		}
	}	
}