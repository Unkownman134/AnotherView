package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.gongding.dao.PracticeDao;
import io.github.gongding.entity.TeacherEntity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/api/teacher/practices")
public class TeacherPracticesServlet extends HttpServlet {
    private final PracticeDao practiceDao = new PracticeDao();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("teacher") == null) {
            resp.sendError(401);
            return;
        }

        TeacherEntity teacher = (TeacherEntity) session.getAttribute("teacher");
        String semesterIdStr = req.getParameter("semesterId");
        String searchTerm = req.getParameter("searchTerm");

        List<Map<String, Object>> practicesData;

        if (semesterIdStr != null && !semesterIdStr.isEmpty() && !semesterIdStr.equals("all")) {
            try {
                int semesterId = Integer.parseInt(semesterIdStr);
                practicesData = practiceDao.getPracticesByTeacherIdAndSemesterId(teacher.getId(), semesterId);
            } catch (NumberFormatException e) {
                resp.sendError(400, "Invalid semester ID");
                return;
            }
        } else if (searchTerm != null && !searchTerm.isEmpty()) {
            practicesData = practiceDao.getPracticesByTeacherIdAndSearchTerm(teacher.getId(), searchTerm);
        } else {
            practicesData = practiceDao.getPracticesByTeacherId(teacher.getId());
        }

        resp.setContentType("application/json;charset=utf-8");
        mapper.writeValue(resp.getWriter(), practicesData);
    }
}