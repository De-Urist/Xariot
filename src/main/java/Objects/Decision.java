package Objects;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Decision {
	
    private final String dec;
    private String id;
    Connection conn = null;
	String serverURL="jdbc:derby://localhost:5700/SSDatabase;create=true"; 

    public Decision(String dec, String id) throws SQLException {
        this.dec = dec;
        this.id = id;
        /*
		java.util.Properties props = new java.util.Properties();
		try {
			Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
			conn = DriverManager.getConnection(serverURL, props);
			System.out.println("Controller managed to open a connection to server database. ");
		} catch (SQLException | InstantiationException | IllegalAccessException | ClassNotFoundException e1) {
			System.out.println("Controller did not manage to open a connection to server database. ");
			e1.printStackTrace();
		}
		
		java.sql.PreparedStatement updateInvite;
		
		if (dec.equals("Accept")) {
			try {
				updateInvite = conn.prepareStatement("update FolderInvites set HasAccepted = 1 where ConnectionId = ?");
				updateInvite.setString(1,id);
				updateInvite.execute();		
				System.out.println("Invite accepted!");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		else {
			try {
				updateInvite = conn.prepareStatement("update FolderInvites set HasAccepted = 2 where ConnectionId = ?");
				updateInvite.setString(1,id);
				updateInvite.execute();	
				System.out.println("Invite declined!");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		conn.close();
		*/
    }

    public String getId() {
        return id;
    }

    public String getDec() {
        return dec;
    }
}
