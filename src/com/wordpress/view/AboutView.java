package com.wordpress.view;

import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.BitmapField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.container.HorizontalFieldManager;
import net.rim.device.api.ui.container.VerticalFieldManager;

import com.wordpress.bb.WordPressResource;
import com.wordpress.controller.AboutController;
import com.wordpress.controller.BaseController;
import com.wordpress.utils.PropertyUtils;
import com.wordpress.view.container.VerticalPaddedFieldManager;

public class AboutView extends BaseView {
	private AboutController controller;
	private LabelField urlAddr;

    public AboutView(AboutController _aboutController) {
    	super(_resources.getString(WordPressResource.TITLE_ABOUT_VIEW));
		controller = _aboutController;
    	
		//Bitmap _bitmap = Bitmap.getBitmapResource("application-icon.png");
        Bitmap _bitmap = PropertyUtils.getAppIcon();
    	String name = PropertyUtils.getAppName();
        
    	String version = PropertyUtils.getAppVersion(); //read from the alx files
        if(version == null || version.trim().equals("")) { //read value from jad file
        	//MIDlet-Version
        	version = PropertyUtils.getIstance().get("MIDlet-Version");
        	if(version == null)
        		version = "";
        }
        
    	HorizontalFieldManager row= new HorizontalFieldManager(Manager.VERTICAL_SCROLL);
    	VerticalFieldManager col1= new VerticalFieldManager();
    	final VerticalFieldManager col2= new VerticalPaddedFieldManager();
    	    	
    	BitmapField wpLogo = new BitmapField(_bitmap, Field.FIELD_HCENTER | Field.FIELD_VCENTER);
    	wpLogo.setSpace(5, 5);
		col1.add(wpLogo);    	
    	LabelField titleField = new LabelField(_resources.getString(WordPressResource.TITLE_APPLICATION));
    	Font fnt = this.getFont().derive(Font.BOLD);
    	titleField.setFont(fnt);
		col2.add(titleField);
    	
    	col2.add(new LabelField(_resources.getString(WordPressResource.ABOUTVIEW_VERSION)+" "+version));
    	col2.add(new LabelField("",Field.FOCUSABLE)); //space after title
    	
    	BasicEditField descriptionField = new BasicEditField(BasicEditField.READONLY);
    	descriptionField.setText(_resources.getString(WordPressResource.ABOUTVIEW_DESC));
    	col2.add(descriptionField);
    	col2.add(new LabelField("",Field.FOCUSABLE)); //space after description
    	
    	BasicEditField developedByField = new BasicEditField(BasicEditField.READONLY);
    	developedByField.setText(_resources.getString(WordPressResource.ABOUTVIEW_DEVELOPED_BY));
    	col2.add(developedByField);
    	col2.add(new LabelField("",Field.FOCUSABLE)); //space after description
    	
    	
    	BasicEditField moreInfoField = new BasicEditField(BasicEditField.READONLY);
    	moreInfoField.setText(_resources.getString(WordPressResource.ABOUTVIEW_MORE_INFO));
    	col2.add(moreInfoField);
    	urlAddr = GUIFactory.createURLLabelField("http://blackberry.wordpress.org", "http://blackberry.wordpress.org", LabelField.FOCUSABLE);
    	col2.add(urlAddr);
    	Bitmap img = Bitmap.getBitmapResource("aboutscreenfooter.png");
    	BitmapField bf = new BitmapField(img);    	
    	col2.add(bf);
    	col2.add(new LabelField("",Field.FOCUSABLE));
    	
    	row.add(col1);
    	row.add(col2);
    	this.add(row);
    }
    
	public boolean onMenu(int instance) {
		boolean result;
		// Prevent the context menu from being shown if focus
		// is on the url field
		if (getLeafFieldWithFocus() == urlAddr && instance == Menu.INSTANCE_CONTEXT) {
			result = false;
		} else {
			result = super.onMenu(instance);
		}
		return result;
	}

    
	public BaseController getController() {
		return controller;
	}
}