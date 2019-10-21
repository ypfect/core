package com.overstar.core.esjob;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Getter
@Setter
public abstract class AbstractJob implements SimpleJob {

	protected static final Logger LOG = LoggerFactory.getLogger("job");

	/** 任务执行时间表达式 */
	private String cron;
	/** 任务分片总数 */
	private Integer shardingTotalCount;
	/** 任务分片参数 */
	private String shardingItemParameters;
	/** 任务参数 */
	private String jobParameter;

	/**
	 * 构造任务函数，eg：“分别查询出2019年一季度三个月注册的用户ID,统计每个用户的在线时长，每5秒进行一次统计”
	 * 
	 * @param cron 任务执行时间表达式，eg: "0/5 * * * * ?"
	 * @param shardingTotalCount 任务分片数, eg: 3 
	 * @param shardingItemParameters 任务分片参数, eg: "0=20190101-20190201,1=20190201-20190301,2=20190301-20190401"
	 * @param jobParameter 任务参数, eg: "select user_id from user where create_time>=#{startTime} and create_time<#{endTime}"
	 */
	public AbstractJob(String cron, Integer shardingTotalCount,
                       String shardingItemParameters, String jobParameter) {
		super();
		this.cron = cron;
		this.shardingTotalCount = shardingTotalCount;
		this.shardingItemParameters = shardingItemParameters;
		this.jobParameter = jobParameter;
	}

	@Override
	public void execute(ShardingContext shardingContext) {
		LOG.info(String.format("Thread ID:%s,任务总片数:%s," + "当前分片项:%s,当前参数:%s,"
				+ "当前任务名称:%s,当前任务参数:%s", Thread.currentThread().getId(),
				shardingContext.getShardingTotalCount(),
				shardingContext.getShardingItem(),
				shardingContext.getShardingParameter(),
				shardingContext.getJobName(), shardingContext.getJobParameter()));
		executeJob(shardingContext.getShardingTotalCount(),
				shardingContext.getShardingItem(),
				shardingContext.getShardingParameter(),
				shardingContext.getJobParameter());
	}

	/**
	 * 执行任务
	 * 
	 * @param shardingTotalCount
	 *            任务分片数
	 * @param shardingItem
	 *            当前分配序号
	 * @param jobParameter
	 *            当前分配任务参数
	 */
	public abstract void executeJob(Integer shardingTotalCount,
			Integer shardingItem, String itemParameter, String jobParameter);
}
