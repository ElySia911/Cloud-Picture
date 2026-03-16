package com.oy.oypicturebackend.manager.sharding;

import com.baomidou.mybatisplus.extension.toolkit.SqlRunner;
import com.oy.oypicturebackend.model.entity.Space;
import com.oy.oypicturebackend.model.enums.SpaceLevelEnum;
import com.oy.oypicturebackend.model.enums.SpaceTypeEnum;
import com.oy.oypicturebackend.service.SpaceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection;
import org.apache.shardingsphere.infra.metadata.database.rule.ShardingSphereRuleMetaData;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.rule.ShardingRule;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 分表管理器，负责创建物理分表，动态维护ShardingSphere 的分表节点配置，确保算法有可路由的目标表，为PictureShardingAlgorithm提供服务
 */
/*@Component*/ //关闭分库分表
@Slf4j
public class DynamicShardingManger {
    @Resource
    private DataSource dataSource;
    @Resource
    private SpaceService spaceService;

    private static final String LOGIC_TABLE_NAME = "picture";
    private static final String DATABASE_NAME = "logic_db";// 配置文件中的数据库名称

    @PostConstruct // 依赖注入完成后执行，拿到可用的分表名称
    public void initialize() {
        log.info("初始化动态分表配置...");
        updateShardingTableNodes();
    }

    /**
     * 获取所有动态分表名称，包括初始表picture和分表picture_{spaceId}
     *
     * @return
     */
    private Set<String> fetchAllPictureTableNames() {
        //为了测试方便，直接对所有团队空间分表（实际上线改为仅对旗舰版生效）
        Set<Long> spaceIds = spaceService.lambdaQuery()
                .eq(Space::getSpaceType, SpaceTypeEnum.TEAM.getValue())//where spaceType = ?
                .list()//执行查询并返回符合条件的Space实体列表
                .stream()
                .map(Space::getId)//对流中每个Space对象调用getID方法，取出id值
                .collect(Collectors.toSet());//将流中每个id值收集到一个Set集合中


        //将集合中每一个spaceId都转换成对应的分表名，并收集成一个新的集合
        Set<String> tableNames = spaceIds.stream()
                .map(spaceId -> LOGIC_TABLE_NAME + "_" + spaceId)
                .collect(Collectors.toSet());

        tableNames.add(LOGIC_TABLE_NAME);//添加初始逻辑表
        return tableNames;
    }

