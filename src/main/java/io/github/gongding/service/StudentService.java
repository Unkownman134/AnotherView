package io.github.gongding.service;

import io.github.gongding.dao.StudentDao;
import io.github.gongding.entity.StudentEntity;
import io.github.gongding.util.PasswordUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class StudentService {
    private static final Logger logger = LoggerFactory.getLogger(StudentService.class);
    private StudentDao studentDao = new StudentDao();

    /**
     * 学生注册业务逻辑
     * @param studentNumber 学生学号
     * @param name 学生姓名
     * @param email 学生邮箱
     * @param school 学生学校
     * @param classof 学生班级
     * @param password 学生密码（明文）
     * @return 如果注册成功返回true，如果学生已存在返回false
     */
    public boolean register(String studentNumber, String name, String email, String school, String classof, String password) {
        logger.info("尝试注册学生，学号: {}", studentNumber);
        logger.debug("注册信息 - 姓名: {}, 邮箱: {}, 学校: {}, 班级: {}", name, email, school, classof);

        try {
            logger.debug("检查学号 {} 是否已存在。", studentNumber);
            if (studentDao.getStudentByStudentNumber(studentNumber) != null) {
                logger.warn("注册失败，学号 {} 已存在。", studentNumber);
                return false;
            }
            logger.debug("学号 {} 不存在，可以注册。", studentNumber);

            String salt= PasswordUtils.generateSalt();
            String hashedPassword = PasswordUtils.hashPassword(password, salt);
            logger.debug("为学号 {} 生成盐和哈希密码。", studentNumber);

            StudentEntity student = new StudentEntity();
            student.setStudentNumber(studentNumber);
            student.setName(name);
            student.setEmail(email);
            student.setSchool(school);
            student.setClassof(classof);
            student.setPasswordSalt(salt);
            student.setPasswordHash(hashedPassword);
            logger.debug("创建学生实体对象，学号: {}", studentNumber);

            logger.debug("调用 StudentDao 添加学生到数据库，学号: {}", studentNumber);
            boolean success = studentDao.addStudent(student);

            if (success) {
                logger.info("学号 {} 注册成功。", studentNumber);
            } else {
                logger.error("学号 {} 注册失败，数据库操作可能出现问题或返回false。", studentNumber);
            }
            return success;
        } catch (Exception e) {
            logger.error("学号 {} 注册过程中发生异常。", studentNumber, e);
            return false;
        }
    }

    /**
     * 学生登录业务逻辑
     * @param studentNumber 学生学号
     * @param password 学生密码（明文）
     * @return 如果登录成功返回StudentEntity对象，如果失败返回null
     */
    public StudentEntity login(String studentNumber, String password) {
        logger.info("尝试登录，学号: {}", studentNumber);

        try {
            //根据学号从数据库获取学生信息
            logger.debug("从数据库获取学号 {} 的学生信息。", studentNumber);
            StudentEntity student = studentDao.getStudentByStudentNumber(studentNumber);

            //验证学生是否存在及密码是否匹配
            if (student != null) {
                logger.debug("找到学号 {} 的学生信息，进行密码验证。", studentNumber);
                if (student.getPasswordHash().equals(PasswordUtils.hashPassword(password,student.getPasswordSalt()))) {
                    logger.info("学号 {} 身份验证成功。", studentNumber);
                    //调用StudentDao的updateStudentLoginTime方法更新学生的最后登录时间
                    logger.debug("更新学号 {} 的最后登录时间。", studentNumber);
                    studentDao.updateStudentLoginTime(student.getStudentNumber());
                    return student;
                } else {
                    logger.warn("学号 {} 身份验证失败，密码不匹配。", studentNumber);
                    return null;
                }
            } else {
                logger.warn("学号 {} 身份验证失败，未找到该学生。", studentNumber);
                return null;
            }
        } catch (Exception e) {
            logger.error("学号 {} 登录过程中发生异常。", studentNumber, e);
            return null;
        }
    }

    /**
     * 根据班级ID获取学生列表的业务逻辑
     * @param classId 班级ID
     * @return 学生实体列表
     */
    public List<StudentEntity> getStudentsByClassId(int classId) {
        logger.info("尝试获取班级 ID {} 的学生列表。", classId);
        try {
            return studentDao.getStudentsByClassIds(Collections.singletonList(classId));
        } catch (Exception e) {
            logger.error("获取班级 ID {} 学生列表时发生异常。", classId, e);
            return null;
        }
    }

    /**
     * 根据课程ID获取学生列表的业务逻辑
     * @param lessonId 课程ID
     * @return 学生实体列表
     */
    public List<StudentEntity> getStudentsByLessonId(int lessonId) {
        logger.info("尝试获取课程 ID {} 的学生列表。", lessonId);
        try {
            return studentDao.getStudentsByLessonId(lessonId);
        } catch (Exception e) {
            logger.error("获取课程 ID {} 学生列表时发生异常。", lessonId, e);
            return null;
        }
    }
}
