# 第二批慢性病资料：采集与复核记录

日期：2026-11-26。范围：血脂异常、痛风、脂肪性肝病、肥胖症、骨质疏松、骨关节炎。新增 24 份参考文件，保留第一批 12 份资料及六个 Skills 原有架构。当前累计九个病种目录、36 份病种资料文件。

## 清单与来源

每个目录包含 sources.md、daily-care.md、monitoring-and-follow-up.md、limitations.md。

| 病种 | 资料入口 | 交叉核对机构 |
|---|---|---|
| 血脂异常 | [dyslipidemia](../../healthpilot-agent/app/skills/chronic_condition_support/references/conditions/dyslipidemia/sources.md) | NIH/NHLBI + CDC |
| 痛风 | [gout](../../healthpilot-agent/app/skills/chronic_condition_support/references/conditions/gout/sources.md) | NIH/NIAMS + NHS |
| 脂肪性肝病 | [fatty-liver-disease](../../healthpilot-agent/app/skills/chronic_condition_support/references/conditions/fatty-liver-disease/sources.md) | NIH/NIDDK + NHS |
| 肥胖症 | [obesity](../../healthpilot-agent/app/skills/chronic_condition_support/references/conditions/obesity/sources.md) | NIH/NIDDK + WHO |
| 骨质疏松 | [osteoporosis](../../healthpilot-agent/app/skills/chronic_condition_support/references/conditions/osteoporosis/sources.md) | NIH/NIAMS + NHS |
| 骨关节炎 | [osteoarthritis](../../healthpilot-agent/app/skills/chronic_condition_support/references/conditions/osteoarthritis/sources.md) | NIH/NIAMS + NHS |

## 两轮核对与第三步边界检查

第一轮：已打开 13 个采用的官方内容页，核实正文、标题、章节及发布日期/审阅日期。以 [NIH 研究所目录](https://www.nih.gov/institutes-nih/list-institutes-centers)核对 NHLBI、NIDDK、NIAMS 身份，以 [NHS 网站说明](https://www.nhs.uk/about-us/about-the-nhs-website/)复核其英格兰公共卫生服务信息来源。WHO 与 CDC 身份核验沿用同日首批记录中的官方机构页；本批重新读取具体病种正文，不声称所有机构页均重新读取。

第二轮：逐项跨机构对照。血脂异常用 NHLBI + CDC；痛风、骨质疏松、骨关节炎用 NIAMS + NHS；脂肪性肝病用 NIDDK + NHS；肥胖症用 NIDDK + WHO。资料是机构健康教育，不冒称系统评价或专科临床指南；两家机构可能引用相同研究，不能据此宣称证据完全独立。单来源补充信息在笔记中标明，未升格为双源结论。

第三步：核对适用人群、时效、冲突、药物与检查边界。全部移除数值目标、补充剂/药物方案、固定复查周期及未经个人确认的临床安排。本次由同一 AI 助手执行，不是两名医师审核，不保证所有内容绝对正确。

## 发现及处理

- 血脂异常：L1 与 L2 对饮食、活动方向一致；L3 仅支持专业随诊背景，不导入原文的治疗后检测周期。
- 痛风：NIAMS 与 NHS 对饮食、饮料及体重管理方向一致。NHS 正文复核到期，限辅助对照，不称最新指南；不用其补充剂或英国饮酒单位建议。
- 脂肪性肝病：NIDDK 与 NHS 对饮食、按需体重管理一致；NIDDK 较旧，只取通用要点。两者酒精表述不同，排除统一限量。NHS 固定复查周期及药物段落不采用。
- 肥胖症：NIDDK 与 WHO 对个体化饮食活动及长期支持一致。NIDDK 的每日监测、具体减重比例/时间和药物/手术条件不导入；项目每周反馈不冒称与该临床项目完全等效。
- 骨质疏松：NIAMS 与 NHS 对防跌倒和个体化活动一致。旧 NIAMS 页面限通用支持；NHS living-with 地址已跳转主页面，采用最终地址。补充剂、激素及药物段落不采用。
- 骨关节炎：NIAMS 与 NHS 对个体化活动及按需体重管理一致。NHS 计划复核日期已过，仅辅助采用一致原则。两页对部分辅助疗法表述不同，相关内容全部不导入。

其他注意：CDC 血脂页更新日期比医学审阅日期新，两者分别登记。NHS 骨质疏松 living-with 页面跳转至主页面，保存最终 URL。NHS 痛风视频审阅日期不作为正文版本。脂肪性肝病保留报告原名，不自动将 NAFLD/NASH 与 MASLD/MASH 互换。所有采用页面正文均可读取；未读取材料不进入证据表。

## 版权与地域

本批仅保存自撰中文短篇事实笔记与链接，不复制原文全文、不做完整翻译、不下载 PDF/图片/标志。未逐份取得全文再分发许可，不因政府网站身份推定所有材料均可任意转载。此前记录的 WHO/CDC 使用政策仍适用；这些笔记不是机构认可的官方译文，也不构成官方背书。

外国资料的饮酒单位、急救号码、医疗转诊流程、BMI 界值、检查频率和治疗建议不直接套用于中国用户。

## 产品约束与验收

资料只供 chronic_condition_support 整理限制、已有安排和记录。weekly_planning 负责连续七天草稿、去重和负担控制；Java 负责通知、定时触发和持久化。没有反馈时不推断完成或病情稳定，不强制每日打卡，不反复提醒疾病身份。

本次检查内容：新增文件数量、UTF-8、相对链接、来源 ID、文档修改格式；不调用模型或运行医疗行为/通知端到端测试。Skills 实际加载仍未实现。

后续已列入范围但本批未完成：哮喘、慢阻肺、冠心病、心力衰竭、卒中后管理。肿瘤、透析/移植、复杂自身免疫病等专科资料按用户要求暂不开展。
