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

@WebFilter({"/api/student/practiceDetails", "/html/student/student-list-question.html"})
public class StudentAccessControlFilter implements Filter {

    private PracticeDao practiceDao;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        practiceDao = new PracticeDao();
    }

    @Override
    public void doFilter(ServletRequest req0, ServletResponse resp0, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) req0;
        HttpServletResponse resp = (HttpServletResponse) resp0;
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();

        HttpSession session = req.getSession(false);
        StudentEntity student = (session != null) ? (StudentEntity) session.getAttribute("student") : null;

        //检查学生是否已登录
        if (student == null) {
            if (uri.startsWith(contextPath + "/api/")) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未登录或会话已过期");
            } else {
                resp.sendRedirect(contextPath + "/html/student/student-login.html");
            }
            return;
        }

        String practiceIdStr = req.getParameter("practice_id");
        String lessonId = req.getParameter("lesson_id");

        //检查practice_id参数是否存在或为空白
        if (practiceIdStr == null || practiceIdStr.trim().isEmpty()) {
            if (uri.startsWith(contextPath + "/api/")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少练习ID参数");
            } else {
                String redirectUrl = contextPath + "/html/student/student.html";
                if (lessonId != null && !lessonId.trim().isEmpty()) {
                    redirectUrl = contextPath + "/html/student/student-list-practice.html?lesson_id=" + lessonId;
                }
                resp.sendRedirect(redirectUrl);
            }
            return;
        }

        int practiceId;
        try {
            practiceId = Integer.parseInt(practiceIdStr);
            if (practiceId <= 0) {
                if (uri.startsWith(contextPath + "/api/")) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "无效的练习ID");
                } else {
                    String redirectUrl = contextPath + "/html/student/student.html";
                    if (lessonId != null && !lessonId.trim().isEmpty()) {
                        redirectUrl = contextPath + "/html/student/student-list-practice.html?lesson_id=" + lessonId;
                    }
                    resp.sendRedirect(redirectUrl);
                }
                return;
            }
        } catch (NumberFormatException e) {
            if (uri.startsWith(contextPath + "/api/")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "练习ID格式不正确");
            } else {
                String redirectUrl = contextPath + "/html/student/student.html";
                if (lessonId != null && !lessonId.trim().isEmpty()) {
                    redirectUrl = contextPath + "/html/student/student-list-practice.html?lesson_id=" + lessonId;
                }
                resp.sendRedirect(redirectUrl);
            }
            return;
        }

        PracticeEntity practice = practiceDao.getPracticeById(practiceId);

        //检查是否找到了练习
        if (practice == null) {
            if (uri.startsWith(contextPath + "/api/")) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到该练习");
            } else {
                String redirectUrl = contextPath + "/html/student/student.html";
                if (lessonId != null && !lessonId.trim().isEmpty()) {
                    redirectUrl = contextPath + "/html/student/student-list-practice.html?lesson_id=" + lessonId;
                }
                resp.sendRedirect(redirectUrl);
            }
            return;
        }

        String mode = req.getParameter("mode");
        LocalDateTime now = LocalDateTime.now();

        String practiceStatus;
        if (practice.getStartAt() != null && now.isBefore(practice.getStartAt())) {
            practiceStatus = "not_started";
        } else if (practice.getEndAt() != null && now.isAfter(practice.getEndAt())) {
            practiceStatus = "ended";
        } else {
            practiceStatus = "in_progress";
        }

        if ("not_started".equals(practiceStatus)) {
            //如果练习尚未开始，不允许访问
            if (uri.startsWith(contextPath + "/api/")) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "练习尚未开始");
            } else {
                String redirectUrl = contextPath + "/html/student/student-list-practice.html";
                if (lessonId != null && !lessonId.trim().isEmpty()) {
                    redirectUrl += "?lesson_id=" + lessonId;
                } else {
                    redirectUrl = contextPath + "/html/student/student.html";
                }
                resp.sendRedirect(redirectUrl);
            }
            return;
        } else if ("in_progress".equals(practiceStatus)) {
            //如果练习正在进行中
            if ("view".equalsIgnoreCase(mode)) {
                //如果访问模式是"view"，不允许访问
                if (uri.startsWith(contextPath + "/api/")) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "练习进行中，不允许查看答案");
                } else {
                    String redirectUrl = contextPath + "/html/student/student-list-question.html?practice_id=" + practiceId;
                    if (lessonId != null && !lessonId.trim().isEmpty()) {
                        redirectUrl += "&lesson_id=" + lessonId;
                    }
                    redirectUrl += "&mode=submit";
                    resp.sendRedirect(redirectUrl);
                }
                return;
            }
            //如果模式不是"view"，允许继续访问
        } else if ("ended".equals(practiceStatus)) {
            if (!"view".equalsIgnoreCase(mode)) {
                if (uri.startsWith(contextPath + "/api/")) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "练习已截止，只能查看答案");
                } else {
                    String redirectUrl = contextPath + "/html/student/student-list-question.html?practice_id=" + practiceId;
                    if (lessonId != null && !lessonId.trim().isEmpty()) {
                        redirectUrl += "&lesson_id=" + lessonId;
                    }
                    redirectUrl += "&mode=view";
                    resp.sendRedirect(redirectUrl);
                }
                return;
            }
        } else {
            if (uri.startsWith(contextPath + "/api/")) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "未知练习状态，无法访问");
            } else {
                String redirectUrl = contextPath + "/html/student/student.html";
                if (lessonId != null && !lessonId.trim().isEmpty()) {
                    redirectUrl = contextPath + "/html/student/student-list-practice.html?lesson_id=" + lessonId;
                }
                resp.sendRedirect(redirectUrl);
            }
            return;
        }

        chain.doFilter(req, resp);
    }

    @Override
    public void destroy() {
        practiceDao = null;
    }
}
