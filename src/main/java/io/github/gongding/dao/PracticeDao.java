package io.github.gongding.dao;

import io.github.gongding.entity.PracticeEntity;
import io.github.gongding.util.DBUtils;
import io.github.gongding.util.PracticeStatusUtils;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PracticeDao {
    private static final Logger logger = LoggerFactory.getLogger(PracticeDao.class);

    /**
     * 创建一个新的练习及其关联的题目和班级
     * @param teacherId 教师ID
     * @param lessonId 课程ID
     * @param semesterId 学期ID
     * @param title 练习标题
     * @param classIds 关联的班级ID数组
     * @param classof 班级信息字符串
     * @param startTime 练习开始时间
     * @param endTime 练习结束时间
     * @param questionIds 包含的题目ID数组
     * @return 新创建练习的ID，如果创建失败则返回-1
     */
    public int createPractice(int teacherId, int lessonId, int semesterId, String title, int[] classIds, String classof, LocalDateTime startTime, LocalDateTime endTime, int[] questionIds) {
        logger.info("尝试创建新练习 - 标题: {}, 教师ID: {}, 课程ID: {}, 学期ID: {}", title, teacherId, lessonId, semesterId);
        logger.debug("练习详情 - 班级信息: {}, 开始时间: {}, 结束时间: {}, 题目ID数量: {}", classof, startTime, endTime, (questionIds != null ? questionIds.length : 0));

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int newPracticeId = -1;

        try {
            conn = DBUtils.getConnection();

            String insertPracticeSql = "INSERT INTO practice (lesson_id, teacher_id, semester_id, title, classof, start_time, end_time, question_num, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            logger.debug("执行 SQL (插入练习): {}", insertPracticeSql);
            pstmt = conn.prepareStatement(insertPracticeSql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setInt(1, lessonId);
            pstmt.setInt(2, teacherId);
            pstmt.setInt(3, semesterId);
            pstmt.setString(4, title);
            pstmt.setString(5, classof);
            pstmt.setTimestamp(6, Timestamp.valueOf(startTime));
            pstmt.setTimestamp(7, Timestamp.valueOf(endTime));
            pstmt.setInt(8, questionIds != null ? questionIds.length : 0);
            //使用PracticeStatusUtils计算当前练习状态
            String calculatedStatus = PracticeStatusUtils.calculateStatus(startTime, endTime);
            pstmt.setString(9, calculatedStatus);
            pstmt.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));

            int affectedRows = pstmt.executeUpdate();
            logger.debug("插入练习记录影响行数: {}", affectedRows);

            if (affectedRows > 0) {
                //获取自动生成的键
                rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    //获取生成的 practice_id
                    newPracticeId = rs.getInt(1);
                    logger.info("成功创建练习记录，新练习 ID: {}", newPracticeId);

                    if (questionIds != null && questionIds.length > 0) {
                        logger.debug("开始插入练习 {} 的题目关联，共 {} 个题目。", newPracticeId, questionIds.length);
                        String insertQuestionsSql = "INSERT INTO practice_question (practice_id, question_id, seq_no) VALUES (?, ?, ?)";
                        PreparedStatement questionPstmt = conn.prepareStatement(insertQuestionsSql);
                        //遍历题目ID数组，批量插入关联关系
                        for (int i = 0; i < questionIds.length; i++) {
                            questionPstmt.setInt(1, newPracticeId);
                            questionPstmt.setInt(2, questionIds[i]);
                            questionPstmt.setInt(3, i + 1);
                            questionPstmt.addBatch();
                            logger.trace("添加题目关联到批量操作 - 练习ID: {}, 题目ID: {}, 序号: {}", newPracticeId, questionIds[i], i + 1);
                        }
                        //执行批量插入
                        int[] questionBatchResult = questionPstmt.executeBatch();
                        logger.debug("批量插入题目关联结果 (每项影响行数): {}", questionBatchResult);
                        DBUtils.close(null, questionPstmt);
                    } else {
                        logger.debug("没有需要关联的题目。");
                    }

                    //如果有选择班级
                    if (classIds != null && classIds.length > 0) {
                        logger.debug("开始插入练习 {} 的班级关联，共 {} 个班级。", newPracticeId, classIds.length);
                        String insertClassesSql = "INSERT INTO practice_class (practice_id, class_id) VALUES (?, ?)";
                        PreparedStatement classPstmt = conn.prepareStatement(insertClassesSql);
                        //遍历班级ID数组，批量插入关联关系
                        for (int classId : classIds) {
                            classPstmt.setInt(1, newPracticeId);
                            classPstmt.setInt(2, classId);
                            classPstmt.addBatch();
                            logger.trace("添加班级关联到批量操作 - 练习ID: {}, 班级ID: {}", newPracticeId, classId);
                        }
                        int[] classBatchResult = classPstmt.executeBatch();
                        logger.debug("批量插入班级关联结果 (每项影响行数): {}", classBatchResult);
                        DBUtils.close(null, classPstmt);
                    } else {
                        logger.debug("没有需要关联的班级。");
                    }
                } else {
                    //如果影响行数大于0但未能获取生成的键，认为创建失败
                    newPracticeId = -1;
                    logger.error("创建练习记录失败，影响行数 > 0 但未能获取生成的键。");
                }
            } else {
                //如果影响行数不大于0，认为插入练习记录失败
                newPracticeId = -1;
                logger.error("创建练习记录失败，practice 表未插入行。");
            }
        } catch (SQLException e) {
            logger.error("创建练习过程中发生数据库异常。", e);
            newPracticeId = -1;
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.info("完成创建新练习操作，返回新练习 ID: {}", newPracticeId);
        return newPracticeId;
    }

    /**
     * 根据教师ID和学期ID查询练习列表
     * @param teacherId 教师ID
     * @param semesterId 学期ID
     * @return 返回匹配条件的练习列表，每个练习为一个Map对象
     */
    public List<Map<String, Object>> getPracticesByTeacherIdAndSemesterId(int teacherId, int semesterId) {
        logger.debug("尝试根据教师ID {} 和学期ID {} 查询练习列表。", teacherId, semesterId);
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Map<String, Object>> practicesData = new ArrayList<>();
        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT p.*, l.title as lesson_title FROM practice p JOIN lesson l ON p.lesson_id = l.lesson_id WHERE p.teacher_id = ? AND p.semester_id = ?";
            logger.debug("执行 SQL: {} with teacherId = {}, semesterId = {}", sql, teacherId, semesterId);
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, teacherId);
            pstmt.setInt(2, semesterId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                PracticeEntity practice = buildPracticeEntity(rs);
                String lessonName = rs.getString("lesson_title");

                //将PracticeEntity的字段和课程名称放入一个Map中
                Map<String, Object> practiceMap = new HashMap<>();
                practiceMap.put("id", practice.getId());
                practiceMap.put("lessonId", practice.getLessonId());
                practiceMap.put("teacherId", practice.getTeacherId());
                practiceMap.put("semesterId", practice.getSemesterId());
                practiceMap.put("title", practice.getTitle());
                practiceMap.put("questionNum", practice.getQuestionNum());
                practiceMap.put("classof", practice.getClassof());
                practiceMap.put("status", practice.getStatus());
                practiceMap.put("startAt", practice.getStartAt());
                practiceMap.put("endAt", practice.getEndAt());
                practiceMap.put("createdAt", practice.getCreatedAt());
                practiceMap.put("lessonName", lessonName); // 添加课程名称

                practicesData.add(practiceMap);
                logger.trace("找到练习: ID = {}, Title = {}, Lesson = {}", practice.getId(), practice.getTitle(), lessonName);
            }
            logger.debug("成功找到 {} 个练习与教师 ID {} 和学期 ID {} 关联。", practicesData.size(), teacherId, semesterId);
        } catch (SQLException e) {
            logger.error("根据教师ID {} 和学期ID {} 查询练习列表时发生数据库异常。", teacherId, semesterId, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成根据教师ID {} 和学期ID {} 查询练习列表操作。", teacherId, semesterId);
        return practicesData;
    }

    /**
     * 根据教师ID查询所有练习列表
     * @param teacherId 教师ID
     * @return 返回匹配条件的练习列表，每个练习为一个Map对象
     */
    public List<Map<String, Object>> getPracticesByTeacherId(int teacherId) {
        logger.debug("尝试根据教师ID {} 查询所有练习列表。", teacherId);
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Map<String, Object>> practicesData = new ArrayList<>(); // 返回 Map 列表
        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT p.*, l.title as lesson_title FROM practice p JOIN lesson l ON p.lesson_id = l.lesson_id WHERE p.teacher_id = ?";
            logger.debug("执行 SQL: {} with teacherId = {}", sql, teacherId);
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, teacherId);

            rs = pstmt.executeQuery();
            while (rs.next()) {
                //调用辅助方法构建PracticeEntity，虽然最终返回的是Map
                PracticeEntity practice = buildPracticeEntity(rs);
                String lessonName = rs.getString("lesson_title");

                Map<String, Object> practiceMap = new HashMap<>();
                practiceMap.put("id", practice.getId());
                practiceMap.put("lessonId", practice.getLessonId());
                practiceMap.put("teacherId", practice.getTeacherId());
                practiceMap.put("semesterId", practice.getSemesterId());
                practiceMap.put("title", practice.getTitle());
                practiceMap.put("questionNum", practice.getQuestionNum());
                practiceMap.put("classof", practice.getClassof());
                practiceMap.put("status", practice.getStatus());
                practiceMap.put("startAt", practice.getStartAt());
                practiceMap.put("endAt", practice.getEndAt());
                practiceMap.put("createdAt", practice.getCreatedAt());
                practiceMap.put("lessonName", lessonName); // 添加课程名称

                practicesData.add(practiceMap);
                logger.trace("找到练习: ID = {}, Title = {}, Lesson = {}", practice.getId(), practice.getTitle(), lessonName);
            }
            logger.debug("成功找到 {} 个练习与教师 ID {} 关联。", practicesData.size(), teacherId);
        } catch (SQLException e) {
            logger.error("根据教师ID {} 查询所有练习列表时发生数据库异常。", teacherId, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成根据教师ID {} 查询所有练习列表操作。", teacherId);
        return practicesData;
    }

    /**
     * 根据教师ID和搜索词查询练习列表
     * 在练习标题、课程标题或班级信息字符串中进行模糊匹配
     * @param teacherId 教师ID
     * @param searchTerm 搜索词
     * @return 返回匹配条件的练习列表，如果找不到则返回空列表
     */
    public List<Map<String, Object>> getPracticesByTeacherIdAndSearchTerm(int teacherId, String searchTerm) {
        logger.debug("尝试根据教师ID {} 和搜索词 '{}' 查询练习列表。", teacherId, searchTerm);
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Map<String, Object>> practicesData = new ArrayList<>();
        try {
            conn = DBUtils.getConnection();
            //同时在 practice.title,lesson.title,practice.classof字段中使用LIKE进行模糊匹配
            String sql = "SELECT p.*, l.title as lesson_title FROM practice p JOIN lesson l ON p.lesson_id = l.lesson_id WHERE p.teacher_id = ? AND (p.title LIKE ? OR l.title LIKE ? OR p.classof LIKE ?)"; // Added p.classof
            logger.debug("执行 SQL: {} with teacherId = {}, searchTerm = '{}'", sql, teacherId, searchTerm);
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, teacherId);
            //设置搜索词的占位符值，使用%进行模糊匹配
            pstmt.setString(2, "%" + searchTerm + "%");
            pstmt.setString(3, "%" + searchTerm + "%");
            pstmt.setString(4, "%" + searchTerm + "%");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                PracticeEntity practice = buildPracticeEntity(rs);
                String lessonName = rs.getString("lesson_title");

                Map<String, Object> practiceMap = new HashMap<>();
                practiceMap.put("id", practice.getId());
                practiceMap.put("lessonId", practice.getLessonId());
                practiceMap.put("teacherId", practice.getTeacherId());
                practiceMap.put("semesterId", practice.getSemesterId());
                practiceMap.put("title", practice.getTitle());
                practiceMap.put("questionNum", practice.getQuestionNum());
                practiceMap.put("classof", practice.getClassof());
                practiceMap.put("status", practice.getStatus());
                practiceMap.put("startAt", practice.getStartAt());
                practiceMap.put("endAt", practice.getEndAt());
                practiceMap.put("createdAt", practice.getCreatedAt());
                practiceMap.put("lessonName", lessonName);

                practicesData.add(practiceMap);
                logger.trace("找到匹配搜索词的练习: ID = {}, Title = {}, Lesson = {}", practice.getId(), practice.getTitle(), lessonName); // TRACE级别
            }
            logger.debug("成功找到 {} 个与搜索词 '{}' 匹配的练习。", practicesData.size(), searchTerm);
        } catch (SQLException e) {
            logger.error("根据教师ID {} 和搜索词 '{}' 查询练习列表时发生数据库异常。", teacherId, searchTerm, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成根据教师ID {} 和搜索词 '{}' 查询练习列表操作。", teacherId, searchTerm);
        return practicesData;
    }

    /**
     * 辅助方法：从ResultSet中构建PracticeEntity对象
     * @param rs 结果集
     * @return 构建好的PracticeEntity对象
     * @throws SQLException 如果访问结果集发生错误
     */
    private PracticeEntity buildPracticeEntity(ResultSet rs) throws SQLException {
        logger.trace("从 ResultSet 构建 PracticeEntity 对象。");
        PracticeEntity practice = new PracticeEntity();
        try {
            practice.setId(rs.getInt("practice_id"));
            practice.setLessonId(rs.getInt("lesson_id"));
            practice.setTeacherId(rs.getInt("teacher_id"));
            practice.setTitle(rs.getString("title"));
            practice.setQuestionNum(rs.getInt("question_num"));
            Timestamp startTs = rs.getTimestamp("start_time");
            practice.setStartAt(startTs != null ? startTs.toLocalDateTime() : null);
            Timestamp endTs = rs.getTimestamp("end_time");
            practice.setEndAt(endTs != null ? endTs.toLocalDateTime() : null);
            practice.setStatus(rs.getString("status"));
            practice.setClassof(rs.getString("classof"));
            practice.setSemesterId(rs.getInt("semester_id"));
            logger.trace("成功构建 PracticeEntity: ID = {}, Title = {}", practice.getId(), practice.getTitle());
        } catch (SQLException e) {
            logger.error("从 ResultSet 构建 PracticeEntity 时发生 SQLException。", e);
            throw e;
        }
        return practice;
    }

    /**
     * 延长练习的截止时间
     * 这个方法执行更新操作
     * @param practiceId 练习ID
     * @param newEndTime 新的截止时间
     */
    public void extendPracticeTime(int practiceId, LocalDateTime newEndTime) {
        logger.info("尝试延长练习 ID {} 的截止时间到 {}", practiceId, newEndTime);
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtils.getConnection();
            String sql = "UPDATE practice SET end_time = ? WHERE practice_id = ?";
            logger.debug("执行 SQL (更新截止时间): {} with endTime = {}, practiceId = {}", sql, newEndTime, practiceId);
            pstmt = conn.prepareStatement(sql);
            pstmt.setTimestamp(1, Timestamp.valueOf(newEndTime));
            pstmt.setInt(2, practiceId);
            int affectedRows = pstmt.executeUpdate();
            logger.debug("更新截止时间影响行数: {}", affectedRows);

            if (affectedRows > 0) {
                logger.debug("成功更新练习 ID {} 的截止时间。", practiceId);
                updatePracticeStatus(practiceId);
            } else {
                logger.warn("更新练习 ID {} 的截止时间失败，可能该练习不存在。", practiceId);
            }

        } catch (SQLException e) {
            logger.error("延长练习 ID {} 的截止时间时发生数据库异常。", practiceId, e);
        } finally {
            DBUtils.close(conn, pstmt);
            logger.debug("关闭数据库资源。");
        }
        logger.info("完成延长练习 ID {} 截止时间操作。", practiceId);
    }

    /**
     * 根据练习ID更新练习的状态
     * 这个方法执行读取和可能的更新操作
     * 它首先获取练习的开始时间和结束时间，然后计算新的状态，如果状态发生变化则更新数据库
     * @param practiceId 练习ID
     */
    public void updatePracticeStatus(int practiceId) {
        logger.debug("尝试更新练习 ID {} 的状态。", practiceId);
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtils.getConnection();
            String selectSql = "SELECT start_time, end_time, status FROM practice WHERE practice_id = ?";
            logger.debug("执行 SQL (查询状态): {} with practiceId = {}", selectSql, practiceId);
            pstmt = conn.prepareStatement(selectSql);
            pstmt.setInt(1, practiceId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                Timestamp startTs = rs.getTimestamp("start_time");
                LocalDateTime startTime = startTs != null ? startTs.toLocalDateTime() : null;
                Timestamp endTs = rs.getTimestamp("end_time");
                LocalDateTime endTime = endTs != null ? endTs.toLocalDateTime() : null;
                String currentStatus = rs.getString("status");

                logger.debug("练习 ID {} 当前状态: {}, 开始时间: {}, 结束时间: {}", practiceId, currentStatus, startTime, endTime);

                String newStatus = PracticeStatusUtils.calculateStatus(startTime, endTime);
                logger.debug("练习 ID {} 计算出的新状态: {}", practiceId, newStatus);

                if (!newStatus.equals(currentStatus)) {
                    logger.info("练习 ID {} 状态发生变化，从 '{}' 更新为 '{}'。", practiceId, currentStatus, newStatus);
                    String updateSql = "UPDATE practice SET status = ? WHERE practice_id = ?";
                    logger.debug("执行 SQL (更新状态): {} with status = {}, practiceId = {}", updateSql, newStatus, practiceId);
                    PreparedStatement updatePstmt = conn.prepareStatement(updateSql);
                    updatePstmt.setString(1, newStatus);
                    updatePstmt.setInt(2, practiceId);
                    int affectedRows = updatePstmt.executeUpdate();
                    logger.debug("更新状态影响行数: {}", affectedRows);
                    DBUtils.close(null, updatePstmt);
                } else {
                    logger.debug("练习 ID {} 状态未变化，无需更新。", practiceId);
                }
            } else {
                logger.warn("更新练习状态失败，未找到练习 ID {}。", practiceId);
            }
        } catch (SQLException e) {
            logger.error("更新练习 ID {} 状态时发生数据库异常。", practiceId, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成更新练习 ID {} 状态操作。", practiceId);
    }

    /**
     * 修改练习的基本信息和关联的题目列表。
     * 这个方法会更新practice表中的基本字段，删除practice_question表中旧的题目关联，然后插入新的题目关联。
     *
     * @param practiceId 要修改的练习的唯一标识符ID
     * @param title 新的练习标题
     * @param classof 新的班级信息字符串
     * @param startTime 新的练习开始时间
     * @param endTime 新的练习结束时间
     * @param questionIds 包含新的题目ID的数组
     * @return 如果练习基本信息更新成功且题目关联操作完成，返回true；
     * 如果在任何数据库操作中发生 SQLException 或 practice 基本信息更新失败，返回false。
     */
    public boolean updatePracticeAndQuestions(int practiceId, String title, String classof, LocalDateTime startTime, LocalDateTime endTime, int[] questionIds) {
        logger.info("尝试修改练习 ID {} 的信息和题目。", practiceId);
        logger.debug("新信息 - 标题: {}, 班级信息: {}, 开始时间: {}, 结束时间: {}, 题目ID数量: {}", title, classof, startTime, endTime, (questionIds != null ? questionIds.length : 0));

        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DBUtils.getConnection();

            //用于更新practice表中指定练习的基本信息字段
            String updatePracticeSql = "UPDATE practice SET title = ?, classof = ?, start_time = ?, end_time = ?, question_num = ? WHERE practice_id = ?";
            logger.debug("执行 SQL (更新练习基本信息): {}", updatePracticeSql);
            pstmt = conn.prepareStatement(updatePracticeSql);
            pstmt.setString(1, title);
            pstmt.setString(2, classof);
            pstmt.setTimestamp(3, Timestamp.valueOf(startTime));
            pstmt.setTimestamp(4, Timestamp.valueOf(endTime));
            pstmt.setInt(5, questionIds != null ? questionIds.length : 0);
            pstmt.setInt(6, practiceId);
            int affectedRows = pstmt.executeUpdate();
            logger.debug("更新练习基本信息影响行数: {}", affectedRows);

            if (affectedRows > 0) {
                logger.debug("成功更新练习 ID {} 的基本信息。", practiceId);
                //用于删除practice_question表中与当前练习ID关联的所有题目记录
                logger.debug("删除练习 ID {} 旧的题目关联。", practiceId);
                String deleteQuestionsSql = "DELETE FROM practice_question WHERE practice_id = ?";
                PreparedStatement deletePstmt = conn.prepareStatement(deleteQuestionsSql);
                deletePstmt.setInt(1, practiceId);
                int deletedRows = deletePstmt.executeUpdate();
                logger.debug("删除旧题目关联影响行数: {}", deletedRows);
                DBUtils.close(null, deletePstmt);

                //如果提供了新的题目ID数组
                if (questionIds != null && questionIds.length > 0) {
                    logger.debug("开始插入练习 {} 新的题目关联，共 {} 个题目。", practiceId, questionIds.length);
                    //用于向practice_question表中批量插入新的题目关联记录
                    String insertQuestionsSql = "INSERT INTO practice_question (practice_id, question_id, seq_no) VALUES (?, ?, ?)";
                    PreparedStatement insertPstmt = conn.prepareStatement(insertQuestionsSql);
                    for (int i = 0; i < questionIds.length; i++) {
                        insertPstmt.setInt(1, practiceId);
                        insertPstmt.setInt(2, questionIds[i]);
                        insertPstmt.setInt(3, i + 1);
                        insertPstmt.addBatch();
                        logger.trace("添加新题目关联到批量操作 - 练习ID: {}, 题目ID: {}, 序号: {}", practiceId, questionIds[i], i + 1);
                    }
                    int[] insertedRows = insertPstmt.executeBatch();
                    logger.debug("批量插入新题目关联结果 (每项影响行数): {}", insertedRows);
                    DBUtils.close(null, insertPstmt);
                } else {
                    logger.debug("没有需要关联的新题目。");
                }
                success = true;

                //根据新的开始和结束时间重新计算并更新练习的状态，确保状态字段是最新的
                updatePracticeStatus(practiceId);
            } else {
                logger.warn("修改练习 ID {} 基本信息失败，可能该练习不存在。", practiceId);
            }
        } catch (SQLException e) {
            logger.error("修改练习 ID {} 信息和题目时发生数据库异常。", practiceId, e);
        } finally {
            DBUtils.close(conn, pstmt);
            logger.debug("关闭数据库资源。");
        }
        logger.info("完成修改练习 ID {} 信息和题目操作，结果: {}", practiceId, success ? "成功" : "失败");
        return success;
    }

    /**
     * 从现有练习复用创建新的练习记录。
     * 这个方法主要复制原有练习的题目，并设置新的基本信息。
     * @param teacherId 创建新练习的教师ID
     * @param lessonId 新练习所属的课程ID
     * @param semesterId 新练习所属的学期ID
     * @param newTitle 新练习的标题
     * @param classIds 新练习关联的班级ID数组
     * @param newStartTime 新练习的开始时间
     * @param newEndTime 新练习的结束时间
     * @param questionIds 新练习包含的题目ID数组
     * @return 新创建练习的ID，如果创建失败则返回-1。
     */
    public int createPracticeFromReuse(int teacherId, int lessonId, int semesterId, String newTitle, int[] classIds, LocalDateTime newStartTime, LocalDateTime newEndTime, int[] questionIds) {
        logger.info("尝试从复用创建新练习 - 标题: {}, 教师ID: {}, 课程ID: {}, 学期ID: {}", newTitle, teacherId, lessonId, semesterId);
        logger.debug("新练习详情 - 开始时间: {}, 结束时间: {}, 题目ID数量: {}", newStartTime, newEndTime, (questionIds != null ? questionIds.length : 0));

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int newPracticeId = -1;

        try {
            conn = DBUtils.getConnection();

            //新练习记录的SQL语句
            String insertPracticeSql = "INSERT INTO practice (lesson_id, teacher_id, semester_id, title, classof, start_time, end_time, question_num, status, created_at) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            logger.debug("执行 SQL (插入新练习): {}", insertPracticeSql);
            pstmt = conn.prepareStatement(insertPracticeSql, Statement.RETURN_GENERATED_KEYS);

            pstmt.setInt(1, lessonId);
            pstmt.setInt(2, teacherId);
            pstmt.setInt(3, semesterId);
            pstmt.setString(4, newTitle);
            pstmt.setString(5, "");
            pstmt.setTimestamp(6, Timestamp.valueOf(newStartTime));
            pstmt.setTimestamp(7, Timestamp.valueOf(newEndTime));
            pstmt.setInt(8, questionIds != null ? questionIds.length : 0);
            String calculatedStatus = PracticeStatusUtils.calculateStatus(newStartTime, newEndTime);
            pstmt.setString(9, calculatedStatus);
            pstmt.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));

            int affectedRows = pstmt.executeUpdate();
            logger.debug("插入新练习记录影响行数: {}", affectedRows);

            if (affectedRows > 0) {
                rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    //获取新创建练习的ID
                    newPracticeId = rs.getInt(1);
                    logger.info("成功从复用创建新练习，新练习 ID: {}", newPracticeId);

                    if (questionIds != null && questionIds.length > 0) {
                        logger.debug("开始插入新练习 {} 的题目关联，共 {} 个题目。", newPracticeId, questionIds.length);
                        //用于向practice_question表批量插入新练习与题目的关联记录
                        String insertQuestionsSql = "INSERT INTO practice_question (practice_id, question_id, seq_no) VALUES (?, ?, ?)";
                        PreparedStatement questionPstmt = conn.prepareStatement(insertQuestionsSql);
                        for (int i = 0; i < questionIds.length; i++) {
                            questionPstmt.setInt(1, newPracticeId);
                            questionPstmt.setInt(2, questionIds[i]);
                            questionPstmt.setInt(3, i + 1);
                            questionPstmt.addBatch();
                            logger.trace("添加题目关联到批量操作 - 练习ID: {}, 题目ID: {}, 序号: {}", newPracticeId, questionIds[i], i + 1);
                        }
                        int[] questionBatchResult = questionPstmt.executeBatch();
                        logger.debug("批量插入题目关联结果 (每项影响行数): {}", questionBatchResult);
                        DBUtils.close(null, questionPstmt);
                    } else {
                        logger.debug("没有需要关联的题目。");
                    }

                    if (classIds != null && classIds.length > 0) {
                        logger.debug("开始插入新练习 {} 的班级关联，共 {} 个班级。", newPracticeId, classIds.length);
                        //用于向practice_class表批量插入新练习与班级的关联记录
                        String insertClassesSql = "INSERT INTO practice_class (practice_id, class_id) VALUES (?, ?)";
                        PreparedStatement classPstmt = conn.prepareStatement(insertClassesSql);
                        for (int classId : classIds) {
                            classPstmt.setInt(1, newPracticeId);
                            classPstmt.setInt(2, classId);
                            classPstmt.addBatch();
                            logger.trace("添加班级关联到批量操作 - 练习ID: {}, 班级ID: {}", newPracticeId, classId);
                        }
                        int[] classBatchResult = classPstmt.executeBatch();
                        logger.debug("批量插入班级关联结果 (每项影响行数): {}", classBatchResult);
                        DBUtils.close(null, classPstmt);
                    } else {
                        logger.debug("没有需要关联的班级。");
                    }

                } else {
                    newPracticeId = -1;
                    logger.error("从复用创建练习记录失败，影响行数 > 0 但未能获取生成的键。");
                }
            } else {
                newPracticeId = -1;
                logger.error("从复用创建练习记录失败，practice 表未插入行。");
            }
        } catch (SQLException e) {
            logger.error("从复用创建练习过程中发生数据库异常。", e);
            newPracticeId = -1;
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.info("完成从复用创建新练习操作，返回新练习 ID: {}", newPracticeId);
        return newPracticeId;
    }

    /**
     * 根据练习的唯一标识符ID查询单个练习的详细信息。
     * 这个方法执行数据库读取操作，返回一个PracticeEntity对象。
     *
     * @param practiceId 要查询的练习的唯一标识符ID。
     * @return 如果找到匹配的练习，返回一个PracticeEntity对象；如果找不到或发生SQL异常，返回null。
     */
    public PracticeEntity getPracticeById(int practiceId) {
        logger.debug("尝试根据练习ID {} 查询单个练习的详细信息。", practiceId);
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        PracticeEntity practice = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT * FROM practice WHERE practice_id = ?";
            logger.debug("执行 SQL: {} with practiceId = {}", sql, practiceId);
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, practiceId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                practice = buildPracticeEntity(rs);
                logger.debug("成功找到练习 ID {} 的详细信息。", practiceId);
            } else {
                logger.debug("未找到练习 ID {} 的详细信息。", practiceId);
            }
        } catch (SQLException e) {
            logger.error("根据练习ID {} 查询单个练习详细信息时发生数据库异常。", practiceId, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成根据练习ID {} 查询单个练习详细信息操作。", practiceId);
        return practice;
    }

    /**
     * 根据课程ID获取练习列表
     * @param lessonId 课程ID
     * @return 练习列表
     */
    public List<PracticeEntity> getPracticesByLessonId(int lessonId) {
        logger.debug("尝试根据课程ID {} 获取练习列表。", lessonId);
        List<PracticeEntity> practices = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT * FROM practice WHERE lesson_id = ?";
            logger.debug("执行 SQL: {} with lessonId = {}", sql, lessonId);
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, lessonId);

            rs = pstmt.executeQuery();
            while (rs.next()) {
                PracticeEntity practice = buildPracticeEntity(rs);
                //TODO
                // updatePracticeStatus(practice.getId());
                practices.add(practice);
                logger.trace("找到练习: ID = {}, Title = {}, Lesson ID = {}", practice.getId(), practice.getTitle(), lessonId);
            }
            logger.debug("成功找到 {} 个练习与课程 ID {} 关联。", practices.size(), lessonId);
        } catch (SQLException e) {
            logger.error("根据课程ID {} 获取练习列表时发生数据库异常。", lessonId, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成根据课程ID {} 获取练习列表操作。", lessonId);
        return practices;
    }
}
