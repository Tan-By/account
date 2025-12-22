## 轻量财务系统（类 GnuCash 教学版）

基于你提供的 7 个用例规约，参考开源项目 **GnuCash** 的理念，实现的一个**教学用**小型财务系统：

- **后端**：Spring Boot 3 + Spring Data JPA + Spring Security（仅基础配置）+ H2 内存数据库  
- **前端**：Vue 3 + Vite + TypeScript + Axios  
- **特性**：支持多币种账户、复式记账、报表生成、银行对账、税务申报、内外部人员管理  

本项目重点是**用例到系统实现的完整链路**，适合作为“系统分析与设计”课程的大作业/实验展示。

---

### 一、功能一览（对应 7 个用例）

- **US007 初始化企业**  
  - 设置账套信息（企业名称、默认币种、启用日期）  
  - 录入期初科目及余额，系统校验：  
    - 资产类余额合计 = 负债类余额合计 + 权益类余额合计  
    - 损益类科目期初余额必须为 0  
  - 接口：`POST /api/init`  
  - 前端页面：`初始化企业` 菜单，对应 `InitCompanyView.vue`

- **US003 记账系统**  
  - 账户/科目管理：树状结构、账户类型（资产/负债/权益/收入/费用）、币种、当前余额。  
  - 复式记账：  
    - 一笔交易由多条分录（借/贷）组成；  
    - 本地与后端都校验“借贷必相等”；  
    - 账户余额随交易自动更新。  
  - 接口：  
    - `GET/POST/PUT/DELETE /api/accounts`  
    - `POST /api/transactions`  
  - 前端页面：  
    - `账户/科目`（账户管理）→ `AccountsView.vue`  
    - `记账（凭证）`（分录表，类 GnuCash Register）→ `TransactionsView.vue`

- **US006 企业外部人员管理**  
  - 维护供应商/客户等外部联系人（含税号、开户行、账号等）。  
  - 规则：  
    - 同一纳税人识别号在“活跃”状态下保持唯一；  
    - “停用”后不影响历史记录，但不能再用于新业务。  
  - 接口：  
    - `GET /api/external-contacts`  
    - `POST /api/external-contacts`（新增）  
    - `PUT /api/external-contacts/{id}`（修改）  
    - `POST /api/external-contacts/{id}/deactivate`（停用）  
  - 前端页面：`外部联系人` → `ContactsView.vue`

- **US004 生成财务报表**  
  - 支持选择报表期间与**报表币种**，进行多币种折算：  
    - 利润表：收入合计、费用合计、净利润；  
    - 资产负债表：资产总计、负债总计、权益总计；  
  - 报表勾稽：  
    - 后端标记 `assetTotal == liabilityTotal + equityTotal` 是否平衡。  
  - 接口：  
    - `POST /api/reports/profit`  
    - `POST /api/reports/balance-sheet`  
  - 前端页面：`财务报表` → `ReportsView.vue`

- **US002 对账系统**  
  - 基于银行账户，在指定期间内：  
    - 从“银行流水表”与“账面分录”中进行自动匹配（简化规则：金额+日期一致视为匹配）；  
    - 统计已匹配/未匹配记录数；  
    - 生成对账任务记录，给出调节前后余额。  
  - 接口：`POST /api/reconciliations`  
  - 前端页面：`银行对账` → `ReconciliationView.vue`

- **US001 税务申报系统**  
  - 根据选择的税种与申报期生成“申报草稿”（当前用固定数据模拟，真实项目可与报表对接）。  
  - 状态流转：`草稿 → 已提交 → 已受理/申报成功`。  
  - 接口：  
    - `GET /api/tax-declarations`  
    - `POST /api/tax-declarations/draft`  
    - `POST /api/tax-declarations/{id}/submit`  
    - `POST /api/tax-declarations/{id}/accept`  
    - `POST /api/tax-declarations/{id}/success`  
  - 前端页面：`税务申报` → `TaxView.vue`

- **US005 企业内部人员管理**  
  - 管理内部用户账户：姓名、用户名、部门、角色、状态（启用/禁用）。  
  - 角色与权限：  
    - `Role` 内部以权限代码集合形式存储（如 `VOUCHER_CREATE`、`REPORT_VIEW`、`USER_MGMT` 等）。  
    - 用户可分配多个角色，权限为角色权限并集（符合规约的业务含义）。  
  - 规则：  
    - 用户名唯一；  
    - 密码使用 BCrypt 加密存储；  
    - 禁用用户后不可登录（当前示例未做登录页面，只做管理接口演示）。  
  - 接口：  
    - `GET /api/users`  
    - `POST /api/users`（创建用户）  
    - `PUT /api/users/{id}`（修改用户与角色）  
    - `POST /api/users/{id}/disable`（禁用）  
  - 前端页面：`用户与权限` → `UsersView.vue`

---

### 二、项目结构

