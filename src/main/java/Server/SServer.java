package Server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.derby.drda.NetworkServerControl;

public class SServer extends Thread {
	
    //Replace the 4 following String variables.
    private String serverPath = "C:\\Users\\user\\Desktop\\Xariot server directory";
    private String masterKeystorePath = "C:\\Users\\user\\Desktop\\Master's key directory";	
    private String serverKeystorePath = "C:\\Users\\user\\Desktop\\serverKeys";
    private String conflictPath = "C:\\Users\\user\\Desktop\\Server conflicting dir";
    private int serverPort;
    private int serverFilePort;
    private int databasePort;
    private int keyExchangePort;
    private int keyFileExchangePort;
    private boolean keysExchanged = false;
    private static boolean dbStarted = false;

    private static int connectionCounter = 0;
    private int port,filePort,dbPort,keyPort,keyFilePort,serverSimulPort, ServerFSDPort, ServerVDPort;

	private ArrayList<ConnectionHandler> handlerList = new ArrayList<>();
	private ArrayList<keyExchangeHandler> keyHandlerList = new ArrayList<>();

	//database values
	static final String JDBC_URL = "jdbc:derby://localhost:5700//SSDatabase;create=true";
	
	public String getServerPath() {
		return this.serverPath;
	}
	
	public String getServerKeyPath() {
		return this.serverKeystorePath;
	}
	
	public String getMasterKeyPath() {
		return this.masterKeystorePath;
	}
	
	public String getConflictPath() {
		return this.conflictPath;
	}
	
	public SServer (int port, int simulPort, int filePort, int dbPort, int FSDPort, int VDPort, int keyPort, int keyFilePort) {
		serverPort = port;
		serverFilePort = filePort;
		databasePort = dbPort;
		keyExchangePort = keyPort;
		keyFileExchangePort = keyFilePort;
		serverSimulPort = simulPort;
		ServerFSDPort = FSDPort;
		ServerVDPort = VDPort;		
	}
	
	public ArrayList<ConnectionHandler> getHandlerList(){
		return handlerList;
	}
	
	public ArrayList<keyExchangeHandler> getkeyHnadlerList(){
		return keyHandlerList;
	}
	
	public void run() {
		//Socket for key exchange
		Socket keyExchangeSocket = null;
		Socket keyFileExchangeSocket = null;
		//Server key listening socket
		ServerSocket keyWelcomeSocket = null;
		ServerSocket keyFileWelcomeSocket = null;		
		
		while(true) {
			if(keyWelcomeSocket == null && keyFileWelcomeSocket == null) {
				try {
					keyWelcomeSocket = new ServerSocket(keyExchangePort);
					keyFileWelcomeSocket = new ServerSocket(keyFileExchangePort);
				}
		        catch (IOException e) {
					e.printStackTrace();
		            System.out.println("Could not use key exchange server port " + keyExchangePort + " and " + keyFileExchangePort);
		            return;
		        }				
			}
			else {
	            System.out.println("Sockets are already listening for public keys! ");
			}
	
	        System.out.println("-------------------------------- Server listening on key exchange sockets " + keyExchangePort + " and " + keyFileExchangePort + " --------------------------------");
	        try {
	        	keyExchangeSocket = keyWelcomeSocket.accept();
	            keyFileExchangeSocket = keyFileWelcomeSocket.accept();
	        }
	        catch (IOException e) {
	           System.out.println("Error accepting connection.");
	            //continue;
	        }
	        //path of the new public key from client
	        String newFileName = handlePublicKeyExchange(keyExchangeSocket,keyFileExchangeSocket,connectionCounter);
			System.out.println(newFileName);
			
	        
	        //Create the Database server
			if(!dbStarted) {
				NetworkServerControl server;
				try {
					server = new NetworkServerControl(InetAddress.getByName("localhost"),databasePort);
		            server.start(null);		
		            System.out.println("Database server started! ");
		            dbStarted = true;
				} catch (Exception e1) {
					System.out.print("Could not start database on server! ");
					e1.printStackTrace();
					dbStarted = false;
				}					
			}
			SecureRandom secureRandom = new SecureRandom();
			secureRandom.nextInt();
			
			///STH HERE
			KeyStore clientKeyStore = null;
			KeyStore serverKeyStore = null;
			SSLContext sslContext = null;
			SSLServerSocketFactory sf = null;
			String passphrase = "qwerty";
			
		    try {
		    	clientKeyStore = setupClientKeyStore(newFileName,clientKeyStore);
		    	serverKeyStore = setupServerKeyStore(serverKeystorePath,passphrase,serverKeyStore);
		        sslContext = setupSSLContext(serverKeyStore, clientKeyStore, passphrase, sslContext, secureRandom);
		        sf = sslContext.getServerSocketFactory();
				
		    } catch( GeneralSecurityException gse ) {
		        gse.printStackTrace();
		    } catch( IOException ie ) {
		        ie.printStackTrace();
		    }
		    
		    SSLServerSocket serverSocket = null;
		    SSLServerSocket serverFileSocket = null;
		    SSLServerSocket serverSimulSocket = null;
		    SSLServerSocket serverFSDSocket = null;
		    SSLServerSocket serverVDSocket = null;
		    
			try {
				serverSocket = (SSLServerSocket)sf.createServerSocket( serverPort + connectionCounter );
				//serverSocket.setNeedClientAuth( false );
			    serverFileSocket = (SSLServerSocket)sf.createServerSocket( serverFilePort + connectionCounter);
			    //serverFileSocket.setNeedClientAuth( false );
			    serverSimulSocket = (SSLServerSocket)sf.createServerSocket( serverSimulPort + connectionCounter);

			    serverFSDSocket = (SSLServerSocket)sf.createServerSocket(ServerFSDPort + connectionCounter);
			    serverVDSocket = (SSLServerSocket)sf.createServerSocket(ServerVDPort + connectionCounter);
			} catch (IOException e2) {
				e2.printStackTrace();
			}
			
			/* Socket for connections made and files */
			SSLSocket connectionSocket = null;
			SSLSocket connectionFileSocket = null;
			SSLSocket simulSocket = null;
			SSLSocket FSDSocket = null;
			SSLSocket VDSocket = null;

			// Listen forever for new connections.  When a new connection
	        // is made a new socket is created to handle it.

		    System.out.println("-------------------------------- Server listening on SSL sockets " + serverPort + " " + serverSimulPort + " " + serverFilePort + " " + databasePort + " --------------------------------");
		    /* Try and accept the connection first and then continue */
		    try {
		        connectionSocket = (SSLSocket) serverSocket.accept();
		        connectionFileSocket = (SSLSocket) serverFileSocket.accept();
		        simulSocket = (SSLSocket) serverSimulSocket.accept();
		        FSDSocket = (SSLSocket) serverFSDSocket.accept();
		        VDSocket = (SSLSocket) serverVDSocket.accept();
		        
		    }
		    catch (IOException e) {
		        System.out.println("Error accepting connection.");
		        continue;
		    }
			System.out.println("<-- Made connection on server socket and file socket -->");
			handleClient( connectionSocket, connectionFileSocket, simulSocket, FSDSocket, VDSocket );
			connectionCounter++;
		}
	}

