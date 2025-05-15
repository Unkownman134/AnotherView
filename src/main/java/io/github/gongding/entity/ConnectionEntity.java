package io.github.gongding.entity;

import java.sql.Connection;

/**
 * 封装连接和使用时间，用来监测是否超时
 */
public class ConnectionEntity {
    Connection connection;
    private Long useStartTime;

    public ConnectionEntity(Connection connection, Long useStartTime) {
        this.connection = connection;
        this.useStartTime = useStartTime;
    }

    public ConnectionEntity() {
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Long getUseStartTime() {
        return useStartTime;
    }

    public void setUseStartTime(Long useStartTime) {
        this.useStartTime = useStartTime;
    }
}
