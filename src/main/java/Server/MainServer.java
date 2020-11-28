package Server;

import java.util.Collections;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages={"Objects"})
public class MainServer implements Runnable
{		
	public static void main( String argv[] ) throws Exception
	{ 
		
		int port = 5500;
		int commulPort = 5550;
		int filePort = 5600;
		int databasePort = 5700;
		int keyExchangePort = 5800;
		int keyFileExchangePort = 5900;
		int FSDPort = 6050;
		int VDPort = 6100;
		//String servletPort = "6000";
		
		SServer myServer = new SServer(port, commulPort, filePort, databasePort, FSDPort , VDPort , keyExchangePort, keyFileExchangePort);
		myServer.start();
		
        SpringApplication.run(MainServer.class, argv);
		
		/*
        SpringApplication app = new SpringApplication(MainServer.class);
        app.setDefaultProperties(Collections.singletonMap("server.port", "8080"));
        app.run(argv);
        */
		//System.out.println("SS shuting down... ");
	}  
	
	public void run() {
		String [] argv = null;
		
		int port = 5500;
		int commulPort = 5550;
		int filePort = 5600;
		int databasePort = 5700;
		int keyExchangePort = 5800;
		int keyFileExchangePort = 5900;
		int FSDPort = 6050;
		int VDPort = 6100;
		//String servletPort = "6000";
		
		SServer myServer = new SServer(port, commulPort, filePort, databasePort, FSDPort , VDPort , keyExchangePort, keyFileExchangePort);
		myServer.start();
		
        SpringApplication.run(MainServer.class);		
	}
}  


