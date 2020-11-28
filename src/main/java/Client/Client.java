package Client;

import java.io.*; 
import java.net.*;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import Daemons.FileScannerDaemon;
import Daemons.VersioningDaemon;
import javafx.application.Platform;


public class Client implements Runnable
{
	//Replace the following three String paths with the ones on this machine.
    String clientPath = "C:\\Users\\user\\Desktop\\Xariot client directory1";
    String clientKeyPath = "C:\\Users\\user\\Desktop\\clientKeys";
    String conflictPath = "C:\\Users\\user\\Desktop\\Client1 conflicting dir";
    boolean flag = false;
    private static int priorityCounter = 0;
    private static String publicKeyServerDir = "";
    String passphrase, username, pubKeyPath, privKeyPath;
    PrintStream outToServer;
    BufferedReader inFromServer ;
    int panel;
	
	public Client (String username, String pubKeyPath, String privKeyPath, String password, String dirName, int panelNum) {
		this.passphrase = password;
		this.pubKeyPath = pubKeyPath;
		this.privKeyPath = privKeyPath;
		this.username = username;
		this.publicKeyServerDir = dirName;
		this.panel = panelNum;
	}
	
	public PrintStream getOut() {
		return outToServer;
	}
	
	public BufferedReader getIn() {
		return inFromServer;
	}

