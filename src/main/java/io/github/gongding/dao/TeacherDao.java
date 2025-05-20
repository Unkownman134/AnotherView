package io.github.gongding.dao;

import io.github.gongding.entity.TeacherEntity;
import io.github.gongding.util.DBUtils;

import java.sql.*;
import java.util.Calendar;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TeacherDao {
    private static final Logger logger = LoggerFactory.getLogger(TeacherDao.class);

    /**
     * 根据教师姓名获取教师实体
     *
     * @param name 教师姓名
     * @return 教师实体，如果未找到则返回null
     */
    public TeacherEntity getTeacherByTeacherName(String name) {
        logger.debug("尝试根据教师姓名 '{}' 获取教师实体。", name);
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        TeacherEntity teacher = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT * FROM teacher WHERE name = ?";
            logger.debug("执行 SQL: {} with name = '{}'", sql, name);
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, name);

            rs = pstmt.executeQuery();
            if (rs.next()) {
                teacher = buildTeacherEntity(rs); // 使用辅助方法构建实体
                logger.debug("成功找到教师姓名 '{}' 的实体，ID: {}", name, teacher.getId());
            } else {
                logger.debug("未找到教师姓名 '{}' 的实体。", name);
            }
        } catch (SQLException e) {
            logger.error("根据教师姓名 '{}' 获取教师实体时发生数据库异常。", name, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成根据教师姓名 '{}' 获取教师实体操作。", name);
        return teacher;
    }

    /**
     * 添加新教师
     *
     * @param teacher 教师实体
     * @return 是否成功添加
     */
    public boolean addTeacher(TeacherEntity teacher) {
        logger.info("尝试添加新教师，姓名: {}", teacher.getName());
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DBUtils.getConnection();
            String sql = "INSERT INTO teacher (name, email, password_salt, password_hash) VALUES(?,?,?,?)";
            logger.debug("执行 SQL (添加教师): {} with name = '{}'", sql, teacher.getName());
            // 注意：不要在日志中记录密码或盐
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, teacher.getName());
            pstmt.setString(2, teacher.getEmail());
            pstmt.setString(3, teacher.getPasswordSalt());
            pstmt.setString(4, teacher.getPasswordHash());

            int affectedRows = pstmt.executeUpdate();
            success = affectedRows > 0;
            logger.debug("添加教师影响行数: {}", affectedRows);

            if (success) {
                logger.info("成功添加新教师，姓名: {}", teacher.getName());
            } else {
                logger.warn("添加新教师失败，姓名: {}，可能数据库操作未成功。", teacher.getName());
            }

        } catch (SQLException e) {
            logger.error("添加新教师时发生数据库异常，姓名: {}", teacher.getName(), e);
        } finally {
            DBUtils.close(conn, pstmt);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成添加新教师操作，姓名: {}，结果: {}", teacher.getName(), success ? "成功" : "失败");
        return success;
    }

    /**
     * 更新教师的最后登录时间
     *
     * @param teacherName 教师姓名
     * @return 如果更新成功，返回true，否则返回false
     */
    public boolean updateTeacherLoginTime(String teacherName) {
        logger.debug("尝试更新教师 '{}' 的最后登录时间。", teacherName);
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DBUtils.getConnection();
            String sql = "UPDATE teacher SET last_login = NOW() WHERE name = ?";
            logger.debug("执行 SQL (更新最后登录时间): {} with name = '{}'", sql, teacherName);
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, teacherName);
            int rowsAffected = pstmt.executeUpdate();
            success = rowsAffected > 0;
            logger.debug("更新最后登录时间影响行数: {}", rowsAffected);

            if (success) {
                logger.debug("成功更新教师 '{}' 的最后登录时间。", teacherName);
            } else {
                logger.warn("更新教师 '{}' 的最后登录时间失败，可能该教师不存在。", teacherName);
            }

        } catch (SQLException e) {
            logger.error("更新教师 '{}' 的最后登录时间时发生数据库异常。", teacherName, e);
        } finally {
            DBUtils.close(conn, pstmt);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成更新教师 '{}' 最后登录时间操作，结果: {}", teacherName, success ? "成功" : "失败");
        return success;
    }

    /**
     * 根据教师ID获取教师实体
     * @param id 教师ID
     * @return 教师实体，如果未找到则返回null
     */
    public TeacherEntity getTeacherById(int id) {
        logger.debug("尝试根据教师ID {} 获取教师实体。", id);
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        TeacherEntity teacher = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT * FROM teacher WHERE teacher_id = ?";
            logger.debug("执行 SQL: {} with id = {}", sql, id);
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                teacher = buildTeacherEntity(rs);
                logger.debug("成功找到教师 ID {} 的实体，姓名: {}", id, teacher.getName());
            } else {
                logger.debug("未找到教师 ID {} 的实体。", id);
            }
        } catch (SQLException e) {
            logger.error("根据教师ID {} 获取教师实体时发生数据库异常。", id, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成根据教师ID {} 获取教师实体操作。", id);
        return teacher;
    }

    /**
     * 辅助方法：从数据库查询结果集中构建TeacherEntity对象。
     *
     * @param rs 包含教师数据的数据库查询结果集。
     * @return 一个填充了从结果集获取的数据的TeacherEntity对象。
     * @throws SQLException 如果在访问结果集时发生数据库访问错误。
     */
    private TeacherEntity buildTeacherEntity(ResultSet rs) throws SQLException {
        logger.trace("从 ResultSet 构建 TeacherEntity 对象。");
        TeacherEntity teacher = new TeacherEntity();
        try {
            Calendar shanghaiCalendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));

            teacher.setId(rs.getInt("teacher_id"));
            teacher.setName(rs.getString("name"));
            teacher.setEmail(rs.getString("email"));
            teacher.setPasswordSalt(rs.getString("password_salt"));
            teacher.setPasswordHash(rs.getString("password_hash"));

            Timestamp lastLoginTs = rs.getTimestamp("last_login", shanghaiCalendar);
            Timestamp createdAtTs = rs.getTimestamp("created_at", shanghaiCalendar);

            teacher.setLastLogin(lastLoginTs != null ? lastLoginTs.toLocalDateTime() : null);
            teacher.setCreatedAt(createdAtTs != null ? createdAtTs.toLocalDateTime() : null);
            logger.trace("成功构建 TeacherEntity: ID = {}, Name = {}", teacher.getId(), teacher.getName());
        } catch (SQLException e) {
            logger.error("从 ResultSet 构建 TeacherEntity 时发生 SQLException。", e);
            throw e;
        }
        return teacher;
    }
}
