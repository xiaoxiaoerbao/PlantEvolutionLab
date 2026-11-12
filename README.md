# Plant Evolution Lab

候选捕获区间评价入口为 `start.Start`，需要 Java 25，可直接在 IDEA 中运行。

无参数时使用 `/Users/xudaxing/Desktop/temp/` 下的两份深度文件、`parameters.tsv`
和 `regions/`，从同目录的 `baseline.json` 读取归一化基准，默认输出到 `/Users/xudaxing/Desktop/temp/output/`。
输出目录支持绝对路径、多级相对路径和只有一级名称的相对路径（如 `006_configuration_range_statistics`）。
相对路径以启动程序时的工作目录为起点，缺失的父目录会自动创建。
输出目录可以预先创建，也可以包含其他文件。每次计算成功后，直接覆盖该目录下的
`range_statistics.tsv`、`configuration_statistics.tsv` 和 `README.txt`，不再新建 `run-*` 子目录。
其他文件和已有子目录保留。控制台会打印本次实际输出路径。

程序参数顺序：

```text
Q20深度文件 Q0深度文件 参数表 区间目录 [输出目录 [baseline.json路径]]
```

每个 `区间目录/config_id/` 必须包含 `final.bed`、`core.bed`、`gap.bed`。
空的 gap 文件合法；core 和 gap 必须无重叠地恰好拼成 final。支持 AB、A、D、R 配置。
深度文件为无表头四列 TSV（也支持 `.gz`）：染色体、1-based 位置、XP56 观察深度、XP57 观察深度。
每条染色体必须连续成块，块内位置严格递增；两份深度文件的染色体块顺序可以不同。

XP56 是 CARMEN，XP57 是 SN055525。归一化基准读取 JSON 顶层的
`a56_CARMEN` 和 `a57_SN055525`，必须为有限正数；不使用 `comparison_bundle_k_ratio` 中的比较值。
未指定 JSON 路径时，读取 `/Users/xudaxing/Desktop/temp/baseline.json`；
在服务器上运行时，可用第六个参数指定服务器上的文件（同样支持相对路径）。
文件不可读、JSON 无效或必需字段缺失/数值无效时，报错并包含文件路径，不回退到固定基线。
原先在命令行末尾传入两个基线数值的方式已移除。
读取时将深度乘 2 一次。省略的位点/染色体按两亲本均为零处理，分母仍为完整 BED 长度。
BED 坐标为 0-based `[start,end)`。

| 输出 | 内容 |
| --- | --- |
| `range_statistics.tsv` | 各 final 区间的深度、归一化深度、覆盖广度、A/T、U、gap 比例及诊断标记 |
| `configuration_statistics.tsv` | 每个配置的区间数、总长度、Y10、C10、A90/T10、U10、无效 U 比例、B1_negative、G 及异常计数 |
| `README.txt` | 本次输入路径、记录数、归一化基准和统计口径 |

结果按参数表顺序、原始 final.bed 记录顺序输出，`range_index` 从 1 开始。
`NA` 表示未定义或不适用。空配置保留汇总行：计数、总长度、产量为 0，比例和分位数为 NA。

`B5/B10` 使用等效深度 `>=5 / >=10`；joint 要求同一碱基处两个亲本同时满足条件。
D 的正亲本是 XP57，R 的正亲本是 XP56。A/T 使用归一化深度和伪计数 0.05。
U 是 Q20 深度和除以 Q0 深度和，两者都乘 2，因此倍增因子抵消。
AB 的 u 取两亲本 U 的最小值，D/R 取正亲本 U；分母为 0 时相应 U 无效。
U10 仅使用有效 u；无效比例按区域数计算。分位数按区域等权，使用 type 7 线性插值：
`h=(n-1)*p`。C10、B1_negative、G 使用长度加权口径。

U 按公式保留原始比值，不截断到 1。Q20 深度和超过 Q0 时，在两级输出的
`q20_gt_q0_xp56/xp57`、`n_q20_gt_q0_xp56/xp57` 中标记/计数；
`u_gt1`、`n_u_gt1` 标记/计数有限且大于 1 的 u，这些有限值仍参与 U10。
诊断列用于核对两份输入的过滤和重叠处理是否一致。
程序计算 Methods S2 的统计量，不执行 Methods S4 的参数筛选。

实现分为 `RangeStatistics`、`ConfigurationStatistics`、`StatisticsCalculator`。
`Configurations` 保存输入参数和区间，`PosDepth.DepthReader` 逐行校验并提供等效深度。
统计入口无需调用 `Configurations.addRegions()`，也不创建两份全量深度数组：

1. 逐个配置收集区间端点，分块外排序去重，写入磁盘。
2. 两份深度文件各顺序读取一次，只在候选端点保存深度和、阈值计数的前缀值。
3. 通过固定预算的磁盘块缓存查询前缀差，逐个配置输出明细并汇总。
4. 只保留当前配置的 BED 和两个分位数数组，完成后删除临时索引。

默认缓存上限 64 MiB，排序块为 1,048,576 个整数（4 MiB）。可通过 JVM 参数
`-Dstatistics.cacheMiB=32 -Dstatistics.sortChunkInts=262144` 调整。
主要堆占用来自最大单个配置、排序块和缓存，另有参数表与排序批次的轻量元信息；
不会将全部配置的区间或全基因组深度记录装入内存。
临时磁盘索引约为每个唯一端点 68 字节，另需端点外排序空间。
两个 TSV 全部成功前，结果暂存在独立临时目录；失败时清理临时结果。
向已有目录发布文件时，先临时备份同名结果；若中途失败，尝试恢复旧结果。
若恢复失败，异常信息会指出保留的备份目录。

独立回归主类为 `start.StartRegressionTest`、`start.StatisticsRegressionTest`
和 `start.NormalizationBaselinesRegressionTest`。
执行 `mvn -o test-compile` 后，以项目依赖及 `target/classes`、`target/test-classes`
为类路径运行这些主类；建议 JVM 参数 `-Xmx128m`。
目前 `mvn test` 不会自动执行这些主类中的断言。
测试用独立逐碱基计算核对统计结果，覆盖补零、阈值边界、亲本方向、无效 U、U 大于 1、
空配置、外排序归并、缓存换页、长整型和最大坐标。
