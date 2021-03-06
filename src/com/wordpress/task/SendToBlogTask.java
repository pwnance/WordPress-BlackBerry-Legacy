package com.wordpress.task;

import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.rms.RecordStoreException;

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.i18n.ResourceBundleFamily;
import net.rim.device.api.system.EncodedImage;

import org.kxmlrpc.XmlRpcException;

import com.wordpress.bb.WordPressCore;
import com.wordpress.bb.WordPressResource;
import com.wordpress.io.AppDAO;
import com.wordpress.io.BlogDAO;
import com.wordpress.io.DraftDAO;
import com.wordpress.io.JSR75FileSystem;
import com.wordpress.io.PageDAO;
import com.wordpress.model.AudioEntry;
import com.wordpress.model.Blog;
import com.wordpress.model.BlogEntry;
import com.wordpress.model.BlogInfo;
import com.wordpress.model.MediaEntry;
import com.wordpress.model.MediaLibrary;
import com.wordpress.model.Page;
import com.wordpress.model.PhotoEntry;
import com.wordpress.model.Post;
import com.wordpress.model.Preferences;
import com.wordpress.model.VideoEntry;
import com.wordpress.utils.CalendarUtils;
import com.wordpress.utils.ImageUtils;
import com.wordpress.utils.MultimediaUtils;
import com.wordpress.utils.Queue;
import com.wordpress.utils.StringUtils;
import com.wordpress.utils.log.Log;
import com.wordpress.utils.observer.Observable;
import com.wordpress.utils.observer.Observer;
import com.wordpress.xmlrpc.BlogConn;
import com.wordpress.xmlrpc.BlogConnResponse;
import com.wordpress.xmlrpc.NewMediaObjectConn;
import com.wordpress.xmlrpc.page.EditPageConn;
import com.wordpress.xmlrpc.page.NewPageConn;
import com.wordpress.xmlrpc.post.EditPostConn;
import com.wordpress.xmlrpc.post.NewPostConn;

public class SendToBlogTask extends TaskImpl {
	
	private Preferences mPrefs = Preferences.getIstance();
	private BlogConn blogConn = null;
	private Queue executionQueue = new Queue(); // queue of BlogConn
	private final Blog blog;
	private BlogEntry blogEntry; //this class should be refactored very soon.
	private Page page;
	private Post post;
	private MediaLibrary library;
	private int draftFolder;
	
		
	public SendToBlogTask(Blog blog, MediaLibrary library) {
		this.blog = blog;
		this.blogEntry = library;
		this.library = library;
		
		//adding multimedia connection
		Vector mediaObjects = library.getMediaObjects();
		for (int i =0; i < mediaObjects.size(); i++ ) {
			MediaEntry tmp = (MediaEntry) mediaObjects.elementAt(i);
			
			BlogConn mediaConnection = new NewMediaObjectConn (blog.getXmlRpcUrl(), 
					blog.getUsername(),blog.getPassword(), blog.getId(), tmp);
			
			if(blog.isHTTPBasicAuthRequired()) {
				mediaConnection.setHttp401Password(blog.getHTTPAuthPassword());
				mediaConnection.setHttp401Username(blog.getHTTPAuthUsername());
			}
			executionQueue.push(mediaConnection);
		}
	}
	
	
	public SendToBlogTask(Blog blog, Page page, int draftPageFolder, BlogConn connection) {
		this.blog = blog;
		this.blogEntry = page;
		this.page = page;
		this.draftFolder = draftPageFolder;
		//adding multimedia connection
		Vector mediaObjects = page.getMediaObjects();
		for (int i =0; i < mediaObjects.size(); i++ ) {
			MediaEntry tmp = (MediaEntry) mediaObjects.elementAt(i);
			
			BlogConn mediaConnection = new NewMediaObjectConn (blog.getXmlRpcUrl(), 
					blog.getUsername(),blog.getPassword(), blog.getId(), tmp);
			
			if(blog.isHTTPBasicAuthRequired()) {
				mediaConnection.setHttp401Password(blog.getHTTPAuthPassword());
				mediaConnection.setHttp401Username(blog.getHTTPAuthUsername());
			}
			executionQueue.push(mediaConnection);
			
			if(blog.isHTTPBasicAuthRequired()) {
				mediaConnection.setHttp401Password(blog.getHTTPAuthPassword());
				mediaConnection.setHttp401Username(blog.getHTTPAuthUsername());
			}
			executionQueue.push(mediaConnection);
		}
		//adding the post conn
		executionQueue.push(connection);
	}

