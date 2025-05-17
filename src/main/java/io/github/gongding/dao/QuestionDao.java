package io.github.gongding.dao;

import io.github.gongding.entity.QuestionEntity;
import io.github.gongding.util.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class QuestionDao {
    /**
     * 根据课程ID查询题目列表，不含选项的组合和打乱
     *
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

    /**
     * 根据练习ID获取题目列表，包含选项、正确答案和错误答案
     *
     * @param practiceId 练习ID
     * @return 题目实体列表，包含选项、正确答案和错误答案
     */
    public List<QuestionEntity> getQuestionsByPracticeId(int practiceId) {
        List<QuestionEntity> questions = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT q.* FROM question q " +
                    "JOIN practice_question pq ON q.question_id = pq.question_id " +
                    "WHERE pq.practice_id = ? ORDER BY pq.seq_no";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, practiceId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                QuestionEntity q = new QuestionEntity();
                q.setId(rs.getInt("question_id"));
                q.setLessonId(rs.getInt("lesson_id"));
                q.setContent(rs.getString("content"));
                q.setType(rs.getString("type"));
                q.setDifficulty(rs.getString("difficulty"));
                q.setScore(rs.getDouble("score"));

                String correct = rs.getString("correct_answer");
                String error = rs.getString("error_answer");
                q.setCorrectAnswer(correct);
                q.setErrorAnswer(error);

                //特别处理选择题的选项
                if ("single_choice".equals(q.getType()) || "multiple_choice".equals(q.getType())) {
                    List<String> options = new ArrayList<>();
                    if (correct != null && !correct.trim().isEmpty()) {
                        Arrays.stream(correct.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .forEach(options::add);
                    }
                    //如果错误答案字符串不为null且不为空白
                    if (error != null && !error.trim().isEmpty()) {
                        //将错误答案字符串按逗号分割，去除首尾空白，过滤掉空字符串，然后添加到options列表中
                        Arrays.stream(error.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .forEach(options::add);
                    }
                    //打乱选项的顺序，使其随机显示
                    Collections.shuffle(options);
                    q.setOptions(options);
                } else {
                    //如果不是选择题，设置options属性为一个空的不可修改列表
                    q.setOptions(Collections.emptyList());
                }

                questions.add(q);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt, rs);
        }
        return questions;
    }

    /**
     * 根据题目的唯一标识符ID查询单个题目的详细信息。
     * 这个方法执行数据库读取操作，返回一个QuestionEntity对象。
     * 除了题目的基本信息外，对于选择题，它还会处理选项的生成和随机排序。
     */
    public QuestionEntity getQuestionById(int questionId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        QuestionEntity question = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT * FROM question WHERE question_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, questionId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                question = new QuestionEntity();
                question.setId(rs.getInt("question_id"));
                question.setLessonId(rs.getInt("lesson_id"));
                question.setContent(rs.getString("content"));
                question.setCorrectAnswer(rs.getString("correct_answer"));
                question.setErrorAnswer(rs.getString("error_answer"));
                question.setType(rs.getString("type"));
                question.setDifficulty(rs.getString("difficulty"));
                question.setScore(rs.getDouble("score"));

                //检查题目类型是否是单选或多选
                if ("single_choice".equals(question.getType()) || "multiple_choice".equals(question.getType())) {
                    List<String> options = new ArrayList<>();
                    String correct = question.getCorrectAnswer();
                    String error = question.getErrorAnswer();
                    if (correct != null && !correct.trim().isEmpty()) {
                        Arrays.stream(correct.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .forEach(options::add);
                    }
                    if (error != null && !error.trim().isEmpty()) {
                        Arrays.stream(error.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .forEach(options::add);
                    }
                    //打乱选项的顺序，使其随机显示给用户
                    Collections.shuffle(options);
                    question.setOptions(options);
                } else {
                    question.setOptions(Collections.emptyList());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt, rs);
        }
        return question;
    }
}
