package Daemons;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;

import Objects.ChecksumSHA1;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class FileScannerDaemon extends TimerTask {
	boolean changed = false;
	String dirPath;
	PrintStream pr,simulpr;
	BufferedReader br,simulbr;
	DataOutputStream dos;
	DataInputStream dis;
	String conflictPath, username;
	ArrayList<String> FilenameHashMap;
	
	public FileScannerDaemon(String username, String conflictPath, String dirPath, PrintStream pr, BufferedReader br, PrintStream simulpr, BufferedReader simulbr, DataOutputStream dos, DataInputStream dis) throws Exception {
        this.dirPath= dirPath;
		this.pr = pr;
		this.br = br;
		this.simulbr = simulbr;
		this.simulpr = simulpr;
		this.dos = dos;
		this.conflictPath = conflictPath;
		this.username = username;
		this.dis = dis;
		this.FilenameHashMap = calculateHashes(new File(dirPath));
		for(int i=0; i<FilenameHashMap.size(); i++) {
			//System.out.println("HASH TEST: " + FilenameHashMap.get(i));
		}
	}
	
	public boolean isChanged() {
		return changed;
	}
	
	//calculate every old hash of each file in this dir
	public ArrayList<String> calculateHashes(File f1) throws Exception {
		File [] dirArr2 = f1.listFiles();
		ArrayList<String> FilenameHashMap = new ArrayList<String>();
		for(File file : dirArr2) {
			if(file.getName().contains("~$") || file.getName().contains("~")) {
				//do nothing
			}
			else {
				String hashOfFile = ChecksumSHA1.getSHA1Checksum(file.getPath());
				FilenameHashMap.add(file.getName());
				FilenameHashMap.add(hashOfFile);						
			}
		}	
		return FilenameHashMap;
	}
	
	@Override
	public void run() {
		File file1 = new File(dirPath);
		//instead of exists(), try isFile() possible bug
		if(file1.exists()) { 
			try {
	            Path dir = Paths.get(dirPath);
	            //increases decreases counter for simmultaneous users in server db
				FileFilter f1 = new FileFilter(username,dirPath,simulpr,simulbr);
				f1.run();
				
				File [] dirArr = file1.listFiles();
				for(File file : dirArr) {
					String filename = file.getName();
                	if(filename.contains("~$") || filename.contains("~")) {
                		//System.out.println("~$ file, or a temporary file was found named: " + filename);
                	}
                	else {
                        //if conflicting file exists here...
                    	File conflictDir = new File(conflictPath);
                    	File conflictingFile = new File(conflictPath + File.separator + "CONFLICT" + filename);
                    	boolean conflictingFileExists = new File(conflictDir,conflictingFile.getName()).exists();
                    	
        				String hashOfChangedFile = ChecksumSHA1.getSHA1Checksum(file.getPath());
        				boolean isOldFile = false;
        				for(int i=0; i<FilenameHashMap.size(); i=i+2) {
        					if(FilenameHashMap.get(i).equals(filename) && FilenameHashMap.get(i+1).equals(hashOfChangedFile)) {
        						isOldFile = true;
        						break;
        					}
        				}	
        				if(isOldFile) {
        				// do nothing
        					System.out.println("The file: " + filename + " has the same hash, the user performed no changes to it.");
        				}
        				else {
							boolean isFileUnlocked = file.renameTo(file);
        					if(!isFileUnlocked) {
        						//being edited
            					//System.out.println("CHANGED HASH TESTING: " + hashOfChangedFile);
    	        				for(int i=0; i<FilenameHashMap.size(); i=i+2) {
    	        					if(FilenameHashMap.get(i).equals(filename)) {		
    	        						FilenameHashMap.remove(i+1);
    	        						FilenameHashMap.remove(i);
    	        						FilenameHashMap.add(filename);	        						
    	        						FilenameHashMap.add(hashOfChangedFile);
    	        						//break;
    	        					}
    	        				}        				
    	        				if(conflictingFileExists) {
    	                    		//CHECK IF THIS CONFLICTING FILE IS THE LATEST
    	                    		System.out.println("There is a conflicting document in: " + conflictDir.getName() + ". Consider copying any changes from the conflicting file. ");
    	                    		System.out.println("Always save a file before deleting the conflicting document. If there is a newer version of the conflicting file, it will be downloaded here. ");
    	                    		System.out.println("Deleting the conflicting document means that you are performing a commit to the original document. If you are the only user with that file open you will not download a conflicting file. ");

    	                    		String hash = ChecksumSHA1.getSHA1Checksum(conflictingFile.getAbsolutePath());
    	                    		pr.println("T." + filename + "," + hash);
    	                    		
    	                    		String uploadResponse = br.readLine();
    	                    		if (uploadResponse.equals("ok")){
    	                    			//upload CONFLICTfile.txt
    		                    		FileUtils.copyFile(file, conflictingFile);
    	                    			unfilteredTransportFile(conflictingFile,conflictPath,pr,br,dos);
    	                    		}
    	                    		else{
    	                    			//download newest conflicting file 
    		                    		System.out.println("There is a newer version of the conflicting document. Downloaded in dir: " + conflictDir.getName() );
    		                    		uploadResponse = uploadResponse.substring(2,uploadResponse.length());
    				        			ArrayList<String> serverSentenceArrayList = new ArrayList<>();
    				        			Collections.addAll(serverSentenceArrayList, uploadResponse.split("\\s*,\\s*"));
    				        			receiveFile(serverSentenceArrayList.get(2), conflictingFile.getPath(), dis);
    	                    		}
    	                    	}
    	                    	else {
    		                    	pr.println("C." + filename + "," + hashOfChangedFile);
    		                    	String response = br.readLine();
    		                    	if (response.equals("ok")) {
    		                    		System.out.println("You are the only user with this file open. Your file is updated.");
    		                    		unfilteredTransportFile(file,file1.getName(),pr,br,dos);
    			                        changed = true;  	                    		   
    		                    	}
    		                    	else if(response.equals("ok2")) {
    		                    		//error test
    		                    	}
    		                    	else if(response.charAt(0) == 'c'){
    		                    		System.out.println("Conflict found! Some other user has the file you saved open on their machine.");
    		                    		System.out.println("However there is no conflicting file yet."
    		                    				+ " Your current document will become the first version of the conflicting file.");
    		                    		FileUtils.copyFile(file, conflictingFile);
    		                    		unfilteredTransportFile(conflictingFile,conflictPath,pr,br,dos);  
    		                    	}
    		                    	else if (response.charAt(0) == 'F'){
    		                    		//case where there is a conflicting file on the server's directory
    		                    		System.out.println("Conflict found! Some other user has the file you saved open on their machine."
    		                    				+ " Downloading conflicting file in: " + conflictPath);
    		        					response = response.substring(2, response.length());
    		                			ArrayList<String> serverSentenceArrayList = new ArrayList<>();
    		                			Collections.addAll(serverSentenceArrayList, response.split("\\s*,\\s*"));
    		                			String FileDirAndName = conflictPath + File.separator + serverSentenceArrayList.get(0);
    		                			try {
    		                				receiveFile(serverSentenceArrayList.get(2),FileDirAndName,dis);   //changed first variable          				
    		                			} catch (IOException e) {
    		                				e.printStackTrace();
    		                			}
    		                    	}
    	                    	}	
        					}
        					else {
        						System.out.println("FILE UPDATED VIA VERSIONING DAEMON. IGNORE UPLOAD!!!!");
            					System.out.println("CHANGED HASH TESTING: " + hashOfChangedFile);
    	        				for(int i=0; i<FilenameHashMap.size(); i=i+2) {
    	        					if(FilenameHashMap.get(i).equals(filename)) {		
    	        						FilenameHashMap.remove(i+1);
    	        						FilenameHashMap.remove(i);
    	        						FilenameHashMap.add(filename);	        						
    	        						FilenameHashMap.add(hashOfChangedFile);
    	        						//break;
    	        					}
    	        				} 
        					}
        				}	                    		
                	}
				}
	        } catch (Exception ex) {
	            System.err.println(ex);
	        }
		}
		else {
			System.out.println("The file does not exist");
		}	
	}
	
    public void transportFile(File file, String dirName, PrintStream out, BufferedReader in, DataOutputStream dos) throws IOException {
		String fileName = file.getName();		
		long fileSize = file.length();
	    out.println("S." + fileName + "," + dirName + "," + fileSize);
	    String response = "";
	    response = br.readLine();
	    if(response.equals("ok")) {
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
	    else {
	    	System.out.println("You have no write access on this folder.");
	    }
    }
    
    public void unfilteredTransportFile(File file, String dirName, PrintStream out, BufferedReader in, DataOutputStream dos) throws IOException {
		String fileName = file.getName();		
		long fileSize = file.length();
	    out.println("S." + fileName + "," + dirName + "," + fileSize);
		byte b[] = new byte[(int) file.length()];
		try (FileInputStream fileInputStream = new FileInputStream(file.getPath())) {
			fileInputStream.read(b);
	        fileInputStream.close();
		}
		dos.write(b);
		System.out.println("Sent " + fileSize + " bytes to server.");
		dos.flush();
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
}

