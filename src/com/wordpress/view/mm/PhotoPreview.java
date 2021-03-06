package com.wordpress.view.mm;

 import net.rim.device.api.system.EncodedImage;
import net.rim.device.api.ui.MenuItem;

import com.wordpress.bb.WordPressResource;
import com.wordpress.controller.BaseController;
import com.wordpress.controller.BlogObjectController;
import com.wordpress.view.BaseView;

 public class PhotoPreview extends BaseView {
 	
    private BlogObjectController controller; //controller associato alla view
	private final String key;
 		
     public PhotoPreview(BlogObjectController _controller, String key, EncodedImage Img) {
     	super(_resources.getString(WordPressResource.TITLE_PREVIEW));
     	this.controller=_controller;
		this.key = key;

		ScrollableImageField scrollableImgField= new ScrollableImageField(Img);         
        add(scrollableImgField); 
        addMenuItem(_deletePhotoItem);
     }
     

     private MenuItem _deletePhotoItem = new MenuItem( _resources, WordPressResource.MENUITEM_MEDIA_REMOVE, 130, 10) {
         public void run() {
        	 controller.deleteLinkToMediaObject(key);
        	 controller.backCmd();
         }
     };     
     
 	public BaseController getController() {
 		return controller;
 	} 	
 }