- `backend/`：Spring Boot 后端  
  - `src/main/java/com/example/accounting/domain/`  
    - `Account`、`Currency`、`ExchangeRate`、`Transaction`、`TransactionEntry`  
    - `Company`、`ExternalContact`、`BankStatementRecord`、`ReconciliationTask`  
    - `TaxDeclaration`、`UserAccount`、`Role` 等实体  
  - `repository/`：JPA 仓储接口  
  - `service/`：业务服务（初始化、记账、报表、对账、税务申报、用户管理）  
  - `web/controller/`：REST 控制器，对应前端所有按钮操作  
  - `web/dto/`：接口请求/响应 DTO  
  - `config/SecurityConfig`：关闭 CSRF，放行 H2 控制台和 Swagger，其他接口默认允许访问（便于课程演示）  
  - `src/test/java/com/example/accounting/`：若干集成测试类，覆盖初始化+记账、外部联系人、报表+对账、税务+用户管理等流程  

- `frontend/`：Vue 3 前端  
  - `src/main.ts`：应用入口，挂载路由与全局样式  
  - `src/router/index.ts`：路由与菜单映射（总览 + 7 大用例页面）  
  - `src/App.vue`：布局框架（左侧导航 + 顶部条 + 右侧主工作区），风格参考 Google 原生应用  
  - `src/views/*.vue`：  
    - `DashboardView`：总览（显示初始化状态、账户数等）  
    - `InitCompanyView`、`AccountsView`、`TransactionsView`  
    - `ReportsView`、`ReconciliationView`、`TaxView`  
    - `UsersView`、`ContactsView`  
  - `src/api.ts`：Axios 实例，统一以 `/api` 访问后端  
  - `src/styles.scss`：全局样式，浅色、扁平、轻量动效风格

---

### 三、运行方式

#### 1. 后端（Spring Boot）

前提：本机已安装 JDK 17+ 与 Maven。

```bash
cd backend
mvn spring-boot:run
```

启动成功后，后端默认监听：`http://localhost:8080`  
H2 控制台：`http://localhost:8080/h2-console`（可用于调试数据库）  

#### 2. 前端（Vue 3 + Vite）

前提：本机已安装 Node.js（建议 18+）与 npm 或 pnpm。

```bash
cd frontend
npm install
npm run dev
```

浏览器访问 `http://localhost:5173` 即可打开前端界面。  
`vite.config.mts` 中已配置将 `/api` 代理到 `http://localhost:8080`。

---

### 四、推荐演示流程（给老师看用）

1. **初始化企业（US007）**  
   - 进入“初始化企业”页面，保持示例数据（资产“现金”1000、权益“实收资本”1000）；  
   - 点击“完成初始化”：若平衡则提示“初始化成功”；若改成不平衡金额，可演示后端抛错。

2. **账户管理 + 记账（US003）**  
   - 在“账户/科目”中新增几个科目（如“银行存款”“办公用品支出”等），演示多币种字段。  
   - 打开“记账（凭证）”：  
     - 录入一笔借方=100、贷方=100 的分录，点击“保存/过账”，说明“借贷必相等”规则；  
     - 故意输入不平衡金额，展示前端与后端的双重校验。

3. **维护外部联系人（US006）**  
   - 在“外部联系人”新增一个供应商，填写纳税人识别号；  
   - 再次使用相同税号尝试新增，展示系统提示“税号已存在”；  
   - 对现有联系人执行“停用”，说明停用后不会影响历史数据。

4. **生成报表（US004）**  
   - 在“财务报表”选择期间与报表币种，点击“生成报表”；  
   - 展示：利润表中的收入、费用、净利润，以及资产负债表中“是否平衡”的标记。

5. **银行对账（US002）**  
   - 在“银行对账”选择一个资产类银行账户及对账期间，点击“开始对账”；  
   - 展示对账结果摘要：已匹配数、未匹配记录数、调节前后余额。  
   - 可配合 H2 控制台或后端测试代码解释匹配规则（金额 + 日期）。

6. **税务申报（US001）**  
   - 在“税务申报”选择税种与期间，点击“生成申报草稿”；  
   - 展示草稿中的计税依据、应纳税额和应补（退）税额；  
   - 点击“提交申报”再点击“模拟税局确认成功”，演示状态从“草稿 → 已提交 → 申报成功”的变化。

7. **内部人员管理（US005）**  
   - 在“用户与权限”页面新增一个用户，设置用户名、初始密码与角色；  
   - 修改该用户的部门、角色；  
   - 最后演示“禁用”操作（状态由“启用”变为“禁用”），强调密码加密存储与权限分离设计思路。

---

### 五、可以进一步扩展的方向（可选）

- 把当前的“税务申报草稿生成”真正与报表/记账数据挂钩，而不是模拟数值。  
- 为用户管理加上真实登录（基于 Spring Security + JWT），按角色控制前端菜单可见性。  
- 丰富 GnuCash 式的账户树展示与图表分析（如资产结构饼图、趋势折线图）。  
- 将 H2 内存库切换为持久化数据库（如 PostgreSQL/MySQL），支持多人协同与长期使用。

---

如需我帮你根据老师的具体要求再裁剪或强化某个用例（例如只重点展示 3–4 个用例），可以在此 README 基础上继续调整。**


