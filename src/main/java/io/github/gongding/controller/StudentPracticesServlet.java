package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.gongding.dao.PracticeDao;
import io.github.gongding.dao.SubmissionDao;
import io.github.gongding.entity.PracticeEntity;
import io.github.gongding.entity.StudentEntity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/student/practices")
public class StudentPracticesServlet extends HttpServlet {
    private final PracticeDao practiceDao = new PracticeDao();
    private final SubmissionDao submissionDao = new SubmissionDao();
    private final ObjectMapper mapper = new ObjectMapper();

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            resp.sendError(401, "未登录");
            return;
        }

        StudentEntity student = (StudentEntity) session.getAttribute("student");
        int studentId = student.getId();

        try {
            //从请求参数中获取名为"lessonId" 的值，即课程的ID字符串
            String lessonIdStr = req.getParameter("lesson_id");
            if (lessonIdStr == null || lessonIdStr.isEmpty()) {
                resp.sendError(400, "缺少课程ID参数");
                return;
            }
            int lessonId = Integer.parseInt(lessonIdStr);

            //获取指定课程下的练习列表，并包含学生的提交信息
            List<PracticeEntity> practices = practiceDao.getPracticesByLessonId(lessonId);

            List<Map<String, Object>> practicesData = new ArrayList<>();

            LocalDateTime now = LocalDateTime.now();

            //遍历获取到的练习列表
            for (PracticeEntity practice : practices) {
                //创建一个HashMap，用于存储当前练习的详细信息和学生完成情况
                Map<String, Object> practiceMap = new HashMap<>();
                practiceMap.put("id", practice.getId());
                practiceMap.put("title", practice.getTitle());
                practiceMap.put("questionNum", practice.getQuestionNum());
                practiceMap.put("startAt", practice.getStartAt());
                practiceMap.put("endAt", practice.getEndAt());

                String status;
                if (practice.getStartAt() != null && now.isBefore(practice.getStartAt())) {
                    status = "not_started";
                } else if (practice.getEndAt() != null && now.isAfter(practice.getEndAt())) {
                    status = "ended";
                } else {
                    status = "in_progress";
                }
                practiceMap.put("status", status);

                int completedQuestions = 0;
                double obtainedScore = 0.0;
                double totalScore = 0.0;

                if (!"not_started".equals(status)) {
                    //获取学生已完成的题目数量
                    completedQuestions = submissionDao.getStudentCompletedQuestionCount(studentId, practice.getId());
                    practiceMap.put("completedQuestions", completedQuestions);

                    if ("ended".equals(status)) {
                        //获取学生在该练习中获得的得分
                        obtainedScore = submissionDao.getStudentObtainedScore(studentId, practice.getId());
                        //获取该练习的总分
                        totalScore = submissionDao.getPracticeTotalScore(practice.getId());

                        practiceMap.put("obtainedScore", obtainedScore);
                        practiceMap.put("totalScore", totalScore);
                    } else {
                        //如果练习未结束，得分和总分设为null
                        practiceMap.put("obtainedScore", null);
                        practiceMap.put("totalScore", null);
                    }
                } else {
                    //如果学生未提交，已完成题目数量、得分和总分都设为默认值或null
                    practiceMap.put("completedQuestions", 0);
                    practiceMap.put("obtainedScore", null);
                    practiceMap.put("totalScore", null);
                }

                practicesData.add(practiceMap);
            }

            resp.setContentType("application/json;charset=utf-8");
            mapper.registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .writeValue(resp.getWriter(), practicesData);

        } catch (NumberFormatException e) {
            resp.sendError(400, "参数错误: 无效的课程ID");
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(500, "服务器错误: " + e.getMessage());
        }
    }
}
