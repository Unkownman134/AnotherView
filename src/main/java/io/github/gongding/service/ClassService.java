package io.github.gongding.service;

import io.github.gongding.dao.ClassDao;
import io.github.gongding.entity.ClassEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ClassService {
    private static final Logger logger = LoggerFactory.getLogger(ClassService.class);
    private ClassDao classDao = new ClassDao();

    /**
     * 获取所有班级的业务逻辑
     * @return 班级实体列表
     */
    public List<ClassEntity> getAllClasses() {
        logger.info("尝试获取所有班级。");
        try {
            List<ClassEntity> classes = classDao.getAllClasses();
            logger.debug("成功从 DAO 获取 {} 个班级。", classes.size());
            return classes;
        } catch (Exception e) {
            logger.error("获取所有班级时发生异常。", e);
            return null;
        }
    }

    /**
     * 添加新班级的业务逻辑
     * @param className 班级名称
     * @return 如果添加成功返回true，如果班级已存在返回false
     */
    public boolean addClass(String className) {
        logger.info("尝试添加新班级，名称: {}", className);
        try {
            if (classDao.getClassIdByClassName(className) != -1) {
                logger.warn("添加班级失败，班级名称 {} 已存在。", className);
                return false;
            }

            ClassEntity newClass = new ClassEntity();
            newClass.setName(className);
            boolean success = classDao.addClass(newClass);

            if (success) {
                logger.info("班级 {} 添加成功。", className);
            } else {
                logger.error("班级 {} 添加失败，数据库操作可能出现问题。", className);
            }
            return success;
        } catch (Exception e) {
            logger.error("添加班级 {} 过程中发生异常。", className, e);
            return false;
        }
    }
}
