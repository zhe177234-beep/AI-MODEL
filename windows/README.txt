商家客服 Agent · Windows 桌面启动版 0.2.0

适用：Windows 10 / Windows 11，使用内置 Windows PowerShell 5.1。
需要联网；推荐已安装 Microsoft Edge 或 Google Chrome。
这是基于浏览器应用窗口的桌面启动包，功能由线上网页提供，不是独立原生EXE，也不是离线版。

使用方法
1. 将 ZIP 全部解压到一个普通文件夹（不要直接在压缩包中运行）。
2. 双击 Start.cmd，打开独立客服工作台窗口。
3. 客服用户名 merchant，密码使用现有 Render MERCHANT_PASSWORD。
4. 可选：双击 Install-desktop-shortcut.cmd，创建桌面快捷方式。
   安装文件复制到当前用户 AppData，无需管理员权限，解压文件夹之后可以移动或删除。
5. Open-in-browser.cmd 可用普通浏览器打开。

包含功能
业务概览、客服会话与关联订单、订单筛选/CSV导出、售后审批、知识编辑、快捷回复设置、操作审计，以及 GrapesJS 页面模板编辑/保存/预览/HTML导出。
模板仅保存设计稿，不自动替换线上商城。

连接地址
https://ai-model-merchant-agent.onrender.com
如需更换服务，在 config.json 中修改 serviceUrl（仅支持HTTPS）。
创建快捷方式后，应修改 %LOCALAPPDATA%\AI-MODEL-Merchant\App\config.json。

运行与登录
使用浏览器应用窗口，不安装Java、Docker或安卓模拟器。
独立浏览器数据位于 %LOCALAPPDATA%\AI-MODEL-Merchant\BrowserProfile。
登录令牌保存在页面会话中，关闭窗口或会话过期后可能需要重新登录。
若脚本被单位/学校电脑策略阻止，可直接用浏览器访问服务地址，无需修改系统策略。
免费服务首次唤醒可能较慢；重启/重新部署可能清空演示数据。
当前模拟支付、发货、退款，不产生真实资金交易。真实模型需在服务端配置API密钥。

卸载
运行 Uninstall-shortcut.cmd 移除本包创建的桌面快捷方式。
如需完全移除本地浏览器数据，先关闭工作台窗口，再手动删除 %LOCALAPPDATA%\AI-MODEL-Merchant 文件夹。
这不删除云端数据。

验证说明
已检查压缩包完整性、配置格式、启动脚本路径与源码；当前执行环境为Linux，没有完成Windows真机运行验证。
源码：https://github.com/zhe177234-beep/AI-MODEL/tree/main/windows