	public void handleClient(Socket clientConnectionSocket, Socket connectionFileSocket, Socket simulSocket, Socket FSDSocket, Socket VDSocket )
    {
		System.out.println("<-- Starting thread to handle connection -->");
		ConnectionHandler myHandler = new ConnectionHandler (this , clientConnectionSocket, connectionFileSocket, simulSocket, FSDSocket, VDSocket );
		handlerList.add(myHandler);
		new Thread(myHandler).start();
	}
	
	public String handlePublicKeyExchange(Socket unsafeSocket, Socket unsafeFileSocket, int counter) {
		System.out.println("<-- Starting thread to exchange public keys -->");
		keyExchangeHandler myKeyHandler = new keyExchangeHandler (this , unsafeSocket, unsafeFileSocket, counter);
		keyHandlerList.add(myKeyHandler);
		new Thread(myKeyHandler).run();
		return myKeyHandler.getNewDirPath();
	}
	
	private static KeyStore setupClientKeyStore(String keystorePath, KeyStore serverKeyStore) throws GeneralSecurityException, IOException {
		serverKeyStore = KeyStore.getInstance( "JKS" );
	    serverKeyStore.load( new FileInputStream( keystorePath ), 
	                        "public".toCharArray() );
	    return serverKeyStore;
	}
	
	private static KeyStore setupServerKeyStore(String keystorePath, String passphrase, KeyStore clientKeyStore) throws GeneralSecurityException, IOException {
		clientKeyStore = KeyStore.getInstance( "JKS" );
	    clientKeyStore.load( new FileInputStream( keystorePath + File.separator + "server.private" ),
	                       passphrase.toCharArray() );
	    return clientKeyStore;
	}
	
	private static SSLContext setupSSLContext(KeyStore serverKeyStore, KeyStore clientKeyStore, String passphrase, SSLContext sslContext, SecureRandom secureRandom) throws GeneralSecurityException, IOException {
	    TrustManagerFactory tmf = TrustManagerFactory.getInstance( "SunX509" );
	    tmf.init( clientKeyStore );

	    KeyManagerFactory kmf = KeyManagerFactory.getInstance( "SunX509" );
	    kmf.init( serverKeyStore, passphrase.toCharArray() );

	    sslContext = SSLContext.getInstance( "TLS" );
	    sslContext.init( kmf.getKeyManagers(),
	                     tmf.getTrustManagers(),
	                     secureRandom );
	    return sslContext;
	}
}
