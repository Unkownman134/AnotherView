package io.github.gongding.dao;

import io.github.gongding.entity.TeacherEntity;
import io.github.gongding.util.DBUtils;

import java.sql.*;
import java.util.Calendar;
import java.util.TimeZone;

public class TeacherDao {
    /**
     * 根据教师姓名获取教师实体
     *
     * @param name 教师姓名
     * @return 教师实体，如果未找到则返回null
     */
    public TeacherEntity getTeacherByTeacherName(String name) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        TeacherEntity teacher = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT * FROM teacher WHERE name = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, name);

            rs = pstmt.executeQuery();
            if (rs.next()) {
                teacher = new TeacherEntity();

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
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt, rs);
        }
        return teacher;
    }

    /**
     * 添加新教师
     *
     * @param teacher 教师实体
     * @return 是否成功添加
     */
    public boolean addTeacher(TeacherEntity teacher) {
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "INSERT INTO teacher (name, email, password_salt, password_hash) VALUES(?,?,?,?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, teacher.getName());
            pstmt.setString(2, teacher.getEmail());
            pstmt.setString(3, teacher.getPasswordSalt());
            pstmt.setString(4, teacher.getPasswordHash());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtils.close(conn, pstmt);
        }
    }

    /**
     * 更新教师的最后登录时间
     *
     * @param teacherName 教师姓名
     * @return 如果更新成功，返回true，否则返回false
     */
    public boolean updateTeacherLoginTime(String teacherName) {
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "UPDATE teacher SET last_login = NOW() WHERE name = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, teacherName);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtils.close(conn, pstmt);
        }
    }
}