	public SendToBlogTask(Post post, int draftPostFolder, BlogConn connection) {
		this.post = post;
		this.blogEntry = post;
		this.blog = post.getBlog();
		this.draftFolder = draftPostFolder;
		//adding multimedia connection
		Vector mediaObjects = post.getMediaObjects();
		Log.trace("Found "+mediaObjects.size()+ " media objs attached to content");
		for (int i =0; i < mediaObjects.size(); i++ ) {
			MediaEntry tmp = (MediaEntry) mediaObjects.elementAt(i);
		
			BlogConn mediaConnection = new NewMediaObjectConn (blog.getXmlRpcUrl(), 
					blog.getUsername(),blog.getPassword(), blog.getId(), tmp);
			
			if(blog.isHTTPBasicAuthRequired()) {
				mediaConnection.setHttp401Password(blog.getHTTPAuthPassword());
				mediaConnection.setHttp401Username(blog.getHTTPAuthUsername());
			}
			executionQueue.push(mediaConnection);
		}
		//adding the post conn
		executionQueue.push(connection);
	}

	public void execute() {
		next();
	}
		
	private void next() {
		
		if (stopping == true)
			return;
		
		if (!executionQueue.isEmpty() && isError == false) {
			blogConn = (BlogConn) executionQueue.pop();
			
			if(blogConn instanceof NewMediaObjectConn) {
				try{
					sendMedia(blogConn);
				} catch (Exception e) {
					final String respMessage = e.getMessage();
					originalException = e;
					appendErrorMsg("Error While Sending Media: \n"+respMessage);
					isError=true;
					next(); //called next to exit
					return;
				}
			} else {
				
				String type=""; //type of action. passed to callback
				
				//sending object to blog
				if( post != null ) {
					
					//adding multimedia info to post. 
					String extendedBody = post.getExtendedBody();
					if(extendedBody != null && !extendedBody.trim().equals("")) {
						extendedBody = buildFullHtml(extendedBody);
						post.setExtendedBody(extendedBody);
					} else {
						String body = post.getBody();
						body = buildFullHtml(body);
						post.setBody(body);
					}
					if(blogConn instanceof NewPostConn) {
						((NewPostConn) blogConn).setPost(post);
						type = "NewPostConn";
					} else {
						((EditPostConn) blogConn).setPost(post);
						type = "EditPostConn";
					}
					
					addTheSignature();
					
				} else {
					
					//adding multimedia info to page. 
					String moreText = page.getMtTextMore();
					if(moreText != null && !moreText.trim().equals("")) {
						moreText= buildFullHtml(moreText);
						page.setMtTextMore(moreText);
					} else {
						String body = page.getDescription();
						body = buildFullHtml(body);
						page.setDescription(body); 
					}
					
					if(blogConn instanceof NewPageConn) {
						((NewPageConn) blogConn).setPage(page);
						type = "NewPageConn";
					} else {
						((EditPageConn) blogConn).setPage(page);
						type = "EditPageConn";
					}
					
				}
				
				blogConn.addObserver(new SendCallBack(type));
				blogConn.startConnWork();
			}
			
		} else {
			//end
			if (progressListener != null)
				progressListener.taskComplete(null);		
		}
	}

	private void addTheSignature() {
		if(post.getId()!= null) {
			Log.trace("This is not a new post, signature not necessary!");
			return;
		}
		
		//add the signature to the end of post here
		String signature = null;
		boolean needSig = false;
		Log.trace("reading the signature settings from blog setting");
		//read the value from blog
		needSig = blog.isSignatureEnabled();
		signature = blog.getSignature();
		if(needSig &&  signature == null) {
			ResourceBundle _resources = WordPressCore.getInstance().getResourceBundle();
			signature = _resources.getString(WordPressResource.DEFAULT_SIGNATURE);
		}
				
		if(needSig && signature != null) {
			Log.trace("adding signature to the post body");
			String extendedBody = post.getExtendedBody();
			if(extendedBody != null && !extendedBody.trim().equals("")) {
				extendedBody+= " <p class=\"post-sig\">"+signature+"</p>";
				post.setExtendedBody(extendedBody);
			} else {
				String bodyWithoutSign = post.getBody();
				bodyWithoutSign += " <p class=\"post-sig\">"+signature+"</p>";
				post.setBody(bodyWithoutSign);
			}
		}
	}

