package io.github.gongding.dao;

import io.github.gongding.entity.QuestionEntity;
import io.github.gongding.util.DBUtils;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class SubmissionDao {
    private final QuestionDao questionDao = new QuestionDao();

    /**
     * 创建学生提交记录并保存答案
     *
     * @param studentId  学生ID
     * @param practiceId 练习ID
     * @param answers 包含每个题目答案的列表
     * @return 新创建的提交记录的submission_id，如果失败则返回 -1
     */
    public int createSubmission(int studentId, int practiceId, List<Map<String, Object>> answers) {
        Connection conn = null;
        PreparedStatement submissionPstmt = null;
        PreparedStatement answerPstmt = null;
        ResultSet rs = null;
        // 初始化新创建提交记录的ID，失败时返回 -1
        int submissionId = -1;

        try {
            conn = DBUtils.getConnection();

            String insertSubmissionSql = "INSERT INTO submission (student_id, practice_id, submitted_at) VALUES (?, ?, ?)";
            submissionPstmt = conn.prepareStatement(insertSubmissionSql, Statement.RETURN_GENERATED_KEYS);
            submissionPstmt.setInt(1, studentId);
            submissionPstmt.setInt(2, practiceId);
            submissionPstmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));

            int affectedRows = submissionPstmt.executeUpdate();

            if (affectedRows > 0) {
                rs = submissionPstmt.getGeneratedKeys();
                if (rs.next()) {
                    //获取新创建提交记录的ID
                    submissionId = rs.getInt(1);

                    String insertAnswerSql = "INSERT INTO submission_answer (submission_id, question_id, student_answer, is_correct, grade, feedback, graded_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
                    answerPstmt = conn.prepareStatement(insertAnswerSql);

                    //遍历学生提交的每个题目的答案
                    for (Map<String, Object> answer : answers) {
                        int questionId = (Integer) answer.get("questionId");
                        String studentAnswer = (String) answer.get("answer");

                        QuestionEntity question = questionDao.getQuestionById(questionId);

                        Boolean isCorrect = null;
                        Double grade = null;
                        String feedback = null;
                        Timestamp gradedAt = null;

                        //检查是否成功获取了题目详情
                        if (question != null) {
                            String questionType = question.getType();
                            String correctAnswer = question.getCorrectAnswer();

                            //检查题目类型是否是单选或填空，并且存在正确答案
                            if (("single_choice".equals(questionType) || "fill_blank".equals(questionType)) && correctAnswer != null && !correctAnswer.trim().isEmpty()) {
                                //对于单选和简单填空，直接比较学生答案和正确答案
                                boolean match = studentAnswer != null && correctAnswer.trim().equalsIgnoreCase(studentAnswer.trim());

                                if ("fill_blank".equals(questionType) && correctAnswer.contains(",")) {
                                    //将正确答案和学生答案都按逗号分割成列表
                                    List<String> correctParts = Arrays.asList(correctAnswer.split(","));
                                    List<String> studentParts = studentAnswer != null && !studentAnswer.trim().isEmpty() ? Arrays.asList(studentAnswer.split(",")) : Collections.emptyList();

                                    //首先检查分割后的部分数量是否一致
                                    match = correctParts.size() == studentParts.size();
                                    if (match) {
                                        for (int i = 0; i < correctParts.size(); i++) {
                                            if (!correctParts.get(i).trim().equalsIgnoreCase(studentParts.get(i).trim())) {
                                                //只要有一个部分不匹配，则整个答案不正确
                                                match = false;
                                                break;
                                            }
                                        }
                                    }
                                }

                                isCorrect = match;
                                //如果答案正确，设置得分和评分时间
                                if (isCorrect != null && isCorrect) {
                                    //设置得分为题目的满分
                                    grade = question.getScore();
                                    gradedAt = Timestamp.valueOf(LocalDateTime.now());
                                }
                            }
                        } else {
                            System.err.println("Question " + questionId + " not found");
                        }

                        answerPstmt.setInt(1, submissionId);
                        answerPstmt.setInt(2, questionId);
                        answerPstmt.setString(3, studentAnswer);

                        if (isCorrect != null) {
                            answerPstmt.setBoolean(4, isCorrect);
                        } else {
                            answerPstmt.setNull(4, Types.TINYINT);
                        }

                        if (grade != null) {
                            answerPstmt.setDouble(5, grade);
                        } else {
                            answerPstmt.setNull(5, Types.DECIMAL);
                        }

                        answerPstmt.setString(6, feedback);
                        answerPstmt.setTimestamp(7, gradedAt);

                        answerPstmt.addBatch();
                    }

                    answerPstmt.executeBatch();

                } else {
                    submissionId = -1;
                }
            } else {
                submissionId = -1;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            submissionId = -1;
        } finally {
            DBUtils.close(conn, submissionPstmt, rs);
            DBUtils.close(null, answerPstmt, null);
        }
        return submissionId;
    }

    /**
     * 计算学生在某个练习中已完成的题目数量
     * @param studentId 学生ID
     * @param practiceId 练习ID
     * @return 已完成题目数量
     */
    public int getStudentCompletedQuestionCount(int studentId, int practiceId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int completedCount = 0;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT COUNT(sa.question_id) AS completed_count " +
                    "FROM submission s " +
                    "JOIN submission_answer sa ON s.submission_id = sa.submission_id " +
                    "WHERE s.student_id = ? AND s.practice_id = ? " +
                    "AND s.submitted_at = (SELECT MAX(submitted_at) FROM submission WHERE student_id = ? AND practice_id = ?) " +
                    "AND (sa.student_answer IS NOT NULL AND sa.student_answer != '')";

            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, practiceId);
            pstmt.setInt(3, studentId);
            pstmt.setInt(4, practiceId);

            rs = pstmt.executeQuery();

            if (rs.next()) {
                completedCount = rs.getInt("completed_count");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt, rs);
        }
        return completedCount;
    }

    /**
     * 计算学生在某个练习中获得的总分数
     * @param studentId 学生ID
     * @param practiceId 练习ID
     * @return 学生在该练习中获得的总分数
     */
    public double getStudentObtainedScore(int studentId, int practiceId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        double obtainedScore = 0.0;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT SUM(q.score) AS obtained_score " +
                    "FROM submission s " +
                    "JOIN submission_answer sa ON s.submission_id = sa.submission_id " +
                    "JOIN question q ON sa.question_id = q.question_id " +
                    "WHERE s.student_id = ? AND s.practice_id = ? " +
                    "AND s.submitted_at = (SELECT MAX(submitted_at) FROM submission WHERE student_id = ? AND practice_id = ?) " + // Latest submission
                    "AND (sa.is_correct = TRUE OR sa.grade IS NOT NULL)"; // Include auto-correct and manually graded

            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, practiceId);
            pstmt.setInt(3, studentId);
            pstmt.setInt(4, practiceId);

            rs = pstmt.executeQuery();

            if (rs.next()) {
                obtainedScore = rs.getDouble("obtained_score");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt, rs);
        }
        return obtainedScore;
    }
}
