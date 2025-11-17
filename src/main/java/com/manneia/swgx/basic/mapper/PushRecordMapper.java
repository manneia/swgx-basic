package com.manneia.swgx.basic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.manneia.swgx.basic.model.entity.PushRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 推送记录Mapper接口
 *
 * @author lk
 */
@Mapper
public interface PushRecordMapper extends BaseMapper<PushRecord> {
}

