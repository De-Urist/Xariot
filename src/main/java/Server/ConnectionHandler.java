package Server;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException; 
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import Objects.ChecksumSHA1;
import Objects.FileObject;
import Objects.SendMail;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

public class ConnectionHandler implements Runnable
{
	// Socket for our endpoint
	protected Socket echoSocket;
	protected Socket fileSocket;
	protected Socket commulSocket;
	protected Socket FSDSocket;
	protected Socket VDSocket;
	private final SServer server;
	String serverURL="jdbc:derby://localhost:5700/SSDatabase;create=true"; 
	Connection conn = null;
	File serverDir = null;
	String email = "";
	String emailPass = "";
	String connUsername = "";
	ArrayList<String> openFiles = new ArrayList<>();
	
	public ConnectionHandler(SServer server, Socket aSocketToHandle, Socket aFileSocketToHandle, Socket aCommulToHandle, Socket aFSDSocketToHandle, Socket aVDSocketToHandle)
	{
		this.server = server;
		this.echoSocket = aSocketToHandle;
		this.fileSocket = aFileSocketToHandle;
		this.commulSocket = aCommulToHandle;
		this.FSDSocket = aFSDSocketToHandle;
		this.VDSocket = aVDSocketToHandle;
	}
	
	/**  * New thread for handling client interaction will start here.   */
    public void run()
        {
    		//SERVER DIRECTORY
    		serverDir = new File (server.getServerPath());
			// Holds messages we get from client
			String clientSentence = "";
			String simulSentence = "";
			// Input object
			BufferedReader inFromClient; 
		    // Output object
			PrintStream outToClient;
			// Client's name
			String peerName;            
			//output file
			DataOutputStream DataFileOs;
			//input file
			DataInputStream DataFileIs;
			// Simul streams object
			BufferedReader inFromSimmul; 
			PrintStream outToSimmul;
			
			//daemons br,pr
			BufferedReader inFromFSD; 
			PrintStream outToFSD;
			BufferedReader inFromVD; 
			PrintStream outToVD;
			
			//Create database connection
			java.util.Properties props = new java.util.Properties();
			try {
				Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
				conn = DriverManager.getConnection(serverURL, props);
				System.out.println("Client managed to open a connection to server database. ");
			} catch (SQLException | InstantiationException | IllegalAccessException | ClassNotFoundException e1) {
				System.out.println("Client did not manage to open a connection to server database. ");
				e1.printStackTrace();
			}
			
            // Attach a println/readLine interface to the socketso we can read and write strings to the socket.
            try {
                /* Get the IP address from the client */
                peerName = echoSocket.getInetAddress().getHostAddress();
				/* Create a writing stream to the socket */
				outToClient = new PrintStream( echoSocket.getOutputStream(), true );
				outToSimmul = new PrintStream( commulSocket.getOutputStream(), true);
				outToFSD = new PrintStream(FSDSocket.getOutputStream(),true);
				outToVD = new PrintStream(VDSocket.getOutputStream(),true);
				//Create output stream for file
				DataFileOs = new DataOutputStream(fileSocket.getOutputStream());
				//Create input stream for file
				DataFileIs = new DataInputStream(fileSocket.getInputStream());
				
				/* Create a reading stream to the socket */
				inFromClient = new BufferedReader( new InputStreamReader( echoSocket.getInputStream() ) ); 
				inFromSimmul = new BufferedReader( new InputStreamReader( commulSocket.getInputStream() ) );        
				inFromFSD = new BufferedReader( new InputStreamReader( FSDSocket.getInputStream() ) );
				inFromVD = new BufferedReader( new InputStreamReader( VDSocket.getInputStream() ) );
            }
            catch (IOException e) {
                System.out.println("Error creating buffered handles.");
                return;
            }
	        System.out.println("Beginning thread to handle how many users are editing a file.");
            OpenFilesThread t1 = new OpenFilesThread (outToSimmul,inFromSimmul,conn);
            new Thread(t1).start();
            
            FSDThread fsd1 = new FSDThread(outToFSD,inFromFSD,conn,server,DataFileOs,DataFileIs,connUsername);
            new Thread(fsd1).start();
            
            VDThread vd1 = new VDThread(outToVD,inFromVD,conn,server,DataFileOs,DataFileIs,serverDir);
            new Thread(vd1).start();
           
            System.out.println("Handling connection to client at " + peerName + " --");
            while ( true )
            {
				try {
					clientSentence = "";
					clientSentence = inFromClient.readLine();
				}
				catch (IOException e) {
					System.out.println( echoSocket.getInetAddress() + "-" + peerName + " broke the connection." );
					//do some queries here for removing counters from db 
					break;
				}
				
				/* Output to screen the message received by the client */
				System.out.println( "Message Received: " + clientSentence );
				if (clientSentence.charAt(0) == 'A') {
					boolean flag = false;
					System.out.println("Got A. Creating tables.");
					try {
						conn.createStatement().execute("create table Folders (Foldername varchar(30), primary key (Foldername))");
						conn.createStatement().execute("create table Files(Filename varchar(30) not null, Hash varchar(60), Date bigint not null, Foldername varchar(30), SimulUsers integer, primary key (Filename), foreign key (Foldername) references Folders(Foldername))");
						conn.createStatement().execute("create table Users(Username varchar(30) not null, PublicKeyPath varchar(100), primary key (Username))");
						conn.createStatement().execute("create table FolderInvites (ConnectionId integer not null GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), Foldername varchar(30), Username varchar(30), LocalRight integer not null, HasAccepted integer not null, primary key(ConnectionId), foreign key (Foldername) references Folders(Foldername), foreign key (Username) references Users(Username))");
						conn.createStatement().execute("create table Comments(Username varchar(30) not null, Comment varchar(300), Foldername varchar(30) not null, foreign key (Username) references Users (Username), foreign key (Foldername) references Folders (Foldername))");
						conn.createStatement().execute("create table History(Username varchar(30) not null, Filename varchar(30) not null, ModDate bigint not null, foreign key (Username) references Users (Username), foreign key (Filename) references Files (Filename))");

						flag = true;
					} catch (SQLException e) {
						e.printStackTrace();
					}
					if(flag) {
						outToClient.println("ok");
					}
					else {
						outToClient.println("no");
					}
				}
				else if (clientSentence.charAt(0) == '1') {
					System.out.println("Got 1. Creating folder from master client's message. ");
					String tidyClientMessage = clientSentence.substring(2, clientSentence.length());
					boolean flag = false;
					String FileDirAndName = server.getServerPath() + File.separator + tidyClientMessage;
					
					File [] filesOfDir = new File (server.getServerPath()).listFiles();
					if (filesOfDir == null) {
						try {
							java.sql.PreparedStatement FileCreation = conn.prepareStatement("insert into Folders (Foldername) values (?)");
							FileCreation.setString(1, tidyClientMessage);
							FileCreation.execute();
							File dir = new File(FileDirAndName);
							dir.mkdir();
							flag = true;						
						} catch (SQLException e) {
							e.printStackTrace();
						}
						if (flag) {
							outToClient.println("ok");
						}
						else {
							outToClient.println("no");
						}							
					}
					else {
						for(File file : filesOfDir) {
							if(file.getName().equals(tidyClientMessage)) {
								outToClient.println("no");
								flag = true;
							}
						}
						if(!flag) {
							try {
								java.sql.PreparedStatement FileCreation = conn.prepareStatement("insert into Folders (Foldername) values (?)");
								FileCreation.setString(1, tidyClientMessage);
								FileCreation.execute();
								File dir = new File(FileDirAndName);
								dir.mkdir();
								flag = true;						
							} catch (SQLException e) {
								e.printStackTrace();
							}
							if (flag) {
								outToClient.println("ok");
							}
							else {
								outToClient.println("no");
							}		
						}
					}
				}
				else if (clientSentence.charAt(0) == 'U') {
					System.out.println("Got U. Waiting for a file transfer from the client...");
					String tidyClientMessage = clientSentence.substring(2, clientSentence.length()); //filename,dirname
        			
					ArrayList<String> clientSentenceArrayList = new ArrayList<>();
        			Collections.addAll(clientSentenceArrayList, tidyClientMessage.split("\\s*,\\s*"));
					String FileDirAndName = server.getServerPath() + File.separator + clientSentenceArrayList.get(1) + File.separator + clientSentenceArrayList.get(0);
	
					boolean flag = false;
					try {
						File f1 = new File(FileDirAndName);
						File [] filesOfDir = new File (server.getServerPath() + File.separator + clientSentenceArrayList.get(1) + File.separator).listFiles();
						for(File file : filesOfDir) {
							if(file.getName().equals(clientSentenceArrayList.get(0))) {
								flag = true;
								System.out.println("File already exists! Try changing this file via the daemon! ");
							}
						}
						if(!flag) {
							//f1.createNewFile();
							receiveFile(clientSentenceArrayList.get(2), FileDirAndName, DataFileIs);

							FileObject fo1 = new FileObject(f1);
						
							//write file object in db
							String hash = fo1.getHash();
							long date = fo1.getLastModified();

							java.sql.PreparedStatement FileCreation = conn.prepareStatement("insert into Files (Filename,Hash,Date,Foldername,SimulUsers) values (?, ?, ?, ?, ?)");
							FileCreation.setString(1, clientSentenceArrayList.get(0));
							FileCreation.setString(2, hash);
							FileCreation.setBigDecimal(3, new BigDecimal(date));
							FileCreation.setString(4, clientSentenceArrayList.get(1));
							FileCreation.setInt(5, 0);
							FileCreation.executeUpdate();
							FileCreation.close();
							
							flag = true;								
						}
					} catch (Exception e) {
						e.printStackTrace();
						System.out.println("Error creating a server file. ");
					}
					if(flag) {
						outToClient.println("ok");
					}
					else {
						outToClient.println("no");
					}			
				}
				else if (clientSentence.charAt(0) == 'R') {
					System.out.println("Got 1 changing user rights. ");
					String tidyClientMessage = clientSentence.substring(2, clientSentence.length());
        			ArrayList<String> clientSentenceArrayList = new ArrayList<>();
        			Collections.addAll(clientSentenceArrayList, tidyClientMessage.split("\\s*,\\s*"));
        			
        			java.sql.PreparedStatement userExists;
					try {
						userExists = conn.prepareStatement("select Username from FolderInvites where Username = ? and Foldername = ?");
						userExists.setString(1, clientSentenceArrayList.get(2));
						userExists.setString(2, clientSentenceArrayList.get(0));
						ResultSet rs = userExists.executeQuery();	
						if(rs.next()) {
		        			java.sql.PreparedStatement changeRight = conn.prepareStatement("update FolderInvites set LocalRight = ? where Username = ? and Foldername = ?");
		        			changeRight.setInt(1, Integer.parseInt(clientSentenceArrayList.get(1)));
		        			changeRight.setString(2, clientSentenceArrayList.get(2));
		        			changeRight.setString(3, clientSentenceArrayList.get(0));
		        			changeRight.execute();
		        			outToClient.println("ok");
						}
						else {
							outToClient.println("NoUser");
						}
						
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
				else if (clientSentence.charAt(0) == '2') {
					System.out.println("Got 2 creating invites. ");
					String tidyClientMessage = clientSentence.substring(2, clientSentence.length());
        			ArrayList<String> clientSentenceArrayList = new ArrayList<>();
        			Collections.addAll(clientSentenceArrayList, tidyClientMessage.split("\\s*,\\s*"));
        			email = clientSentenceArrayList.get(0);
        			emailPass = clientSentenceArrayList.get(1);
					boolean flag = false;
        				try {
    						java.sql.PreparedStatement selectUsername = conn.prepareStatement("select Username from Users where Username = ?");
    						selectUsername.setString(1, clientSentenceArrayList.get(3));
    						ResultSet rs = selectUsername.executeQuery();
    						if(rs.next()) {
    							//user is already in the system
    						}
    						else {
	 							java.sql.PreparedStatement UserCreation= conn.prepareStatement("insert into Users (Username,PublicKeyPath) values (?, ?)");
								UserCreation.setString(1, clientSentenceArrayList.get(3));
								UserCreation.setString(2, null);   		
								UserCreation.execute();
    						}
							java.sql.PreparedStatement InviteCreation= conn.prepareStatement("insert into FolderInvites (Foldername, Username, LocalRight, HasAccepted) values (?, ?, ?, ?)");
							InviteCreation.setString(1,clientSentenceArrayList.get(2));
							InviteCreation.setString(2,clientSentenceArrayList.get(3));
							InviteCreation.setInt(3, Integer.parseInt(clientSentenceArrayList.get(4)));
							InviteCreation.setInt(4, 0);
							InviteCreation.execute();
							flag = true;
							
							PreparedStatement selectId = conn.prepareStatement("select ConnectionId from FolderInvites where Foldername = ? and Username = ?");
							selectId.setString(1, clientSentenceArrayList.get(2));
							selectId.setString(2,clientSentenceArrayList.get(3));
							ResultSet rsHash = selectId.executeQuery();
							String thisId = "";
							if(rsHash.next()) {
								thisId = String.valueOf(rsHash.getInt(1));
							}
							
							SendMail s1 = new SendMail(email,emailPass,clientSentenceArrayList.get(5),clientSentenceArrayList.get(2),clientSentenceArrayList.get(3), clientSentenceArrayList.get(4),thisId);
							s1.run();
							
						} catch (Exception e) {
							e.printStackTrace();
						}
					if(flag) {
						outToClient.println("ok");
					}
					else {
						outToClient.println("no");
					}
				}
				
				//TESTING DATABASES
				else if (clientSentence.charAt(0) == '4') {
					System.out.println("Got 4. Show table. " + System.getProperty("line.separator"));
					
					final String SQL_STATEMENT = "select * from Files";
						Statement statement;
						try {
							statement = conn.createStatement();
							ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);
							ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
							int columnCount = resultSetMetaData.getColumnCount();
							for(int i = 1; i <= columnCount; i++) {
								System.out.format("%20s", resultSetMetaData.getColumnName(i)+ " | ");
							}
							while (resultSet.next()) {
								System.out.println("");
								for(int i = 1; i <= columnCount; i++) {
									System.out.format("%20s", resultSet.getString(i)+ " | ");
								}
							}	
							if (statement != null) statement.close();
						} catch (SQLException e) {
							e.printStackTrace();
						}
					outToClient.println("ok");
				}
				
				//TESTING DATABASES
				else if (clientSentence.charAt(0) == '5') {
					System.out.println("Got 5. Show table. " + System.getProperty("line.separator"));
					
					final String SQL_STATEMENT = "select * from FolderInvites";
						Statement statement;
						try {
							statement = conn.createStatement();
							ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);
							ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
							int columnCount = resultSetMetaData.getColumnCount();
							for(int i = 1; i <= columnCount; i++) {
								System.out.format("%20s", resultSetMetaData.getColumnName(i)+ " | ");
							}
							while (resultSet.next()) {
								System.out.println("");
								for(int i = 1; i <= columnCount; i++) {
									System.out.format("%20s", resultSet.getString(i)+ " | ");
								}
							}	
							if (statement != null) statement.close();
						} catch (SQLException e) {
							e.printStackTrace();
						}
					outToClient.println("ok");
				}
				//TESTING DATABASES
				else if (clientSentence.charAt(0) == '6') {
					System.out.println("Got 6. Show table. " + System.getProperty("line.separator"));
					
					final String SQL_STATEMENT = "select * from Users";
						Statement statement;
						try {
							statement = conn.createStatement();
							ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);
							ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
							int columnCount = resultSetMetaData.getColumnCount();
							for(int i = 1; i <= columnCount; i++) {
								System.out.format("%20s", resultSetMetaData.getColumnName(i)+ " | ");
							}
							while (resultSet.next()) {
								System.out.println("");
								for(int i = 1; i <= columnCount; i++) {
									System.out.format("%20s", resultSet.getString(i)+ " | ");
								}
							}	
							if (statement != null) statement.close();
						} catch (SQLException e) {
							e.printStackTrace();
						}
					outToClient.println("ok");
				}
				
				else if (clientSentence.charAt(0) == '7') {
					System.out.println("Got 7. Mapping keypath to username and saving username attribute. ");
					String tidyClientMessage = clientSentence.substring(2, clientSentence.length());
        			ArrayList<String> clientSentenceArrayList = new ArrayList<>();
        			Collections.addAll(clientSentenceArrayList, tidyClientMessage.split("\\s*,\\s*"));
        			
        			connUsername = clientSentenceArrayList.get(0);
        			String publicKeyPath = server.getServerKeyPath() + File.separator + clientSentenceArrayList.get(1) + File.separator + "client.public";
        			try {
	        			java.sql.PreparedStatement userExists = conn.prepareStatement("select Username from Users where Username = ?");
						userExists.setString(1, clientSentenceArrayList.get(0));
						ResultSet rs = userExists.executeQuery();
	        			if(rs.next()) {
		        			java.sql.PreparedStatement connectPublicPathUser = conn.prepareStatement("update Users set PublicKeyPath = ? where Username = ?");
		        			connectPublicPathUser.setString(1, publicKeyPath);
		        			connectPublicPathUser.setString(2, connUsername);
		        			connectPublicPathUser.execute();
	        				outToClient.println("ok");
	        			}	
	        			else {
	        				outToClient.println("no");
	        			}
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
				else if (clientSentence.charAt(0) == '8') {
					System.out.println("Got 8. Adding a new comment for a file. ");
					String tidyClientMessage = clientSentence.substring(2, clientSentence.length());
        			ArrayList<String> clientSentenceArrayList = new ArrayList<>();
        			Collections.addAll(clientSentenceArrayList, tidyClientMessage.split("\\s*,\\s*"));
					java.sql.PreparedStatement belongs;
					try {
						belongs = conn.prepareStatement("select Username from FolderInvites where Username = ? and HasAccepted = 1 and Foldername = ?");
						belongs.setString(1, clientSentenceArrayList.get(2));
						belongs.setString(2, clientSentenceArrayList.get(0));
						ResultSet rs = belongs.executeQuery();	
						if(rs.next()) {
							java.sql.PreparedStatement CommentInsert = conn.prepareStatement("insert into Comments (Username,Comment,Foldername) values (?,?,?)");
							CommentInsert.setString(1, clientSentenceArrayList.get(2));
							CommentInsert.setString(2, clientSentenceArrayList.get(1));
							CommentInsert.setString(3, clientSentenceArrayList.get(0));
							CommentInsert.execute();
							outToClient.println("ok");
						}
						else {
							outToClient.println("noaccess");
						}
					} catch (SQLException e) {
						outToClient.println("notexist");
						e.printStackTrace();
					}
				}
				else if(clientSentence.charAt(0) == '9') {
					System.out.println("Got 9. Returning all comments for a project to a user. ");
					String tidyClientMessage = clientSentence.substring(2, clientSentence.length());
        			ArrayList<String> clientSentenceArrayList = new ArrayList<>();
        			Collections.addAll(clientSentenceArrayList, tidyClientMessage.split("\\s*,\\s*"));
        			java.sql.PreparedStatement belongs;
					try {
						belongs = conn.prepareStatement("select Username from FolderInvites where Username = ? and HasAccepted = 1 and Foldername = ?");
						belongs.setString(1, clientSentenceArrayList.get(1));
						belongs.setString(2, clientSentenceArrayList.get(0));

						ResultSet rs = belongs.executeQuery();
						if(rs.next()) {
		        			java.sql.PreparedStatement commentQuery;
		        			commentQuery = conn.prepareStatement("select Username, Comment from Comments where Foldername = ?");
		        			commentQuery.setString(1, clientSentenceArrayList.get(0));	
							ResultSet rs1 = commentQuery.executeQuery();
							if(rs1.next()) {
								String allComments = "";
								outToClient.println("ok");
								allComments = rs1.getString(1) + "," + rs1.getString(2);
								while(rs1.next()) {
									allComments = allComments + "," + rs1.getString(1) + "," + rs1.getString(2);
								}
								outToClient.println(allComments);
							}
							else {
								outToClient.println("nocomments");
							}
						}
						else {
							outToClient.println("noaccess");
						}
					} catch (SQLException e) {
						e.printStackTrace();
					}
	
				}

				else if (clientSentence.charAt(0) == '0') {
					System.out.println( "Closing connection with " + echoSocket.getInetAddress() + "." );
					outToClient.flush();					
					break;					
				}	
				else {
					System.out.println("Null test.");
				}
            }

            System.out.println("Closing " + peerName + " connection");

            // Close all the handles
            try {
				inFromClient.close();
				outToClient.close();
				
				DataFileOs.close();
				DataFileIs.close();
				
				echoSocket.close();
				fileSocket.close();
            }
            catch (IOException e) {
            }
	}
    public void transportFile(File file, String dirName, PrintStream out, DataOutputStream dos) throws IOException {
		String fileName = file.getName();		
		long fileSize = file.length();
	    out.println("F." + fileName + "," + dirName + "," + fileSize);
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


