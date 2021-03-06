//#preprocess
package com.wordpress.view;

import net.rim.device.api.system.Display;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.container.VerticalFieldManager;

//#ifdef VER_4.7.0 | BlackBerrySDK5.0.0 | BlackBerrySDK6.0.0 | BlackBerrySDK7.0.0
import net.rim.device.api.ui.Touchscreen;
//#endif

import com.wordpress.controller.BaseController;


/**
 * Base view with an image in background
 * @author dercoli
 *
 */
public abstract class StandardBaseView extends BaseView {

	protected VerticalFieldManager _container;
	
	public StandardBaseView(long style) {
		super(style);
		initForegroundManager();
	}
	
	public StandardBaseView(String title, long style) {
		super(title, style);
		initForegroundManager();
	}
		
	private void initForegroundManager() {
		
	  	VerticalFieldManager internalManager = new VerticalFieldManager( Manager.NO_VERTICAL_SCROLL | Manager.NO_VERTICAL_SCROLLBAR ) {
    		public void paintBackground( Graphics g ) {
    			g.clear();
    			int color = g.getColor();
    			g.setColor(0xefebef);
    			//g.drawBitmap(0, 0, Display.getWidth(), Display.getHeight(), _backgroundBitmap, 0, 0);
    			g.fillRect( 0, 0, Display.getWidth(), Display.getHeight() );
    			g.setColor( color );
    		//	g.clear();
    		//	g.drawBitmap(0, 0, Display.getWidth(), Display.getHeight(), _backgroundBitmap, 0, 0);
    		}
    		
    		protected void sublayout( int maxWidth, int maxHeight ) {
    			int titleFieldHeight = 0;
    			if ( titleField != null ) {
    				titleFieldHeight = titleField.getHeight();
    			}
    		    //#ifdef VER_4.7.0 | BlackBerrySDK5.0.0 | BlackBerrySDK6.0.0 | BlackBerrySDK7.0.0
    			if (Touchscreen.isSupported() == true && bottomButtonsManager != null && isBottomBarVisible) {
    				titleFieldHeight += bottomButtonsManager.getHeight();
    			}  			   				
    			//#endif
    			
    			int displayWidth = Display.getWidth(); 
    			int displayHeight = Display.getHeight();
    			
    			super.sublayout( displayWidth, displayHeight - titleFieldHeight );
    			setExtent( displayWidth, displayHeight - titleFieldHeight );
    		}
    		
    	};
    	
    	_container = new VerticalFieldManager( Manager.VERTICAL_SCROLL | Manager.VERTICAL_SCROLLBAR );
    	internalManager.add( _container );
    	super.add( internalManager );
	}
	
	public void add( Field field ) {
		_container.add( field );
	}
	
	public void delete( Field field ) {
		_container.delete( field );
	}
	
	
	public Field getField(int index) {
		return _container.getField( index );
	}
	
	public boolean isDirty() {
		return _container.isDirty();
	}
	
	public void setDirty(boolean value) {
		_container.setDirty(value);
	}
	
	public int getFieldCount() {
		return _container.getFieldCount();
	}
	
	public void insert(Field field, int index) {
		 _container.insert(field, index);
	}
	
    //return the controller associated with this view
    public abstract BaseController getController();
}