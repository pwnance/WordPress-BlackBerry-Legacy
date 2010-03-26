package com.wordpress.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.rms.RecordStoreException;

import com.wordpress.model.Blog;
import com.wordpress.model.MediaEntry;
import com.wordpress.model.MediaLibrary;
import com.wordpress.utils.log.Log;

public class MediaLibraryDAO implements BaseDAO {
	
	public static synchronized MediaLibrary[] loadAllMediaLibrary(Blog blog) throws IOException, RecordStoreException {
		
		String blogNameMD5=BlogDAO.getBlogFolderName(blog);
    	String commentsFilePath=AppDAO.getBaseDirPath()+blogNameMD5+MEDIALIBRARY_FILE;
    	MediaLibrary[] mediaLib = null;
    	
    	if (!JSR75FileSystem.isFileExist(commentsFilePath)){
    		return new MediaLibrary[0];
    	}   	
    	
    	DataInputStream in = JSR75FileSystem.getDataInputStream(commentsFilePath);
    	Serializer ser= new Serializer(in);
    	Vector comments = (Vector) ser.deserialize();
    	in.close();

    	mediaLib = new MediaLibrary[comments.size()];
    	for (int i = 0; i < comments.size(); i++) {
    		Hashtable elementAt = (Hashtable) comments.elementAt(i);
    		MediaLibrary tmpEntry = hashtable2MediaLibraryEntry(elementAt);
    		mediaLib[i] = tmpEntry;
		}
    	return mediaLib;
	}
	
	
	public static synchronized boolean deleteMediaLibrary(Blog blog, int itemIdx) throws IOException, RecordStoreException {
		
		String blogNameMD5=BlogDAO.getBlogFolderName(blog);
    	String commentsFilePath=AppDAO.getBaseDirPath()+blogNameMD5+MEDIALIBRARY_FILE;
    	
    	if (!JSR75FileSystem.isFileExist(commentsFilePath)){
    		return false;
    	}   	
    	
    	DataInputStream in = JSR75FileSystem.getDataInputStream(commentsFilePath);
    	Serializer ser= new Serializer(in);
    	Vector comments = (Vector) ser.deserialize();
    	in.close();
    	comments.removeElementAt(itemIdx);
    	
    	//store objs
    	JSR75FileSystem.createFile(commentsFilePath); //create the file
    	DataOutputStream out = JSR75FileSystem.getDataOutputStream(commentsFilePath);
    	ser= new Serializer(out);
    	ser.serialize(comments);
    	out.close();
    	return true;
	}
	
	public static synchronized boolean updateMediaLibrary(Blog blog, int itemIdx, MediaLibrary updatedItem) throws IOException, RecordStoreException {
		
		String blogNameMD5=BlogDAO.getBlogFolderName(blog);
    	String commentsFilePath=AppDAO.getBaseDirPath()+blogNameMD5+MEDIALIBRARY_FILE;
    	Vector comments = null;
    	Serializer ser = null;
    	
    	if (JSR75FileSystem.isFileExist(commentsFilePath)){
    		DataInputStream in = JSR75FileSystem.getDataInputStream(commentsFilePath);
    		ser= new Serializer(in);
    		comments = (Vector) ser.deserialize();
    		in.close();
    	} else {
    		comments = new Vector();
    	}
    	
    	Hashtable tmpData = mediaItem2Hashtable(updatedItem);
    	if(itemIdx != -1) {
    		comments.setElementAt(tmpData, itemIdx);
    	} else {
    		comments.addElement(tmpData);
    	}
    	//store objs
    	JSR75FileSystem.createFile(commentsFilePath); //create the file
    	DataOutputStream out = JSR75FileSystem.getDataOutputStream(commentsFilePath);
    	ser= new Serializer(out);
    	ser.serialize(comments);
    	out.close();
    	return true;
	}
	
	
	//store comments and updates comment waiting notification info
	public static synchronized void storeMediaLibraries(Blog blog, MediaLibrary[] mediaEntries) throws IOException, RecordStoreException {
		Log.trace(">>> storeMediaLibray ");
		Vector serializedData = new Vector(mediaEntries.length);
		for (int i = 0; i < mediaEntries.length; i++) {
			Hashtable tmpData = mediaItem2Hashtable(mediaEntries[i]);
			serializedData.setElementAt(tmpData, i);
		}
		
		String blogNameMD5=BlogDAO.getBlogFolderName(blog);
		String commentsFilePath=AppDAO.getBaseDirPath()+blogNameMD5+MEDIALIBRARY_FILE;
    	
    	JSR75FileSystem.createFile(commentsFilePath); //create the file
    	DataOutputStream out = JSR75FileSystem.getDataOutputStream(commentsFilePath);

    	Serializer ser= new Serializer(out);
    	ser.serialize(serializedData);
    	out.close();
		Log.trace("<<< storeMediaLibray");
	}
	
	private static synchronized Hashtable mediaItem2Hashtable(MediaLibrary item) {
		
        Hashtable content = new Hashtable();
        if (item.getTitle() != null) {
            content.put("title", item.getTitle());
        }
        
		//convert media object before save them
		Vector mediaObjects = item.getMediaObjects();
		Vector hashedMediaIbjects = new Vector(mediaObjects.size());
		for (int i = 0; i < mediaObjects.size(); i++) {
			MediaEntry tmp = (MediaEntry) mediaObjects.elementAt(i);
			hashedMediaIbjects.addElement(tmp.serialize());
			}
		content.put("mediaObjects", hashedMediaIbjects);
		
		return content;
	}
	
	private static synchronized MediaLibrary hashtable2MediaLibraryEntry(Hashtable storedData) {
		MediaLibrary entry = new MediaLibrary();
		entry.setTitle((String) storedData.get("title"));
		
		if(storedData.get("mediaObjects") != null) {
			Vector hashedMediaIbjects = (Vector) storedData.get("mediaObjects");
			Vector mediaObjects = new Vector(hashedMediaIbjects.size());
			for (int i = 0; i < hashedMediaIbjects.size(); i++) {
				Hashtable tmp = (Hashtable) hashedMediaIbjects.elementAt(i);
				MediaEntry tmpMedia = MediaEntry.deserialize(tmp);
				if(tmpMedia != null )
					mediaObjects.addElement(tmpMedia);
				}
		entry.setMediaObjects(mediaObjects);
		}
        return entry;
	}
}
