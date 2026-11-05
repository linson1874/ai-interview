# 接口文档 — 认证模块 v0.2（Day4 登录/注销/当前用户）

> 合同性质同 01 文档；版本变更由裁决人记录。通用约定（Result 包装、错误码规则）继承用户模块文档 v0.3。
> v0.2 变更（2026-11-05，裁决#5）：Redis 存根改单键 `login:user:{userId}`(值=当前token)，天然**单端在线、重登互踢**；TTL 与 JWT 过期同源注入；新增用例 L。
> 方案：**JWT(2h) + Redis 登录态**。JWT 负责携带 userId 与防伪，Redis key `login:user:{userId}`（值=当前有效 token）负责"可注销、可互踢、单端在线"。

## 1. 凭证约定

- 登录后，客户端此后每个请求带头：`Authorization: Bearer <token>`
- 校验链：① JWT 签名/有效期合法 → ② Redis 存根 `login:user:{userId}` 存在**且值等于本 token** → 两关都过才算已登录（不等 = 已注销或被互踢）
- **未登录访问受保护接口 → HTTP 401 + code=3**（启用错误码表预留的 3）
- 登录凭证错误 → **HTTP 200 + code=1，msg 一律为"用户名或密码错误"**——不区分"用户不存在"还是"密码错"，防止攻击者枚举出哪些用户名注册过（安全惯例，面试可讲）

## 2. 接口：登录 `POST /user/login`

### 2.1 请求参数

| 字段 | 类型 | 必填 | 校验 | 提示语 |
|---|---|---|---|---|
| username | string | 是 | @NotBlank | "用户名不能为空" |
| password | string | 是 | @NotBlank | "密码不能为空" |

### 2.2 响应

成功 `data` = LoginVO：
```json
{ "code": 0, "msg": "ok", "data": { "token": "eyJhb...", "nickname": "李四" } }
```
失败（用户不存在 / 密码不匹配，**同一文案**）：
```json
{ "code": 1, "msg": "用户名或密码错误", "data": null }
```

### 2.3 业务规则

1. 按 username 查用户（@TableLogic 自动过滤 deleted）
2. `passwordEncoder.matches(明文, 库中密文)` 校验——注意是 matches 不是加密后比对！BCrypt 每次加密结果都不同（随机盐），**永远不能** `encode(输入)==库里值`
3. 通过后签发 JWT（subject=userId，时长=`jwt.expire-minutes`），**覆盖写** `login:user:{userId}`=新token、TTL 同源注入——覆盖即踢掉上一设备的存根（单端互踢；禁止代码里再出现第二个"2小时"硬编码）
4. 返回给前端的 data 里**绝不出现 password**

## 3. 接口：当前用户信息 `GET /user/me`（受保护）

成功 `data` = UserVO（id/username/nickname/avatar/email/phone，无 password/deleted）：
```json
{ "code": 0, "msg": "ok", "data": { "id": 5, "username": "lisi_3650", "nickname": "李四", "avatar": null, "email": null, "phone": null } }
```
无头/坏 token/已注销 → HTTP 401 + `{"code":3,"msg":"未登录或凭证失效"}`

## 4. 接口：注销 `POST /user/logout`（受保护）

删除 Redis `login:user:{userId}`（userId 取请求属性，Controller 不再手工 substring token）；无论 key 是否存在都返回成功（幂等）。注销后原 token 立刻 401——这就是"JWT+Redis"对"提前失效"痛点的回答。

## 5. 验收用例

| # | 场景 | 期望 |
|---|---|---|
| F | 正确账密登录（用库里已有的 lisi_3650 / 123456） | code=0，拿到 token；Redis 里 `keys login:token:*` 可见 |
| G | 密码错 | code=1，"用户名或密码错误" |
| H | 不存在的用户名 | code=1，**文案与 G 完全一致**（不许泄露"用户不存在"） |
| I | 带 token GET /user/me | code=0，返回 UserVO，**响应 JSON 里搜不到 password 字样** |
| J | 不带 / 带假 token | HTTP 401 + code=3 |
| K | logout 后再用原 token | HTTP 401（验证 Redis 删除生效） |
| L | 同账号第二次登录成功后，原设备旧 token 再请求 | HTTP 401 + code=3（旧存根已被覆盖踢掉——单端互踢生效） |

## 6. 受保护范围

拦截器拦 `/user/**`，**放行**：`/user/register`、`/user/login`（+将来 `/doc.html` 等文档资源，见 Knife4j 接入时）。
