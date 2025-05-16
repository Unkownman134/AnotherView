package io.github.gongding.dao;

import io.github.gongding.entity.QuestionEntity;
import io.github.gongding.util.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class QuestionDao {
    /**
     * 根据课程ID查询题目列表，不含选项的组合和打乱
     * @param lessonId 课程ID
     * @return 题目实体列表
     */
    public List<QuestionEntity> getQuestionsByLessonId(int lessonId) {
        List<QuestionEntity> questions = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT * FROM question WHERE lesson_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, lessonId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                QuestionEntity q = new QuestionEntity();
                q.setId(rs.getInt("question_id"));
                q.setLessonId(rs.getInt("lesson_id"));
                q.setContent(rs.getString("content"));
                q.setCorrectAnswer(rs.getString("correct_answer"));
                q.setErrorAnswer(rs.getString("error_answer"));
                q.setType(rs.getString("type"));
                q.setDifficulty(rs.getString("difficulty"));
                q.setScore(rs.getDouble("score"));
                questions.add(q);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt, rs);
        }
        return questions;
    }
}
