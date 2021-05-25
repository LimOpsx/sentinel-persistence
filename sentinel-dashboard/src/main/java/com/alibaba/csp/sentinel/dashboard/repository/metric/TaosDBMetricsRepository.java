package com.alibaba.csp.sentinel.dashboard.repository.metric;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.nacos.common.utils.CollectionUtils;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author LimOps
 */
@Repository("taosDBMetricsRepository")
public class TaosDBMetricsRepository implements MetricsRepository<MetricEntity> {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 时间格式
     */
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * 数据库名称
     */
    private static final String SENTINEL_DATABASE = "sentinel_db";

    /**
     * 数据表名称
     */
    private static final String TAG = "0.1";

    /**
     * 北京时间领先UTC时间8小时 UTC: Universal Time Coordinated,世界统一时间
     */
    private static final Integer UTC_8 = 8;

    public TaosDBMetricsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(MetricEntity metric) {
        if (null == metric || StringUtil.isBlank(metric.getApp())) {
            return;
        }
//        Field              |         Type         |   Length    |   Note   |
//=================================================================================
//        ts                             | TIMESTAMP            |           8 |          |
//        id                             | INT                  |           4 |          |
//        gmt_create                     | TIMESTAMP            |           8 |          |
//        gmt_modified                   | TIMESTAMP            |           8 |          |
//        app                            | NCHAR                |          50 |          |
//        _timestamp                     | TIMESTAMP            |           8 |          |
//        _resource                      | NCHAR                |          50 |          |
//        pass_qps                       | INT                  |           4 |          |
//        success_qps                    | INT                  |           4 |          |
//        block_qps                      | INT                  |           4 |          |
//        exception_qps                  | INT                  |           4 |          |
//        rt                             | DOUBLE               |           8 |          |
//        _count                         | INT                  |           4 |          |
//        resource_code                  | INT                  |           4 |          |
//        _version                       | DOUBLE               |           8 | TAG      |


        Timestamp gmtCreate = new Timestamp(metric.getGmtCreate().getTime());
        Timestamp modified = new Timestamp(metric.getGmtModified().getTime());
        Timestamp timestamp = new Timestamp(metric.getTimestamp().getTime());

        Long id = metric.getId();
        if (Objects.isNull(id)) {
            metric.setId(RandomUtil.randomLong(100,5000));
        }
        jdbcTemplate.update("insert into d1001 using meters tags (?) VALUES(now,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                TAG,
                metric.getId()
                , gmtCreate
                , modified
                , metric.getApp()
                , timestamp
                , metric.getResource()
                , metric.getPassQps()
                , metric.getSuccessQps()
                , metric.getBlockQps()
                , metric.getExceptionQps()
                , metric.getRt()
                , metric.getCount()
                , metric.getResourceCode());
    }

    @Override
    public void saveAll(Iterable<MetricEntity> metrics) {
        metrics.forEach(metric -> {
            Timestamp gmtCreate = new Timestamp(metric.getGmtCreate().getTime());
            Timestamp modified = new Timestamp(metric.getGmtModified().getTime());
            Timestamp timestamp = new Timestamp(metric.getTimestamp().getTime());

            Long id = metric.getId();
            if (Objects.isNull(id)) {
                metric.setId(RandomUtil.randomLong(100,5000));
            }
            jdbcTemplate.update("insert into d1001 using meters tags (?) VALUES(now,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    TAG,
                    metric.getId()
                    , gmtCreate
                    , modified
                    , metric.getApp()
                    , timestamp
                    , metric.getResource()
                    , metric.getPassQps()
                    , metric.getSuccessQps()
                    , metric.getBlockQps()
                    , metric.getExceptionQps()
                    , metric.getRt()
                    , metric.getCount()
                    , metric.getResourceCode());
        });
    }

