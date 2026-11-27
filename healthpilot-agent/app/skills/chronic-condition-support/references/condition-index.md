# 病种覆盖范围与资料状态

版本：草稿v0.6；资料核对日期：2026-11-26

机器可读清单：[conditions/manifest.json](conditions/manifest.json)。版本 0.2.1，登记 20 个资料包及 80 份文件，供后续加载器选择和校验；当前尚未接入自动加载。
清单别名仅供候选检索，不代表诊断或医学等价。选择后先读 limitations.md，再读 sources.md 及实际需要的内容。
新增、删除、改名或调整资料适用范围/核对状态时，同步维护本索引与清单；详细边界以对应资料正文为准。

## 一、当前能力

所有病种均可使用通用整理流程：
- 整理用户提供的信息。
- 整理已有个人安排。
- 保留监测与检查记录。
- 标注异常、冲突、缺失和待确认内容。
- 向周计划提供必要约束。

本版本不代表已具备所有病种的专属管理知识。

当前尚未完成：
- 全病种指南审核。
- 专属检查清单和复查周期。
- 专属监测阈值。
- 临床效果验证。
- 模型行为验收。

## 二、病种专属建议的准入

只有具备对应source_cross_checked资料，
且文件齐全、用户满足适用范围并已读取 limitations 时，
才提供新增的病种专属注意事项。

没有对应资料时：
- 可以整理已有医嘱。
- 可以整理就诊问题。
- 可以传递明确限制。
- 不凭模型记忆新增检查、数值或治疗性方案。

临时搜索到一篇文章不等于资料source_cross_checked。

## 三、资料状态

唯一准入状态使用 manifest.json 中的 review_status；中文“已核对”仅为 source_cross_checked 的展示文案，不是额外状态。未知状态一律不准入。

使用：
- unreviewed。
- source_cross_checked，附适用范围及核对信息。
- suspended，附原因。

当前没有列出的病种默认unreviewed。
病名识别成功不改变审核状态。

### 已有批次的限定范围资料

以下“已核对”只表示完成官方来源核实、跨机构内容对照与适用边界检查，
不表示医师审核、临床认证或模型行为已经验证。
仅开放资料中明确列出的通用注意事项，不开放数值、用药、诊断或统一检查周期。

| 病种 | 状态及适用范围 | 入口 |
|---|---|---|
| 高血压 | 已核对；非孕成年人的常规生活支持 | [来源与文件索引](conditions/hypertension/sources.md) |
| 2 型糖尿病 | 已核对；非孕成年人的一般生活支持，不外推至其他类型 | [来源与文件索引](conditions/type-2-diabetes/sources.md) |
| 慢性肾病 | 已核对；非孕成人、非透析的通用记录与生活支持；旧来源限辅助核对 | [来源与文件索引](conditions/chronic-kidney-disease/sources.md) |

每次使用先读对应 limitations.md，再按需读 daily-care.md 或 monitoring-and-follow-up.md。
新增第二批：血脂异常，限定成人通用支持；[来源与限制入口](conditions/dyslipidemia/sources.md)。
新增第二批：痛风，限定成人通用支持；[来源与限制入口](conditions/gout/sources.md)。
新增第二批：脂肪性肝病，限定成人通用支持；[来源与限制入口](conditions/fatty-liver-disease/sources.md)。
新增第二批：肥胖症，限定成人通用支持；[来源与限制入口](conditions/obesity/sources.md)。
新增第二批：骨质疏松，限定成人通用支持；[来源与限制入口](conditions/osteoporosis/sources.md)。
新增第二批：骨关节炎，限定成人通用支持；[来源与限制入口](conditions/osteoarthritis/sources.md)。
第二批核对细节见 docs/research/chronic-condition-batch2-2026-11-26.md；九个病种均非完整专科管理方案。
首批来源、受限页面与核对方法见项目 docs/research/chronic-condition-sources-2026-11-26.md。

第三批限定范围资料（均已完成来源核实、跨机构内容复核与边界检查）：

| 主题 | 适用范围 | 入口 |
|---|---|---|
| 哮喘 | 成人常规生活支持及已有专业安排整理；具体排除情形先读 limitations.md | [来源与限制入口](conditions/asthma/sources.md) |
| 慢阻肺 | 成人常规生活支持及已有专业安排整理；具体排除情形先读 limitations.md | [来源与限制入口](conditions/copd/sources.md) |
| 冠心病 | 成人常规生活支持及已有专业安排整理；具体排除情形先读 limitations.md | [来源与限制入口](conditions/coronary-heart-disease/sources.md) |
| 心力衰竭 | 成人常规生活支持及已有专业安排整理；具体排除情形先读 limitations.md | [来源与限制入口](conditions/heart-failure/sources.md) |
| 卒中后管理 | 成人常规生活支持及已有专业安排整理；具体排除情形先读 limitations.md | [来源与限制入口](conditions/post-stroke/sources.md) |