	private void prepareImage(MediaEntry mediaEntry) throws Exception {
		String filePath = mediaEntry.getFilePath();
		byte[] photosBytes;
		int h, w;
		String MIMEtype = "";
		
		photosBytes = JSR75FileSystem.readFile(filePath);
		boolean isRes = false;
		int imageResizeWidth = 0;
		int imageResizeHeight = 0;
		
		isRes = blog.isResizePhotos(); //get the option from the blog settings
		if(isRes) {
			int selectedResizeOption = blog.getImageResizeSetting().intValue();
			if (selectedResizeOption == BlogInfo.ALWAYS_ASK_IMAGE_RESIZE_SETTING) {
				//read the value from the image if it was set
				if(((PhotoEntry)mediaEntry).getResizeHeight() == null ||  
						((PhotoEntry)mediaEntry).getResizeWidth() == null ) {
					isRes = false;
				} else {
						imageResizeWidth = ((PhotoEntry)mediaEntry).getResizeWidth().intValue();
						imageResizeHeight = ((PhotoEntry)mediaEntry).getResizeHeight().intValue();
					}
			} else {
				//read the value from the blogOptions is available
				int[] blogResizeValues = blog.getDefaultImageResizeSettings(selectedResizeOption);
				imageResizeWidth = blogResizeValues[0];
				imageResizeHeight = blogResizeValues[1];
			}
		}
		
		if(isRes){
			Hashtable content;
			
			try { //the resize function can throw "out of memory error" from JVM
				
				String fileName = mediaEntry.getFileName() != null ? mediaEntry.getFileName() : filePath;
				
				if(imageResizeWidth <= 0 || imageResizeHeight <= 0)
				{
					content = ImageUtils.resizePhoto(photosBytes, fileName, this, ImageUtils.DEFAULT_RESIZE_WIDTH, ImageUtils.DEFAULT_RESIZE_HEIGHT);
					Log.trace("img resize settings are NOT valid, using the default");
				} else {
					content = ImageUtils.resizePhoto(photosBytes, fileName, this, imageResizeWidth, imageResizeHeight);
				}
			} catch (Error  err) { //capturing the JVM error. 
	    		Log.error(err, "Error while resizing");
	    		throw new IOException("Sorry, we are unable to resize your images");
	    	}
			
			//resizing img is a long task. if user has stoped the operation..
			if (stopping == true)
				return;
			
			//save the res img in a temp file
			photosBytes = (byte[]) content.get("bits");
			String imageTempFilePath = AppDAO.getImageTempFilePath();
			if(JSR75FileSystem.isFileExist(imageTempFilePath)) {
				JSR75FileSystem.removeFile(imageTempFilePath);
			}
			JSR75FileSystem.write(imageTempFilePath, photosBytes);
			
			h = Integer.parseInt( (String) content.get("height") );
			w = Integer.parseInt( (String) content.get("width") );
			mediaEntry.setFilePath(imageTempFilePath);
			MIMEtype = (String) content.get("type");
			
			if( content.get("name") != null ) {
				mediaEntry.setFileName( (String) content.get("name"));
			}
			
		} else {
			EncodedImage imgTmp = EncodedImage.createEncodedImage(photosBytes, 0, -1);
			h = imgTmp.getHeight();
			w = imgTmp.getWidth();
			MIMEtype = imgTmp.getMIMEType();
		}
		
		mediaEntry.setMIMEType(MIMEtype);
		mediaEntry.setWidth(w);
		mediaEntry.setHeight(h);
				
		//rotating the image
		PhotoEntry currentPhoto = (PhotoEntry)mediaEntry;
		int rotationAngle = currentPhoto.getRotationAngle();
		if (rotationAngle == 0) return;
		filePath = mediaEntry.getFilePath(); //may not be the original file location
		photosBytes = JSR75FileSystem.readFile(filePath);
		try { //the rotate function can throw "out of memory error" from JVM
			
			String fileName = mediaEntry.getFileName() != null ? mediaEntry.getFileName() : filePath;
			Hashtable rotatedContentInfo = ImageUtils.rotatePhoto(photosBytes, rotationAngle, fileName);
			//save the img in a temp file
			photosBytes = (byte[]) rotatedContentInfo.get("bits");
			String imageTempFilePath = AppDAO.getImageTempFilePath();
			if(JSR75FileSystem.isFileExist(imageTempFilePath)) {
				JSR75FileSystem.removeFile(imageTempFilePath);
			}
			JSR75FileSystem.write(imageTempFilePath, photosBytes);

			h = Integer.parseInt( (String) rotatedContentInfo.get("height") );
			w = Integer.parseInt( (String) rotatedContentInfo.get("width") );
			mediaEntry.setFilePath(imageTempFilePath);
			MIMEtype = (String) rotatedContentInfo.get("type");
			mediaEntry.setMIMEType(MIMEtype);
			mediaEntry.setWidth(w);
			mediaEntry.setHeight(h);
			
			if( rotatedContentInfo.get("name") != null ) {
				mediaEntry.setFileName( (String) rotatedContentInfo.get("name"));
			}

		} catch (OutOfMemoryError e) { //capturing OutOfMemoryError error. 
			Log.error(e, "OutOfMemoryError while rotating images");
			throw new IOException("Unable to rotate images. Probably one or more images are far too big. You can set the image resizing option within the setting screen, this will allow you to rotate images.");
		} catch (Error  err) { //capturing the JVM error. 
			Log.error(err, "Error while rotating the photo:");
			throw new IOException("Sorry, we are unable to rotate your images");
		}
	}
	
