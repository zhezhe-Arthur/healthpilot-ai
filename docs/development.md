# HealthPilot AI 开发手册

> 版本：v1.1 · 2026-11-25
> 范围：第一章 Server（Java 服务）；第二章 Docker（WSL2、Docker Desktop、OpenSandbox）；第三章 HealthPilot-agent（Python AI 服务）
> 本手册记录**项目开发架构、设计约束、实现状态及技术原理**。日常启动与故障处理见《HealthPilot AI 运行手册》。

## 第一章 Server（Java 服务）

### 1.1 项目定位与技术架构

HealthPilot AI 是一个健康管理 AI 应用。用户维护健康画像与健康目标，查看每日健康计划；每周统一提交反馈，由 AI 复盘并调整下一周的七天计划。**产品没有每日手动打卡功能。**

项目采用服务分层，而非让 Agent 直接承担全部业务：

```text
React 前端（规划/开发中）
       │ HTTP / JSON
       ▼
Java Spring Boot 业务服务（healthpilot-server，:8080）
  ├─ JWT 鉴权与用户数据隔离
  ├─ 健康画像、目标、每周评估
  ├─ 七天计划管理：DRAFT → CONFIRMED
  ├─ JPA / Flyway
  └─ MySQL 8
       │ 后续由 Java 主动请求（尚未联调）
       ▼
Python FastAPI + DeepAgents（:8001，基础调用已完成）
  ├─ 单 Agent + Skills
  ├─ 只读健康数据工具与安全护栏
  └─ 按需调用 OpenSandbox（:8090，环境已部署）
```

**职责原则：**

- **Java**：真实业务状态、权限校验、数据库读写、每周计划的草稿保存与用户确认。
- **Python Agent**：基于经授权的健康信息生成结构化建议，不直接操作业务数据库。
- **MySQL**：持久化用户、健康资料、评估和计划。
- **OpenSandbox**：隔离运行不可信或需要执行环境的代码，不等同于 AI 模型本身。

### 1.2 已确定的技术基线

| 项目 | 当前方案 |
|---|---|
| 语言 | Java 17 |
| 业务框架 | Spring Boot 4.1.2 |
| 构建工具 | Maven |
| Web | Spring Web MVC |
| 认证 | Spring Security + JWT |
| 数据访问 | Spring Data JPA / Hibernate |
| 数据库 | MySQL 8 |
| 迁移 | Flyway |
| 后端源码目录 | `D:\AI\healthpilot-ai\healthpilot-server` |
| Java 服务端口 | `8080` |
| Agent 技术选型 | Python + FastAPI + DeepAgents / LangGraph（基础调用已完成，Skills 待接入） |
| 沙箱服务 | OpenSandbox Server，端口 `8090`（部署已验证） |

业务服务的启动入口是 `HealthPilotServerApplication`。源码基础包为 `com.healthpilot.server`，七天计划功能位于 `com.healthpilot.server.healthplan`。

### 1.3 Java 后端已实现范围

| 模块 | 功能 | 当前状态 |
|---|---|---|
| 01–03 | 用户注册、登录、JWT、刷新令牌及安全机制 | 已实现 |
| 04 | 健康画像、健康目标、每周健康评估及快照 | 已实现 |
| 05-1 / 05-2 | 七天计划草稿、查询、确认及冲突控制 | 已实现并验证 |
| 05-3 | Python Agent 自动生成计划 | 尚未与 Java 联调 |

七天计划的核心状态：

```text
创建草稿 → DRAFT → 用户确认 → CONFIRMED
```

需要保持的业务规则：

1. 计划由用户主动确认；AI 不绕过 Java 直接把草稿改成确认状态。
2. `CONFIRMED` 计划不能被普通草稿写入直接覆盖；已验证冲突情形返回 HTTP `409`。
3. 用户只能读写自己的健康资料和计划。
4. 每日仅查看计划；**每周**提交一次健康反馈及各计划项的执行数量，AI 据此复盘下一周。
5. 数据库历史存在 `health_daily_checkins` 表，不代表当前产品保留每日打卡功能；不要在新 UI 或接口设计中重新引入此流程。

