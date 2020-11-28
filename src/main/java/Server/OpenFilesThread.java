package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;

public class OpenFilesThread implements Runnable{
	PrintStream pr;
	BufferedReader br;
	String clientResponse = "";
	Connection conn;
	ArrayList<String> openFiles = new ArrayList<>();

	public OpenFilesThread(PrintStream pr, BufferedReader br, Connection conn) {
		this.br = br;
		this.pr = pr;
		this.conn = conn;
	}
	
	@Override
	public void run() {
		while(true) {
			try {			
				clientResponse = "";
				clientResponse = br.readLine();
			}
			catch (IOException e) {
				//here
				System.out.println("Client broke connection.");
				break;
			}
			if (clientResponse.charAt(0) == 'P') {
				System.out.println("Got P removing files that exist in clientMsg & AL and adding "
						+ "ones not in the AL + db queries ");
				String tidyClientMessage = clientResponse.substring(2, clientResponse.length());
    			if(clientResponse.equals("P.")) {
    				//do nothing
    				System.out.println("No files changed state on clientside.");
    			}
    			else {
	    			ArrayList<String> clientResponseArrayList = new ArrayList<>();
	    			Collections.addAll(clientResponseArrayList, tidyClientMessage.split("\\s*,\\s*"));
	    			int simulUsers = 0;    				
	    			for(int i=0; i<clientResponseArrayList.size(); i++) {
	    				if(openFiles.contains(clientResponseArrayList.get(i))) {
	    					//remove
	    					openFiles.remove(clientResponseArrayList.get(i));
							try {
	    						java.sql.PreparedStatement selectSimul = conn.prepareStatement("select"
	    								+ " SimulUsers from Files where Filename = ?");
	    						selectSimul.setString(1, clientResponseArrayList.get(i));
	    						ResultSet rs = selectSimul.executeQuery();
	    						if(rs.next()) {
	    							simulUsers = rs.getInt(1);
	    						}
							    java.sql.PreparedStatement updateSimul = conn.prepareStatement("update"
							    		+ " Files set SimulUsers = ? where Filename = ?");
							    updateSimul.setInt(1, simulUsers-1);			    
							    updateSimul.setString(2, clientResponseArrayList.get(i));
							    updateSimul.executeUpdate();
							} catch (SQLException e) {
								e.printStackTrace();
							}
	    				}
	    				else {
	    					//add
	    					openFiles.add(clientResponseArrayList.get(i));
							try {
	    						java.sql.PreparedStatement selectSimul = conn.prepareStatement("select SimulUsers from Files where Filename = ?");
	    						selectSimul.setString(1, clientResponseArrayList.get(i));
	    						ResultSet rs = selectSimul.executeQuery();
	    						if(rs.next()) {
	    							simulUsers = rs.getInt(1);
	    						}
							    java.sql.PreparedStatement updateSimul = conn.prepareStatement("update Files set SimulUsers = ? where Filename = ?");
							    updateSimul.setInt(1, simulUsers+1);			    
							    updateSimul.setString(2, clientResponseArrayList.get(i));
							    updateSimul.executeUpdate();
							} catch (SQLException e) {
								e.printStackTrace();
							}
	    				}
	    			}    				
    			}
			}
			else if (clientResponse.charAt(0) == 'W') {
				System.out.println("Got W sending files last opened for this username. ");
				String tidyClientMessage = clientResponse.substring(2, clientResponse.length());
    			String firstMsg = "W.";
				if(openFiles.isEmpty()) {
    				pr.println(firstMsg);
    			}
    			else {
    				for(int i=0; i<openFiles.size(); i++) {
    					firstMsg = firstMsg + "," + openFiles.get(i);
    				}
    				pr.println(firstMsg);  				
    			}
			}
			else {
				System.out.println("Other message...");
			}
		}
	}
}
