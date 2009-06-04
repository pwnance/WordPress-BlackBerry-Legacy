package com.wordpress.xmlrpc;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

public class GetTemplateConn extends BlogConn  {


	public GetTemplateConn(String url){
		super(url, "", "");
	}
	
	//we have overrided execute method, because there isn't xml-rpc conn, but only a simple http conn 
	protected Object execute(String aCommand, Vector aArgs){
		isWorking=true;
		
		HttpConnection conn = null;
		String response = null;
		
		try {
			conn = (HttpConnection) Connector.open(urlConnessione);
			int rc = conn.getResponseCode();
			if( rc == HttpConnection.HTTP_OK ){
				
				//read the response
				
				InputStream in = conn.openInputStream();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				int c;
				while ((c = in.read()) >= 0)
				{
					baos.write(c);
				}
				response = new String (baos.toByteArray());
				
			} else {
			    // deal with errors, warnings, redirections, etc.
				throw new Exception(""+conn.getResponseCode());
			}

		} catch (Exception e) {
			 setErrorMessage(e, "A server communications error occured:");
		}
  	   System.out.println("termine richiesta HTTP-GET");
		isWorking=false;
		return response;
	}
	

	public void run() {
		try {
			
			Object response = execute("", null);

			if(connResponse.isError()) {
				notifyObservers(connResponse);
				return;	
			}
			connResponse.setResponseObject(response);
		}
		catch (Exception cce) {
			setErrorMessage(cce, "Get Template error: Invalid server response");
		}

		try {
			notifyObservers(connResponse);
		} catch (Exception e) {
			System.out.println("Get Template error: Notify error"); 		
		}
	}
}
