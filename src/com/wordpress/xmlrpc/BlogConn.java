package com.wordpress.xmlrpc;


import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.pki.Certificate;

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.cldc.io.ssl.TLSException;
import net.rim.device.cldc.io.ssl.TLSIOException;

import org.kxmlrpc.XmlRpcClient;
import org.kxmlrpc.XmlRpcException;
import org.xmlpull.v1.XmlPullParserException;

import com.wordpress.bb.WordPressResource;
import com.wordpress.model.Blog;
import com.wordpress.model.Category;
import com.wordpress.model.Tag;
import com.wordpress.utils.log.Log;
import com.wordpress.utils.observer.Observable;
import com.wordpress.view.dialog.CredentialDialog;

public abstract class BlogConn extends Observable implements Runnable {
	
	protected final Boolean TRUE = new Boolean(true);
    protected final Boolean FALSE = new Boolean(false);
    protected static final int MAX_NUMBER_OF_REDIRECTIONS = 5;
    
	protected String urlConnessione;
    protected String mUsername;
    protected String mPassword;
   
    protected XmlRpcClient mConnection;	
	protected BlogConnResponse connResponse = new BlogConnResponse();
	protected boolean isWorking = false;
	protected Thread t = null;
	protected int threadPriority = Thread.NORM_PRIORITY;
	protected static final int minThreadPriority = Thread.MIN_PRIORITY;
	
	//401 HTTP Auth data
	protected int dialogResponse = Dialog.CANCEL;
	public boolean keepGoing = true;
	protected String authMessage = null;
	protected String http401Username = null;
	protected String http401Password = null;
	
	//request HTTP Headers
	protected Hashtable requestHeaders = new Hashtable();

	//create a variable to store the ResourceBundle for localization support
	protected static ResourceBundle _resources;
	
	static {
		//retrieve a reference to the ResourceBundle for localization support
		_resources = ResourceBundle.getBundle(WordPressResource.BUNDLE_ID, WordPressResource.BUNDLE_NAME);
	}

	public BlogConn(String url, String user, String password) {
	    mUsername = user;
	    mPassword = password;
	    urlConnessione=url;
	}
	
	public BlogConn(String url) {
	    urlConnessione=url;
	}
	
	public void startConnWork(){
		Log.debug("Inizio richiesta XML-RPC");
		isWorking=true;
		t = new Thread(this);
		t.setPriority(threadPriority); //thread by default is set to priority normal
		t.start();
	}
	
	public void startConnWorkBackground(){
		Log.debug("Inizio richiesta XML-RPC in background");
		isWorking=true;
		t = new Thread(this);
		t.setPriority(minThreadPriority); //thread by default is set to priority normal
		t.start();
	}
	
	public void setConnPriority (int priority) throws IllegalArgumentException  {
		Log.trace("connection background thread priority was set to: " + priority);	
		if(priority > Thread.MAX_PRIORITY || priority < Thread.MIN_PRIORITY)
			throw new IllegalArgumentException("Thread priority value is out of range");
		else
		this.threadPriority = priority;
	}
	
	
	/**
	 * blocca il funzionamento della connessione immediatamente.
	 */
	public void stopConnWork() {
		if (!isWorking)
			return;
		
		Log.debug("User requested stop the XML-RPC connection");
		isWorking = false;
		try {
			t.interrupt();
		} catch (Exception e) {
			Log.error(e, "Error while interrupting Conn Thread");
		} finally {
			t = null;
			Log.trace("XmlRpc Thread was set to null");
		}

		if(mConnection != null) {
			mConnection.closeXmlRpcConnection(); //closing the underlying connection streams could cause an exception on thread.
			mConnection.stopXmlRpcClient(); //set the stop flag to stop long thread. seems that t.interrupt doesn't work
			mConnection = null;
		}

		connResponse = new BlogConnResponse();
		connResponse.setError(false);
		connResponse.setStopped(true);
		connResponse.setResponse("connection stopped by user");
		notifyObservers(connResponse);
	}
	
	public abstract  void run();

