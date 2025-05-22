package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.gongding.entity.AdminEntity;
import io.github.gongding.entity.ClassEntity;
import io.github.gongding.entity.QuestionEntity;
import io.github.gongding.entity.SemesterEntity; // 导入 SemesterEntity
import io.github.gongding.entity.TeacherEntity;
import io.github.gongding.service.ClassService;
import io.github.gongding.service.QuestionService;
import io.github.gongding.service.SemesterService; // 导入 SemesterService
import io.github.gongding.service.TeacherService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/adminInfo")
public class AdminInfoServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminInfoServlet.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final QuestionService questionService = new QuestionService();
    private final TeacherService teacherService = new TeacherService();
    private final ClassService classService = new ClassService();
    private final SemesterService semesterService = new SemesterService(); // 实例化 SemesterService

    public AdminInfoServlet() {
        logger.debug("AdminInfoServlet 构造方法执行。");
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        logger.debug("ObjectMapper 配置完成。");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestUrl = request.getRequestURL().toString();
        String remoteAddr = request.getRemoteAddr();
        String action = request.getParameter("action");
        logger.info("收到来自 IP 地址 {} 的 GET 请求: {} (Action: {}).", remoteAddr, requestUrl, action);

        response.setContentType("application/json;charset=utf-8");
        Map<String, Object> responseMap = new HashMap<>();

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("admin") == null) {
            logger.warn("未登录或会话过期，拒绝访问 {}。", requestUrl);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            responseMap.put("success", false);
            responseMap.put("message", "未登录或会话已过期");
            mapper.writeValue(response.getWriter(), responseMap);
            return;
        }
        logger.debug("管理员已登录，从 Session 获取管理员信息。");

        if ("getQuestions".equals(action)) {
            try {
                List<QuestionEntity> questions = questionService.getAllQuestions();
                responseMap.put("success", true);
                responseMap.put("questions", questions);
                responseMap.put("message", "题目数据加载成功。");
                logger.debug("成功获取 {} 个题目。", questions.size());
            } catch (Exception e) {
                logger.error("获取题目数据时发生异常。", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                responseMap.put("success", false);
                responseMap.put("message", "获取题目数据时发生内部错误。");
            }
        } else if ("getTeachers".equals(action)) {
            try {
                List<TeacherEntity> teachers = teacherService.getAllTeachers();
                responseMap.put("success", true);
                responseMap.put("teachers", teachers);
                responseMap.put("message", "教师数据加载成功。");
                logger.debug("成功获取 {} 个教师。", teachers.size());
            } catch (Exception e) {
                logger.error("获取教师数据时发生异常。", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                responseMap.put("success", false);
                responseMap.put("message", "获取教师数据时发生内部错误。");
            }
        } else if ("getClasses".equals(action)) {
            try {
                List<ClassEntity> classes = classService.getAllClasses();
                responseMap.put("success", true);
                responseMap.put("classes", classes);
                responseMap.put("message", "班级数据加载成功。");
                logger.debug("成功获取 {} 个班级。", classes.size());
            } catch (Exception e) {
                logger.error("获取班级数据时发生异常。", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                responseMap.put("success", false);
                responseMap.put("message", "获取班级数据时发生内部错误。");
            }
        } else if ("getTeacherClasses".equals(action)) {
            String teacherIdStr = request.getParameter("teacherId");
            if (teacherIdStr == null || teacherIdStr.isEmpty()) {
                logger.warn("请求缺少 teacherId 参数。");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseMap.put("success", false);
                responseMap.put("message", "缺少教师ID参数。");
            } else {
                try {
                    int teacherId = Integer.parseInt(teacherIdStr);
                    List<Integer> associatedClassIds = teacherService.getAssociatedClassIds(teacherId);
                    responseMap.put("success", true);
                    responseMap.put("associatedClassIds", associatedClassIds);
                    responseMap.put("message", "教师关联班级数据加载成功。");
                    logger.debug("成功获取教师 ID {} 关联的 {} 个班级ID。", teacherId, associatedClassIds.size());
                } catch (NumberFormatException e) {
                    logger.warn("teacherId 参数格式无效: {}", teacherIdStr);
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    responseMap.put("success", false);
                    responseMap.put("message", "教师ID参数格式无效。");
                } catch (Exception e) {
                    logger.error("获取教师关联班级数据时发生异常，教师ID: {}", teacherIdStr, e);
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    responseMap.put("success", false);
                    responseMap.put("message", "获取教师关联班级数据时发生内部错误。");
                }
            }
        } else if ("getSemesters".equals(action)) {
            try {
                List<SemesterEntity> semesters = semesterService.getAllSemesters();
                responseMap.put("success", true);
                responseMap.put("semesters", semesters);
                responseMap.put("message", "学期数据加载成功。");
                logger.debug("成功获取 {} 个学期。", semesters.size());
            } catch (Exception e) {
                logger.error("获取学期数据时发生异常。", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                responseMap.put("success", false);
                responseMap.put("message", "获取学期数据时发生内部错误。");
            }
        } else {
            AdminEntity sessionAdmin = (AdminEntity) session.getAttribute("admin");
            String adminName = sessionAdmin.getName();
            logger.debug("Session 中的管理员姓名: {}", adminName);

            try {
                responseMap.put("success", true);
                responseMap.put("admin", sessionAdmin);
                responseMap.put("message", "管理员信息加载成功。");

                logger.debug("成功获取学号 {} 的最新信息，返回给客户端。", adminName);
            } catch (Exception e) {
                logger.error("获取管理员信息或处理响应时发生异常，姓名: {}", adminName, e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                responseMap.put("success", false);
                responseMap.put("message", "获取管理员信息时发生内部错误。");
            }
        }

        mapper.writeValue(response.getWriter(), responseMap);
        logger.info("完成处理 GET 请求: {}", requestUrl);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String requestUrl = request.getRequestURL().toString();
        String remoteAddr = request.getRemoteAddr();
        String action = request.getParameter("action");
        logger.info("收到来自 IP 地址 {} 的 POST 请求: {} (Action: {}).", remoteAddr, requestUrl, action);

        response.setContentType("application/json;charset=utf-8");
        Map<String, Object> responseMap = new HashMap<>();

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("admin") == null) {
            logger.warn("未登录或会话过期，拒绝访问 {}。", requestUrl);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            responseMap.put("success", false);
            responseMap.put("message", "未登录或会话已过期");
            mapper.writeValue(response.getWriter(), responseMap);
            return;
        }
        logger.debug("管理员已登录，从 Session 获取管理员信息。");

        if ("updateTeacherClasses".equals(action)) {
            String teacherIdStr = request.getParameter("teacherId");
            String classIdsStr = request.getParameter("classIds");

            if (teacherIdStr == null || teacherIdStr.isEmpty()) {
                logger.warn("更新教师班级关联请求缺少 teacherId 参数。");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseMap.put("success", false);
                responseMap.put("message", "缺少教师ID参数。");
            } else {
                try {
                    int teacherId = Integer.parseInt(teacherIdStr);
                    List<Integer> classIds = null;
                    if (classIdsStr != null && !classIdsStr.isEmpty()) {
                        classIds = Arrays.stream(classIdsStr.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .map(Integer::parseInt)
                                .collect(Collectors.toList());
                    }

                    boolean success = teacherService.updateTeacherClasses(teacherId, classIds);
                    if (success) {
                        responseMap.put("success", true);
                        responseMap.put("message", "教师班级关联更新成功。");
                        logger.info("教师 ID {} 的班级关联更新成功。", teacherId);
                    } else {
                        responseMap.put("success", false);
                        responseMap.put("message", "教师班级关联更新失败。");
                        logger.warn("教师 ID {} 的班级关联更新失败。", teacherId);
                    }
                } catch (NumberFormatException e) {
                    logger.warn("更新教师班级关联请求中参数格式无效: teacherId={}, classIds={}", teacherIdStr, classIdsStr, e);
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    responseMap.put("success", false);
                    responseMap.put("message", "参数格式无效。");
                } catch (Exception e) {
                    logger.error("更新教师班级关联时发生异常，教师ID: {}", teacherIdStr, e);
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    responseMap.put("success", false);
                    responseMap.put("message", "更新教师班级关联时发生内部错误。");
                }
            }
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseMap.put("success", false);
            responseMap.put("message", "不支持的 POST 请求动作。");
            logger.warn("收到不支持的 POST 请求动作: {}", action);
        }

        mapper.writeValue(response.getWriter(), responseMap);
        logger.info("完成处理 POST 请求: {}", requestUrl);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("AdminInfoServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("AdminInfoServlet 销毁。");
        super.destroy();
    }
}
