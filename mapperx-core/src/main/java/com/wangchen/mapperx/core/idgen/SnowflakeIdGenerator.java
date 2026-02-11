package com.wangchen.mapperx.core.idgen;

import com.wangchen.mapperx.core.spi.IdGenerator;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 融合 MP 优点的纯净雪花 ID 生成器
 *
 * @author chenwang
 */
public class SnowflakeIdGenerator implements IdGenerator<Long> {

    // 2021-01-01T00:00:00Z
    private static final long EPOCH = 1609459200000L;
    private static final String NODE_ID_ENV_VAR = "SNOWFLAKE_NODE_ID";

    private final long nodeId;
    private volatile long sequence = 0L;
    private volatile long lastTimestamp = -1L;

    /**
     * 构造器：自动从环境变量或自动分配获取 nodeId
     */
    public SnowflakeIdGenerator() {
        String nodeIdStr = System.getenv(NODE_ID_ENV_VAR);
        if (nodeIdStr != null && !nodeIdStr.trim().isEmpty()) {
            this.nodeId = parseNodeId(nodeIdStr);
        } else {
            this.nodeId = generateAutoNodeId();
        }
    }

    /**
     * 手动指定 nodeId（用于确定性部署，不推荐，推荐读取环境变量）
     */
    public SnowflakeIdGenerator(long nodeId) {
        if (nodeId < 0 || nodeId > 1023) {
            throw new IllegalArgumentException("nodeId must be between 0 and 1023");
        }
        this.nodeId = nodeId;
    }

    @Override
    public Long generate(Class<?> entityClass) {
        return nextId();
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                try {
                    // 等待回拨恢复 左移 1 位，等价于乘以 2
                    wait(offset << 1);
                    timestamp = System.currentTimeMillis();
                    if (timestamp < lastTimestamp) {
                        throw new RuntimeException("Clock moved backwards after waiting: " + offset + " ms");
                    }
                } catch (InterruptedException e) {
                    // 捕获中断异常后，恢复线程的中断状态
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException("Clock moved backwards: " + offset + " ms");
            }
        }

        // 同一毫秒内的请求 → 序列号自增
        if (lastTimestamp == timestamp) {
            // & 4095L 等价于 % 4096，保证序列号范围 0-4095（12位）
            sequence = (sequence + 1) & 4095L;
            // 序列号用完（等于0）→ 自旋等待下一毫秒
            if (sequence == 0) {
                while (timestamp <= lastTimestamp) {
                    timestamp = System.currentTimeMillis();
                }
            }
        } else {
            // 新的毫秒 → 序列号随机设为1-3（MP的优化点） 
            // 分布式环境下，多个节点可能在同一毫秒首次生成 ID，若都用 0 则碰撞概率略高；随机 1-3 可降低这种概率
            sequence = ThreadLocalRandom.current().nextLong(1, 3);
        }
        // 更新上一次生成 ID的时间戳
        lastTimestamp = timestamp;
        // 按雪花算法拼接最终ID（核心计算逻辑）
        return ((timestamp - EPOCH) << 22) | (nodeId << 12) | sequence;
    }

    // ================= 私有方法 =================

    private static long parseNodeId(String str) {
        try {
            long id = Long.parseLong(str.trim());
            if (id < 0 || id > 1023) {
                throw new IllegalArgumentException();
            }
            return id;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid " + NODE_ID_ENV_VAR + ": " + str);
        }
    }

    private static long generateAutoNodeId() {
        try {
            // IP 哈希 + PID 哈希（比 MAC 更稳定）
            String jvmName = ManagementFactory.getRuntimeMXBean().getName();
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            String seed = jvmName + "@" + hostAddress;
            return Math.abs(seed.hashCode()) % 1024;
        } catch (Exception ignored) {
            return 1;
        }
    }
}