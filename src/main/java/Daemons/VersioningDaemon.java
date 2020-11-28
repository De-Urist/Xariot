package Daemons;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.TimerTask;

import org.apache.commons.io.FileUtils;

import Objects.ChecksumSHA1;

public class VersioningDaemon extends TimerTask{
	String clientPath = "";
	PrintStream pr;
	BufferedReader br;
	DataInputStream dis;
	DataOutputStream dos;
	String tidyServerMessage = "";
	String username = "";	
	File clientDir;


	public VersioningDaemon(PrintStream pr, BufferedReader br, DataInputStream dis, DataOutputStream dos, String clientPath, String username) {
		this.pr = pr;
		this.br = br;
		this.dis = dis;
		this.dos = dos;
		this.clientPath = clientPath;
		this.username = username;
	}
	
	public void run() {
		//System.out.println("Daemon running... ");	
		String response = "";
		File clientDir = new File(clientPath);

		File [] filesOfDir = clientDir.listFiles();
		if(filesOfDir.length == 0) {
			System.out.println("Request everything from server. ");
			pr.println("V.A." + username);
			try {
				response = br.readLine();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			while(!response.equals("done") ) {
				if(response.equals("no")) {
					break;
				}
				if(response.charAt(0) == 'F') {
					tidyServerMessage = response.substring(2, response.length());
        			ArrayList<String> serverSentenceArrayList = new ArrayList<>();
        			Collections.addAll(serverSentenceArrayList, tidyServerMessage.split("\\s*,\\s*"));
        			String FileDir = clientPath + File.separator + serverSentenceArrayList.get(1);
        			String FileDirAndName = FileDir + File.separator + serverSentenceArrayList.get(0);
        			File dir = new File(FileDir);
        			File file = new File (FileDirAndName);
        			if(!dir.exists()) {
        				dir.mkdir();
        			}
					try {
						receiveFile(serverSentenceArrayList.get(2), FileDirAndName, dis);					
						pr.println("next");
						response = br.readLine();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}		
			}
			if(response.equals("no")) {
				System.out.println("Nothing to synchronize to you! Register to a project first! ");
			}
			else {
				System.out.println("You have downloaded every file and directory available to you! ");
			}
		}
		else {
			//Send each file one by one to the server
			for(File file : filesOfDir) {
			pr.println("V.O." + username);
				if(file.isDirectory()) {
					String dirName = file.getName();
					File [] filesOfThisDir = file.listFiles();
					for(File fileInDir : filesOfThisDir) {
						boolean isFileUnlocked = fileInDir.renameTo(fileInDir);
						if(fileInDir.getName().contains("~$") || fileInDir.getName().contains("~")) {
							//do nothing
							//System.out.println("Ignoring file: " + fileInDir.getName() + " as it is either a temp file or it is being edited by the user." );
						}
						else if(!isFileUnlocked){
							pr.println("F." + fileInDir.getName());
						}
						else {
							String hashOfFile = "";
							String fileName = fileInDir.getName();
							try {
								hashOfFile = ChecksumSHA1.getSHA1Checksum(fileInDir.getPath());
								pr.println("F." + fileName + "," + dirName + "," + hashOfFile);
								response = br.readLine();
								if(response.charAt(0) == 'G') {
									System.out.println("File: " + fileName + " is up to date.");
								}
								else if(response.charAt(0) == 'U') {
									System.out.println("File: " + fileName + " is being updated now.");
									response = br.readLine();
									tidyServerMessage = response.substring(2, response.length());
				        			ArrayList<String> serverSentenceArrayList = new ArrayList<>();
				        			Collections.addAll(serverSentenceArrayList, tidyServerMessage.split("\\s*,\\s*"));
				        			String FilePath = fileInDir.getPath();
				        			receiveFile(serverSentenceArrayList.get(2), FilePath, dis);
								}
								else if(response.charAt(0) == 'N') {
									System.out.println("File: " + fileName + " does not exist in the server. Uploading it to the server.");
									transportFile(fileInDir,dirName,pr,dos);
								}
								else if(response.charAt(0) == 'Z') {
									System.out.println("This directory: " + dirName + " does not exist in the server! Remove it manually from your system.");
									break;
								}
								else {
									System.out.println("Error ");
									System.out.println("TESTING ERROR: " + response );
									break;
								}
							} catch (Exception e) {
								e.printStackTrace();
							}							
						}
					}
					System.out.println("Done updating files in the directory: " + dirName + " .");

					pr.println("D.");
					while(true) {
						try {
							response = br.readLine();
							if(response.charAt(0) == 'F') {
								//start receiving files // F.filename,dirname,filesize
								tidyServerMessage = response.substring(2, response.length());
			        			ArrayList<String> serverSentenceArrayList = new ArrayList<>();
			        			Collections.addAll(serverSentenceArrayList, tidyServerMessage.split("\\s*,\\s*"));
			        			String FileDir = clientPath + File.separator + serverSentenceArrayList.get(1);
			        			String FileDirAndName = FileDir + File.separator + serverSentenceArrayList.get(0);
			        			File dir = new File(FileDir);
			        			File file2 = new File (FileDirAndName);
			        			if(!dir.exists()) {
			        				dir.mkdir();
			        			}
								try {
									receiveFile(serverSentenceArrayList.get(2), FileDirAndName, dis);
									System.out.println("New file received from the server and put in directory: " + dirName + " .");
								} catch (IOException e) {
									System.out.println("File: " + serverSentenceArrayList.get(0) + " is not written into the clientside as it is being edited by the user!");
								}												
							}	
							else {
								System.out.println("Done checking for new files in directory: " + dirName + " .");
								break;
							}
						} catch (IOException e) {
							e.printStackTrace();
						}		
					}
				}
				else {
					System.out.println(file + " is not a directory! ");
				}
			}
		}
	}
    public void receiveFile(String fileSizeString, String FileDirAndName, DataInputStream dis)
    		throws IOException {
		long fileSize = Long.parseLong(fileSizeString);
		byte []b=new byte[(int) fileSize];
		dis.read(b);
		System.out.println("Read " + fileSize + " bytes.");
	    try (FileOutputStream fos = new FileOutputStream(FileDirAndName,false)) {
	        fos.write(b);
	        fos.close();
	    }
    }
    
    public void transportFile(File file, String dirName, PrintStream out, DataOutputStream dos)
    		throws IOException {
		String fileName = file.getName();		
		long fileSize = file.length();
	    out.println("F." + fileName + "," + dirName + "," + fileSize);
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
}