	protected Object execute(String aCommand, Vector aArgs){
		isWorking=true;
		
		Object response = null;
		if(mConnection == null) {
			mConnection = new XmlRpcClient(urlConnessione);
		}
		//set HTTP auth if available
		if(http401Password != null && http401Username != null) {
			if(!http401Password.trim().equalsIgnoreCase("") 
					&& !http401Username.trim().equalsIgnoreCase("")) {
				mConnection.setHttp401Password(http401Password);
				mConnection.setHttp401Username(http401Username);
				Log.trace("HTTP auth are available to the XML-RPC client");
			}
		}
		mConnection.setRequestHeaders(requestHeaders);
		try {
			response = mConnection.execute(aCommand, aArgs);
		} catch (ConnectionNotFoundException cnfe) {
			setErrorMessage(cnfe, "The server was not found");
		} catch (TLSIOException tlsioe) {
			setErrorMessage(tlsioe, "SSL communication error (TLSIOException)");
			TLSException innerTLSException = tlsioe.getException();
			if ( innerTLSException != null ) {

				if ( innerTLSException.getMessage() != null )
					Log.error("Inner TLSException of the TLSIOException says: " + innerTLSException.getMessage());
				else 
					Log.error("The underlying TLSException found, but the message is null!");

				Exception exception = innerTLSException.getException();
				if( exception != null ) { 
					Log.error( "Inner Exception of the TLSException : " +exception.getClass() );
					if( exception.getMessage()!= null )
						Log.error("Inner Exception of the TLSException says: " + exception.getMessage() );
					else 
						Log.error("Inner Exception of the TLSException found, but the message is null!");
				} else {
					Log.error( "Inner Exception of the TLSException NOT Found");
				}

			} else {
				Log.error("NO underlying TLSException found");
			}
			logCertificateException( tlsioe );
		} catch (javax.microedition.pki.CertificateException ce) {
			setErrorMessage(ce, "SSL communication error (CertificateException)");
			logCertificateException( ce );
		} catch (IOException ioe) {
			setErrorMessage(ioe, "A server communication error occurred");
		} catch (XmlRpcException xre) {
			setErrorMessage(xre, "Server Error " +xre.code+"");
		} catch (XmlPullParserException parserEx) { //catch all parser exception and rewrite the log message for user
			Log.error("Parser Exception : "+parserEx.getMessage());
			XmlPullParserException rewrittenEx = new XmlPullParserException("Malformed blog response");
			setErrorMessage(rewrittenEx, "A server communication error occurred");
		} catch (Exception t) {
			setErrorMessage(t, "An error occurred");
		} 
   	 
		Log.trace("Ended XML-RPC request");
		isWorking=false;
		return response;
	}
	
	private void logCertificateException (javax.microedition.pki.CertificateException ce) {
		Log.error(">>> Logging Certificate details");

		try {
			Log.error("SSL certificate not accepted. Reason: " + ce.getReason() + ", certificate: " + ce.getCertificate());
		} catch (Exception e) {
		}
		
		byte reason = ce.getReason();
		int cast = (int)reason & 0xff;
		Log.error("Reason: " + cast);
	/*	switch (reason) {
		case TLSIOException.BAD_EXTENSIONS:
			
			break;

		default:
			break;
		}*/
		Certificate certificate = ce.getCertificate();
		if ( certificate != null ) {
			if ( certificate.getType() != null )
				Log.error("Cert Type: " + certificate.getType());
			if ( certificate.getVersion() != null )
				Log.error("Cert Version: " +certificate.getVersion());
			if ( certificate.getSigAlgName() != null )
				Log.error("Cert SigAlgName: " +certificate.getSigAlgName());
			if ( certificate.getIssuer() != null )
				Log.error("Cert Issuer: " +certificate.getIssuer());
			if ( certificate.getSubject() != null )
				Log.error("Cert Subjet: " +certificate.getSubject());
			if ( certificate.getSerialNumber() != null )
				Log.error("Cert Serial Number: " +certificate.getSerialNumber());
			Log.error("Not After: "+certificate.getNotAfter());
		} else {
			Log.error("Certificate is null!");
		}
		Log.error("<<< Logging Certificate details");
	}
	
