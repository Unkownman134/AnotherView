package io.github.gongding.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gongding.dao.SubmissionDao;
import io.github.gongding.entity.StudentEntity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/api/student/submit")
public class StudentSubmitAnswersServlet extends HttpServlet {
    private final ObjectMapper mapper = new ObjectMapper();
    private final SubmissionDao submissionDao = new SubmissionDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
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

        //声明一个Map变量用于存储从请求体JSON中解析出的数据
        Map<String, Object> requestData;
        try {
            //TypeReference用于提供目标Map的泛型类型信息
            requestData = mapper.readValue(req.getInputStream(), new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "请求数据格式错误");
            return;
        }

        //从解析出的requestData Map中获取名为"practiceId"的值
        Object practiceIdObj = requestData.get("practiceId");
        if (!(practiceIdObj instanceof Integer)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "practiceId 格式错误或缺失");
            return;
        }
        //将practiceIdObj转换为int类型的practiceId
        int practiceId = (Integer) practiceIdObj;

        if (practiceId <= 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "无效的练习ID");
            return;
        }

        Object answersObj = requestData.get("answers");
        if (!(answersObj instanceof List)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "answers 格式错误或缺失");
            return;
        }

        List<Map<String, Object>> answers;
        try {
            answers = mapper.convertValue(answersObj, new TypeReference<List<Map<String, Object>>>() {});
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "answers 内容格式不正确");
            return;
        }

        if (answers == null || answers.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "答案列表不能为空");
            return;
        }

        try {
            //创建提交记录并保存答案
            submissionDao.createSubmission(student.getId(), practiceId, answers);
            resp.getWriter().write(mapper.writeValueAsString(Map.of("message", "提交成功")));
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "提交失败：" + e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器内部错误，提交失败");
        }
    }
}