// Code by Sammy Pandey ---------------------------------------------
// Purpose: So our parser can open a database connection without repeating connection code everywhere
// What this does: Returns a Connection to sentence_builder
package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Db {
    // TODO: change username/password for my machine
    private static final String URL  = "jdbc:mysql://localhost:3306/sentence_builder";
    private static final String USER = "root";
    private static final String PASS = "password";

    private Db() {}

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
// -----------------------------------------------------------