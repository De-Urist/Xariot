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

public class AlertBoxFiveField { //actually six
	public static void display(String title, Label l1, TextField t1, Label l2, TextField t2, Label l3, TextField t3, Label l4, TextField t4, Label l5, TextField t5, Label l6, TextField t6, PrintStream out, BufferedReader in) {
		Stage window = new Stage();
		
		window.initModality(Modality.APPLICATION_MODAL);
		window.setTitle(title);
		window.setMinWidth(250);

		Button submit = new Button("Submit");

		submit.setOnAction(e -> { 
			if(t1.getText().equals("") || t2.getText().equals("") || t3.getText().equals("") || t4.getText().equals("") || t5.getText().equals("") || t6.getText().equals("")) {
				System.out.println("Some field was left empty! Try to send an invite by filling all text fields.");
			}
			else {
				//"Your E-mail, Application password, Folderame, Name, RightNumber, E-mail ");

				String sentence = "2." + t2.getText() + "," + t3.getText() + "," + t1.getText() + "," + t6.getText() + "," + t5.getText() + "," + t4.getText();
				out.println(sentence);
				String serverOutput = "";
				try {
					serverOutput = in.readLine();
					if(serverOutput.equals("ok")) {
						System.out.println("Invite recorded and an email is sent to : " + t4.getText());
					}
					else {
						System.out.println("Error creating an invite! Try again! ");
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
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
		grid.add(l4, 0, 3);
		grid.add(t4, 1, 3);
		grid.add(l5, 0, 4);
		grid.add(t5, 1, 4);
		grid.add(l6, 0, 5);
		grid.add(t6, 1, 5);
		
	    grid.add(hbButtons1, 0, 6, 6, 1);
	    grid.setAlignment(Pos.BASELINE_CENTER);
	    grid.setPadding(new Insets(20, 10, 10, 10));	
	    
		Scene scene = new Scene(grid);
		window.setScene(scene);
		window.showAndWait();
	}
}
