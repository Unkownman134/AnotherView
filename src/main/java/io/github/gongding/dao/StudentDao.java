package io.github.gongding.dao;

import io.github.gongding.entity.StudentEntity;
import io.github.gongding.util.DBUtils;

import java.sql.*;
import java.util.Calendar;
import java.util.TimeZone;

public class StudentDao {
    /**
     * 根据学号查询学生信息
     * @param studentNumber 学生学号
     * @return 如果找到返回StudentEntity对象，否则返回null
     */
    public StudentEntity getStudentByStudentNumber(String studentNumber) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        StudentEntity student = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT * FROM student WHERE student_number = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentNumber);

            rs = pstmt.executeQuery();
            if (rs.next()) {
                student = new StudentEntity();

                //创建一个上海时区的Calendar实例，用于处理时间戳的时区转换
                Calendar shanghaiCalendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
                //从结果集中获取时间戳，并使用指定的时区进行处理
                Timestamp lastLoginTs = rs.getTimestamp("last_login", shanghaiCalendar);
                Timestamp createdAtTs = rs.getTimestamp("created_at", shanghaiCalendar);

                student.setId(rs.getInt("student_id"));
                student.setStudentNumber(rs.getString("student_number"));
                student.setName(rs.getString("name"));
                student.setEmail(rs.getString("email"));
                student.setSchool(rs.getString("school"));
                student.setClassof(rs.getString("classof"));
                student.setPasswordSalt(rs.getString("password_salt"));
                student.setPasswordHash(rs.getString("password_hash"));
                //将Timestamp转换为LocalDateTime，如果时间戳为null则设置为null
                student.setLastLogin(lastLoginTs != null ? lastLoginTs.toLocalDateTime() : null);
                student.setCreatedAt(createdAtTs != null ? createdAtTs.toLocalDateTime() : null);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt, rs);
        }
        return student;
    }

    /**
     * 添加学生信息
     * @param student 要添加的StudentEntity对象
     * @return 如果添加成功，返回true；否则返回false
     */
    public boolean addStudent(StudentEntity student) {
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "INSERT INTO student (student_number, name, email, school, classof, password_salt, password_hash) VALUES(?,?,?,?,?,?,?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, student.getStudentNumber());
            pstmt.setString(2, student.getName());
            pstmt.setString(3, student.getEmail());
            pstmt.setString(4, student.getSchool());
            pstmt.setString(5, student.getClassof());
            pstmt.setString(6, student.getPasswordSalt());
            pstmt.setString(7, student.getPasswordHash());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt);
        }
        return false;
    }
}
