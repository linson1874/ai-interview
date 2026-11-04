package com.itlzy.aiinterview;

import com.itlzy.aiinterview.entity.User;
import com.itlzy.aiinterview.mapper.UserMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    void insertAndQuery() {
        User user = new User();
        // 用户名带时间戳后缀,保证测试可重复运行不触发 uk_username 唯一索引
        user.setUsername("zhangsan_" + System.currentTimeMillis() % 100000);
        user.setPassword("明文占位_注册接口阶段换BCrypt");
        user.setNickname("张三");
        user.setStatus(1);

        int rows = userMapper.insert(user);
        Assertions.assertEquals(1, rows);
        System.out.println("插入成功, 新ID = " + user.getId());
        System.out.println("回查结果 = " + userMapper.selectById(user.getId()));
    }
}