### 1.4 已验证的七天计划接口

以下为当前已实现的接口；需要身份认证的接口应附带正确的 JWT。

| 方法 | 路径 | 用途 |
|---|---|---|
| `POST` | `/api/v1/health-plans/draft` | 创建或更新周计划草稿 |
| `GET` | `/api/v1/health-plans/{weekStartDate}` | 按周起始日期查询 |
| `GET` | `/api/v1/health-plans/` | 查询计划列表 |
| `POST` | `/api/v1/health-plans/{planId}/confirm` | 确认草稿 |
| `GET` | `/api/v1/health` | 基础健康检查接口 |

`weekStartDate` 和 `planId` 为路径参数，分别传实际日期和计划 ID。示例中不硬编码 JWT、用户数据及请求 DTO；应使用项目当前定义的字段与校验规则。

### 1.5 数据库设计与迁移约束

- 业务数据库：`healthpilot_db`；测试使用独立的 `healthpilot_test_db`，**不要把集成测试指向正式业务库**。
- 数据库结构由 Flyway 管理。项目已推进至 `V6`，其中 `V6` 创建 `health_plans`、`health_plan_items`。
- `V4` 中遗留的每日打卡表是历史数据库结构，不属于现行功能。
- Hibernate 使用 `ddl-auto: validate` 校验表结构，不能把它当作自动建表迁移机制。
- 密码等凭据采用环境变量配置，例如 `DB_PASSWORD`；不要提交到 Git。
- 计划实体使用乐观锁版本字段 `@Version`，配合服务层检查处理并发编辑与状态冲突。

推荐开发顺序：**先设计迁移脚本与实体映射 → Repository / Service → API DTO 和校验 → Controller → 自动化测试 → 手工 HTTP 验证**。

### 1.6 Java 开发与验证原则

在 Windows PowerShell 中进入业务服务目录：

```powershell
cd D:\AI\healthpilot-ai\healthpilot-server
mvn validate
mvn clean compile
mvn test
mvn package
```

上述命令分别验证 Maven 配置、编译、测试与打包；**实际执行前需要数据库及测试环境按当前项目配置可用**。也可以在 IDEA 中运行 `HealthPilotServerApplication`。

已有验证事实：

- 七天计划模块的 `HealthPlanServiceTest` 已有 4 项测试通过。
- 创建周计划草稿、查询、确认，以及对已确认计划的覆盖冲突，均已进行 HTTP 验证。
- Java/MySQL 曾出现 `1045 Access denied`，本质是连接凭据或权限问题，应先检查测试库账号和连接参数。
- Spring Boot 控制台出现自动生成的安全密码提示，不等于本项目 JWT 业务认证失效；要按实际安全配置判断。

**交付规范：**代码仅保留必要、简洁的中文注释；原理和设计解释记录在手册或沟通中。每个功能完成后以核心自动化测试和一次有效 HTTP 验证作为完成条件，不无限追加无关测试。

### 1.7 待开发：Java 与 Python Agent 的边界

这部分是**既定下一阶段方案，不代表现在已完成**。

- Java 查询并整理授权用户的健康画像、目标、每周反馈，把必要数据传给 Agent 服务。
- Python 使用一个 DeepAgents Agent，并按需加载五种 Skills：`exercise`、`nutrition`、`sleep`、`mental_health`、`weekly_planning`。
- Agent 使用只读业务工具，输出按 Pydantic 约束的结构化七天计划。
- Java 再次校验输出字段、权限及业务规则，保存为 `DRAFT`；用户确认后才进入 `CONFIRMED`。
- Guardrails 覆盖输入检查、高风险健康建议控制、可用工具限制、输出结构校验。OpenSandbox 仅在确有隔离执行需求时使用，不能直接挂载数据库凭据。

