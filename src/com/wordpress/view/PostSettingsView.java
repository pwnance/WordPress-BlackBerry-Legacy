package com.wordpress.view;

import java.util.Date;
import java.util.TimeZone;

import net.rim.device.api.i18n.SimpleDateFormat;
import net.rim.device.api.system.Display;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.component.CheckboxField;
import net.rim.device.api.ui.component.DateField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.PasswordEditField;
import net.rim.device.api.ui.container.HorizontalFieldManager;
import net.rim.device.api.ui.container.VerticalFieldManager;

import com.wordpress.bb.WordPressResource;
import com.wordpress.controller.BaseController;
import com.wordpress.controller.BlogObjectController;
import com.wordpress.utils.CalendarUtils;
import com.wordpress.view.component.BorderedFieldManager;
import com.wordpress.view.component.HorizontalPaddedFieldManager;


public class PostSettingsView extends BaseView {
	
    private BlogObjectController controller; //controller associato alla view
    private VerticalFieldManager _container;
    private DateField  authoredOn;
    private PasswordEditField passwordField;
	private CheckboxField resizePhoto;
    
    
    public PostSettingsView(BlogObjectController _controller, Date postAuth, String password, boolean isResImg) {
    	super(_resources.getString(WordPressResource.MENUITEM_SETTINGS));
    	this.controller=_controller;
    	
     	VerticalFieldManager internalManager = new VerticalFieldManager( Manager.NO_VERTICAL_SCROLL | Manager.NO_VERTICAL_SCROLLBAR ) {
    		public void paintBackground( Graphics g ) {
    			g.clear();
    			int color = g.getColor();
    			g.setColor( Color.LIGHTGREY );
    			g.drawBitmap(0, 0, Display.getWidth(), Display.getHeight(), _backgroundBitmap, 0, 0);
    			//g.fillRect( 0, 0, Display.getWidth(), Display.getHeight() );
    			g.setColor( color );
    		}
    		
    		protected void sublayout( int maxWidth, int maxHeight ) {
    			
    			int titleFieldHeight = 0;
    			if ( titleField != null ) {
    				titleFieldHeight = titleField.getHeight();
    			}
    			
    			int displayWidth = Display.getWidth(); // I would probably make these global
    			int displayHeight = Display.getHeight();
    			
    			super.sublayout( displayWidth, displayHeight - titleFieldHeight );
    			setExtent( displayWidth, displayHeight - titleFieldHeight );
    		}
    		
    	};
    	
    	_container = new VerticalFieldManager( Manager.VERTICAL_SCROLL | Manager.VERTICAL_SCROLLBAR );
    	internalManager.add( _container );
    	super.add( internalManager );
    	
    	
    	long datetime = new Date().getTime();;
    	if(postAuth != null ) {
    		datetime = CalendarUtils.adjustTimeToDefaultTimezone(postAuth.getTime());
    	}
			
		//row date 
        BorderedFieldManager rowDate = new BorderedFieldManager(
        		Manager.NO_HORIZONTAL_SCROLL
        		| Manager.NO_VERTICAL_SCROLL
        		| BorderedFieldManager.BOTTOM_BORDER_NONE);
        
        HorizontalFieldManager innerContainerForDateField = new HorizontalPaddedFieldManager(HorizontalFieldManager.NO_HORIZONTAL_SCROLL 
                | HorizontalFieldManager.NO_VERTICAL_SCROLL | HorizontalFieldManager.USE_ALL_WIDTH);

		LabelField lblDate = getLabel(_resources.getString(WordPressResource.LABEL_POST_PUBLISHEDON));   
	    authoredOn= new DateField("", datetime, DateField.DATE_TIME);
	    authoredOn.setTimeZone(TimeZone.getDefault()); //setting the field time zone
		SimpleDateFormat sdFormat = new SimpleDateFormat(_resources.getString(WordPressResource.DEFAULT_DATE_FORMAT));
	    authoredOn.setFormat(sdFormat);
	    innerContainerForDateField.add(lblDate);
	    innerContainerForDateField.add(authoredOn);
	    
	    rowDate.add(innerContainerForDateField);
		add(rowDate); 
		
        //row password
        BorderedFieldManager rowPassword = new BorderedFieldManager(
        		Manager.NO_HORIZONTAL_SCROLL
        		| Manager.NO_VERTICAL_SCROLL
        		| BorderedFieldManager.BOTTOM_BORDER_NONE);
		LabelField lblPassword = getLabel(_resources.getString(WordPressResource.LABEL_PASSWD));		
        passwordField = new PasswordEditField("", password, 64, Field.EDITABLE);
        rowPassword.add(lblPassword);
        rowPassword.add(passwordField);
        //LabelField that displays text in the specified color.
		LabelField lblDesc = getLabel(_resources.getString(WordPressResource.DESCRIPTION_POST_PASSWORD)); 
		Font fnt = this.getFont().derive(Font.ITALIC);
		lblDesc.setFont(fnt);
		rowPassword.add(lblDesc);
		add(rowPassword);
		
		//resize photo sections
		BorderedFieldManager rowPhotoRes = new BorderedFieldManager(
				Manager.NO_HORIZONTAL_SCROLL
				| Manager.NO_VERTICAL_SCROLL);
		resizePhoto=new CheckboxField(_resources.getString(WordPressResource.LABEL_RESIZEPHOTOS), isResImg);
		rowPhotoRes.add(resizePhoto);

		//LabelField that displays text in the specified color.
		LabelField lblDescResize = getLabel(_resources.getString(WordPressResource.DESCRIPTION_RESIZEPHOTOS)); 
		lblDescResize.setFont(fnt);
		rowPhotoRes.add(lblDescResize);
		add(rowPhotoRes);
		
		//this.add(new LabelField("", Field.NON_FOCUSABLE)); //space before buttons
    }
    
	public void add( Field field ) {
		_container.add( field );
	}
    
    //override onClose() to display a dialog box when the application is closed    
	public boolean onClose()   {

		if(authoredOn.isDirty()) {
			long gmtTime = CalendarUtils.adjustTimeFromDefaultTimezone(authoredOn.getDate());
			controller.setAuthDate(gmtTime);
		}
		
		if(passwordField.isDirty()) {
			controller.setPassword(passwordField.getText());
		}
		
		if(resizePhoto.isDirty()){
			controller.setPhotoResizing(resizePhoto.getChecked());
		}
		
		controller.backCmd();
		return true;
    }
	
	public BaseController getController() {
		return controller;
	}
}