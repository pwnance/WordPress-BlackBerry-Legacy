package com.wordpress.bb;

import net.rim.device.api.system.Bitmap;

/**
 * Class to provide information about the application
 * and its environment.  
 */
public final class WordPressInfo {

	/** System event log GUID */
    public final static long GUID = 0x97ebc046dec5817fL;
    public final static long COMMENTS_UID = 0x8de9c6b3a49fd864L;
    
	public final static String STATS_AUTH_ENDPOINT_URL = "https://public-api.wordpress.com/getuserblogs.php";
	public final static String STATS_ENDPOINT_URL = "http://stats.wordpress.com/csv.php";
	public final static String BB_APP_STATS_ENDPOINT_URL = "http://api.wordpress.org/bbapp/update-check/1.0/";
	public final static String BB_APP_SIGNUP_URL = "http://wordpress.com/signup/?ref=wp-blackberry";
    
    private static Bitmap icon = Bitmap.getBitmapResource("application-icon.png");
    private static Bitmap newCommentsIcon = Bitmap.getBitmapResource("application-icon-new.png");
    
    /**
     * Initializes the application information from the descriptor and the
     * command-line arguments.  This method must be called on startup.
     * @param args Arguments
     */
    public static synchronized void initialize(String args[]) {

    }
   
    public static Bitmap getIcon() {
    	return icon;
    }
    
    public static Bitmap getNewCommentsIcon() {
    	return newCommentsIcon;
    }    
}