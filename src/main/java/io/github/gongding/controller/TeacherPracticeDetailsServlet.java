package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.gongding.dao.*;
import io.github.gongding.entity.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/teacher/practice/details")
public class TeacherPracticeDetailsServlet extends HttpServlet {
    private final PracticeDao practiceDao = new PracticeDao();
    private final LessonDao lessonDao = new LessonDao();
    private final SemesterDao semesterDao = new SemesterDao();
    private final QuestionDao questionDao = new QuestionDao();
    private final TeacherDao teacherDao = new TeacherDao();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("teacher") == null) {
            resp.sendError(401, "Unauthorized");
            return;
        }

        TeacherEntity loggedInTeacher = (TeacherEntity) session.getAttribute("teacher");
        //尝试将practiceIdStr字符串解析为整数类型的practiceId
        int teacherId = loggedInTeacher.getId();
        String practiceIdStr = req.getParameter("id");

        if (practiceIdStr == null || practiceIdStr.isEmpty()) {
            resp.sendError(400, "Missing practice ID");
            return;
        }

        try {
            //尝试将practiceIdStr字符串解析为整数类型的practiceId
            int practiceId = Integer.parseInt(practiceIdStr);

            //根据练习ID从数据库获取PracticeEntity对象
            PracticeEntity practice = practiceDao.getPracticeById(practiceId);

            //检查是否找到了练习并且该练习的教师ID与当前登录教师的ID一致
            if (practice == null || practice.getTeacherId() != teacherId) {
                resp.sendError(404, "Practice not found or not accessible");
                return;
            }

            //如果练习存在且属于当前教师，则继续获取关联的详细信息
            LessonEntity lesson = lessonDao.getLessonById(practice.getLessonId());
            SemesterEntity semester = semesterDao.getSemesterById(practice.getSemesterId());
            TeacherEntity teacher = teacherDao.getTeacherById(practice.getTeacherId());
            //获取练习包含的题目列表
            List<QuestionEntity> questions = questionDao.getQuestionsByPracticeId(practiceId);

            Map<String, Object> practiceDetails = new HashMap<>();
            practiceDetails.put("id", practice.getId());
            practiceDetails.put("title", practice.getTitle());
            practiceDetails.put("questionNum", practice.getQuestionNum());
            practiceDetails.put("classof", practice.getClassof());
            practiceDetails.put("startAt", practice.getStartAt());
            practiceDetails.put("endAt", practice.getEndAt());
            practiceDetails.put("status", practice.getStatus());
            practiceDetails.put("lessonId", practice.getLessonId());
            practiceDetails.put("semesterId", practice.getSemesterId());
            practiceDetails.put("teacherId", practice.getTeacherId());


            practiceDetails.put("lessonName", lesson != null ? lesson.getTitle() : "未知课程");
            practiceDetails.put("semesterName", semester != null ? semester.getName() : "未知学期");
            practiceDetails.put("teacherName", teacher != null ? teacher.getName() : "未知教师");

            practiceDetails.put("questions", questions);

            resp.setContentType("application/json;charset=utf-8");
            mapper.writeValue(resp.getWriter(), practiceDetails);

        } catch (NumberFormatException e) {
            resp.sendError(400, "Invalid practice ID format");
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(500, "Error fetching practice details: " + e.getMessage());
        }
    }
}
