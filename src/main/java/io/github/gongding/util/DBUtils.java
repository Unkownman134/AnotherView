package io.github.gongding.util;

import io.github.gongding.pool.DataSourceManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBUtils {
    /**
     * 获取数据库连接
     * @return 一个数据库连接对象
     */
    public static Connection getConnection() {
        return DataSourceManager.getConn();
    }

    /**
     * 关闭数据库资源
     * @param conn 要关闭的数据库连接
     * @param stmt 要关闭的Statement对象
     * @param rs 要关闭的ResultSet对象
     */
    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (stmt != null) stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (conn != null) {
            DataSourceManager.close(conn);
        }
    }

    /**
     * 提供一个重载方法，方便只关闭Connection和Statement的情况
     * @param conn 要关闭的数据库连接
     * @param stmt 要关闭的Statement对象
     */
    public static void close(Connection conn, Statement stmt) {
        close(conn, stmt, null);
    }
}
