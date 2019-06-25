package etu.wollen.vk.database;

import org.sqlite.Function;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class DatabaseWrapper {
    private static volatile DatabaseWrapper instance;

    private Connection conn;
    private String databaseFilename;

    public static DatabaseWrapper getInstance() {
        DatabaseWrapper localInstance = instance;
        if (localInstance == null) {
            synchronized (DatabaseWrapper.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new DatabaseWrapper();
                }
            }
        }
        return localInstance;
    }

    private DatabaseWrapper(){}

    private Connection connect() throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFilename);

        // Create regexp() function to make the REGEXP operator available
        Function.create(connection, "REGEXP", new Function() {
            @Override
            protected void xFunc() throws SQLException {
                String expression = value_text(0);
                String value = value_text(1);
                if (value == null)
                    value = "";

                Pattern pattern=Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                result(pattern.matcher(value).find() ? 1 : 0);
            }
        });
        return connection;
    }

    Connection getConnection() throws SQLException {
        if (conn == null) {
            try {
                conn = connect();
                System.out.println("Database connected: " + databaseFilename);
            }
            catch (ClassNotFoundException e){
                throw new SQLException(e);
            }
        }
        return conn;
    }

    void closeConnection() throws SQLException {
        if(conn != null) {
            conn.close();
            System.out.println("Database closed: " + databaseFilename);
        }
    }

    public void setDatabaseFilename(String databaseFilename) {
        this.databaseFilename = databaseFilename;
    }
}
