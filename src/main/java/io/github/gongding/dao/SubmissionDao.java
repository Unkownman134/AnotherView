package io.github.gongding.dao;

import io.github.gongding.entity.QuestionEntity;
import io.github.gongding.util.DBUtils;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubmissionDao {
    private static final Logger logger = LoggerFactory.getLogger(SubmissionDao.class);
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
        logger.info("尝试创建学生 ID {} 对练习 ID {} 的提交记录。", studentId, practiceId);
        logger.debug("提交答案数量: {}", (answers != null ? answers.size() : 0));

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int submissionIdToUse = -1;

        try {
            conn = DBUtils.getConnection();

            String selectLatestSubmissionSql = "SELECT submission_id FROM submission " +
                    "WHERE student_id = ? AND practice_id = ? " +
                    "ORDER BY submission_id DESC LIMIT 1";
            logger.debug("执行 SQL (查询最新提交): {} with studentId = {}, practiceId = {}", selectLatestSubmissionSql, studentId, practiceId);
            stmt = conn.prepareStatement(selectLatestSubmissionSql);
            stmt.setInt(1, studentId);
            stmt.setInt(2, practiceId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                submissionIdToUse = rs.getInt("submission_id");
                logger.debug("找到学生 ID {} 对练习 ID {} 的现有最新提交记录，ID: {}", studentId, practiceId, submissionIdToUse);
            } else {
                logger.debug("未找到学生 ID {} 对练习 ID {} 的现有提交记录。", studentId, practiceId);
            }
            if (rs != null) { rs.close(); rs = null; }
            if (stmt != null) { stmt.close(); stmt = null; }


            if (submissionIdToUse != -1) {
                logger.debug("删除现有提交记录 {} 的关联答案。", submissionIdToUse);
                String deleteAnswersSql = "DELETE FROM submission_answer WHERE submission_id = ?";
                stmt = conn.prepareStatement(deleteAnswersSql);
                stmt.setInt(1, submissionIdToUse);
                int deletedRows = stmt.executeUpdate();
                logger.debug("删除旧答案影响行数: {}", deletedRows);
                if (stmt != null) { stmt.close(); stmt = null; }

                logger.debug("更新现有提交记录 {} 的提交时间。", submissionIdToUse);
                String updateSubmissionTimeSql = "UPDATE submission SET submitted_at = ? WHERE submission_id = ?";
                stmt = conn.prepareStatement(updateSubmissionTimeSql);
                stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setInt(2, submissionIdToUse);
                int updatedRows = stmt.executeUpdate();
                logger.debug("更新提交时间影响行数: {}", updatedRows);
                if (stmt != null) { stmt.close(); stmt = null; }

            } else {
                logger.debug("创建新的提交记录，学生 ID: {}, 练习 ID: {}", studentId, practiceId);
                String insertSubmissionSql = "INSERT INTO submission (student_id, practice_id, submitted_at) VALUES (?, ?, ?)";
                stmt = conn.prepareStatement(insertSubmissionSql, Statement.RETURN_GENERATED_KEYS);
                stmt.setInt(1, studentId);
                stmt.setInt(2, practiceId);
                stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                int affectedRows = stmt.executeUpdate();
                logger.debug("插入新提交记录影响行数: {}", affectedRows);

                if (affectedRows > 0) {
                    rs = stmt.getGeneratedKeys();
                    if (rs.next()) {
                        submissionIdToUse = rs.getInt(1);
                        logger.info("成功创建新的提交记录，ID: {}", submissionIdToUse);
                    } else {
                        logger.error("创建提交记录失败，影响行数 > 0 但未能获取生成的 submission_id。");
                        return -1;
                    }
                } else {
                    logger.error("创建提交记录失败，submission 表未插入行。学生ID: {}, 练习ID: {}", studentId, practiceId);
                    return -1;
                }
                if (rs != null) { rs.close(); rs = null; }
                if (stmt != null) { stmt.close(); stmt = null; }
            }

            if (submissionIdToUse == -1) {
                logger.error("无效的 submission_id ({})，无法保存答案。学生ID: {}, 练习ID: {}", submissionIdToUse, studentId, practiceId);
                return -1;
            }

            logger.debug("开始保存提交记录 {} 的答案，共 {} 个。", submissionIdToUse, (answers != null ? answers.size() : 0));
            String insertAnswerSql = "INSERT INTO submission_answer (submission_id, question_id, student_answer, is_correct, grade, feedback, graded_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
            stmt = conn.prepareStatement(insertAnswerSql);

            if (answers != null) {
                for (Map<String, Object> answerMap : answers) {
                    if (!answerMap.containsKey("questionId") || !answerMap.containsKey("studentAnswer")) {
                        logger.warn("提交答案Map缺少必要字段 (questionId 或 studentAnswer): {}", answerMap);
                        continue;
                    }

                    int questionId = (Integer) answerMap.get("questionId");
                    String studentAnswerText = (String) answerMap.get("studentAnswer");
                    logger.trace("处理题目 {} 的学生答案。", questionId);

                    QuestionEntity question = questionDao.getQuestionById(questionId);

                    Boolean isCorrect = null;
                    Double grade = null;
                    String feedback = null;
                    Timestamp gradedAt = null;

                    if (question != null) {
                        String questionType = question.getType();
                        String correctAnswerDB = question.getCorrectAnswer();
                        logger.trace("题目 {} 详情 - 类型: {}, 正确答案: '{}'", questionId, questionType, correctAnswerDB);

                        if (("single_choice".equals(questionType) || "multiple_choice".equals(questionType) || "fill_blank".equals(questionType))
                                && correctAnswerDB != null && !correctAnswerDB.trim().isEmpty()) {
                            logger.trace("题目 {} 是可自动评分题型。", questionId);
                            if (studentAnswerText != null && !studentAnswerText.trim().isEmpty()) {
                                String studentAnswerTrimmed = studentAnswerText.trim();
                                String correctAnswerTrimmed = correctAnswerDB.trim();
                                logger.trace("学生答案: '{}'", studentAnswerTrimmed);

                                if ("single_choice".equals(questionType)) {
                                    isCorrect = correctAnswerTrimmed.equalsIgnoreCase(studentAnswerTrimmed);
                                } else if ("multiple_choice".equals(questionType)) {
                                    Set<String> correctOptions = new HashSet<>(Arrays.asList(correctAnswerTrimmed.toLowerCase().split("\\s*,\\s*")));
                                    Set<String> studentOptions = new HashSet<>(Arrays.asList(studentAnswerTrimmed.toLowerCase().split("\\s*,\\s*")));
                                    isCorrect = correctOptions.equals(studentOptions);
                                    logger.trace("多选题比较 - 正确选项集合: {}, 学生选项集合: {}", correctOptions, studentOptions);
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
                                            logger.trace("填空题答案数量不匹配 - 正确部分数: {}, 学生部分数: {}", correctParts.size(), studentParts.size());
                                        }
                                    } else {
                                        isCorrect = correctAnswerTrimmed.equalsIgnoreCase(studentAnswerTrimmed);
                                    }
                                    logger.trace("填空题比较结果: {}", isCorrect);
                                }
                            } else {
                                isCorrect = false;
                                logger.trace("学生答案为空，标记为不正确。");
                            }
                            if (Boolean.TRUE.equals(isCorrect)) {
                                grade = question.getScore();
                                logger.trace("自动评分：答案正确，得分 {}", grade);
                            } else if (Boolean.FALSE.equals(isCorrect)){
                                grade = 0.0;
                                logger.trace("自动评分：答案不正确，得分 0.0");
                            }
                        } else {
                            logger.trace("题目 {} 不是可自动评分题型或没有正确答案。", questionId);
                        }
                    } else {
                        logger.warn("提交答案时未找到题目详情，题目ID: {}。无法进行自动评分。", questionId);
                    }

                    stmt.setInt(1, submissionIdToUse);
                    stmt.setInt(2, questionId);
                    stmt.setString(3, studentAnswerText);
                    if (isCorrect != null) stmt.setBoolean(4, isCorrect); else stmt.setNull(4, Types.TINYINT);
                    if (grade != null) stmt.setDouble(5, grade); else stmt.setNull(5, Types.DECIMAL);
                    stmt.setString(6, feedback);
                    stmt.setTimestamp(7, gradedAt);
                    stmt.addBatch();
                    logger.trace("添加答案到批量插入 - 题目ID: {}, 学生答案: '{}', 是否正确: {}, 评分: {}", questionId, studentAnswerText, isCorrect, grade);
                }
            } else {
                logger.debug("提交的答案列表为 null。");
            }

            int[] batchResult = stmt.executeBatch();
            logger.debug("批量插入答案结果 (每项影响行数): {}", batchResult);

            logger.info("成功创建提交记录 {} 并保存答案。", submissionIdToUse);
            return submissionIdToUse;


        } catch (SQLException e) {
            logger.error("创建提交记录或保存答案时发生数据库异常。学生ID: {}, 练习ID: {}", studentId, practiceId, e);
            return -1;
        } catch (Exception e) {
            logger.error("创建提交记录或保存答案时发生其他异常。学生ID: {}, 练习ID: {}", studentId, practiceId, e);
            return -1;
        } finally {
            DBUtils.close(null, stmt, rs);
            DBUtils.close(conn, null, null);
            logger.debug("关闭数据库资源。");
        }
    }

    /**
     * 计算学生在某个练习中已完成的题目数量
     * @param studentId 学生ID
     * @param practiceId 练习ID
     * @return 已完成题目数量
     */
    public int getStudentCompletedQuestionCount(int studentId, int practiceId) {
        logger.debug("尝试计算学生 ID {} 在练习 ID {} 中已完成的题目数量。", studentId, practiceId);
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
            logger.debug("执行 SQL (计算完成题目数): {} with studentId = {}, practiceId = {}", sql, studentId, practiceId);

            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, practiceId);
            pstmt.setInt(3, studentId);
            pstmt.setInt(4, practiceId);

            rs = pstmt.executeQuery();

            if (rs.next()) {
                completedCount = rs.getInt("completed_count");
                logger.debug("学生 ID {} 在练习 ID {} 中已完成 {} 道题目。", studentId, practiceId, completedCount);
            } else {
                logger.debug("未能获取学生 ID {} 在练习 ID {} 中的完成题目数量。", studentId, practiceId);
            }

        } catch (SQLException e) {
            logger.error("计算学生 ID {} 在练习 ID {} 中已完成题目数量时发生数据库异常。", studentId, practiceId, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成计算学生 ID {} 在练习 ID {} 中已完成题目数量操作，结果: {}", studentId, practiceId, completedCount);
        return completedCount;
    }

    /**
     * 计算学生在某个练习中获得的总分数
     * @param studentId 学生ID
     * @param practiceId 练习ID
     * @return 学生在该练习中获得的总分数
     */
    public double getStudentObtainedScore(int studentId, int practiceId) {
        logger.debug("尝试计算学生 ID {} 在练习 ID {} 中获得的总分数。", studentId, practiceId);
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        double obtainedScore = 0.0;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT SUM(CASE " +
                    "WHEN sa.is_correct = TRUE THEN q.score " +
                    "WHEN sa.grade IS NOT NULL THEN sa.grade " +
                    "ELSE 0 " +
                    "END) AS obtained_score " +
                    "FROM submission s " +
                    "JOIN submission_answer sa ON s.submission_id = sa.submission_id " +
                    "JOIN question q ON sa.question_id = q.question_id " +
                    "WHERE s.student_id = ? AND s.practice_id = ? " +
                    "AND s.submitted_at = (SELECT MAX(submitted_at) FROM submission WHERE student_id = ? AND practice_id = ?) " +
                    "AND (sa.is_correct = TRUE OR sa.grade IS NOT NULL)";
            logger.debug("执行 SQL (计算获得分数): {} with studentId = {}, practiceId = {}", sql, studentId, practiceId);

            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, practiceId);
            pstmt.setInt(3, studentId);
            pstmt.setInt(4, practiceId);

            rs = pstmt.executeQuery();

            if (rs.next()) {
                obtainedScore = rs.getDouble("obtained_score");
                logger.debug("学生 ID {} 在练习 ID {} 中获得的总分: {}", studentId, practiceId, obtainedScore);
            } else {
                logger.debug("未能获取学生 ID {} 在练习 ID {} 中的总分数。", studentId, practiceId);
            }

        } catch (SQLException e) {
            logger.error("计算学生 ID {} 在练习 ID {} 中获得总分数时发生数据库异常。", studentId, practiceId, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成计算学生 ID {} 在练习 ID {} 中获得总分数操作，结果: {}", studentId, practiceId, obtainedScore);
        return obtainedScore;
    }

    /**
     * 计算某个练习的总分数
     * @param practiceId 练习ID
     * @return 练习的总分数
     */
    public double getPracticeTotalScore(int practiceId) {
        logger.debug("尝试计算练习 ID {} 的总分数。", practiceId);
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
            logger.debug("执行 SQL (计算练习总分): {} with practiceId = {}", sql, practiceId);

            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, practiceId);

            rs = pstmt.executeQuery();

            if (rs.next()) {
                totalScore = rs.getDouble("total_score");
                if (Double.isNaN(totalScore) || Double.isInfinite(totalScore)) {
                    logger.warn("计算练习 ID {} 总分得到无效数值: {}", practiceId, totalScore);
                    totalScore = 0.0; // 或者其他默认值
                }
                logger.debug("练习 ID {} 的总分: {}", practiceId, totalScore);
            } else {
                logger.debug("未能获取练习 ID {} 的总分数，可能练习没有关联题目。", practiceId);
            }

        } catch (SQLException e) {
            logger.error("计算练习 ID {} 总分数时发生数据库异常。", practiceId, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成计算练习 ID {} 总分数操作，结果: {}", practiceId, totalScore);
        return totalScore;
    }

    /**
     * 获取学生对特定练习的最近一次提交记录及答案
     * @param studentId 学生ID
     * @param practiceId 练习ID
     * @return 最近一次提交的Map表示，如果不存在则返回null
     */
    public Map<String, Object> getLatestSubmission(int studentId, int practiceId) {
        logger.debug("尝试获取学生 ID {} 对练习 ID {} 的最近一次提交记录。", studentId, practiceId);
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Map<String, Object> submission = null;

        try {
            conn = DBUtils.getConnection();

            String selectLatestSubmissionSql = "SELECT submission_id FROM submission WHERE student_id = ? AND practice_id = ? ORDER BY submitted_at DESC LIMIT 1";
            logger.debug("执行 SQL (查询最近提交ID): {} with studentId = {}, practiceId = {}", selectLatestSubmissionSql, studentId, practiceId);
            pstmt = conn.prepareStatement(selectLatestSubmissionSql);
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, practiceId);
            rs = pstmt.executeQuery();

            int submissionId = -1;
            if (rs.next()) {
                submissionId = rs.getInt("submission_id");
                logger.debug("找到学生 ID {} 对练习 ID {} 的最近提交记录，ID: {}", studentId, practiceId, submissionId);
            } else {
                logger.debug("未找到学生 ID {} 对练习 ID {} 的提交记录。", studentId, practiceId);
                return null;
            }
            DBUtils.close(null, pstmt, rs);
            pstmt = null;
            rs = null;

            if (submissionId != -1) {
                String answersSql = "SELECT question_id, student_answer, is_correct, grade, feedback FROM submission_answer WHERE submission_id = ?";
                logger.debug("执行 SQL (查询提交答案): {} with submissionId = {}", answersSql, submissionId);
                pstmt = conn.prepareStatement(answersSql);
                pstmt.setInt(1, submissionId);
                rs = pstmt.executeQuery();

                List<Map<String, Object>> answers = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> answer = new HashMap<>();

                    int questionId = rs.getInt("question_id");

                    String studentAnswer = rs.getString("student_answer");
                    Object isCorrectObj = rs.getObject("is_correct");
                    Object gradeObj = rs.getObject("grade");
                    String feedback = rs.getString("feedback");
                    Boolean isCorrect = null;

                    if (isCorrectObj instanceof Boolean) {
                        isCorrect = (Boolean) isCorrectObj;
                    } else if (isCorrectObj != null) {
                        logger.warn("提交记录 {} 中题目 {} 的 is_correct 字段类型异常: {}", submissionId, questionId, isCorrectObj.getClass().getName());
                    }

                    Double grade = null;
                    if (gradeObj instanceof BigDecimal) {
                        grade = ((BigDecimal) gradeObj).doubleValue();
                    } else if (gradeObj instanceof Number) {
                        grade = ((Number) gradeObj).doubleValue();
                    } else if (gradeObj != null) {
                        logger.warn("提交记录 {} 中题目 {} 的 grade 字段类型异常: {}", submissionId, questionId, gradeObj.getClass().getName());
                    }

                    answer.put("questionId", questionId);
                    answer.put("studentAnswer", studentAnswer);
                    answer.put("isCorrect", isCorrect);
                    answer.put("grade", grade);
                    answer.put("feedback", feedback);
                    answers.add(answer);
                    logger.trace("找到提交记录 {} 的答案 - 题目ID: {}, 学生答案: '{}'", submissionId, questionId, studentAnswer);
                }
                logger.debug("成功找到提交记录 {} 的 {} 个答案。", submissionId, answers.size());

                submission = new HashMap<>();
                submission.put("submissionId", submissionId);
                submission.put("answers", answers);
            }

        } catch (SQLException e) {
            logger.error("获取学生 ID {} 对练习 ID {} 的最近一次提交记录时发生数据库异常。", studentId, practiceId, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成获取学生 ID {} 对练习 ID {} 的最近一次提交记录操作。", studentId, practiceId);
        return submission;
    }

    /**
     * 更新学生提交记录中单个题目答案的评分和反馈。
     *
     * @param submissionId 提交记录的唯一标识符ID。
     * @param questionId 题目答案所属的题目的唯一标识符ID。
     * @param grade 要设置的新的得分。
     * @param feedback 要设置的新的反馈信息（可以为 null）。
     * @return 如果更新成功返回true，否则返回false。
     */
    public boolean updateSubmissionAnswerGrade(int submissionId, int questionId, double grade, String feedback) {
        logger.info("尝试更新提交记录 {} 中题目 {} 的评分和反馈。", submissionId, questionId);
        logger.debug("新评分: {}, 新反馈: '{}'", grade, feedback);
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DBUtils.getConnection();
            String sql = "UPDATE submission_answer SET grade = ?, feedback = ?, graded_at = NOW() WHERE submission_id = ? AND question_id = ?";
            logger.debug("执行 SQL (更新评分): {} with submissionId = {}, questionId = {}, grade = {}, feedback = '{}'", sql, submissionId, questionId, grade, feedback);
            pstmt = conn.prepareStatement(sql);
            pstmt.setDouble(1, grade);
            pstmt.setString(2, feedback);
            pstmt.setInt(3, submissionId);
            pstmt.setInt(4, questionId);

            int affectedRows = pstmt.executeUpdate();
            success = affectedRows > 0;
            logger.debug("更新评分影响行数: {}", affectedRows);

            if (success) {
                logger.info("成功更新提交记录 {} 中题目 {} 的评分。", submissionId, questionId);
            } else {
                logger.warn("更新提交记录 {} 中题目 {} 的评分失败，可能该答案记录不存在。", submissionId, questionId);
            }

        } catch (SQLException e) {
            logger.error("更新提交记录 {} 中题目 {} 的评分时发生数据库异常。", submissionId, questionId, e);
        } finally {
            DBUtils.close(conn, pstmt);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成更新提交记录 {} 中题目 {} 评分操作，结果: {}", submissionId, questionId, success ? "成功" : "失败");
        return success;
    }
}
