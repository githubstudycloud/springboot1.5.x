package com.study.gateway.dao.busy;

import com.study.gateway.entity.busy.Test;
import org.apache.ibatis.annotations.Select;

public interface TestDao {
    @Select("SELECT * FROM test WHERE id = #{id}")
    Test getById(Long id);
}
