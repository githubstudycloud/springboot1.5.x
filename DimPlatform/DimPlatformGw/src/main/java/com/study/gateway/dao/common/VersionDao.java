//package com.study.gateway.dao.common;
//
//import com.study.gateway.entity.common.Version;
//import org.apache.ibatis.annotations.Select;
//
//public interface VersionDao {
//    @Select("SELECT * FROM version ORDER BY id DESC LIMIT 1")
//    Version getLatestVersion();
//
//    @Select("SELECT busy FROM version WHERE version = #{version}")
//    String getBusyNameByVersion(String version);
//}