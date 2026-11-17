package com.itlzy.aiinterview.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/** 合同08 K2 方案a：手工双源（调研笔记在案）——
 *  ① Boot 的 DataSourceAutoConfiguration 见任意用户 Bean 即退位，所以两个源都自己造；
 *  ② @Primary 双重身份：MP/事务管理器的 @ConditionalOnSingleCandidate 通行证 + 按类型注入的唯一裁决；
 *  ③ MySQL↔PG 无事务一致性需求（K2 裁决：写管线分离，PG 侧不存在要回滚的业务事务——
 *      异常处理里永远不要有"补偿向量写"的冲动）。 */
@Configuration
public class DualDataSourceConfig {

    /** MySQL 事务域：复用现有 spring.datasource.* 词表（url/username/password 原键零改动）。
     *  先绑 DataSourceProperties 再 build——直接绑 HikariDataSource 会踩词表坑（Hikari 只认 jdbc-url，spring.datasource.url 绑不上去） */
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties mysqlDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource mysqlDataSource() {
        return mysqlDataSourceProperties().initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
    }

    /** PG 记忆域：pg.datasource.* 独立命名空间 */
    @Bean
    @ConfigurationProperties("pg.datasource")
    public DataSourceProperties pgDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource pgDataSource() {
        return pgDataSourceProperties().initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
    }

    /** 检索层专供（工单②的 KnowledgeSearchDao 注入这一支，默认 JdbcTemplate 不存在也不给） */
    @Bean
    public JdbcTemplate pgJdbcTemplate(@Qualifier("pgDataSource") DataSource pgDataSource) {
        return new JdbcTemplate(pgDataSource);
    }
}