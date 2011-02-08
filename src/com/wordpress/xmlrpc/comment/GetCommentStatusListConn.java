package com.wordpress.xmlrpc.comment;

import java.util.Hashtable;
import java.util.Vector;

import com.wordpress.utils.log.Log;
import com.wordpress.xmlrpc.BlogConn;

public class GetCommentStatusListConn extends BlogConn  {
	
	private String blogId = null;
	
	public GetCommentStatusListConn(String hint, String blogId, String userHint, String passwordHint){
		super(hint, userHint, passwordHint);
		this.blogId = blogId;
	}
	
	public void run() {
		try {
			
			Vector args = new Vector(4);
	        args.addElement(blogId);
	        args.addElement(mUsername);
	        args.addElement(mPassword);
		
	        Object response = execute("wp.getCommentStatusList", args);
			if(connResponse.isError()) {
				notifyObservers(connResponse);
				return;		
			}		
            Hashtable commentData = (Hashtable) response;
            connResponse.setResponseObject(commentData);
 			}
			catch (Exception e) {
				setErrorMessage(e, "Error while loading comments status list");
	        }
			
			try {
				notifyObservers(connResponse);
			} catch (Exception e) {
				Log.error("GetCommentStatusList error: Notify error"); 
			}
			
		}
	}