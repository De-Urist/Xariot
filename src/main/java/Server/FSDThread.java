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

import Objects.ChecksumSHA1;
import Objects.FileObject;

public class FSDThread implements Runnable{
	PrintStream pr;
	BufferedReader br;
	String clientSentence = "";
	Connection conn;
	private final SServer server;
	String connUsername = "";
	DataOutputStream DataFileOs = null;
	DataInputStream DataFileIs = null;
	
	public FSDThread(PrintStream pr, BufferedReader br, Connection conn, SServer server, DataOutputStream DataFileOs, DataInputStream DataFileIs, String name ) {
		this.br = br;
		this.pr = pr;
		this.conn = conn;
		this.server = server;
		this.DataFileIs = DataFileIs;
		this.DataFileOs = DataFileOs;
		this.connUsername = name;
	}
	
	@Override
	public void run() {
		while(true) {
			try {			
				clientSentence = "";
				clientSentence = br.readLine();
			}
			catch (IOException e) {
				//here
				System.out.println("Client broke connection.");
				break;
			}
			if (clientSentence.charAt(0) == 'S') {
				System.out.println("Got S updating a file. ");
				String tidyClientMessage = clientSentence.substring(2, clientSentence.length());
    			ArrayList<String> clientSentenceArrayList = new ArrayList<>();
    			Collections.addAll(clientSentenceArrayList, tidyClientMessage.split("\\s*,\\s*"));
    			String FileDirAndName = "";
    			if(clientSentenceArrayList.get(1).contains("CONFLICT")) {
    				FileDirAndName = server.getConflictPath() + File.separator + clientSentenceArrayList.get(0);
    			}
    			else {
        			FileDirAndName = server.getServerPath() + File.separator + clientSentenceArrayList.get(1) + File.separator + clientSentenceArrayList.get(0);
    			}
    			//result set for rights read/write
    			try {
					receiveFile(clientSentenceArrayList.get(2),FileDirAndName,DataFileIs);
					File f1 = new File(FileDirAndName);
					if(f1.exists()) {
						FileObject fo1 = new FileObject(f1);
						String hash = fo1.getHash();
						long date = fo1.getLastModified();
						
						java.sql.PreparedStatement updateFiles = conn.prepareStatement("update Files set Hash = ? , Date = ? where Filename = ?");
						updateFiles.setString(1, hash);
						updateFiles.setBigDecimal(2, new BigDecimal(date));
						updateFiles.setString(3, clientSentenceArrayList.get(0));
						updateFiles.executeUpdate();	
						
						java.sql.PreparedStatement updateHistory = conn.prepareStatement("insert into History (Username,Filename,ModDate) values (?,?,?)");
						updateHistory.setString(1, connUsername);
						updateHistory.setString(2, clientSentenceArrayList.get(0));
						updateHistory.setBigDecimal(3, new BigDecimal(date));
						updateHistory.executeUpdate();
					}
				} catch ( Exception e) {
					e.printStackTrace();
				}
			}
			else if(clientSentence.charAt(0) == 'C') {
				//C.filename,hash
				System.out.println("Got C resolving conflict. ");
				String tidyClientMessage = clientSentence.substring(2, clientSentence.length());
    			ArrayList<String> clientSentenceArrayList = new ArrayList<>();
    			Collections.addAll(clientSentenceArrayList, tidyClientMessage.split("\\s*,\\s*"));
				
				try {	
					java.sql.PreparedStatement selectHash = conn.prepareStatement("select Hash from Files where Filename = ?");
					selectHash.setString(1, clientSentenceArrayList.get(0));
					ResultSet rs1 = selectHash.executeQuery();
					rs1.next();
					if( rs1.getString(1).equals(clientSentenceArrayList.get(1))) {
						pr.println("ok2");
					}
					else {
						try {
							java.sql.PreparedStatement selectSimul = conn.prepareStatement("select SimulUsers from Files where Filename = ?");
							selectSimul.setString(1, clientSentenceArrayList.get(0));
							ResultSet rs = selectSimul.executeQuery();
							rs.next();
							if(rs.getInt(1) == 1) {
								pr.println("ok");
							}
							else {
		                    	File conflictDir = new File(server.getConflictPath());
								File confFile = new File(server.getConflictPath() + File.separator + "CONFLICT" + clientSentenceArrayList.get(0));
		                    	boolean conflictingFileExists = new File(conflictDir,confFile.getName()).exists();
								if(conflictingFileExists) { //give conflict file to user
									transportFile(confFile, server.getConflictPath(), pr, DataFileOs);
									System.out.println("TESTING. Sent conflicting file to client.");
								}
								else {
									pr.println("c."); //receive first conflicting file
									String clientResponse = br.readLine();
		                			ArrayList<String> clientSentenceArrayList3 = new ArrayList<>();
		                			Collections.addAll(clientSentenceArrayList3, clientResponse.split("\\s*,\\s*"));
		                			try {
		                				receiveFile(clientSentenceArrayList3.get(2),confFile.getPath(),DataFileIs);             				
		                			} catch (IOException e) {
		                				e.printStackTrace();
		                			}
								}
							}
						} catch (SQLException | IOException e) {
							e.printStackTrace();
						}	
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			else if(clientSentence.charAt(0) == 'T') {
				System.out.println("Got T sending newest conflicting file. Or accepting an updated conflicting file. ");
				String tidyClientMessage = clientSentence.substring(2, clientSentence.length());
    			ArrayList<String> clientSentenceArrayList = new ArrayList<>();
    			Collections.addAll(clientSentenceArrayList, tidyClientMessage.split("\\s*,\\s*"));
    			String conflictDirPath = server.getConflictPath();
    			File conflictDir = new File(conflictDirPath);
    			File conflictingFile = new File(conflictDirPath + File.separator + "CONFLICT" + clientSentenceArrayList.get(0));
            	boolean conflictingFileExists = new File(conflictDir,conflictingFile.getName()).exists();
            	if(conflictingFileExists) {
            		String hash = "";
					try {
						hash = ChecksumSHA1.getSHA1Checksum(conflictingFile.getAbsolutePath());
					} catch (Exception e1) {
						e1.printStackTrace();
					}
            		if(hash.equals(clientSentenceArrayList.get(1))) {
            			pr.println("ok");
            			try {
            				String clientResponse = br.readLine();
            				clientResponse = clientResponse.substring(2,clientResponse.length());
            				//filename,dirname,size
            				ArrayList<String> clientSentenceArrayList2 = new ArrayList<>();
            				Collections.addAll(clientSentenceArrayList2, clientResponse.split("\\s*,\\s*"));
            				String confFileDirAndName = server.getConflictPath() + File.separator + clientSentenceArrayList2.get(0);
            				
							receiveFile(clientSentenceArrayList2.get(2),confFileDirAndName,DataFileIs);
						} catch ( Exception e) {
							e.printStackTrace();
						}
            		}
            		else {
						try {
							transportFile(conflictingFile, conflictDir.getName(), pr, DataFileOs);
						} catch (IOException e) {
							e.printStackTrace();
						}
            		}
            	}
            	else {
            		
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
