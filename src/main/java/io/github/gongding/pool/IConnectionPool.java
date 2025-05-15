package io.github.gongding.pool;

import java.sql.Connection;

public interface IConnectionPool {
    Connection getConn();

    void releaseConn(Connection conn);
}
