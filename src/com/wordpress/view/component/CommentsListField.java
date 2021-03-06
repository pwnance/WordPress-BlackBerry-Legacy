//#preprocess
package com.wordpress.view.component;

import java.util.Vector;

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.Characters;
import net.rim.device.api.system.EncodedImage;
import net.rim.device.api.system.KeypadListener;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.DrawStyle;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
//#ifdef VER_4.7.0 | BlackBerrySDK5.0.0 | BlackBerrySDK6.0.0 | BlackBerrySDK7.0.0
import net.rim.device.api.ui.TouchGesture;
import net.rim.device.api.ui.TouchEvent;
//#endif
import net.rim.device.api.ui.component.ListField;

import com.wordpress.bb.WordPressCore;
import com.wordpress.bb.WordPressInfo;
import com.wordpress.bb.WordPressResource;
import com.wordpress.controller.GravatarController;
import com.wordpress.model.Comment;
import com.wordpress.utils.ImageUtils;
import com.wordpress.utils.log.Log;
import com.wordpress.utils.observer.Observable;
import com.wordpress.utils.observer.Observer;

public class CommentsListField {
    
	private Vector _listData = new Vector();
    private InternalCommentsListField _innerListField;
    private boolean checkBoxVisible = false;
    private ListCallBack listFieldCallBack = null;
	private GravatarController gvtController;
    private ListActionListener defautActionListener = null;
	
	public void setDefautActionListener(ListActionListener defautActionListener) {
		this.defautActionListener = defautActionListener;
	}
    
	public void setDefautLoadMoreListener(ListLoadMoreListener defautLoadMoreListener) {
		this._innerListField.setDefautLoadMoreListener(defautLoadMoreListener);
	}

	//Used by Load More
	public void resetListItems(Comment[] _elements, int selected) {
		//clear the list
		while ( _innerListField.isEmpty() == false ) {
			_innerListField.delete(0);	
		}
		_listData = new Vector();
		
        //Populate the ListField & Vector with data.
        for(int count = 0; count < _elements.length; ++count)
        {
        	ChecklistData checklistData = new ChecklistData(_elements[count], false);
        	if ( count == selected )
        		checklistData.setSelected(true); 
           _listData.addElement(checklistData);
           _innerListField.insert(count);
        }
	}
	
   public boolean isCheckBoxVisible() {
		return checkBoxVisible;
	}

	public void setCheckBoxVisible(boolean checkBoxVisible) {
		this.checkBoxVisible = checkBoxVisible;
		_innerListField.invalidate(); //invalidate all list
		WordPressCore.getInstance().setBulkCommentsModerationEnabled(checkBoxVisible); //store the settings
	}

	public boolean[] getSelected(){
       int elementLength = _listData.size();
       boolean[] selected= new boolean[elementLength];
       //Populate the ListField & Vector with data.
       for(int count = 0; count < elementLength; ++count)
       {
    	   //Get the ChecklistData for this row.
           ChecklistData data = (ChecklistData)_listData.elementAt(count);
           selected[count]= data.isChecked();
       }
       return selected;
   }

	
	public Comment[] getSelectedComments() {
		Vector comments = new Vector();
		int elementLength = _listData.size();
		for (int count = 0; count < elementLength; ++count) {
			// Get the ChecklistData for this row.
			ChecklistData data = (ChecklistData) _listData.elementAt(count);
			if (data.isChecked())
				comments.addElement(data.getComment());

		}
		Comment[] commentsArray = new Comment[comments.size()];
		comments.copyInto(commentsArray);
		return commentsArray;
	}
	
	//return the focused comment
	public Comment getFocusedComment() {
		int selectedIndex = _innerListField.getSelectedIndex();
		if (selectedIndex != -1) {
			ChecklistData data = (ChecklistData) _listData.elementAt(selectedIndex);
			return data.getComment();
		} else {
			return null;
		}
	}

   public CommentsListField(Comment[] _elements, boolean[] _elementsChecked, GravatarController gvtController) {
		_innerListField = new InternalCommentsListField();
        //Set the ListFieldCallback
        listFieldCallBack = new ListCallBack();
        _innerListField.setCallback(listFieldCallBack);
        ResourceBundle resourceBundle = WordPressCore.getInstance().getResourceBundle();
        String emptyListString = resourceBundle.getString(WordPressResource.MESSAGE_NOTHING_TO_SEE_HERE);
        _innerListField.setEmptyString(emptyListString, DrawStyle.LEFT);
        _innerListField.setRowHeight(BasicListFieldCallBack.getRowHeightForDoubleLineRow());

        int elementLength = _elements.length;
	    
        //Populate the ListField & Vector with data.
        for(int count = 0; count < elementLength; ++count)
        {
           ChecklistData checklistData = new ChecklistData(_elements[count], _elementsChecked[count]);
           if(count == 0) checklistData.setSelected(true); //select the first element
           _listData.addElement(checklistData);
           _innerListField.insert(count);
        }
        
    	this.gvtController = gvtController;
    	gvtController.addObserver(new GravatarCallBack());
   }   
   