---

## 第二章 Docker（WSL2、Docker Desktop 与 OpenSandbox）

### 2.1 Docker 在本项目中的职责

Docker 为 OpenSandbox 提供可创建、执行、销毁的 Linux 容器。当前部署时：

```text
Windows 11
  ├─ Docker Desktop 4.100.1
  │    └─ Docker Engine 29.9.3（Linux 容器）
  └─ WSL2（Ubuntu-24.04，Python 3.12.3）
       ├─ OpenSandbox Server（127.0.0.1:8090）
       ├─ OpenSandbox CLI（osb）
       └─ 请求 Docker Desktop Engine 创建隔离容器
            └─ python:3.12-slim（已实测执行成功）
```

**概念区分：**WSL2 是在 Windows 上运行 Linux 的环境；Docker Engine 负责管理容器；OpenSandbox Server 负责按接口管理沙箱生命周期；DeepAgents 是后续调用沙箱的 Agent 框架。四者不是同一个组件。

### 2.2 本机已验证版本与磁盘布局

| 组件 | 版本 / 路径 | 说明 |
|---|---|---|
| WSL | `2.5.9.0` | WSL2 |
| Ubuntu | `Ubuntu-24.04` | 新发行版，已迁移成功 |
| Docker Desktop | `4.100.1` | WSL2 Linux Engine |
| Docker Engine | `29.9.3` | Client/Server 同时可用 |
| Ubuntu Python | `3.12.3` | OpenSandbox 虚拟环境 |
| Java 工程 | `D:\AI\healthpilot-ai` | 业务代码 |
| Ubuntu 虚拟磁盘 | `D:\WSL\Ubuntu-24.04` | Linux 软件和 Python 环境 |
| Docker 数据 | `E:\DockerData\wis\DockerDesktopWSL` | 镜像与容器数据 |
| OpenSandbox venv | `/home/pengz/.venvs/opensandbox` | Ubuntu 内路径 |
| OpenSandbox 配置 | `/home/pengz/.sandbox.toml` | Ubuntu 内路径，含密钥 |

旧的 `Ubuntu` 发行版因虚拟磁盘路径缺失无法正常启动，已保留其注册信息；**不要执行 `wsl --unregister Ubuntu` 或直接移动旧盘文件**。Docker Desktop 的 WSL Integration 当前仅开启 `Ubuntu-24.04`。

### 2.3 Docker Desktop 的关键配置

在 Docker Desktop 的经典设置页面：

- `Settings → Resources → WSL Integration`：新 `Ubuntu-24.04` 开启，旧 `Ubuntu` 关闭；默认发行版自动集成保持关闭。
- `Settings → Resources → Advanced → Disk image location`：Docker 数据在 E 盘。
- 在 Ubuntu 中执行 `docker version`，**必须同时出现 Client 和 Server**，才表示连接到 Docker Engine。

Docker Desktop 已自带 Linux 容器引擎，**不要在 Ubuntu 内重复安装一套 Docker Engine**，以免 CLI 连接到错误的守护进程。

### 2.4 OpenSandbox 的设计与安全配置

已使用以下命令生成 Docker 示例配置：

```bash
opensandbox-server init-config ~/.sandbox.toml --example docker
```

其中 `[server]` 的有效配置要点：

```toml
[server]
host = "127.0.0.1"
port = 8090
api_key = "<请使用本机生成的随机密钥，切勿提交到仓库>"
```

上面是**说明性片段**，不是让你直接覆盖整份 `.sandbox.toml`；配置还包含 proxy、runtime、storage、docker、ingress、egress 等分节，必须保留原始示例的其他必要参数。

安全约束：

