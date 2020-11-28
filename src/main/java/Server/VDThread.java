package Server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;

import Objects.FileObject;

public class VDThread implements Runnable{
	PrintStream pr;
	BufferedReader br;
	String clientSentence = "";
	Connection conn;
	private final SServer server;
	DataOutputStream DataFileOs = null;
	DataInputStream DataFileIs = null;
	File serverDir;
	
	public VDThread(PrintStream pr, BufferedReader br, Connection conn, SServer server, DataOutputStream DataFileOs, DataInputStream DataFileIs, File serverDir) {
		this.br = br;
		this.pr = pr;
		this.conn = conn;
		this.server = server;
		this.DataFileIs = DataFileIs;
		this.DataFileOs = DataFileOs;
		this.serverDir = serverDir;
	}
	
	@Override
	public void run() {
		while(true) {
			try {			
				clientSentence = "";
				clientSentence = br.readLine();
			}
			catch (IOException e) {
				//happens because of no threads on master?
				System.out.println("Client broke connection.");
				break;
			}
			if (clientSentence.charAt(0) == 'V') {
				System.out.println("Got V, deciding whether to send some files or all files");
				String thisUser = "";
				String tidyClientMessage = clientSentence.substring(2, clientSentence.length());
				
				if(tidyClientMessage.charAt(0) == 'A') {
					System.out.println("Got A, sending all files");
					String tidyClientMessage2 = tidyClientMessage.substring(2, tidyClientMessage.length());
        			ArrayList<String> clientSentenceArrayList = new ArrayList<>();
        			Collections.addAll(clientSentenceArrayList, tidyClientMessage2.split("\\s*,\\s*"));
        			/*
					java.sql.PreparedStatement selectFolder;
					try {
						selectFolder = conn.prepareStatement("select Username from Users where PublicKeyPath = ?");
						selectFolder.setString(1, server.getServerKeyPath() + File.separator + clientSentenceArrayList.get(0) + File.separator + "client.public");
						ResultSet rsUser = selectFolder.executeQuery();
						if(rsUser.next()) {
							thisUser = rsUser.getString(1);
						}					
					} catch (SQLException e1) {
						e1.printStackTrace();
					}
					*/
					try {
						java.sql.PreparedStatement selectAllAccepted = conn.prepareStatement("select Foldername from FolderInvites where Username = ? and HasAccepted = 1");
						selectAllAccepted.setString(1, clientSentenceArrayList.get(0));
						ResultSet rsUF = selectAllAccepted.executeQuery();
						if(rsUF.next()) {
							 do{
								String dirName = rsUF.getString(1);
								String pathOfDir = server.getServerPath() + File.separator + dirName;
								File [] filesOfDir = new File (pathOfDir).listFiles();
								String localClientResponse = "next";
								for(File file : filesOfDir) {
									try {
										if(localClientResponse.equals("next")) {
											transportFile(file, dirName, pr, DataFileOs);
										}
										localClientResponse = br.readLine();
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
								System.out.println("Sent all files in this directory.");
							} while(rsUF.next());
							pr.println("done");
							System.out.println("Sent all directories and their content.");									
						}
						else {
							pr.println("no");
						}
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
				else if(tidyClientMessage.charAt(0) == 'O'){
					System.out.println("Got O, Check every hash that is being sent with the db and mark them so to send them back afterwards.");
					String tidyClientMessage2 = tidyClientMessage.substring(2, tidyClientMessage.length());
					java.sql.PreparedStatement selectFolder;
					try {
						selectFolder = conn.prepareStatement("select Username from Users where PublicKeyPath = ?");
						selectFolder.setString(1, server.getServerKeyPath() + File.separator + tidyClientMessage2 + File.separator + "client.public");
						ResultSet rsUser = selectFolder.executeQuery();
						if(rsUser.next()) {
							thisUser = rsUser.getString(1);
						}						
					} catch (SQLException e1) {
						e1.printStackTrace();
					}
					String clientMsg = "";
					String thisHash = "";
					try {
						clientMsg = br.readLine();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					ArrayList <String> filesClientHas = new ArrayList<>();
					String currentDir = "";
					while(clientMsg.charAt(0) == 'F') {
						String tidyClientMessage3 = clientMsg.substring(2, clientMsg.length());
		        		ArrayList<String> clientSentenceArrayList = new ArrayList<>();
		        		Collections.addAll(clientSentenceArrayList, tidyClientMessage3.split("\\s*,\\s*"));
		        		
		        		if(clientSentenceArrayList.size() < 2) {
			       			filesClientHas.add(clientSentenceArrayList.get(0));
		        		}
		        		else {
			        		currentDir = clientSentenceArrayList.get(1);
			        		//check if the actual dir exists
			        		File [] filesOfServer = serverDir.listFiles();
			        		boolean flag = false;
			        		for(File dir : filesOfServer) {
			        			if(dir.getName().equals(clientSentenceArrayList.get(1))) {
			        				flag = true;
			        				break;
			        			}
			        		}			        			
				       		if(flag) {
								java.sql.PreparedStatement selectFile;
								try {
									selectFile = conn.prepareStatement("select Hash from Files where Filename = ?");
									selectFile.setString(1, clientSentenceArrayList.get(0));
									ResultSet rsHash = selectFile.executeQuery();
									if(rsHash.next()) {
										thisHash = rsHash.getString(1);
										if(thisHash.equals(clientSentenceArrayList.get(2))) {
							       			pr.println("G.");
							       			filesClientHas.add(clientSentenceArrayList.get(0));
										}
										else {
						        			pr.println("U.");
						        			filesClientHas.add(clientSentenceArrayList.get(0));
						        			File updatedFile = new File(server.getServerPath() + File.separator + currentDir + File.separator + clientSentenceArrayList.get(0));
						        			transportFile(updatedFile, currentDir, pr, DataFileOs);
										}
									}
									else {
						       			pr.println("N.");
						        		//new file from client
						        		clientMsg = br.readLine();											
						        		tidyClientMessage = clientMsg.substring(2, clientMsg.length());
					        			ArrayList<String> clientSentenceArrayListNewF = new ArrayList<>();
					        			Collections.addAll(clientSentenceArrayListNewF, tidyClientMessage.split("\\s*,\\s*"));
					        			String fileDirAndName = server.getServerPath() + File.separator + clientSentenceArrayListNewF.get(1) + File.separator + clientSentenceArrayListNewF.get(0);
					        			receiveFile(clientSentenceArrayListNewF.get(2),fileDirAndName,DataFileIs);
					        			//DATABASE QUERIES (OH LORD)
					        			File f1 = new File(fileDirAndName);
										FileObject fo1 = new FileObject(f1);
										String hash = fo1.getHash();
										long date = fo1.getLastModified();
										
										java.sql.PreparedStatement FileCreation = conn.prepareStatement("insert into Files (Filename,Hash,Date,Foldername) values (?, ?, ?, ?)");											
										FileCreation.setString(1, clientSentenceArrayListNewF.get(0));
										FileCreation.setString(2, hash);
										FileCreation.setBigDecimal(3, new BigDecimal(date));
										FileCreation.setString(4, clientSentenceArrayListNewF.get(1));
										FileCreation.executeUpdate();
										FileCreation.close();
									}
								} catch (Exception e) {
									e.printStackTrace();
								}		        			
				        	}
				        	else {
				        		pr.println("Z.");
				        	}		        			
		        		}
		        		//Finally try to get another input (if it is D. break loop)
						try {
							clientMsg = br.readLine();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					String thisDirPath = server.getServerPath() + File.separator + currentDir;
					File thisDir = new File(thisDirPath);
	        		File [] filesOfThisDir = thisDir.listFiles();
	        		for(File file : filesOfThisDir) {
	        			if(filesClientHas.contains(file.getName())) {
	        				System.out.println("Client has file: " + file.getName());
	        			}
	        			else {
	        				try {
	        					
								transportFile(file,currentDir,pr,DataFileOs);
							} catch (IOException e) {
								e.printStackTrace();
							}
	        			}
	        		}
	        		pr.println(".");
				}
			}
			else {
				System.out.println("Other message...");
			}
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
