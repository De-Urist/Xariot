package GUI;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import MasterClient.MasterClient;
import Server.MainServer;
import javafx.application.Application;
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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainMaster extends Application {
	Button bootServer, genKeypair, createDb, createProject, uploadFile, inviteUsers, changeRights, delete;
	Label password, welcome, serverOut;
	TextField passwordF;
	Scene scene1,scene2;
	PrintStream out;
	BufferedReader in;
	DataOutputStream dos;
	DataInputStream dis;
	
	public static void main (String [] args) {
		launch(args);
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		//grid1
	    GridPane grid1 = new GridPane();
	    grid1.setHgap(10);
	    grid1.setVgap(12);
	    HBox hbButtons1 = new HBox();
	    hbButtons1.setSpacing(10.0);
	 
	    primaryStage.setResizable(false);
		primaryStage.setTitle("Xariot Master Client");	 
		
		bootServer = new Button();
		bootServer.setText("Boot server");
		bootServer.setOnAction(e -> { 
			MainServer server = new MainServer();
			server.run();
			System.out.println("Server started.");
			MasterClient MasterClient = new MasterClient(passwordF.getText());
			MasterClient.run();
			System.out.println("Master client connected. ");
			
			out = MasterClient.getOut();
			in = MasterClient.getIn();
			dis = MasterClient.getDis();
			dos = MasterClient.getDos();
			
			primaryStage.setScene(scene2);
			
			
		}); //you need to add the server bootup here
		genKeypair = new Button();
		genKeypair.setText("Generate keypair");
		
		password = new Label("Password : ");
		passwordF = new TextField();
	
		hbButtons1.getChildren().addAll(bootServer,genKeypair);
		hbButtons1.setAlignment(Pos.BOTTOM_CENTER);
	    grid1.add(password, 0, 0);
	    grid1.add(passwordF, 1, 0);
	    grid1.add(hbButtons1, 0, 2, 2, 1);
	    grid1.setAlignment(Pos.BASELINE_CENTER);
	    grid1.setPadding(new Insets(20, 10, 10, 10));	
	    
	    //grid2
		GridPane grid2 = new GridPane();
	    grid2.setHgap(10);
	    grid2.setVgap(12);
	    
	    VBox hbButtons2 = new VBox();
	    hbButtons2.setSpacing(10.0);
	    createDb = new Button();
	    createDb.setText("Create server database");
	    createProject = new Button();
	    createProject.setText("Create a synchronization project/directory");
	    uploadFile = new Button();
	    uploadFile.setText("Upload a file to a synchronization directory");
	    inviteUsers = new Button();
	    inviteUsers.setText("Invite users via e-mail");
	    changeRights = new Button();
	    changeRights.setText("Change rights on a user");
	    //delete = new Button();
	    //delete.setText("Delete a file from a directory");
	    welcome = new Label("Welcome to Xariot Master User");
	    serverOut = new Label("Server output : ");

	    /*
	    TextArea textBox = TextAreaBuilder.create().prefWidth(220).prefHeight(300).wrapText(true).build();
	    Console console = new Console(textBox);
	    PrintStream ps = new PrintStream(console,true);
	    System.setOut(ps);
        System.setErr(ps);
        */
	    
	    TextArea textBox =  new TextArea();
	    textBox.prefHeight(300);
	    textBox.prefWidth(220);
	    textBox.setWrapText(true);
	    
	    /*
	    Console console = new Console(textBox);
	    PrintStream ps = new PrintStream(console,true);
	    System.setOut(ps);
        System.setErr(ps);
	    */
	    
	    hbButtons2.getChildren().addAll(createDb,createProject,uploadFile,inviteUsers,changeRights);//,delete
	    hbButtons2.setAlignment(Pos.BASELINE_CENTER);
	    grid2.add(welcome, 0, 0);
	    grid2.add(hbButtons2, 0, 2, 2, 1);
	    grid2.add(serverOut, 0, 3);
	    grid2.add(textBox, 0, 4);
	    grid2.setAlignment(Pos.BASELINE_CENTER);
	    grid2.setPadding(new Insets(20, 10, 10, 10));	
	    
	    //button functionality for grid2
	    Label projectName = new Label("Project name : ");
	    TextField projectNameF = new TextField();
	    Label filePath = new Label("File path : ");
	    TextField filePathF = new TextField();
	    Label uEmail = new Label ("Your email : ");
	    TextField uEmailF = new TextField();
	    Label emailPass = new Label("Email password : ");
	    TextField emailPassF = new TextField();
	    Label targetEmail = new Label ("Targer email : ");
	    TextField targetEmailF = new TextField();
	    Label rightNum = new Label ("Right number : ");
	    TextField rightNumF = new TextField();
	    Label customUsername = new Label ("Custom username : ");
	    TextField customUsernameF = new TextField();
	    Label username = new Label("Username : ");
	    TextField usernameF = new TextField();
	    Label filename = new Label("Filename : ");
	    TextField filenameF = new TextField();
	    
	    createDb.setOnAction(e -> {
	    	System.out.println("Database has been initialised! ");
	    	out.println("A.");	
			String serverOutput;
			try {
				serverOutput = in.readLine();
				if(serverOutput.equals("ok")) {
					System.out.println("Tables created! ");
				}
				else if(serverOutput.equals("already")) {
					System.out.println("Unable to create tables! Database is created already! ");
				}
				else {
					System.out.println("Unable to create tables! Some other error exists! ");
				}				
			} catch (IOException e1) {
				e1.printStackTrace();
			} 
	    });
	    createProject.setOnAction(e -> AlertBoxOneField.display("Create project", null, projectName, projectNameF, out, in, null));
	    uploadFile.setOnAction(e -> AlertBoxTwoField.display("Upload file", null, projectName, projectNameF, filePath, filePathF, out, in, dos));
	    inviteUsers.setOnAction(e -> AlertBoxFiveField.display("Invite users", projectName, projectNameF, uEmail, uEmailF, emailPass, emailPassF, targetEmail, targetEmailF, rightNum, rightNumF, customUsername, customUsernameF, out, in));
	    changeRights.setOnAction(e -> AlertBoxThreeField.display("Change rights", projectName, projectNameF,rightNum, rightNumF, username, usernameF, out, in));
	    //delete.setOnAction(e -> AlertBoxTwoField.display("Delete file", projectName, projectNameF, filename, filenameF));
	    
	    //scene initialization
		scene1 = new Scene(grid1, 320, 400);
		scene2 = new Scene(grid2, 350, 600);
		primaryStage.setScene(scene1);
		primaryStage.show();
	}
}
