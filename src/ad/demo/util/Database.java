package ad.demo.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database
{
	public static Connection connect()
	{
        Connection conn = null;
        try
        {
        	Class.forName("org.sqlite.JDBC");
        	
            // db parameters
            String url = "jdbc:sqlite:/faac.db";
            
            // create a connection to the database
            conn = DriverManager.getConnection(url);
        }
        catch (SQLException e)
        {
        	e.printStackTrace();
        }
        catch (ClassNotFoundException e)
        {
			e.printStackTrace();
		}
        
		return conn;
    }
}
