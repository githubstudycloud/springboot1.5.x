package com.study.gateway.util;

import com.study.gateway.dao.common.VersionDao;
import com.study.gateway.entity.common.Version;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DataSourceSwitch {

    private static VersionDao versionDao;

    private static Map<String, Map<String, String>> dataSourceConfig;

    @Autowired
    public void setVersionDao(VersionDao versionDao) {
        DataSourceSwitch.versionDao = versionDao;
    }

    @ConfigurationProperties(prefix = "spring.datasource")
    public void setDataSourceConfig(Map<String, Map<String, String>> dataSourceConfig) {
        DataSourceSwitch.dataSourceConfig = dataSourceConfig;
    }

    public static BasicDataSource getDataSource(String versionName) {
        String busyName = versionDao.getBusyNameByVersion(versionName);
        Map<String, String> config = dataSourceConfig.get(busyName);
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setUrl(config.get("url"));
        dataSource.setUsername(config.get("username"));
        dataSource.setPassword(config.get("password"));
        dataSource.setDriverClassName(config.get("driver-class-name"));
        return dataSource;
    }
}