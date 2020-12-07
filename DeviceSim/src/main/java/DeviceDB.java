import java.sql.*;

public class
DeviceDB {

    public static void main (String [] args) throws ClassNotFoundException, SQLException {

        //connection to sql database
        String url = "jdbc:mysql://localhost:3306/devicemgmt";
        String user = "root";
        String pass = "root1234";
        Class.forName("com.mysql.jdbc.Driver");
        Connection conn = DriverManager.getConnection(url, user, pass);
        System.out.println("Connected to database.");
        Statement statement = conn.createStatement();
    }
}
