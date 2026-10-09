# 部署
## 状态
部署配置已准备，不代表已创建公网服务。必须取得实际URL并验证健康、买家与客服流程后，才能记录“在线部署完成”。

## Render 持久化单实例
根目录 render.yaml 使用 Docker、starter 服务及1GB磁盘，属于付费配置。在用户确认具体费用前不要创建收费资源。
Docker build context=backend，Dockerfile=backend/Dockerfile，健康路径=/api/health。
H2 文件位于 /app/data，磁盘必须挂载这个目录，否则服务重启可能丢失数据。
设置 MERCHANT_PASSWORD（至少12位），MODEL_API_KEY 和 MODEL_NAME。没有模型密钥仍可以运行商城及人工客服。
创建完成后取得实际 HTTPS URL，Android 登录页填写该URL；不要猜测服务域名。

## 自有服务器
复制.env.example并设置密码，运行docker compose up --build -d；使用Caddy或Nginx配置域名与HTTPS并代理本地8080。MySQL数据保存在mysql-data卷，升级前备份。

## 上线验收
- /api/health返回ok，网页资源加载成功。
- 买家注册、下单、模拟支付、查询本人订单。
- 第二买家不能读取第一买家订单。
- 客服可模拟发货，买家可提交售后，重复审批不重复执行。
- 配置模型后生成草稿，工具记录返回真实演示订单状态，买家只能看到已审核消息。
- 人工接管时Agent暂停，旧草稿不能发送。
- Android连接公网HTTPS服务并完成登录、查单、回复。
- 服务重启后数据保留。

## 秘密与数据
所有密钥通过托管环境注入。模型会收到会话最近20条已发送消息和工具数据，客户资料需要按实际部署用途管理。不要使用真实客户资料做演示。后端单店授权已经实现，多商家隔离尚未实现。
