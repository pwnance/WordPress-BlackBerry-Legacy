package com.wordpress.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;

/**
 * This class includes implementation for reading files using JSR-75.
 * 
 */
public class JSR75FileSystem  {

  /**
   * Read a file using JSR-75 API.
   * 
   * @param filename
   *          fully-qualified file path following "file:///" qualifier
   * @return file data
   * @throws IOException
   *           if an exception occurs
   */
  public static synchronized byte[] readFile(String filename) throws IOException {
    System.out.println("Loading file:///" + filename);

    FileConnection fconn = null;
    InputStream is = null;
    try {
      fconn = (FileConnection) Connector.open("file:///" + filename, Connector.READ);
      // commented to speed up
      // if (!fconn.exists() || !fconn.canRead())
      //   throw new Exception("File does not exist");

      int sz = (int) fconn.fileSize();
      byte[] result = new byte[sz];

      is = fconn.openInputStream();

      // multiple bytes
      int ch = 0;
      int rd = 0;
      while ((rd != sz) && (ch != -1)) {
        ch = is.read(result, rd, sz - rd);
        if (ch > 0) {
          rd += ch;
        }
      }

      return result;
    } finally {
      FileUtils.closeStream(is);
      FileUtils.closeConnection(fconn);
    }
  }

  

  /**
   * List all roots in the filesystem
   * 
   * @return a vector containing all the roots
   * @see com.nutiteq.utils.fs.FileSystem#getRoots()
   */
  public static synchronized  Vector getRoots() {
    final Vector v = new Vector();

    // list roots
    final Enumeration en = FileSystemRegistry.listRoots();

    // enumerate
    while (en.hasMoreElements()) {
      String root = (String) en.nextElement();
      if (!root.endsWith("/")) {
        root += '/';
      }
      v.addElement(root);
    }

    return v;
  }

  /**
   * List all files in a directory.
   * 
   * @param path
   *          path to list, null to list root
   * @return a vector of file names
   */
  public static synchronized  Vector listFiles(final String path) throws IOException {
    if (path == null || path.length() == 0) {
      return getRoots();
    }

    // open directory
    final Vector v = new Vector();
    FileConnection fconn = null;
    try {
      fconn = (FileConnection) Connector.open("file:///" + path, Connector.READ);
      v.addElement("../");
      final Enumeration en = fconn.list();
      while (en.hasMoreElements()) {
        String filename = (String) en.nextElement();

        // convert absolute to relative path
        int pos = filename.length() - 2;
        while (pos >= 0 && filename.charAt(pos) != '/') {
          pos--;
        }
        if (pos >= 0) {
          filename = filename.substring(pos + 1);
        }

        v.addElement(filename);
      }
    } finally {
      if (fconn != null) {
        fconn.close();
      }
    }

    return v;
  }

  /**
   * Check if a file is a directory
   * 
   * @param filename
   *          file to check
   * @return true if it is a directory
   */
  public static synchronized  boolean isDirectory(String filename) {
    return filename.endsWith("/");
  }

}
