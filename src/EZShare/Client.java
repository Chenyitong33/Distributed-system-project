package EZShare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;

public class Client {

	private static String ip = "localhost"; // 127.0.0.1
	private static int port = 8080;
	private static boolean debugon = false; // DEBUG
	private static boolean valid = true; // VALID 
	private static boolean shutdown = false;
	public static void main(String[] args) {

        Options options = new Options();
        // client command line arguments
        options.addOption("channel",true,"channel");
        options.addOption("debug",false,"print debug information");
        options.addOption("description",true,"resource description");
        options.addOption("exchange",false,"exchange server list with server");
        options.addOption("fetch",false,"fetch resources from server");
        options.addOption("host",true,"server host, a domain name or IP address");
        options.addOption("name",true,"resource name");
        options.addOption("owner",true,"owner");
        options.addOption("port",true,"server port, an integer");
        options.addOption("publish",false,"publish resource on server");
        options.addOption("query",false,"query for resources from server");
        options.addOption("remove",false,"remove resource from server");
        options.addOption("secret",true,"secret");
        options.addOption("servers",true,"server list, host1:port1,host2:port2,...");
        options.addOption("share",false,"share resource on server");
        options.addOption("tags",true,"resource tags, tag1,tag2,tag3,...");
        options.addOption("uri",true,"resource URI");
        

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
			cmd = parser.parse(options,args);
		} catch (org.apache.commons.cli.ParseException e) {
			// TODO Auto-generated catch block
			System.err.println( "Parsing failed.  Reason: " + e.getMessage() );
			try {
				cmd = parser.parse(options, new String[]{ "-debug" });// set debugon
				valid  = false;
			} catch (org.apache.commons.cli.ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
		}
        // automatically generate the help statement
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "ant", options );
        
        // change HOST
        if(cmd.hasOption("host")) {
        	ip = cmd.getOptionValue("host");
        }
        
        // change PORT
        if(cmd.hasOption("port")) {
        	port = Integer.parseInt(cmd.getOptionValue("port"));
        }
        
