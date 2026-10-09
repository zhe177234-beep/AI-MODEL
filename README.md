# AI-MODEL · 电商客服 Agent
面向商家与客服的原生 Android 毕业设计项目。

## 当前状态
初始工程：Android 客服界面、Spring Boot 演示回复接口、Docker 启动配置。
当前回复来自明确标记的演示规则，没有接入大模型，也没有订单/支付/退款执行能力。工程尚未在 Android SDK 与 Maven 环境完成编译验证。请勿作为生产系统上线。

## 目录
- android/：Kotlin + Jetpack Compose 原生客户端
- backend/：Java 21 + Spring Boot
- docs/：范围、接口与开发计划

## 后端启动
安装 Docker 后，在仓库根目录运行：
```sh
docker compose up --build
```
健康检查：http://localhost:8080/api/health

## Android
Android Studio 打开 android/，安装 Android SDK 35，同步 Gradle，运行 app。
模拟器默认后端地址：http://10.0.2.2:8080；真机修改 app/build.gradle.kts 的 API_BASE_URL 为电脑局域网地址。
仅 debug 构建允许明文 HTTP；正式发行必须使用 HTTPS。

## 后续里程碑
1. MySQL 持久化、用户登录、商家权限隔离。
2. Vue 商城、商品、购物车、模拟支付、订单和售后。
3. 会话与消息持久化、实时通知、人工接管和审核发送。
4. 模型工具调用循环、知识检索、订单授权校验与人工确认。
5. 自动化测试、APK 构建、演示数据及论文实验。

接口演示不需要模型密钥。任何模型或数据库凭据都应由后端环境变量注入，禁止提交到仓库。
