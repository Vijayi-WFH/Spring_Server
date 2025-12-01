package com.tse.core_application.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
public class DbConfig {

    @Value("${secondarydb.datasource.url}")
    private String secondaryUrl;

    @Value("${secondarydb.datasource.username}")
    private String secondaryUsername;

    @Value("${secondarydb.datasource.password}")
    private String secondaryPassword;

    @Value("${spring.datasource.url}")
    private String primaryUrl;

    @Value("${spring.datasource.username}")
    private String primaryUsername;

    @Value("${spring.datasource.password}")
    private String primaryPassword;

    @Primary
    @Bean(name = "primaryDataSource")
    public DataSource primaryDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        // This is needed if we are using JDBC -- for my Sql: ("com.mysql.jdbc.Driver")
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(primaryUrl);
        dataSource.setUsername(primaryUsername);
        dataSource.setPassword(primaryPassword);
        return dataSource;
    }

    @Bean(name = "secondaryDataSource")
    public DataSource secondaryDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        // This is needed if we are using JDBC -- for my Sql: ("com.mysql.jdbc.Driver")
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(secondaryUrl);
        dataSource.setUsername(secondaryUsername);
        dataSource.setPassword(secondaryPassword);
        return dataSource;
    }


////    @Bean(name = "primaryDb")
////    @Primary
////    @ConfigurationProperties(prefix="spring.datasource")
////    public DataSource primaryDataSource() {
////        return DataSourceBuilder.create().build();
////    }
////
////    @Bean(name = "secondaryDb")
////    @ConfigurationProperties(prefix="secondarydb.datasource")
////    public DataSource secondaryDataSource() {
////        return DataSourceBuilder.create().build();
////    }
//
////    @Bean(name="tmPrimary")
////    @Autowired
////    @Primary
////    DataSourceTransactionManager tmPrimary(@Qualifier("datasource1") DataSource datasource) {
////        DataSourceTransactionManager txm  = new DataSourceTransactionManager(datasource);
////        return txm;
////    }
////
////    @Bean(name="tmSecondary")
////    @Autowired
////    DataSourceTransactionManager tmSecondary(@Qualifier ("tmSecondary") DataSource datasource) {
////        DataSourceTransactionManager txm  = new DataSourceTransactionManager(datasource);
////        return txm;
////    }
}
