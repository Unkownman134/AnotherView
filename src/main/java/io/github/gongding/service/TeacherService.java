package io.github.gongding.service;

import io.github.gongding.dao.TeacherDao;
import io.github.gongding.entity.TeacherEntity;
import io.github.gongding.util.PasswordUtils;

public class TeacherService {
    private TeacherDao teacherDao =new TeacherDao();

    public boolean register(String name, String email, String password) {
        if (teacherDao.getTeacherByTeacherName(name) != null) {
            return false;
        }

        String salt= PasswordUtils.generateSalt();
        String hashedPassword = PasswordUtils.hashPassword(password, salt);

        TeacherEntity teacher = new TeacherEntity();
        teacher.setName(name);
        teacher.setEmail(email);
        teacher.setPasswordSalt(salt);
        teacher.setPasswordHash(hashedPassword);

        return teacherDao.addTeacher(teacher);
    }

    public TeacherEntity login(String name, String password) {
        TeacherEntity teacher = teacherDao.getTeacherByTeacherName(name);
        if (teacher != null && teacher.getPasswordHash().equals(PasswordUtils.hashPassword(password, teacher.getPasswordSalt()))) {
            teacherDao.updateTeacherLoginTime(teacher.getName());
            return teacher;
        } else {
            return null;
        }
    }
}