1. 使用 `127.0.0.1` 限制在本机访问管理端口。
2. 配置 `server.api_key`；访问管理 API 时携带 `OPEN-SANDBOX-API-KEY`。
3. 配置文件权限设置为 `600`，不得打印、截图传播真实 Key。
4. 不将 MySQL 密码、JWT Secret 等业务凭据暴露给沙箱容器。
5. 沙箱创建应设置超时，测试完成后主动销毁；隔离是风险降低措施，并非绝对安全边界。

Java 的 `8080` 与 OpenSandbox 的 `8090` 是两个不同服务端口。

### 2.5 沙箱执行链路与已完成验证

```text
osb sandbox create
   → OpenSandbox Server（API Key 鉴权）
   → Docker Engine 创建容器
   → osb command run 执行 Python
   → 获取标准输出
   → osb sandbox kill 请求删除容器
```

**真实验证结果：**

- `GET http://127.0.0.1:8090/health` 返回 `{"status":"healthy"}`。
- 未带 API Key 访问管理接口得到 HTTP `401`，错误码 `MISSING_API_KEY`，说明认证生效。
- 使用 `python:3.12-slim` 成功创建沙箱，运行 Python 后输出 `OpenSandbox OK`。

**已确认的 CLI 兼容性注意事项：**

- Bash 中不要把沙箱 ID 放在 `< >` 尖括号中，否则会被解析为重定向符号。
- 当前 OpenSandbox Server 对 `osb command run ... --argv -- ...` 返回缺少 `RunCommandRequest.Command` 的校验错误。
- 这个环境已实测成功的格式为：`osb command run 实际ID -o raw -- python -c "print('OpenSandbox OK')"`。

### 2.6 开发阶段的边界与后续任务

本章已完成的是**环境部署与独立命令执行验证**，不是完整 Agent 接入。

后续开发需实现 Python Agent 调用 OpenSandbox 的工具层，并明确是否真的需要执行用户或模型生成的代码。健康计划生成优先通过 Skills、结构化输出和业务规则完成，不为了使用沙箱而增加不必要的执行环节。

---

**文档维护约定：**新增功能时同步更新实现状态；配置、路径、命令以本机实际验证为准。部署操作与日常启停流程统一收录在《HealthPilot AI 运行手册》。

## 第三章 HealthPilot-agent（Python AI 服务）

### 3.1 定位与服务边界

Agent 基于健康档案、健康目标和每周反馈生成连续七天的健康计划。采用 **单 Agent + Skills**，Java 负责授权、业务校验和持久化；生成结果经 Java 保存为 DRAFT，用户确认后才变为 CONFIRMED。没有每日手动打卡。

目标链路为：Java（8080）→ FastAPI Agent（8001）→ DeepSeek / DeepAgents → 结构化计划 → Java。当前仅完成独立服务骨架和模型基础调用，尚未完成这条业务链路。OpenSandbox（8090）仅在需要隔离执行时接入，当前 Agent 未调用沙箱。

### 3.2 当前代码结构与依赖

项目目录：`D:\AI\healthpilot-ai\healthpilot-agent`。

| 文件 / 目录 | 当前职责 |
|---|---|
| `app/main.py` | FastAPI 入口，提供 `GET /health` |
| `app/config.py` | 使用 pydantic-settings 加载环境变量和当前工作目录中的 .env |
| `app/agent/health_agent.py` | 模型初始化、角色提示和单 Agent 创建 |
| `app/api/`、`app/schemas/` | 当前仅有包初始化文件，业务 API 和计划 Schema 待开发 |
| `test_agent.py` | 真实模型调用的手工冒烟脚本，无自动化断言 |
| `requirements.txt` | fastapi、uvicorn[standard]、pydantic-settings、deepagents、langchain-openai |
| `.env.example` | 已存在但为空，配置示例尚待补齐 |
| `.env`、`.venv/` | 本地配置与独立虚拟环境，不提交 Git |

依赖清单尚未固定版本；复现时应记录实际安装版本。此次未检查本地密钥内容。

