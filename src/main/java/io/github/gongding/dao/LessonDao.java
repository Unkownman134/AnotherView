package io.github.gongding.dao;

import io.github.gongding.entity.LessonEntity;
import io.github.gongding.util.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LessonDao {
    /**
     * 根据课程ID查询课程信息
     * @param lessonId 课程的唯一标识符ID
     * @return 如果找到，返回LessonEntity对象；否则返回null
     */
    public LessonEntity getLessonById(int lessonId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        LessonEntity lesson = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT * FROM lesson WHERE lesson_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, lessonId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                lesson = new LessonEntity();
                lesson.setId(rs.getInt("lesson_id"));
                lesson.setTitle(rs.getString("title"));
                lesson.setDescription(rs.getString("description")); // 关键字段
                lesson.setTeacherId(rs.getInt("teacher_id"));
                lesson.setSemesterId(rs.getInt("semester_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt, rs);
        }
        return lesson;
    }

    /**
     * 根据教师ID查询该教师负责的所有课程信息
     * @param teacherId 教师的唯一标识符ID
     * @return 返回该教师负责的课程列表，如果找不到则返回空列表
     */
    public List<LessonEntity> getLessonsByTeacherId(int teacherId) {
        List<LessonEntity> lessons = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT * FROM lesson WHERE teacher_id = ?";
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt, rs);
        }
        return lessons;
    }
}