    public ListField getCommentListField() {
		return _innerListField;
	}

	//The menu item added to the screen when the _checkList field has focus.
    //This menu item toggles the checked/unchecked status of the selected row.
    public MenuItem _toggleItem = new MenuItem("Change Option", 1000, 50)    {
        public void run()
        {
            //Get the index of the selected row.
            int index = _innerListField.getSelectedIndex();
            
            //Get the ChecklistData for this row.
            ChecklistData data = (ChecklistData)_listData.elementAt(index);
            
            //Toggle its status.
            data.toggleChecked();
            
            //Update the Vector with the new ChecklistData.
            _listData.setElementAt(data, index);
            
            //Invalidate the modified row of the ListField.
            _innerListField.invalidate(index);
        }
    }; 
    
    private class ListCallBack extends BasicListFieldCallBack {
    	  
        protected Bitmap checkedBitmap         = Bitmap.getBitmapResource("check.png");
        protected Bitmap uncheckedBitmap       = Bitmap.getBitmapResource("uncheck.png");
 	    
    	//Draws the list row.
        public void drawListRow(ListField list, Graphics graphics, int index, int y, int w) 
        {
            //Get the ChecklistData for the current row.
            ChecklistData currentRow = (ChecklistData)this.get(list, index);
            Comment currentComment = currentRow.getComment();
            
            Font originalFont = graphics.getFont();
            int originalColor = graphics.getColor();
            int height = list.getRowHeight();
            
           	drawBackground(graphics, 0, y, w, height, currentRow.isSelected);
        	drawBorder(graphics, 0, y, w, height, currentRow.isSelected);
       
            int leftImageWidth = 0;
            
            //If it is checked draw the String prefixed with a checked box,
            //prefix an unchecked box if it is not.
    		if (isCheckBoxVisible()) {
    			if (currentRow.isChecked()) {
    				drawLeftImage(graphics, 3, y, height, checkedBitmap);
    			} else {
    				drawLeftImage(graphics, 3, y, height, uncheckedBitmap);
    			}
    			leftImageWidth = 32;
    		} else {
    			String authorEmail = currentComment.getAuthorEmail();
    			EncodedImage gravatarImage = gvtController.getLatestGravatar(authorEmail);
    			//just another check. may user has changed the device font width
    			int resizeDim = BasicListFieldCallBack.getImageHeightForDoubleLineRow();
    			if(gravatarImage.getHeight() > resizeDim  || gravatarImage.getWidth() > resizeDim) {
    				gravatarImage = ImageUtils.resizeEncodedImage(gravatarImage, resizeDim, resizeDim);
    			} 
    			Bitmap tmpBitmap = gravatarImage.getBitmap();
    			leftImageWidth = tmpBitmap.getWidth();
    			drawLeftImage(graphics, 3, y, height, tmpBitmap );//use the bitmap. resized image here!!
    		}
    		
    		leftImageWidth+=3;
    		int authorWidth = 2;
            if(currentComment.getStatus().equalsIgnoreCase("hold"))
            	authorWidth = drawTextOnFirstRow(graphics, leftImageWidth, y, w  - leftImageWidth, height, currentComment.getAuthor(), Color.YELLOWGREEN);
            else if(currentComment.getStatus().equalsIgnoreCase("spam"))
            	authorWidth = drawTextOnFirstRow(graphics, leftImageWidth, y, w  - leftImageWidth, height, currentComment.getAuthor(), Color.RED);
            else
            	authorWidth = drawTextOnFirstRow(graphics, leftImageWidth, y, w  - leftImageWidth, height, currentComment.getAuthor(), currentRow.isSelected);
            
           // int spaceAvailableForEmail = w -( leftImageWidth + authorWidth + NEW_PADDING );
           // drawEMailText(graphics, leftImageWidth + authorWidth + NEW_PADDING , y, spaceAvailableForEmail, height, currentComment.getAuthorEmail(), currentRow.isSelected);
           
            drawSecondRowText(graphics, leftImageWidth, y, w - leftImageWidth, height, currentComment.getContent(), currentRow.isSelected);
            
            graphics.setFont(originalFont);
            graphics.setColor(originalColor);
        }
        
