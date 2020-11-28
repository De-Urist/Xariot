package GUI;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AlertBoxTwoField {
	public static void display(String title, String username, Label l1, TextField t1, Label l2, TextField t2, PrintStream out, BufferedReader in, DataOutputStream dos) {
		Stage window = new Stage();
		
		window.initModality(Modality.APPLICATION_MODAL);
		window.setTitle(title);
		window.setMinWidth(250);

		Button submit = new Button("Submit");
		submit.setOnAction(e -> { 
			if(title.equals("Upload file")) {
				String filePath = t2.getText();
				File userFile = new File(filePath);
				if(userFile.exists()) {
					long fileSize = userFile.length();				
					String filename = userFile.getName();
					String folderName = t1.getText();
					out.println("U." + filename + "," + folderName + "," + fileSize);	
					System.out.println("Uploading file in Xariot server dir... ");
					
					File file = new File(filePath);
					byte[] b = new byte[(int) file.length()];
				    try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
			            fileInputStream.read(b);
					    dos.write(b);
					    dos.flush();			            
				    } catch (FileNotFoundException e1) {
						e1.printStackTrace();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					try {
						String serverOutput = in.readLine();
						if(serverOutput.equals("ok")) {
							System.out.println("File " + filename + " uploaded successfully to: " + folderName);
						}
						else {
							System.out.println("Error uploading file! ");
						}						
					} catch (IOException e1) {
						e1.printStackTrace();
					} 
				}
				else {
					System.out.println("The path you chose is invalid try again! ");
				}
			}
			else if(title.equals("Submit comment")){
				out.println("8." + t1.getText() + "," + t2.getText() + "," + username);
				try {
					String serverResponse = in.readLine();
					if(serverResponse.equals("ok")) {
						System.out.println("Comment submitted on project: " + t1.getText());
					}
					else if(serverResponse.equals("noaccess")){
						System.out.println("You have no access on this project!");
					}
					else if(serverResponse.equals("notexist")) {
						System.out.println("The project does not exist!");
					}
					else {
						System.out.println("Error");
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
		grid.add(l2, 0, 1);
		grid.add(t2, 1, 1);
		
	    grid.add(hbButtons1, 0, 2, 2, 1);
	    grid.setAlignment(Pos.BASELINE_CENTER);
	    grid.setPadding(new Insets(20, 10, 10, 10));	
	    
		Scene scene = new Scene(grid);
		window.setScene(scene);
		window.showAndWait();
	}
}
