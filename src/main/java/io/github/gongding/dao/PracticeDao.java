package io.github.gongding.dao;

import io.github.gongding.util.DBUtils;
import io.github.gongding.util.PracticeStatusUtils;

import java.sql.*;
import java.time.LocalDateTime;

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
}