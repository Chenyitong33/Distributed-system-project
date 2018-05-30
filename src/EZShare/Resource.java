package EZShare;

import java.net.*;

public class Resource {

	public String name;
	public String description;
	public String[] tags;
	public URI uri;
	private String channel;
	public String owner;
	public String ezserver;
	//public HashMap<String, Object> primarykey;
	
	// signals for string error ???
	//public boolean stringerror;
	//private String rule;
	
	public Resource() {
		name = "";
		description = "";
		tags= new String[]{};
		uri = null;
		channel = "";
		owner = "";
		ezserver = "localhost:8080";
		//rule = "";
		//primarykey = null;
	}
	
	public String getChannel() {
		return channel;
	}

	public void changeChannel(String word) {
		channel = word;
	}	
}
