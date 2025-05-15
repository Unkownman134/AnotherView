package io.github.gongding.service;

import io.github.gongding.dao.StudentDao;
import io.github.gongding.entity.StudentEntity;
import io.github.gongding.util.PasswordUtils;

public class StudentService {
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
        if (studentDao.getStudentByStudentNumber(studentNumber) != null) {
            return false;
        }

        String salt= PasswordUtils.generateSalt();
        String hashedPassword = PasswordUtils.hashPassword(password, salt);

        StudentEntity student = new StudentEntity();
        student.setStudentNumber(studentNumber);
        student.setName(name);
        student.setEmail(email);
        student.setSchool(school);
        student.setClassof(classof);
        student.setPasswordSalt(salt);
        student.setPasswordHash(hashedPassword);

        return studentDao.addStudent(student);
    }
}
