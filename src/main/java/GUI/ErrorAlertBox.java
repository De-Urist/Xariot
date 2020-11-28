package GUI;

import java.io.BufferedReader;
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

public class ErrorAlertBox {
	public static void display(String title) {
		Stage window = new Stage();
		Label l1 = new Label();
		
		window.initModality(Modality.APPLICATION_MODAL);
		window.setTitle(title);
		window.setMinWidth(250);

		Button submit = new Button("Ok");
		submit.setOnAction(e -> window.close());
		
	    GridPane grid = new GridPane();
	    grid.setHgap(10);
	    grid.setVgap(12);
	    HBox hbButtons1 = new HBox();
	    hbButtons1.setSpacing(10.0);	
	    
		hbButtons1.getChildren().addAll(submit);
		hbButtons1.setAlignment(Pos.BOTTOM_CENTER);
		
		if(title.equals("Keystore not found!")) {
			l1.setText("You have no public/private keystore pair! Generate one and come back again! ");
		}
		else if(title.equals("Empty field!")) {
			l1.setText("Some text field was left empty! Try again! ");			
		}
		else {
			l1.setText("Error! ");
		}
		
		grid.add(l1, 0, 0);
	    grid.add(hbButtons1, 0, 2, 2, 1);
	    grid.setAlignment(Pos.BASELINE_CENTER);
	    grid.setPadding(new Insets(20, 10, 10, 10));	
	    
		Scene scene = new Scene(grid);
		window.setScene(scene);
		window.showAndWait();
	}
}