	protected void showHTTPAuthDialog() {
		//this check is necessary to ensure the user has not clicked cancel meanwhile
		if (!isWorking)
			return;
		
		final CredentialDialog dlg;
		
		if (authMessage == null) 
			dlg = new CredentialDialog();
		else
			dlg = new CredentialDialog(authMessage);
		
		UiApplication.getUiApplication().invokeAndWait(new Runnable()
           {
              public void run()
              {
              	 dialogResponse = dlg.doModal();
              }
           });
		
		if(dialogResponse == Dialog.D_OK) {
			http401Password = dlg.getPassWord();
			http401Username = dlg.getUserName();
      	} else {
      		http401Password  = null;
      		http401Username = null;
      		keepGoing = false;
      	}
	}
	
	public void setAuthMessage(String authMessage) {
		this.authMessage = authMessage;
	}

	public void setHttp401Username(String http401Username) {
		this.http401Username = http401Username;
	}
	
	public void setHttp401Password(String http401Password) {
		this.http401Password = http401Password;
	}
	
	public String getHttp401Password() {
		return http401Password;
	}
	
	public String getHttp401Username() {
		return http401Username;
	}
	
	public void setHTTPHeaderField(String name, String value) {
		if ( value == null )
			requestHeaders.remove(name);
		else
			requestHeaders.put(name, value);
	}
		
	protected void setPostCategories(int[] categories, String postID) throws Exception {
		Log.debug(">>> Set Post categories ");
		Vector args;
		if (categories != null) {
		    Vector catVector = new Vector(categories.length);
		    for (int i = 0; i < categories.length; i++) {
		    	Hashtable primary = new Hashtable(4);
		        primary.put("categoryId", String.valueOf(categories[i]));
		        catVector.addElement(primary);
			}
		  		    
		    args = new Vector(4);
		    args.addElement(postID);
		    args.addElement(mUsername);
		    args.addElement(mPassword);
		    args.addElement(catVector);
		         
		    Object  response = execute("mt.setPostCategories", args);
			if(connResponse.isError()) {
				throw new Exception("Error while setting post categories");
			}
		}

	}
	
	//retrive comments 
	protected synchronized Vector getComments(String blogId, String postId, String status, int offset, int number){
		try{
			Log.debug(">>> getComments");
			Hashtable structData = new Hashtable(5);
			
			if (postId != null) {
				structData.put("post_id", postId);
			} else {
				
			}
			if(status != null && !status.equalsIgnoreCase(""))
				structData.put("status", status);
			
			if (offset!= 0 ) {
				structData.put("offset", String.valueOf(offset));
			}
			if (number != 0) {
				structData.put("number", String.valueOf(number));
			}
			
			Vector args = new Vector(5);
			args.addElement(blogId);
			args.addElement(mUsername);
			args.addElement(mPassword);
			args.addElement(structData);
			
			Object response = execute("wp.getComments", args);
			if(connResponse.isError()) {
				//notifyObservers(connResponse);
				return null;		
			}
			
			return ((Vector)response);
		}
		catch (ClassCastException e) {
			throw new ClassCastException("GetComments error: Invalid server response "+ e.getMessage());
		}
		
	}
		
	//retrive "recent post title list"
	protected synchronized Vector getRecentPostTitle(String blogID, int maxPost) throws Exception {
		try {
			Log.debug(">>> getRecentPostTitle");
			
			Vector args = new Vector(4);
	        args.addElement(blogID);
	        args.addElement(mUsername);
	        args.addElement(mPassword);
	        args.addElement(new Integer(maxPost));

	        Object response = execute("mt.getRecentPostTitles", args);
			if(connResponse.isError()) {
				return null;		
			}

            Vector responsePosts = (Vector) response;
            return responsePosts;

		} catch (ClassCastException cce) {
			throw new Exception("Error while reading recent post title list");
		}
	}
	