	private void sendMedia(BlogConn blogConn) throws IOException, RecordStoreException, Exception {
		MediaEntry mediaEntry = ((NewMediaObjectConn) blogConn).getMediaObj();
		String filePath = mediaEntry.getFilePath();
		Log.trace("Sending file: "+filePath);
		SendMediaCallBack sendCallBack= null;
		//cut off the video resize
		if (mediaEntry instanceof PhotoEntry) {
			Log.trace("Media file is an img content");
			String oldFilePath = mediaEntry.getFilePath(); //save the old file path before resize
			prepareImage(mediaEntry);
			sendCallBack = new SendMediaCallBack(mediaEntry);
			sendCallBack.setOldFilePath(oldFilePath); //set the old file path, because error during resize/sending
		}else if (mediaEntry instanceof VideoEntry) {
			Log.trace("Media file is a video content");
			//set the right content type based on the file extension
			String[] split = StringUtils.split(mediaEntry.getFilePath(), ".");
			String ext = split[split.length-1];
			String videoMIMEType = MultimediaUtils.getFileMIMEType(ext);
			mediaEntry.setMIMEType(videoMIMEType);
			sendCallBack = new SendMediaCallBack(mediaEntry);
		} else {
			Log.trace("Media file is an audio content");
			//set the right content type based on the file extension
			String[] split = StringUtils.split(mediaEntry.getFilePath(), ".");
			String ext = split[split.length-1];
			String audioMIMEType = MultimediaUtils.getFileMIMEType(ext);
			mediaEntry.setMIMEType(audioMIMEType);
			sendCallBack = new SendMediaCallBack(mediaEntry);
		}
					
		blogConn.addObserver(sendCallBack);
		blogConn.setConnPriority(Thread.MIN_PRIORITY); 
		blogConn.startConnWork();						
	}	
	
	//callback for send post to the blog
	private class SendCallBack implements Observer{
		String type = "";
	
		public SendCallBack(String type) {
			super();
			this.type = type;
		}