### 3.3 模型接入与单 Agent 配置

当前通过 `ChatOpenAI` 连接 `https://api.deepseek.com`，模型名读取 `DEEPSEEK_MODEL`，代码默认值为 `deepseek-flash`。这里只记录项目配置，不将模型别名与某个厂商版本永久绑定。

现有调用参数为 `temperature=0.3`、`timeout=60`、`max_retries=2`。代码通过 HarnessProfile 禁用默认通用子 Agent，同时传入 `subagents=[]`。当前 `tools=[]` 表示未添加业务自定义工具，不代表框架没有内置工具。

系统提示要求使用中文、不编造缺失健康信息、只提供一般健康管理建议、不进行疾病诊断或处方治疗，并遵循七天计划、每周反馈、用户确认规则。这些是提示层约束；程序级安全校验仍待实现。FastAPI 当前没有导入或调用 health_agent，健康检查成功不能证明模型服务可用。

### 3.4 六个 Skills 的文件与接入状态

| Skill | 规划职责 |
|---|---|
| exercise | 根据运动基础、限制条件和反馈制定渐进运动安排 |
| nutrition | 结合饮食偏好、过敏与限制制定可执行的饮食行动 |
| sleep | 根据作息和睡眠反馈制定一般性改善建议 |
| mental_health | 提供低负担的压力管理与心理健康支持 |
| weekly_planning | 综合各领域及慢性病约束，依据已知反馈编排七天计划；无反馈时保守更新，不猜测完成量 |
| chronic_condition_support | 慢性病管理和记录：整理已有安排、限制、记录、待办及未知项；不诊断或用药 |

2026-11-26 本地检查确认六个 Skill 各已有 SKILL.md、safety.md 和两份基础参考文件，共 24 份用户已有文件。现有 health_agent.py 尚未配置 Skills 加载；文件存在不等于运行接入已完成。

2026-11-26 新增高血压、2 型糖尿病、慢性肾病首批扩展资料，各四份文件，位于 chronic_condition_support/references/conditions/。采用 WHO、NIH/NHLBI、NIH/NIDDK、CDC，完成来源身份/版本核实及跨机构内容对照，详见 [核对记录](research/chronic-condition-sources-2026-11-26.md)。仅支持限定范围通用生活注意与已有安排整理，不提供诊断、用药、个人阈值或统一复查周期；不代表临床审核或全病种覆盖。

2026-11-26 第二批新增血脂异常、痛风、脂肪性肝病、肥胖症、骨质疏松、骨关节炎六个资料包，共 24 份文件。累计九个病种、36 份参考文件；完成限定范围的来源与内容核对，详见 [第二批记录](research/chronic-condition-batch2-2026-11-26.md)。保留原 Skills 架构；旧来源和跨来源差异已标注。哮喘、慢阻肺、冠心病、心衰及卒中后管理资料尚待后续完成，复杂专科暂不开展。

2026-11-26 第三批完成哮喘、慢阻肺、冠心病、心力衰竭、卒中后管理五项资料，共 20 份文件，累计 14 个病种/管理主题、56 份参考文件。此前第二批记录中的待办已在本批完成；复杂专科仍不开展。[第三批核对记录](research/chronic-condition-batch3-2026-11-26.md)记录 NHLBI/NHS 双轮核对、旧来源和不采用内容。保留个人监测频率，不能将每周产品反馈替代医学监测；心肺及卒中康复只整理已有专业安排，不自行制定处方。

会话提出的加载方案是用 CompositeBackend 分离临时状态与 Skills 文件，将 Skills 目录映射到 `/skills/`，限制写入并避免暴露项目根目录及 .env。具体 API 须按安装版本核对。验收需看到实际读取 SKILL.md 的工具调用，不能只根据模型回答判断加载成功。

### 3.5 开发进度与下一步

