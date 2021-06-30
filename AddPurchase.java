import java.io.*;
import java.sql.Date;
import java.util.Scanner;
import java.time.*;
import java.util.*;
import java.net.*;
import java.text.*;
import java.lang.*;
import java.io.*;
import java.sql.*;
import pgpass.*;
import java.util.regex.*;  

/*============================================================================
CLASS AddPurchase
============================================================================*/

public class AddPurchase {
    private Connection conDB;        // Connection to the database system.
    private String url;              // URL: Which database?
    DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    java.util.Date DateValue = new java.util.Date(System.currentTimeMillis());
    private Integer cid;                       // the customer id who made the purchase
    private String  club;                      // which club that the purchase is made
    private String title;                      // which book the customer purchased
    private Integer year;                      // year of the book that customer purchased
    private String when=formatter.format(DateValue); //when the purchase is made. if not provided, use the system current time.
    private Integer qnty=1;                    // the number of copies of the book in this purchase. The default is 1.
    private String user="maazah";             // Database user account
    final Map<String, List<String>> params = new HashMap<>();
    // Constructor
    public AddPurchase (String[] args) {
        // Set up the DB connection.
        try {
            // Register the driver with DriverManager.
            Class.forName("org.postgresql.Driver").newInstance();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (InstantiationException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            System.exit(0);
        }

         List<String> options = null;
                for (int i = 0; i < args.length; i++) {
                      
                    final String a = args[i];
                    
                     

                    if (a.charAt(0) == '-' && ((a.charAt(1) >= 'a' && a.charAt(1) <= 'z') || (a.charAt(1) >= 'A' && a.charAt(1) <= 'Z'))) {
                        if (a.length() < 2) {
                            System.err.println("Error at argument " + a);
                            return;
                        }

                        options = new ArrayList<>();
                        params.put(a.substring(1), options);

                    }
                    else if (options != null) {
                        options.add(a);
                    }
                    else {
                        System.err.println("Illegal parameter usage");
                        return;
                    }


                }

        if (params.get("c")==null | params.get("b")==null | params.get("t")==null | params.get("y")==null)  
       {
            // Don't know what's wanted.  Bail.
            
            
            System.out.println("\nUsage: java AddPurchase -c <cid>  -b <club>  -t <title> -y <year>  [-w <when>] [-q <qnty> ] [-u <user> ]");
            System.exit(0);
        } else {
            try {
                cid   = new Integer(params.get("c").get(0));
                club  = new String(params.get("b").get(0));
                title = new String(params.get("t").get(0));
                year  = new Integer(params.get("y").get(0));

            
                if (params.get("w")!=null ){
                DateFormat df = new SimpleDateFormat("yyyy-mm-dd");
                df.setLenient(false);
                df.parse(params.get("w").get(0));
                when  = new String(params.get("w").get(0));}
                if (params.get("q")!=null){
                qnty  = new Integer(params.get("q").get(0));}
                if (params.get("u")!=null){
                user  = new String(params.get("u").get(0));}
            }
            catch (NumberFormatException | ParseException e) {
                System.out.println("\nUsage: java AddPurchase -c <cid>  -b <club>  -t <title> -y <year>  [-w <when>] [-q <qnty> ] [-u <user> ]");
                System.out.println("Provide an INT for the cid.");
                System.out.println("Provide a STRING for the club.");
                System.out.println("Provide a STRING for the tile.");
                System.out.println("Provide an INT for the year.");
                System.out.println("Provide a STRING for the user.");
                System.out.println("Provide a INT for the qnty.");
                System.out.println("Provide a STRING for when in format 'yyyy-MM-dd' ");
                System.exit(0);
            }
        }

        // URL: Which database?
        //url = "jdbc:postgresql://db:5432/<dbname>?currentSchema=yrb";
        url = "jdbc:postgresql://db:5432/";

        // set up acct info
        // fetch the PASSWD from <.pgpass>
        Properties props = new Properties();
        try {
            String passwd = PgPass.get("db", "*", user, user);
            props.setProperty("user",    user);
            props.setProperty("password", passwd);
            // props.setProperty("ssl","true"); // NOT SUPPORTED on DB
        } catch(PgPassException e) {
            System.out.print("\nCould not obtain PASSWD from <.pgpass>.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Initialize the connection.
        try {
            // Connect with a fall-thru id & password
            //conDB = DriverManager.getConnection(url,"<username>","<password>");
            conDB = DriverManager.getConnection(url, props);
        } catch(SQLException e) {
            System.out.print("\nSQL: database connection error.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Let's have autocommit turned off.  No particular reason here.
        try {
            conDB.setAutoCommit(false);
        } catch(SQLException e) {
            System.out.print("\nFailed trying to turn autocommit off.\n");
            e.printStackTrace();
            System.exit(0);
        }

        if(qnty<0){
            System.out.println("Quantity is not positive");
            System.out.println("Bye.");
            System.exit(0);
        }

        if (!customerCheck()) {
            System.out.print("There is no customer# ");
            System.out.print(cid);
            System.out.println(" in the database.");
            System.out.println("Bye.");
            System.exit(0);
        }
        if (!clubCheck()) {
            System.out.print("There is no club ");
            System.out.print(club);
            System.out.println(" in the database.");
            System.out.println("Bye.");
            System.exit(0);
        }
        if(!bookCheck()){
            System.out.print("There is no book with ");
            System.out.print(title);
            System.out.print(" and year ");
            System.out.print(year);
            System.out.println(" in the database.");
            System.out.println("Bye.");
            System.exit(0);
        }
        if(!cidclubCheck()){
            System.out.print("There is no customer# ");
            System.out.print(cid);
            System.out.print(" that is member of club ");
            System.out.print(club);
            System.out.println(" in the database.");
            System.out.println("Bye.");
            System.exit(0);
        }
        if(!clubofferbookCheck()){
            System.out.print("There is no club ");
            System.out.print(club);
            System.out.print(" that offer's book with title ");
            System.out.print(title);
            System.out.print(" and year ");
            System.out.print(year);
            System.out.println(" in the database.");
            System.out.println("Bye.");
            System.exit(0);
        }
        if(qnty<0){
            System.out.print("Qnty is not positive");
            System.out.println("Bye.");
            System.exit(0);
        }
    DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    java.util.Date currDate = new java.util.Date(System.currentTimeMillis());

      try {    
      
      java.util.Date date = new SimpleDateFormat("yyyy-MM-dd").parse(when);
      String newstr = new SimpleDateFormat("yyyy-MM-dd").format(date);


      if(newstr.equals(formatter.format(currDate))==false){
      System.out.println("The new purhcase is not made today");
            System.out.println("Bye.");
            System.exit(0);

      } 
      } 
    catch (ParseException e) {
    //Handle exception here
      e.printStackTrace();
     }


        // Add purchase into yrb_purchase.
        Add_Purchase();

        // Commit.  Okay, here nothing to commit really, but why not...
        try {
            conDB.commit();
        } catch(SQLException e) {
            System.out.print("\nFailed trying to commit.\n");
            e.printStackTrace();
            System.exit(0);
        }
        // Close the connection.
        try {
            conDB.close();
        } catch(SQLException e) {
            System.out.print("\nFailed trying to close the connection.\n");
            e.printStackTrace();
            System.exit(0);
        }

    }

    public boolean customerCheck() {
        String            queryText = "";     // The SQL text.
        PreparedStatement querySt   = null;   // The query handle.
        ResultSet         answers   = null;   // A cursor.

        boolean           inDB      = false;  // Return.

        queryText =
                "SELECT name       "
                        + "FROM yrb_customer "
                        + "WHERE cid = ?     ";

        // Prepare the query.
        try {
            querySt = conDB.prepareStatement(queryText);
        } catch(SQLException e) {
            System.out.println("SQL#1 failed in prepare");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Execute the query.
        try {
            querySt.setInt(1, cid.intValue());
            answers = querySt.executeQuery();
        } catch(SQLException e) {
            System.out.println("SQL#1 failed in execute");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Any answer?
        try {
            if (answers.next()) {
                inDB = true;

            } else {
                inDB = false;

            }
        } catch(SQLException e) {
            System.out.println("SQL#1 failed in cursor.");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Close the cursor.
        try {
            answers.close();
        } catch(SQLException e) {
            System.out.print("SQL#1 failed closing cursor.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        // We're done with the handle.
        try {
            querySt.close();
        } catch(SQLException e) {
            System.out.print("SQL#1 failed closing the handle.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        return inDB;
    }

    public boolean clubCheck() {
        String            queryText = "";     // The SQL text.
        PreparedStatement querySt   = null;   // The query handle.
        ResultSet         answers   = null;   // A cursor.

        boolean           inDB      = false;  // Return.

        queryText =
                "SELECT *       "
                        + "FROM yrb_club "
                        + "WHERE club = ?     ";

        // Prepare the query.
        try {
            querySt = conDB.prepareStatement(queryText);
        } catch(SQLException e) {
            System.out.println("SQL#1 failed in prepare");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Execute the query.
        try {
            querySt.setString(1, club.toString());
            answers = querySt.executeQuery();
        } catch(SQLException e) {
            System.out.println("SQL#1 failed in execute");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Any answer?
        try {
            if (answers.next()) {
                inDB = true;

            } else {
                inDB = false;

            }
        } catch(SQLException e) {
            System.out.println("SQL#1 failed in cursor.");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Close the cursor.
        try {
            answers.close();
        } catch(SQLException e) {
            System.out.print("SQL#1 failed closing cursor.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        // We're done with the handle.
        try {
            querySt.close();
        } catch(SQLException e) {
            System.out.print("SQL#1 failed closing the handle.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        return inDB;
    }
    public boolean bookCheck() {
        String            queryText = "";     // The SQL text.
        PreparedStatement querySt   = null;   // The query handle.
        ResultSet         answers   = null;   // A cursor.

        boolean           inDB      = false;  // Return.

        queryText =
                "SELECT *       "
                        + "FROM yrb_book "
                        + "WHERE title = ?     "
                        + "AND year = ?";

        // Prepare the query.
        try {
            querySt = conDB.prepareStatement(queryText);
        } catch(SQLException e) {
            System.out.println("SQL#1 failed in prepare");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Execute the query.
        try {
            querySt.setString(1, title.toString());
            querySt.setInt(2, year.intValue());
            answers = querySt.executeQuery();
        } catch(SQLException e) {
            System.out.println("SQL#1 failed in execute");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Any answer?
        try {
            if (answers.next()) {
                inDB = true;

            } else {
                inDB = false;

            }
        } catch(SQLException e) {
            System.out.println("SQL#1 failed in cursor.");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Close the cursor.
        try {
            answers.close();
        } catch(SQLException e) {
            System.out.print("SQL#1 failed closing cursor.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        // We're done with the handle.
        try {
            querySt.close();
        } catch(SQLException e) {
            System.out.print("SQL#1 failed closing the handle.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        return inDB;
    }
    public boolean cidclubCheck() {
        String            queryText = "";     // The SQL text.
        PreparedStatement querySt   = null;   // The query handle.
        ResultSet         answers   = null;   // A cursor.

        boolean           inDB      = false;  // Return.

        queryText =
                "SELECT *       "
                        + "FROM yrb_member "
                        + "WHERE cid = ?     "
                        + "AND club = ?";

        // Prepare the query.
        try {
            querySt = conDB.prepareStatement(queryText);
        } catch(SQLException e) {
            System.out.println("SQL#1 failed in prepare");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Execute the query.
        try {
            querySt.setInt(1, cid.intValue());
            querySt.setString(2, club.toString());
            answers = querySt.executeQuery();
        } catch(SQLException e) {
            System.out.println("SQL#1 failed in execute");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Any answer?
        try {
            if (answers.next()) {
                inDB = true;

            } else {
                inDB = false;

            }
        } catch(SQLException e) {
            System.out.println("SQL#1 failed in cursor.");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Close the cursor.
        try {
            answers.close();
        } catch(SQLException e) {
            System.out.print("SQL#1 failed closing cursor.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        // We're done with the handle.
        try {
            querySt.close();
        } catch(SQLException e) {
            System.out.print("SQL#1 failed closing the handle.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        return inDB;
    }
    public boolean clubofferbookCheck() {
        String            queryText = "";     // The SQL text.
        PreparedStatement querySt   = null;   // The query handle.
        ResultSet         answers   = null;   // A cursor.

        boolean           inDB      = false;  // Return.

        queryText =
                "SELECT *       "
                        + "FROM yrb_offer "
                        + "WHERE title = ?     "
                        + "AND year = ?"
                        + "AND club = ?";

        // Prepare the query.
        try {
            querySt = conDB.prepareStatement(queryText);
        } catch(SQLException e) {
            System.out.println("SQL#1 failed in prepare");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Execute the query.
        try {
            querySt.setString(1, title.toString());
            querySt.setInt(2, year.intValue());
            querySt.setString(3, club.toString());
            answers = querySt.executeQuery();
        } catch(SQLException e) {
            System.out.println("SQL#1 failed in execute");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Any answer?
        try {
            if (answers.next()) {
                inDB = true;

            } else {
                inDB = false;

            }
        } catch(SQLException e) {
            System.out.println("SQL#1 failed in cursor.");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Close the cursor.
        try {
            answers.close();
        } catch(SQLException e) {
            System.out.print("SQL#1 failed closing cursor.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        // We're done with the handle.
        try {
            querySt.close();
        } catch(SQLException e) {
            System.out.print("SQL#1 failed closing the handle.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        return inDB;
    }

    public void Add_Purchase() {
        String            queryText = "";     // The SQL text.
        PreparedStatement querySt   = null;   // The query handle.
        ResultSet         answers   = null;   // A cursor.

        queryText =
                "INSERT INTO yrb_purchase "
                        + "VALUES (?, ?, ?, ?, ?, ?)";
        // Prepare the query.
        try {
            querySt = conDB.prepareStatement(queryText);
        } catch(SQLException e) {
            System.out.println("SQL#2 failed in prepare");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Execute the query.
        try {
            querySt.setInt(1, cid.intValue());
            querySt.setString(2, club.toString());
            querySt.setString(3, title.toString());
            querySt.setInt(4, year.intValue());
            if (when.matches("\\d{4}-\\d{2}-\\d{2}")) {
            java.util.Date date = new SimpleDateFormat("yyyy-MM-dd").parse(when);
            Timestamp timestamp = new Timestamp(date.getTime());
            querySt.setTimestamp(5, timestamp);}
            else{
            java.util.Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(when);
            Timestamp timestamp = new Timestamp(date.getTime());
            querySt.setTimestamp(5, timestamp);
            }
            
            querySt.setInt(6, qnty.intValue());
            querySt.executeUpdate();
        } catch(SQLException | ParseException e) {
            System.out.println("SQL#2 failed in execute");
            System.out.println(e.toString());
            System.exit(0);
        }


        
        // We're done with the handle.
        try {
            querySt.close();
        } catch(SQLException e) {
            System.out.print("SQL#2 failed closing the handle.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

    }

    public static void main(String[] args) {
        AddPurchase ct = new AddPurchase(args);
    }
}