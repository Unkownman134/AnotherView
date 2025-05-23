package io.github.gongding.filter;

import io.github.gongding.dao.PracticeDao;
import io.github.gongding.entity.PracticeEntity;
import io.github.gongding.entity.StudentEntity;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebFilter({"/api/student/practiceDetails", "/html/student/student-list-question.html"})
public class StudentAccessControlFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(StudentAccessControlFilter.class);
    private PracticeDao practiceDao;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("StudentAccessControlFilter 初始化。");
        practiceDao = new PracticeDao();
        logger.debug("PracticeDao 实例创建成功。");
    }

    @Override
    public void doFilter(ServletRequest req0, ServletResponse resp0, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) req0;
        HttpServletResponse resp = (HttpServletResponse) resp0;
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();

        String requestMethod = req.getMethod();
        String remoteAddr = req.getRemoteAddr();
        logger.debug("收到来自 IP 地址 {} 的 {} 请求: {}", remoteAddr, requestMethod, uri);

        HttpSession session = req.getSession(false);
        StudentEntity student = (session != null) ? (StudentEntity) session.getAttribute("student") : null;

        //检查学生是否已登录
        if (student == null) {
            logger.warn("用户未登录或会话已过期，拒绝访问 {}。", uri);
            if (uri.startsWith(contextPath + "/api/")) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未登录或会话已过期");
                logger.debug("返回 401 未授权错误。");
            } else {
                resp.sendRedirect(contextPath + "/html/student/student-login.html");
                logger.debug("重定向到学生登录页面。");
            }
            return;
        }
        logger.debug("学生已登录，ID: {}", student.getId());

        String practiceIdStr = req.getParameter("practice_id");
        String lessonId = req.getParameter("lesson_id");
        logger.debug("请求参数 - practice_id: {}, lesson_id: {}", practiceIdStr, lessonId);

        //检查practice_id参数是否存在或为空白
        if (practiceIdStr == null || practiceIdStr.trim().isEmpty()) {
            logger.warn("缺少练习ID参数，拒绝访问 {}。", uri);
            if (uri.startsWith(contextPath + "/api/")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少练习ID参数");
                logger.debug("返回 400 错误。");
            } else {
                String redirectUrl = contextPath + "/html/student/student.html";
                if (lessonId != null && !lessonId.trim().isEmpty()) {
                    redirectUrl = contextPath + "/html/student/student-list-practice.html?lesson_id=" + lessonId;
                }
                resp.sendRedirect(redirectUrl);
                logger.debug("缺少练习ID，重定向到: {}", redirectUrl);
            }
            return;
        }

        int practiceId;
        try {
            practiceId = Integer.parseInt(practiceIdStr);
            logger.debug("解析的练习ID: {}", practiceId);
            if (practiceId <= 0) {
                logger.warn("无效的练习ID (非正数): {}，拒绝访问 {}。", practiceIdStr, uri);
                if (uri.startsWith(contextPath + "/api/")) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "无效的练习ID");
                    logger.debug("返回 400 错误。");
                } else {
                    String redirectUrl = contextPath + "/html/student/student.html";
                    if (lessonId != null && !lessonId.trim().isEmpty()) {
                        redirectUrl = contextPath + "/html/student/student-list-practice.html?lesson_id=" + lessonId;
                    }
                    resp.sendRedirect(redirectUrl);
                    logger.debug("无效练习ID，重定向到: {}", redirectUrl);
                }
                return;
            }
        } catch (NumberFormatException e) {
            logger.warn("练习ID格式不正确: {}，拒绝访问 {}。", practiceIdStr, uri, e);
            if (uri.startsWith(contextPath + "/api/")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "练习ID格式不正确");
                logger.debug("返回 400 错误。");
            } else {
                String redirectUrl = contextPath + "/html/student/student.html";
                if (lessonId != null && !lessonId.trim().isEmpty()) {
                    redirectUrl = contextPath + "/html/student/student-list-practice.html?lesson_id=" + lessonId;
                }
                resp.sendRedirect(redirectUrl);
                logger.debug("练习ID格式错误，重定向到: {}", redirectUrl);
            }
            return;
        }

        logger.debug("调用 PracticeDao 获取练习 ID {} 的信息。", practiceId);
        PracticeEntity practice = practiceDao.getPracticeById(practiceId);

        //检查是否找到了练习
        if (practice == null) {
            logger.warn("未找到练习 ID {} 的信息。", practiceId);
            if (uri.startsWith(contextPath + "/api/")) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到该练习");
                logger.debug("返回 404 错误。");
            } else {
                String redirectUrl = contextPath + "/html/student/student.html";
                if (lessonId != null && !lessonId.trim().isEmpty()) {
                    redirectUrl = contextPath + "/html/student/student-list-practice.html?lesson_id=" + lessonId;
                }
                resp.sendRedirect(redirectUrl);
                logger.debug("未找到练习，重定向到: {}", redirectUrl);
            }
            return;
        }
        logger.debug("成功找到练习 ID {} 的信息。", practiceId);

        String mode = req.getParameter("mode");
        LocalDateTime now = LocalDateTime.now();
        logger.debug("当前时间: {}, 请求模式: '{}'", now, mode);

        String practiceStatus;
        if (practice.getStartAt() != null && now.isBefore(practice.getStartAt())) {
            practiceStatus = "not_started";
        } else if (practice.getEndAt() != null && now.isAfter(practice.getEndAt())) {
            practiceStatus = "ended";
        } else {
            practiceStatus = "in_progress";
        }
        logger.debug("练习 ID {} 的状态: {}", practiceId, practiceStatus);

        if ("not_started".equals(practiceStatus)) {
            //如果练习尚未开始，不允许访问
            logger.warn("练习 ID {} 尚未开始，拒绝访问 {}。", practiceId, uri);
            if (uri.startsWith(contextPath + "/api/")) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "练习尚未开始");
                logger.debug("返回 403 错误。");
            } else {
                String redirectUrl = contextPath + "/html/student/student-list-practice.html";
                if (lessonId != null && !lessonId.trim().isEmpty()) {
                    redirectUrl += "?lesson_id=" + lessonId;
                } else {
                    redirectUrl = contextPath + "/html/student/student.html";
                }
                resp.sendRedirect(redirectUrl);
                logger.debug("练习未开始，重定向到: {}", redirectUrl);
            }
            return;
        } else if ("in_progress".equals(practiceStatus)) {
            //如果练习正在进行中
            if ("view".equalsIgnoreCase(mode)) {
                //如果访问模式是"view"，不允许访问
                logger.warn("练习 ID {} 正在进行中，不允许查看答案 (mode='view')，拒绝访问 {}。", practiceId, uri);
                if (uri.startsWith(contextPath + "/api/")) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "练习进行中，不允许查看答案");
                    logger.debug("返回 403 错误。");
                } else {
                    String redirectUrl = contextPath + "/html/student/student-list-question.html?practice_id=" + practiceId;
                    if (lessonId != null && !lessonId.trim().isEmpty()) {
                        redirectUrl += "&lesson_id=" + lessonId;
                    }
                    redirectUrl += "&mode=submit";
                    resp.sendRedirect(redirectUrl);
                    logger.debug("练习进行中，重定向到提交模式: {}", redirectUrl);
                }
                return;
            }
            //如果模式不是"view"，允许继续访问
            logger.debug("练习 ID {} 正在进行中，允许访问 (mode='{}')。", practiceId, mode);
        } else if ("ended".equals(practiceStatus)) {
            //如果练习已截止
            if (!"view".equalsIgnoreCase(mode)) {
                logger.warn("练习 ID {} 已截止，只能查看答案 (mode='{}')，拒绝访问 {}。", practiceId, mode, uri);
                if (uri.startsWith(contextPath + "/api/")) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "练习已截止，只能查看答案");
                    logger.debug("返回 403 错误。");
                } else {
                    String redirectUrl = contextPath + "/html/student/student-list-question.html?practice_id=" + practiceId;
                    if (lessonId != null && !lessonId.trim().isEmpty()) {
                        redirectUrl += "&lesson_id=" + lessonId;
                    }
                    redirectUrl += "&mode=view";
                    resp.sendRedirect(redirectUrl);
                    logger.debug("练习已截止，重定向到查看模式: {}", redirectUrl);
                }
                return;
            }
            //如果模式是"view"，允许继续访问
            logger.debug("练习 ID {} 已截止，允许访问 (mode='view')。", practiceId);
        } else {
            //未知练习状态
            logger.error("练习 ID {} 状态未知: '{}'，拒绝访问 {}。", practiceId, practiceStatus, uri);
            if (uri.startsWith(contextPath + "/api/")) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "未知练习状态，无法访问");
                logger.debug("返回 500 错误。");
            } else {
                String redirectUrl = contextPath + "/html/student/student.html";
                if (lessonId != null && !lessonId.trim().isEmpty()) {
                    redirectUrl = contextPath + "/html/student/student-list-practice.html?lesson_id=" + lessonId;
                }
                resp.sendRedirect(redirectUrl);
                logger.debug("未知练习状态，重定向到: {}", redirectUrl);
            }
            return;
        }

        logger.debug("学生访问控制检查通过，继续 Filter 链。");
        chain.doFilter(req, resp);
    }

    @Override
    public void destroy() {
        logger.info("StudentAccessControlFilter 销毁。");
        practiceDao = null;
        logger.debug("PracticeDao 实例已置为 null。");
    }
}
