package io.github.gongding.dao;

import io.github.gongding.entity.LessonEntity;
import io.github.gongding.util.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
                lesson.setSemesterId(rs.getInt("semester_id"));
                lesson.setTeacherId(rs.getInt("teacher_id"));
                lesson.setDescription(rs.getString("description"));
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
}