    /**
     * 更新ShardingSphere 的 actual-data-nodes 动态表名配置
     * （把写死的 oy_picture.picture 替换为 oy_picture.picture,oy_picture.picture_10001,...）
     */
    private void updateShardingTableNodes() {
        Set<String> tableNames = fetchAllPictureTableNames();// ["picture","picture_10001","picture_10002"]

        //用stream遍历每个表名，通过map给每个表名前加上数据库名"oy_picture."，然后使用collect方法将流中新拼接好的表名收集到一个字符串列表中，最后用join方法将列表中的表名用逗号连接起来
        String newActualDataNodes = tableNames.stream()
                .map(tableName -> "oy_picture." + tableName)
                .collect(Collectors.joining(","));// oy_picture.picture,oy_picture.picture_10001,oy_picture.picture_10002

        log.info("动态分表 actual-data-nodes 配置：{}", newActualDataNodes);

        ContextManager contextManager = getContextManager();//拿到框架的上下文管理器
//---------
        //从框架的上下文管理器拿到某个具体数据库的规则集合，集合中包含了所有类型的规则（分片，加密等）
        ShardingSphereRuleMetaData ruleMetaData = contextManager.getMetaDataContexts()
                .getMetaData()//获取所有数据库元数据
                .getDatabases()//获取所有数据库配置
                .get(DATABASE_NAME)//获取指定的数据库
                .getRuleMetaData();//获取该数据库的所有规则元数据

        //从 RuleMetaData 存储的所有规则集合中，筛选出类型为 ShardingRule（分片规则） 的规则，且该方法要求集合中这类规则只能有一个，找到则返回封装后的 Optional
        Optional<ShardingRule> shardingRule = ruleMetaData.findSingleRule(ShardingRule.class);
//----------
        //判断是否找到分片规则，初始状态下，isPresent返回true，yml配置了分片规则
        if (shardingRule.isPresent()) {

            //get方法取出分片规则的实例，但这个实例是只读的，通过getConfiguration方法和强转得到可编辑的实例， 这个实例是一个容器，存储了每张数据表如何分片的规则
            // 初始状态下，ruleConfig仅包含yml中配置的picture表对应的分片规则（无其他表的分片配置），且actualDataNodes为oy_picture.picture
            ShardingRuleConfiguration ruleConfig = (ShardingRuleConfiguration) shardingRule.get().getConfiguration();

            //getTables() 读取容器中每一条数据，每条数据代表一张数据表的分片规则，以流式方式处理每张表的分片配置（如picture、user、space等）
            List<ShardingTableRuleConfiguration> updatedRules = ruleConfig.getTables()
                    .stream()
                    .map(oldTableRule -> {
                        //如果某一条规则的逻辑表名等于指定的 LOGIC_TABLE_NAME
                        if (LOGIC_TABLE_NAME.equals(oldTableRule.getLogicTable())) {
                            //新建一个分片规则对象，第一个参数是逻辑表名，第二个参数是更新后的物理表列表
                            ShardingTableRuleConfiguration newTableRuleConfig = new ShardingTableRuleConfiguration(LOGIC_TABLE_NAME, newActualDataNodes);

                            //将旧规则中的分库分表策略、主键生成策略、审计策略等配置复制到新规则中
                            newTableRuleConfig.setDatabaseShardingStrategy(oldTableRule.getDatabaseShardingStrategy());
                            newTableRuleConfig.setTableShardingStrategy(oldTableRule.getTableShardingStrategy());
                            newTableRuleConfig.setKeyGenerateStrategy(oldTableRule.getKeyGenerateStrategy());
                            newTableRuleConfig.setAuditStrategy(oldTableRule.getAuditStrategy());
                            return newTableRuleConfig;//返回新的配置对象
                        }
                        return oldTableRule;
                    })
                    .collect(Collectors.toList());//把处理后的所有表的规则收集成一个新的列表updatedRules

            ruleConfig.setTables(updatedRules);//将处理后的分片规则列表回写到容器，覆盖原本的分片规则列表
            //把更新后的规则配置正式提交给 ShardingSphere 框架
            contextManager.alterRuleConfiguration(DATABASE_NAME, Collections.singleton(ruleConfig));
            contextManager.reloadDatabase(DATABASE_NAME);//让框架重新加载指定数据库的配置
            log.info("动态分表规则更新成功！");
        } else {
            log.error("未找到 ShardingSphere 的分片规则配置，动态分表更新失败。");
        }


    }


    /**
     * 动态创建空间图片分表
     *
     * @param space
     */
    public void createSpacePictureTable(Space space) {
        //动态创建分表，仅为旗舰版团队空间创建分表
        if (SpaceTypeEnum.TEAM.getValue() == space.getSpaceType() && space.getSpaceLevel() == SpaceLevelEnum.FLAGSHIP.getValue()) {
            Long spaceId = space.getId();
            String tableName = LOGIC_TABLE_NAME + "_" + spaceId;//拼接表名 例picture_1001

            //编写创建新表的sql
            String createTableSql = "CREATE TABLE " + tableName + " LIKE picture"; //复制picture表的结构来创建新的表，但不复制数据
            try {
                SqlRunner.db().update(createTableSql);//调用工具类SqlRunner执行建表Sql
                //更新分表
                updateShardingTableNodes();//建完表调用
            } catch (Exception e) {
                e.printStackTrace();
                log.error("创建图片空间分表失败，空间id={}", space.getId());
            }
        }

    }


    /**
     * 获取ShardingSphere的上下文管理器（ContextManager），它保存了分片规则、数据源配置、表元数据等所有运行时核心配置信息
     *
     * @return
     */
    private ContextManager getContextManager() {
        //从数据源获取数据库连接，将获取到的连接拆包为 ShardingSphere 框架的 ShardingSphereConnection 类型。（ShardingSphereConnection实现了Connection）
        try (ShardingSphereConnection connection = dataSource.getConnection().unwrap(ShardingSphereConnection.class)) {
            return connection.getContextManager();//获取ShardingSphere的上下文管理器，并作为方法返回值返回
        } catch (SQLException e) {
            throw new RuntimeException("获取 ShardingSphere ContextManager 失败", e);
        }

    }
}

