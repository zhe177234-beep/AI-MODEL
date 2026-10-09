# 部署
## 免费在线演示
URL：https://ai-model-merchant-agent.onrender.com
Render 服务：https://dashboard.render.com/web/srv-db441ajl550s73ag0tv0
已在用户确认的工作区创建 free、Docker、Singapore 单实例服务，使用本仓库 main 分支。当前通过公开仓库 URL 部署，推送代码不会自动上线；修改后须在 Render 选择 Manual Deploy → Deploy latest commit，或通过已授权的部署工具触发。若后续连接 Git Provider 凭据，可再启用自动部署。
根目录 render.yaml 同步为免费方案，不创建付费磁盘或数据库。H2 文件位于 /app/data 的临时文件系统，重启、休眠或重新部署可能清空演示数据。需要长期保存时，再选择持久化数据库或付费磁盘。
Docker build context=backend，Dockerfile=backend/Dockerfile。验收健康路径=/api/health。
客服用户名 merchant，密码通过 Render 的 Environment 页面查看 MERCHANT_PASSWORD，未写入仓库。Android 登录页填写上面的实际 HTTPS URL。
MODEL_API_KEY、MODEL_NAME 尚未配置；在 Render Environment 设置后自动重新部署。MODEL_BASE_URL 默认为 https://api.openai.com/v1，可改为兼容服务。没有模型密钥仍可运行商城及人工客服。

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
- 免费方案的数据持久化不作为已通过项；持久化部署须另行验证服务重启后数据保留。

## 秘密与数据
所有密钥通过托管环境注入。模型会收到会话最近20条已发送消息和工具数据，客户资料需要按实际部署用途管理。不要使用真实客户资料做演示。后端单店授权已经实现，多商家隔离尚未实现。
