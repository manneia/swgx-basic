# 发票同步日志配置说明

## 日志文件位置

所有日志文件都存储在 `log-gxh/` 目录下：

### 1. 发票同步服务日志
- **文件名**: `invoice-sync.log`
- **路径**: `log-gxh/invoice-sync.log`
- **内容**: 
  - 发票状态同步过程
  - 全量发票查询接口调用
  - 状态比对详情
  - 状态变更记录

### 2. 发票批量测试日志
- **文件名**: `invoice-batch-test.log`
- **路径**: `log-gxh/invoice-batch-test.log`
- **内容**:
  - 批量采集测试过程
  - 接口调用详情
  - 数据保存记录

### 3. 应用主日志
- **文件名**: `basic.log`
- **路径**: `log-gxh/basic.log`
- **内容**: 其他所有应用日志

## 日志归档

日志文件会自动按日期归档：
- **归档路径**: `log-gxh/yyyy-MM/`
- **归档格式**: `invoice-sync.yyyy-MM-dd.i.gz`
- **单文件大小**: 最大 50MB
- **保留时间**: 60天

## 日志级别

- **InvoiceCollectionService**: INFO
- **InvoiceCollectionBatchTest**: INFO
- **其他**: INFO

## 查看日志

### Windows
```cmd
# 查看发票同步日志
type log-gxh\invoice-sync.log

# 实时监控日志（使用 PowerShell）
Get-Content log-gxh\invoice-sync.log -Wait -Tail 50

# 查看批量测试日志
type log-gxh\invoice-batch-test.log
```

### Linux/Mac
```bash
# 查看发票同步日志
cat log-gxh/invoice-sync.log

# 实时监控日志
tail -f log-gxh/invoice-sync.log

# 查看最后100行
tail -n 100 log-gxh/invoice-sync.log

# 搜索特定内容
grep "状态变化" log-gxh/invoice-sync.log
```

## 日志示例

### 发票同步日志 (invoice-sync.log)
```
2024-12-22 10:30:15.123 [pool-1-thread-1] INFO  InvoiceCollectionService - ========== 开始优化版发票状态同步 ==========
2024-12-22 10:30:15.124 [pool-1-thread-1] INFO  InvoiceCollectionService - 数据库中待同步发票数量: 19856
2024-12-22 10:30:15.125 [pool-1-thread-1] INFO  InvoiceCollectionService - 同步类型: 未入账
2024-12-22 10:30:15.126 [pool-1-thread-1] INFO  InvoiceCollectionService - 涉及纳税人数量: 9
2024-12-22 10:30:15.200 [pool-1-thread-1] INFO  InvoiceCollectionService - >>> [1/9] 开始查询纳税人 913101156072273832 的全量发票
2024-12-22 10:30:15.201 [pool-1-thread-1] INFO  InvoiceCollectionService -     ========== 全量发票查询接口调用 ==========
2024-12-22 10:30:15.202 [pool-1-thread-1] INFO  InvoiceCollectionService -     > 接口URL: http://api.baiwangjs.com/swgx-saas/swgx-api/interface/jxgl/jxqlfpcx
2024-12-22 10:30:15.203 [pool-1-thread-1] INFO  InvoiceCollectionService -     > 纳税人识别号: 913101156072273832
2024-12-22 10:30:15.204 [pool-1-thread-1] INFO  InvoiceCollectionService -     > 查询日期范围: 2024-06-22 至 2024-12-22
2024-12-22 10:30:15.205 [pool-1-thread-1] INFO  InvoiceCollectionService -     > 页码: 1, 每页大小: 200
2024-12-22 10:30:15.800 [pool-1-thread-1] INFO  InvoiceCollectionService -     > 接口响应耗时: 595 ms
2024-12-22 10:30:15.850 [pool-1-thread-1] INFO  InvoiceCollectionService -     > 响应状态: success=true, code=0, msg=操作成功
2024-12-22 10:30:15.851 [pool-1-thread-1] INFO  InvoiceCollectionService -     > 第 1 页返回 200 条记录
2024-12-22 10:30:16.100 [pool-1-thread-1] INFO  InvoiceCollectionService - 【状态变化】发票: 0400224130 23456789
2024-12-22 10:30:16.101 [pool-1-thread-1] INFO  InvoiceCollectionService -   - 发票状态: 0 -> 1
2024-12-22 10:30:16.102 [pool-1-thread-1] INFO  InvoiceCollectionService -   - 勾选状态: null -> 01
2024-12-22 10:30:16.103 [pool-1-thread-1] INFO  InvoiceCollectionService -   - 入账状态: 01 -> 02
```

