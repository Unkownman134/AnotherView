package io.github.gongding.dao;

import io.github.gongding.entity.ClassEntity;
import io.github.gongding.util.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassDao {
    private static final Logger logger = LoggerFactory.getLogger(ClassDao.class);

    /**
     * 根据班级ID获取班级实体
     * @param id 班级ID
     * @return 班级实体，如果未找到则返回null
     */
    public ClassEntity getClassById(int id) {
        logger.debug("尝试根据班级ID {} 获取班级实体。", id);
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ClassEntity cls = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT class_id, name FROM class WHERE class_id = ?";
            logger.debug("执行 SQL: {} with id = {}", sql, id);
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                cls = new ClassEntity();
                cls.setId(rs.getInt("class_id"));
                cls.setName(rs.getString("name"));
                logger.debug("成功找到班级 ID {} 的实体。", id);
            } else {
                logger.debug("未找到班级 ID {} 的实体。", id);
            }
        } catch (SQLException e) {
            // 记录数据库异常
            logger.error("根据班级ID {} 获取班级实体时发生数据库异常。", id, e);
            // Removed: e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt, rs); // 假设 DBUtils.close() 会处理关闭连接的日志
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成根据班级ID {} 获取班级实体操作。", id);
        return cls;
    }

    /**
     * 根据教师ID获取班级列表
     * @param teacherId 教师ID
     * @return 班级实体列表
     */
    public List<ClassEntity> getClassesByTeacherId(int teacherId) {
        logger.debug("尝试根据教师ID {} 获取班级列表。", teacherId);
        List<ClassEntity> classes = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT c.class_id, c.name FROM class c JOIN class_teacher ct ON c.class_id = ct.class_id WHERE ct.teacher_id = ?";
            logger.debug("执行 SQL: {} with teacherId = {}", sql, teacherId);

            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, teacherId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                ClassEntity cls = new ClassEntity();
                cls.setId(rs.getInt("class_id"));
                cls.setName(rs.getString("name"));
                classes.add(cls);
                logger.trace("找到班级: ID = {}, Name = {}", cls.getId(), cls.getName());
            }
            logger.debug("成功找到 {} 个班级与教师 ID {} 关联。", classes.size(), teacherId);
        } catch (SQLException e) {
            logger.error("根据教师ID {} 获取班级列表时发生数据库异常。", teacherId, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成根据教师ID {} 获取班级列表操作。", teacherId);
        return classes;
    }

    /**
     * 根据练习ID获取关联的班级列表
     * @param practiceId 练习ID
     * @return 班级实体列表
     */
    public List<ClassEntity> getClassesByPracticeId(int practiceId) {
        logger.debug("尝试根据练习ID {} 获取关联班级列表。", practiceId);
        List<ClassEntity> classes = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT c.class_id, c.name FROM class c JOIN practice_class pc ON c.class_id = pc.class_id WHERE pc.practice_id = ?";
            logger.debug("执行 SQL: {} with practiceId = {}", sql, practiceId);

            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, practiceId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                ClassEntity cls = new ClassEntity();
                cls.setId(rs.getInt("class_id"));
                cls.setName(rs.getString("name"));
                classes.add(cls);
                logger.trace("找到关联班级: ID = {}, Name = {}", cls.getId(), cls.getName());
            }
            logger.debug("成功找到 {} 个班级与练习 ID {} 关联。", classes.size(), practiceId);
        } catch (SQLException e) {
            logger.error("根据练习ID {} 获取关联班级列表时发生数据库异常。", practiceId, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成根据练习ID {} 获取关联班级列表操作。", practiceId);
        return classes;
    }

    /**
     * 根据班级名称查询班级ID。
     *
     * @param className 班级名称。
     * @return 班级ID，如果找到则返回对应的ID，如果未找到则返回-1。
     */
    public int getClassIdByClassName(String className) {
        logger.debug("尝试根据班级名称 {} 查询班级ID。", className);
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int classId = -1;
        String sql = "SELECT class_id FROM class WHERE name = ?";

        try {
            conn = DBUtils.getConnection();
            logger.debug("执行 SQL (查询班级ID): {} with className = {}", sql, className);
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, className);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                classId = rs.getInt("class_id");
                logger.debug("成功找到班级名称 {} 对应的班级ID: {}", className, classId);
            } else {
                logger.warn("未找到班级名称 {} 对应的班级ID。", className);
            }
        } catch (SQLException e) {
            logger.error("根据班级名称 {} 查询班级ID时发生数据库异常。", className, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        return classId;
    }

    /**
     * 获取所有班级实体
     * @return 班级实体列表
     */
    public List<ClassEntity> getAllClasses() {
        logger.debug("尝试查询所有班级列表。");
        List<ClassEntity> classes = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT class_id, name FROM class";
            logger.debug("执行 SQL: {}", sql);
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                ClassEntity cls = new ClassEntity();
                cls.setId(rs.getInt("class_id"));
                cls.setName(rs.getString("name"));
                classes.add(cls);
                logger.trace("找到班级: ID = {}, Name = '{}'", cls.getId(), cls.getName());
            }
            logger.debug("成功找到 {} 个班级。", classes.size());
        } catch (SQLException e) {
            logger.error("查询所有班级列表时发生数据库异常。", e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成查询所有班级列表操作。");
        return classes;
    }

    /**
     * 添加新班级
     * @param classEntity 班级实体
     * @return 是否成功添加
     */
    public boolean addClass(ClassEntity classEntity) {
        logger.info("尝试添加新班级，名称: {}", classEntity.getName());
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DBUtils.getConnection();
            String sql = "INSERT INTO class (name) VALUES(?)";
            logger.debug("执行 SQL (添加班级): {} with name = '{}'", sql, classEntity.getName());
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, classEntity.getName());

            int affectedRows = pstmt.executeUpdate();
            success = affectedRows > 0;
            logger.debug("添加班级影响行数: {}", affectedRows);

            if (success) {
                logger.info("成功添加新班级，名称: {}", classEntity.getName());
            } else {
                logger.warn("添加新班级失败，名称: {}，可能数据库操作未成功。", classEntity.getName());
            }

        } catch (SQLException e) {
            logger.error("添加新班级时发生数据库异常，名称: {}", classEntity.getName(), e);
        } finally {
            DBUtils.close(conn, pstmt);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成添加新班级操作，名称: {}，结果: {}", classEntity.getName(), success ? "成功" : "失败");
        return success;
    }
}
