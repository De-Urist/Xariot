package Server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;

public class keyExchangeHandler implements Runnable{
	// Socket for our endpoint
	protected Socket keyExchangeSocket;
	protected Socket keyFileExchangeSocket;
	private final SServer server;
	private boolean isExchanged = false;
	private String newDirPath = "";
	private int Counter;
	
	public keyExchangeHandler(SServer server, Socket keyExchangeSocket, Socket keyFileExchangeSocket, int counter)
	{
		this.server = server;
		this.keyExchangeSocket = keyExchangeSocket;
		this.keyFileExchangeSocket = keyFileExchangeSocket;
		this.Counter = counter;
	}
	
	public String getNewDirPath() {
		return this.newDirPath;
	}

	@Override
	public void run() {
		String clientSentence = "";
		DataOutputStream dos;
		DataInputStream dis;
		
		BufferedReader inFromClient;
		PrintStream outToClient;
        try {
			dos = new DataOutputStream(keyFileExchangeSocket.getOutputStream());
			dis = new DataInputStream(keyFileExchangeSocket.getInputStream());
			
			outToClient = new PrintStream( keyExchangeSocket.getOutputStream(), true );
			inFromClient = new BufferedReader( new InputStreamReader( keyExchangeSocket.getInputStream() ) ); 
        }
        catch (IOException e) {
            System.out.println("Error creating buffered handlers.");
            return;
        }
		while(!isExchanged) {
			System.out.println("Getting the public key file from the user and creating it on the server's keystore directory. ");
			try {
				clientSentence = "";
				clientSentence = inFromClient.readLine();
			}
			catch (IOException e) {
				System.out.println( keyExchangeSocket.getInetAddress() + " broke the connection." );
				break;
			}
			if(clientSentence.charAt(0) == 'K') {
				try {
					//K.size,dirname
					String tidyClientMessage = clientSentence.substring(2, clientSentence.length());
					ArrayList<String> clientSentenceArrayList = new ArrayList<>();
        			Collections.addAll(clientSentenceArrayList, tidyClientMessage.split("\\s*,\\s*"));
        			File keyContainer = new File(server.getServerKeyPath() + File.separator + clientSentenceArrayList.get(1));
        			keyContainer.mkdir();
        			String keystorePath = keyContainer.getPath() + File.separator + "client.public";
					receiveFile(clientSentenceArrayList.get(0), keystorePath, dis);
					newDirPath = keyContainer.getPath() + File.separator + "client.public" ;	//return this
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.out.println("Wrote the client's public key on the server, sending server's public key. ");
				File serverKeyFile = new File(server.getServerKeyPath() + File.separator + "server.public");
				try {
					transportFile(serverKeyFile, outToClient, dos);
				} catch (IOException e) {
					e.printStackTrace();
				}
				isExchanged = true;
				System.out.println("Public key exchange complete! ");				
			}
			else if(clientSentence.charAt(0) == 'C'){
				String tidyClientMessage = clientSentence.substring(2, clientSentence.length());
				File [] filesInKeystoreDir = new File(server.getServerKeyPath()).listFiles();
				boolean flag = false;
				for(File file : filesInKeystoreDir) {
					if(file.getName().equals(tidyClientMessage)) {
						flag = true;
						newDirPath = file.getPath() + File.separator + "client.public";
						outToClient.println("ok");
						isExchanged = true;
					}
				}
				if(!flag) {
					outToClient.println("no");
					try {
						clientSentence = inFromClient.readLine();
						String tidyMessage = clientSentence.substring(2, clientSentence.length());
						ArrayList<String> clientSentenceArrayList = new ArrayList<>();
	        			Collections.addAll(clientSentenceArrayList, tidyClientMessage.split("\\s*,\\s*"));
	        			File keyContainer = new File(server.getServerKeyPath() + File.separator + clientSentenceArrayList.get(1));
	        			keyContainer.mkdir();
	        			String keystorePath = keyContainer.getPath() + File.separator + "client.public";
						receiveFile(clientSentenceArrayList.get(0), keystorePath, dis);
						newDirPath = keyContainer.getPath() + File.separator + "client.public" ;	//return this
						isExchanged = true;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			else if(clientSentence.charAt(0) == 'M') {
				newDirPath = server.getServerKeyPath() + File.separator + "server.public";
				isExchanged = true;
			}
			outToClient.println(Integer.toString(Counter));
		}
	}
	
	//changed	
    public void transportFile(File file, PrintStream out, DataOutputStream dos) throws IOException {
		String fileName = file.getName();		
		long fileSize = file.length();
	    out.println("K." + fileSize);
		byte b[] = new byte[(int) file.length()];
	    try (FileInputStream fileInputStream = new FileInputStream(file.getPath())) {
            fileInputStream.read(b);
            fileInputStream.close();
	    }
	    dos.write(b);
		System.out.println("Sent " + fileSize + " bytes to client.");
	    dos.flush();
		//DataFileOs.flush();
    }
    
    //changed
    public void receiveFile(String fileSizeString, String FileDirAndName, DataInputStream dis) throws IOException {
		long fileSize = Long.parseLong(fileSizeString);
		byte []b=new byte[(int) fileSize];
		dis.read(b);
		System.out.println("Read " + fileSize + " bytes.");

	    try (FileOutputStream fos = new FileOutputStream(FileDirAndName)) {
	        fos.write(b);
	        fos.close();
	    }
    }
}
