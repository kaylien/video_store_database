import java.util.Properties;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.io.FileInputStream;

/**
 * Runs queries against a back-end database
 */
public class Query {
    private static Properties configProps = new Properties();

    private static String imdbUrl;
    private static String customerUrl;

    private static String postgreSQLDriver;
    private static String postgreSQLUser;
    private static String postgreSQLPassword;

    // DB Connection
    private Connection _imdb;
    private Connection _customer_db;

    // Canned queries

    // added the LOWER to the sql query to make it case insensitive
    private String _search_sql = 
                      "SELECT m.*"
                    + " FROM movie m"
                    + " WHERE LOWER(name) LIKE LOWER(?)"
                    + " ORDER BY m.id";
    private PreparedStatement _search_statement;


    private String _director_mid_sql = 
                       "SELECT y.* "
                     + "FROM movie_directors x, directors y "
                     + "WHERE x.mid = ? and x.did = y.id";
    private PreparedStatement _director_mid_statement;

    private String _actors_mid_sql = 
                      "SELECT a.id, a.fname, a.lname"
                    + " FROM casts c, actor a"
                    + " WHERE c.mid = ? AND a.id = c.pid";
    private PreparedStatement _actors_mid_statement;

    
    private String _customer_renting_sql = 
                    "SELECT rentedBy"
                    + " FROM rental"
                    + " WHERE forMovie = ? AND dateReturned IS NULL";
    private PreparedStatement _customer_renting_statement;

    private String _rent_movie_sql = "INSERT INTO Rental(dateRented, dateReturned, rentedBy, forMovie) VALUES (current_date, NULL, ?, ?)";
    private PreparedStatement _rent_movie_statement;

    //if the cid is equal to the logged in user, make YOU ARE CURRENTLY RENTING
    //if the cid is existing and return date null, make UNAVAILABLE
    //if there is no return date null for the movie, make AVAILABLE

    //this is assuming that we only have one copy of a movie that can be out at a time
    //hence there can only be one result, and one cid returned

    /* uncomment, and edit, after your create your own customer database */

    private String _customer_renting_how_many_sql = 
    "SELECT COUNT(*) AS count, p.maxRentals"
    + " FROM Rental r, Customer c, Plan p"
    + " WHERE r.rentedBy = c.id AND p.pName = c.pname"
    + " AND rentedBy = ? AND dateReturned IS NULL"
    + " GROUP BY p.maxRentals";
    private PreparedStatement _customer_renting_how_many_statement;


    
    private String _customer_login_sql = "SELECT * FROM customer WHERE username = ? and password = ?";
    private PreparedStatement _customer_login_statement;


    private String _begin_transaction_read_write_sql = "BEGIN TRANSACTION READ WRITE";
    private PreparedStatement _begin_transaction_read_write_statement;

    private String _commit_transaction_sql = "COMMIT TRANSACTION";
    private PreparedStatement _commit_transaction_statement;

    private String _rollback_transaction_sql = "ROLLBACK TRANSACTION";
    private PreparedStatement _rollback_transaction_statement;



    public Query() {
    }

    /**********************************************************/
    /* Connections to postgres databases */

    public void openConnection() throws Exception {
        configProps.load(new FileInputStream("dbconn.config"));
        
        
        imdbUrl        = configProps.getProperty("imdbUrl");
        customerUrl    = configProps.getProperty("customerUrl");
        postgreSQLDriver   = configProps.getProperty("postgreSQLDriver");
        postgreSQLUser     = configProps.getProperty("postgreSQLUser");
        postgreSQLPassword = configProps.getProperty("postgreSQLPassword");


        /* load jdbc drivers */
        Class.forName(postgreSQLDriver).newInstance();

        /* open connections to TWO databases: imdb and the customer database */
        _imdb = DriverManager.getConnection(imdbUrl, // database
                postgreSQLUser, // user
                postgreSQLPassword); // password

        _customer_db = DriverManager.getConnection(customerUrl, // database
                postgreSQLUser, // user
                postgreSQLPassword); // password
    }

    public void closeConnection() throws Exception {
        _imdb.close();
        _customer_db.close();
    }

    /**********************************************************/
    /* prepare all the SQL statements in this method.
      "preparing" a statement is almost like compiling it.  Note
       that the parameters (with ?) are still not filled in */

    public void prepareStatements() throws Exception {

        _search_statement = _imdb.prepareStatement(_search_sql);
        _director_mid_statement = _imdb.prepareStatement(_director_mid_sql);
        _actors_mid_statement = _imdb.prepareStatement(_actors_mid_sql);

        /* uncomment after you create your customers database */
        
        _customer_login_statement = _customer_db.prepareStatement(_customer_login_sql);
        _begin_transaction_read_write_statement = _customer_db.prepareStatement(_begin_transaction_read_write_sql);
        _commit_transaction_statement = _customer_db.prepareStatement(_commit_transaction_sql);
        _rollback_transaction_statement = _customer_db.prepareStatement(_rollback_transaction_sql);
         

        /* add here more prepare statements for all the other queries you need */
        /* . . . . . . */
        _customer_renting_statement = _customer_db.prepareStatement(_customer_renting_sql);
        _customer_renting_how_many_statement = _customer_db.prepareStatement(_customer_renting_how_many_sql);
        _rent_movie_statement = _customer_db.prepareStatement(_rent_movie_sql);
    }


    /**********************************************************/
    /* suggested helper functions  */