### 批量测试日志 (invoice-batch-test.log)
```
2024-12-22 11:00:00.123 [main] INFO  InvoiceCollectionBatchTest - ========== 开始批量采集发票数据 ==========
2024-12-22 11:00:00.124 [main] INFO  InvoiceCollectionBatchTest - 纳税人数量: 9
2024-12-22 11:00:00.125 [main] INFO  InvoiceCollectionBatchTest - 月份范围: 2024-01 至 2024-12
2024-12-22 11:00:00.200 [main] INFO  InvoiceCollectionBatchTest - >>> 开始处理纳税人 [1/9]: 913101156072273832
2024-12-22 11:00:00.201 [main] INFO  InvoiceCollectionBatchTest -   >> 采集月份: 2024-01
2024-12-22 11:00:00.300 [main] INFO  InvoiceCollectionBatchTest -     ========== 接口调用开始 ==========
2024-12-22 11:00:00.301 [main] INFO  InvoiceCollectionBatchTest -     > 接口URL: http://api.baiwangjs.com/swgx-saas/swgx-api/interface/jxgl/jxqlfpcx
2024-12-22 11:00:00.900 [main] INFO  InvoiceCollectionBatchTest -     > 接口响应耗时: 599 ms
2024-12-22 11:00:00.950 [main] INFO  InvoiceCollectionBatchTest -     > 第 1 页返回 150 条记录
```

## 日志分析技巧

### 1. 统计状态变化数量
```bash
# Linux/Mac
grep "【状态变化】" log-gxh/invoice-sync.log | wc -l

# Windows PowerShell
(Select-String "【状态变化】" log-gxh\invoice-sync.log).Count
```

### 2. 查找特定发票
```bash
# Linux/Mac
grep "0400224130 23456789" log-gxh/invoice-sync.log

# Windows PowerShell
Select-String "0400224130 23456789" log-gxh\invoice-sync.log
```

### 3. 统计接口调用次数
```bash
# Linux/Mac
grep "接口调用开始" log-gxh/invoice-sync.log | wc -l

# Windows PowerShell
(Select-String "接口调用开始" log-gxh\invoice-sync.log).Count
```

### 4. 查看错误日志
```bash
# Linux/Mac
grep "ERROR" log-gxh/invoice-sync.log

# Windows PowerShell
Select-String "ERROR" log-gxh\invoice-sync.log
```

## 调试模式

如果需要更详细的日志（包括 DEBUG 级别），可以临时修改 `logback-spring.xml`：

```xml
<!--发票同步服务日志（调试模式）-->
<logger name="com.manneia.swgx.basic.service.InvoiceCollectionService" level="DEBUG" additivity="false">
    <appender-ref ref="INVOICE_SYNC_FILE"/>
    <appender-ref ref="CONSOLE"/>
</logger>
```

DEBUG 模式会额外输出：
- 每张发票的详细比对过程
- 数据库查询详情
- 更多的中间状态信息

## 注意事项

1. **磁盘空间**: 日志文件可能会占用较多磁盘空间，建议定期清理旧日志
2. **性能影响**: 大量日志输出可能影响性能，生产环境建议使用 INFO 级别
3. **敏感信息**: 日志中可能包含发票号码等敏感信息，注意保护日志文件安全
4. **日志轮转**: 系统会自动压缩和归档旧日志，无需手动处理

## 故障排查

### 问题1: 日志文件未生成
- 检查 `log-gxh/` 目录是否存在
- 检查应用是否有写入权限
- 查看控制台是否有错误信息

### 问题2: 日志内容不完整
- 确认日志级别设置正确
- 检查是否有异常导致日志中断
- 查看是否有日志文件大小限制

### 问题3: 找不到特定日志
- 确认时间范围是否正确
- 检查是否已被归档到月份目录
- 使用 grep/Select-String 搜索归档文件
