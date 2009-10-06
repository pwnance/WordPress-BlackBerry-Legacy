package com.wordpress.controller;

import java.io.IOException;
import java.util.Vector;

import javax.microedition.rms.RecordStoreException;

import net.rim.device.api.ui.UiApplication;

import com.wordpress.io.CommentsDAO;
import com.wordpress.model.Blog;
import com.wordpress.view.CommentsView;

public class RecentCommentsController extends CommentsController {
	
	public RecentCommentsController(Blog currentBlog) {
		super(currentBlog);
	}
	
	public void showView() {
		Vector comments = null;
		try {
			comments = CommentsDAO.loadComments(currentBlog);
		} catch (IOException e) {
			displayError(e, "Error while loading comments from memory");
		} catch (RecordStoreException e) {
			displayError(e, "Error while loading comments from memory");
		}

		storedComments = CommentsDAO.vector2Comments(comments);	
		
		view= new CommentsView(this, storedComments, gravatarController, currentBlog.getName());
		UiApplication.getUiApplication().pushScreen(view);
	}
	
	protected void storeComment(Vector comments) {
		try{
			CommentsDAO.storeComments(currentBlog, comments);
		} catch (IOException e) {
			displayError(e, "Error while updating phone memory");
		} catch (RecordStoreException e) {
			displayError(e, "Error while updating phone memory");
		}
		catch (Exception e) {
			displayError(e, "Error while updating phone memory");
		} 
	}
}
