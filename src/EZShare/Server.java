package EZShare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.net.ServerSocketFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONArray;

public class Server {
	private static int port = 8080;
	private static String advertisedhostname = "localhost";
	private static String secret = "whoami";// "5uv1ii7ec362me7hkch3s7l5c4";
	private static int exchangeinterval = 600; // seconds
	private static int connectionintervallimit = 1; // seconds
	// Identifies the user number connected
	private static int counter = 0;
	private static List<Resource> resourcelist = new ArrayList<Resource>();
	private static List<JSONObject> serverrecords = new ArrayList<JSONObject>();
	private static boolean debugon = false; // DEBUG
	private static long mainTime;
	private static long newTime;
	private static long previousTime;
	private static boolean relay = false; // if the field is not provided
	private static boolean shutdown;
	public static void main(String[] args) {
		mainTime = System.currentTimeMillis();// initialize the start time

		Options options = new Options();
		// Server command line arguments
		options.addOption("advertisedhostname", true, "advertised host name");
		options.addOption("connectionintervallimit", true, "connection interval limit in seconds");
		options.addOption("exchangeinterval", true, "exchange interval in seconds");
		options.addOption("port", true, "server port, an integer");
		options.addOption("secret", true, "secret");
		options.addOption("debug", false, "print debug information");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;

		try {
			cmd = parser.parse(options, args);
		} catch (org.apache.commons.cli.ParseException e) {
			// TODO Auto-generated catch block
			System.err.println( "Parsing failed.  Reason: " + e.getMessage() );
		}
		
		// automatically generate the help statement
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "ant", options );
        
		// change HOST
		if (cmd.hasOption("advertisedhostname")) {
			advertisedhostname = cmd.getOptionValue("advertisedhostname");
		}

		// change PORT
		if (cmd.hasOption("port")) {
			port = Integer.parseInt(cmd.getOptionValue("port"));
		}

		if (cmd.hasOption("connectionintervallimit")) {
			connectionintervallimit = Integer.parseInt(cmd.getOptionValue("connectionintervallimit"));
		}

		if (cmd.hasOption("exchangeinterval")) {
			exchangeinterval = Integer.parseInt(cmd.getOptionValue("exchangeinterval"));
		}

		// DEBUG command--------------------------------------------
		if (cmd.hasOption("debug")) {
			debugon = true;
		}
		// SECRET command--------------------------------------------
		if (cmd.hasOption("secret")) {
			secret = cmd.getOptionValue("secret");
		}
		//------------------------------Console-------------------------------------
		System.out.println("Starting the EZShare Server");
		System.out.println("using secret: " + secret);
		System.out.println("using advertised hostname: " + advertisedhostname);
		System.out.println("bound to port: " + port);
		// System.out.println("started");

