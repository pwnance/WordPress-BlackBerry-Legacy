package com.wordpress.view;

import java.util.Hashtable;

import net.rim.device.api.io.http.HttpHeaders;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.CheckboxField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.ObjectChoiceField;
import net.rim.device.api.ui.component.PasswordEditField;
import net.rim.device.api.ui.container.HorizontalFieldManager;
import net.rim.device.api.ui.text.URLTextFilter;

import com.wordpress.bb.WordPressResource;
import com.wordpress.controller.AddBlogsController;
import com.wordpress.controller.BaseController;
import com.wordpress.utils.Tools;
import com.wordpress.view.component.BorderedFieldManager;

public class AddBlogsView extends StandardBaseView {
	
    private AddBlogsController controller= null;
	private BasicEditField blogUrlField;
	private BasicEditField userNameField;
	private PasswordEditField passwordField;
	private ObjectChoiceField  maxRecentPost;
	private CheckboxField resizePhoto;
	private BasicEditField imageResizeWidthField;
	private BasicEditField imageResizeHeightField;
	
	public boolean isResizePhoto(){
		return resizePhoto.getChecked();
	}
	
	public Integer getImageResizeWidth() {
		return Integer.valueOf(imageResizeWidthField.getText());
	}
	
	public Integer getImageResizeHeight() {
		return Integer.valueOf(imageResizeHeightField.getText());
	}
	
	public String getBlogUrl() {
		return blogUrlField.getText();
	}
	
	public void setBlogUrl(String newUrl) {
		 blogUrlField.setText(newUrl);
	}
	
	public String getBlogUser() {
		return userNameField.getText();
	}
	
	public String getBlogPass() {
		return passwordField.getText();
	}
	
