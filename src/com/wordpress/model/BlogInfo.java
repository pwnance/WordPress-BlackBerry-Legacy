package com.wordpress.model;

public class BlogInfo {

	public static int STATE_ADDED_TO_QUEUE = 0;
	public static int STATE_LOADING = 1;
	public static int STATE_LOADED = 2;
	public static int STATE_LOADED_WITH_ERROR = 3;
	public static int STATE_ERROR = 4;
	
	private String id;
	private String name;
	private String xmlRpcUrl; //real url for publishing on this blog
	private String username;
	private String password;
	private int state=-1;
	private boolean isCommentNotifies = false; //true when comment notifies is active
	

	public BlogInfo(String id, String name, String xmlRpcUrl, String usr, String passwd, int state, boolean notifies) {
		super();
		this.id = id;
		this.name = name;
		this.xmlRpcUrl = xmlRpcUrl;
		this.state = state;
		this.isCommentNotifies = notifies;
		this.username = usr;
		this.password = passwd;
	}
	
	public String getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public String getXmlRpcUrl() {
		return xmlRpcUrl;
	}
	
	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public int getState() {
		return state;
	}
	
	public void setState(int state) {
		this.state = state;
	}

	public boolean isCommentNotifies() {
		return isCommentNotifies;
	}
	
	public void setCommentNotifies(boolean isCommentNotifies) {
		this.isCommentNotifies = isCommentNotifies;
	}
	
	//variable state and isCommentNotifies not considered
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((xmlRpcUrl == null) ? 0 : xmlRpcUrl.hashCode());
		return result;
	}

	//variable state and isCommentNotifies not considered
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BlogInfo other = (BlogInfo) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (xmlRpcUrl == null) {
			if (other.xmlRpcUrl != null)
				return false;
		} else if (!xmlRpcUrl.equals(other.xmlRpcUrl))
			return false;
		return true;
	}
}
	
