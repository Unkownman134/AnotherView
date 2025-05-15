package io.github.gongding.pool.config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.Set;

public class DataSourceConfig {
    private String driver;
    private String url;
    private String username;
    private String password;

    //连接池初始大小
    private String initSize = "3";
    //连接池最大大小
    private String maxSize = "10";
    //是否开启连接安全检查
    private String health = "true";
    //健康检查启动延迟时间（毫秒）
    private String delay = "1000";
    //健康检查执行间隔时间（毫秒）
    private String period = "1000";
    //连接超时时间（毫秒）
    private String timeout = "1000";
    //获取连接等待时间（毫秒）
    private String waittime = "500000";

    /**
     * 构造方法
     */
    public DataSourceConfig() {
        try {
            Properties properties = new Properties();
            properties.load(DataSourceConfig.class.getClassLoader().getResourceAsStream("db.properties"));
            Set<Object> keySet = properties.keySet();
            for (Object key : keySet) {
                String filedName = key.toString().split("\\.")[1];
                String filedValue = properties.getProperty(key.toString());

                //使用反射获取当前类的字段（根据属性名）
                Field field = this.getClass().getDeclaredField(filedName);

                //使用反射获取当前类的setter方法
                Method method = this.getClass().getDeclaredMethod(toUpper(filedName), field.getType());
                //使用反射调用setter方法，将属性值设置到对应的字段中
                method.invoke(this, filedValue);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getInitSize() {
        return initSize;
    }

    public void setInitSize(String initSize) {
        this.initSize = initSize;
    }

    public String getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(String maxSize) {
        this.maxSize = maxSize;
    }

    public String getHealth() {
        return health;
    }

    public void setHealth(String health) {
        this.health = health;
    }

    public String getDelay() {
        return delay;
    }

    public void setDelay(String delay) {
        this.delay = delay;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getTimeout() {
        return timeout;
    }

    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }

    public String getWaittime() {
        return waittime;
    }

    public void setWaittime(String waittime) {
        this.waittime = waittime;
    }

    @Override
    public String toString() {
        return "DataSourceConfig{" +
                "driver='" + driver + '\'' +
                ", url='" + url + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", initSize='" + initSize + '\'' +
                ", maxSize='" + maxSize + '\'' +
                ", health='" + health + '\'' +
                ", delay='" + delay + '\'' +
                ", period='" + period + '\'' +
                ", timeout='" + timeout + '\'' +
                ", waittime='" + waittime + '\'' +
                '}';
    }

    /**
     * 将字段名转换为对应的 setter 方法名
     * @param filedName 字段名
     * @return 对应的 setter 方法名
     */
    public String toUpper(String filedName) {
        char[] chars = filedName.toCharArray();
        chars[0] -= 32;
        return "set" + new String(chars);
    }
}
