package MasterClient;

import java.io.*; 
import java.net.*;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Scanner;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument; 

public class MasterClient implements Runnable
{
	// Replace the following String with the path of the server's 
	// key directory
    String serverKeyPath = "C:\\Users\\user\\Desktop\\serverKeys";
    private static int priorityCounter = 0;
	BufferedReader masterPrivInput;
	String password;
	PrintStream outToServer, FSDoutToServer, VDoutToServer, outToSim;
	BufferedReader inFromServer, FSDinFromServer, VDinFromServer, inFromSim;  
	DataOutputStream dos;
	DataInputStream dis;
	
	public MasterClient(String password) {
		this.password = password;
	}
	
	public PrintStream getOut() {
		return outToServer;
	}
	
	public BufferedReader getIn() {
		return inFromServer;
	}
	public DataOutputStream getDos() {
		return dos;
	}
	
	public DataInputStream getDis (){
		return dis;
	}
	
	public void connect(String host, int port, int simmulPort, int filePort, int FSDPort, int VDPort, int KeyPort, int KeyFilePort) throws Exception
	{
		
		Socket keyExchangeSocket;
		Socket keyFileExchangeSocket;
		
		//Key exchange parts
		PrintStream keyOutToServer;
		BufferedReader keyInFromServer;

		keyExchangeSocket = new Socket(host, KeyPort);
		keyFileExchangeSocket = new Socket(host, KeyFilePort);
		
		keyOutToServer = new PrintStream( keyExchangeSocket.getOutputStream(), true); 
		keyInFromServer = new BufferedReader(new InputStreamReader( keyExchangeSocket.getInputStream()));

		File [] filesInKeystoreDir = new File(serverKeyPath).listFiles();
		
		if(filesInKeystoreDir == null) {
			System.out.println("Your key directory is empty! You need a public/private keypair combo. ");
			return;
		}
		else {
			keyOutToServer.println("M.");
		}
		
		System.out.println("Receiving priority from server. ");
		priorityCounter = Integer.parseInt(keyInFromServer.readLine());
		
		keyExchangeSocket.close();
		keyFileExchangeSocket.close();
		
		/* Our socket end VIA SSL HERE */
		System.out.println("Started generating secure random and getting the keystores. ");
		masterPrivInput = new BufferedReader( new InputStreamReader( System.in ) ); 

		SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextInt();
		
		KeyStore clientKeyStore = null;
		KeyStore serverKeyStore = null;
		SSLContext sslContext = null;
		SSLSocketFactory sf = null;
		
		String passphrase = "";
		System.out.println("Please input the password of your private key (master1)(qwerty)");
		System.out.println("Changed, no more need for the master keystore.");

		passphrase = masterPrivInput.readLine();
		
	    try {
	    	clientKeyStore = setupClientKeyStore(serverKeyPath,passphrase,clientKeyStore);
	    	serverKeyStore = setupServerKeyStore(serverKeyPath,serverKeyStore);
	        sslContext = setupSSLContext(serverKeyStore, clientKeyStore, passphrase, sslContext, secureRandom);
	        sf = sslContext.getSocketFactory();
	    } catch( GeneralSecurityException gse ) {
	        gse.printStackTrace();
	    } catch( IOException ie ) {
	        ie.printStackTrace();
	    }
	    
		Socket clientSocket = null;
		Socket clientFileSocket = null;
		Socket clientSimSocket = null;
		Socket clientFSDSocket = null;
		Socket clientVDSocket = null;
      
		/* For reading from user */
		BufferedReader inFromUser;       
		
		/* For writing to socket */
		PrintStream outToSim;
		// For reading from socket */
		BufferedReader inFromSim;    
		
		//Input stream for files
		DataInputStream dis;
		//Output stream for files
		DataOutputStream dos;
		
		String sentence = null, sentence2 = null, serverOutput = null;
        String selection = "";

		String fileName = "";
		long fileSize = 0;
		int portint = port + priorityCounter ;
		System.out.println("-- Client connecting to host/port " + host + "/" + portint + " --");

			/* Connect to the server at the specified host/port */
            try {
        		clientSocket = sf.createSocket(host,port + priorityCounter);
            	/* Create a buffer to hold the user's input */
				inFromUser = new BufferedReader( new InputStreamReader( System.in ) ); 
				/* Create a writing buffer to the socket */
				outToServer = new PrintStream( clientSocket.getOutputStream(), true); 
				/* Create a reading buffer to the socket */
				inFromServer = new BufferedReader(new InputStreamReader( clientSocket.getInputStream()) );
				
				clientSimSocket = sf.createSocket(host, simmulPort + priorityCounter);
				outToSim = new PrintStream( clientSimSocket.getOutputStream(), true); 
				inFromSim = new BufferedReader(new InputStreamReader( clientSimSocket.getInputStream()) );
				
				//same for files
				clientFileSocket = sf.createSocket(host,filePort + priorityCounter);
				//file input
				dis = new DataInputStream (clientFileSocket.getInputStream());
				//file output
				dos = new DataOutputStream (clientFileSocket.getOutputStream());
				
				clientFSDSocket = sf.createSocket(host,FSDPort + priorityCounter);
				clientVDSocket = sf.createSocket(host, VDPort + priorityCounter);
				PrintStream FSDoutToServer;
				PrintStream VDoutToServer;
				BufferedReader FSDinFromServer;
				BufferedReader VDinFromServer;
				
				FSDoutToServer = new PrintStream( clientFSDSocket.getOutputStream(),true);
				FSDinFromServer = new BufferedReader(new InputStreamReader(clientFSDSocket.getInputStream()));
				VDoutToServer = new PrintStream(clientVDSocket.getOutputStream(),true);
				VDinFromServer = new BufferedReader(new InputStreamReader(clientVDSocket.getInputStream()));
				
            } catch (UnknownHostException e) {
				System.out.println("Can not locate host/port " + host + "/" + port + " Priority: " + priorityCounter);
				return;
            } catch (IOException e) {
            	int x = port + priorityCounter;
				System.out.println("Could not establish connection to: " + host + "/" + x);
				return;
            }
 
            System.out.println("Master connection established ");

            String myHostAddress = clientSocket.getInetAddress().getHostAddress();

            //add a thread for sending opened files ect... and make it via outToSim, inFromSim and the 2 daemons for VD/FSD
            try
            {
	           Scanner scanner = new Scanner(System.in);
	           	do 
	           	{

					System.out.println( "Welcome to SocketSync Master User ,your address is: " + myHostAddress);
					System.out.println("------Main Menu------");
					System.out.println("A.Create database server (Click once to create tables)");
					System.out.println("1.Create a synchronization folder ");
					System.out.println("U.Upload a file to a synchronization folder ");
					System.out.println("2.Invite users to a folder project / Configure e-mail access ");
					System.out.println("4.See database Files (testing) ");
					System.out.println("5.See database FolderInvites (testing) ");
					System.out.println("6.See database Users (testing) ");
					
					System.out.println("0.Exit");
					
					System.out.println("Navigate by using numbers 1-9, A-Z");
					selection = scanner.nextLine();

	        		switch (selection)
	        		{
	        			case "1":
	    					System.out.println("Please input the name of the folder you want to create. ");
							
	    					sentence = inFromUser.readLine();
	    					outToServer.println("1." + sentence);
							serverOutput = inFromServer.readLine(); 
							
							if(serverOutput.equals("ok")) {
								System.out.println("Success creating a folder. ");
							}
							else{
								System.out.println("Failure creating folder. ");
								System.out.println("This folder name already exists, or there is some other problem! ");
							}
							break;
	        			case "U":
	        				System.out.println("Please input the path of the file you want to upload. ");
	        				System.out.println("Example: C\\user\\file\\example.docx");

							sentence = inFromUser.readLine();	

							File userFile = new File(sentence);
							if (userFile.exists()) {
								fileName = userFile.getName();		
								fileSize = userFile.length();
							}
							else {
								System.out.println("Invalid file chosen! ");
								break;
							}
							
	        				System.out.println("Please input the folder name, where this file will be uploaded to. ");
							sentence2 = inFromUser.readLine();

							//Send upload packet with file name        
							outToServer.println("U." + fileName + "," + sentence2 + "," + fileSize);	
							System.out.println("Uploading file in Xariot server dir... ");

							File file = new File(sentence);
							byte[] b = new byte[(int) file.length()];
						    try (FileInputStream fileInputStream = new FileInputStream(sentence)) {
					            fileInputStream.read(b);
						    }
						    dos.write(b);
							System.out.println("Sent a byte to server.");
						    dos.flush();
						   
							System.out.println("Waiting for server response. ");
							serverOutput = inFromServer.readLine(); 
							if(serverOutput.equals("ok")) {
								System.out.println("File " + fileName + " uploaded successfully to: " + sentence2);
							}
							else {
								System.out.println("Error uploading file! ");
							}
							
	        				break;
	        			case "A":
	    					System.out.println("Creating database tables... ");
							outToServer.println("A.");	
							serverOutput = inFromServer.readLine(); 
							if(serverOutput.equals("ok")) {
								System.out.println("Tables created! ");
							}
							else if(serverOutput.equals("already")) {
								System.out.println("Unable to create tables! Database is created already! ");
							}
							else {
								System.out.println("Unable to create tables! Some other error exists! ");
							}

							break;
	        			case "2": 
							System.out.println("Invite users by generating a username they will input on the server to join. " );
							System.out.println("Example: Foldername1,User1,1,user1@gmail.com ");
							System.out.println("Your E-mail, Application password, Folderame, Name, RightNumber, E-mail ");
							
							sentence = inFromUser.readLine();	  
							outToServer.println("2." + sentence );
							serverOutput = inFromServer.readLine(); 

							if(serverOutput.equals("ok")) {
								System.out.println("Invite recorded and an email is sent! ");
							}
							else {
								System.out.println("Error creating an invite! ");
							}
							break;
	        			case"4":
	        				System.out.println("Return some table. ");
							outToServer.println("4.");
							serverOutput = inFromServer.readLine(); 
							System.out.println(serverOutput); 
							break;
							
	        			case"5":
	        				System.out.println("Return some table. ");
							outToServer.println("5.");
							serverOutput = inFromServer.readLine(); 
							System.out.println(serverOutput); 
							break;
							
	        			case"6":
	        				System.out.println("Return some table. ");
							outToServer.println("6.");
							serverOutput = inFromServer.readLine(); 
							System.out.println(serverOutput); 
							break;
	        		}
	        		//selection ="";
            	}
	           	while (selection != "0");
            	System.out.println("Exiting ");
            	outToServer.println("0.");
            	scanner.close();
                // Close all of our connections.
                outToServer.close();
                inFromServer.close();
                clientSocket.close();
                
            } catch (IOException e) {
				System.out.println("I/O to socket failed: " + host);
            }
	}  /* End Connect Method */
	
	
	public void transportFile(File file, PrintStream out, DataOutputStream dos, String dirName) throws IOException {
		long fileSize = file.length();
	    out.println("K." + fileSize + "," + dirName);
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

	public static void main( String[] argv ) throws Exception
	{
		String server, pass;
		int port, filePort, keyPort, FSDPort, VDPort, keyFilePort,simmulPort;
		
		server = "localhost";
		port = 5500;
		simmulPort = 5550;
		filePort = 5600;
		FSDPort = 6050;
		VDPort = 6100;
		keyPort = 5800;
		keyFilePort = 5900;
		
		pass = "qwerty"; //MAKE A SCANNER HERE FOR NON GUI IMPLEMENTATION OF PASSWORD
		
		MasterClient myclient = new MasterClient(pass);
		myclient.connect(server, port, simmulPort, filePort, FSDPort , VDPort, keyPort, keyFilePort);
		System.out.println("<-- Client has exited -->");
     } 
	
	private static KeyStore setupServerKeyStore(String keystorePath, KeyStore serverKeyStore) throws GeneralSecurityException, IOException {
		serverKeyStore = KeyStore.getInstance( "JKS" );
	    serverKeyStore.load( new FileInputStream( keystorePath + File.separator + "server.public" ), 
	                        "public".toCharArray() );
	    return serverKeyStore;
	}
	private static KeyStore setupClientKeyStore(String keystorePath, String passphrase, KeyStore clientKeyStore) throws GeneralSecurityException, IOException {
		clientKeyStore = KeyStore.getInstance( "JKS" );
	    clientKeyStore.load( new FileInputStream( keystorePath + File.separator + "server.private" ),
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
		String host;
		int port, filePort, keyPort, FSDPort, VDPort, keyFilePort,simmulPort;
		
		host = "localhost";
		port = 5500;
		simmulPort = 5550;
		filePort = 5600;
		FSDPort = 6050;
		VDPort = 6100;
		keyPort = 5800;
		keyFilePort = 5900;
				
		Socket keyExchangeSocket;
		Socket keyFileExchangeSocket;
		
		//Key exchange parts
		PrintStream keyOutToServer;
		BufferedReader keyInFromServer;

		try {
			keyExchangeSocket = new Socket(host, keyPort);
			keyFileExchangeSocket = new Socket(host, keyFilePort);
			
			keyOutToServer = new PrintStream( keyExchangeSocket.getOutputStream(), true); 
			keyInFromServer = new BufferedReader(new InputStreamReader( keyExchangeSocket.getInputStream()));
	
			File [] filesInKeystoreDir = new File(serverKeyPath).listFiles();
			
			if(filesInKeystoreDir == null) {
				System.out.println("Your key directory is empty! You need a public/private keypair combo. ");
				return;
			}
			else {
				keyOutToServer.println("M.");
			}
			
			//System.out.println("Receiving priority from server. ");
			priorityCounter = Integer.parseInt(keyInFromServer.readLine());
			
			keyExchangeSocket.close();
			keyFileExchangeSocket.close();			
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		/* Our socket end VIA SSL HERE */
		//System.out.println("Started generating secure random and getting the keystores. ");
		masterPrivInput = new BufferedReader( new InputStreamReader( System.in ) ); 

		SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextInt();
		
		KeyStore clientKeyStore = null;
		KeyStore serverKeyStore = null;
		SSLContext sslContext = null;
		SSLSocketFactory sf = null;
		
		String passphrase = password;

	    try {
	    	clientKeyStore = setupClientKeyStore(serverKeyPath,passphrase,clientKeyStore);
	    	serverKeyStore = setupServerKeyStore(serverKeyPath,serverKeyStore);
	        sslContext = setupSSLContext(serverKeyStore, clientKeyStore, passphrase, sslContext, secureRandom);
	        sf = sslContext.getSocketFactory();
	    } catch( GeneralSecurityException gse ) {
	        gse.printStackTrace();
	    } catch( IOException ie ) {
	        ie.printStackTrace();
	    }
	    
		Socket clientSocket = null;
		Socket clientFileSocket = null;
		Socket clientSimSocket = null;
		Socket clientFSDSocket = null;
		Socket clientVDSocket = null;          

			/* Connect to the server at the specified host/port */
            try {
        		clientSocket = sf.createSocket(host,port + priorityCounter);
				/* Create a writing buffer to the socket */
				outToServer = new PrintStream( clientSocket.getOutputStream(), true); 
				/* Create a reading buffer to the socket */
				inFromServer = new BufferedReader(new InputStreamReader( clientSocket.getInputStream()) );
				
				clientSimSocket = sf.createSocket(host, simmulPort + priorityCounter);
				outToSim = new PrintStream( clientSimSocket.getOutputStream(), true); 
				inFromSim = new BufferedReader(new InputStreamReader( clientSimSocket.getInputStream()) );
				
				//same for files
				clientFileSocket = sf.createSocket(host,filePort + priorityCounter);
				//file input
				dis = new DataInputStream (clientFileSocket.getInputStream());
				//file output
				dos = new DataOutputStream (clientFileSocket.getOutputStream());
				
				clientFSDSocket = sf.createSocket(host,FSDPort + priorityCounter);
				clientVDSocket = sf.createSocket(host, VDPort + priorityCounter);
				
				FSDoutToServer = new PrintStream( clientFSDSocket.getOutputStream(),true);
				FSDinFromServer = new BufferedReader(new InputStreamReader(clientFSDSocket.getInputStream()));
				VDoutToServer = new PrintStream(clientVDSocket.getOutputStream(),true);
				VDinFromServer = new BufferedReader(new InputStreamReader(clientVDSocket.getInputStream()));
				
            } catch (UnknownHostException e) {
				//System.out.println("Can not locate host/port " + host + "/" + port + " Priority: " + priorityCounter);
				return;
            } catch (IOException e) {
				return;
            }
 
            System.out.println("Master connection established ");

            String myHostAddress = clientSocket.getInetAddress().getHostAddress();

            //add a thread for sending opened files ect... and make it via outToSim, inFromSim and the 2 daemons for VD/FSD
            	
	}
} 

