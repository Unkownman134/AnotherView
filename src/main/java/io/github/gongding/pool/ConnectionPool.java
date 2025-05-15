package io.github.gongding.pool;

import io.github.gongding.entity.ConnectionEntity;
import io.github.gongding.pool.config.DataSourceConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionPool implements IConnectionPool {
    //记录当前连接总数，使用AtomicInteger保证原子性
    AtomicInteger connectionCount = new AtomicInteger(0);

    private DataSourceConfig dataSourceConfig;

    //空闲连接池
    Vector<Connection> freePools = new Vector<Connection>();
    //正在使用的连接池
    Vector<ConnectionEntity> usePools = new Vector<ConnectionEntity>();

    /**
     * 构造方法
     * @param dataSourceConfig 数据源配置
     */
    public ConnectionPool(DataSourceConfig dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
        init();
    }

    /**
     * 初始化连接池
     */
    private void init() {
        for (int i = 0; i < Integer.valueOf(dataSourceConfig.getInitSize()); i++) {
            Connection connection = createConnection();
            freePools.add(connection);
        }
        // 如果配置开启健康检查，则启动健康检查任务
        if (Boolean.valueOf(dataSourceConfig.getHealth()) == true) {
            checkConnectionTimeOut();
        }
    }

    /**
     * 启动连接超时检查任务
     */
    private void checkConnectionTimeOut() {
        Worker worker = new Worker();
        new Timer().schedule(worker, Integer.valueOf(dataSourceConfig.getDelay()), Integer.valueOf(dataSourceConfig.getPeriod()));
    }

    /**
     * 健康检查的定时任务
     */
    class Worker extends TimerTask {
        public void run() {
            try {
                for (int i = 0; i < usePools.size(); i++) {
                    ConnectionEntity connectionEntity = usePools.get(i);
                    //检查连接是否超时（当前时间-使用开始时间>超时时间）
                    if ((System.currentTimeMillis() - connectionEntity.getUseStartTime()) > Long.valueOf(dataSourceConfig.getTimeout())) {
                        Connection connection = connectionEntity.getConnection();
                        if (isAvailable(connection)) {
                            //关闭超时且可用的连接
                            connection.close();
                            usePools.remove(i);
                            connectionCount.decrementAndGet();
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
            }
        }
    }

    /**
     * 创建新的数据库连接
     * @return 创建的连接
     */
    private synchronized Connection createConnection() {
        Connection connection = null;
        try {
            Class.forName(dataSourceConfig.getDriver());
            connection = DriverManager.getConnection(dataSourceConfig.getUrl(), dataSourceConfig.getUsername(), dataSourceConfig.getPassword());
            //连接总数加一
            connectionCount.incrementAndGet();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return connection;
    }

    /**
     * 检查连接是否可用
     * @param connection 连接
     * @return 是否可用
     */
    private boolean isAvailable(Connection connection) {
        try {
            //检查连接是否不为null且未关闭
            if (connection != null && !connection.isClosed()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取数据库连接
     * @return 获取到的连接
     */
    public synchronized Connection getConn() {
        Connection connection = null;
        try {
            //循环直到获取到可用连接
            while (connection == null) {
                if (!freePools.isEmpty()) {
                    //从空闲连接池中移除第一个连接
                    connection = freePools.remove(0);
                    if (!isAvailable(connection)) {
                        //如果不可用，连接总数减一
                        connectionCount.decrementAndGet();
                        connection = null;
                    } else {
                        //如果可用，添加到正在使用的连接池，并记录使用开始时间
                        usePools.add(new ConnectionEntity(connection, System.currentTimeMillis()));
                    }
                } else {
                    //检查当前连接总数是否小于最大连接数
                    if (connectionCount.get() < Integer.valueOf(dataSourceConfig.getMaxSize())) {
                        connection = createConnection();
                        usePools.add(new ConnectionEntity(connection, System.currentTimeMillis()));
                    } else {
                        //如果达到最大连接数，等待一段时间（由waittime配置）
                        this.wait(Integer.valueOf(dataSourceConfig.getWaittime()));
                        //等待后继续尝试，避免无限循环
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return connection;
    }

    /**
     * 释放数据库连接
     * @param connection 待释放连接
     */
    public synchronized void releaseConn(Connection connection) {
        if (isAvailable(connection)) {
            //如果可用，添加到空闲连接池
            freePools.add(connection);
        } else {
            connectionCount.decrementAndGet();
        }
        //从正在使用的连接池中移除对应的连接
        usePools.removeIf(entity -> entity.getConnection() == connection);
        //唤醒所有等待获取连接的线程
        this.notifyAll();
    }
}
