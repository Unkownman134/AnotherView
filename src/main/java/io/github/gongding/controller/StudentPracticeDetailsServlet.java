package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.gongding.dao.PracticeDao;
import io.github.gongding.entity.PracticeEntity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/student/practiceDetails")
public class StudentPracticeDetailsServlet extends HttpServlet {
    private final PracticeDao practiceDao = new PracticeDao();
    private final ObjectMapper mapper = new ObjectMapper();

    public StudentPracticeDetailsServlet() {
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=utf-8");

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未登录或会话已过期");
            return;
        }

        String practiceIdStr = req.getParameter("practice_id");
        if (practiceIdStr == null || practiceIdStr.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少练习ID参数");
            return;
        }

        try {
            int practiceId = Integer.parseInt(practiceIdStr);

            PracticeEntity practice = practiceDao.getPracticeById(practiceId);

            if (practice != null) {
                Map<String, Object> practiceDetails = new HashMap<>();
                practiceDetails.put("id", practice.getId());
                practiceDetails.put("title", practice.getTitle());
                practiceDetails.put("questionNum", practice.getQuestionNum());
                practiceDetails.put("startAt", practice.getStartAt());
                practiceDetails.put("endAt", practice.getEndAt());

                mapper.writeValue(resp.getWriter(), practiceDetails);
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到该练习");
            }

        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "练习ID格式不正确");
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器内部错误");
        }
    }
}
