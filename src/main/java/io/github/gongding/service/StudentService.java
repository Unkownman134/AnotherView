package io.github.gongding.service;

import io.github.gongding.dao.StudentDao;
import io.github.gongding.entity.LessonEntity;
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
     * @return 如果注册成功返回"success"，如果学生已存在返回"学生已存在"，如果班级不存在或数据库操作失败返回"班级不存在或注册失败"，如果发生其他异常返回"内部服务器错误"
     */
    public String register(String studentNumber, String name, String email, String school, String classof, String password) {
        logger.info("尝试注册学生，学号: {}", studentNumber);
        logger.debug("注册信息 - 姓名: {}, 邮箱: {}, 学校: {}, 班级: {}", name, email, school, classof);

        try {
            logger.debug("检查学号 {} 是否已存在。", studentNumber);
            if (studentDao.getStudentByStudentNumber(studentNumber) != null) {
                logger.warn("注册失败，学号 {} 已存在。", studentNumber);
                return "学生已存在";
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
                return "success";
            } else {
                logger.error("学号 {} 注册失败，数据库操作可能出现问题或班级不存在。", studentNumber);
                return "班级不存在或注册失败";
            }
        } catch (Exception e) {
            logger.error("学号 {} 注册过程中发生异常。", studentNumber, e);
            return "内部服务器错误";
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

    /**
     * 获取所有学生的业务逻辑
     * @return 学生实体列表
     */
    public List<StudentEntity> getAllStudents() {
        logger.info("尝试获取所有学生。");
        try {
            List<StudentEntity> students = studentDao.getAllStudents();
            logger.debug("成功从 DAO 获取 {} 个学生。", students.size());
            return students;
        } catch (Exception e) {
            logger.error("获取所有学生时发生异常。", e);
            return null;
        }
    }

    /**
     * 获取学生已关联的课程ID列表的业务逻辑
     * @param studentId 学生ID
     * @return 课程ID列表
     */
    public List<Integer> getAssociatedLessonIds(int studentId) {
        logger.info("尝试获取学生 ID {} 已关联的课程ID列表。", studentId);
        try {
            return studentDao.getAssociatedLessonIds(studentId);
        } catch (Exception e) {
            logger.error("获取学生 ID {} 关联课程时发生异常。", studentId, e);
            return null;
        }
    }

    /**
     * 获取学生已关联的班级ID列表的业务逻辑
     * @param studentId 学生ID
     * @return 班级ID列表
     */
    public List<Integer> getAssociatedClassIdsForStudent(int studentId) {
        logger.info("尝试获取学生 ID {} 已关联的班级ID列表。", studentId);
        try {
            return studentDao.getAssociatedClassIds(studentId);
        } catch (Exception e) {
            logger.error("获取学生 ID {} 关联班级时发生异常。", studentId, e);
            return null;
        }
    }

    /**
     * 更新学生的课程关联的业务逻辑
     * @param studentId 学生ID
     * @param lessonIds 要关联的课程ID列表
     * @return 是否成功更新
     */
    public boolean updateStudentLessons(int studentId, List<Integer> lessonIds) {
        logger.info("尝试更新学生 ID {} 的课程关联。", studentId);
        try {
            return studentDao.updateStudentLessons(studentId, lessonIds);
        } catch (Exception e) {
            logger.error("更新学生 ID {} 课程关联时发生异常。", studentId, e);
            return false;
        }
    }

    /**
     * 获取学生参与的课程列表的业务逻辑。
     * @param studentNumber 学生学号
     * @return 课程实体列表
     */
    public List<LessonEntity> getStudentLessons(String studentNumber) {
        logger.info("尝试获取学号 {} 参与的课程列表。", studentNumber);
        try {
            List<LessonEntity> lessons = studentDao.getStudentLessons(studentNumber);
            logger.debug("成功从 DAO 获取学号 {} 参与的 {} 门课程。", studentNumber, lessons.size());
            return lessons;
        } catch (Exception e) {
            logger.error("获取学号 {} 参与的课程列表时发生异常。", studentNumber, e);
            return null;
        }
    }

    /**
     * 根据学号获取学生实体。
     * @param studentNumber 学生学号
     * @return 学生实体，如果未找到则返回null
     */
    public StudentEntity getStudentByStudentNumber(String studentNumber) {
        logger.info("尝试获取学号 {} 的学生实体。", studentNumber);
        try {
            return studentDao.getStudentByStudentNumber(studentNumber);
        } catch (Exception e) {
            logger.error("获取学号 {} 的学生实体时发生异常。", studentNumber, e);
            return null;
        }
    }
}
