# 慢性病扩展资料包：采集与核对记录

日期：2026-11-26。范围：首批高血压、2 型糖尿病、慢性肾病。其他病种尚未完成专属资料核对。用户允许采用具备权威性的美国资料。

## 交付清单

资料根目录：`healthpilot-agent/app/skills/chronic_condition_support/references/conditions/`。

| 病种 | 来源与逐项边界 | 文件 |
|---|---|---|
| 高血压 | [来源](../../healthpilot-agent/app/skills/chronic_condition_support/references/conditions/hypertension/sources.md) | sources.md、daily-care.md、monitoring-and-follow-up.md、limitations.md |
| 2 型糖尿病 | [来源](../../healthpilot-agent/app/skills/chronic_condition_support/references/conditions/type-2-diabetes/sources.md) | 同上四份 |
| 慢性肾病 | [来源](../../healthpilot-agent/app/skills/chronic_condition_support/references/conditions/chronic-kidney-disease/sources.md) | 同上四份 |

共 12 份参考文件。本记录为第 13 份新增文件。更新病种索引及两份手册；保留已有 SKILL.md、安全文件及原始资料 ZIP，不改变程序、依赖或接口。

## 核对方法与结果

这次是同一 AI 助手执行的多步骤资料核对，不是两名医师审查，也不是临床验证。

1. 第一轮：打开官方正文，核对标题、日期字段、相关章节和发布机构。用 WHO 机构页、NIH 研究所目录及 CDC 组织页再次核实机构身份，避免仅凭搜索排序或 .gov 后缀认定内容适用。
2. 第二轮：逐项对照另一机构的权威健康教育资料。高血压为 WHO + NHLBI；2 型糖尿病为 WHO + NIDDK；慢性肾病为 CDC + NIDDK。不同网页不自动算独立机构，也未证明其研究证据完全独立。
3. 边界复查：区分健康教育与诊疗指南，去除诊断、用药、目标值和统一检查周期；单来源细节明确标注，项目的数据与提醒规则明确为项目设计。

| 内容 | 交叉核对结果 | 准入范围 |
|---|---|---|
| 高血压健康生活方式、专业随诊 | H1/H2 一致 | 通用生活支持和已有安排整理 |
| 糖尿病饮食活动、团队参与 | D1/D2 一致 | 非孕成人 2 型糖尿病的一般支持 |
| 肾病检查解释、饮食个体化 | K1/K2 一致，K3 补充频率需咨询 | 已有记录与个人安排；K2 较旧，限辅助 |
| 血压目标、个体检查周期 | 存在人群、版本和地区差异 | 不导入，不让模型选择数值 |
| 眼/足/肾照护方向 | D1 背景信息 | 仅形成咨询问题；不冒称双源个人检查清单 |

机构证据：[WHO](https://www.who.int/about)、[NIH](https://www.nih.gov/institutes-nih/list-institutes-centers)、[CDC](https://www.cdc.gov/about/organization/)。临床内容原始链接与章节按病种登记；访问日期不替代页面发布日期或 Last Reviewed。

## 未采用与访问限制

- [KDIGO CKD 指南入口](https://kdigo.org/guidelines/ckd-evaluation-and-management/)：本次返回 403，未采纳无法读取的指南内容。
- [国家卫健委页面](https://www.nhc.gov.cn/wjw/c100175/202307/784d06fd2f3a4214a99c28716d13b354.shtml)：本次返回 412，未以搜索摘要替代全文审核。
- [NHS 肾病生活管理](https://www.nhs.uk/conditions/kidney-disease/living-with/)：已读，页面审阅 2023-03-22、计划复核 2026-03-22；计划复核日期已过，不列为本批主要采用来源。
- NIDDK 肾病页面 Last Reviewed 为 2016-10：仍可访问不代表已更新。仅辅助核对通用记录与饮食个体化，不采用旧数值或治疗建议。
- 不声称已覆盖所有慢性病；不根据单条记录自动诊断、分型或升级覆盖范围。

## 版权处理

本批采用短篇中文事实综合笔记和链接，不进行整页翻译，不下载/重发全文、PDF、图片、图表或标志。所有笔记非官方译文，原资料权利仍归相应权利人。

已查看 [WHO 版权政策](https://www.who.int/about/policies/publishing/copyright)与 [CDC 材料使用政策](https://www.cdc.gov/other/agencymaterials.html)。CDC 说明多数材料可使用，但存在第三方及境外版权例外；不能把整个站点视为无条件可重发。NIH 各项资源的全文再分发许可本次未逐项确认，因此同样仅保存链接和自行撰写的简短事实笔记。此记录不作法律许可保证。

## 项目使用与维护

- 先读取 condition-index，再按病种读取 limitations 与所需章节；不把来源核对状态视作临床认证。
- chronic_condition_support 整理约束、记录、既有待办与未知信息；weekly_planning 排入七天 DRAFT、去重和控制负担；Java 执行持久化、定时触发和通知。
- 无每周反馈时，不能猜测完成情况或稳定程度；保留未解决异常，只对原有已确认计划进行保守编排。模型不自行循环通知。
- 新增病种、来源失效、正文变动或引入数值规则时重新核对；本包没有自动更新机制。
- 本次验证限文件完整性、相对链接和修改范围；未运行模型、临床评估或端到端通知测试。下一步仍需实现 Skills 加载并验证实际引用、安全拒答与多病冲突行为。
