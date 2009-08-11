package com.wordpress.xmlrpc.page;

import java.util.Vector;

import com.wordpress.io.PageDAO;
import com.wordpress.model.Page;
import com.wordpress.xmlrpc.BlogConn;

public class EditPageConn extends BlogConn  {

	private boolean isPublished=false;
	private final int blogId;
	private Page page;

	public EditPageConn(String hint, String userHint, String passwordHint, int blogId, Page page, boolean isPublished) {
		super(hint, userHint, passwordHint);
		this.blogId = blogId;
		this.page = page;
		this.isPublished=isPublished;
	}
	
	public void setPage(Page page) {
		this.page = page;
	}
	
	public void run() {
		try{
			if (page.getID() < 0) {
				setErrorMessage("Page does not have an Id");
				notifyObservers(connResponse);
				return;
			}

			Vector args = new Vector(6);
			args.addElement(String.valueOf(blogId));
			args.addElement(String.valueOf(page.getID()));
			args.addElement(mUsername);
			args.addElement(mPassword);
			args.addElement(PageDAO.page2Hashtable(page));
			args.addElement(isPublished ? TRUE : FALSE);

			Object response = execute("wp.editPage", args);
			if(connResponse.isError()) {
				notifyObservers(connResponse);
				return;		
			}

			connResponse.setResponseObject(response);			
		} catch (Exception e) {
			setErrorMessage(e, "EditPage error: Invalid server response");
		}

		try {
			notifyObservers(connResponse);
		} catch (Exception e) {
			System.out.println("EditPage error: Notify error"); 
		}
	}
}