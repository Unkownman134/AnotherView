package io.github.gongding.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gongding.dao.SubmissionDao;
import io.github.gongding.entity.StudentEntity;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/student/submit")
public class StudentSubmitAnswersServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(StudentSubmitAnswersServlet.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final SubmissionDao submissionDao = new SubmissionDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String requestUrl = req.getRequestURL().toString();
        String remoteAddr = req.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 POST 请求: {} (学生提交答案)。", remoteAddr, requestUrl);

        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        HttpSession session = req.getSession(false);
        if (session == null) {
            logger.warn("未登录或会话已过期，拒绝访问 {}。", requestUrl);
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "用户未登录或会话已过期");
            return;
        }

        StudentEntity student = (StudentEntity) session.getAttribute("student");

        if (student == null || student.getId() <= 0) {
            logger.error("Session 中的学生信息无效，拒绝访问 {}。", requestUrl);
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "学生信息无效，请重新登录");
            return;
        }
        int studentId = student.getId();
        String studentNumber = student.getStudentNumber();
        logger.debug("学生已登录，学号: {} (ID: {})。", studentNumber, studentId);

        //声明一个Map变量用于存储从请求体JSON中解析出的数据
        Map<String, Object> requestData;
        try {
            //TypeReference用于提供目标Map的泛型类型信息
            logger.debug("尝试从请求体读取并解析 JSON 数据。");
            requestData = mapper.readValue(req.getInputStream(), new TypeReference<Map<String, Object>>() {});
            logger.debug("成功解析请求体 JSON 数据。");
        } catch (IOException e) {
            logger.warn("请求数据格式错误，无法解析 JSON。", e);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "请求数据格式错误");
            return;
        }

        //从解析出的requestData Map中获取名为"practiceId"的值
        Object practiceIdObj = requestData.get("practiceId");
        logger.debug("从 JSON 数据中获取 practiceId: {}", practiceIdObj);
        if (!(practiceIdObj instanceof Integer)) {
            logger.warn("practiceId 格式错误或缺失，拒绝处理。practiceIdObj 类型: {}", (practiceIdObj != null ? practiceIdObj.getClass().getName() : "null"));
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "practiceId 格式错误或缺失");
            return;
        }
        //将practiceIdObj转换为int类型的practiceId
        int practiceId = (Integer) practiceIdObj;
        logger.debug("解析的练习ID: {}", practiceId);

        if (practiceId <= 0) {
            logger.warn("无效的练习ID (非正数): {}，拒绝处理。", practiceId);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "无效的练习ID");
            return;
        }

        Object answersObj = requestData.get("answers");
        logger.debug("从 JSON 数据中获取 answers 列表，数量: {}", (answersObj instanceof List ? ((List<?>) answersObj).size() : "非列表"));
        if (!(answersObj instanceof List)) {
            logger.warn("answers 格式错误或缺失，拒绝处理。answersObj 类型: {}", (answersObj != null ? answersObj.getClass().getName() : "null"));
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "answers 格式错误或缺失");
            return;
        }

        List<Map<String, Object>> answers;
        try {
            answers = mapper.convertValue(answersObj, new TypeReference<List<Map<String, Object>>>() {});
            logger.debug("成功将 answersObj 转换为 List<Map<String, Object>>。");
        } catch (IllegalArgumentException e) {
            logger.warn("answers 内容格式不正确，无法转换为 List<Map<String, Object>>。", e);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "answers 内容格式不正确");
            return;
        }

        if (answers == null || answers.isEmpty()) {
            logger.warn("答案列表为空，拒绝处理。");
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "答案列表不能为空");
            return;
        }
        logger.debug("答案列表包含 {} 个答案。", answers.size());


        try {
            logger.debug("检查学生 {} 对练习 {} 是否已存在提交记录。", studentId, practiceId);
            Map<String, Object> existingSubmission = submissionDao.getLatestSubmission(studentId, practiceId);
            boolean isUpdateOperation = (existingSubmission != null && existingSubmission.containsKey("submissionId"));
            logger.debug("是否存在现有提交记录: {}", isUpdateOperation);

            logger.debug("调用 SubmissionDao.createSubmission 保存提交记录，学生ID: {}, 练习ID: {}", studentId, practiceId);
            int submissionId = submissionDao.createSubmission(studentId, practiceId, answers);
            logger.debug("SubmissionDao.createSubmission 返回 submissionId: {}", submissionId);

            if (submissionId != -1) {
                String message = isUpdateOperation ? "修改成功" : "提交成功";
                //返回操作类型给前端，方便前端显示不同提示
                logger.info("学生 {} 对练习 {} 提交处理成功，操作: {}，新/更新提交ID: {}", studentId, practiceId, (isUpdateOperation ? "更新" : "创建"), submissionId);
                resp.getWriter().write(mapper.writeValueAsString(Map.of("message", message, "submissionId", submissionId, "operation", isUpdateOperation ? "updated" : "created")));
                logger.debug("已向客户端返回提交成功响应。");
            } else {
                logger.error("学生 {} 对练习 {} 提交处理失败，未能保存提交记录。", studentId, practiceId);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "提交处理失败，未能保存提交记录");
            }
        } catch (IllegalArgumentException e) {
            logger.warn("提交失败，参数异常。", e);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "提交失败：" + e.getMessage());
        } catch (RuntimeException e) {
            logger.error("提交处理时发生运行时异常。学生ID: {}, 练习ID: {}", studentId, practiceId, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器内部错误，提交失败");
        } catch (Exception e) {
            logger.error("提交处理时发生未知异常。学生ID: {}, 练习ID: {}", studentId, practiceId, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器内部错误，提交失败");
        }
        logger.info("完成处理 POST 请求: {} (学生提交答案)。", requestUrl);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("StudentSubmitAnswersServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("StudentSubmitAnswersServlet 销毁。");
        super.destroy();
    }
}
