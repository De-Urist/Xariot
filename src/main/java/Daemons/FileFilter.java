package Daemons;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TimerTask;

import org.apache.commons.io.FileUtils;

public class FileFilter extends TimerTask{
	String dirPath;
	PrintStream pr;
	BufferedReader br;
	String username;
	
	public FileFilter(String username, String dirPath, PrintStream pr, BufferedReader br) {
        this.dirPath= dirPath;
		this.pr = pr;
		this.br = br;
		this.username = username;
	}
	
	@Override
	public void run() {
		System.out.println(" > > > Receiving previous opened files...");
		pr.println("W." + username);
		try {
			String response = br.readLine();
			String tidyServerMessage = response.substring(2, response.length());
			System.out.println(" > > > SERVER RESPONSE: " + response);
			if(response.equals("W.")) {
				//same files, do nothing
				String msgToServer = "P.";
				File clientDir = new File(dirPath);
				File [] filesOfDir = clientDir.listFiles();
				for(File file : filesOfDir) {
					if(file.getName().contains("~$") || file.getName().contains("~")) {
						//do nothing
					}
					else {
						boolean isFileUnlocked = file.renameTo(file);
						if(!isFileUnlocked) {
							msgToServer = msgToServer + "," + file.getName();
							System.out.println("File: " + file.getName() + " is being edited by user.");
						}
						else {
							//do nothing
							System.out.println("File: " + file.getName() + " is not opened by user.");
						}						
					}
				}
		        System.out.println(" > > > Sending open files and scanning logic for the first time...");
		        pr.println(msgToServer);	
			}
			else {
				ArrayList<String> serverSentenceArrayList = new ArrayList<>();
				Collections.addAll(serverSentenceArrayList, tidyServerMessage.split("\\s*,\\s*"));	
				String msgToServer = "P.";
				File clientDir = new File(dirPath);
				File [] filesOfDir = clientDir.listFiles();
				for(File file : filesOfDir) {
					if(file.getName().contains("~$") || file.getName().contains("~")) {
						//do nothing
					}
					else {
						boolean isFileUnlocked = file.renameTo(file);
						if(!isFileUnlocked) {
							if(serverSentenceArrayList.contains(file.getName())) {
								System.out.println("File: " + file.getName() + " is being edited by user.");
							}
							else {
								msgToServer = msgToServer + "," + file.getName();
								System.out.println("File: " + file.getName() + " was closed by the user.");
							}
						}
						else {
							if(serverSentenceArrayList.contains(file.getName())) {
								msgToServer = msgToServer + "," + file.getName();
								System.out.println("File: " + file.getName() + " is being edited by user.");
							}
							else {
								System.out.println("File: " + file.getName() + " is not opened by user.");
							}
						}
					}
				}
	            System.out.println(" > > > Sending open files and scanning logic...");
				pr.println(msgToServer);	
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
