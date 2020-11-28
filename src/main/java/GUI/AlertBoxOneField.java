package GUI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;

import javafx.geometry.Insets;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AlertBoxOneField {

	public static void display(String title, String username, Label l1, TextField t1, PrintStream out, BufferedReader in, TextArea box) {
		Stage window = new Stage();
		
		window.initModality(Modality.APPLICATION_MODAL);
		window.setTitle(title);
		window.setMinWidth(250);

		Button submit = new Button("Submit");
		submit.setOnAction(e -> { 
			if(title.equals("Read comments")) {
				out.println("9." + t1.getText() + "," + username);
				try {
					String serverOutput = in.readLine();
					if(serverOutput.equals("ok")) {
						String response = in.readLine();
	        			ArrayList<String> serverSentenceArrayList = new ArrayList<>();
	        			Collections.addAll(serverSentenceArrayList, response.split("\\s*,\\s*"));
	        			String finalText = "";
	        			while(!serverSentenceArrayList.isEmpty()) {
	        				
	        				finalText = finalText + "\n" + "Comments for: " + t1.getText();
	        				String user = serverSentenceArrayList.get(0);
	        				serverSentenceArrayList.remove(0);
	        				finalText = finalText + "\n" + user + ": ";
	        				String text = serverSentenceArrayList.get(0);
	        				serverSentenceArrayList.remove(0);
	        				finalText = finalText + "\n" + "-" + text;
	        				
	        				/*
	        				System.out.println("Comments for: " + t1.getText());
	        				String user = serverSentenceArrayList.get(0);
	        				serverSentenceArrayList.remove(0);
	        				System.out.println(user + ": ");
	        				String text = serverSentenceArrayList.get(0);
	        				serverSentenceArrayList.remove(0);
	        				System.out.println("-" + text);
	        				*/
	        			}
	        			box.setText(finalText);
					}
					else if(serverOutput.equals("noaccess")){
						System.out.println("Error reading comments. You do not have access to this project! ");
					}
					else if(serverOutput.equals("nocomments")) {
						System.out.println("There are no comments for this project! ");
					}
				} catch (IOException e1) {
					e1.printStackTrace();
					System.out.print("The client could not communicate with the server while requesting to read comments. Please try again.");
				}
			}
			else if (title.equals("Create project")) {
				out.println("1." + t1.getText());
				try {
					String serverOutput = in.readLine();
					if(serverOutput.equals("ok")) {
						System.out.println("Success creating a folder named : " + t1.getText());
					}
					else{
						System.out.println("Failure creating folder. ");
						System.out.println("This folder name already exists, or there is some other problem! ");
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				} 			
			}
			else {
				System.out.println("Error");
			}
			window.close();
		});
	
		Button cancel = new Button ("Cancel");
		cancel.setOnAction(e -> window.close());
		
	    GridPane grid = new GridPane();
	    grid.setHgap(10);
	    grid.setVgap(12);
	    HBox hbButtons1 = new HBox();
	    hbButtons1.setSpacing(10.0);	
	    
		hbButtons1.getChildren().addAll(submit,cancel);
		hbButtons1.setAlignment(Pos.BOTTOM_CENTER);
		
		grid.add(l1, 0, 0);
		grid.add(t1, 1, 0);
	    grid.add(hbButtons1, 0, 2, 2, 1);
	    grid.setAlignment(Pos.BASELINE_CENTER);
	    grid.setPadding(new Insets(20, 10, 10, 10));	
	    
		Scene scene = new Scene(grid);
		window.setScene(scene);
		window.showAndWait();
	}
	public void submitFunction1() {
		
	}
}