		try(Socket socket = new Socket(ip,port)) {
			DataInputStream input = new DataInputStream(socket.getInputStream());
			DataOutputStream output = new DataOutputStream(socket.getOutputStream());
			/*
			output.writeUTF("Hello server");
			output.flush();
			*/

	        JSONObject command = new JSONObject();
	        Resource resource = new Resource();	        
	        JSONObject send = new JSONObject(); // which we can put resource in 

	        
	        // change the ezserver
	        resource.ezserver = ip + ":" + port;
	        
	        // DEBUG command--------------------------------------------
	        if (cmd.hasOption("debug")) {
	        	debugon = true;
	        }
	        
			if(debugon == true) {
				System.out.println("setting debug on");
			}
	        
	        if(valid == true) {

		        // PUBLISH command------------------------------------------
		        if(cmd.hasOption("publish")){
		        	if(cmd.hasOption("remove")
		        			||cmd.hasOption("share")
		        			||cmd.hasOption("query")
		        			||cmd.hasOption("exchange")
		        			||cmd.hasOption("fetch")) {
		        		command.put("command",  "REDUNDANT");
		        		System.out.println("jfdkjfakj");
		        	} else {

			        	if(debugon == true) {
				        	System.out.println("publishing to " + ip + ":" + port);
			        	}
			        	
			        	if(cmd.hasOption("name")) {
			        		resource.name = cmd.getOptionValue("name");
			        		
			        	}
			        	if(cmd.hasOption("description")) {
			        		resource.description = cmd.getOptionValue("description");
			        		
			        	}
			        	if(cmd.hasOption("tags")) {
			        		resource.tags = cmd.getOptionValue("tags").split(",");
			        	
			        	}
			        	if(cmd.hasOption("channel")) {
			        		
			        		resource.changeChannel(cmd.getOptionValue("channel"));
			        		
			        	}
			        	if(cmd.hasOption("owner")) {
			        		resource.owner = cmd.getOptionValue("owner");
			        		
			        	}
			        	
			        	// MANDANTORY URI
				        if(cmd.hasOption("uri")) {
				        	resource.uri = URI.create((cmd.getOptionValue("uri")));
				        }
			        	
			        	send.put("name", resource.name);
			        	send.put("description",resource.description);
			        	JSONArray arr = new JSONArray();
			        	for(String tag: resource.tags) {
			        		arr.add(tag);
			        	}
		        		send.put("tags", arr);
		        		send.put("channel", resource.getChannel());
		        		send.put("owner", resource.owner);
		        		if(resource.uri == null) {
		        			send.put("uri", "");
		        		} else {
		    	        	send.put("uri", resource.uri.toString());
		        		}
			        	send.put("ezserver", resource.ezserver);
			        	command.put("resource", send);
			        	command.put("command",  "PUBLISH");
		        	}
		        	
		        } 

				// REMOVE command-------------------------------------------
				if (cmd.hasOption("remove")) {
					if(cmd.hasOption("publish")
		        			||cmd.hasOption("share")
		        			||cmd.hasOption("query")
		        			||cmd.hasOption("exchange")
		        			||cmd.hasOption("fetch")) {
						command.put("command",  "REDUNDANT");
					} else {

						if(debugon == true) {
				        	System.out.println("removing to " + ip + ":" + port);
			        	}
						
						if (cmd.hasOption("channel")) {
							resource.changeChannel(cmd.getOptionValue("channel"));
						}

						if (cmd.hasOption("owner")) {
							resource.owner = cmd.getOptionValue("owner");
						}

						if (cmd.hasOption("uri")) {
							resource.uri = resource.uri = URI.create((cmd.getOptionValue("uri")));
						}

						send.put("channel", resource.getChannel());
						send.put("owner", resource.owner);
						if(resource.uri == null) {
		        			send.put("uri", "");
		        		} else {
		    	        	send.put("uri", resource.uri.toString());
		        		}
						command.put("resource", send);
						command.put("command", "REMOVE");
					}

				}
		        // SHARE command---------------------------------------------
		        if(cmd.hasOption("share")){
		        	if(cmd.hasOption("remove")
		        			||cmd.hasOption("publish")
		        			||cmd.hasOption("query")
		        			||cmd.hasOption("exchange")
		        			||cmd.hasOption("fetch")) {
		        		command.put("command",  "REDUNDANT");
		        	} else {

			        	if(debugon == true) {
				        	System.out.println("sharing to " + ip + ":" + port);
			        	}
			        	
			        	if(cmd.hasOption("name")) {
			        		resource.name = cmd.getOptionValue("name");
			        		
			        	}
			        	if(cmd.hasOption("description")) {
			        		resource.description = cmd.getOptionValue("description");
			        		
			        	}
			        	if(cmd.hasOption("tags")) {
			        		resource.tags = cmd.getOptionValue("tags").split(",");
			        	}
			        	if(cmd.hasOption("channel")) {
			        		
			        		resource.changeChannel(cmd.getOptionValue("channel"));
			        		
			        	}
			        	if(cmd.hasOption("owner")) {
			        		resource.owner = cmd.getOptionValue("owner");
			        	}
			        	
			        	// MANDANTORY URI
				        if(cmd.hasOption("uri")) {
				        	resource.uri = URI.create((cmd.getOptionValue("uri")));
				        }
				        // MANDANTORY secret
				        if(cmd.hasOption("secret")) {
				        	command.put("secret", cmd.getOptionValue("secret"));
				        } else {
				        	command.put("secret", null);
				        }
			        	send.put("name", resource.name);
			        	send.put("description",resource.description);
		        		
			        	JSONArray arr = new JSONArray();
			        	for(String tag: resource.tags) {
			        		arr.add(tag);
			        	}
		        		send.put("tags", arr);
		        		send.put("channel", resource.getChannel());
		        		send.put("owner", resource.owner);
		        		if(resource.uri == null) {
		        			send.put("uri", "");
		        		} else {
		    	        	send.put("uri", resource.uri.toString());
		        		}
			        	send.put("ezserver", resource.ezserver);
			        	command.put("resource", send);
			        	command.put("command",  "SHARE");
		        	}
		        	
		        }
		        // QUERY command------------------------------------------------
		        if(cmd.hasOption("query")){
		        	if(cmd.hasOption("remove")
		        			||cmd.hasOption("share")
		        			||cmd.hasOption("publish")
		        			||cmd.hasOption("exchange")
		        			||cmd.hasOption("fetch")) {
		        		command.put("command",  "REDUNDANT");
		        	} else {

			        	if(debugon == true) {
				        	System.out.println("quering to " + ip + ":" + port);
			        	}
			        	
			        	if(cmd.hasOption("name")) {
			        		resource.name = cmd.getOptionValue("name");
			        	} 
			        	if(cmd.hasOption("description")) {
			        		resource.description = cmd.getOptionValue("description");
			        	} 
			        	if(cmd.hasOption("tags")) {
			        		resource.tags = cmd.getOptionValue("tags").split(",");
			        	}
			        	if(cmd.hasOption("channel")) {
			        		
			        		resource.changeChannel(cmd.getOptionValue("channel"));
			        		
			        	}
			        	if(cmd.hasOption("owner")) {
			        		resource.owner = cmd.getOptionValue("owner");
			        	}

			        	if(cmd.hasOption("uri")) {
				        	resource.uri = URI.create((cmd.getOptionValue("uri")));
				        } 
			        	
			        	if(resource.uri == null) {
		        			send.put("uri", ""); //URI is not null!!!!!!!!
		        		} else {
		    	        	send.put("uri", resource.uri.toString());
		        		}
			        	send.put("name", resource.name);
			        	send.put("description",resource.description);
		        		//send.put("tags", Arrays.toString(resource.tags));
			        	JSONArray arr = new JSONArray();
			        	for(String tag: resource.tags) {
			        		arr.add(tag);
			        	}
		        		send.put("tags", arr);
		        		send.put("channel", resource.getChannel());
		        		send.put("owner", resource.owner);
		        		send.put("ezserver", null);
			        	command.put("resourceTemplate", send);
			        	command.put("command",  "QUERY");
			        	command.put("relay", true); //relay field
		        	}
		        }
		        // FETCH command---------------------------------------------
		        if(cmd.hasOption("fetch")){
		        	if(cmd.hasOption("remove")
		        			||cmd.hasOption("share")
		        			||cmd.hasOption("query")
		        			||cmd.hasOption("exchange")
		        			||cmd.hasOption("publish")) {
		        		command.put("command",  "REDUNDANT");
		        	} else {

			        	if(debugon == true) {
				        	System.out.println("fetching to " + ip + ":" + port);
			        	}
			        	
			        	// should have channel
			        	if(cmd.hasOption("channel")) {
			        	
			        		resource.changeChannel(cmd.getOptionValue("channel"));
			        		
			        	}
			        	// should have uri
			        	if(cmd.hasOption("uri")) {
				        	resource.uri = URI.create((cmd.getOptionValue("uri")));
				        } 
			        	
			        	if(resource.uri == null) {
		        			send.put("uri", "");// URI is not null!!!!!!!!!!!!!
		        		} else {
		    	        	send.put("uri", resource.uri.toString());
		        		}
			        	
			        	send.put("name", resource.name);
			        	send.put("description",resource.description);
			        	JSONArray arr = new JSONArray();
		        		send.put("tags", arr);
		        		send.put("channel", resource.getChannel());
		        		send.put("owner", resource.owner);
		        		send.put("ezserver", null);
			        	command.put("resourceTemplate", send);
			        	command.put("command",  "FETCH");
		        	}
		        }
		        // EXCHANGE command---------------------------------------------
		        if(cmd.hasOption("exchange")){
		        	if(cmd.hasOption("remove")
		        			||cmd.hasOption("share")
		        			||cmd.hasOption("query")
		        			||cmd.hasOption("fetch")
		        			||cmd.hasOption("publish")) {
		        		command.put("command",  "REDUNDANT");
		        	} else {

			        	if(debugon == true) {
				        	System.out.println("exchanging to " + ip + ":" + port);
			        	}
			        	
			        	JSONArray servers = new JSONArray();
			        	// should have servers
			        	if(cmd.hasOption("servers")) {
			        		if (cmd.getOptionValue("servers").isEmpty()) {
			        			
			        		} else {
			        			String[] serverlist = cmd.getOptionValue("servers").split(",");
				        		for(String s :serverlist) {
				        			String[] separate = s.split(":");
				        			JSONObject server = new JSONObject();
				        			if(separate.length>1) {
				        				server.put("hostname", separate[0]);
				        				server.put("port", Integer.parseInt(separate[1]));
				        			} else {
				        				//server = null;
				        			}
				        			servers.add(server);	
				        		}
			        		}
			        		
			        	}
			        	
			        	command.put("serverList", servers);
			        	command.put("command",  "EXCHANGE");
		        	}
		        	
		        }
		        // if ALL valid commands have not shown
		        if(!cmd.hasOption("exchange")&&!cmd.hasOption("publish")&&(cmd.hasOption("remove"))
		        	&&(cmd.hasOption("share"))&&(cmd.hasOption("query"))&&(cmd.hasOption("fetch"))) {
		        	// do not put "command" in KEYS!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		        }
		         
	        } else {// if the command is like "pub",...
	        	
	        	command.put("command","INVALID");
				
	        }
	        // ----------------------Get input from the server--------------------
	        // print sent information...
	        if (debugon == true) {
	        	System.out.println("SENT: " + command);
	        }
	        
	        // WRITE the command to the server
			output.writeUTF(command.toJSONString());

			//CREATE a JSONPareser
			JSONParser js_parser = new JSONParser();
			
			// input > 0
			while(!shutdown){
				if(input.available() > 0)
				{
					// if FETCH,
					// DOWNLOAD the file------------------------------------------
					if (cmd.hasOption("fetch")) {
						String response = input.readUTF();
						
						//String size = input.readUTF();
						if (debugon == true) {
							System.out.println("RECEIVED: " + response);
							
						}
						// get the response from the server
						JSONObject status = (JSONObject) js_parser.parse(response);
						
						// if the response is success and file existed
						if (status.get("response").equals("success")) {
							String result = input.readUTF();
							if (debugon == true) {
								System.out.println(result);
								}
							JSONObject fetched = (JSONObject) js_parser.parse(result);
							// The file location
							File f = new File((URI.create((String) fetched.get("uri"))).getPath());
							String fileName = "C:/Users/frankie/workspace/EZShare/client_files/" + f.getName();

							// Create a RandomAccessFile to read and write the
							// output file.
							RandomAccessFile downloadingFile = new RandomAccessFile(fileName, "rw");

							// Find out how much size is remaining to get from
							// the
							// server.
							long fileSizeRemaining = (Long) fetched.get("resourceSize");

							int chunkSize = setChunkSize(fileSizeRemaining);

							// Represents the receiving buffer
							byte[] receiveBuffer = new byte[chunkSize];

							// Variable used to read if there are remaining size
							// left to read.
							int num;

							if (debugon == true) {
								System.out.println("Downloading " + fileName + " of size " + fileSizeRemaining);
							}

							while ((num = input.read(receiveBuffer)) > 0) {
								// Write the received bytes into the
								// RandomAccessFile
								downloadingFile.write(Arrays.copyOf(receiveBuffer, num));
								// Reduce the file size left to read..
								fileSizeRemaining -= num;

								// Set the chunkSize again
								chunkSize = setChunkSize(fileSizeRemaining);
								receiveBuffer = new byte[chunkSize];
								System.out.println(fileSizeRemaining);
								// If you're done then break
								if (fileSizeRemaining == 0) {
									break;
								}
							}
							if (debugon == true) {
								System.out.println("File received!");
							}

							downloadingFile.close();
							String resultsize = input.readUTF();
							
							if (debugon == true) {
								System.out.println(resultsize);
							}
						} else {
							
							//String size = input.readUTF();
							if (debugon == true) {
								System.out.println("RECEIVED: " + response);
								
							}
						}
						
					} else {
						// Regular receive-------------------------------------------------------------
						JSONObject result = (JSONObject) js_parser.parse(input.readUTF());
						if (debugon == true) {
							System.out.println("RECEIVED: " + result);
						}
					}
					
					/*
					// extract the first and last [ ]
					JSONArray array = (JSONArray) result.get("results");
					if (debugon == true) {
						System.out.println("RECEIVED: ");
						for (Object message: array) {
							//if(((JSONObject) message).containsKey("command_name"))
								//fetched = (JSONObject) message;
							System.out.println(message);
						}
					}
					*/
					
					// result.get("results").toString().substring(1,
					// result.get("results").toString().length()-1));
				} else {
					shutdown = true;
				}
			}
			socket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public static int setChunkSize(long fileSizeRemaining){
		// Determine the chunkSize
		int chunkSize=512*512;
		
		// If the file size remaining is less than the chunk size
		// then set the chunk size to be equal to the file size.
		if(fileSizeRemaining<chunkSize){
			chunkSize=(int) fileSizeRemaining;
		}
		
		return chunkSize;
	}

}
