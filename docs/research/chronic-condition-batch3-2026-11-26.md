# 第三批慢性病资料：呼吸与心脑血管

核对日期：2026-11-26。新增五项：哮喘、慢阻肺、冠心病、心力衰竭、卒中后管理。每项四份文件，新增 20 份；累计 14 个病种/管理主题、56 份病种参考文件。卒中后管理是照护主题，计数不代表医学分类中的 14 个互斥疾病。

## 文件清单

根目录：healthpilot-agent/app/skills/chronic_condition_support/references/conditions/。

| 主题 | 文件入口 | 文件组成 |
|---|---|---|
| 哮喘 | [asthma](../../healthpilot-agent/app/skills/chronic_condition_support/references/conditions/asthma/sources.md) | sources.md、daily-care.md、monitoring-and-follow-up.md、limitations.md |
| 慢阻肺 | [copd](../../healthpilot-agent/app/skills/chronic_condition_support/references/conditions/copd/sources.md) | sources.md、daily-care.md、monitoring-and-follow-up.md、limitations.md |
| 冠心病 | [coronary-heart-disease](../../healthpilot-agent/app/skills/chronic_condition_support/references/conditions/coronary-heart-disease/sources.md) | sources.md、daily-care.md、monitoring-and-follow-up.md、limitations.md |
| 心力衰竭 | [heart-failure](../../healthpilot-agent/app/skills/chronic_condition_support/references/conditions/heart-failure/sources.md) | sources.md、daily-care.md、monitoring-and-follow-up.md、limitations.md |
| 卒中后管理 | [post-stroke](../../healthpilot-agent/app/skills/chronic_condition_support/references/conditions/post-stroke/sources.md) | sources.md、daily-care.md、monitoring-and-follow-up.md、limitations.md |

## 两轮核对与边界复查

第一轮：本次打开 12 个采用的官方健康内容页面，核实标题、正文、相关章节和更新时间/审阅日期；重新查看 [NIH 研究所目录](https://www.nih.gov/institutes-nih/list-institutes-centers)及 [NHS 网站介绍](https://www.nhs.uk/about-us/about-the-nhs-website/)确认发布机构。NHLBI 是 NIH 下属心肺血液研究所；NHS 页面属于英格兰公共医疗服务网站。官方身份不等于任何内容均适用于每位用户。

第二轮：逐项对照 NIH/NHLBI 与 NHS，结果按主题如下。没有把同一机构的两页当成两个独立机构，也不声称两机构的底层研究完全独立。

- 哮喘：两机构对诱因管理、个体化活动和个人行动计划一致。急性用药段落全部排除；R2 的紧急求助背景只用于求助提示。
- 慢阻肺：两机构对刺激物、适合活动和症状变差需医疗帮助一致。NHS living-with 地址跳转主页面，保存最终地址；不照搬英国康复课程时长。
- 冠心病：NHLBI 与 NHS 一致支持专业心脏康复；仅采用可迁移原则。急症具体表现为额外 NHS 来源，不冒称每个症状都得到两机构逐条独立验证。
- 心力衰竭：较旧 NHLBI 与较新的 NHS 对观察变化和及时联系医疗团队一致。采用共同原则，不采用数值或治疗安排；NHS living-with 已跳转主页面。
- 卒中后管理：NHLBI 与 NHS 对个体差异、多专业康复与已有计划一致。具体卒中警示例子来自 S3，不建立诊断评分；用药、手术和饮食质地处方全部排除。

第三步边界复查：只采用生活支持、已有安排和记录所需事实。检查人群、版本、冲突及与项目安全边界的兼容性，排除药物、设备调整、个体阈值、康复处方及统一检查周期。紧急求助提示不作为诊断或完整分诊流程。

本次为同一 AI 助手执行的多步骤资料核对，不是两名医师审核，不构成临床验证、系统评价或监管认证。单来源细节已在文件中明确标注。

## 时效、重定向与未采用内容

- NHLBI 心衰页更新于 2022-03-24，较旧；仅用来辅助确认与 2026 年 NHS 页一致的变化观察/及时求助原则，不宣称最新心衰治疗指南。
- NHS 慢阻肺与心衰的 living-with 入口跳转主页面，保存最终 URL。页面发布日期与访问日期分开记录，视频日期不当正文日期。
- 本次采用页均可读取正文，无需以搜索摘要替代。未下载或读取的外链、PDF及视频不作为审核证据。
- 不采用美国/英国急救号码、驾驶法规、转诊渠道、疫苗年龄规则、康复课程固定时长、用药方案与数值阈值。
- 个人行动计划只能来自专业团队；模型不根据模板新建哮喘分区或药物行动指令。
- 不自行开氧疗、限水限盐、心率训练区间、食物质地或吞咽康复方案。多病共存时保留冲突，不能用普通运动/饮食计划覆盖专科限制。

## 版权与保存方式

仅保存自撰中文简短事实笔记、出处和链接，不整页翻译、不复制全文、图片、标志或 PDF。未逐份取得原文全文再分发许可，不因公共机构身份假定全站可自由转载；本批不进行原件再分发。笔记不是官方译本，不代表 NIH/NHS 背书。

## 与产品的衔接

周计划整合已有安排、控制负担并保持 DRAFT，由用户确认；Java 负责保存、定时触发和通知。没有每日手动打卡不改变个人医嘱要求的监测频率。缺失反馈只保留未知；已有异常不自动判恢复或持续恶化。当前明显急性异常优先求助，不能等周反馈、后台提醒或用户确认普通计划。

不每日重复疾病身份，不复制无关病历至周计划。照护者提供的信息保留来源并遵循授权。

## 验证与后续

本次检查：五个新目录共 20 份文件、累计 56 份病种参考文件；UTF-8、来源编号、相对链接和修改格式。未运行收费模型、服务、临床评估或通知链路；Skills 加载和安全行为验收仍待实现。

用户要求的优先批次和随后扩展批次资料均已完成限定范围采集。肿瘤、透析/移植、复杂自身免疫病等按要求暂不开展。本次未改写已有 SKILL.md、安全文件或前两批原资料，不改变依赖、接口、Git 暂存状态，不提交。
