package io.github.gongding.dao;

import io.github.gongding.entity.ClassEntity;
import io.github.gongding.util.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ClassDao {
    /**
     * 根据班级ID获取班级实体
     * @param id 班级ID
     * @return 班级实体，如果未找到则返回null
     */
    public ClassEntity getClassById(int id) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ClassEntity cls = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT class_id, name FROM class WHERE class_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                cls = new ClassEntity();
                cls.setId(rs.getInt("class_id"));
                cls.setName(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt, rs);
        }
        return cls;
    }

    /**
     * 根据教师ID获取班级列表
     * @param teacherId 教师ID
     * @return 班级实体列表
     */
    public List<ClassEntity> getClassesByTeacherId(int teacherId) {
        List<ClassEntity> classes = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT c.class_id, c.name FROM class c JOIN class_teacher ct ON c.class_id = ct.class_id WHERE ct.teacher_id = ?";

            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, teacherId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                ClassEntity cls = new ClassEntity();
                cls.setId(rs.getInt("class_id"));
                cls.setName(rs.getString("name"));
                classes.add(cls);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt, rs);
        }
        return classes;
    }
}
