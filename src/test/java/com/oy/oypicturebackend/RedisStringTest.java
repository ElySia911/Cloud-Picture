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
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public  void testRedisStringOperations(){
        //通过opsForValue获取一个可以操作Redis的对象
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();

        String key="testKey";
        String value="testValue";

        valueOps.set(key,value);//新增一个key为testKey，value为testValue的记录
        String storedValue = valueOps.get(key);//取出key为testKey的value
        System.out.println("新增成功，新增的value为"+storedValue);
        assertEquals(value,storedValue,"存储的值与预期不一致");

        String updatedValue="updatedValue";
        valueOps.set(key,updatedValue);
        storedValue=valueOps.get(key);
        System.out.println("更新成功，更新后的value为"+storedValue);
        assertEquals(updatedValue,storedValue,"更新后的值与预期不一致");

        storedValue=valueOps.get(key);
        System.out.println("查询出来的value为"+storedValue);
        assertNotNull(storedValue,"查询的值为空");
        assertEquals(updatedValue,storedValue,"查询的值与预期不一致");

        stringRedisTemplate.delete(key);
        storedValue=valueOps.get(key);
        System.out.println("成功删除指定的key");
        assertNull(storedValue,"删除后的值不为空");
    }
}
