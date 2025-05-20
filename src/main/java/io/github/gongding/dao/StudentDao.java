package io.github.gongding.dao;

import io.github.gongding.entity.LessonEntity;
import io.github.gongding.entity.StudentEntity;
import io.github.gongding.util.DBUtils;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StudentDao {
    private static final Logger logger = LoggerFactory.getLogger(StudentDao.class);

    /**
     * 根据学号查询学生信息
     * @param studentNumber 学生学号
     * @return 如果找到返回StudentEntity对象，否则返回null
     */
    public StudentEntity getStudentByStudentNumber(String studentNumber) {
        logger.debug("尝试根据学号 {} 查询学生信息。", studentNumber);
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        StudentEntity student = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT * FROM student WHERE student_number = ?";
            logger.debug("执行 SQL: {} with studentNumber = {}", sql, studentNumber);
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

                logger.debug("成功找到学号 {} 的学生信息，ID: {}, 姓名: {}", studentNumber, student.getId(), student.getName());
            } else {
                logger.debug("未找到学号 {} 的学生信息。", studentNumber);
            }
        } catch (SQLException e) {
            logger.error("根据学号 {} 查询学生信息时发生数据库异常。", studentNumber, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成根据学号 {} 查询学生信息操作。", studentNumber);
        return student;
    }

    /**
     * 添加学生信息
     * @param student 要添加的StudentEntity对象
     * @return 如果添加成功，返回true；否则返回false
     */
    public boolean addStudent(StudentEntity student) {
        logger.info("尝试添加学生信息，学号: {}", student.getStudentNumber());
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DBUtils.getConnection();
            String sql = "INSERT INTO student (student_number, name, email, school, classof, password_salt, password_hash) VALUES(?,?,?,?,?,?,?)";
            logger.debug("执行 SQL (添加学生): {} with studentNumber = {}", sql, student.getStudentNumber());
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, student.getStudentNumber());
            pstmt.setString(2, student.getName());
            pstmt.setString(3, student.getEmail());
            pstmt.setString(4, student.getSchool());
            pstmt.setString(5, student.getClassof());
            pstmt.setString(6, student.getPasswordSalt());
            pstmt.setString(7, student.getPasswordHash());

            int affectedRows = pstmt.executeUpdate();
            success = affectedRows > 0;
            logger.debug("添加学生信息影响行数: {}", affectedRows);

            if (success) {
                logger.info("成功添加学生信息，学号: {}", student.getStudentNumber());
            } else {
                logger.warn("添加学生信息失败，学号: {}，可能数据库操作未成功。", student.getStudentNumber());
            }

        } catch (SQLException e) {
            logger.error("添加学生信息时发生数据库异常，学号: {}", student.getStudentNumber(), e);
        } finally {
            DBUtils.close(conn, pstmt);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成添加学生信息操作，学号: {}，结果: {}", student.getStudentNumber(), success ? "成功" : "失败");
        return success;
    }

    /**
     * 更新学生的最后登录时间
     * @param studentNumber 学生学号
     * @return 如果更新成功，返回 true，否则返回 false
     */
    public boolean updateStudentLoginTime(String studentNumber) {
        logger.debug("尝试更新学号 {} 的最后登录时间。", studentNumber);
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DBUtils.getConnection();
            String sql = "UPDATE student SET last_login = NOW() WHERE student_number = ?";
            logger.debug("执行 SQL (更新最后登录时间): {} with studentNumber = {}", sql, studentNumber);
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentNumber);
            int affectedRows = pstmt.executeUpdate();
            success = affectedRows > 0;
            logger.debug("更新最后登录时间影响行数: {}", affectedRows);

            if (success) {
                logger.debug("成功更新学号 {} 的最后登录时间。", studentNumber);
            } else {
                logger.warn("更新学号 {} 的最后登录时间失败，可能该学生不存在。", studentNumber);
            }

        } catch (SQLException e) {
            logger.error("更新学号 {} 的最后登录时间时发生数据库异常。", studentNumber, e);
        } finally {
            DBUtils.close(conn, pstmt);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成更新学号 {} 最后登录时间操作，结果: {}", studentNumber, success ? "成功" : "失败");
        return success;
    }

    /**
     * 根据学生的学号查询该学生参与的所有课程列表。
     *
     * @param studentNumber 要查询课程列表的学生的学号。
     * @return 一个LessonEntity对象的列表，包含该学生参与的所有课程。如果学生没有关联的课程或发生SQL异常，返回空列表。
     */
    public List<LessonEntity> getStudentLessons(String studentNumber) {
        logger.debug("尝试根据学号 {} 查询参与的所有课程列表。", studentNumber);
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<LessonEntity> lessons = new ArrayList<>();

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT l.lesson_id AS id, l.title " +
                    "FROM lesson l " +
                    "JOIN lesson_student ls ON l.lesson_id = ls.lesson_id " +
                    "JOIN student s ON ls.student_id = s.student_id " +
                    "WHERE s.student_number = ?";
            logger.debug("执行 SQL: {} with studentNumber = {}", sql, studentNumber);
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentNumber);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                LessonEntity lesson = new LessonEntity();
                lesson.setId(rs.getInt("id"));
                lesson.setTitle(rs.getString("title"));
                lessons.add(lesson);
                logger.trace("找到学生 {} 参与的课程: ID = {}, Title = {}", studentNumber, lesson.getId(), lesson.getTitle());
            }
            logger.debug("成功找到学生 {} 参与的 {} 个课程。", studentNumber, lessons.size());
        } catch (SQLException e) {
            logger.error("根据学号 {} 查询学生课程列表时发生数据库异常。", studentNumber, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成根据学号 {} 查询学生课程列表操作。", studentNumber);
        return lessons;
    }

    /**
     * 根据班级ID列表获取学生列表
     * @param classIds 班级ID列表
     * @return 学生实体列表
     */
    public List<StudentEntity> getStudentsByClassIds(List<Integer> classIds) {
        logger.debug("尝试根据班级ID列表 {} 获取学生列表。", classIds);
        List<StudentEntity> students = new ArrayList<>();
        if (classIds == null || classIds.isEmpty()) {
            logger.debug("班级ID列表为空或为null，返回空学生列表。");
            return students;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT s.student_id, s.student_number, s.name, s.classof " +
                    "FROM student s JOIN class c ON s.classof = c.name " +
                    "WHERE c.class_id IN (";
            for (int i = 0; i < classIds.size(); i++) {
                sql += "?";
                if (i < classIds.size() - 1) {
                    sql += ",";
                }
            }
            sql += ")";
            logger.debug("执行 SQL: {} with classIds = {}", sql, classIds);

            pstmt = conn.prepareStatement(sql);
            for (int i = 0; i < classIds.size(); i++) {
                pstmt.setInt(i + 1, classIds.get(i));
            }

            rs = pstmt.executeQuery();
            while (rs.next()) {
                StudentEntity student = new StudentEntity();
                student.setId(rs.getInt("student_id"));
                student.setStudentNumber(rs.getString("student_number"));
                student.setName(rs.getString("name"));
                student.setClassof(rs.getString("classof"));
                students.add(student);
                logger.trace("找到班级关联学生: ID = {}, 学号 = {}, 姓名 = {}", student.getId(), student.getStudentNumber(), student.getName());
            }
            logger.debug("成功找到 {} 个与班级 ID 列表 {} 关联的学生。", students.size(), classIds);
        } catch (SQLException e) {
            logger.error("根据班级ID列表 {} 获取学生列表时发生数据库异常。", classIds, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成根据班级ID列表 {} 获取学生列表操作。", classIds);
        return students;
    }
}
