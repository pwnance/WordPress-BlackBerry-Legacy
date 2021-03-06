package com.wordpress.controller;

import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.rms.RecordStoreException;

import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;

import com.wordpress.bb.WordPressResource;
import com.wordpress.io.DraftDAO;
import com.wordpress.model.Blog;
import com.wordpress.model.Post;
import com.wordpress.utils.log.Log;
import com.wordpress.view.DraftPostsView;
import com.wordpress.view.dialog.ConnectionInProgressView;


public class DraftPostsController extends BaseController {
	
	private DraftPostsView view = null;
	ConnectionInProgressView connectionProgressView=null;
	private Blog currentBlog=null;
	private Hashtable[] loadedPostInfo = null; //shorcut to post  title
	private int[] loadedPostID = null; //shorcut to post ID
	
	public DraftPostsController(Blog currentBlog) {
		super();	
		this.currentBlog=currentBlog;
	}

	public void showView() {
	    try {
	    	
		    this.view= new DraftPostsView(this,loadedPostInfo);
			UiApplication.getUiApplication().pushScreen(view);
			
		} catch (Exception e) {
	    	displayError(e, "Error while reading drafts phones memory");
		}
	}

	private void loadPostInfo() throws IOException, RecordStoreException {
		//if we can't load file name index, we exit immediately		
		String[] loadedPostIndex = null;
		try {
			loadedPostIndex = DraftDAO.getPostsInfo(currentBlog);
		} catch (Exception e) {
			Log.error(e, "Could not load draft posts index from disk");
			throw new IOException ("Could not load draft posts index from disk");
		}
		
		//try to read data from storage.
		Vector vectorPost = new Vector();
		Vector vectorPostFileName = new Vector();
		for (int i = 0; i < loadedPostIndex.length; i++) {
			try{
				String currPostFile = (String)loadedPostIndex[i];    		
				Post loadDraftPost = DraftDAO.loadPost(currentBlog, Integer.parseInt(currPostFile));
				String title = loadDraftPost.getTitle();
				if (title == null || title.length() == 0) {
					title = "No title";
				}
				//create a small hashtable with necessary data
				Hashtable smallPostData = new Hashtable();				
				smallPostData .put("title", title);
				Date dateCreated = loadDraftPost.getAuthoredOn();
				if (dateCreated != null)
					smallPostData .put("date_created_gmt", dateCreated);
				
				
				vectorPost.addElement(smallPostData);
				vectorPostFileName.addElement(currPostFile);
			} catch (Exception e) {
				Log.error(e, "Could not load some post from disk");
				throw new IOException ("Could not load some post from disk");
			}
		}
	    
    	    	
		loadedPostInfo = new Hashtable[vectorPost.size()];
		loadedPostID = new int[vectorPost.size()];
		
		for (int i = 0; i < vectorPost.size(); i++) {
			loadedPostID[i] = Integer.parseInt((String)vectorPostFileName.elementAt(i));
			loadedPostInfo[i] = (Hashtable)vectorPost.elementAt(i);
		}
	}


	public String getCurrentBlogName() {
		return currentBlog.getName();
	}

	public void updateViewDraftPostList() {
		try {
			loadPostInfo();
			view.refresh(loadedPostInfo);
		} catch (Exception e) {
	    	displayError(e, "Error while reading drafts phones memory");
		}
	}
	
	
	public void deletePost(int selected){
		int result=this.askQuestion(_resources.getString(WordPressResource.MESSAGE_DELETE_POST));   
    	if(Dialog.YES != result) {
    		return;
    	}		
		int draftPostID = loadedPostID[selected];
		try {
			DraftDAO.removePost(currentBlog, draftPostID);
		} catch (IOException e) {
	    	displayError(e, "Error while deleting draft post");
		} catch (RecordStoreException e) {
			displayError(e, "Error while deleting draft post");
		}
		updateViewDraftPostList();
	}
	
	public void newPost() {
		if (currentBlog != null) {
			FrontController.getIstance().newPost(currentBlog); // show the new post view
		}
	}
	
	/** starts the post loading */
	public void editPost(int selected) {
		try {
			if (selected != -1) {
				int draftPostID = loadedPostID[selected];
				Post post = DraftDAO.loadPost(currentBlog, draftPostID);
				FrontController.getIstance().showDraftPost(post, draftPostID);
			}
		} catch (Exception e) {
			displayError(e, "Error while loading draft post");
		}
	}	
	
	public void refreshView() {
	}
	
	//return back to pst list. update screen
	public void toPostsList() {
		FrontController.getIstance().backAndRefreshView(true); //deep refresh
	}
	
}