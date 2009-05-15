package com.wordpress.xmlrpc;

import java.util.Hashtable;
import java.util.TimeZone;
import java.util.Vector;

import com.wordpress.model.Blog;

public class BlogAuthConn extends BlogConn  {
	
	
	private final int maxRecentPostCount; 

	public BlogAuthConn(String hint,String userHint, String passwordHint, TimeZone tz, int maxRecentPostCount) {
		super(hint, userHint, passwordHint, tz);
		this.maxRecentPostCount = maxRecentPostCount;
	}

	/**
	 * Load blogs 
	 * 
	 */
	public void run() {
		Vector args;

		args = new Vector(2);
		args.addElement(this.mUsername);
		args.addElement(this.mPassword);

		Object response = execute("wp.getUsersBlogs", args);
		if(connResponse.isError()) {
			notifyObservers(connResponse);
			return;		
		} 
	
		try {
			Vector blogs = (Vector) response;
			Blog[] myBlogs= new Blog[blogs.size()];
			
			Hashtable blogData = null;
			for (int i = 0; i < blogs.size(); i++) {
				blogData = (Hashtable) blogs.elementAt(i);
			
				System.out.println("blogId: "+(String) blogData.get("blogid"));
				System.out.println("blogName: "+(String) blogData.get("blogName"));
				System.out.println("blogURL: " +(String) blogData.get("url"));
				System.out.println("blogXMLRPC: " +(String) blogData.get("xmlrpc"));
			
				Blog currentBlog= new Blog((String)blogData.get("blogid") , (String)blogData.get("blogName"),
						(String)blogData.get("url"), (String)blogData.get("xmlrpc"), this.mUsername, this.mPassword);
				
				myBlogs[i]=currentBlog;		
			}		
			
			connResponse.setResponseObject(myBlogs);
		} catch (ClassCastException cce) {
			setErrorMessage(cce, "loadBlogs error");
		} catch (Exception e) {
			setErrorMessage(e, "Invalid server response");
		}
		
		try {
			notifyObservers(connResponse);
		} catch (Exception e) {
			System.out.println("Blog Auth Notify Error"); 
		}
	}
}