	public int getMaxRecentPostIndex() {
		return maxRecentPost.getSelectedIndex();
	}
	

	
	public AddBlogsView(AddBlogsController addBlogsController, Hashtable values) {
	    	super(_resources.getString(WordPressResource.TITLE_ADDBLOGS), Manager.NO_VERTICAL_SCROLL | Manager.NO_VERTICAL_SCROLLBAR);
	    	this.controller=addBlogsController;
	        
	        //loading input data
	        String user= (String)values.get("user");
	        String pass= (String)values.get("pass");
	        String url= (String)values.get("url");
	        String[] recentPost=(String[])values.get("recentpost");
	        int recentPostSelect= ((Integer)values.get("recentpostselected")).intValue();
			boolean isResImg= ((Boolean)values.get("isresphotos")).booleanValue();
			Integer imageResizeWidth = (Integer)values.get("imageResizeWidth");
			Integer imageResizeHeight = (Integer)values.get("imageResizeHeight");

	        //end loading
            //row url
			BorderedFieldManager rowURL = new BorderedFieldManager(
	        		Manager.NO_HORIZONTAL_SCROLL
	        		| Manager.NO_VERTICAL_SCROLL
	        		| BorderedFieldManager.BOTTOM_BORDER_NONE);
            //HorizontalFieldManager rowURL = new HorizontalFieldManager();
    		LabelField lblUrl = getLabel(_resources.getString(WordPressResource.LABEL_URL)); 
            blogUrlField = new BasicEditField("", url, 100, Field.EDITABLE);
            blogUrlField.setFilter(new URLTextFilter());
            if(blogUrlField.getTextLength() > 0)
            	blogUrlField.setCursorPosition(blogUrlField.getTextLength());//set the cursor at the end
            rowURL.add(lblUrl);
            rowURL.add(blogUrlField);
            add(rowURL);
            
            //row username
            BorderedFieldManager rowUserName = new BorderedFieldManager(
	        		Manager.NO_HORIZONTAL_SCROLL
	        		| Manager.NO_VERTICAL_SCROLL
	        		| BorderedFieldManager.BOTTOM_BORDER_NONE);
    		LabelField lblUserName = getLabel(_resources.getString(WordPressResource.LABEL_USERNAME)); 
            userNameField = new BasicEditField("", user, 60, Field.EDITABLE);
            rowUserName.add(lblUserName);
    		rowUserName.add(userNameField);
    		add(rowUserName);
    		
            //row password
            BorderedFieldManager rowPassword = new BorderedFieldManager(
	        		Manager.NO_HORIZONTAL_SCROLL
	        		| Manager.NO_VERTICAL_SCROLL
	        		| BorderedFieldManager.BOTTOM_BORDER_NONE);
    		LabelField lblPassword = getLabel(_resources.getString(WordPressResource.LABEL_PASSWD)); 
            passwordField = new PasswordEditField("", pass, 64, Field.EDITABLE);
            rowPassword.add(lblPassword);
            rowPassword.add(passwordField);
            add(rowPassword);

            //row max recent post
            BorderedFieldManager rowMaxRecentPost = new BorderedFieldManager(
	        		Manager.NO_HORIZONTAL_SCROLL
	        		| Manager.NO_VERTICAL_SCROLL
	        		| BorderedFieldManager.BOTTOM_BORDER_NONE);
            maxRecentPost = new ObjectChoiceField (_resources.getString(WordPressResource.LABEL_MAXRECENTPOST), recentPost,recentPostSelect);
            rowMaxRecentPost.add(maxRecentPost);
            add(rowMaxRecentPost);            

            //row resize photos
            BorderedFieldManager rowResizePhotos = new BorderedFieldManager(
	        		Manager.NO_HORIZONTAL_SCROLL
	        		| Manager.NO_VERTICAL_SCROLL);
    		resizePhoto=new CheckboxField(_resources.getString(WordPressResource.LABEL_RESIZEPHOTOS), isResImg);
    		rowResizePhotos.add(resizePhoto);
    		BasicEditField lblDesc = getDescriptionTextField(_resources.getString(WordPressResource.DESCRIPTION_RESIZEPHOTOS)); 
			rowResizePhotos.add(lblDesc);
			
            HorizontalFieldManager rowImageResizeWidth = new HorizontalFieldManager();
            rowImageResizeWidth.add( getLabel(_resources.getString(WordPressResource.LABEL_RESIZE_IMAGE_WIDTH)));
            imageResizeWidthField = new BasicEditField(
            		"", 
            		(imageResizeWidth == null ? "" : imageResizeWidth.toString()), 
            		4, 
            		Field.EDITABLE | BasicEditField.FILTER_NUMERIC);
            rowImageResizeWidth.add(imageResizeWidthField);
            rowResizePhotos.add(rowImageResizeWidth);
            
            HorizontalFieldManager rowImageResizeHeight = new HorizontalFieldManager();
            rowImageResizeHeight.add( getLabel(_resources.getString(WordPressResource.LABEL_RESIZE_IMAGE_HEIGHT)));
            imageResizeHeightField = new BasicEditField(
            		"", 
            		(imageResizeHeight == null ? "" : imageResizeHeight.toString()), 
            		4, 
            		Field.EDITABLE | BasicEditField.FILTER_NUMERIC);
            rowImageResizeHeight.add(imageResizeHeightField);
            rowResizePhotos.add(rowImageResizeHeight);

            add(rowResizePhotos);
            
            ButtonField buttonOK= new ButtonField(_resources.getString(WordPressResource.BUTTON_OK), ButtonField.CONSUME_CLICK);
            ButtonField buttonBACK= new ButtonField(_resources.getString(WordPressResource.BUTTON_BACK), ButtonField.CONSUME_CLICK);
    		buttonBACK.setChangeListener(addBlogsController.getBackButtonListener());
            buttonOK.setChangeListener(addBlogsController.getOkButtonListener());
            
            HorizontalFieldManager buttonsManager = new HorizontalFieldManager(Field.FIELD_HCENTER);
            buttonsManager.add(buttonOK);
            buttonsManager.add(buttonBACK);
    		add(buttonsManager); 
    		add(new LabelField("", Field.NON_FOCUSABLE)); //space after buttons
    		
            HorizontalFieldManager buttonsManagerGetFreeBlog = new HorizontalFieldManager(Field.FIELD_HCENTER);
            ButtonField buttonGetFreeBlog= new ButtonField(_resources.getString(WordPressResource.GET_FREE_BLOG), ButtonField.CONSUME_CLICK);
            buttonGetFreeBlog.setChangeListener(listenerGetBlogButton);
            buttonsManagerGetFreeBlog.add(buttonGetFreeBlog);
    		add(buttonsManagerGetFreeBlog); 
    		add(new LabelField("", Field.NON_FOCUSABLE)); //space after button
    		
    		addMenuItem(_addBlogItem);
    		addMenuItem(_getFreeBlogItem);
	}
	 
	
	private FieldChangeListener listenerGetBlogButton = new FieldChangeListener() {
	    public void fieldChanged(Field field, int context) {
	    	openWordPressSignUpURL();
	   }
	};
	
	//add blog menu item 
	private MenuItem _addBlogItem = new MenuItem( _resources, WordPressResource.MENUITEM_ADDBLOG, 140, 10) {
		public void run() {
			controller.addBlogs(0);
		}
	};
	
	private void openWordPressSignUpURL(){
		HttpHeaders headers = new HttpHeaders();
    	headers.addProperty("User-Agent", "wp-blackberry/"+ Tools.getAppVersion());
    	Tools.getBrowserSession("http://wordpress.com/signup/?ref=wp-blackberry","/wp-blackberry/AddBlogScreen", headers, null);
	}
	
	//add blog menu item 
	private MenuItem _getFreeBlogItem = new MenuItem( _resources, WordPressResource.GET_FREE_BLOG_MENU_ITEM, 150, 20) {
		public void run() {
			openWordPressSignUpURL();
		}
	};

	   //override onClose() to by-pass the standard dialog box when the screen is closed    
	public boolean onClose()   {
		return controller.discardChange();			
	}
	
	public BaseController getController() {
		return controller;
	}
}