2026-11-26 审查修复：正式计划统一为 Java 提供的周一起始自然周，ISO 星期 1—7；运动与综合规划共用 missing-feedback-policy.md，区分条件性草稿、暂停澄清及急性安全分流。资料准入统一使用 source_cross_checked / unreviewed / suspended，临床审核、临床验证及模型行为验证分别为 not_performed。清单版本 0.2.1。新增 [跨 Skill 验收案例](research/skills-contract-acceptance.md)，仅静态检查，行为验收尚未执行。Skills 加载器与 Java 接口未修改；未调用收费模型。两份手册已在本地更新，不代表已提交或已同步远程，run.md 原有忽略规则保留。


2026-11-26 第四批根据用户新授权，增加甲减、甲亢、类风湿关节炎、慢性乙肝、乳腺癌治疗后随访支持、结直肠癌治疗后随访支持，共 24 份资料。当前累计 20 个病种/管理主题、80 份参考文件；清单版本 0.2.0（结构仍为 1.0.0）。这更新了此前暂不扩展专科的范围决定，但仅开放上述限定支持；透析/移植及活动性肿瘤治疗仍未覆盖。[第四批核对记录](research/chronic-condition-batch4-2026-11-26.md)列明跨机构核对、404 替代来源及旧版本限制。当前检查通过清单与路径完整性，不代表临床审核或运行加载完成。

2026-11-26 增加 `chronic_condition_support/references/conditions/manifest.json`，登记现有 14 个资料包。`schema_version` 表示清单结构版本，`manifest_version`/`package_version` 表示项目清单/资料包版本，不是指南版本。每项包含 ID、中文名称、检索别名、核对日期/状态、适用与排除范围摘要及四份文件路径。`files` 路径相对于清单目录，`review_record` 相对于项目根目录。别名只用于检索，不自动推导诊断或替换医学名称；详细规则仍需读取 limitations 和 sources。`source_cross_checked` 不代表临床认证。

清单已完成 JSON 解析、ID 唯一性、目录覆盖及引用文件检查；未新增运行时加载代码。后续加载器应处理未知版本、缺失文件、暂停/未核对状态和不适用人群。新增或修改资料包时同步更新清单及 condition-index；当前未自动执行这些准入校验。

| 步骤 | 当前状态 | 后续验收 |
|---|---|---|
| 05-3.2.1 FastAPI 骨架 | 代码已存在；会话有用户提供的成功启动日志 | 健康检查响应与代码一致 |
| 05-3.2.2 DeepAgents + Skills | 模型基础调用曾由用户验证；六个 Skills 文件已存在，四批 20 个病种/管理主题资料已做限定范围核对；加载与行为验收未完成 | 实际读取 Skills，生成符合业务要求的计划 |
| 05-3.2.3 结构化输出与安全校验 | 待开发 | Pydantic 计划 Schema 与关键安全约束通过验证 |
| 05-3.2.4 Java 联调 | 待开发 | Java 调用 Agent、校验、保存 DRAFT、用户确认 |

下一步继续 **05-3.2.2：完善并接入六个健康 Skills**，验证真实资料读取、多病冲突和缺失反馈处理。之后定义七天计划 Schema、校验输出，再建立 Java 调用接口。JAVA_SERVER_URL 配置存在不代表已经实现通信。Java 负责通知与持久化；本次仅增加参考资料和文档，未实现定时提醒或重新验证模型服务。

### 3.6 资料来源与维护

2026-11-25 根据会话《AI医疗项目规划》（ID：`6ae46934-2834-83ee-8cfe-9391a7fcabea`）本次接口返回的最近 5 轮及本地源码更新。会话包括用户提供的 Uvicorn 启动日志与 test_agent.py 模型回复；本次文档任务未重新启动服务或调用模型。

长期维护固定使用 development.md 与 run.md。根目录 README 和 Agent README 仍含早期日/月计划等描述，后续应按现行每周反馈方案整理。