	public void connect(String host, int port, int simmulPort, int filePort, int FSDPort, int VDPort, int KeyPort, int KeyFilePort) throws Exception
	{
		Socket keyExchangeSocket;
		Socket keyFileExchangeSocket;
		
		//Key exchange parts
		PrintStream keyOutToServer;
		BufferedReader keyInFromServer;
		DataOutputStream keydos;
		DataInputStream keydis;
		BufferedReader keyPathUser;

		try {
				keyExchangeSocket = new Socket(host, KeyPort);
				keyFileExchangeSocket = new Socket(host, KeyFilePort);
				
				keyPathUser = new BufferedReader( new InputStreamReader( System.in ) ); 

				keyOutToServer = new PrintStream( keyExchangeSocket.getOutputStream(), true); 
				keyInFromServer = new BufferedReader(new InputStreamReader( keyExchangeSocket.getInputStream()));
				
				keydos = new DataOutputStream(keyFileExchangeSocket.getOutputStream());
				keydis = new DataInputStream(keyFileExchangeSocket.getInputStream());
				
				File [] filesInKeystoreDir = new File(clientKeyPath).listFiles();
				if (filesInKeystoreDir == null) {
					System.out.println("You have no public/private keystore pair! Generate one and come back again! ");
				}
				else {
					for(File file : filesInKeystoreDir) {
						if(file.getName().equals("server.public")) {
							flag = true;
							String clientSelection = "";
							String serverResponse = "";
							System.out.println("You have the server's public key, you have connected once already! ");
							System.out.println("Please input the directory name you typed last time you connected to the system! ");
							clientSelection = keyPathUser.readLine();
							publicKeyServerDir = clientSelection;
							keyOutToServer.println("C." + clientSelection);
							serverResponse = keyInFromServer.readLine();
							if(serverResponse.equals("ok")) {
								System.out.println("The server has found your public key in file: " + clientSelection );
								System.out.println("Initiating ssl connection to the server. ");
							}
							else {
								String passName = "";
								System.out.println("The server has NOT found your public key in file: " + clientSelection );
								System.out.println("Please type the path of your public keystore in your system. " );
								clientSelection = keyPathUser.readLine();
								
								File clientPublic = new File(clientSelection);
								
								System.out.println("Please input a directory name, where your public key will be saved on the server. ");
								System.out.println("You can use the same directory name to help the server recognise your public key in it's system. ");
								passName = keyPathUser.readLine();
								publicKeyServerDir = passName;
								
								//K.filesize,dirname
								try {
									transportFile(clientPublic, keyOutToServer, keydos, passName);
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
					}
					if(!flag) {
						String serverResponse = "";
						String clientKeystorePath = "";
						String passName = "";
						
						System.out.println("Please input the path of your public keystore! ");
						clientKeystorePath = keyPathUser.readLine();
						File clientPublic = new File(clientKeystorePath);
						
						System.out.println("Please input a directory name, where your public key will be saved on the server. ");
						System.out.println("You can use the same directory name to help the server recognise your public key in it's system. ");
						passName = keyPathUser.readLine();
						publicKeyServerDir = passName;
						
						try {
							transportFile(clientPublic, keyOutToServer, keydos, passName);
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						serverResponse = keyInFromServer.readLine();
						if(serverResponse.charAt(0) == 'K') {
							String tidyServerMessage = serverResponse.substring(2, serverResponse.length());
							String serverPublicPath = clientKeyPath + File.separator + "server.public";
							File serverPublic = new File(serverPublicPath);
							receiveFile(tidyServerMessage, serverPublicPath,keydis);
							System.out.println("Server public key written! ");
						}						
					}
				}
		}
		catch (UnknownHostException e) {
			System.out.println("Can not locate host/port " + host + "/" + KeyPort + "/" + KeyFilePort);
			return;
        } catch (IOException e) {
			System.out.println("Could not establish connection to: " + host + "/" + KeyPort + "/" + KeyFilePort);
			return;
        }
		
		System.out.println("---Receiving priority from server.--- ");
		priorityCounter = Integer.parseInt(keyInFromServer.readLine());

		keyOutToServer.close();
		keyInFromServer.close();
		keydos.close();
		keydis.close();
		keyExchangeSocket.close();
		keyFileExchangeSocket.close();
		
		/* Our socket end MAKE WITH SSL*/
		System.out.println("Started generating secure random and getting the keystores. ");
		//Stop time here?
		
		SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextInt();
		
		KeyStore clientKeyStore = null;
		KeyStore serverKeyStore = null;
		SSLContext sslContext = null;
		SSLSocketFactory sf = null;

		String passphrase = "";
		System.out.println("Please input the password of your private key. ");
		passphrase = keyPathUser.readLine();
		
	    try {
	    	clientKeyStore = setupClientKeyStore(clientKeyPath,passphrase,clientKeyStore);
	    	serverKeyStore = setupServerKeyStore(clientKeyPath,serverKeyStore);
	        sslContext = setupSSLContext(serverKeyStore, clientKeyStore, passphrase, sslContext, secureRandom);
	        sf = sslContext.getSocketFactory();
	      } catch( GeneralSecurityException gse ) {
	        gse.printStackTrace();
	      } catch( IOException ie ) {
	        ie.printStackTrace();
	      }
	    
		Socket clientSocket = null;
		Socket clientFileSocket = null;
		Socket clientSimmulSocket = null;
		Socket clientFSDSocket = null;
		Socket clientVDSocket = null;
		
		//Create 2 SSL sockets
		clientSocket = sf.createSocket(host,port + priorityCounter);
		clientFileSocket = sf.createSocket(host,filePort + priorityCounter);
		clientSimmulSocket = sf.createSocket(host,simmulPort + priorityCounter);
		clientFSDSocket = sf.createSocket(host,FSDPort + priorityCounter);
		clientVDSocket = sf.createSocket(host, VDPort + priorityCounter);
		    
		/* For writing to socket */
		PrintStream outToServer;
		// For reading from socket */
		BufferedReader inFromServer;          
		/* For reading from user */
		BufferedReader inFromUser;
		//Input stream for files
		DataInputStream dis;
		//Output stream for files
		DataOutputStream dos;
		
		//2 more for open files daemon
		PrintStream outToSimmul;
		BufferedReader inFromSimmul;  
		
		//more for DAEMONS
		PrintStream FSDoutToServer;
		PrintStream VDoutToServer;
		BufferedReader FSDinFromServer;
		BufferedReader VDinFromServer;
		
		String inviteUsername = null, serverOutput = null, publicKeyFolderName = null;
		//USERNAME FOR TESTING PURPOSES (make it dynamic later)
        String selection = "";
        int portnum = port + priorityCounter;
		System.out.println("-- Client connecting to host:port " + host + ":" + portnum + " --");
			/* Connect to the server at the specified host/port */
            try {
				/* Create a buffer to hold the user's input */
				inFromUser = new BufferedReader( new InputStreamReader( System.in ) ); 
				/* Create a writing buffer to the socket */
				outToServer = new PrintStream( clientSocket.getOutputStream(), true); 
				/* Create a reading buffer to the socket */
				inFromServer = new BufferedReader(new InputStreamReader( clientSocket.getInputStream()) );
				//file input/same for files & daemon sockets
				dis = new DataInputStream (clientFileSocket.getInputStream());
				//file output
				dos = new DataOutputStream (clientFileSocket.getOutputStream());
				
				outToSimmul = new PrintStream( clientSimmulSocket.getOutputStream(), true); 
				inFromSimmul = new BufferedReader(new InputStreamReader( clientSimmulSocket.getInputStream()) );
					
				FSDoutToServer = new PrintStream( clientFSDSocket.getOutputStream(),true);
				FSDinFromServer = new BufferedReader(new InputStreamReader(clientFSDSocket.getInputStream()));
				
				VDoutToServer = new PrintStream(clientVDSocket.getOutputStream(),true);
				VDinFromServer = new BufferedReader(new InputStreamReader(clientVDSocket.getInputStream()));
            
            } catch (UnknownHostException e) {
				System.out.println("Can not locate host/port " + host + "/" + port);
				return;
            } catch (IOException e) {
				System.out.println("Could not establish connection to: " + host + "/" + port);
				return;
            }
            try
            {
            	//change invites accepted via email
            	Scanner scanner = new Scanner(System.in);
				String username = "";
				System.out.println("Please input the username given by the master user in your email. ");
				username = scanner.nextLine();

				outToServer.println("7." + username + "," + publicKeyServerDir);	
				serverOutput = inFromServer.readLine(); 
				if(serverOutput.equals("ok")) {
					System.out.println("You are accepted in Xariot! ");	
				}
				else {
					System.out.println("Error! ");
					//should crash it? with no survivors
				}
	           	do 
	           	{
					System.out.println( "Welcome to SocketSync " + username + "." );
					System.out.println("------Main Menu------");
					System.out.println("1.Write a comment for a project. ");
					System.out.println("2.Look at all the comments for a project. ");					
					System.out.println("0.Exit. ");
					System.out.println("Navigate by using numbers 1-9");
					
					//Versioning / download daemon
					VersioningDaemon v1 = new VersioningDaemon(VDoutToServer,VDinFromServer,dis,dos,clientPath,username);
					v1.run();
					
    				File [] clientDirs = new File[0];
					clientDirs = new File (clientPath).listFiles();

					for(File file : clientDirs) {
						if (file.isDirectory()) {
							Timer scannerTimer = new Timer(true);
							scannerTimer.schedule(new FileScannerDaemon(username, conflictPath, file.getPath(), FSDoutToServer, FSDinFromServer, outToSimmul, inFromSimmul, dos, dis), 0, 4000);
						}
					}
					
					Timer versioningDaemon = new Timer(true);
    				versioningDaemon.schedule(new VersioningDaemon(VDoutToServer,VDinFromServer,dis,dos,clientPath,username), 6000, 8000);
    				
    				selection = scanner.nextLine();
	        		switch (selection)
	        		{
	        			case "1":
							System.out.println("Submit a comment by first inputing the project name. ");
							String projName = inFromUser.readLine();	
							
							System.out.println("Now type your comment (Max 300 letters). ");
							String comment = inFromUser.readLine();
							
							outToServer.println("8." + projName + "," + comment + "," + username);	
							serverOutput = inFromServer.readLine(); 
							if(serverOutput.equals("ok")) {
								System.out.println("Comment submitted! ");	
							}
							else if(serverOutput.equals("noaccess")){
								System.out.println("Error submitting comment. You do not have access to this project! ");
							}
							else if(serverOutput.equals("notexist")) {
								System.out.println("Error submitting comment. The project: " + projName + " does not exist! ");
							}
							break;
	        			case"2":
							System.out.println("Put the project name you want to see all comments for. ");
							String projectName = inFromUser.readLine();	
							outToServer.println("9." + projectName + "," + username);
							
							serverOutput = inFromServer.readLine(); 
							if(serverOutput.equals("ok")) {
								String response = inFromServer.readLine();
			        			ArrayList<String> serverSentenceArrayList = new ArrayList<>();
			        			Collections.addAll(serverSentenceArrayList, response.split("\\s*,\\s*"));
			        			while(!serverSentenceArrayList.isEmpty()) {
			        				System.out.println("Comments for: " + projectName);
			        				String user = serverSentenceArrayList.get(0);
			        				serverSentenceArrayList.remove(0);
			        				System.out.println(user + ": ");
			        				String text = serverSentenceArrayList.get(0);
			        				serverSentenceArrayList.remove(0);
			        				System.out.println("-" + text);
			        			}
							}
							else if(serverOutput.equals("noaccess")){
								System.out.println("Error reading comments. You do not have access to this project! ");
							}
							else if(serverOutput.equals("nocomments")) {
								System.out.println("There are no comments for this project! ");
							}
	        				break;
	        		}
	        		//selection ="";
            	}
	           	while (selection != "0");
            	System.out.println("Exiting ");
            	scanner.close();
                // Close all of our connections.
                outToServer.close();
                inFromServer.close();
                clientSocket.close();
                VDoutToServer.close();
                VDinFromServer.close();
                FSDinFromServer.close();
                FSDoutToServer.close();
                outToSimmul.close();
                inFromSimmul.close();
                dos.close();
                dis.close();
                
            } catch (IOException e) {
				System.out.println("I/O to socket failed: " + host);
            }
	}

	public static void main( String[] argv ) throws IOException, InterruptedException
	{
		String server;
		int port, filePort, keyPort, FSDPort, VDPort, keyFilePort,simmulPort;
		
		server = "localhost";
		port = 5500;
		simmulPort = 5550;
		filePort = 5600;
		FSDPort = 6050;
		VDPort = 6100;
		keyPort = 5800;
		keyFilePort = 5900;
		

		Client myclient = new Client(null, null, null ,null, null, 0);
		try {
			myclient.connect(server, port, simmulPort, filePort, FSDPort , VDPort, keyPort, keyFilePort);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("<-- Client has exited -->");
    } 	
	
	public void transportFile(File file, PrintStream out, DataOutputStream dos, String dirName) throws IOException {
		long fileSize = file.length();
	    out.println("K." + fileSize + "," + dirName); //obsolete not used in client
		byte b[] = new byte[(int) file.length()];
	    try (FileInputStream fileInputStream = new FileInputStream(file.getPath())) {
            fileInputStream.read(b);
            fileInputStream.close();
	    }
	    dos.write(b);
		System.out.println("Sent " + fileSize + " bytes to server.");
	    dos.flush();
		//DataFileOs.flush();
    }
    public void receiveFile(String fileSizeString, String FileDirAndName, DataInputStream dis) throws IOException {
		long fileSize = Long.parseLong(fileSizeString);
		byte []b=new byte[(int) fileSize];
		dis.read(b);
		System.out.println("Read " + fileSize + " bytes.");

	    try (FileOutputStream fos = new FileOutputStream(FileDirAndName,false)) {
	        fos.write(b);
	        fos.close();
	    }
    }
	private static KeyStore setupServerKeyStore(String keystorePath, KeyStore serverKeyStore) throws GeneralSecurityException, IOException {
		serverKeyStore = KeyStore.getInstance( "JKS" );
	    serverKeyStore.load( new FileInputStream( keystorePath + File.separator + "server.public" ), 
	                        "public".toCharArray() );
	    return serverKeyStore;
	}
	private static KeyStore setupClientKeyStore(String keystorePath, String passphrase, KeyStore clientKeyStore) throws GeneralSecurityException, IOException {
		clientKeyStore = KeyStore.getInstance( "JKS" );
	    clientKeyStore.load( new FileInputStream( keystorePath + File.separator + "client.private" ),
	                       passphrase.toCharArray() );
	    return clientKeyStore;
	}
	private static SSLContext setupSSLContext(KeyStore serverKeyStore, KeyStore clientKeyStore, String passphrase, SSLContext sslContext, SecureRandom secureRandom) throws GeneralSecurityException, IOException {
	    TrustManagerFactory tmf = TrustManagerFactory.getInstance( "SunX509" );
	    tmf.init( serverKeyStore );

	    KeyManagerFactory kmf = KeyManagerFactory.getInstance( "SunX509" );
	    kmf.init( clientKeyStore, passphrase.toCharArray() );

	    sslContext = SSLContext.getInstance( "TLS" );
	    sslContext.init( kmf.getKeyManagers(),
	                     tmf.getTrustManagers(),
	                     secureRandom );
	    return sslContext;
	}

	@Override
	public void run() {
		{
			String host;
			int port, filePort, KeyPort, FSDPort, VDPort, KeyFilePort,simmulPort;
			
			host = "localhost";
			port = 5500;
			simmulPort = 5550;
			filePort = 5600;
			FSDPort = 6050;
			VDPort = 6100;
			KeyPort = 5800;
			KeyFilePort = 5900;
			Socket keyExchangeSocket;
			Socket keyFileExchangeSocket;
			
			//Key exchange parts
			PrintStream keyOutToServer;
			BufferedReader keyInFromServer;
			DataOutputStream keydos;
			DataInputStream keydis;
			BufferedReader keyPathUser;

			try {
					keyExchangeSocket = new Socket(host, KeyPort);
					keyFileExchangeSocket = new Socket(host, KeyFilePort);
					
					keyOutToServer = new PrintStream( keyExchangeSocket.getOutputStream(), true); 
					keyInFromServer = new BufferedReader(new InputStreamReader( keyExchangeSocket.getInputStream()));
					
					keydos = new DataOutputStream(keyFileExchangeSocket.getOutputStream());
					keydis = new DataInputStream(keyFileExchangeSocket.getInputStream());
					
					File [] filesInKeystoreDir = new File(clientKeyPath).listFiles();
					if (filesInKeystoreDir == null) {
						System.out.println("You have no public/private keystore pair! Generate one and come back again! ");
					}
					else {
						/*
						for(File file : filesInKeystoreDir) {
							if(file.getName().equals("server.public")) {
								//flag = true;
								String serverResponse = "";
								keyOutToServer.println("C." + publicKeyServerDir);
								serverResponse = keyInFromServer.readLine();
								if(serverResponse.equals("ok")) {
									System.out.println("The server has found your public key in file: " + publicKeyServerDir );
									System.out.println("Initiating ssl connection to the server. ");
								}
								else {
									System.out.println("Error, the directory name is not valid!");
								}
								/*
								else {
									String passName = "";
									System.out.println("The server has NOT found your public key in file: " + publicKeyServerDir );
									System.out.println("Please type the path of your public keystore in your system. " );
									
									File clientPublic = new File(clientSelection);
									
									System.out.println("Please input a directory name, where your public key will be saved on the server. ");
									System.out.println("You can use the same directory name to help the server recognise your public key in it's system. ");
									passName = keyPathUser.readLine();
									publicKeyServerDir = passName;
									
									//K.filesize,dirname
									try {
										transportFile(clientPublic, keyOutToServer, keydos, passName);
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
							}
						}
						*/
						if(panel == 2) {
							String serverResponse = "";
							keyOutToServer.println("C." + publicKeyServerDir);
							serverResponse = keyInFromServer.readLine();
							if(serverResponse.equals("ok")) {
								System.out.println("The server has found your public key in file: " + publicKeyServerDir );
								System.out.println("Initiating ssl connection to the server. ");
							}
							else {
								System.out.println("Error, the directory name is not valid!");
							}
						}
						else if(panel == 1) {
							String serverResponse = "";
							//String clientKeystorePath = "";
							//String passName = "";
							/*
							System.out.println("Please input the path of your public keystore! ");
							clientKeystorePath = pubKeyPath;
							File clientPublic = new File(clientKeystorePath);
							
							System.out.println("Please input a directory name, where your public key will be saved on the server. ");
							System.out.println("You can use the same directory name to help the server recognise your public key in it's system. ");
							passName = keyPathUser.readLine();
							publicKeyServerDir = passName;
							*/
							File clientPublic = new File(pubKeyPath);

							try {
								transportFile(clientPublic, keyOutToServer, keydos, publicKeyServerDir); //last var changed
							} catch (IOException e) {
								e.printStackTrace();
							}
							
							serverResponse = keyInFromServer.readLine();
							if(serverResponse.charAt(0) == 'K') {
								String tidyServerMessage = serverResponse.substring(2, serverResponse.length());
								String serverPublicPath = clientKeyPath + File.separator + "server.public";
								File serverPublic = new File(serverPublicPath);
								receiveFile(tidyServerMessage, serverPublicPath,keydis);
								System.out.println("Server public key written! ");
							}				
							else {
								System.out.println("Error exchanging keys!");
							}
						}
					}
			}
			catch (UnknownHostException e) {
				System.out.println("Can not locate host/port " + host + "/" + KeyPort + "/" + KeyFilePort);
				return;
	        } catch (IOException e) {
				System.out.println("Could not establish connection to: " + host + "/" + KeyPort + "/" + KeyFilePort);
				return;
	        }
			
			System.out.println("---Receiving priority from server.--- ");
			try {
				priorityCounter = Integer.parseInt(keyInFromServer.readLine());
				keyOutToServer.close();
				keyInFromServer.close();
				keydos.close();
				keydis.close();
				keyExchangeSocket.close();
				keyFileExchangeSocket.close();				
			} catch (NumberFormatException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			/* Our socket end MAKE WITH SSL*/
			System.out.println("Started generating secure random and getting the keystores. ");
			
			SecureRandom secureRandom = new SecureRandom();
			secureRandom.nextInt();
			
			KeyStore clientKeyStore = null;
			KeyStore serverKeyStore = null;
			SSLContext sslContext = null;
			SSLSocketFactory sf = null;
			
		    try {
		    	clientKeyStore = setupClientKeyStore(clientKeyPath,passphrase,clientKeyStore);
		    	serverKeyStore = setupServerKeyStore(clientKeyPath,serverKeyStore);
		        sslContext = setupSSLContext(serverKeyStore, clientKeyStore, passphrase, sslContext, secureRandom);
		        sf = sslContext.getSocketFactory();
		      } catch( GeneralSecurityException gse ) {
		        gse.printStackTrace();
		      } catch( IOException ie ) {
		        ie.printStackTrace();
		      }
		    
			Socket clientSocket = null;
			Socket clientFileSocket = null;
			Socket clientSimmulSocket = null;
			Socket clientFSDSocket = null;
			Socket clientVDSocket = null;
			
			//Create 2 SSL sockets
			try {
				clientSocket = sf.createSocket(host,port + priorityCounter);
				clientFileSocket = sf.createSocket(host,filePort + priorityCounter);
				clientSimmulSocket = sf.createSocket(host,simmulPort + priorityCounter);
				clientFSDSocket = sf.createSocket(host,FSDPort + priorityCounter);
				clientVDSocket = sf.createSocket(host, VDPort + priorityCounter);				
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			             
			//Input stream for files
			DataInputStream dis;
			//Output stream for files
			DataOutputStream dos;
			
			//2 more for open files daemon
			PrintStream outToSimmul;
			BufferedReader inFromSimmul;  
			
			//more for DAEMONS
			PrintStream FSDoutToServer;
			PrintStream VDoutToServer;
			BufferedReader FSDinFromServer;
			BufferedReader VDinFromServer;
			
			String inviteUsername = null, serverOutput = null, publicKeyFolderName = null;
			
	        int portnum = port + priorityCounter;
			System.out.println("-- Client connecting to host:port " + host + ":" + portnum + " --");
				/* Connect to the server at the specified host/port */
	            try {
					/* Create a writing buffer to the socket */
					outToServer = new PrintStream( clientSocket.getOutputStream(), true); 
					/* Create a reading buffer to the socket */
					inFromServer = new BufferedReader(new InputStreamReader( clientSocket.getInputStream()) );
					//file input/same for files & daemon sockets
					dis = new DataInputStream (clientFileSocket.getInputStream());
					//file output
					dos = new DataOutputStream (clientFileSocket.getOutputStream());
					
					outToSimmul = new PrintStream( clientSimmulSocket.getOutputStream(), true); 
					inFromSimmul = new BufferedReader(new InputStreamReader( clientSimmulSocket.getInputStream()) );
						
					FSDoutToServer = new PrintStream( clientFSDSocket.getOutputStream(),true);
					FSDinFromServer = new BufferedReader(new InputStreamReader(clientFSDSocket.getInputStream()));
					
					VDoutToServer = new PrintStream(clientVDSocket.getOutputStream(),true);
					VDinFromServer = new BufferedReader(new InputStreamReader(clientVDSocket.getInputStream()));
	            
	            } catch (UnknownHostException e) {
					System.out.println("Can not locate host/port " + host + "/" + port);
					return;
	            } catch (IOException e) {
					System.out.println("Could not establish connection to: " + host + "/" + port);
					return;
	            }
	            try
	            {
					outToServer.println("7." + username + "," + publicKeyServerDir);	
					serverOutput = inFromServer.readLine(); 
					if(serverOutput.equals("ok")) {
						System.out.println("You are accepted in Xariot! ");	
						//Versioning / download daemon
						VersioningDaemon v1 = new VersioningDaemon(VDoutToServer,VDinFromServer,dis,dos,clientPath,username);
					    Platform.runLater(() -> {
						    v1.run();
					    });
	    				File [] clientDirs = new File[0];
						clientDirs = new File (clientPath).listFiles();

						for(File file : clientDirs) {
							if (file.isDirectory()) {
								Timer scannerTimer = new Timer(true);
							    Platform.runLater(() -> {
							    	try {
										scannerTimer.schedule(new FileScannerDaemon(username, conflictPath, file.getPath(), FSDoutToServer, FSDinFromServer, outToSimmul, inFromSimmul, dos, dis), 0, 4000);
									} catch (Exception e) {
										e.printStackTrace();
									}
							    });
							}
						}
						Timer versioningDaemon = new Timer(true);
					    Platform.runLater(() -> {
					    	versioningDaemon.schedule(new VersioningDaemon(VDoutToServer,VDinFromServer,dis,dos,clientPath,username), 6000, 8000);	
					    });
					}
					else {
						System.out.println("Error! You are not accepted by the server, your username is not valid. ");
						System.out.println("Close the application and try again! ");			
					}
			
	    			/*
	            	System.out.println("Exiting ");
	            	scanner.close();
	                // Close all of our connections.
	                outToServer.close();
	                inFromServer.close();
	                clientSocket.close();
	                VDoutToServer.close();
	                VDinFromServer.close();
	                FSDinFromServer.close();
	                FSDoutToServer.close();
	                outToSimmul.close();
	                inFromSimmul.close();
	                dos.close();
	                dis.close();
	                */	
	            } catch (Exception e) {
					System.out.println("I/O to socket failed: " + host);
	            }
		}
	}
} 