    @Override
    public List<MetricEntity> queryByAppAndResourceBetween(String app, String resource, long startTime, long endTime) {
        List<MetricEntity> results = new ArrayList<MetricEntity>();
        if (StringUtil.isBlank(app)) {
            return results;
        }

        if (StringUtil.isBlank(resource)) {
            return results;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM d1001 ");
        String where = " WHERE ";
        StringBuilder whereSql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        if (StringUtil.isNotBlank(app)) {
            whereSql.append("app").append("=").append("?");
            params.add(app);
        }
        if (StringUtil.isNotBlank(resource)) {
            if (whereSql.length() > 0) {
                whereSql.append(" AND ").append("resource").append("=").append("?");
                params.add(resource);
            }
        }
        if (startTime > 0) {
            if (whereSql.length() > 0) {
                whereSql.append(" AND ").append("_timestamp").append(">=").append("?");
                params.add(startTime);
            }
        }
        if (endTime > 0) {
            if (whereSql.length() > 0) {
                whereSql.append(" AND ").append("_timestamp").append("<=").append("?");
                params.add(endTime);
            }
        }
        if (whereSql.length() > 0) {
            whereSql.insert(0, where);
            sql.append(whereSql);
        }

        List<MetricEntity> metricEntities = jdbcTemplate.query(sql.toString(), new RowMapper<MetricEntity>() {
            /**
             * Implementations must implement this method to map each row of data
             * in the ResultSet. This method should not call {@code next()} on
             * the ResultSet; it is only supposed to map values of the current row.
             *
             * @param rs     the ResultSet to map (pre-initialized for the current row)
             * @param rowNum the number of the current row
             * @return the result object for the current row (may be {@code null})
             * @throws SQLException if a SQLException is encountered getting
             *                      column values (that is, there's no need to catch SQLException)
             */
            @Override
            public MetricEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
                MetricEntity metricEntity = new MetricEntity();
                metricEntity.setId(rs.getLong("id"));
                metricEntity.setGmtCreate(rs.getTime("gmtCreate"));
                metricEntity.setGmtModified(rs.getTime("gmtmodified"));
                metricEntity.setResource(rs.getString("resource"));
                metricEntity.setPassQps(rs.getLong("passQps"));
                metricEntity.setSuccessQps(rs.getLong("successQps"));
                metricEntity.setBlockQps(rs.getLong("blockQps"));
                metricEntity.setTimestamp(rs.getTimestamp("_timestamp"));
                metricEntity.setExceptionQps(rs.getLong("exceptionQps"));
                metricEntity.setRt(rs.getDouble("rt"));
                metricEntity.setCount(rs.getInt("count"));
                return metricEntity;
            }
        }, params.toArray());

        if (CollectionUtils.isEmpty(metricEntities)) {
            return results;
        }

        return metricEntities;
    }

    @Override
    public List<String> listResourcesOfApp(String app) {
        List<String> result = new ArrayList<>();
        if (StringUtil.isBlank(app)) {
            return result;
        }

        List<MetricEntity> metricEntities = getMetricEntities(app);

        if (CollectionUtils.sizeIsEmpty(metricEntities)) {
            return result;
        }
        Map<String, MetricEntity> resourceCount = new HashMap<>(32);

        for (MetricEntity metricEntity : metricEntities) {
            String resource = metricEntity.getResource();
            if (resourceCount.containsKey(resource)) {
                MetricEntity oldEntity = resourceCount.get(resource);
                oldEntity.addPassQps(metricEntity.getPassQps());
                oldEntity.addRtAndSuccessQps(metricEntity.getRt(), metricEntity.getSuccessQps());
                oldEntity.addBlockQps(metricEntity.getBlockQps());
                oldEntity.addExceptionQps(metricEntity.getExceptionQps());
                oldEntity.addCount(1);
            } else {
                resourceCount.put(resource, MetricEntity.copyOf(metricEntity));
            }
        }


        return resourceCount.entrySet()
                .stream()
                .sorted((o1, o2) -> {
                    MetricEntity e1 = o1.getValue();
                    MetricEntity e2 = o2.getValue();
                    int t = e2.getBlockQps().compareTo(e1.getBlockQps());
                    if (t != 0) {
                        return t;
                    }
                    return e2.getPassQps().compareTo(e1.getPassQps());
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private List<MetricEntity> getMetricEntities(String app) {
        return jdbcTemplate.query("select * from d1001 where app = ?", new RowMapper<MetricEntity>() {
                /**
                 * Implementations must implement this method to map each row of data
                 * in the ResultSet. This method should not call {@code next()} on
                 * the ResultSet; it is only supposed to map values of the current row.
                 *
                 * @param rs     the ResultSet to map (pre-initialized for the current row)
                 * @param rowNum the number of the current row
                 * @return the result object for the current row (may be {@code null})
                 * @throws SQLException if a SQLException is encountered getting
                 *                      column values (that is, there's no need to catch SQLException)
                 */
                @Override
                public MetricEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
                    MetricEntity metricEntity = new MetricEntity();
                    metricEntity.setId(rs.getLong("id"));
                    metricEntity.setGmtCreate(rs.getTime("gmtCreate"));
                    metricEntity.setGmtModified(rs.getTime("gmtmodified"));
                    metricEntity.setResource(rs.getString("resource"));
                    metricEntity.setPassQps(rs.getLong("passQps"));
                    metricEntity.setSuccessQps(rs.getLong("successQps"));
                    metricEntity.setBlockQps(rs.getLong("blockQps"));
                    metricEntity.setTimestamp(rs.getTimestamp("_timestamp"));
                    metricEntity.setExceptionQps(rs.getLong("exceptionQps"));
                    metricEntity.setRt(rs.getDouble("rt"));
                    metricEntity.setCount(rs.getInt("count"));
                    return metricEntity;
                }
            },app);
    }

}