第三批记录见 docs/research/chronic-condition-batch3-2026-11-26.md。累计 14 个病种/管理主题、56 份病种参考文件；不是全病种覆盖。
不要求每日产品打卡，不得因此降低已有个人监测频率。当前急性异常优先求助，不等待周反馈或后台提醒。

## 四、后续扩展资料要求

第四批按用户最新授权增加常见专科的限定支持，非全专科准入：

| 主题 | 已核对的适用范围 | 入口 |
|---|---|---|
| 甲状腺功能减退 | 已确诊非孕成年人的一般记录与专业随诊支持 | [来源与限制](conditions/hypothyroidism/sources.md) |
| 甲状腺功能亢进 | 已确诊非孕成年人的一般记录与就诊沟通 | [来源与限制](conditions/hyperthyroidism/sources.md) |
| 类风湿关节炎 | 已确诊成年人的关节保护和已有安排整理 | [来源与限制](conditions/rheumatoid-arthritis/sources.md) |
| 慢性乙型肝炎 | 已有专业诊断成人的记录、随诊沟通与有限健康教育 | [来源与限制](conditions/chronic-hepatitis-b/sources.md) |
| 乳腺癌治疗后随访支持 | 成年乳腺癌用户进入专业团队安排的治疗后随访阶段，仅整理既有安排 | [来源与限制](conditions/breast-cancer-follow-up/sources.md) |
| 结直肠癌治疗后随访支持 | 成年结直肠癌用户治疗后既有随访与生活限制整理 | [来源与限制](conditions/colorectal-cancer-follow-up/sources.md) |

详见 docs/research/chronic-condition-batch4-2026-11-26.md。先读对应 limitations；肿瘤只整理已有治疗后随访，未开放诊断、治疗或复发预测。甲亢、类风湿等旧资料的时效限制不因进入清单而消失。

每个病种资料至少包括：
- 病种名称。
- 适用人群和阶段。
- 不适用情形。
- 发布机构、标题、版本与日期。
- 原始URL和对应章节。
- 核对日期及审核状态。
- 可支持的日常注意事项。
- 监测、复查建议的前提与例外。
- 必须由专业人员个体化确定的事项。
- 与其他限制可能发生的冲突。
- 未解决问题。

资料只提供来源允许范围内的综合笔记，
不整批复制受限制的指南全文或图片。

## 五、多病共存

- 分别保留各项来源和个人安排。
- 不将多个单病建议机械叠加。
- 不默认限制越多越安全。
- 不自行选择治疗优先级。
- 冲突时暂缓受影响的新增建议，
  并整理需要确认的问题。
- 不受影响的记录整理可以继续。

## 六、信息传递原则

对其他Skills，只传递必要内容：
- 明确限制。
- 适用条件。
- 来源和时间。
- 相关待办。
- 未知项及影响范围。

不把完整病历和无关私人信息复制到周计划。
不要求每日展示疾病清单。

## 七、一般背景来源

WHO — Noncommunicable diseases
页面日期：2025-09-25
此前核对日期：2026-11-26

https://www.who.int/news-room/fact-sheets/detail/noncommunicable-diseases

用途：
理解长期健康因素和健康服务衔接的一般背景。

限制：
不能据此生成所有病种的复查频率、个人阈值或专属安排。
非传染性疾病的范围也不等于所有慢性疾病。

本版本中的记录、状态、交接和去重规则属于项目设计，
不代表WHO或其他机构认证了HealthPilot。

## 八、维护要求

- 新增病种资料时更新真实覆盖状态。
- 来源变化或失效时重新核对。
- 不把访问日期当作发布日期。
- 不把历史验证说成本次重新验证。
- unreviewed规则不得通过其他Skill间接变成确定建议。

临床审核与模型行为验证独立记录：clinical_review_status、clinical_validation_status、model_behavior_validation_status 均不能由 source_cross_checked 推导。当前三者均为 not_performed。清单与正文冲突时暂停受影响新增建议，不能用展示文案绕过清单状态。
