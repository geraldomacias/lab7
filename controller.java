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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class controller {
  public static void main(String[] args) {
    try {
      controller c = new controller();
      //c.demo1();
      //c.demo2();
      c.demo3();
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
      //fieldchange = fieldchange.toLowerCase();
      System.out.println("Enter the updated value");
      String newarg = scanner.nextLine();

      System.out.println("fieldchange:" + fieldchange);

      // Update first name
      // Update last name
      if (fieldchange.equals("first name") || fieldchange.equals("last name")) {
        // Create a prepared sql statement to change the last name
        String updateSql = "";
        if (fieldchange.equals("first name")) {
          updateSql = "UPDATE lab7_reservations SET FirstName = ? WHERE CODE = ?";
        }
        else {
          updateSql = "UPDATE lab7_reservations SET LastName = ? WHERE CODE = ?";
        }
        // Start transaction
        conn.setAutoCommit(false);

        try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
          // Step 4: Send SQL statement to DBMS
          pstmt.setString(1, newarg);
          pstmt.setString(2, resnumber);
  		    int rowCount = pstmt.executeUpdate();
          // Step 5: Handle results
  		    System.out.format("Updated %d records for %s %n", rowCount, newarg);
          // Step 6: Commit or rollback transaction
  		    conn.commit();
  	    } catch (SQLException e) {
  		      conn.rollback();
  	    }
      }
      else if (fieldchange.equals("begin date") || fieldchange.equals("end date")) {
        System.out.println("Attmepting to change date");
        // Check for conflict with given dates
        // Create a prepared sql statement
        // TODO This statement needs work
        // Must check if new begin date crosses into other reservation
        // Must check if new end date crosses into other reservation
        // Get information from current reservation
        String reservation = "SELECT * FROM lab7_reservations WHERE CODE = ?";
        String roomcode, room, checkin, checkout, rate;
        String adults, kids, lastname, firstname;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date begin, out;
        begin = out = new Date();
        roomcode = room = checkin = checkout = rate = adults = kids = lastname = firstname = "";
        // Start transaction
        conn.setAutoCommit(false);
        try (PreparedStatement pstmt = conn.prepareStatement(reservation)) {
          pstmt.setString(1, resnumber);
          ResultSet rs = pstmt.executeQuery();
          // Get current reservation
          while (rs.next()) {
            roomcode = rs.getString("CODE");
            room = rs.getString("Room");
            checkin = rs.getString("CheckIn");
            checkout = rs.getString("Checkout");
            rate = rs.getString("Rate");
            lastname = rs.getString("LastName");
            firstname = rs.getString("FirstName");
            adults = rs.getString("Adults");
            kids = rs.getString("Kids");
          }
          System.out.println("---- Current reservation ----");
          System.out.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n",
          roomcode, room, checkin, checkout, rate, lastname, firstname, adults, kids);
          // Check for begin date and end date crossing
          if (fieldchange.equals("begin date")) {
            try {
              begin = sdf.parse(newarg);
              out = sdf.parse(checkout);
            } catch (Exception e) {
              System.out.println("Failed string parse");
            }

          } else {
            try {
              begin = sdf.parse(checkin);
              out = sdf.parse(newarg);
            } catch (Exception e) {
              System.out.println("Failed string parse");
            }
          }
          // Date crosses
          if (begin.after(out) || out.before(begin)) {
            System.out.println("Invalid date given!");
            System.out.println("Checkin date cannot come after checkout date");
          }
          // Date are equal
          else if (begin.equals(out)) {
            System.out.println("Invalid date given!");
            System.out.println("Checkin date cannot be the same as checkout date");
          }
          // Dates do not cross eachother
          else {
            System.out.println("Checkin and Checkout dates do not cross themselves");
            // Check conflict with other reservations
            String sqlcheckdate = "SELECT * FROM lab7_reservations " +
                    "WHERE CODE <> ? AND '?' BETWEEN CheckIn AND Checkout" +
                    "AND Room = ?";
            // Start transaction
            //conn.setAutoCommit(false);
            try (PreparedStatement psmt2 = conn.prepareStatement(sqlcheckdate)) {
              psmt2.setString(1, resnumber);
              psmt2.setString(2, newarg);
              psmt2.setString(3, room);
      		    ResultSet rs2 = psmt2.executeQuery();
              // If the set is empty, no conflict
              if (!rs2.next()) {
                // Update the reservation
                System.out.println("No conflicts with given date " + newarg);
                String updateSql = "";
                if (fieldchange.equals("begin date")) {
                  updateSql = "UPDATE lab7_reservations " +
                              "SET CheckIn = '?' " +
                              "WHERE CODE = ? ";
                } else {
                  updateSql = "UPDATE lab7_reservations " +
                              "SET Checkout = '?' " +
                              "WHERE CODE = ? ";
                }
                // Start transaction
                conn.setAutoCommit(false);
                try (Statement stmt3 = conn.createStatement()) {
                  // Step 4: Send SQL statement to DBMS
          		    int rowCount = stmt3.executeUpdate(updateSql);
                  // Step 5: Handle results
          		    System.out.format("Updated reservation %d %n", resnumber);
          	    } catch (SQLException e) {
          		      conn.rollback();
          	    }
              } else {
                System.out.println("The given date has a conflict with reservation:"
                                  + rs.getString("CODE"));
              }
            } catch (SQLException e) {
      		      conn.rollback();
      	    }
          }
        } catch (SQLException e) {
  		      conn.rollback();
  	    }
      }
    }
    // Update number of children
    // Update number of adults
  }
}