        private void drawEMailText(Graphics graphics, int x, int y, int width, int height, String email, boolean selected) {
        	Font fnt = Font.getDefault();
    		int fontHeight = ((5* fnt.getHeight()) / 6);
        	
            graphics.setFont(Font.getDefault().derive(Font.PLAIN, fontHeight));
            if (selected) {
                graphics.setColor(Color.WHITE);
            } else {
                graphics.setColor(Color.GRAY);
            }
            graphics.drawText(email, x , y + PADDING, DrawStyle.RIGHT | DrawStyle.TOP | DrawStyle.ELLIPSIS, width);
        }
        
    	 
        //Returns the object at the specified index.
        public Object get(ListField list, int index) 
        {
            return _listData.elementAt(index);
        }
    }
    
    //A class to hold the comment in the CheckBox and it's checkbox state (checked or unchecked).
    private class ChecklistData  {

    	private Comment comment;
        private boolean _checked;
        private boolean isSelected;
       
        ChecklistData(Comment comment, boolean checked)
        {
        	this.comment = comment;
        	_checked = checked;
        }

        public Comment getComment() {
        	return comment;
        }
                   
        private void setSelected(boolean flag){
        	isSelected = flag;
        }
        
        private boolean isChecked()
        {
            return _checked;
        }
                
        private void setChecked(boolean checked)
        {
            _checked = checked;
        }
        
        //Toggle the checked status.
        private void toggleChecked()
        {
            _checked = !_checked;
        }
    }
    
    private class GravatarCallBack implements Observer {
    	
    	public void update(Observable observable, Object object) {
    		if(object instanceof String) {
    			String email = (String) object;			
    			int elementLength = _listData.size();
    			
    			for(int count = 0; count < elementLength; count++)
    			{
    				//Get the ChecklistData for this row.
    				ChecklistData data = (ChecklistData)_listData.elementAt(count);
    				if ( data.comment.getAuthorEmail().equalsIgnoreCase(email) )
    					_innerListField.invalidate(count); //request row repaint
    			}
    		}	
    	}
    }
    
    
    private class InternalCommentsListField extends ListField
    {
    	private ListLoadMoreListener loadMoreListener;
    
    	public void setDefautLoadMoreListener(ListLoadMoreListener defautLoadMoreListener) {
    		this.loadMoreListener = defautLoadMoreListener;
    	}
    	
    	protected void drawFocus(Graphics graphics, boolean on) { } //remove the standard focus highlight
		
		private void checkItemAction() {
			  //Get the index of the selected row.
            int index = getSelectedIndex();
            
            //Get the ChecklistData for this row.
            ChecklistData data = (ChecklistData)_listData.elementAt(index);
            
            //Toggle its status.
            data.toggleChecked();
            
            //Update the Vector with the new ChecklistData.
            _listData.setElementAt(data, index);
            
            //Invalidate the modified row of the ListField.
            invalidate(index);
        
		}
		
		public void setSelectedIndex(int index) {
        	ChecklistData data = null;
            int oldSelection = getSelectedIndex();
            super.setSelectedIndex(index);
            int newSelection = getSelectedIndex();
            
            if(oldSelection != -1) {
            	data = (ChecklistData)_listData.elementAt(oldSelection);
            	data.setSelected(false);
            	invalidate(oldSelection);
            }
            
            if(newSelection != -1) {
            	data = (ChecklistData)_listData.elementAt(newSelection);
            	data.setSelected(true);
            	invalidate(newSelection);
            }
		}
		
		private boolean defaultAction() {
	   		if (!isCheckBoxVisible()) {
    			if (defautActionListener != null) {
    				defautActionListener.actionPerformed();
    				return true;
    			} else        			
    			return false;
    		} else {
    			checkItemAction();
    			return true;
    		}
		}
		
        /**
         * Overrides default implementation.  Performs default action if the 
         * 4ways trackpad was clicked; otherwise, the default action occurs.
         * 
         * @see net.rim.device.api.ui.Screen#navigationClick(int,int)
         */
    	protected boolean navigationClick(int status, int time) {
    		Log.trace(">>> navigationClick");
    		
    		if ((status & KeypadListener.STATUS_TRACKWHEEL) == KeypadListener.STATUS_TRACKWHEEL) {
    			Log.trace("Input came from the trackwheel");
    			// Input came from the trackwheel
    			return super.navigationClick(status, time);
    			
    		} else if ((status & KeypadListener.STATUS_FOUR_WAY) == KeypadListener.STATUS_FOUR_WAY) {
    			Log.trace("Input came from a four way navigation input device");
    			return defaultAction();
    		}
    		
    		return false;
    	}
		
