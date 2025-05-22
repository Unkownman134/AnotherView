package io.github.gongding.dao;

import io.github.gongding.entity.LessonEntity;
import io.github.gongding.util.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LessonDao {
    private static final Logger logger = LoggerFactory.getLogger(LessonDao.class);

    /**
     * 根据课程ID查询课程信息
     * @param lessonId 课程的唯一标识符ID
     * @return 如果找到，返回LessonEntity对象；否则返回null
     */
    public LessonEntity getLessonById(int lessonId) {
        logger.debug("尝试根据课程ID {} 查询课程信息。", lessonId);
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        LessonEntity lesson = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT * FROM lesson WHERE lesson_id = ?";
            logger.debug("执行 SQL: {} with lessonId = {}", sql, lessonId);
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, lessonId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                lesson = new LessonEntity();
                lesson.setId(rs.getInt("lesson_id"));
                lesson.setTitle(rs.getString("title"));
                lesson.setDescription(rs.getString("description"));
                lesson.setTeacherId(rs.getInt("teacher_id"));
                lesson.setSemesterId(rs.getInt("semester_id"));
                Timestamp createdAtTs = rs.getTimestamp("created_at");
                lesson.setCreatedAt(createdAtTs != null ? createdAtTs.toLocalDateTime() : null);
                logger.debug("成功找到课程 ID {} 的实体，标题: {}", lessonId, lesson.getTitle());
            } else {
                logger.debug("未找到课程 ID {} 的实体。", lessonId);
            }
        } catch (SQLException e) {
            logger.error("根据课程ID {} 查询课程信息时发生数据库异常。", lessonId, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成根据课程ID {} 查询课程信息操作。", lessonId);
        return lesson;
    }

    /**
     * 根据教师ID查询该教师负责的所有课程信息
     * @param teacherId 教师的唯一标识符ID
     * @return 返回该教师负责的课程列表，如果找不到则返回空列表
     */
    public List<LessonEntity> getLessonsByTeacherId(int teacherId) {
        logger.debug("尝试根据教师ID {} 查询负责的所有课程信息。", teacherId);
        List<LessonEntity> lessons = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT * FROM lesson WHERE teacher_id = ?";
            logger.debug("执行 SQL: {} with teacherId = {}", sql, teacherId);
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, teacherId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                LessonEntity lesson = new LessonEntity();
                lesson.setId(rs.getInt("lesson_id"));
                lesson.setTitle(rs.getString("title"));
                lesson.setDescription(rs.getString("description"));
                lesson.setTeacherId(rs.getInt("teacher_id"));
                lesson.setSemesterId(rs.getInt("semester_id"));
                Timestamp createdAtTs = rs.getTimestamp("created_at");
                lesson.setCreatedAt(createdAtTs != null ? createdAtTs.toLocalDateTime() : null);
                lessons.add(lesson);
                logger.trace("找到课程: ID = {}, Title = {}", lesson.getId(), lesson.getTitle());
            }
            logger.debug("成功找到 {} 个课程与教师 ID {} 关联。", lessons.size(), teacherId);
        } catch (SQLException e) {
            logger.error("根据教师ID {} 查询课程列表时发生数据库异常。", teacherId, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成根据教师ID {} 查询课程列表操作。", teacherId);
        return lessons;
    }

    /**
     * 获取所有课程信息
     * @return 所有课程的列表
     */
    public List<LessonEntity> getAllLessons() {
        logger.debug("尝试获取所有课程信息。");
        List<LessonEntity> lessons = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT * FROM lesson";
            logger.debug("执行 SQL: {}", sql);
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                LessonEntity lesson = new LessonEntity();
                lesson.setId(rs.getInt("lesson_id"));
                lesson.setTeacherId(rs.getInt("teacher_id"));
                lesson.setSemesterId(rs.getInt("semester_id"));
                lesson.setTitle(rs.getString("title"));
                lesson.setDescription(rs.getString("description"));
                Timestamp createdAtTs = rs.getTimestamp("created_at");
                lesson.setCreatedAt(createdAtTs != null ? createdAtTs.toLocalDateTime() : null);
                lessons.add(lesson);
                logger.trace("找到课程: ID = {}, Title = '{}'", lesson.getId(), lesson.getTitle());
            }
            logger.debug("成功获取 {} 个课程。", lessons.size());
        } catch (SQLException e) {
            logger.error("获取所有课程信息时发生数据库异常。", e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成获取所有课程信息操作。");
        return lessons;
    }

    /**
     * 添加新课程
     * @param lesson 课程实体
     * @return 是否成功添加
     */
    public boolean addLesson(LessonEntity lesson) {
        logger.info("尝试添加新课程，标题: {}", lesson.getTitle());
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DBUtils.getConnection();
            String sql = "INSERT INTO lesson (teacher_id, semester_id, title, description) VALUES (?, ?, ?, ?)";
            logger.debug("执行 SQL (添加课程): {} with title = '{}'", sql, lesson.getTitle());
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, lesson.getTeacherId());
            pstmt.setInt(2, lesson.getSemesterId());
            pstmt.setString(3, lesson.getTitle());
            pstmt.setString(4, lesson.getDescription());

            int affectedRows = pstmt.executeUpdate();
            success = affectedRows > 0;
            logger.debug("添加课程影响行数: {}", affectedRows);

            if (success) {
                logger.info("成功添加新课程，标题: {}", lesson.getTitle());
            } else {
                logger.warn("添加新课程失败，标题: {}，可能数据库操作未成功。", lesson.getTitle());
            }

        } catch (SQLException e) {
            logger.error("添加新课程时发生数据库异常，标题: {}", lesson.getTitle(), e);
        } finally {
            DBUtils.close(conn, pstmt);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成添加新课程操作，标题: {}，结果: {}", lesson.getTitle(), success ? "成功" : "失败");
        return success;
    }

    /**
     * 更新课程的教师ID
     * @param lessonId 课程ID
     * @param teacherId 新的教师ID
     * @return 是否成功更新
     */
    public boolean updateLessonTeacher(int lessonId, int teacherId) {
        logger.info("尝试更新课程 ID {} 的教师为教师 ID {}。", lessonId, teacherId);
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DBUtils.getConnection();
            String sql = "UPDATE lesson SET teacher_id = ? WHERE lesson_id = ?";
            logger.debug("执行 SQL (更新课程教师): {} with lessonId = {} and teacherId = {}", sql, lessonId, teacherId);
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, teacherId);
            pstmt.setInt(2, lessonId);

            int affectedRows = pstmt.executeUpdate();
            success = affectedRows > 0;
            logger.debug("更新课程教师影响行数: {}", affectedRows);

            if (success) {
                logger.info("成功更新课程 ID {} 的教师为教师 ID {}。", lessonId, teacherId);
            } else {
                logger.warn("更新课程 ID {} 的教师失败，可能课程不存在或数据库操作未成功。", lessonId);
            }
        } catch (SQLException e) {
            logger.error("更新课程 ID {} 的教师时发生数据库异常。", lessonId, e);
        } finally {
            DBUtils.close(conn, pstmt);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成更新课程 ID {} 教师操作，结果: {}", lessonId, success ? "成功" : "失败");
        return success;
    }
}