		public void update(Observable observable, final Object object) {
			BlogConnResponse resp = (BlogConnResponse) object;

			// if(resp.isStopped()){
			if (stopping) { // stopping on task not on connection
				// stopping = true;
			} else if (!resp.isError()) {

				// response from post
				try {
					if (post != null) {
						// delete post from draft after sending
						DraftDAO.removePost(blog, draftFolder);
						if(resp.getResponseObject() != null) { //safety check
							
							if(type.equalsIgnoreCase("NewPostConn")) {
								//add post on disk
								String postID = String.valueOf(resp.getResponseObject());
								Log.info("new post was added to blog with id: "+postID);
								post.setId(postID); //update the post ID
								if(post.getAuthoredOn() == null) {
									long gmtTime = CalendarUtils.adjustTimeFromDefaultTimezone(System.currentTimeMillis());
									post.setAuthoredOn(gmtTime);
								}
								Vector recentPostTitles = blog.getRecentPostTitles();
								Hashtable post2Hashtable = DraftDAO.post2Hashtable(post);
								
								//we are now calling the server with a getPost, just after the pub, so we should check if it is scheduled
				            	 if(post.getStatus() != null && post.getStatus().equalsIgnoreCase("publish")) {
				            		 Date righNowDate = new Date();//this date is NOT at GMT timezone 
				            		 long righNow = CalendarUtils.adjustTimeFromDefaultTimezone(righNowDate.getTime());

				            		 long postDateLong = post.getAuthoredOn().getTime();
				            		 if(postDateLong > righNow) //set post as scheduled
				            			 post2Hashtable.put("post_status", "future");
				            	 }

				            	recentPostTitles.insertElementAt(post2Hashtable, 0);
								blog.setRecentPostTitles(recentPostTitles); 
							} else {
								//update previous post on disk
								String responseValue=String.valueOf(resp.getResponseObject());
								Vector recentPostTitles = blog.getRecentPostTitles();
								if(responseValue != null)
									for (int i = 0; i < recentPostTitles.size(); i++) {
										Hashtable postData = (Hashtable) recentPostTitles.elementAt(i);
										String tmpPostID =(String) postData.get("postid");
										if(tmpPostID.equalsIgnoreCase(post.getId())){
											recentPostTitles.setElementAt(DraftDAO.post2Hashtable(post), i);
											break;
										}
									}
								blog.setRecentPostTitles(recentPostTitles);
							}
						}
						BlogDAO.updateBlog(blog);							
					} else {
						//remove draft page on disk
						PageDAO.removePage(blog, draftFolder);
					}
				} catch (IOException e) {
					originalException = e;
					final String respMessage = e.getMessage();
					errorMsg.append(respMessage + "\n");
				} catch (RecordStoreException e) {
					originalException = e;
					final String respMessage = e.getMessage();
					errorMsg.append(respMessage + "\n");
				} catch (Exception e) {
					originalException = e;
					final String respMessage = e.getMessage();
					errorMsg.append(respMessage + "\n");
				}	
			} else {
				final String respMessage = resp.getResponse();
				errorMsg.append(respMessage + "\n");
				isError = true;
				originalException = (resp.getResponseObject() instanceof Exception) ? (Exception)resp.getResponseObject() : null;
			}
			next(); // call to next
		}
	}
	
	
	
	//callback for send post to the blog
	private class SendMediaCallBack implements Observer {

		private MediaEntry mediaObj;
		private String oldFilePath = null;

		public void setOldFilePath(String oldFilePath) {
			this.oldFilePath = oldFilePath;
		}

		SendMediaCallBack(MediaEntry mediaObj){
			this.mediaObj = mediaObj;
		}

		public void update(Observable observable, final Object object) {

			try {
				BlogConnResponse resp= (BlogConnResponse) object;

				if(stopping){ //stopping on task not on connection

				} else if(!resp.isError()) {				
					Hashtable content =(Hashtable)resp.getResponseObject();
					String file = (String)content.get("file");
					String url = (String)content.get("url");
					mediaObj.setFileURL(url);
					mediaObj.setFileName(file);
					//get the videopress shortcode if it is in response
					String videoPressShortCode = (String)content.get("videopress_shortcode");
					if(videoPressShortCode != null && !videoPressShortCode.trim().equals(""))
						mediaObj.setVideoPressShortCode(videoPressShortCode);

				} else { //there was an error

					/*check for error code and videopress/space upgrade*/
					if (resp.getResponseObject() instanceof XmlRpcException && 
							((XmlRpcException) resp.getResponseObject()).code == 500) {
						
						ResourceBundleFamily _resources = ResourceBundle.getBundle(WordPressResource.BUNDLE_ID, WordPressResource.BUNDLE_NAME);

						String respMessage = null;
						if (mediaObj instanceof VideoEntry){
							respMessage = _resources.getString(WordPressResource.ERROR_VP_UPGRADE);
						} else if (mediaObj instanceof AudioEntry && blog.isWPCOMBlog()){ 
							respMessage = _resources.getString(WordPressResource.ERROR_SPACE_UPGRADE);
						} else {
							respMessage = resp.getResponse();
						}
						errorMsg.append(respMessage+"\n");
						isError=true;
					} else {
						originalException = (resp.getResponseObject() instanceof Exception) ? (Exception)resp.getResponseObject() : null;
						final String respMessage=resp.getResponse();
						errorMsg.append(respMessage+"\n");
						isError=true;
					}
				}
			} catch (Exception e) {
				originalException = e;
				errorMsg.append("Server Response Error on media upload\n");
				isError=true;
			}

			//set the path of the media to the real path, not to the tmp path used for resize
			if(this.oldFilePath != null)
				mediaObj.setFilePath(oldFilePath);

			//remove the tmp image file
			try {
				String imageTempFilePath = AppDAO.getImageTempFilePath();
				if(JSR75FileSystem.isFileExist(imageTempFilePath)) {
					JSR75FileSystem.removeFile(imageTempFilePath);
				}
			} catch (Exception e) {
			}

			next(); // call to next
		}
	}
	

