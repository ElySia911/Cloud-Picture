package com.oy.oypicturebackend.manager.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

/**
 * 图片分表算法
 * 必须实现ShardingSphere提供的StandardShardingAlgorithm<Long>接口，用于标准分片算法 <Long>表示对Long类型的字段进行分片
 */
public class PictureShardingAlgorithm implements StandardShardingAlgorithm<Long> {

    /**
     * 根据spaceId进行分表
     *
     * @param availableTargetNames 表示可用的物理表集合，目前配置文件中写死了只有一个物理表
     * @param preciseShardingValue 表示分片值对象，里面包含了分片需要用到的字段值，例如逻辑表名，分片字段名，分片字段的值
     * @return
     */
    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Long> preciseShardingValue) {

        Long spaceId = preciseShardingValue.getValue();//从分片值对象中获取分片键的值，即spaceId的值
        String logicTableName = preciseShardingValue.getLogicTableName();//获取逻辑表名
        // spaceId 为 null 表示查询所有图片
        if (spaceId == null) {
            return logicTableName;//返回逻辑表名，让ShardingSphere自动去查询所有表
        }
        String realTableName = "picture_" + spaceId; // 根据 spaceId 动态生成分表名，例如spaceId为1，则分表名为 picture_1

        //检查生成的分表名是否存在可用的物理表集合中，存在则路由到该物理表名
        if (availableTargetNames.contains(realTableName)) {
            return realTableName;
        } else {
            return logicTableName;//如果不存在就兜底返回逻辑表
        }
    }

    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Long> rangeShardingValue) {
        return new ArrayList<>();
    }

    @Override
    public Properties getProps() {
        return null;
    }

    @Override
    public void init(Properties properties) {

    }
}