		ServerSocketFactory factory = ServerSocketFactory.getDefault();
		try (ServerSocket server = factory.createServerSocket(port)) {

			System.out.println("started");

			if (debugon == true) {
				System.out.println("setting debug on");
			}

			boolean isActive = true;

			// for the SERVER INTERACTIONS--------------------------------------------------------
			Thread t = new Thread(() -> serverInteraction());
			t.start();
			/*
			try {
				t.join();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			*/
			previousTime = 0;// initiates the time before a new connection
			
			while (isActive) {
				//------------------------------Connection Interval Limit------------------------------
				Socket Client = server.accept();
				shutdown = false;
				newTime = System.currentTimeMillis(); // initiates the time after a new connection
				
				counter++;

				System.out.println("Client " + counter + ": Applying for connection!");
				
				// print current time
				System.out.println("Current server time is: " + getTime());
				// no less than a limit!!!!!!!!!!!!!!!!!!
				if (newTime - previousTime < connectionintervallimit * 1000 &&
						previousTime != 0) {
					System.out.println("Client " + counter + ": cannot connect!");
					Client.close();
					
				} else {
					System.out.println("Client " + counter + ": connected!");
					Thread r = new Thread(() -> serverClient(Client));
					r.start();
					/*
					try {
						r.join();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					*/
				}
				if (shutdown == true) {
					Client.close();
				}
				previousTime = System.currentTimeMillis();
				//Client.setSoTimeout(connectionintervallimit * 1000); // not the socket time!!!
				
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void serverInteraction() {
		// TODO Auto-generated method stub
		while (true) {
			//System.out.println(System.currentTimeMillis());
			//System.out.println("test start");
			// =========================Every X
			// minutes==========================
			if ((System.currentTimeMillis() - mainTime) % (exchangeinterval*1000) == 0) {
				//System.out.println("2222");
				// ------------------------------------------------------------------
				// Randomly selected server from the Server Records
				JSONObject send = new JSONObject();
				Random randomizer = new Random();
				if (!serverrecords.isEmpty()) {

					JSONObject random = serverrecords.get(randomizer.nextInt(serverrecords.size()));
					String hostname = (String) random.get("hostname");
					int port1 = Integer.parseInt(random.get("port").toString());
			        Socket socket = new Socket();
			        InetSocketAddress endPoint = new InetSocketAddress(hostname, port1);

			        if ( endPoint.isUnresolved() ) {

			            System.out.println("Failure " + endPoint);
			            serverrecords.remove(random);
			            
			        } else try { 

			            socket.connect( endPoint );
			            System.out.printf("Success:    %s  \n",  endPoint);
			            DataInputStream Input = new DataInputStream(socket.getInputStream());
						DataOutputStream Output = new DataOutputStream(socket.getOutputStream());

						// Initiates an EXCHANGE command with it.
						// It provides the selected server with a copy of its
						// entire Server Records list.
						if (debugon == true) {
							System.out.println("exchanging to " + hostname + ":" + port1);
						}

						JSONArray servers = new JSONArray();
						JSONObject itself = new JSONObject();
						for (JSONObject record : serverrecords) {
							servers.add(record);
						}
						itself.put("hostname", advertisedhostname);
						itself.put("port", port); //put its own port number !!!!!!!!!!!!!!!!!!!!!!!!!!!
						servers.add(itself);
						send.put("serverList", servers);
						send.put("command", "EXCHANGE");
						
						Output.writeUTF(send.toJSONString());
						if (debugon == true) {
							System.out.println("SENT in Server Interactions: " + send);
							System.out.println("RECEIVED in Server Interactions: " + Input.readUTF());
						}
			        } catch( IOException ioe ) {

			            System.out.printf("Failure:    %s message: %s - %s \n", 
			                endPoint , ioe.getClass().getSimpleName(),  ioe.getMessage());

			        }/*
					try (Socket socket = new Socket(hostname, port)) {
						DataInputStream Input = new DataInputStream(socket.getInputStream());
						DataOutputStream Output = new DataOutputStream(socket.getOutputStream());

						// Initiates an EXCHANGE command with it.
						// It provides the selected server with a copy of its
						// entire Server Records list.
						if (socket.isConnected()) {
							if (debugon == true) {
								System.out.println("exchanging to " + hostname + ":" + port);
							}

							JSONArray servers = new JSONArray();
							for (JSONObject record : serverrecords) {
								servers.add(record);
							}
							send.put("serverList", servers);
							send.put("command", "EXCHANGE");
							Output.writeUTF(send.toJSONString());
							if (debugon == true) {
								System.out.println("SENT in Server Interactions: " + send);
								System.out.println("RECEIVED in Server Interactions: " + Input.readUTF());
							}

						} else {
							serverrecords.remove(random);
						}
					} catch (UnknownHostException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}*/
				}

			}
		}
	}

	public static String getTime() {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		String stringTime = sdf.format(date);
		return stringTime;
	}

	private static JSONArray  parseCommand(JSONObject command, DataOutputStream output) {
		int result = 0;
		//Resource resource = new Resource();
		JSONArray seq = new JSONArray();
		JSONObject receive = new JSONObject();
		JSONObject message = new JSONObject();
		Resource resource = new Resource();
		if(command.containsKey("command")) {
			// PUBLISH-------------------------------------------------------------------
			if(command.get("command").equals("PUBLISH")) {

				boolean publisherror = false;


				receive = (JSONObject) command.get("resource");
				resource.name = (String) receive.get("name");
				
				if(receive.get("uri").equals("")) {
					resource.uri = null;
				} else {
					resource.uri = URI.create(((String)receive.get("uri")));
				}
				
				resource.description = (String) receive.get("description");
				resource.changeChannel((String) receive.get("channel"));
				resource.owner = (String) receive.get("owner");
				resource.tags = new String[((JSONArray) receive.get("tags")).size()];
				resource.ezserver = (String) receive.get("ezserver");
				for(int i = 0, count = resource.tags.length; i< count; i++)
				{
				    resource.tags[i] = (String) ((JSONArray) receive.get("tags")).get(i);

				}
				// Publishing a resource with the same channel and URI but different owner is not allowed.
				for (Resource r: resourcelist) {
					if(resource.getChannel().equals(r.getChannel())&&resource.uri.equals(r.uri)
							&& resource.owner.equals(r.owner)) {
						publisherror = true;
					}
				}
				
				if (resource.uri == null) {
					message.put("response",  "error");
					message.put("errorMessage",  "missing resource");
				} else if (!resource.uri.isAbsolute()
						|| resource.uri.getScheme().equals("file")) {
					//The URI must be present, must be absolute and cannot be a file scheme.
					message.put("response",  "error");
					message.put("errorMessage",  "invalid resource");
				} else if (resource.name.startsWith(" ") || resource.name.endsWith(" ") || resource.name.contains("\0")) {
					message.put("response",  "error");
					message.put("errorMessage",  "invalid resource");
				} else if (resource.description.startsWith(" ") || resource.description.endsWith(" ") 
						|| resource.description.contains("\0")) {
					message.put("response",  "error");
					message.put("errorMessage",  "invalid resource");
				} else if (resource.getChannel().startsWith(" ") || resource.getChannel().endsWith(" ") 
						|| resource.getChannel().contains("\0")) {
					message.put("response",  "error");
					message.put("errorMessage",  "invalid resource");
				} else if (resource.owner.startsWith(" ") || resource.owner.endsWith(" ") || resource.owner.contains("\0") 
						|| resource.owner.equals("*")) {
					message.put("response",  "error");
					message.put("errorMessage",  "invalid resource");
				} else if (publisherror == true) {
					message.put("response",  "error");
					message.put("errorMessage",  "cannot publish resource");
				} else {
					// check if the primary key is same with exists
					// if yes, overwrite
					for (Resource r: resourcelist) {
						if (r.owner.equals(resource.owner)&&r.getChannel().equals(resource.getChannel())
								&&r.uri.equals(resource.uri)) {
							resourcelist.remove(r);
						}
					}
					resourcelist.add(resource);
					message.put("response",  "success");
				}
				//seq.add(message);
				try {
					output.writeUTF(message.toJSONString());
					if (debugon == true) {
    	        		System.out.println("SENT: " + message);
    	        	}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			} //REMOVE---------------------------------------------------------
		 else if (command.get("command").equals("REMOVE")) {
			boolean removesuccess = false;
			receive = (JSONObject) command.get("resource");
			resource.name = (String) receive.get("name");
			if(receive.get("uri").equals("")) {
				resource.uri = null;
			} else {
				resource.uri = URI.create(((String)receive.get("uri")));
			}

			resource.changeChannel((String) receive.get("channel"));
			resource.owner = (String) receive.get("owner");

			if (resource.uri == null) {
				message.put("response",  "error");
				message.put("errorMessage",  "missing resource");
			} else if (!resource.uri.isAbsolute()) {
				message.put("response", "error");
				message.put("errorMessage", "invalid resource");
			} else {
				Iterator<Resource> iter = resourcelist.iterator();
				while(iter.hasNext()) {
					Resource r = iter.next();
					if (r.owner.equals(resource.owner) && r.getChannel().equals(resource.getChannel())
							&& r.uri.equals(resource.uri)) {
						iter.remove();
						message.put("response", "success");
						removesuccess = true;
					} 
				}
				if (removesuccess == false) {
					message.put("response", "cannot remove resource");
				}
			}
			//seq.add(message);
			try {
				output.writeUTF(message.toJSONString());
				if (debugon == true) {
	        		System.out.println("SENT: " + message);
	        	}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if(command.get("command").equals("SHARE")) {
				// SHARE--------------------------------------------------------------------------
				boolean shareerror = false;
				
				receive = (JSONObject) command.get("resource");
				resource.name = (String) receive.get("name");
				if(receive.get("uri").equals("")) {
					resource.uri = null;
				} else {
					resource.uri = URI.create(((String)receive.get("uri")));
				}
				resource.description = (String) receive.get("description");
				resource.changeChannel((String) receive.get("channel"));
				resource.owner = (String) receive.get("owner");
				resource.tags = new String[((JSONArray) receive.get("tags")).size()];
				resource.ezserver = (String) receive.get("ezserver");
				for(int i = 0, count = resource.tags.length; i< count; i++)
				{
				    resource.tags[i] = (String) ((JSONArray) receive.get("tags")).get(i);

				}
				File file = new File(resource.uri.getPath());
				
				// Sharing a resource with the same channel and URI but different owner is not allowed.
				for (Resource r: resourcelist) {
					if(resource.getChannel().equals(r.getChannel())&&resource.uri.equals(r.uri)
							&& resource.owner.equals(r.owner)) {
						shareerror = true;
					}
				}
				
				if (resource.uri == null) {
					message.put("response",  "error");
					message.put("errorMessage",  "missing resource");
				} else if (!resource.uri.isAbsolute()
						|| !resource.uri.getScheme().equals("file")
						|| !file.exists()) {
					//The URI must be present, must be absolute, non-authoritative and must be a file scheme. It must point
					//to a file on the local file system that the server can read as a file.
					message.put("response",  "error");
					message.put("errorMessage",  "invalid resource");
				} else if (command.get("secret") == null) {
					//If the resource or secret field was not given or not of the correct type
					message.put("response",  "error");
					message.put("errorMessage",  "missing resource and/or secret");
				} else if (!command.get("secret").getClass().equals(String.class)) {
					//If the resource or secret field was not given or not of the correct type
					message.put("response",  "error");
					message.put("errorMessage",  "missing resource and/or secret");
				} else if (!command.get("secret").equals(secret)) {
					message.put("response",  "error");
					message.put("errorMessage",  "incorrect secret");
				} else if (resource.name.startsWith(" ") || resource.name.endsWith(" ") || resource.name.contains("\0")) {
					message.put("response",  "error");
					message.put("errorMessage",  "invalid resource");
				} else if (resource.description.startsWith(" ") || resource.description.endsWith(" ") 
						|| resource.description.contains("\0")) {
					message.put("response",  "error");
					message.put("errorMessage",  "invalid resource");
				} else if (resource.getChannel().startsWith(" ") || resource.getChannel().endsWith(" ") 
						|| resource.getChannel().contains("\0")) {
					message.put("response",  "error");
					message.put("errorMessage",  "invalid resource");
				} else if (resource.owner.startsWith(" ") || resource.owner.endsWith(" ") || resource.owner.contains("\0") 
						|| resource.owner.equals("*")) {
					message.put("response",  "error");
					message.put("errorMessage",  "invalid resource");
				} else if (shareerror == true) {
					message.put("response",  "error");
					message.put("errorMessage",  "cannot share resource");
				} else {
					// check if the primary key is same with exists
					// if yes, overwrite
					for (Resource r: resourcelist) {
						if (r.owner.equals(resource.owner)&&r.getChannel().equals(resource.getChannel())
								&&r.uri.equals(resource.uri)) {
							resourcelist.remove(r);
						}
					}
					resourcelist.add(resource);
					message.put("response",  "success");
				}
				//seq.add(message);
				try {
					output.writeUTF(message.toJSONString());
					if (debugon == true) {
    	        		System.out.println("SENT: " + message);
    	        	}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			} else if(command.get("command").equals("QUERY")) {
				// QUERY--------------------------------------------------------------------------
				if (command.containsKey("relay")) {
					relay = (boolean) command.get("relay");
				}
				
				receive = (JSONObject) command.get("resourceTemplate");
				
				resource.name = (String) receive.get("name");
				if(receive.get("uri").equals("")) {
					resource.uri = null;
				} else {
					resource.uri = URI.create(((String)receive.get("uri")));
				}
				resource.description = (String) receive.get("description");
				resource.changeChannel((String) receive.get("channel"));
				resource.owner = (String) receive.get("owner");
				resource.tags = new String[((JSONArray) receive.get("tags")).size()];
				for(int i = 0, count = resource.tags.length; i< count; i++)
				{
				    resource.tags[i] = (String) ((JSONArray) receive.get("tags")).get(i);

				}
				
				
				if (!(resource.uri == null)&&!resource.uri.isAbsolute()) {
					message.put("response",  "error");
					message.put("errorMessage",  "invalid resourceTemplate");
				} else if (resource.name.startsWith(" ") || resource.name.endsWith(" ") || resource.name.contains("\0")) {
					message.put("response",  "error");
					message.put("errorMessage",  "invalid resourceTemplate");
				} else if (resource.description.startsWith(" ") || resource.description.endsWith(" ") 
						|| resource.description.contains("\0")) {
					message.put("response",  "error");
					message.put("errorMessage",  "invalid resourceTemplate");
				} else if (resource.getChannel().startsWith(" ") || resource.getChannel().endsWith(" ") 
						|| resource.getChannel().contains("\0")) {
					message.put("response",  "error");
					message.put("errorMessage",  "invalid resourceTemplate");
				} else if (resource.owner.startsWith(" ") || resource.owner.endsWith(" ") || resource.owner.contains("\0") 
						|| resource.owner.equals("*")) {
					message.put("response",  "error");
					message.put("errorMessage",  "invalid resourceTemplate");
				} else if (command.get("relay") == null) {
					// if the relay field is not given
					message.put("response",  "error");
					message.put("errorMessage",  "missing resourceTemplate");
				} else {
					int sum = 0; // sum of the sizes in other servers
					// create a JSONObject List
					List<JSONObject> jslist = new ArrayList<JSONObject>();
					// if RELAY is true------------------------------------------------------------------
					//===================================================================================
					if (relay == true) {
						
						for (JSONObject record:serverrecords) {
							String hostname = (String) record.get("hostname");
			            	int port = Integer.parseInt(record.get("port").toString());
			            	try(Socket socket = new Socket(hostname,port)) {
			            		DataInputStream in = new DataInputStream(socket.getInputStream());
			        			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			        			/*
			        			out.writeUTF("Hello server");
			        			out.flush();
			        			*/
			        			// ----------------------send query to a new server---------------------------
			        			if (debugon == true) {
			        				System.out.println("quering to " + hostname + ":" + port);
			        			}
			        			
			        			
			        			JSONObject com = new JSONObject();
			        			// put resource in
			        			JSONObject send = new JSONObject(); 
			        			send.put("name", resource.name);
			    	        	send.put("description",resource.description);
			    	        	
			    	        	if(resource.uri == null) {
				        			send.put("uri", ""); //URI is not null
				        		} else {
				    	        	send.put("uri", resource.uri.toString());
				        		}
			    	        	
			    	        	JSONArray arr = new JSONArray();
			    	        	for(String tag: resource.tags) {
			    	        		arr.add(tag);
			    	        	}
			            		send.put("tags", arr);
			            		send.put("channel", ""); // channel changed!!!
			            		send.put("owner", ""); // owner changed!!!
			            		send.put("ezserver", null);
			    	        	com.put("resourceTemplate", send);
			    	        	com.put("command",  "QUERY");
			    	        	com.put("relay", false); //relay field changed!!!
			    	        	
			    	        	//WRITE the command to Server
			    				out.writeUTF(com.toJSONString());
			    				if (debugon == true) {
			    					System.out.println("SENT: " + com);
			    				}
			    	        	
			    				
			    				//----------------------read from other servers------------------
			    				while(true){
			    					if(in.available() > 0) {

					    				//CREATE a JSONPareser
					        			JSONParser js_parser = new JSONParser();
					        			
					        			JSONObject returned = (JSONObject) js_parser.parse(in.readUTF());
					        			if (returned.containsKey("response")) {
					        				if(returned.get("response").equals("error")){
					        					break; //do nothing
					        				}
					        			}
					        			if (returned.containsKey("name")) {// or any attribute else
					        				jslist.add(returned); //add to the list
					        				System.out.println("receive from other servers: " + returned);
					        			}
					        			if (returned.containsKey("resultSize")) {
					        				break; //break the while loop
					        			}
			    					}
			    				}
			        			
			        			//----------------------------------------------------------------------
			        			
			            	} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (org.json.simple.parser.ParseException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
					// check QUERY rules enforced by the server
					List<Resource> rslist = new ArrayList<Resource>();
					//boolean tagpresent = true;
					
					for (Resource c : resourcelist) {

						// The template channel equals (case sensitive) the
						// resource channel
						if (c.getChannel().equals(resource.getChannel())) {
							
							// 1. ALL exists--------------------------------------------------------------
							
							// Any tags present in the template also are present
							// in the candidate (case insensitive)
							if (resource.tags.length > 0
									&& Arrays.asList(c).containsAll(Arrays.asList(resource.tags))) {
								// If the template contains an owner that is not
								// "", then the candidate owner must equal it
								// (case sensitive)
								if (!resource.owner.equals("") && resource.owner.equals(c.owner)) {
									// If the template contains a URI then the
									// candidate URI matches (case sensitive)
									if (!(resource.uri == null) && resource.uri.equals(c.uri)) {
										if ((!resource.name.equals("") && c.name.contains(resource.name))
												|| (!resource.description.equals("") && c.name.contains(resource.name))
												|| (resource.description.equals("")) && (resource.name.equals(""))) {
											rslist.add(c);
										}
									}
								}
							}
							// 2-----------------------------------------------------------------------------
							// only if owner is ""
							if (resource.owner.equals("")) {
								// Any tags present in the template also are
								// present
								// in the candidate (case insensitive)
								if (resource.tags.length > 0
										&& Arrays.asList(c).containsAll(Arrays.asList(resource.tags))) {

									// If the template contains a URI then the
									// candidate URI matches (case sensitive)
									if (!(resource.uri == null) && resource.uri.equals(c.uri)) {
										if ((!resource.name.equals("") && c.name.contains(resource.name))
												|| (!resource.description.equals("") && c.name.contains(resource.name))
												|| (resource.description.equals("")) && (resource.name.equals(""))) {
											rslist.add(c);
										}
									}
								}

							}
							// 3------------------------------------------------------------------------
							// only if tags is []
							if (resource.tags.length == 0) {
								// If the template contains an owner that is not
								// "", then the candidate owner must equal it
								// (case sensitive)
								if (!resource.owner.equals("") && resource.owner.equals(c.owner)) {
									// If the template contains a URI then the
									// candidate URI matches (case sensitive)
									if (!(resource.uri == null) && resource.uri.equals(c.uri)) {
										if ((!resource.name.equals("") && c.name.contains(resource.name))
												|| (!resource.description.equals("") && c.name.contains(resource.name))
												|| (resource.description.equals("")) && (resource.name.equals(""))) {
											rslist.add(c);
										}
									}
								}
							}
							// 4--------------------------------------------------------------------------------
							// only if URI is ""
							if (resource.uri == null) {
								if (resource.tags.length > 0
										&& Arrays.asList(c).containsAll(Arrays.asList(resource.tags))) {
									// If the template contains an owner that is
									// not
									// "", then the candidate owner must equal
									// it
									// (case sensitive)
									if (!resource.owner.equals("") && resource.owner.equals(c.owner)) {

										if ((!resource.name.equals("") && c.name.contains(resource.name))
												|| (!resource.description.equals("") && c.name.contains(resource.name))
												|| (resource.description.equals("")) && (resource.name.equals(""))) {
											rslist.add(c);
										}

									}
								}
							}
							// 5----------------------------------------------------------------------------------
							// only if owner is "" and tag is []
							if (resource.owner.equals("") && resource.tags.length == 0) {

								// If the template contains a URI then the
								// candidate URI matches (case sensitive)
								if (!(resource.uri == null) && resource.uri.equals(c.uri)) {
									if ((!resource.name.equals("") && c.name.contains(resource.name))
											|| (!resource.description.equals("") && c.name.contains(resource.name))
											|| (resource.description.equals("")) && (resource.name.equals(""))) {
										rslist.add(c);
									}
								}

							}
							// 6---------------------------------------------------------------------------------------
							// only if owner is "" and URI is ""
							if (resource.owner.equals("") && (resource.uri == null)) {
								// Any tags present in the template also are
								// present
								// in the candidate (case insensitive)
								if (resource.tags.length > 0
										&& Arrays.asList(c).containsAll(Arrays.asList(resource.tags))) {

									if ((!resource.name.equals("") && c.name.contains(resource.name))
											|| (!resource.description.equals("") && c.name.contains(resource.name))
											|| (resource.description.equals("")) && (resource.name.equals(""))) {
										rslist.add(c);
									}

								}

							}
							// 7-------------------------------------------------------------------------------
							// only if tag is [] and URI is ""
							if ((resource.uri == null) && resource.tags.length == 0) {

								// If the template contains an owner that is not
								// "", then the candidate owner must equal it
								// (case sensitive)
								if (!resource.owner.equals("") && resource.owner.equals(c.owner)) {

									if ((!resource.name.equals("") && c.name.contains(resource.name))
											|| (!resource.description.equals("") && c.name.contains(resource.name))
											|| (resource.description.equals("")) && (resource.name.equals(""))) {
										rslist.add(c);
									}

								}

							}
							// 8---------------------------------------------------------------------------------
							// if owner is "" and tag is [] and URI is ""
							if (resource.owner.equals("") && (resource.uri == null)
									&& resource.tags.length == 0) {

								if ((!resource.name.equals("") && c.name.contains(resource.name))
										|| (!resource.description.equals("") && c.name.contains(resource.name))
										|| (resource.description.equals("")) && (resource.name.equals(""))) {
									rslist.add(c);
									//System.out.println("test query!");
								}
								
							}
							
						}
						
					}
					// query succeed
					message.put("response", "success");
					
					JSONObject size = new JSONObject();
					
					
					for(Resource r: rslist) {
						JSONObject o = new JSONObject();
						o.put("name", r.name);
						o.put("description", r.description);
						o.put("channel",r.getChannel());
						o.put("uri", r.uri.toString());
						if(!r.owner.equals(""))
							o.put("owner", "*");//The server will never reveal the owner of a resource in a response. If a resource has an owner then it will be
						//replaced with the "*" character
						else
							o.put("owner","");
						o.put("ezserver", r.ezserver);
						JSONArray arr = new JSONArray();
			        	for(String tag: r.tags) {
			        		arr.add(tag);
			        	}
		        		o.put("tags", arr);
						
						//seq.add(o);
		        		jslist.add(o);
					}
					size.put("resultSize", jslist.size());
					//seq.add(size);
					try {
						output.writeUTF(message.toJSONString());
						if (debugon == true) {
        	        		System.out.println("SENT: " + message);
        	        	}
						Iterator<JSONObject> iter = jslist.iterator();
						while(iter.hasNext()) {
							JSONObject js = iter.next();
							output.writeUTF(js.toJSONString());
							if (debugon == true) {
	        	        		System.out.println(js);
	        	        	}
						}
						output.writeUTF(size.toJSONString());
						if (debugon == true) {
        	        		System.out.println(size);
        	        	}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				
				//seq.add(message);
					
			} else if(command.get("command").equals("FETCH")) {
				// FETCH--------------------------------------------------------------------------
				
				receive = (JSONObject) command.get("resourceTemplate");
				resource.name = (String) receive.get("name");
				if(receive.get("uri").equals("")) {
					resource.uri = null;
				} else {
					resource.uri = URI.create(((String)receive.get("uri")));
				}
				resource.description = (String) receive.get("description");
				resource.changeChannel((String) receive.get("channel"));
				resource.owner = (String) receive.get("owner");
				resource.tags = new String[((JSONArray) receive.get("tags")).size()];
				for(int i = 0, count = resource.tags.length; i< count; i++)
				{
				    resource.tags[i] = (String) ((JSONArray) receive.get("tags")).get(i);

				}
				//File file = new File(resource.uri.getPath());
				
				if (resource.uri == null) {
					message.put("response",  "error");
					message.put("errorMessage",  "missing resource");
					try {
						output.writeUTF(message.toJSONString());
						if (debugon == true) {
        	        		System.out.println("SENT: " + message);
        	        	}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} // response:error
				} else if (!resource.uri.isAbsolute()
						|| !resource.uri.getScheme().equals("file")) {
					//The URI must be present, must be absolute, non-authoritative and must be a file scheme. It must point
					//to a file on the local file system that the server can read as a file.
					message.put("response",  "error");
					message.put("errorMessage",  "invalid resourceTemplate");
					try {
						output.writeUTF(message.toJSONString());
						if (debugon == true) {
        	        		System.out.println("SENT: " + message);
        	        	}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} // response:error
				} else {
					// declare the following things
					File f = null;
					JSONObject resultsize = new JSONObject();
					JSONObject j = new JSONObject();
					
					for (Resource r: resourcelist) {
						if (r.getChannel().equals(resource.getChannel())
								&&r.uri.equals(resource.uri)) {
							f = new File(resource.uri.getPath());
							// Create a new resource JSONObject
							j.put("name", r.name);
							j.put("channel", r.getChannel());
							JSONArray arr = new JSONArray();
				        	for(String tag: r.tags) {
				        		arr.add(tag);
				        	}
			        		j.put("tags", arr);
			        		j.put("uri", r.uri.toString());
							j.put("owner", r.owner);
							j.put("description", r.description);
							j.put("resourceSize", f.length());
							j.put("ezserver", r.ezserver);
						}
					}
					if (f != null) {
						if (f.exists()) {
							// Send this back to client so that they know what
							// the file is.

							try {
								message.put("response", "success");
								output.writeUTF(message.toJSONString());
								if (debugon == true) {
									System.out.println("SENT: " + message);
								}
								output.writeUTF(j.toJSONString());
								if (debugon == true) {
									System.out.println("Sending File: " + j);
								}
								// Start sending file
								RandomAccessFile byteFile = new RandomAccessFile(f, "r");
								byte[] sendingBuffer = new byte[512 * 512];
								int num;
								// While there are still bytes to send..
								while ((num = byteFile.read(sendingBuffer)) > 0) {
									System.out.println(num);
									output.write(Arrays.copyOf(sendingBuffer, num));
									System.out.println("test!");
								}
								byteFile.close();

								// resultSize must be 1
								resultsize.put("resultSize", 1);
								output.writeUTF(resultsize.toJSONString());
								if (debugon == true) {
									System.out.println(resultsize);
								}

							} catch (IOException e) {
								e.printStackTrace();
							}
						} else {
							// Throw an error here..
							message.put("errorMessage", "file does not exist");
							try {
								output.writeUTF(message.toJSONString());
								if (debugon == true) {
									System.out.println("SENT: " + message);
								}
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					} else {

						message.put("response", "success");
						//resultsize.put("resultSize", 0); // which has no valid
															// uri
						try {
							output.writeUTF(message.toJSONString());
							//output.writeUTF(resultsize.toJSONString());
							if (debugon == true) {
								System.out.println("SENT: " + message);
								System.out.println(resultsize);
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

				}
				//seq.add(message);
				
				
			} else if(command.get("command").equals("EXCHANGE")) {
				//EXCHANGE-------------------------------------------------------
				JSONArray record = (JSONArray) command.get("serverList");
				boolean validserver = true;
				
				if(record.isEmpty()) {
					message.put("response",  "error");
					message.put("errorMessage",  "missing serverList");
				} else {
					// =========Test the validation of server records=========
					for(Object server:record) {
						//System.out.println(((JSONObject)server).get("hostname"));
						URI uri;
						try {
							uri = new URI("my://" + ((JSONObject)server).get("hostname")+":"+((JSONObject)server).get("port").toString());
							String host = uri.getHost();
							int portnum = uri.getPort();
							 if (uri.getHost() == null || uri.getPort() == -1) {
								 validserver = false;
							 }
						} catch (URISyntaxException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} 		
						//=====================================================
					}
					if (validserver == false) {
						message.put("response", "error");
						message.put("errorMessage",  "invalid serverList");
					} else {
						
						for(Object server:record) {
							
							JSONObject o = (JSONObject)server;
							if (Integer.parseInt(o.get("port").toString()) == port &&
									o.get("hostname").equals(advertisedhostname)) {
								System.out.println("remove itself from the list.");
							}else if(serverrecords.contains(server)){
								System.out.println("the record already existed");
							}
							else {
								serverrecords.add((JSONObject) server);
							}
							
						}
						message.put("response", "success");
					}
					
				}
				try {
					output.writeUTF(message.toJSONString());
					if (debugon == true) {
    	        		System.out.println("SENT: " + message);
    	        	}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}//----------------------------------finished--------------------------------
			else {
				// there are other commands rather than "publish", "remove", ...
				message.put("response",  "error");
				message.put("errorMessage",  "invalid command");
				//seq.add(message);
				try {
					output.writeUTF(message.toJSONString());
					if (debugon == true) {
    	        		System.out.println("SENT: " + message);
    	        	}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			
		//	there is no command
		
		} else {
			message.put("response",  "error");
			message.put("errorMessage",  "missing or incorrect type for command");
			
			try {
				output.writeUTF(message.toJSONString());
				if (debugon == true) {
	        		System.out.println("SENT: " + message);
	        	}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return seq; // no use here!!!!!!!!!!!!!!!!!!
	}
	
	private synchronized static void serverClient(Socket client) {
		try(Socket clientSocket = client) {
			JSONParser parser = new JSONParser();
			DataInputStream input = new DataInputStream(clientSocket.getInputStream());
			DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());

			//-----------------------handling with multiple threads--------------------------
			boolean isAlive = true;
			while(isAlive) {
				if(input.available() > 0) {
					
					JSONObject command = (JSONObject)parser.parse(input.readUTF());
					
					if (debugon == true) {
						System.out.println("RECEIVED: " + command);
					}
					
					
					JSONArray message = parseCommand(command, output);
					isAlive = false; // change it to false when finishing the task!!!!!!!!!
					shutdown = true;
					/*delete these stuff
					if (debugon == true) {
						System.out.println("SENT: ");
						for (Object o: message) {
							System.out.println(o);
						}
					}
					JSONObject results = new JSONObject();
					results.put("results", message);
					output.writeUTF(results.toJSONString());*/
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (org.json.simple.parser.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}