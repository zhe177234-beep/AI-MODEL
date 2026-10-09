# API
除商品/政策/健康及登录注册外，请求头需要 Authorization: Bearer <token>。
请求与响应均为JSON，错误为 {"error":"说明"}。

| 方法 | 路径 | 权限/请求 |
|---|---|---|
| POST | /api/auth/register | username/password，创建买家 |
| POST | /api/auth/login | username/password，返回token/user |
| GET/POST | /api/auth/me /api/auth/logout | 登录用户 |
| GET | /api/products | 公开商品 |
| GET | /api/cart | 买家本人购物车 |
| POST | /api/cart/items | 买家：productId/quantity，0表示移除 |
| POST | /api/cart/checkout | 买家：address/requestKey，原子批量结算 |
| POST | /api/orders | 买家：productId/quantity/address/requestKey |
| GET | /api/orders | 买家看本人，客服看全店 |
| GET | /api/orders/{id} | 同上 |
| POST | /api/orders/{id}/pay | 买家本人，模拟支付 |
| POST | /api/orders/{id}/ship | 客服：tracking，模拟发货 |
| POST | /api/orders/{id}/refund | 买家本人：reason |
| GET | /api/refunds | 按角色过滤 |
| POST | /api/refunds/{id}/approve | 客服人工模拟退款 |
| GET/POST | /api/knowledge | 公开读取；客服新增title/content |
| POST | /api/conversations/mine | 买家获取或创建本人会话 |
| GET | /api/conversations | 客服会话列表 |
| GET/POST | /api/conversations/{id}/messages | 会话参与者；发送content |
| POST | /api/conversations/{id}/mode | 客服：mode=ASSISTED/HUMAN |
| POST | /api/conversations/{id}/draft | 客服：生成草稿及trace |
| POST | /api/conversations/{id}/drafts/{message}/approve | 客服审核发送 |
| GET | /api/agent/status | 客服：配置状态 |
| GET | /api/audit | 客服：最近100条操作 |
