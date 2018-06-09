import java.util.*;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.text.ParseException;
import java.text.SimpleDateFormat;


public class controller {
  public static void main(String[] args) {
    controller c = new controller();
    Scanner keyboard = new Scanner(System.in);
    int input = 0;

    // Make an interactive menu for slecting which option to run
    System.out.println("********* Welcome to Geraldo's and Luis' lab7 from " +
                      " csc365spring2018 *********");
    do {
      System.out.println("Please select one of the following menu options.");
      System.out.println("[1] Rooms and Rates");
      System.out.println("[2] Reservations");
      System.out.println("[3] Reservation Change");
      System.out.println("[4] Reservation Cancellation");
      System.out.println("[5] Detailed Reservation Information");
      System.out.println("[6] Revenue");
      System.out.println("[0] to exit");
      input = keyboard.nextInt();
      try {
        if (input == 1) {
          c.demo1();
        } else if (input == 2) {
          c.demo2();
        } else if  (input == 3) {
          c.demo3();
        } else if (input == 4) {
          c.demo4();
        } else if (input == 5) {
          c.demo5();
        } else if (input == 6) {
          c.demo6();
        } else if (input == 0) {
          System.out.println("Terminating program");
          return;
        } else {
          System.out.println("Invalid menu option.");
          System.out.println("Please try again.");
          input = -1;
        }
      } catch (SQLException e) {
         System.err.println("SQLException: " + e.getMessage());
      }
    } while (input > 0);
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
        "RIGHT OUTER JOIN " +
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
         "((SELECT Room, SUM(days) AS days " +
         "FROM  " +
           "(SELECT * " +
           "FROM  " +
             "(SELECT Room, DATEDIFF(CURRENT_DATE, CheckIn) AS days " +
             "FROM lab7_reservations " +
             "WHERE DATE_SUB(CURRENT_DATE, INTERVAL 180 DAY) BETWEEN CheckIn AND Checkout) AS a " +
           "UNION " +
             "(SELECT Room, DATEDIFF(Checkout, CURRENT_DATE) AS days " +
             "FROM lab7_reservations " +
             "WHERE CURRENT_DATE BETWEEN CheckIn AND Checkout)) AS a " +
         "GROUP BY Room) " +
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
  private void demo2() throws SQLException {
     String days_1, base_rate_1, room_code_1;
     Scanner sc = new Scanner(System.in);
     String firstName, lastName, roomCode, bedType, checkIn, checkOut, year, month, day, sql;
     String roomChoice[] = new String[10];
     String roomSuggestions[] = new String[5];

     int kids, adults, i, occupancy;

     // Step 0: Load MySQL JDBC Driver
     // No longer required as of JDBC 2.0  / Java 6
     System.out.println("R2: Reservations ***********");
     try{
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
        System.out.println("Customers first name:");
        firstName = sc.nextLine();
        System.out.println("Customers last name:");
        lastName = sc.nextLine();
        System.out.println("Code of desired room:");
        roomCode = sc.nextLine();
        System.out.println("Desired bed type:");
        bedType = sc.nextLine();

        System.out.println("Begin year of stay:");
        year = sc.nextLine();
        System.out.println("Begin month of stay:");
        month = sc.nextLine();
        System.out.println("Begin day of stay:");
        day = sc.nextLine();
        checkIn = year + "-" + month + "-" + day;

        System.out.println("End year of stay:");
        year = sc.nextLine();
        System.out.println("End month of stay:");
        month = sc.nextLine();
        System.out.println("End day of stay:");
        day = sc.nextLine();
        checkOut = year + "-" + month + "-" + day;

        System.out.println("Number of kids:");
        kids = sc.nextInt();
        System.out.println("Number of adults:");
        adults = sc.nextInt();
        occupancy = kids + adults;

        // TODO: wont work if both are "Any"
        if (roomCode == "Any"){
           sql =
                        " SELECT R.RoomName " +
                        " FROM lab7_reservations AS  RES, lab7_rooms AS R " +
                        " WHERE RES.Room = R.RoomCode " +
                        " AND '" + checkIn +
                        "' NOT BETWEEN RES.CheckIn AND RES.CheckOut " +
                        " AND '" + checkOut +
                        "' NOT BETWEEN RES.CheckIn AND RES.CheckOut " +
                        " GROUP BY R.RoomCode; ";
        } else {
           sql =
                        " SELECT R.RoomName " +
                        " FROM lab7_reservations AS  RES, lab7_rooms AS R " +
                        " WHERE RES.Room = R.RoomCode " +
                        " AND '" + checkIn +
                        "' NOT BETWEEN RES.CheckIn AND RES.CheckOut " +
                        " AND '" + checkOut +
                        "' NOT BETWEEN RES.CheckIn AND RES.CheckOut " +
                        " AND R.bedType = '" + bedType +
                        "' AND R.maxOcc >= " + occupancy +
                        " GROUP BY R.RoomCode; ";
        }


        if (bedType == "Any"){
           sql =
                        " SELECT R.RoomName " +
                        " FROM lab7_reservations AS  RES, lab7_rooms AS R " +
                        " WHERE RES.Room = R.RoomCode " +
                        " AND '" + checkIn +
                        "' NOT BETWEEN RES.CheckIn AND RES.CheckOut " +
                        " AND '" + checkOut +
                        "' NOT BETWEEN RES.CheckIn AND RES.CheckOut " +
                        " GROUP BY R.RoomCode; ";
        } else {
           sql =
                  " SELECT R.RoomName " +
                  " FROM lab7_reservations AS  RES, lab7_rooms AS R " +
                  " WHERE RES.Room = R.RoomCode " +
                  " AND '" + checkIn +
                  "' NOT BETWEEN RES.CheckIn AND RES.CheckOut " +
                  " AND '" + checkOut +
                  "' NOT BETWEEN RES.CheckIn AND RES.CheckOut " +
                  " AND R.bedType = '" + bedType +
                  "' AND R.maxOcc >= " + occupancy +
                  " GROUP BY R.RoomCode; ";
        }

        // sql = " SELECT R.RoomName " +
        //        " FROM lab7_reservations AS  RES, lab7_rooms AS R " +
        //        " WHERE RES.Room = R.RoomCode " +
        //        " AND '" + checkIn +
        //        "' NOT BETWEEN RES.CheckIn AND RES.CheckOut " +
        //        " AND '" + checkOut +
        //        "' NOT BETWEEN RES.CheckIn AND RES.CheckOut " +
        //        "' AND R.maxOcc >= " + occupancy;
        //
        // if (bedType != "Any"){
        //    sql += " AND R.bedType = '" + bedType;
        // }
        // if (roomCode != "Any"){
        //    sql += " AND R.RoomCode = '" + roomCode;
        // }
        //
        // sql += " GROUP BY R.RoomCode; ";
        //
        // System.out.println("SQL: " + sql);



        // Step 3: (omitted in this example) Start transaction
        try (Statement stmt = conn.createStatement()) {

           // Step 4: Send SQL statement to DBMS
           ResultSet rs = stmt.executeQuery(sql);
           // Step 5: Handle results
           i = 0;
           System.out.format("\n\nAvailable rooms:\n");
           while(rs.next()) {
              String roomname = rs.getString("RoomName");
              System.out.print(i + ": ");
              System.out.format("%s\n", roomname);
              roomChoice[i] = roomname;
              i++;
           }
        }

        // If there are no mathces! Find 5 with less restraints
        if (i == 0){
           sql = " SELECT R.RoomName " +
           " FROM lab7_reservations AS  RES, lab7_rooms AS R " +
           " WHERE RES.Room = R.RoomCode " +
           " AND '" + checkIn +
           "' NOT BETWEEN RES.CheckIn AND RES.CheckOut " +
           " AND '" + checkOut +
           "' NOT BETWEEN RES.CheckIn AND RES.CheckOut " +
           "' AND R.maxOcc >= " + occupancy +
           " GROUP BY R.RoomCode; ";
           try (Statement stmt = conn.createStatement()) {

              // Step 4: Send SQL statement to DBMS
              ResultSet rs = stmt.executeQuery(sql);
              // Step 5: Handle results
              System.out.format("\n\nAvailable rooms:\n");
              while(rs.next()) {
                 if (i == 5){
                    break;
                 }
                 String roomname = rs.getString("RoomName");
                 System.out.print(i + ": ");
                 System.out.format("%s\n", roomname);
                 roomSuggestions[i] = roomname;
                 i++;
              }
           }

           sql = " SELECT R.RoomName " +
           " FROM lab7_reservations AS  RES, lab7_rooms AS R " +
           " WHERE RES.Room = R.RoomCode " +
           " AND '" + checkIn +
           "' NOT BETWEEN RES.CheckIn AND RES.CheckOut " +
           " AND '" + checkOut +
           "' NOT BETWEEN RES.CheckIn AND RES.CheckOut " +
           " GROUP BY R.RoomCode; ";
           try (Statement stmt = conn.createStatement()) {

              // Step 4: Send SQL statement to DBMS
              ResultSet rs = stmt.executeQuery(sql);
              // Step 5: Handle results
              while(rs.next()) {
                 if (i == 5){
                    break;
                 }
                 String roomname = rs.getString("RoomName");
                 System.out.print(i + ": ");
                 System.out.format("%s\n", roomname);
                 roomSuggestions[i] = roomname;
                 i++;
              }
           }

           sql = " SELECT R.RoomName " +
           " FROM lab7_reservations AS  RES, lab7_rooms AS R " +
           " WHERE RES.Room = R.RoomCode " +
           " GROUP BY R.RoomCode; ";
           try (Statement stmt = conn.createStatement()) {

              // Step 4: Send SQL statement to DBMS
              ResultSet rs = stmt.executeQuery(sql);
              // Step 5: Handle results
              while(rs.next()) {
                 if (i == 5){
                    break;
                 }
                 String roomname = rs.getString("RoomName");
                 System.out.print(i + ": ");
                 System.out.format("%s\n", roomname);
                 roomSuggestions[i] = roomname;
                 i++;
              }
           }
        }

        System.out.println("Which room would you like? (Pick a number)");
        int roomNumber = sc.nextInt();

        String days  =
        "SELECT DATEDIFF('" + checkOut + "' , '" + checkIn +
        "') AS DateDiff;" ;

        try (Statement stmt = conn.createStatement()) {
           // Step 4: Send SQL statement to DBMS
           ResultSet rs = stmt.executeQuery(days);

           rs.next();
           days_1 = rs.getString("DateDiff");
           // System.out.format("%s\n", days_1);
           // Step 5: Handle results
        }


        String base_rate = "SELECT basePrice " +
        " FROM lab7_rooms " +
        " WHERE RoomName = " + "'" + roomChoice[roomNumber] +
        "';";
        try (Statement stmt = conn.createStatement()) {
           // Step 4: Send SQL statement to DBMS
           ResultSet rs = stmt.executeQuery(base_rate);
           rs.next();
           base_rate_1 = rs.getString("basePrice");
           // System.out.format("%s\n", /*row_number,*/ base_rate_1);

           // Step 5: Handle results
        }

        String room_code = "SELECT RoomCode " +
        " FROM lab7_rooms " +
        "WHERE RoomName =" + "'" + roomChoice[roomNumber] +
        "';";
        try (Statement stmt = conn.createStatement()) {
           // Step 4: Send SQL statement to DBMS
           ResultSet rs = stmt.executeQuery(room_code);
           rs.next();
           room_code_1 = rs.getString("RoomCode");
           // System.out.format("%s\n", /*row_number,*/ room_code_1);

           // Step 5: Handle results
        }

        double booking_rate = (Float.parseFloat(days_1) * Float.parseFloat(base_rate_1)) + (.18 * Float.parseFloat(base_rate_1));

        /*Number of weekend days * (110% of the room base rate) -------------TODO*/


        System.out.println("\nConfirmation for:" );
        System.out.println("Customer: " + firstName + " " + lastName);
        System.out.println("Room: " + room_code_1 + " " + roomChoice[roomNumber] + " ");
        System.out.println("From: " + checkIn + " to " +checkOut);
        System.out.println("Adults: " + adults);
        System.out.println("Children: " + kids);
        System.out.println("Rate: " + booking_rate);

        System.out.println("\n-----------------------");
        System.out.println("0: Place reservation\n1: Cancel");
        System.out.println("-----------------------\n");
        int confirmation = sc.nextInt();

        Random r = new Random();
        int low = 0;
        int high = 10000;
        int result = r.nextInt(high - low) + low;

        if (confirmation == 0) {
           String insertSql = "INSERT INTO lab7_reservations " +
           " VALUES (" +
           String.format("%05d", result) + " , '" +
           room_code_1 + "' , " +
           "'" + checkIn + "'" + "," +
           "'" + checkOut + "'" + "," +
           booking_rate + "," +
           "'" + lastName + "'" + "," +
           "'" + firstName + "'" + "," +
           adults + "," + kids + ");";

           System.out.println(insertSql);
           try (Statement stmt = conn.createStatement()) {

              // Step 4: Send SQL statement to DBMS
              // boolean exRes = stmt.execute(sql);
              int ru = stmt.executeUpdate(insertSql);
              // TODO
           }
        }
        else if (confirmation == 1){
           System.out.println("Going back to main menu...");
        }
        else {
           System.out.println("Wrong input...quiting....");
        }

        // Step 6: (omitted in this example) Commit or rollback transaction
     }
     // Step 7: Close connection (handled by try-with-resources syntax)
  }


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
              "--OPTIONS--\n-first name-\n-last name-\n-begin date-\n-end date-\n" +
              "-number of children-\n-number of adults-\n" +
              "-Or type in 'no change'-");
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
          if (rowCount > 0) {
  		    System.out.format("Updated %d records for %s %n", rowCount, newarg);
          // Step 6: Commit or rollback transaction
  		    conn.commit();
        } else {
          System.out.println("** No records were changed. **\n" +
          "** Enter in a valid reservation cod. **");
          conn.rollback();
        }

  	    } catch (SQLException e) {
  		      conn.rollback();
  	    }
      }
      else if (fieldchange.equals("begin date") || fieldchange.equals("end date")) {
        System.out.println("** Attmepting to change date **");
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
          if (!rs.next()) {
            System.out.print("Invalid room code!\nTerminating program.");
            return;
          } else {
            rs.beforeFirst();
          }
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
            System.out.println("** Checkin and Checkout dates do not cross themselves **");
            // Check conflict with other reservations
            String sqlcheckdate = "SELECT * FROM lab7_reservations " +
                    "WHERE CODE <> ? AND ? BETWEEN CheckIn AND Checkout " +
                    "AND Room = ?";
            // Start transaction
            conn.setAutoCommit(false);
            try (PreparedStatement psmt2 = conn.prepareStatement(sqlcheckdate)) {
              psmt2.setString(1, resnumber);
              psmt2.setDate(2, java.sql.Date.valueOf(newarg));
              psmt2.setString(3, room);
      		    ResultSet rs2 = psmt2.executeQuery();

              // If the set is empty, no conflict
              if (!rs2.next()) {
                // Update the reservation
                System.out.println("** No conflicts with given date " + newarg + " **");
                String updateSql = "";
                if (fieldchange.equals("begin date")) {
                  System.out.println("** Updating checkin date **");
                  updateSql = "UPDATE lab7_reservations " +
                              "SET CheckIn = ? " +
                              "WHERE CODE = ? ";
                } else {
                  System.out.println("** Updating checkout date **");
                  updateSql = "UPDATE lab7_reservations " +
                              "SET Checkout = ? " +
                              "WHERE CODE = ? ";
                }
                // Start transaction
                conn.setAutoCommit(false);
                try (PreparedStatement stmt3 = conn.prepareStatement(updateSql)) {
                  stmt3.setDate(1, java.sql.Date.valueOf(newarg));
                  stmt3.setString(2, resnumber);
                  // Step 4: Send SQL statement to DBMS
          		    int rowCount = stmt3.executeUpdate();
                  // Step 5: Handle results
                  if (rowCount > 0) {
          		      System.out.println("** Updated reservation " + resnumber + " **");
                    conn.commit();
                  } else {
                    System.out.println("Error updating reservation");
                    conn.rollback();
                  }
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
      else if (fieldchange.equals("number of children") || fieldchange.equals("number of adults")) {
        // Update number of children
        // Update number of adults
        System.out.println("Updating " + fieldchange);
        // Convert the given number string to integer
        int people = Integer.parseInt(newarg);
        String reservation = "SELECT * FROM lab7_reservations WHERE CODE = ?";
        String roomcode, room, checkin, checkout, rate;
        String adults, kids, lastname, firstname;
        String updateSql;
        updateSql = roomcode = room = checkin = checkout = rate = adults = kids = lastname = firstname = "";
        conn.setAutoCommit(false);
        try (PreparedStatement pstmt = conn.prepareStatement(reservation)) {
          pstmt.setString(1, resnumber);
          ResultSet rs = pstmt.executeQuery();
          // Get current reservation
          if (!rs.next()) {
            System.out.println("Invalid room code!\nTerminating program.");
            return;
          } else {
            rs.beforeFirst();
          }
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
          // Do not allow more than 4 children or less than 0 children per room
          if (fieldchange.equals("number of children")) {
            if (people < 0) {
              System.out.println("Cannot have less than 0 children in a room");
            } else if (people > 5) {
              System.out.println("Cannot have more than 4 children per room");
            } else {
              updateSql = "UPDATE lab7_reservations " +
                          "SET Kids = ? " +
                          "WHERE CODE = ? ";
            }
          }
          // Do not allow less than 1 adult and more than 4 adults
          else {
            if (people < 1) {
              System.out.println("Cannot have less than 1 adult per room");
            } else if (people > 4) {
              System.out.println("Cannot have more than 4 adults per room");
            } else {
              updateSql = "UPDATE lab7_reservations " +
                          "SET Adults = ? " +
                          "WHERE CODE = ? ";
            }
          }
          conn.setAutoCommit(false);
          try (PreparedStatement pstmt2 = conn.prepareStatement(updateSql)) {
            pstmt2.setString(1, newarg);
            pstmt2.setString(2, resnumber);
            int rowCount = pstmt2.executeUpdate();
            if (rowCount > 0) {
              System.out.println("** Updated " + fieldchange + " to " + newarg + " **");
              conn.commit();
            } else {
              System.out.println("Failed to update reservation");
              conn.rollback();
            }
          } catch (SQLException e) {
              conn.rollback();
          }
        } catch (SQLException e) {
            conn.rollback();
        }
      }
    }
  }
  // ***************************************************************************
  // Requrirement #4
  // ***************************************************************************
  /*
  Reservation Cancellation.
  Allow the user to cancel an existing reservation.
  Accept from the user a reservation code, confirm the cancellation,
  then remove the reservation record from the database.
  */
  private void demo4() throws SQLException {
    String roomcode, room, checkin, checkout, rate;
    String adults, kids, lastname, firstname;
    String updateSql;
    updateSql = roomcode = room = checkin = checkout = rate = adults = kids = lastname = firstname = "";

    System.out.println("-- Reservation Cancellation --");
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
      // Get user input
      Scanner scanner = new Scanner(System.in);
      System.out.println("Enter in the reservation code for the reservation you wish to cancel");
      String resnumber = scanner.nextLine();
      String sql = "SELECT * FROM lab7_reservations WHERE CODE = ?";
      // Print the current reservation
      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, resnumber);
        ResultSet rs = pstmt.executeQuery();
        // Get current reservation
        if (!rs.next()) {
          System.out.println("Invalid room code!\nTerminating program.");
          return;
        } else {
          rs.beforeFirst();
        }
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
        System.out.println("Code\tRoom\tCheckin\tCheckout\tRate\tLastName\tFirstName\tAdults\tKids");
        System.out.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n",
        roomcode, room, checkin, checkout, rate, lastname, firstname, adults, kids);
        System.out.println("Are you sure you want to delete this reservation? (y/n)");
        String answer = scanner.nextLine();
        if (answer.equals("y") || answer.equals("Y")) {
          // Remove the reservation
          String deleteSql = "DELETE FROM lab7_reservations WHERE CODE = ?";
          conn.setAutoCommit(false);
          try (PreparedStatement pstmt2 = conn.prepareStatement(deleteSql)) {
        		pstmt2.setString(1, resnumber);
        		int rowCount = pstmt2.executeUpdate();
            if (rowCount > 0) {
              System.out.println("Reservation successfully removed");
              conn.commit();
            } else {
              System.out.println("Reservation failed to delete.");
              System.out.println("Please try again");
              conn.rollback();
              demo4();
            }
          } catch (SQLException e) {
    		  conn.rollback();
          }
        } else if (answer.equals("n") || answer.equals("N")) {
          System.out.println("You have chosen to keep your reservation.");
          System.out.println("Enjoy your stay :)");
          return;
        } else {
          System.out.println("Invalid option, please try again.");
          demo4();
        }
      } catch (SQLException e) {
          conn.rollback();
      }
    }
  }
  // ***************************************************************************
  // Requrirement #5
  // ***************************************************************************
  private void demo5() throws SQLException {
    /*
    Present the user with a search prompt or form that allows them to
    enter any combination of the fields listed below
    (a blank entry should indicate ”Any”).
    For all fields except dates, permit partial values using SQL LIKE
    wildcards (for example: GL% should be allowed as a last name search value)
      • First name
      • Last name
      • A range of dates
      • Room code
      • Reservation code
      Using the search information provided,
      display a list of all matching reservations found in the database.
      The list shall show the contents of every attribute from the
      lab7 reservations table (as well as the full name of the room,
      and any extra information about the room you wish to add).
    */
     Scanner sc = new Scanner(System.in);
     String firstName, lastName, roomCode, reservationCode, checkIn, checkOut, year, month, day;
     String conditions = "";

     System.out.println("R5: Detailed Reservation Information ***");
     System.out.println("Enter the options you would like to search for: (leave blank if no prefrence)");

     // Get the customers name
     System.out.println("Customers first name:");
     firstName = sc.nextLine();
     System.out.println("Customers last name:");
     lastName = sc.nextLine();

     // Get the customers beging date
     System.out.println("Begin year of stay:");
     year = sc.nextLine();
     System.out.println("Begin month of stay:");
     month = sc.nextLine();
     System.out.println("Begin day of stay:");
     day = sc.nextLine();
     checkIn = year + "-" + month + "-" + day;

     // Get the customers end date
     System.out.println("End year of stay:");
     year = sc.nextLine();
     System.out.println("End month of stay:");
     month = sc.nextLine();
     System.out.println("End day of stay:");
     day = sc.nextLine();
     checkOut = year + "-" + month + "-" + day;

     // Get the customers room code
     System.out.println("Code of desired room:");
     roomCode = sc.nextLine();
     System.out.println("Reservation Code:");
     reservationCode = sc.nextLine();

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
        int where = 0;
        if(firstName.length() != 0){
           if (where == 0) {
              conditions += " WHERE ";
              where++;
           } else{
              conditions += " AND ";
           }
           if (firstName.indexOf('%') >= 0){
             conditions += " FirstName LIKE '" + firstName + "' ";
           }
           else {
             conditions += " FirstName = '" + firstName + "' ";
          }
        }
        if(lastName.length() != 0){
           if (where == 0){
              conditions += " WHERE ";
              where++;
           } else {
              conditions += " AND ";
           }
           if (lastName.indexOf('%') >= 0){
             conditions += " LastName LIKE '" + lastName + "' ";
           }
           else {
             conditions += " LastName = '" + lastName + "' ";
           }
        }
        if(checkIn.length() == 10){
           if (where == 0){
              conditions += " WHERE ";
              where++;
           } else {
              conditions += " AND ";
           }
           if (checkIn.indexOf('%') >= 0){
             conditions += " CheckIn LIKE '" + checkIn + "' ";
           }
           else {
             conditions += " CheckIn = '" + checkIn + "' ";
            }
        }
        if(checkOut.length() == 10){
           if (where == 0){
              conditions += " WHERE ";
              where++;
           } else{
              conditions += " AND ";
           }
           if (checkOut.indexOf('%') >= 0){
             conditions += " CheckOut LIKE '" + checkOut + "' ";
           }
           else {
           conditions += " CheckOut = '" + checkOut + "' ";
         }
        }
        if(roomCode.length() != 0){
           if (where == 0){
              conditions += " WHERE ";
              where++;
           } else{
              conditions += " AND ";
           }
           if (roomCode.indexOf('%') >= 0){
             conditions += " Room LIKE '" + roomCode + "' ";
           }
           else {
             conditions += " Room = '" + roomCode + "' ";
           }
        }
        if(reservationCode.length() != 0 ){
           if (where == 0){
              conditions += " WHERE ";
              where++;
           } else{
              conditions += " AND ";
           }
           if (reservationCode.indexOf('%') >= 0){
             conditions += " CODE LIKE " + reservationCode + " ";
           }
           else {
             conditions += " CODE = " + reservationCode + " ";
          }
        }
        conditions += ";";

        String sql = " SELECT * FROM lab7_reservations " + conditions;
        //System.out.println("The sql query is: " + sql);

        try (Statement stmt = conn.createStatement()) {
           // Step 4: Send SQL statement to DBMS
           ResultSet rs = stmt.executeQuery(sql);
           // Step 5: Handle results

           if (!rs.next()) {
           System.out.println("No rooms match the current search criteria. Please try again.");
           return;
          } else {
           rs.beforeFirst();
          }
           System.out.format("CODE\t\tRoom\t\tCheckIn\t\tCheckout\t\tRate\t\tLastName\t\tFirstName\t\tAdults\t\tKids\n");
           while(rs.next()) {
             String code1 = rs.getString("CODE");
             String room1 = rs.getString("Room");
             String checkIn1 = rs.getString("CheckIn");
             String checkOut1 = rs.getString("Checkout");
             String rate1 = rs.getString("Rate");
             String lastName1 = rs.getString("LastName");
             String firstName1 = rs.getString("FirstName");
             String adults1 = rs.getString("Adults");
             String kids1 = rs.getString("Kids");

             System.out.format("%s\t\t%s\t\t%s\t%s\t\t%s\t\t%s\t\t%s\t\t\t%s\t\t%s\n", code1, room1, checkIn1, checkOut1, rate1, lastName1, firstName1, adults1, kids1);
          }
        }
     }
  }

  // ***************************************************************************
  // Requrirement #6
  // ***************************************************************************
  private void demo6() throws SQLException {
    /*
    When this option is selected, your system shall provide a month-by-month
    overview of revenue for an entire year. For the purpose of this assignment,
    all revenue from the reservation is assigned to the month and year when the
    reservation ended. For example a seven-day hotel stay that started
    on October 30 will be treated as November revenue.
    Your system shall display a list of rooms, and, for each room,
    13 columns: 12 columns showing dollar revenue for each month and a
    13th column to display total year revenue for the room.
    There shall also be a ”totals” row in the table, which contains column
    totals. All amounts should be rounded to the nearest whole dollar.
    */
    System.out.println("--------------------");
    System.out.println("-- Revenue report --");
    System.out.println("--------------------");
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
         String sql = "SELECT * " +
         "FROM  " +
           "(SELECT mon2.Room, January, February, March, April, May, June, July, August, September, October, November, December, mon2.Year, Year_Revenue " +
           "FROM " +
             "(SELECT * " +
             "FROM " +
               "(SELECT Room, " +
                 "MAX(CASE WHEN (Month = 'January' AND YEAR = 2018) THEN Month_Revenue ELSE NULL END) AS January, " +
                 "MAX(CASE WHEN (Month = 'February' AND YEAR = 2018) THEN Month_Revenue ELSE NULL END) AS February, " +
                 "MAX(CASE WHEN (Month = 'March' AND YEAR = 2018) THEN Month_Revenue ELSE NULL END) AS March, " +
                 "MAX(CASE WHEN (Month = 'April' AND YEAR = 2018) THEN Month_Revenue ELSE NULL END) AS April, " +
                 "MAX(CASE WHEN (Month = 'May' AND YEAR = 2018) THEN Month_Revenue ELSE NULL END) AS May, " +
                 "MAX(CASE WHEN (Month = 'June' AND YEAR = 2018) THEN Month_Revenue ELSE NULL END) AS June, " +
                 "MAX(CASE WHEN (Month = 'July' AND YEAR = 2018) THEN Month_Revenue ELSE NULL END) AS July, " +
                 "MAX(CASE WHEN (Month = 'August' AND YEAR = 2018) THEN Month_Revenue ELSE NULL END) AS August, " +
                 "MAX(CASE WHEN (Month = 'September' AND YEAR = 2018) THEN Month_Revenue ELSE NULL END) AS September, " +
                 "MAX(CASE WHEN (Month = 'October' AND YEAR = 2018)  THEN Month_Revenue ELSE NULL END) AS October, " +
                 "MAX(CASE WHEN (Month = 'November' AND YEAR = 2018) THEN Month_Revenue ELSE NULL END) AS November, " +
                 "MAX(CASE WHEN (Month = 'December' AND YEAR = 2018) THEN Month_Revenue ELSE NULL END) AS December, " +
                 "2018 AS Year " +
               "FROM " +
                 "(SELECT * " +
                 "FROM " +
                   "(SELECT Room, Year, Month, ROUND(SUM(Revenue), 2) AS Month_Revenue " +
                   "FROM " +
                     "(SELECT Room, MONTHNAME(Checkout) AS Month, YEAR(Checkout) AS Year, DATEDIFF(Checkout, Checkin) AS Days, Rate, (DATEDIFF(Checkout, Checkin) * Rate) AS Revenue " +
                     "FROM lab7_reservations) AS a " +
                   "GROUP BY Room, Year, Month) AS a " +
                 "UNION " +
                   "(SELECT DISTINCT Room, 2019 AS Year, MONTHNAME(STR_TO_DATE(Month, '%m')) AS Month, 0.00 AS Year_Revenue " +
                   "FROM " +
                     "(SELECT DISTINCT Room, MONTH(Checkout) AS Month " +
                     "FROM lab7_reservations " +
                     "WHERE MONTH(Checkout)  > 1) AS m1)) AS mon2 " +
                     "GROUP BY Room) AS t18 " +
            "UNION " +
               "(SELECT Room, " +
                 "MAX(CASE WHEN (Month = 'January' AND YEAR = 2019) THEN Month_Revenue ELSE NULL END) AS January, " +
                 "MAX(CASE WHEN (Month = 'February' AND YEAR = 2019) THEN Month_Revenue ELSE NULL END) AS February, " +
                 "MAX(CASE WHEN (Month = 'March' AND YEAR = 2019) THEN Month_Revenue ELSE NULL END) AS March, " +
                 "MAX(CASE WHEN (Month = 'April' AND YEAR = 2019) THEN Month_Revenue ELSE NULL END) AS April, " +
                 "MAX(CASE WHEN (Month = 'May' AND YEAR = 2019) THEN Month_Revenue ELSE NULL END) AS May, " +
                 "MAX(CASE WHEN (Month = 'June' AND YEAR = 2019) THEN Month_Revenue ELSE NULL END) AS June, " +
                 "MAX(CASE WHEN (Month = 'July' AND YEAR = 2019) THEN Month_Revenue ELSE NULL END) AS July, " +
                 "MAX(CASE WHEN (Month = 'August' AND YEAR = 2019) THEN Month_Revenue ELSE NULL END) AS August, " +
                 "MAX(CASE WHEN (Month = 'September' AND YEAR = 2019) THEN Month_Revenue ELSE NULL END) AS September, " +
                 "MAX(CASE WHEN (Month = 'October' AND YEAR = 2019)  THEN Month_Revenue ELSE NULL END) AS October, " +
                 "MAX(CASE WHEN (Month = 'November' AND YEAR = 2019) THEN Month_Revenue ELSE NULL END) AS November, " +
                 "MAX(CASE WHEN (Month = 'December' AND YEAR = 2019) THEN Month_Revenue ELSE NULL END) AS December, " +
                 "2019 AS Year " +
               "FROM  " +
                 "(SELECT * " +
                 "FROM " +
                   "(SELECT Room, Year, Month, ROUND(SUM(Revenue), 2) AS Month_Revenue " +
                   "FROM " +
                     "(SELECT Room, MONTHNAME(Checkout) AS Month, YEAR(Checkout) AS Year, DATEDIFF(Checkout, Checkin) AS Days, Rate, (DATEDIFF(Checkout, Checkin) * Rate) AS Revenue " +
                     "FROM lab7_reservations) AS a " +
                   "GROUP BY Room, Year, Month) AS a " +
                 "UNION " +
                   "(SELECT DISTINCT Room, 2019 AS Year, MONTHNAME(STR_TO_DATE(Month, '%m')) AS Month, 0.00 AS Year_Revenue " +
                   "FROM " +
                     "(SELECT DISTINCT Room, MONTH(Checkout) AS Month " +
                     "FROM lab7_reservations " +
                     "WHERE MONTH(Checkout)  > 1) AS m1)) AS mon2 " +
                     "GROUP BY Room)) as mon2 " +
           "LEFT OUTER JOIN " +
             "(SELECT Room, Year, ROUND(SUM(Revenue), 2) AS Year_Revenue " +
             "FROM " +
               "(SELECT Room, MONTHNAME(Checkout) AS Month, YEAR(Checkout) AS Year, DATEDIFF(Checkout, Checkin) AS Days, Rate, (DATEDIFF(Checkout, Checkin) * Rate) AS Revenue " +
               "FROM lab7_reservations) AS a " +
             "GROUP BY Year, Room " +
             "ORDER BY Year, Room) AS ye " +
           "ON mon2.Room = ye.Room AND mon2.Year = ye.Year " +
           "ORDER BY Year, mon2.Room) AS a " +
       "UNION  " +
         "(SELECT 'Total' AS Month,  " +
           "SUM(January) AS January,  " +
           "SUM(February) AS February,  " +
           "SUM(March) AS March,  " +
           "SUM(April) AS April,  " +
           "SUM(May) AS May,  " +
           "SUM(June) AS June,  " +
           "SUM(July) AS July,  " +
           "SUM(August) AS August,  " +
           "SUM(September) AS September,  " +
           "SUM(October) AS October,  " +
           "SUM(November) AS November,  " +
           "SUM(December) AS December, " +
           "'----' AS Year, " +
           "SUM(Year_Revenue) AS Year_Revenue " +
         "FROM " +
           "(SELECT mon2.Room, January, February, March, April, May, June, July, August, September, October, November, December, mon2.Year, Year_Revenue " +
           "FROM " +
             "(SELECT * " +
             "FROM " +
               "(SELECT Room, " +
                 "MAX(CASE WHEN (Month = 'January' AND YEAR = 2018) THEN Month_Revenue ELSE NULL END) AS January, " +
                 "MAX(CASE WHEN (Month = 'February' AND YEAR = 2018) THEN Month_Revenue ELSE NULL END) AS February, " +
                 "MAX(CASE WHEN (Month = 'March' AND YEAR = 2018) THEN Month_Revenue ELSE NULL END) AS March, " +
                 "MAX(CASE WHEN (Month = 'April' AND YEAR = 2018) THEN Month_Revenue ELSE NULL END) AS April, " +
                 "MAX(CASE WHEN (Month = 'May' AND YEAR = 2018) THEN Month_Revenue ELSE NULL END) AS May, " +
                 "MAX(CASE WHEN (Month = 'June' AND YEAR = 2018) THEN Month_Revenue ELSE NULL END) AS June, " +
                 "MAX(CASE WHEN (Month = 'July' AND YEAR = 2018) THEN Month_Revenue ELSE NULL END) AS July, " +
                 "MAX(CASE WHEN (Month = 'August' AND YEAR = 2018) THEN Month_Revenue ELSE NULL END) AS August, " +
                 "MAX(CASE WHEN (Month = 'September' AND YEAR = 2018) THEN Month_Revenue ELSE NULL END) AS September, " +
                 "MAX(CASE WHEN (Month = 'October' AND YEAR = 2018)  THEN Month_Revenue ELSE NULL END) AS October, " +
                 "MAX(CASE WHEN (Month = 'November' AND YEAR = 2018) THEN Month_Revenue ELSE NULL END) AS November, " +
                 "MAX(CASE WHEN (Month = 'December' AND YEAR = 2018) THEN Month_Revenue ELSE NULL END) AS December, " +
                 "2018 AS Year " +
               "FROM " +
                 "(SELECT * " +
                 "FROM " +
                   "(SELECT Room, Year, Month, ROUND(SUM(Revenue), 2) AS Month_Revenue " +
                   "FROM " +
                     "(SELECT Room, MONTHNAME(Checkout) AS Month, YEAR(Checkout) AS Year, DATEDIFF(Checkout, Checkin) AS Days, Rate, (DATEDIFF(Checkout, Checkin) * Rate) AS Revenue " +
                     "FROM lab7_reservations) AS a " +
                   "GROUP BY Room, Year, Month) AS a " +
                 "UNION " +
                   "(SELECT DISTINCT Room, 2019 AS Year, MONTHNAME(STR_TO_DATE(Month, '%m')) AS Month, 0.00 AS Year_Revenue " +
                   "FROM " +
                     "(SELECT DISTINCT Room, MONTH(Checkout) AS Month " +
                     "FROM lab7_reservations " +
                     "WHERE MONTH(Checkout)  > 1) AS m1)) AS mon2 " +
                     "GROUP BY Room) AS t18 " +
             "UNION " +
               "(SELECT Room, " +
                 "MAX(CASE WHEN (Month = 'January' AND YEAR = 2019) THEN Month_Revenue ELSE NULL END) AS January, " +
                 "MAX(CASE WHEN (Month = 'February' AND YEAR = 2019) THEN Month_Revenue ELSE NULL END) AS February, " +
                 "MAX(CASE WHEN (Month = 'March' AND YEAR = 2019) THEN Month_Revenue ELSE NULL END) AS March, " +
                 "MAX(CASE WHEN (Month = 'April' AND YEAR = 2019) THEN Month_Revenue ELSE NULL END) AS April, " +
                 "MAX(CASE WHEN (Month = 'May' AND YEAR = 2019) THEN Month_Revenue ELSE NULL END) AS May, " +
                 "MAX(CASE WHEN (Month = 'June' AND YEAR = 2019) THEN Month_Revenue ELSE NULL END) AS June, " +
                 "MAX(CASE WHEN (Month = 'July' AND YEAR = 2019) THEN Month_Revenue ELSE NULL END) AS July, " +
                 "MAX(CASE WHEN (Month = 'August' AND YEAR = 2019) THEN Month_Revenue ELSE NULL END) AS August, " +
                 "MAX(CASE WHEN (Month = 'September' AND YEAR = 2019) THEN Month_Revenue ELSE NULL END) AS September, " +
                 "MAX(CASE WHEN (Month = 'October' AND YEAR = 2019)  THEN Month_Revenue ELSE NULL END) AS October, " +
                 "MAX(CASE WHEN (Month = 'November' AND YEAR = 2019) THEN Month_Revenue ELSE NULL END) AS November, " +
                 "MAX(CASE WHEN (Month = 'December' AND YEAR = 2019) THEN Month_Revenue ELSE NULL END) AS December, " +
                 "2019 AS Year " +
               "FROM " +
                 "(SELECT * " +
                 "FROM " +
                   "(SELECT Room, Year, Month, ROUND(SUM(Revenue), 2) AS Month_Revenue " +
                   "FROM " +
                     "(SELECT Room, MONTHNAME(Checkout) AS Month, YEAR(Checkout) AS Year, DATEDIFF(Checkout, Checkin) AS Days, Rate, (DATEDIFF(Checkout, Checkin) * Rate) AS Revenue " +
                     "FROM lab7_reservations) AS a " +
                   "GROUP BY Room, Year, Month) AS a " +
                 "UNION " +
                   "(SELECT DISTINCT Room, 2019 AS Year, MONTHNAME(STR_TO_DATE(Month, '%m')) AS Month, 0.00 AS Year_Revenue " +
                   "FROM " +
                     "(SELECT DISTINCT Room, MONTH(Checkout) AS Month " +
                     "FROM lab7_reservations " +
                     "WHERE MONTH(Checkout)  > 1) AS m1)) AS mon2 " +
                     "GROUP BY Room)) as mon2 " +
           "LEFT OUTER JOIN " +
             "(SELECT Room, Year, ROUND(SUM(Revenue), 2) AS Year_Revenue " +
             "FROM " +
               "(SELECT Room, MONTHNAME(Checkout) AS Month, YEAR(Checkout) AS Year, DATEDIFF(Checkout, Checkin) AS Days, Rate, (DATEDIFF(Checkout, Checkin) * Rate) AS Revenue " +
               "FROM lab7_reservations) AS a " +
             "GROUP BY Year, Room " +
             "ORDER BY Year, Room) AS ye " +
           "ON mon2.Room = ye.Room AND mon2.Year = ye.Year " +
           "ORDER BY Year, mon2.Room) as p) ";

           try (Statement stmt = conn.createStatement()) {
     		      // Step 4: Send SQL statement to DBMS
     		      ResultSet rs = stmt.executeQuery(sql);
     		      // Step 5: Handle results
               System.out.format("Room\tJan\tFeb\tMar\tApr\tMay\tJune\tJulyy\tAug\tSept\tOct\tNov\tDec\tYear\tRev\n");
               while(rs.next()) {
                 String room = rs.getString("Room");
                 String jan = rs.getString("January");
                 String feb = rs.getString("February");
                 String mar = rs.getString("March");
                 String apr = rs.getString("April");
                 String may = rs.getString("May");
                 String june = rs.getString("June");
                 String july = rs.getString("July");
                 String aug = rs.getString("August");
                 String sept = rs.getString("September");
                 String oct = rs.getString("October");
                 String nov = rs.getString("November");
                 String dec = rs.getString("December");
                 String year = rs.getString("Year");
                 String rev = rs.getString("Year_Revenue");

                 System.out.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n", room, jan, feb, mar, apr, may, june, july, aug, sept, oct, nov, dec, year, rev);
               }
          }
      }
  }
}
