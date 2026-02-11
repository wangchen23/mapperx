package com.wangchen.mapperx.core.annotation;

/**
 * 主键生成策略
 *
 * @author chenwang
 * @date 2026/2/9 09:36
 **/
public enum IdStrategy {

    /**
     * 数据库自增（如 MySQL AUTO_INCREMENT）
     * <p>插入时主键字段留空，由数据库生成</p>
     */
    AUTO,

    /**
     * 框架自动生成 UUID（32位小写无横线字符串）
     * <p>仅适用于 String 类型主键，插入前自动赋值</p>
     */
    UUID,

    /**
     * 用户必须手动指定主键值
     * <p>若为 null，则抛出异常（防止误插）</p>
     */
    ASSIGN,

    /**
     * 不启用任何自动处理（默认安全策略）
     * <p>完全由用户或数据库控制，框架不干预</p>
     */
    NONE,

    /**
     * 用户自定义生成策略
     * <p>需配合 {@link IdentifierGenerator} 实现类使用</p>
     */
    CUSTOM,


    /**
     * 分布式雪花 ID（64位 Long）
     * <p>框架自动集成 {@code SnowflakeIdGenerator}，支持：
     * - 生产环境：通过 {@code SNOWFLAKE_NODE_ID} 环境变量指定节点 ID（0~1023）
     * - 开发环境：自动分配（仅限单机）
     * </p>
     * <p>仅适用于 {@code Long} 或 {@code long} 类型主键</p>
     */
    SNOWFLAKE
}
