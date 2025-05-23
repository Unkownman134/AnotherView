package io.github.gongding.service;

import io.github.gongding.dao.LessonDao;
import io.github.gongding.entity.LessonEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

public class LessonService {
    private static final Logger logger = LoggerFactory.getLogger(LessonService.class);
    private LessonDao lessonDao = new LessonDao();

    /**
     * 根据课程ID获取课程信息
     * @param lessonId 课程ID
     * @return 课程实体，如果未找到则返回null
     */
    public LessonEntity getLessonById(int lessonId) {
        logger.info("尝试获取课程 ID {}。", lessonId);
        try {
            return lessonDao.getLessonById(lessonId);
        } catch (Exception e) {
            logger.error("获取课程 ID {} 时发生异常。", lessonId, e);
            return null;
        }
    }

    /**
     * 根据教师ID获取该教师负责的所有课程信息
     * @param teacherId 教师ID
     * @return 课程实体列表
     */
    public List<LessonEntity> getLessonsByTeacherId(int teacherId) {
        logger.info("尝试获取教师 ID {} 的所有课程。", teacherId);
        try {
            return lessonDao.getLessonsByTeacherId(teacherId);
        } catch (Exception e) {
            logger.error("获取教师 ID {} 的课程时发生异常。", teacherId, e);
            return null;
        }
    }

    /**
     * 获取所有课程的业务逻辑
     * @return 课程实体列表
     */
    public List<LessonEntity> getAllLessons() {
        logger.info("尝试获取所有课程。");
        try {
            List<LessonEntity> lessons = lessonDao.getAllLessons();
            logger.debug("成功从 DAO 获取 {} 个课程。", lessons.size());
            return lessons;
        } catch (Exception e) {
            logger.error("获取所有课程时发生异常。", e);
            return null;
        }
    }

    /**
     * 添加新课程的业务逻辑
     * @param title 课程标题
     * @param description 课程描述
     * @param semesterId 所属学期ID
     * @param teacherId 所属教师ID
     * @return 如果添加成功返回true，否则返回false
     */
    public boolean addLesson(String title, String description, int semesterId, int teacherId) {
        logger.info("尝试添加新课程，标题: {}，教师ID: {}", title, teacherId);
        try {
            LessonEntity newLesson = new LessonEntity();
            newLesson.setTitle(title);
            newLesson.setDescription(description);
            newLesson.setSemesterId(semesterId);
            newLesson.setTeacherId(teacherId);
            newLesson.setCreatedAt(LocalDateTime.now());

            boolean success = lessonDao.addLesson(newLesson);

            if (success) {
                logger.info("课程 {} 添加成功。", title);
            } else {
                logger.error("课程 {} 添加失败，数据库操作可能出现问题。", title);
            }
            return success;
        } catch (Exception e) {
            logger.error("添加课程 {} 过程中发生异常。", title, e);
            return false;
        }
    }

    /**
     * 为课程指定教师的业务逻辑
     * @param lessonId 课程ID
     * @param teacherId 教师ID
     * @return 是否成功指定
     */
    public boolean assignTeacherToLesson(int lessonId, int teacherId) {
        logger.info("尝试为课程 ID {} 指定教师 ID {}。", lessonId, teacherId);
        try {
            boolean success = lessonDao.updateLessonTeacher(lessonId, teacherId);
            if (success) {
                logger.info("成功为课程 ID {} 指定教师 ID {}。", lessonId, teacherId);
            } else {
                logger.error("为课程 ID {} 指定教师 ID {} 失败。", lessonId, teacherId);
            }
            return success;
        } catch (Exception e) {
            logger.error("为课程 ID {} 指定教师 ID {} 过程中发生异常。", lessonId, teacherId, e);
            return false;
        }
    }
}
