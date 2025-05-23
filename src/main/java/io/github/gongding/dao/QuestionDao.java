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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuestionDao {
    private static final Logger logger = LoggerFactory.getLogger(QuestionDao.class);

    /**
     * 根据课程ID查询题目列表，不含选项的组合和打乱
     *
     * @param lessonId 课程ID
     * @return 题目实体列表
     */
    public List<QuestionEntity> getQuestionsByLessonId(int lessonId) {
        logger.debug("尝试根据课程ID {} 查询题目列表。", lessonId);
        List<QuestionEntity> questions = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT * FROM question WHERE lesson_id = ?";
            logger.debug("执行 SQL: {} with lessonId = {}", sql, lessonId);
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
                logger.trace("找到题目: ID = {}, Content = '{}'", q.getId(), q.getContent());
            }
            logger.debug("成功找到 {} 个题目与课程 ID {} 关联。", questions.size(), lessonId);
        } catch (SQLException e) {
            logger.error("根据课程ID {} 查询题目列表时发生数据库异常。", lessonId, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成根据课程ID {} 查询题目列表操作。", lessonId);
        return questions;
    }

    /**
     * 根据练习ID获取题目列表，包含选项、正确答案和错误答案
     *
     * @param practiceId 练习ID
     * @return 题目实体列表，包含选项、正确答案和错误答案
     */
    public List<QuestionEntity> getQuestionsByPracticeId(int practiceId) {
        logger.debug("尝试根据练习ID {} 获取题目列表。", practiceId);
        List<QuestionEntity> questions = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT q.* FROM question q " +
                    "JOIN practice_question pq ON q.question_id = pq.question_id " +
                    "WHERE pq.practice_id = ? ORDER BY pq.seq_no";
            logger.debug("执行 SQL: {} with practiceId = {}", sql, practiceId);
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
                logger.trace("找到题目: ID = {}, Type = {}, Content = '{}'", q.getId(), q.getType(), q.getContent());

                //特别处理选择题的选项
                if ("single_choice".equals(q.getType()) || "multiple_choice".equals(q.getType())) {
                    logger.trace("处理选择题选项，题目ID: {}", q.getId());
                    List<String> options = new ArrayList<>();
                    if (correct != null && !correct.trim().isEmpty()) {
                        Arrays.stream(correct.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .forEach(options::add);
                        logger.trace("添加正确选项: {}", correct);
                    }
                    //如果错误答案字符串不为null且不为空白
                    if (error != null && !error.trim().isEmpty()) {
                        //将错误答案字符串按逗号分割，去除首尾空白，过滤掉空字符串，然后添加到options列表中
                        Arrays.stream(error.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .forEach(options::add);
                        logger.trace("添加错误选项: {}", error);
                    }
                    //打乱选项的顺序，使其随机显示
                    Collections.shuffle(options);
                    q.setOptions(options);
                    logger.trace("打乱选项后: {}", options);
                } else {
                    //如果不是选择题，设置options属性为一个空的不可修改列表
                    q.setOptions(Collections.emptyList());
                }

                questions.add(q);
            }
            logger.debug("成功找到 {} 个题目与练习 ID {} 关联。", questions.size(), practiceId);
        } catch (SQLException e) {
            logger.error("根据练习ID {} 获取题目列表时发生数据库异常。", practiceId, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成根据练习ID {} 获取题目列表操作。", practiceId);
        return questions;
    }

    /**
     * 根据题目的唯一标识符ID查询单个题目的详细信息。
     * 这个方法执行数据库读取操作，返回一个QuestionEntity对象。
     * 除了题目的基本信息外，对于选择题，它还会处理选项的生成和随机排序。
     */
    public QuestionEntity getQuestionById(int questionId) {
        logger.debug("尝试根据题目ID {} 查询单个题目详细信息。", questionId);
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        QuestionEntity question = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT * FROM question WHERE question_id = ?";
            logger.debug("执行 SQL: {} with questionId = {}", sql, questionId);
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
                logger.debug("成功找到题目 ID {} 的详细信息。", questionId);
                logger.trace("题目详情: ID = {}, Type = {}, Content = '{}'", question.getId(), question.getType(), question.getContent());

                //检查题目类型是否是单选或多选
                if ("single_choice".equals(question.getType()) || "multiple_choice".equals(question.getType())) {
                    logger.debug("处理选择题选项，题目ID: {}", questionId);
                    List<String> options = new ArrayList<>();
                    String correct = question.getCorrectAnswer();
                    String error = question.getErrorAnswer();
                    if (correct != null && !correct.trim().isEmpty()) {
                        Arrays.stream(correct.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .forEach(options::add);
                        logger.trace("添加正确选项: {}", correct);
                    }
                    if (error != null && !error.trim().isEmpty()) {
                        Arrays.stream(error.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .forEach(options::add);
                        logger.trace("添加错误选项: {}", error);
                    }
                    //打乱选项的顺序，使其随机显示给用户
                    Collections.shuffle(options);
                    question.setOptions(options);
                    logger.trace("打乱选项后: {}", options);
                } else {
                    question.setOptions(Collections.emptyList());
                }
            } else {
                logger.debug("未找到题目 ID {} 的详细信息。", questionId);
            }
        } catch (SQLException e) {
            logger.error("根据题目ID {} 查询单个题目详细信息时发生数据库异常。", questionId, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成根据题目ID {} 查询单个题目详细信息操作。", questionId);
        return question;
    }

    /**
     * 获取所有题目信息
     * @return 题目实体列表，包含所有题目
     */
    public List<QuestionEntity> getAllQuestions() {
        logger.debug("尝试查询所有题目列表。");
        List<QuestionEntity> questions = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT * FROM question";
            logger.debug("执行 SQL: {}", sql);
            pstmt = conn.prepareStatement(sql);
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
                logger.trace("找到题目: ID = {}, Content = '{}'", q.getId(), q.getContent());
            }
            logger.debug("成功找到 {} 个题目。", questions.size());
        } catch (SQLException e) {
            logger.error("查询所有题目列表时发生数据库异常。", e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成查询所有题目列表操作。");
        return questions;
    }
}
