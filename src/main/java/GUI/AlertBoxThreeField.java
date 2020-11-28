package GUI;

import java.io.BufferedReader;
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

public class AlertBoxThreeField {
	public static void display(String title, Label l1, TextField t1, Label l2, TextField t2, Label l3, TextField t3, PrintStream out, BufferedReader in) {
		Stage window = new Stage();
		
		window.initModality(Modality.APPLICATION_MODAL);
		window.setTitle(title);
		window.setMinWidth(250);

		Button submit = new Button("Submit");
		submit.setOnAction(e -> { 
			if(title.equals("Change rights")) {
				if(t1.getText().equals("") || t2.getText().equals("") || t3.getText().equals("")) {
					System.out.println("Some field was left empty! Try to change rights by filling all fields! ");				
				}
				else {
					//project,rightnum,username
					out.println('R' + t1.getText() + "," + t2.getText() + "," + t3.getText());
					try {
						String serverResponse = in.readLine();
						if(serverResponse.equals("NoUser")) {
							System.out.println("The username you typed does not exist on this project! ");
						}
						else if (serverResponse.equals("ok")) {
							System.out.println("The access rights for user " + t3.getText() + " in the project " + t1.getText() + " was successfully changed to right number " + t2.getText());
						}
						else {
							System.out.println("Some other error occured! Please try again!");
						}
					} catch (IOException e1) {
						e1.printStackTrace();
						System.out.println("Error");
					}
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
		grid.add(l3, 0, 2);
		grid.add(t3, 1, 2);
		
	    grid.add(hbButtons1, 0, 3, 3, 1);
	    grid.setAlignment(Pos.BASELINE_CENTER);
	    grid.setPadding(new Insets(20, 10, 10, 10));	
	    
		Scene scene = new Scene(grid);
		window.setScene(scene);
		window.showAndWait();
	}
}
