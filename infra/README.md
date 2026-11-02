# HealthPilot Infrastructure

## 模块简介

负责 HealthPilot AI 的开发环境、Docker 容器、服务编排和部署相关配置。

目标是让开发者能够从 GitHub 克隆项目，在完成环境配置后运行完整系统。

## 计划使用的技术

- Docker
- Docker Compose
- MySQL 8
- Nginx
- GitHub Actions

## 计划部署的服务

### Java Server
Spring Boot 核心业务服务，负责业务接口、数据访问与业务规则。

### Python Agent
基于 LangGraph 的智能体服务，负责计划生成、健康复盘、知识检索与支持性对话。

### React Web
用户访问的前端界面，由 Web 容器提供静态资源。

### MySQL
保存用户、健康画像、计划、打卡、复盘及相关业务数据。

## 部署目标

- 使用 Docker Compose 编排服务
- 通过环境变量管理应用配置
- 支持数据库持久化与迁移
- 提供清晰的启动和停止命令
- 支持独立的开发与演示环境

## 安全要求

- 不向 Git 提交真实 API Key、密码或生产环境配置
- 使用 .env.example 提供配置示例
- 数据库不对公网直接暴露
- 开源仓库仅使用虚构演示数据
- 对外服务启用必要的身份认证和访问控制

## 开发状态

部署配置将在业务基础模块完成后逐步实现。