    public int helper_compute_remaining_rentals(int cid) throws Exception {
        /* how many movies can she/he still rent ? */
        /* you have to compute and return the difference between the customer's plan
           and the count of oustanding rentals */

            _customer_renting_how_many_statement.clearParameters();
            _customer_renting_how_many_statement.setInt(1, cid);
            ResultSet customer_rented_set = _customer_renting_how_many_statement.executeQuery();
            int customerIsRenting = 0;
            int maxRentals = 0;
            while (customer_rented_set.next()) {
                customerIsRenting= customer_rented_set.getInt(1);
                maxRentals = customer_rented_set.getInt(2);
        }

            return (maxRentals - customerIsRenting);

    }

    public String helper_compute_customer_name(int cid) throws Exception {
        /* you find  the first + last name of the current customer */
        return ("JoeFirstName" + " " + "JoeLastName");


    }

    public boolean helper_check_plan(int plan_id) throws Exception {
        /* is plan_id a valid plan id ?  you have to figure out */
        return true;
    }

    public boolean helper_check_movie(int mid) throws Exception {
        /* is mid a valid movie id ? you have to figure out  */
        return true;
    }

    private int helper_who_has_this_movie(int mid) throws Exception {
        /* find the customer id (cid) of whoever currently rents the movie mid; return -1 if none */
        
            _customer_renting_statement.clearParameters();
            _customer_renting_statement.setInt(1, mid);

            ResultSet customer_renting_set = _customer_renting_statement.executeQuery();
            int cid = -1;
            while (customer_renting_set.next()) {
                cid = customer_renting_set.getInt(1);
            }

            customer_renting_set.close();
            return cid;

    }

    /**********************************************************/
    /* login transaction: invoked only once, when the app is started  */
    public int transaction_login(String name, String password) throws Exception {
        /* authenticates the user, and returns the user id, or -1 if authentication fails */

        /* Uncomment after you create your own customers database */
        
        int cid;

        _customer_login_statement.clearParameters();
        _customer_login_statement.setString(1,name);
        _customer_login_statement.setString(2,password);
        ResultSet cid_set = _customer_login_statement.executeQuery();
        if (cid_set.next()) cid = cid_set.getInt(1);
        else cid = -1;
        return(cid);
         
        //return (55);
    }

    public void transaction_personal_data(int cid) throws Exception {
        /* println the customer's personal data: name, and plan number */

    }


    /**********************************************************/
    /* main functions in this project: */

    public void transaction_search(int cid, String movie_title)
            throws Exception {
        /* searches for movies with matching titles: SELECT * FROM movie WHERE name LIKE movie_title */
        /* prints the movies, directors, actors, and the availability status:
           AVAILABLE, or UNAVAILABLE, or YOU CURRENTLY RENT IT */

        /* set the first (and single) '?' parameter */
        _search_statement.clearParameters();
        _search_statement.setString(1, '%' + movie_title + '%');

        ResultSet movie_set = _search_statement.executeQuery();
        while (movie_set.next()) {
            int mid = movie_set.getInt(1);
            System.out.println("ID: " + mid + " NAME: "
                    + movie_set.getString(2) + " YEAR: "
                    + movie_set.getString(3));
            /* do a dependent join with directors */
            _director_mid_statement.clearParameters();
            _director_mid_statement.setInt(1, mid);
            ResultSet director_set = _director_mid_statement.executeQuery();
            while (director_set.next()) {
                System.out.println("\t\tDirector: " + director_set.getString(3)
                        + " " + director_set.getString(2));
            }
            director_set.close();
            /* now you need to retrieve the actors, in the same manner */
            /* then you have to find the status: of "AVAILABLE" "YOU HAVE IT", "UNAVAILABLE" */
            _actors_mid_statement.clearParameters();
            _actors_mid_statement.setInt(1, mid);
            ResultSet actor_set = _actors_mid_statement.executeQuery();
            while (actor_set.next()) {
                System.out.println("\t\tActress/Actor: " + actor_set.getString(3)
                        + " " + actor_set.getString(2)); 
            }
            actor_set.close();
            // renting
            int renter = helper_who_has_this_movie(mid);

            if (renter == cid) {
                System.out.println("YOU'RE RENTING THIS"); 
            }
            else if (renter != cid) {
                System.out.println("UNAVAILABLE");
            }

            else if (renter == -1) {
                System.out.println("AVAILABLE");
            }


        }
        System.out.println();
    }

    public void transaction_choose_plan(int cid, int pid) throws Exception {
        /* updates the customer's plan to pid: UPDATE customers SET plid = pid */
        /* remember to enforce consistency ! */



    }

    public void transaction_list_plans() throws Exception {
        /* println all available plans: SELECT * FROM plan */
    }
    
    public void transaction_list_user_rentals(int cid) throws Exception {
        /* println all movies rented by the current user*/
    }

    public void transaction_rent(int cid, int mid) throws Exception {
        /* rend the movie mid to the customer cid */
        /* remember to enforce consistency ! */

        _rent_movie_statement.clearParameters();
        _rent_movie_statement.setInt(1, cid);
        _rent_movie_statement.setInt(2, mid);
        _rent_movie_statement.executeUpdate();

        System.out.println("Successfully rented");


    }

    public void transaction_return(int cid, int mid) throws Exception {
        /* return the movie mid by the customer cid */
    }

    public void transaction_fast_search(int cid, String movie_title)
            throws Exception {
        /* like transaction_search, but uses joins instead of independent joins
           Needs to run three SQL queries: (a) movies, (b) movies join directors, (c) movies join actors
           Answers are sorted by mid.
           Then merge-joins the three answer sets */

        _search_statement.clearParameters();
        _search_statement.setString(1, '%' + movie_title + '%');

        ResultSet fast_movie_set = _search_statement.executeQuery();

    }

}
