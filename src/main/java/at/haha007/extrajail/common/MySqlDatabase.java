package at.haha007.extrajail.common;

import lombok.SneakyThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MySqlDatabase {


    private final String host, username, password, database, datasource;
    private final boolean useSSL;
    private Connection connection;

    public MySqlDatabase(String host, String username, String password, String database, String datasource, boolean useSSL) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.database = database;
        this.useSSL = useSSL;
        this.datasource = datasource;
    }

    @SneakyThrows
    public void connect()  {
        try {
            Class.forName(datasource);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }
        connection = DriverManager.getConnection(String.format("jdbc:mysql://%s/%s?user=%s&password=%s&useSSL=%b&autoReconnect=yes", host, database, username, password, useSSL));
    }

    public void disconnect() throws SQLException {
        if (isConnected()) connection.close();
    }

    public boolean isConnected() throws SQLException {
        return connection != null && !connection.isClosed();
    }

    public PreparedStatement prepareStatement(String statement) throws SQLException {
        return connection.prepareStatement(statement);
    }

}