        // Allow the space bar to toggle the status of the selected row.
		protected boolean keyChar(char key, int status, int time) {
			boolean retVal = false;
			// If the spacebar or enter was pressed...
			if ((key == Characters.SPACE || key == Characters.ENTER)) {
				retVal = defaultAction();
			}// end if keychar
			return retVal;
		}
        
        
    	//#ifdef VER_4.7.0 | BlackBerrySDK5.0.0 | BlackBerrySDK6.0.0 | BlackBerrySDK7.0.0
    	protected boolean touchEvent(TouchEvent message) {
    		if(!this.getContentRect().contains(message.getX(1), message.getY(1)))
    		{       			
    			return false;
    		} 
    		int eventCode = message.getEvent();
    		if(WordPressInfo.isForcelessTouchClickSupported) {
    			if (eventCode == TouchEvent.GESTURE) {
    				TouchGesture gesture = message.getGesture();
    				int gestureCode = gesture.getEvent();
    				if (gestureCode == TouchGesture.TAP) {
    					return defaultAction();
        			} else if (gestureCode == TouchGesture.HOVER) {
        				return false; //Show the contextual menu
        			} else {
        				//is not a click!
        				checkLoadMore();
        			}
    			} 
    		} else {
    			if(eventCode == TouchEvent.CLICK) {
    				return defaultAction();
    			} else {
    				//is not a click!
    				checkLoadMore();
    			}
    		}
    		return false;	
    	}
    	//#endif
    	
        protected int moveFocus(int amount, int status, int time) {
        	ChecklistData data = null;
            int oldSelection = getSelectedIndex();
            if(oldSelection != -1) {
            	data = (ChecklistData)_listData.elementAt(oldSelection);
            	data.setSelected(false);
            	invalidate(oldSelection);
            }

            // Forward the call
            int ret = super.moveFocus(amount, status, time);
            int newSelection = getSelectedIndex();
            
            // Get the next enabled item;
            if(newSelection != -1) {
                data = (ChecklistData)_listData.elementAt(newSelection);
                data.setSelected(true);
                invalidate(newSelection);
            }
            //invalidate();

            return ret;
        }
        
        
        protected void moveFocus(int x, int y, int status, int time) {
        	ChecklistData data = null;
            int oldSelection = getSelectedIndex();
            super.moveFocus(x, y, status, time);
            int newSelection = getSelectedIndex();
            
            if(oldSelection != -1) {
            	data = (ChecklistData)_listData.elementAt(oldSelection);
            	data.setSelected(false);
            	invalidate(oldSelection);
            }
            
            if(newSelection != -1) {
            	data = (ChecklistData)_listData.elementAt(newSelection);
            	data.setSelected(true);
            	invalidate(newSelection);
            }
            //invalidate();
        }
        

    	protected boolean navigationMovement(int dx, int dy, int status, int time) {
        	Log.debug("dx: "+dx+" dy: "+dy+" status: "+ status+" time: "+time);
        	if ( dy == 1 ) //moving down in the list 
        		checkLoadMore();
        	return super.navigationMovement(dx, dy, status, time);
        }
    	
        private void checkLoadMore() {
        	if( this.loadMoreListener == null )
        		return;
        	
        	Manager manager = this.getManager();
        	int managerHeight = manager.getHeight();
        	int managerContentHeight = manager.getContentHeight();
        	int managerVerticalScroll = manager.getVerticalScroll();
        	int managerVirtualHeight = manager.getVirtualHeight();

    		Log.debug("getVirtualHeight: "+managerVirtualHeight+" getVerticalScroll: "+managerVerticalScroll );
    		Log.debug("getContentHeight: "+managerContentHeight+" getHeight: "+managerHeight);
        	    	
    		if( managerVerticalScroll == 0 ||  managerVerticalScroll ==  managerHeight )
        		return;
        	
        	int calculatedVirtualHeight = managerVerticalScroll + managerHeight;
        	int calculatedVirtuaContentHeight = managerVerticalScroll + managerContentHeight;
        	
        	boolean shouldLoadMore =  calculatedVirtualHeight >= managerVirtualHeight;
        	shouldLoadMore = shouldLoadMore || true == calculatedVirtuaContentHeight >= managerVirtualHeight; //just another check
        	
        	if (shouldLoadMore) this.loadMoreListener.loadMore();
        } 
        
    };// End of the internal list field declaration
} 