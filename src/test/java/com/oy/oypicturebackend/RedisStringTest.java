package com.oy.oypicturebackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RedisStringTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate; // 引入Maven依赖后直接注入即可使用，这是系统默认提供开箱即用的一个对象

    @Test
    public  void testRedisStringOperations() {
        //通过opsForValue获取StringRedisTemplate中用于操作Redis字符串类型数据的操作对象，key和value都是String类型
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();

        String key = "testKey";
        String value = "testValue";

        valueOps.set(key, value);//向 Redis 中写入键值对（key=testKey，value=testValue），如果 key 不存在则新增，存在则覆盖
        String storedValue = valueOps.get(key);//从 Redis 中读取指定 key 对应的 value，赋值给storedValue
        System.out.println("新增成功，新增的value为" + storedValue);
        assertEquals(value, storedValue, "存储的值与预期不一致");//assertEquals(预期值,实际值,失败提示)，JUnit断言，验证从Redis读取的value是否和写入的testValue一致

        String updatedValue = "updatedValue";
        valueOps.set(key, updatedValue);//再次调用 set 方法，覆盖原有 key 的 value，实现更新操作
        storedValue = valueOps.get(key);
        System.out.println("更新成功，更新后的value为" + storedValue);
        assertEquals(updatedValue, storedValue, "更新后的值与预期不一致");

        storedValue = valueOps.get(key);
        System.out.println("查询出来的value为" + storedValue);
        assertNotNull(storedValue, "查询的值为空");//断言验证查询结果不为空（确保 key 存在）
        assertEquals(updatedValue, storedValue, "查询的值与预期不一致");

        stringRedisTemplate.delete(key);
        storedValue=valueOps.get(key);
        System.out.println("成功删除指定的key");
        assertNull(storedValue,"删除后的值不为空");
    }
}
