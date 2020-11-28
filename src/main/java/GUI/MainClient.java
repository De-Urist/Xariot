package GUI;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.PrintStream;

import Client.Client;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextAreaBuilder;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainClient extends Application{
	Button connectBtn, generateBtn, submitComment, readComments, submit, cancel;
	Label username,pubkp, privkp, password;
	Label username2, selectedUsername, boxTitle;
	Label serverDirName;
	TextField usernameF, pubkpF, privkpF, passwordF, serverDirNameF;
	Scene scene1, scene2;
	int startingScene;
	PrintStream out;
	BufferedReader in;
	DataOutputStream dos;
	DataInputStream dis;
    String clientKeyPath = "C:\\Users\\user\\Desktop\\clientKeys"; //2 for client2
    boolean flag = false;

	public static void main (String [] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		
		File [] filesInKeystoreDir = new File(clientKeyPath).listFiles();
		if (filesInKeystoreDir == null) {
			startingScene = 0;
		}
		else {
			for(File file : filesInKeystoreDir) {
				if(file.getName().equals("server.public")) {
					flag = true;
					startingScene = 2;
				}
			}
			if(!flag) {
				startingScene = 1;
			}
		}
		
		//grid 1 solid values initialization
	    GridPane grid1 = new GridPane();
	    grid1.setHgap(10);
	    grid1.setVgap(12);
	    HBox hbButtons1 = new HBox();
	    hbButtons1.setSpacing(10.0);	
	    
	    primaryStage.setResizable(false);
		primaryStage.setTitle("Xariot Client");	    
		//change to scanning if server public key exists
		if(startingScene == 1) {
			//Stage1
			connectBtn = new Button();
			connectBtn.setText("Connect to server");
			connectBtn.setOnAction(e -> { 
				if(usernameF.getText().equals("") || pubkpF.getText().equals("") || privkpF.getText().equals("") || passwordF.getText().equals("") || serverDirNameF.getText().equals("")) {
					ErrorAlertBox.display("Empty field!");
				}
				else {
			    	selectedUsername.setText(usernameF.getText());
			    	Client c1 = new Client(usernameF.getText(), pubkpF.getText(), privkpF.getText(), passwordF.getText(), serverDirNameF.getText(), 1);
			    	Platform.runLater(() -> {
				    	c1.run();	
			    	});
			    	out = c1.getOut();
			    	in = c1.getIn();			    	
					primaryStage.setScene(scene2);	
				}
			});
			
			generateBtn = new Button();
			generateBtn.setText("Generate keypair");
			
			hbButtons1.getChildren().addAll(connectBtn,generateBtn);
			hbButtons1.setAlignment(Pos.BOTTOM_CENTER);
			
			//one more texfgield label
			username = new Label("Username : ");
			usernameF = new TextField();
			pubkp = new Label("Public key path : ");
			pubkpF = new TextField();
			privkp = new Label("Private key path : ");
			privkpF = new TextField();
			password = new Label("Password : ");
			passwordF = new TextField();
			serverDirName = new Label("Server key storage directory name : ");
			serverDirNameF = new TextField();
			
		    grid1.add(username, 0, 0);
		    grid1.add(usernameF, 1, 0);
		    grid1.add(pubkp, 0, 1);
		    grid1.add(pubkpF, 1, 1);    
		    grid1.add(privkp, 0, 2);
		    grid1.add(privkpF, 1, 2);
		    grid1.add(password, 0, 3);
		    grid1.add(passwordF, 1, 3);   
		    grid1.add(serverDirName, 0, 4);
		    grid1.add(serverDirNameF, 1, 4);
		    grid1.add(hbButtons1, 0, 5, 5, 1);
		    grid1.setAlignment(Pos.BASELINE_CENTER);
		    grid1.setPadding(new Insets(20, 10, 10, 10));		
		    	
		    
		}
		else if (startingScene == 2){
			//Stage3
			connectBtn = new Button();
			connectBtn.setText("Connect to server");
			connectBtn.setOnAction(e -> {
				if(usernameF.getText().equals("") || serverDirNameF.getText().equals("") || passwordF.getText().equals("")) {
					ErrorAlertBox.display("Empty field!");	
				}
				else {
			    	selectedUsername.setText(usernameF.getText());
			    	Client c2 = new Client(usernameF.getText(), null, null, passwordF.getText(), serverDirNameF.getText(), 2);
			    		c2.run();
			    	out = c2.getOut();
			    	in = c2.getIn();			    	
				}
				primaryStage.setScene(scene2);
			});
			
			generateBtn = new Button();
			generateBtn.setText("Generate keypair");
			
			hbButtons1.getChildren().addAll(connectBtn,generateBtn);
			hbButtons1.setAlignment(Pos.BOTTOM_CENTER);
			
			username = new Label("Username : ");
			usernameF = new TextField();
			serverDirName = new Label("Server dir name : ");
			serverDirNameF = new TextField();
			password = new Label("Password : ");
			passwordF = new TextField();
			
		    grid1.add(username, 0, 0);
		    grid1.add(usernameF, 1, 0);
		    grid1.add(serverDirName, 0, 1);
		    grid1.add(serverDirNameF, 1, 1);  
		    grid1.add(password, 0, 2);
		    grid1.add(passwordF, 1, 2);
		    grid1.add(hbButtons1, 0, 3, 3, 1);
		    grid1.setAlignment(Pos.BASELINE_CENTER);
		    grid1.setPadding(new Insets(20, 10, 10, 10));	
		    
		    
		}
		else {
			ErrorAlertBox.display("Keystore not found!");
		}
		
	    //Stage2
		GridPane grid2 = new GridPane();
	    grid2.setHgap(10);
	    grid2.setVgap(12);
	    
	    HBox hbButtons2 = new HBox();
	    hbButtons2.setSpacing(10.0);
	    
	    username2 = new Label("Username : ");
	    selectedUsername = new Label();
	    submitComment = new Button();
	    Label l1 = new Label("Project name : ");
	    TextField t1 = new TextField();		    
	    Label l2 = new Label("Comment : ");
	    TextField t2 = new TextField();
	    submitComment.setText("Submit comment");
	    
	    readComments = new Button();
	    readComments.setText("Read comments");
	    
	    boxTitle = new Label("Server output : ");
	    TextArea textBox =  new TextArea();
	    textBox.prefHeight(300);
	    textBox.prefWidth(220);
	    textBox.setWrapText(true);
	    //Console console = new Console(textBox);
	    //PrintStream ps = new PrintStream(console,true);
	    //System.setOut(ps);
        //System.setErr(ps);
    
	    grid2.add(username2, 0, 0);
	    grid2.add(selectedUsername, 1, 0);
	    grid2.add(submitComment, 0, 1);
	    grid2.add(readComments, 1, 1);
	    grid2.add(boxTitle, 0, 2);
	    grid2.add(textBox, 0, 3);
	    
	    grid2.setAlignment(Pos.BASELINE_CENTER);
	    grid2.setPadding(new Insets(20, 10, 10, 10));	  
	    
	    submitComment.setOnAction(e -> AlertBoxTwoField.display("Submit comment", usernameF.getText() , l1, t1, l2, t2, out , in, dos));
	    readComments.setOnAction(e -> AlertBoxOneField.display("Read comments", usernameF.getText(), l1, t1, out , in, textBox));

	    //scene initialization
		scene1 = new Scene(grid1, 320, 400);
		scene2 = new Scene(grid2, 320, 400);
		primaryStage.setScene(scene1);
		primaryStage.show();
	}
}
