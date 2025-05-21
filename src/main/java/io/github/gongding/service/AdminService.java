package io.github.gongding.service;

import io.github.gongding.dao.AdminDao;
import io.github.gongding.entity.AdminEntity;
import io.github.gongding.util.PasswordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminService {
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    private AdminDao adminDao = new AdminDao();

    /**
     * 管理员登录业务逻辑
     * @param name 管理员姓名
     * @param password 管理员密码（明文）
     * @return 如果登录成功返回AdminEntity对象，如果失败返回null
     */
    public AdminEntity login(String name, String password) {
        logger.info("尝试登录，姓名: {}", name);

        try {
            logger.debug("从数据库获取姓名 {} 的管理员信息。", name);
            AdminEntity admin = adminDao.getAdminByName(name);

            if (admin != null) {
                logger.debug("找到姓名 {} 的管理员信息，进行密码验证。", name);
                if (admin.getPasswordHash().equals(PasswordUtils.hashPassword(password, admin.getPasswordSalt()))) {
                    logger.info("姓名 {} 身份验证成功。", name);
                    logger.debug("更新姓名 {} 的最后登录时间。", name);
                    adminDao.updateAdminLoginTime(admin.getName());
                    return admin;
                } else {
                    logger.warn("姓名 {} 身份验证失败，密码不匹配。", name);
                    return null;
                }
            } else {
                logger.warn("姓名 {} 身份验证失败，未找到该管理员。", name);
                return null;
            }
        } catch (Exception e) {
            logger.error("姓名 {} 登录过程中发生异常。", name, e);
            return null;
        }
    }
}