	protected synchronized void getBlogCategories(Blog blog) throws Exception {
		Log.debug("Loading categories for the blog: "+ blog.getName());
		Vector args;
		Object response;
		Vector categoryStructs;
		Hashtable categoryStruct;
		Category[] categories;

		args = new Vector(3);
		args.addElement(blog.getId());
		args.addElement(mUsername);
		args.addElement(mPassword);

		response = execute("wp.getCategories", args);
		if(connResponse.isError()) {
			 //blog.setCategories(null);
			 return;
		}
		
		args = null;

		try {
		    categoryStructs = (Vector) response;
		    categoryStruct = null;
		    if (categoryStructs.size() > 0) {
		        categories = new Category[categoryStructs.size()];
		        for (int i = 0; i < categories.length; i++) {
		            categoryStruct = (Hashtable) categoryStructs.elementAt(i);
		            categories[i] = new Category
		            ((String) categoryStruct.get("categoryId"),
		            (String) categoryStruct.get("categoryName"),
		            (String) categoryStruct.get("description"),
		            Integer.parseInt((String) categoryStruct.get("parentId")),
		            (String) categoryStruct.get("htmlUrl"),
		            (String) categoryStruct.get("rssUrl")
		            );
		        }
		        blog.setCategories(categories);
		    } else {
		        blog.setCategories(null);
		    }
		    Log.debug("categories loaded succesfull : "+ blog.getName());
		} catch (ClassCastException cce) {
		    throw new Exception("Error while reading Categories");
		}
	}
	
	
	//retrive all pages from blog
	protected synchronized Vector getPages(String blogID, int maxPages) throws Exception{
		try{
			Log.debug(">>> getPages");
			Vector args = new Vector(3);
			args.addElement(blogID);
			args.addElement(mUsername);
			args.addElement(mPassword);
			args.addElement(String.valueOf(maxPages));
			
			Object response = execute("wp.getPages", args);
			if(connResponse.isError()) {
				return null;		
			}
			
			Vector pagesVector = (Vector) response;
			return pagesVector;
			
		} catch (ClassCastException cce) {
			throw new Exception("Error while reading pages data from blog");
		}
	}
				
	//retrive the blog "tag list"
	protected synchronized void getTagList(Blog blog) throws Exception {
		try {
			Log.debug("reading tag list for the blog : "
					+ blog.getName());

			Vector args = new Vector(3);
			args.addElement(String.valueOf(blog.getId()));
			args.addElement(mUsername);
			args.addElement(mPassword);

			Object response = execute("wp.getTags", args);
			if(connResponse.isError()) {
				// blog.setTags(null);
				 return;
				//throw new Exception("cannot read tag list");
			}

			Vector tags = (Vector) response;

			Tag[] mytags= new Tag[tags.size()];
			int blogId =Integer.parseInt(blog.getId());
			
			Hashtable tagData = null;
			for (int i=0; i<tags.size(); i++){
				tagData = (Hashtable) tags.elementAt(i);
				int tagId=Integer.parseInt((String)tagData.get("tag_id"));
				String tagName=(String) tagData.get("name");
				int count=Integer.parseInt((String) tagData.get("count"));
				String slug=(String) tagData.get("slug");
				String htmlUrl=(String) tagData.get("html_url");
				String rssUrl= (String) tagData.get("rss_url");
				Tag myTag= new Tag(tagId,tagName,count,slug, htmlUrl, rssUrl);
				mytags[i]=myTag;
			}
			blog.setTags(mytags);
			
			Log.debug("End reading tag list for the blog : "
					+ blog.getName());
		} catch (ClassCastException cce) {
			throw new Exception("Error while reading post status list");
		}
	}
	
	protected void setErrorMessage(Exception e, String err){
	/*	if (!isWorking) 
			return;*/

		//check if there is a prev error in the error stack
		if(connResponse.isError()) {
			String prevErr = connResponse.getResponse();
			if ( ! prevErr.trim().equalsIgnoreCase("") )
				err = prevErr +"\n\n"+ err;  
		}
		
		boolean isConnectionStoppedByUser = connResponse.isStopped();
		
		connResponse = new BlogConnResponse();
		connResponse.setError(true);
		connResponse.setResponseObject(e); //set the exception as response option
		if(isConnectionStoppedByUser) //check if the conn was stoppped by user
			connResponse.setStopped(true);
		
		if(e != null && e.getMessage()!= null ) {
			//connResponse.setResponse(err+"\n"+e.getMessage());
			connResponse.setResponse(err);
			Log.error(err+" -> "+e.getMessage());
		} else {
			connResponse.setResponse(err);
			Log.error(err);			
		}
	}
	
	protected void setErrorMessage(String err){
		this.setErrorMessage(null, err);
	}
}
