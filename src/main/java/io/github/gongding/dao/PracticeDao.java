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

public class PracticeDao {
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
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int newPracticeId = -1;

        try {
            conn = DBUtils.getConnection();
            String insertPracticeSql = "INSERT INTO practice (lesson_id, teacher_id, semester_id, title, classof, start_time, end_time, question_num, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
            if (affectedRows > 0) {
                //获取自动生成的键
                rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    //获取生成的 practice_id
                    newPracticeId = rs.getInt(1);
                    if (questionIds != null && questionIds.length > 0) {
                        String insertQuestionsSql = "INSERT INTO practice_question (practice_id, question_id, seq_no) VALUES (?, ?, ?)";
                        PreparedStatement questionPstmt = conn.prepareStatement(insertQuestionsSql);
                        //遍历题目ID数组，批量插入关联关系
                        for (int i = 0; i < questionIds.length; i++) {
                            questionPstmt.setInt(1, newPracticeId);
                            questionPstmt.setInt(2, questionIds[i]);
                            questionPstmt.setInt(3, i + 1);
                            questionPstmt.addBatch();
                        }
                        //执行批量插入
                        questionPstmt.executeBatch();
                        DBUtils.close(null, questionPstmt);
                    }
                    //如果有选择班级
                    if (classIds != null && classIds.length > 0) {
                        String insertClassesSql = "INSERT INTO practice_class (practice_id, class_id) VALUES (?, ?)";
                        PreparedStatement classPstmt = conn.prepareStatement(insertClassesSql);
                        //遍历班级ID数组，批量插入关联关系
                        for (int classId : classIds) {
                            classPstmt.setInt(1, newPracticeId);
                            classPstmt.setInt(2, classId);
                            classPstmt.addBatch();
                        }
                        classPstmt.executeBatch();
                        DBUtils.close(null, classPstmt);
                    }
                } else {
                    //如果影响行数大于0但未能获取生成的键，认为创建失败
                    newPracticeId = -1;
                }
            } else {
                //如果影响行数不大于0，认为插入练习记录失败
                newPracticeId = -1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            newPracticeId = -1;
        } finally {
            DBUtils.close(conn, pstmt, rs);
        }
        return newPracticeId;
    }

    /**
     * 根据教师ID和学期ID查询练习列表
     * @param teacherId 教师ID
     * @param semesterId 学期ID
     * @return 返回匹配条件的练习列表，每个练习为一个Map对象
     */
    public List<Map<String, Object>> getPracticesByTeacherIdAndSemesterId(int teacherId, int semesterId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Map<String, Object>> practicesData = new ArrayList<>();
        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT p.*, l.title as lesson_title FROM practice p JOIN lesson l ON p.lesson_id = l.lesson_id WHERE p.teacher_id = ? AND p.semester_id = ?";
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt, rs);
        }
        return practicesData;
    }

    /**
     * 根据教师ID查询所有练习列表
     * @param teacherId 教师ID
     * @return 返回匹配条件的练习列表，每个练习为一个Map对象
     */
    public List<Map<String, Object>> getPracticesByTeacherId(int teacherId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Map<String, Object>> practicesData = new ArrayList<>(); // 返回 Map 列表
        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT p.*, l.title as lesson_title FROM practice p JOIN lesson l ON p.lesson_id = l.lesson_id WHERE p.teacher_id = ?";
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt, rs);
        }
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
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Map<String, Object>> practicesData = new ArrayList<>();
        try {
            conn = DBUtils.getConnection();
            //同时在 practice.title,lesson.title,practice.classof字段中使用LIKE进行模糊匹配
            String sql = "SELECT p.*, l.title as lesson_title FROM practice p JOIN lesson l ON p.lesson_id = l.lesson_id WHERE p.teacher_id = ? AND (p.title LIKE ? OR l.title LIKE ? OR p.classof LIKE ?)"; // Added p.classof
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt, rs);
        }
        return practicesData;
    }

    /**
     * 辅助方法：从ResultSet中构建PracticeEntity对象
     * @param rs 结果集
     * @return 构建好的PracticeEntity对象
     * @throws SQLException 如果访问结果集发生错误
     */
    private PracticeEntity buildPracticeEntity(ResultSet rs) throws SQLException {
        PracticeEntity practice = new PracticeEntity();
        practice.setId(rs.getInt("practice_id"));
        practice.setLessonId(rs.getInt("lesson_id"));
        practice.setTeacherId(rs.getInt("teacher_id"));
        practice.setTitle(rs.getString("title"));
        practice.setQuestionNum(rs.getInt("question_num"));
        practice.setStartAt(rs.getTimestamp("start_time").toLocalDateTime());
        practice.setEndAt(rs.getTimestamp("end_time").toLocalDateTime());
        practice.setStatus(rs.getString("status"));
        practice.setClassof(rs.getString("classof"));
        practice.setSemesterId(rs.getInt("semester_id"));
        return practice;
    }
}