package com.manneia.swgx.basic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.manneia.swgx.basic.model.entity.InvoiceHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 往期勾选发票信息Mapper
 *
 * @author lk
 */
@Mapper
public interface InvoiceHistoryMapper extends BaseMapper<InvoiceHistory> {
}
