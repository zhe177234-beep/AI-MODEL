# AI-MODEL · 商家电商客服 Agent
原生 Android 客服应用 + Vue 自建商城 + Java Spring Boot 后端。单店毕业设计项目，所有支付、发货和退款都是模拟交易。

## 已实现
- 买家注册登录，商品列表、购物车、批量结算、下单、库存扣减、模拟支付、订单查询与售后申请。
- 客服登录、订单列表、模拟发货、人工批准售后与操作审计。
- 买家咨询、客服人工回复、Agent 草稿、人工审核发送、人工接管。
- 数据库持久化、Flyway 迁移、基于角色与订单所有权的访问检查。
- 可配置的 Chat Completions 模型接口与工具调用循环：查商品、查本人订单、检索知识。
- 模型工具只有读取能力，发货/退款由独立客服接口人工确认。
- 客户新消息、人工回复和接管使旧草稿失效；重复下单、退款审批有幂等控制。

## 项目结构
- `android/`：Kotlin / Jetpack Compose 原生客服工作台
- `backend/`：Java 17 / Spring Boot / JDBC / Vue 3 前端静态资源
- `compose.yaml`：MySQL 8.4 + 后端
- `render.yaml`：Render 持久化 H2 部署配置（付费服务与磁盘；尚未上线）
- `docs/`：使用、接口、验收与部署说明

## 本地启动：Docker
1. 复制 `.env.example` 为 `.env`，更换数据库和客服密码（至少12位）。
2. 运行 `docker compose up --build -d`。
3. 浏览器打开 http://localhost:8080，使用 `.env` 的客服账号登录；买家自行注册。
4. 健康检查 http://localhost:8080/api/health。

默认端口只绑定电脑的回环地址。真机测试需要通过受控局域网或 HTTPS 反向代理访问；不要将数据库端口暴露到公网。

## 不使用 Docker
安装 JDK 17、Maven 3.9：
```sh
mvn -f backend/pom.xml verify
export MERCHANT_PASSWORD='设置至少12位的客服密码'
java -jar backend/target/customer-agent-0.1.0.jar
```
默认数据库为工作目录下的 `data/merchant` H2 文件；MySQL 使用 `DB_URL / DB_USER / DB_PASSWORD` 配置。PowerShell 使用 `$env:MERCHANT_PASSWORD='...'`。

## 原生 Android
Android Studio 打开 `android/`，JDK 17，Gradle 8.9，SDK 35。可执行 `cd android && ./gradlew :app:assembleDebug`（Windows用 `gradlew.bat`），GitHub Actions 同样构建并上传 APK。
App 登录页可填写后端地址；模拟器用 `http://10.0.2.2:8080`，线上填写 HTTPS URL。使用客服账号登录。正式发布版本拒绝明文 HTTP，需自己的发行签名。

## 真实模型
在后端环境变量设置：
```text
MODEL_BASE_URL=https://api.openai.com/v1
MODEL_API_KEY=<通过托管服务的私密环境变量填写>
MODEL_NAME=<你账号可调用且支持工具调用的模型>
```
支持同协议服务。不能只填写 ChatGPT 订阅账号，必须有模型 API 凭据。密钥不进入 App、不进入仓库。未配置时明确显示“模型未配置”，由客服人工回复；服务失败时生成转人工提示。

## 演示流程
买家注册 → 下单 → 模拟支付 → 客服模拟发货 → 买家咨询订单 → 客服生成 Agent 草稿 → 查看工具记录 → 审核发送 → 买家申请售后 → 客服确认模拟退款。

## 验证与边界
本地已通过 Java 编译/打包与9项自动测试：权限、库存、幂等、草稿失效、接管、工具授权及模拟模型HTTP循环。详细结果见 docs/VERIFICATION.md。
真实模型质量和公网部署必须在配置真实API凭据与托管账号后继续验证。MySQL Docker 实例尚未在当前环境实测。

本版本为单店演示系统，不含真实支付、真实物流、多商家、自动发送、向量检索或大规模生产运维。知识检索使用中文字符匹配，消息通过刷新/网页5秒轮询更新。Android 采用手动刷新。
