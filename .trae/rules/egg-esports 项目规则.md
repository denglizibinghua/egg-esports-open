---
alwaysApply: false
description: 
---
## 项目概述
电竞赛事预测平台，暗黑电竞风。预测模型为纯彩池制（Parimutuel），0% 抽水。

## 技术栈
- 后端：Spring Boot 3.0.6 / MyBatis-Plus / MySQL / Redis / JWT
- 前端：Vue 2 + Element UI（暗黑主题）
- 端口：前端 8080 → 后端 8088

## 代码规范
- Controller 路径统一 `/api/xxx`
- Service 层必须有接口 + 实现类
- 数据库操作统一用 MyBatis-Plus BaseMapper，不手写 XML
- 接口返回统一用 `Result<T>` 包装（code + msg + data）
- 前端请求统一用 `@/utils/request.js` 封装的 axios 实例
- 异常统一抛 `BusinessException`，由全局异常处理器捕获

## 禁止事项
- 禁止引入新依赖，除非先和我确认
- 禁止修改 application.yml 中的数据库/Redis/JWT 配置
- 禁止在前端使用 localStorage 存敏感信息（token 除外）
- 禁止删除已有接口或修改接口签名

## 项目结构
- 后端：`egg-server/src/main/java/com/egg/esports/`
  - controller / service / mapper / entity / config / utils
- 前端：`egg-web/src/`
  - views/（页面） / components/（组件） / utils/（工具） / api/（接口封装）