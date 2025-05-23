package io.github.gongding.service;

import io.github.gongding.dao.TeacherDao;
import io.github.gongding.entity.TeacherEntity;
import io.github.gongding.util.PasswordUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TeacherService {
    private static final Logger logger = LoggerFactory.getLogger(TeacherService.class);
    private TeacherDao teacherDao =new TeacherDao();

    /**
     * 教师注册业务逻辑
     * @param name 教师姓名
     * @param email 教师邮箱
     * @param password 教师密码（明文）
     * @return 如果注册成功返回true，如果教师已存在返回false
     */
    public boolean register(String name, String email, String password) {
        logger.info("尝试注册教师，姓名: {}", name);
        logger.debug("注册信息 - 邮箱: {}", email);

        try {
            logger.debug("检查教师姓名 {} 是否已存在。", name);
            if (teacherDao.getTeacherByTeacherName(name) != null) {
                logger.warn("注册失败，教师姓名 {} 已存在。", name);
                return false;
            }
            logger.debug("教师姓名 {} 不存在，可以注册。", name);

            String salt= PasswordUtils.generateSalt();
            String hashedPassword = PasswordUtils.hashPassword(password, salt);
            logger.debug("为教师 {} 生成盐和哈希密码。", name);

            TeacherEntity teacher = new TeacherEntity();
            teacher.setName(name);
            teacher.setEmail(email);
            teacher.setPasswordSalt(salt);
            teacher.setPasswordHash(hashedPassword);
            logger.debug("创建教师实体对象，姓名: {}", name);

            logger.debug("调用 TeacherDao 添加教师到数据库，姓名: {}", name);
            boolean success = teacherDao.addTeacher(teacher);

            if (success) {
                logger.info("教师 {} 注册成功。", name);
            } else {
                logger.error("教师 {} 注册失败，数据库操作可能出现问题或返回false。", name);
            }
            return success;
        } catch (Exception e) {
            logger.error("教师 {} 注册过程中发生异常。", name, e);
            return false;
        }
    }

    /**
     * 教师登录业务逻辑
     * @param name 教师姓名
     * @param password 教师密码（明文）
     * @return 如果登录成功返回TeacherEntity对象，如果失败返回null
     */
    public TeacherEntity login(String name, String password) {
        logger.info("尝试教师登录，姓名: {}", name);

        try {
            //根据姓名从数据库获取教师信息
            logger.debug("从数据库获取教师 {} 的信息。", name);
            TeacherEntity teacher = teacherDao.getTeacherByTeacherName(name);

            if (teacher != null) {
                logger.debug("找到教师 {} 的信息，进行密码验证。", name);
                if (teacher.getPasswordHash().equals(PasswordUtils.hashPassword(password, teacher.getPasswordSalt()))) {
                    logger.info("教师 {} 身份验证成功。", name);
                    //调用TeacherDao的updateTeacherLoginTime方法更新教师的最后登录时间
                    logger.debug("更新教师 {} 的最后登录时间。", name);
                    teacherDao.updateTeacherLoginTime(teacher.getName());
                    return teacher;
                } else {
                    logger.warn("教师 {} 身份验证失败，密码不匹配。", name);
                    return null;
                }
            } else {
                logger.warn("教师 {} 身份验证失败，未找到该教师。", name);
                return null;
            }
        } catch (Exception e) {
            logger.error("教师 {} 登录过程中发生异常。", name, e);
            return null;
        }
    }

    /**
     * 获取所有教师的业务逻辑
     * @return 教师实体列表
     */
    public List<TeacherEntity> getAllTeachers() {
        logger.info("尝试获取所有教师。");
        try {
            List<TeacherEntity> teachers = teacherDao.getAllTeachers();
            logger.debug("成功从 DAO 获取 {} 个教师。", teachers.size());
            return teachers;
        } catch (Exception e) {
            logger.error("获取所有教师时发生异常。", e);
            return null;
        }
    }

    /**
     * 获取教师已关联的班级ID列表的业务逻辑
     * @param teacherId 教师ID
     * @return 班级ID列表
     */
    public List<Integer> getAssociatedClassIds(int teacherId) {
        logger.info("尝试获取教师 ID {} 已关联的班级ID列表。", teacherId);
        try {
            return teacherDao.getAssociatedClassIds(teacherId);
        } catch (Exception e) {
            logger.error("获取教师 ID {} 关联班级时发生异常。", teacherId, e);
            return null;
        }
    }

    /**
     * 更新教师的班级关联的业务逻辑
     * @param teacherId 教师ID
     * @param classIds 要关联的班级ID列表
     * @return 是否成功更新
     */
    public boolean updateTeacherClasses(int teacherId, List<Integer> classIds) {
        logger.info("尝试更新教师 ID {} 的班级关联。", teacherId);
        try {
            return teacherDao.updateTeacherClasses(teacherId, classIds);
        } catch (Exception e) {
            logger.error("更新教师 ID {} 班级关联时发生异常。", teacherId, e);
            return false;
        }
    }

    /**
     * 根据班级ID获取教师列表的业务逻辑
     * @param classId 班级ID
     * @return 教师实体列表
     */
    public List<TeacherEntity> getTeachersByClassId(int classId) {
        logger.info("尝试获取班级 ID {} 的教师列表。", classId);
        try {
            return teacherDao.getTeachersByClassId(classId);
        } catch (Exception e) {
            logger.error("获取班级 ID {} 教师列表时发生异常。", classId, e);
            return null;
        }
    }

    /**
     * 根据课程ID获取教师列表的业务逻辑
     * @param lessonId 课程ID
     * @return 教师实体列表
     */
    public List<TeacherEntity> getTeachersByLessonId(int lessonId) {
        logger.info("尝试获取课程 ID {} 的教师列表。", lessonId);
        try {
            return teacherDao.getTeachersByLessonId(lessonId);
        } catch (Exception e) {
            logger.error("获取课程 ID {} 教师列表时发生异常。", lessonId, e);
            return null;
        }
    }
}
