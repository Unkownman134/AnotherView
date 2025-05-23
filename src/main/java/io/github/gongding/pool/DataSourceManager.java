package io.github.gongding.pool;

import io.github.gongding.pool.config.DataSourceConfig;

import java.sql.Connection;

public class DataSourceManager {
    static DataSourceConfig dataSourceConfig = new DataSourceConfig();
    static ConnectionPool connectionPool = new ConnectionPool(dataSourceConfig);

    public static Connection getConn() {
        return connectionPool.getConn();
    }

    public static void close(Connection connection) {
        connectionPool.releaseConn(connection);
    }
}
