package io.github.gongding.dao;

import io.github.gongding.entity.AdminEntity;
import io.github.gongding.util.DBUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Calendar;
import java.util.TimeZone;

public class AdminDao {
    private static final Logger logger = LoggerFactory.getLogger(AdminDao.class);

    /**
     * 根据管理员姓名获取管理员实体
     *
     * @param name 管理员姓名
     * @return 管理员实体，如果未找到则返回null
     */
    public AdminEntity getAdminByName(String name) {
        logger.debug("尝试根据管理员姓名 '{}' 获取管理员实体。", name);
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        AdminEntity admin = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT * FROM admin WHERE name = ?";
            logger.debug("执行 SQL: {} with name = '{}'", sql, name);
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, name);

            rs = pstmt.executeQuery();
            if (rs.next()) {
                admin = buildAdminEntity(rs);
                logger.debug("成功找到管理员姓名 '{}' 的实体，ID: {}", name, admin.getId());
            } else {
                logger.debug("未找到管理员姓名 '{}' 的实体。", name);
            }
        } catch (SQLException e) {
            logger.error("根据管理员姓名 '{}' 获取管理员实体时发生数据库异常。", name, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成根据管理员姓名 '{}' 获取管理员实体操作。", name);
        return admin;
    }

    /**
     * 更新管理员的最后登录时间
     *
     * @param adminName 管理员姓名
     * @return 如果更新成功，返回true，否则返回false
     */
    public boolean updateAdminLoginTime(String adminName) {
        logger.debug("尝试更新管理员 '{}' 的最后登录时间。", adminName);
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DBUtils.getConnection();
            String sql = "UPDATE admin SET last_login = NOW() WHERE name = ?";
            logger.debug("执行 SQL (更新最后登录时间): {} with name = '{}'", sql, adminName);
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, adminName);
            int rowsAffected = pstmt.executeUpdate();
            success = rowsAffected > 0;
            logger.debug("更新最后登录时间影响行数: {}", rowsAffected);

            if (success) {
                logger.debug("成功更新管理员 '{}' 的最后登录时间。", adminName);
            } else {
                logger.warn("更新管理员 '{}' 的最后登录时间失败，可能该管理员不存在。", adminName);
            }

        } catch (SQLException e) {
            logger.error("更新教师 '{}' 的最后登录时间时发生数据库异常。", adminName, e);
        } finally {
            DBUtils.close(conn, pstmt);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成更新管理员 '{}' 最后登录时间操作，结果: {}", adminName, success ? "成功" : "失败");
        return success;
    }

    /**
     * 辅助方法：从数据库查询结果集中构建AdminEntity对象。
     *
     * @param rs 包含教师数据的数据库查询结果集。
     * @return 一个填充了从结果集获取的数据的AdminEntity对象。
     * @throws SQLException 如果在访问结果集时发生数据库访问错误。
     */
    private AdminEntity buildAdminEntity(ResultSet rs) throws SQLException {
        logger.trace("从 ResultSet 构建 AdminEntity 对象。");
        AdminEntity admin = new AdminEntity();
        try {
            Calendar shanghaiCalendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));

            admin.setId(rs.getInt("admin_id"));
            admin.setName(rs.getString("name"));
            admin.setEmail(rs.getString("email"));
            admin.setPasswordSalt(rs.getString("password_salt"));
            admin.setPasswordHash(rs.getString("password_hash"));

            Timestamp lastLoginTs = rs.getTimestamp("last_login", shanghaiCalendar);
            Timestamp createdAtTs = rs.getTimestamp("create_at", shanghaiCalendar);

            admin.setLastLogin(lastLoginTs != null ? lastLoginTs.toLocalDateTime() : null);
            admin.setCreatedAt(createdAtTs != null ? createdAtTs.toLocalDateTime() : null);
            logger.trace("成功构建 AdminEntity: ID = {}, Name = {}", admin.getId(), admin.getName());
        } catch (SQLException e) {
            logger.error("从 ResultSet 构建 AdminEntity 时发生 SQLException。", e);
            throw e;
        }
        return admin;
    }
}