	/**
	 * Build the Full post/page html. 
	 * @param 
	 * @return
	 */
	private synchronized String buildFullHtml(String html) {
		StringBuffer topMediaFragment = new StringBuffer();
		StringBuffer bottomMediaFragment = new StringBuffer();
		Vector mediaObjects;
		
		mediaObjects = blogEntry.getMediaObjects();
		
		for (int i = 0; i < mediaObjects.size(); i++) {

			MediaEntry remoteFileInfo = (MediaEntry)mediaObjects.elementAt(i);
			StringBuffer tmpBuff = null;
			if(remoteFileInfo.isVerticalAlignmentOnTop())
				tmpBuff  = 	topMediaFragment;
			else
				tmpBuff  = 	bottomMediaFragment;
			
			String currentMediaObjHTML;
			if (remoteFileInfo instanceof VideoEntry) {
				currentMediaObjHTML = buildVideoHTML(remoteFileInfo);
			} else {
				currentMediaObjHTML = remoteFileInfo.getMediaObjectAsHtml();
			}
			tmpBuff.append(currentMediaObjHTML);
		}
		return topMediaFragment.toString() + html + bottomMediaFragment.toString();
	}


	private String buildVideoHTML(MediaEntry remoteFileInfo) {
		boolean isRes = false;
		Integer videoResizeWidth = null;
		Integer videoResizeHeight = null;

		//read values from blog settings
		isRes = blog.isResizeVideos();
		videoResizeWidth = blog.getVideoResizeWidth();
		videoResizeHeight = blog.getVideoResizeHeight();
		
		String currentMediaObjHTML = remoteFileInfo.getMediaObjectAsHtml();
		if (isRes && currentMediaObjHTML.endsWith("]")) {
			String resizeString = "";
			//video press tag with resizing enable
			if(videoResizeWidth != null && videoResizeWidth.intValue() != 0)
			{
				resizeString = " w="+videoResizeWidth.toString();
			}
			
			if(videoResizeHeight != null && videoResizeHeight.intValue() != 0)
			{
				resizeString += " h="+videoResizeHeight.toString();
			}
			resizeString+= "]";
			
			currentMediaObjHTML = StringUtils.replaceLast(currentMediaObjHTML, "]", resizeString);
		}
		return currentMediaObjHTML;
	}
	
	
	public void stop() {
		super.stop();
		//stop the connection underlying
		blogConn.stopConnWork();
	}
	
	/**
	 * Build the html fragment for the photos
	 * @param remoteFileInfo  contain files info on the WP server (used after sending MM files to the blog)
	 * @return
	 
	private synchronized String buildHtmlPhotoFragment() {
		StringBuffer photoFragment = new StringBuffer();
		Vector mediaObjects;
		if(post != null)
			mediaObjects = post.getMediaObjects();
		else 
			mediaObjects = page.getMediaObjects();
		
		for (int i = 0; i < mediaObjects.size(); i++) {

			MediaEntry remoteFileInfo = (MediaEntry)mediaObjects.elementAt(i);

			photoFragment.append("<p><a href=\""+remoteFileInfo.getFileURL()+"\">" +
							"<img class=\"alignnone size-full wp-image-364\"" +
							" src=\""+remoteFileInfo.getFileURL()+"\" alt=\"\" " +
							"width=\""+remoteFileInfo.getWidth()+"\" height=\""+remoteFileInfo.getHeight()+"\" />" +
									"</a></p>");
		}
		
		return photoFragment.toString();
	} */
}
