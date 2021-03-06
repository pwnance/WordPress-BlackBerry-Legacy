//#preprocess
package com.wordpress.view;

import java.util.Vector;

import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;

import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.CheckboxField;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.SeparatorField;

//#ifdef VER_4.7.0 | BlackBerrySDK5.0.0 | BlackBerrySDK6.0.0 | BlackBerrySDK7.0.0
import net.rim.device.api.ui.VirtualKeyboard;
//#endif

import com.wordpress.bb.WordPressResource;
import com.wordpress.controller.BaseController;
import com.wordpress.controller.MediaLibraryController;
import com.wordpress.model.MediaEntry;
import com.wordpress.model.MediaLibrary;
import com.wordpress.utils.log.Log;
import com.wordpress.view.container.BorderedFieldManager;


public class MediaLibraryView extends MediaView {
	
	private MediaLibrary entry;
	
	//content of tabs summary
	private BasicEditField title;
	private CheckboxField cutAndPaste;
	
    public MediaLibraryView(MediaLibraryController _controller, MediaLibrary  entry) {
    	super(_controller);  	
		this.entry = entry;
                
    	//#ifdef VER_4.7.0 | BlackBerrySDK5.0.0 | BlackBerrySDK6.0.0 | BlackBerrySDK7.0.0
    	VirtualKeyboard virtKbd = getVirtualKeyboard();
    	if(virtKbd != null)
    		virtKbd.setVisibility(VirtualKeyboard.HIDE);
    	//#endif
		
        //row title
    	BorderedFieldManager outerManagerRowInfo = new BorderedFieldManager(Manager.NO_HORIZONTAL_SCROLL
         		| Manager.NO_VERTICAL_SCROLL | BorderedFieldManager.BOTTOM_BORDER_NONE);
		title = new BasicEditField(_resources.getString(WordPressResource.LABEL_TITLE)+": ", entry.getTitle(), 100, Field.EDITABLE) {
	    	//#ifdef VER_4.7.0 | BlackBerrySDK5.0.0 | BlackBerrySDK6.0.0 | BlackBerrySDK7.0.0
			protected void onUnfocus() {
				super.onUnfocus();
				VirtualKeyboard virtKbd = getVirtualKeyboard();
		    	if(virtKbd != null)
		    		virtKbd.setVisibility(VirtualKeyboard.HIDE);
			}
			protected void onFocus(int direction) {
				super.onFocus(direction);
				VirtualKeyboard virtKbd = getVirtualKeyboard();
		    	if(virtKbd != null)
		    		virtKbd.setVisibility(VirtualKeyboard.SHOW);
			}
			//#endif
		};
		
        outerManagerRowInfo.add(title);
    	BasicEditField lblTitleDesc = getDescriptionTextField(_resources.getString(WordPressResource.MEDIALIBRARYSCREEEN_TITLE_DESC));
    	outerManagerRowInfo.add(lblTitleDesc);
    	
    	//add the separator    		
    	SeparatorField sepatorField = GUIFactory.createSepatorField();
    	sepatorField.setMargin(5,0,5,0);
    	outerManagerRowInfo.add(sepatorField);
        
    	//add the cut&paste fields
    	cutAndPaste = new CheckboxField(_resources.getString(WordPressResource.MEDIALIBRARYSCREEEN_COPY_FILE_URL), entry.isCutAndPaste());
    	outerManagerRowInfo.add(cutAndPaste);
    	BasicEditField lblCutAndPasteDesc = getDescriptionTextField(_resources.getString(WordPressResource.MEDIALIBRARYSCREEEN_COPY_FILE_URL_DESC));
    	outerManagerRowInfo.add(lblCutAndPasteDesc);
                    
    	insert(outerManagerRowInfo, 0);
    	
		addMenuItem(_saveDraftItem);
		
		//updateThumbs 
		Vector	mediaObjects = entry.getMediaObjects();
		for (int i = 0; i < mediaObjects.size(); i++) {
			MediaEntry tmp = (MediaEntry) mediaObjects.elementAt(i);
			addMedia(tmp);
		}
		
		cutAndPaste.setFocus();
    }
       
  //override the method of photoview class
    protected void addExclusiveMenuItem(Menu menu, int instance) {
    
    }
        
    //save a local copy of post
    private MenuItem _saveDraftItem = new MenuItem( _resources, WordPressResource.MENUITEM_SAVE_LOCALDRAFT, 100220, 10) {
        public void run() {
    		try {
    			updateModel();
    			((MediaLibraryController)controller).saveLibrary();
	    		controller.backCmd();
    		} catch (Exception e) {
    			controller.displayError(e, "Error while saving media library files");
    		}
        }
    };
    
    //send post to blog
    private MenuItem _submitItem = new MenuItem( _resources, WordPressResource.MENUITEM_SEND, 100230, 10) {
        public void run() {
    		try {
    			updateModel();
    			((MediaLibraryController)controller).sendLibraryToBlog();
    		} catch (Exception e) {
    			controller.displayError(e, "Error while sending media files");
    		}
        }
    };
          
            	
  //Override the makeMenu method so we can add a custom menu item
	protected void makeMenu(Menu menu, int instance)
	{
		if ( ((MediaLibraryController)controller).isSendMenuItemAvailable() ) {
			menu.add(_submitItem);
		}
		
		//Create the default menu.
		super.makeMenu(menu, instance);
	}
    
    
	/*
	 * Update data model and Track changes.
	 * 
	 * Photos changes are tracked into controller 
	 */
	private void updateModel() throws Exception{	
		
		if(title.isDirty()){
			entry.setTitle(title.getText());
			controller.setObjectAsChanged(true);
			Log.trace("title dirty");
		}
		
		if(cutAndPaste.isDirty()) {
			entry.setCutAndPaste(cutAndPaste.getChecked());
			controller.setObjectAsChanged(true);
		}
	}

	public boolean onClose()   {
		controller.removeMediaFileJournalListener();
		try {
			updateModel();
		} catch (Exception e) {
			controller.displayError(e, _resources.getString(WordPressResource.ERROR_WHILE_SAVING_PAGE));
		}
		return ((MediaLibraryController)controller).dismissView();	
    }
	
	public BaseController getController() {
		return controller;
	}
}