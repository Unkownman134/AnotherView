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
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int submissionIdToUse = -1;

        try {
            conn = DBUtils.getConnection();
            conn.setAutoCommit(false);

            String selectLatestSubmissionSql = "SELECT submission_id FROM submission " +
                    "WHERE student_id = ? AND practice_id = ? " +
                    "ORDER BY submission_id DESC LIMIT 1";
            stmt = conn.prepareStatement(selectLatestSubmissionSql);
            stmt.setInt(1, studentId);
            stmt.setInt(2, practiceId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                submissionIdToUse = rs.getInt("submission_id");
            }
            if (rs != null) { rs.close(); rs = null; }
            if (stmt != null) { stmt.close(); stmt = null; }


            if (submissionIdToUse != -1) {
                String deleteAnswersSql = "DELETE FROM submission_answer WHERE submission_id = ?";
                stmt = conn.prepareStatement(deleteAnswersSql);
                stmt.setInt(1, submissionIdToUse);
                stmt.executeUpdate();
                if (stmt != null) { stmt.close(); stmt = null; }

                String updateSubmissionTimeSql = "UPDATE submission SET submitted_at = ? WHERE submission_id = ?";
                stmt = conn.prepareStatement(updateSubmissionTimeSql);
                stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setInt(2, submissionIdToUse);
                stmt.executeUpdate();
                if (stmt != null) { stmt.close(); stmt = null; }

            } else {
                String insertSubmissionSql = "INSERT INTO submission (student_id, practice_id, submitted_at) VALUES (?, ?, ?)";
                stmt = conn.prepareStatement(insertSubmissionSql, Statement.RETURN_GENERATED_KEYS);
                stmt.setInt(1, studentId);
                stmt.setInt(2, practiceId);
                stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    rs = stmt.getGeneratedKeys();
                    if (rs.next()) {
                        submissionIdToUse = rs.getInt(1);
                    } else {
                        System.err.println("创建提交记录失败，未能获取生成的 submission_id。");
                        conn.rollback();
                        return -1;
                    }
                } else {
                    System.err.println("创建提交记录失败，submission 表未插入行。");
                    conn.rollback();
                    return -1;
                }
                if (rs != null) { rs.close(); rs = null; }
                if (stmt != null) { stmt.close(); stmt = null; }
            }

            if (submissionIdToUse == -1) {
                System.err.println("无效的 submission_id，无法保存答案。");
                conn.rollback();
                return -1;
            }

            String insertAnswerSql = "INSERT INTO submission_answer (submission_id, question_id, student_answer, is_correct, grade, feedback, graded_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
            stmt = conn.prepareStatement(insertAnswerSql);

            for (Map<String, Object> answerMap : answers) {
                int questionId = (Integer) answerMap.get("questionId");
                String studentAnswerText = (String) answerMap.get("studentAnswer");

                QuestionEntity question = questionDao.getQuestionById(questionId);

                Boolean isCorrect = null;
                Double grade = null;
                String feedback = null;
                Timestamp gradedAt = null;

                if (question != null) {
                    String questionType = question.getType();
                    String correctAnswerDB = question.getCorrectAnswer();

                    if (("single_choice".equals(questionType) || "multiple_choice".equals(questionType) || "fill_blank".equals(questionType))
                            && correctAnswerDB != null && !correctAnswerDB.trim().isEmpty()) {
                        if (studentAnswerText != null && !studentAnswerText.trim().isEmpty()) {
                            String studentAnswerTrimmed = studentAnswerText.trim();
                            String correctAnswerTrimmed = correctAnswerDB.trim();
                            if ("single_choice".equals(questionType)) {
                                isCorrect = correctAnswerTrimmed.equalsIgnoreCase(studentAnswerTrimmed);
                            } else if ("multiple_choice".equals(questionType)) {
                                Set<String> correctOptions = new HashSet<>(Arrays.asList(correctAnswerTrimmed.toLowerCase().split("\\s*,\\s*")));
                                Set<String> studentOptions = new HashSet<>(Arrays.asList(studentAnswerTrimmed.toLowerCase().split("\\s*,\\s*")));
                                isCorrect = correctOptions.equals(studentOptions);
                            } else if ("fill_blank".equals(questionType)) {
                                if (correctAnswerTrimmed.contains(",")) {
                                    List<String> correctParts = Arrays.asList(correctAnswerTrimmed.split("\\s*,\\s*"));
                                    List<String> studentParts = Arrays.asList(studentAnswerTrimmed.split("\\s*,\\s*"));
                                    if (correctParts.size() == studentParts.size()) {
                                        boolean allMatch = true;
                                        for (int i = 0; i < correctParts.size(); i++) {
                                            if (!correctParts.get(i).trim().equalsIgnoreCase(studentParts.get(i).trim())) {
                                                allMatch = false;
                                                break;
                                            }
                                        }
                                        isCorrect = allMatch;
                                    } else {
                                        isCorrect = false;
                                    }
                                } else {
                                    isCorrect = correctAnswerTrimmed.equalsIgnoreCase(studentAnswerTrimmed);
                                }
                            }
                        } else {
                            isCorrect = false;
                        }
                        if (Boolean.TRUE.equals(isCorrect)) {
                            grade = question.getScore();
                        } else if (Boolean.FALSE.equals(isCorrect)){
                            grade = 0.0;
                        }
                    }
                } else {
                    System.err.println("警告: 提交答案时未找到题目详情，题目ID: " + questionId);
                }

                stmt.setInt(1, submissionIdToUse);
                stmt.setInt(2, questionId);
                stmt.setString(3, studentAnswerText);
                if (isCorrect != null) stmt.setBoolean(4, isCorrect); else stmt.setNull(4, Types.TINYINT);
                if (grade != null) stmt.setDouble(5, grade); else stmt.setNull(5, Types.DECIMAL);
                stmt.setString(6, feedback);
                stmt.setTimestamp(7, gradedAt);
                stmt.addBatch();
            }
            stmt.executeBatch();
            conn.commit();
            return submissionIdToUse;


        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return -1;
        } finally {
            DBUtils.close(null, stmt, rs);
            DBUtils.close(conn, null, null);
        }
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

    /**
     * 计算某个练习的总分数
     * @param practiceId 练习ID
     * @return 练习的总分数
     */
    public double getPracticeTotalScore(int practiceId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        double totalScore = 0.0;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT SUM(q.score) AS total_score " +
                    "FROM practice_question pq " +
                    "JOIN question q ON pq.question_id = q.question_id " +
                    "WHERE pq.practice_id = ?";

            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, practiceId);

            rs = pstmt.executeQuery();

            if (rs.next()) {
                totalScore = rs.getDouble("total_score");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt, rs);
        }
        return totalScore;
    }

    /**
     * 获取学生对特定练习的最近一次提交记录及答案
     * @param studentId 学生ID
     * @param practiceId 练习ID
     * @return 最近一次提交的Map表示，如果不存在则返回null
     */
    public Map<String, Object> getLatestSubmission(int studentId, int practiceId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Map<String, Object> submission = null;

        try {
            conn = DBUtils.getConnection();

            String latestSubmissionSql = "SELECT submission_id FROM submission WHERE student_id = ? AND practice_id = ? ORDER BY submitted_at DESC LIMIT 1";
            pstmt = conn.prepareStatement(latestSubmissionSql);
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, practiceId);
            rs = pstmt.executeQuery();

            int submissionId = -1;
            if (rs.next()) {
                submissionId = rs.getInt("submission_id");
            }
            DBUtils.close(null, pstmt, rs);
            pstmt = null;
            rs = null;

            if (submissionId != -1) {
                String answersSql = "SELECT question_id, student_answer, is_correct, grade, feedback FROM submission_answer WHERE submission_id = ?";
                pstmt = conn.prepareStatement(answersSql);
                pstmt.setInt(1, submissionId);
                rs = pstmt.executeQuery();

                List<Map<String, Object>> answers = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> answer = new HashMap<>();
                    answer.put("questionId", rs.getInt("question_id"));
                    answer.put("studentAnswer", rs.getString("student_answer"));
                    answer.put("isCorrect", rs.getObject("is_correct"));
                    answer.put("grade", rs.getObject("grade"));
                    answer.put("feedback", rs.getString("feedback"));
                    answers.add(answer);
                }

                submission = new HashMap<>();
                submission.put("submissionId", submissionId);
                submission.put("answers", answers);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt, rs);
        }
        return submission;
    }


}
