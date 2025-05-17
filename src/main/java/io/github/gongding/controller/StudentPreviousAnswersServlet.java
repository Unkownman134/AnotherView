package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gongding.dao.SubmissionDao;
import io.github.gongding.entity.StudentEntity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/student/previous-answers")
public class StudentPreviousAnswersServlet extends HttpServlet {
    private final ObjectMapper mapper = new ObjectMapper();
    private final SubmissionDao submissionDao = new SubmissionDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");

        HttpSession session = req.getSession(false);
        if (session == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "用户未登录或会话已过期");
            return;
        }

        StudentEntity student = (StudentEntity) session.getAttribute("student");
        if (student == null || student.getId() <= 0) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "学生信息无效，请重新登录");
            return;
        }

        String practiceIdStr = req.getParameter("practice_id");
        if (practiceIdStr == null || practiceIdStr.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少练习ID参数");
            return;
        }

        int practiceId;
        try {
            practiceId = Integer.parseInt(practiceIdStr);
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "练习ID格式不正确");
            return;
        }

        if (practiceId <= 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "无效的练习ID");
            return;
        }

        try {
            Map<String, Object> submission = submissionDao.getLatestSubmission(student.getId(), practiceId);

            if (submission == null || submission.isEmpty()) {
                resp.getWriter().write("{}");
                return;
            }

            resp.getWriter().write(mapper.writeValueAsString(submission));
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器内部错误：" + e.getMessage());
        }
    }
}