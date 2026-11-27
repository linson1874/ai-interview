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

/** 双数据源配置：MySQL 事务域（主源）+ PG 知识域，两者无跨库事务一致性要求。
 *  @author 林子阳 */
@Configuration
public class DualDataSourceConfig {

    /** MySQL 数据源属性（主源，复用 spring.datasource.* 配置）；先绑 Properties 再 build，避免 Hikari 的 jdbc-url 绑定坑 */
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

    /** PG 数据源属性（pg.datasource.* 独立配置） */
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

    /** PG 专用的 JdbcTemplate，供检索层注入 */
    @Bean
    public JdbcTemplate pgJdbcTemplate(@Qualifier("pgDataSource") DataSource pgDataSource) {
        return new JdbcTemplate(pgDataSource);
    }
}