package com.stationerymall.mapper;

import com.stationerymall.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户表 {@code users} 的纯数据访问接口。
 * <p>
 * 约束：本接口只做数据存取，<b>不包含</b>用户名唯一性判断、密码校验、角色判断等业务逻辑。
 */
@Mapper
public interface UserMapper {

    /** 新增用户；role 由上层强制写为 NORMAL_USER，主键回填到 user.id */
    int insert(User user);

    /** 按主键查询 */
    User selectById(@Param("id") Long id);

    /** 按用户名查询（登录、注册重名校验的数据来源） */
    User selectByUsername(@Param("username") String username);

    /** 按主键动态更新：用户名 / 密码 / 角色；null 字段不更新 */
    int updateById(User user);
}
