package Objects;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DecisionController implements ErrorController {
	
	String serverURL="jdbc:derby://localhost:5700/SSDatabase;create=true"; 
	Connection conn = null;
	
    @RequestMapping("/inv")
    public String decision(@RequestParam (value = "dec") String dec,
    		@RequestParam (value = "id") String id) throws SQLException
    {
    	//http://localhost:8080/inv?dec=Accept&id=123
    	//db conn query/update with params dec and id
		//Create database connection
    	
		java.util.Properties props = new java.util.Properties();
		try {
			Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
			conn = DriverManager.getConnection(serverURL, props);
			System.out.println("Controller managed to open a connection to server database. ");
		} catch (SQLException | InstantiationException | IllegalAccessException | ClassNotFoundException e1) {
			System.out.println("Controller did not manage to open a connection to server database. ");
			e1.printStackTrace();
		}
		
		String decision = String.format(dec);
		String inviteId = String.format(id);
		
		java.sql.PreparedStatement updateInvite;
		if (decision.equals("Accept")) {
			try {
				updateInvite = conn.prepareStatement("update FolderInvites set HasAccepted = 1 where ConnectionId = ?");
				updateInvite.setString(1,inviteId);
				updateInvite.execute();		
				return "Invite accepted!";
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		else {
			try {
				updateInvite = conn.prepareStatement("update FolderInvites set HasAccepted = 2 where ConnectionId = ?");
				updateInvite.setString(1,inviteId);
				updateInvite.execute();	
				return "Invite declined!";
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		conn.close();
		
		return "Error!";
    }
    @RequestMapping("/error")
    public String error() {
        return "Error handling";
    }

    @Override
    public String getErrorPath() {
        return "/error";
    }
}