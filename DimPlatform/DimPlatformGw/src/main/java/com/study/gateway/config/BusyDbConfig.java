//package com.study.gateway.config;
//
//import com.study.gateway.util.DataSourceSwitch;
//import org.apache.ibatis.session.SqlSessionFactory;
//import org.mybatis.spring.SqlSessionFactoryBean;
//import org.mybatis.spring.annotation.MapperScan;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
//import javax.sql.DataSource;
//
//@Configuration
//@MapperScan(basePackages = "com.study.gateway.dao.busy", sqlSessionFactoryRef = "busySqlSessionFactory")
//public class BusyDbConfig {
//
//    @Bean
//    public SqlSessionFactory busySqlSessionFactory(DataSource busyDataSource) throws Exception {
//        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
//        sqlSessionFactoryBean.setDataSource(busyDataSource);
//        sqlSessionFactoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/busy/*.xml"));
//        return sqlSessionFactoryBean.getObject();
//    }
//
//    @Bean
//    public DataSource busyDataSource(String version) {
//        return DataSourceSwitch.getDataSource(version);
//    }
//}