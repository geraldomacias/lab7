import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import java.util.Map;
import java.util.Scanner;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

public class controller {
  public static void main(String[] args) {
    try {
      controller c = new controller();
      c.demo1();
      //c.demo2();
      //c.demo3();
      //c.demo4();
      //c.demo5();
    } catch (SQLException e) {
  	    System.err.println("SQLException: " + e.getMessage());
    }
  }
  // ***************************************************************************
  // Requrirement #1
  // ***************************************************************************
  private void demo1() throws SQLException {
      System.out.println("R1: Rooms and Rates ***********");
    // Step 0: Load MySQL JDBC Driver
    // No longer required as of JDBC 2.0  / Java 6
    try {
      Class.forName("com.mysql.jdbc.Driver");
      System.out.println("MySQL JDBC Driver loaded");
    } catch (ClassNotFoundException ex) {
      System.err.println("Unable to load JDBC Driver");
      System.exit(-1);
    }
    // Step 1: Establish connection to RDBMS
  	try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
  							   System.getenv("HP_JDBC_USER"),
  							   System.getenv("HP_JDBC_PW"))) {
  	    // Step 2: Construct SQL statement
  	    String sql = "SELECT DISTINCT roo.RoomCode, roo.RoomName, roo.Beds, " +
          "roo.BedType, roo.maxOcc, roo.basePrice, " +
          "roo.decor, parta.popularity, partb.Available, " +
          "partc.recent_stay_day_count, partc.Checkout AS last_checkout " +
        "FROM lab7_reservations AS a " +
        "JOIN " +
         "(SELECT DISTINCT Room, CURRENT_DATE AS Available " +
         "FROM lab7_reservations " +
         "WHERE Room NOT IN " +
          "(SELECT Room " +
          "FROM lab7_reservations " +
          "WHERE CURRENT_DATE BETWEEN CheckIn AND Checkout) " +
        "UNION " +
         "(SELECT DISTINCT a.Room, MIN(a.Checkout) AS Available " +
         "FROM lab7_reservations AS a " +
         "CROSS JOIN lab7_reservations AS b " +
         "WHERE a.Room = b.Room " +
         "AND a.Checkout <> b.Checkin " +
         "AND CURRENT_DATE BETWEEN a.CheckIn AND a.Checkout " +
         "GROUP BY a.Room)) AS partb " +
        "ON a.Room = partb.Room " +
        "JOIN " +
         "(SELECT a.Room, DATEDIFF(Checkout, CheckIn) AS recent_stay_day_count, Checkout " +
         "FROM lab7_reservations AS a " +
         "JOIN " +
          "(SELECT Room, MAX(CheckOut) AS mco " +
          "FROM lab7_reservations " +
          "WHERE Checkout < CURRENT_DATE " +
          "GROUP BY Room) AS b " +
         "ON a.Room = b.Room " +
         "WHERE mco = Checkout) AS partc " +
        "ON a.Room = partc.Room " +
        "JOIN " +
         "(SELECT a.Room, ROUND((a.days + b.days) / 180, 2) AS popularity " +
         "FROM (SELECT Room, SUM(DATEDIFF(Checkout, CheckIn)) AS days " +
           "FROM lab7_reservations " +
           "WHERE DATEDIFF(CURRENT_DATE, Checkout) < 180 AND CURRENT_DATE < CheckIn " +
           "GROUP BY Room) AS a " +
         "JOIN " +
          "((SELECT Room, DATEDIFF(Checkout, CURRENT_DATE) AS days " +
           "FROM lab7_reservations " +
           "WHERE CURRENT_DATE BETWEEN CheckIn AND Checkout) " +
           "UNION " +
           "(SELECT DISTINCT Room, 0 AS days " +
           "FROM lab7_reservations " +
           "WHERE Room NOT IN " +
            "(SELECT DISTINCT Room " +
            "FROM lab7_reservations " +
            "WHERE CURRENT_DATE BETWEEN CheckIn AND Checkout))) as b " +
           "ON a.Room = b.room) AS parta " +
        "ON a.Room = parta.Room " +
        "JOIN lab7_rooms AS roo " +
        "ON a.Room = roo.RoomCode " +
        "ORDER BY parta.popularity DESC;";

  	    // Step 3: (omitted in this example) Start transaction
  	    try (Statement stmt = conn.createStatement()) {
  		      // Step 4: Send SQL statement to DBMS
  		      ResultSet rs = stmt.executeQuery(sql);
  		      // Step 5: Handle results
            System.out.format("Roomcode\tRoomName\tBeds\tBedType\tMaxOcc\tPrice\tDecor\tPopularity\tAvailable\tLastStayLen\tLast Checkout Date\n");
            while(rs.next()) {
              String roomcode = rs.getString("RoomCode");
              String roomname = rs.getString("RoomName");
              String bed = rs.getString("Beds");
              String bedtype = rs.getString("BedType");
              String maxocc = rs.getString("maxOcc");
              String baseprice = rs.getString("basePrice");
              String decor = rs.getString("decor");
              String popularity = rs.getString("popularity");
              String available = rs.getString("Available");
              String rsdc = rs.getString("recent_stay_day_count");
              String lastcheckout = rs.getString("last_checkout");

              System.out.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n", roomcode, roomname, bed, bedtype, maxocc, baseprice, decor, popularity, available, rsdc, lastcheckout);
            }

  	    }
  	    // Step 6: (omitted in this example) Commit or rollback transaction
  	}
  }
  // ***************************************************************************
  // Requrirement #2
  // ***************************************************************************


  // ***************************************************************************
  // Requrirement #3
  // ***************************************************************************
  // Allow the user to provide a new value or to indicate ”no change”
  // for a given field. Update the reservation based on any
  // new information provided. If the user requests different
  // begin and/or end dates, make sure to check whether the new
  // begin and end dates conflict with another reservation in the system.
  private void demo3() throws SQLException {
    // Get user input
    System.out.println("R3: Reservation Change **********");
    // Connect to the sql database
    try {
  	    Class.forName("com.mysql.jdbc.Driver");
  	    System.out.println("MySQL JDBC Driver loaded");
  	} catch (ClassNotFoundException ex) {
  	    System.err.println("Unable to load JDBC Driver");
  	    System.exit(-1);
  	}

    try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
    							   System.getenv("HP_JDBC_USER"),
    							   System.getenv("HP_JDBC_PW"))) {
      Scanner scanner = new Scanner(System.in);
      System.out.println("Enter a reservation number");
      String resnumber = scanner.nextLine();
      System.out.println("Enter in a field to change\n" +
              "Ex: first name, last name, begin date, end date, " +
              "number of children, number of adults\n" +
              "Or type in 'no change'");
      String fieldchange = scanner.nextLine();
      String[] fieldchangelist = fieldchange.split(" ");
      System.out.println("Enter the updated value");
      String newarg = scanner.nextLine();
      // Update first name
      // Update last name
        // Check for conflict with given dates
        // Update begin dates
          // Retrieve old checkin date
          // Check for schedule conflict
        // Update end date
          // Retrieve old checkout date
    }
    // Update number of children
    // Update number of adults
  }
}
