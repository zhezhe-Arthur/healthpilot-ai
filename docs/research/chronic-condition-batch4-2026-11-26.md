# 第四批：常见专科的限定范围资料

日期：2026-11-26。用户本次授权扩展常见专科，替代此前“复杂专科暂不开展”的范围决定，仅落实本批六项，并未开放全专科。

新增 24 份参考文件，累计 20 个病种/管理主题、80 份参考文件；manifest.json 清单版本升级为 0.2.0，结构版本仍为 1.0.0。

| 主题 | 资料入口 |
|---|---|
| 甲状腺功能减退 | [hypothyroidism](../../healthpilot-agent/app/skills/chronic_condition_support/references/conditions/hypothyroidism/sources.md) |
| 甲状腺功能亢进 | [hyperthyroidism](../../healthpilot-agent/app/skills/chronic_condition_support/references/conditions/hyperthyroidism/sources.md) |
| 类风湿关节炎 | [rheumatoid-arthritis](../../healthpilot-agent/app/skills/chronic_condition_support/references/conditions/rheumatoid-arthritis/sources.md) |
| 慢性乙型肝炎 | [chronic-hepatitis-b](../../healthpilot-agent/app/skills/chronic_condition_support/references/conditions/chronic-hepatitis-b/sources.md) |
| 乳腺癌治疗后随访支持 | [breast-cancer-follow-up](../../healthpilot-agent/app/skills/chronic_condition_support/references/conditions/breast-cancer-follow-up/sources.md) |
| 结直肠癌治疗后随访支持 | [colorectal-cancer-follow-up](../../healthpilot-agent/app/skills/chronic_condition_support/references/conditions/colorectal-cancer-follow-up/sources.md) |

每包四份文件：sources.md、daily-care.md、monitoring-and-follow-up.md、limitations.md。

## 来源与两轮核对

第一轮：本次读取 11 个不同的采用正文页面，核对标题、日期及相关章节。NCI 通用随访页被两个肿瘤包共享，不算两个独立来源。采用 NIH/NIDDK、NIH/NIAMS、NIH/NCI、CDC、WHO 和 NHS。

机构身份复核沿用本会话同日已读取的 [NIH 研究所目录](https://www.nih.gov/institutes-nih/list-institutes-centers)、[NHS 网站说明](https://www.nhs.uk/about-us/about-the-nhs-website/)、[CDC 组织页](https://www.cdc.gov/about/organization/)和 [WHO 介绍](https://www.who.int/about)。其中 NIH 目录列有 NCI、NIDDK、NIAMS；不是仅凭网址或搜索排名认定权威。本次未声称重新读取全部机构页。

第二轮：比较不同机构原文，仅接纳范围明确的共同原则；单来源细节单独标注。

- 甲状腺功能减退：两机构均说明症状需专业检查确认。NIDDK 日期较旧，限一般背景；碘信息是单来源限制依据，未声称双来源逐条确认。
- 甲状腺功能亢进：NIDDK 与 NHS 对症状及专业评估一致；两页较旧且 NHS 已过计划复核日期，只开放记录与沟通，不支持最新治疗结论。
- 类风湿关节炎：两机构对个体化活动/休息及沟通一致。旧资料只支持这些一般原则；不采用药物、疫苗日程、辅助器具处方或统一复查。
- 慢性乙型肝炎：WHO 与 CDC 对专业检查/监测一致。CDC 的传播澄清和补充剂信息标为单来源；不导入美国人口筛查和疫苗政策。原 living-with URL 404，改用实际可读 Basics 页。
- 乳腺癌治疗后随访支持：NCI 的通用治疗后计划原则与 NHS 乳腺癌团队沟通要求相符；未获得双源病种专属检查日程，因此仅支持已有安排，不声称覆盖完整肿瘤康复。
- 结直肠癌治疗后随访支持：NCI 与 NHS 一致支持专业团队参与；具体随访计划依据来自 NCI 通用资料，不冒称 NHS 当前页提供了治疗后检查周期。旧 living-with 地址跳转导航页，采用实际治疗正文。

第三步边界检查：删除药物、数值目标、检查周期和诊断/治疗决策。甲亢与类风湿两组较旧资料仅支持有限记录和沟通，不声称已核对最新专科指南。肿瘤两包仅支持已由团队决定的治疗后随访，不代表已具备癌症管理、分期或复发预测能力。核对由同一 AI 助手完成，不是双医师审核或临床认证。

## 访问与时效限制

- https://www.cdc.gov/hepatitis-b/living-with/index.html 本次 404；采用 CDC Hepatitis B Basics 正文。
- https://www.nhs.uk/conditions/breast-cancer-in-women/living-with/ 本次 404；采用当前 treatment-for-breast-cancer-in-women 正文。
- NHS bowel-cancer/living-with 跳转到导航页，导航页不足以提供随访证据，改读 treatment 正文。
- NIDDK 甲减/甲亢分别审阅于 2021-03、2021-08；NIAMS 类风湿 2022-11。NHS 甲亢、类风湿计划复核日期均已过；在各 sources.md 中保留，不以仍可访问冒充最新。
- CDC 乙肝页发布、更新、审阅字段不同，照实分别记录，不用页首日期替换全部字段。
- 乳腺癌资料的 NHS 人群范围有限；不外推专属干预。结直肠癌的具体检查日程没有完成双源核验，未导入。

## 版权与产品边界

只保存自撰中文短篇事实笔记和链接，不复制全文、完整译文、图片或 PDF。未逐份取得原件再分发许可，因此不分发原件；不是官方译本或机构背书。

不自动开药、检查或康复任务，不根据疾病名称作饮食禁令。乙肝涉及感染，不把所有慢性病都视为非传染性；不主动向其他人披露健康信息。肿瘤随访不得变成普通人群筛查计划。

维持六个 Skills 架构。Java 负责授权、记录保存、定时触发和通知；模型只整理已有安排、相关约束、未知及草稿。无反馈不推断稳定，已有监测频率不因每周反馈而降低，不重复强调疾病身份。

## 验证与未实现事项

检查清单 JSON、20 个唯一 ID 与目录覆盖、80 份引用文件、各包来源编号、UTF-8 和本地相对链接。未运行模型、服务、临床验证或实时通知测试。自动加载与程序安全校验仍待实现。未改写原有 SKILL.md、安全文件或前三批资料，未提交 Git。

透析/移植、活动性肿瘤治疗及其他复杂专科尚未扩展；本次没有把它们登记为已覆盖。
