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
     * @param availableTargetNames 表示当前所有可用的真实表名集合，这个参数的值由yml的actual-data-nodes字段来配置，当前已经写死是只有一个表picture
     * @param preciseShardingValue 表示分片值的对象，里面包含了分片需要用到的字段值例如spaceId
     * @return
     */
    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Long> preciseShardingValue) {

        Long spaceId = preciseShardingValue.getValue();//取出分片键值，即spaceId的值
        String logicTableName = preciseShardingValue.getLogicTableName();//获取逻辑表名
        // spaceId 为 null 表示查询所有图片
        if (spaceId == null) {
            return logicTableName;//返回逻辑表名，让ShardingSphere自动去查询所有表
        }
        // 根据 spaceId 动态生成分表名，例如spaceId为1，则分表名为 picture_1
        String realTableName = "picture_" + spaceId;
        if (availableTargetNames.contains(realTableName)) {
            return realTableName;//如果当前数据库中存在这个拼接出来的表（picture_1）那就返回这个表名，表示确定使用这个分表，直接访问这个分表
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
