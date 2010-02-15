package com.wordpress.controller;

import net.rim.device.api.ui.UiApplication;

import com.wordpress.bb.NotificationHandler;
import com.wordpress.io.AppDAO;
import com.wordpress.io.BlogDAO;
import com.wordpress.model.Blog;
import com.wordpress.model.BlogInfo;
import com.wordpress.model.Preferences;
import com.wordpress.utils.log.Log;
import com.wordpress.view.NotificationView;


public class NotificationController extends BaseController {
	
	private NotificationView view = null;
	private BlogInfo[] blogsList = new BlogInfo[0];
    private Preferences mPrefs=Preferences.getIstance();
    
	public NotificationController(BlogInfo[] blogs) {
		super();
	   	try {
	   			blogsList = blogs;
				String[] blogName = new String[blogs.length];
				boolean[] blogSelected = new boolean[blogs.length];
				
				for (int i = 0; i < blogs.length; i++) {
					BlogInfo blogInfo = blogs[i];
					blogName[i] = blogInfo.getName();
					blogSelected[i] = blogInfo.isCommentNotifies();
				}

				this.view= new NotificationView(this, blogName, blogSelected);
				
			} catch (Exception e) {
				Log.error(e, "Error while reading stored blog");
			}
	}
	
	public void saveSettings(boolean[] selected, int updateIntervalIndex) {
		Log.trace(">>> NotificationController.saveSettings");
		if( getSelectedIntervalTime() != updateIntervalIndex ) {
			mPrefs.setUpdateTimeIndex(updateIntervalIndex);

			try {
				AppDAO.storeApplicationPreferecens(mPrefs);
			} catch (Exception e) {
				displayError(e, "Error while saving update interval preference");
				return;
			}
		}
		
		//controllare le impostaziani di notifica dei blog
		for (int i = 0; i < blogsList.length; i++) {
			BlogInfo blogInfo = blogsList[i];
			boolean newValue = selected[i];
			if (newValue != blogInfo.isCommentNotifies()) {
				try {
					//update also the blogInfo element. it is a reference to the blogInfo obj used also in the  main view.
					blogInfo.setCommentNotifies(newValue);
					Blog blog = BlogDAO.getBlog(blogInfo);
					blog.setCommentNotifies(newValue);
					BlogDAO.updateBlog(blog);
				} catch (Exception e) {
					displayError(e, "Error while updating Blog");
				}
			}
		}

		//enable the notifications
		NotificationHandler.getInstance().shutdown();
		
		if (mPrefs.getUpdateTimeIndex() != 0)
			NotificationHandler.getInstance().setEnabled(true, updateIntervalIndex);
		Log.trace("<<< NotificationController.saveSettings");
	}
	
	public void showView(){
		UiApplication.getUiApplication().pushScreen(view);
	}

	
	public String[] getIntervalTimeLabels(){
		String[] choices = {"OFF", "120 minutes", "90 minutes", "60 minutes", "30 minutes", "15 minutes", "10 minutes", "5 minutes"};
		return choices;
	}
	
	public int getSelectedIntervalTime() {
		return mPrefs.getUpdateTimeIndex();
	}

	
	public void refreshView() {
		
	}	
}