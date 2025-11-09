# blog-frontend 技术报告

## 概述

- 基于 Vue 3（script setup）与 Vite 5 构建的单页应用，对接博客微服务体系。
- 选用 Element Plus 作为 UI 组件库，使用 Pinia 管理状态，Vue Router 负责路由导航，Axios 封装服务请求。
- 通过 Vite 开发代理接入后端服务：文章服务（8082）、认证服务（8081）、用户/通知服务与静态上传（8083）。
- 突出个性化体验：个人工作台、类实时通知轮询、Markdown 驱动的文章内容展示与丰富的主题皮肤。

## 构建与工具链

- `package.json` 提供 `dev`、`build`、`preview` 脚本；尚未配置 lint/测试工具。
- `vite.config.js` 将 `@` 映射到 `src/`，并为三大后端服务及上传接口设置 HTTP 代理。
- 全局样式位于 `src/assets/main.css`，用于定制 Element Plus 主题变量并提供通用样式。

## 应用外壳

- `src/main.js`：初始化 Vue 应用，挂载 Pinia、路由及 Element Plus。
- `App.vue`：在顶层包裹 `GlobalNav`、`<router-view>` 与 `LoginModal`，确保认证相关流程的响应式体验。

## 状态管理（Pinia）

### `authStore`

- 持久化管理 `accessToken`、`refreshToken` 与 `user`（存入 `localStorage`）。
- 登录时写入令牌，设置 Axios 默认 Authorization 头，解析 JWT 载荷获取用户 ID，并通过 `fetchMe`（用户服务查询）回填资料。
- 包含 `register`（委派用户服务并自动登录）、`logout`（可选调用黑名单 API）、`updateAvatar`（向 `/api/users/me/avatar` 上传 FormData）、`tryRefreshToken`（对接刷新接口）等方法。
- 暴露 `openLogin`/`closeLogin`、`showLoginModal` 等模态框控制，提供会话过期处理以清理状态并提示重新登录。

### `articleStore`

- 集中维护分页/筛选状态：文章状态、标签（前端多选过滤）、关键词、作者过滤等。
- `fetchArticles` 请求 `/articles`（后端页码从 0 开始），统一处理计数字段，并在作者过滤导致缺失交互数据时，通过详情接口补齐点赞/评论数。
- 附带 `fetchTags`、`filterByStatus`、`toggleTag`、`searchArticles`、`filterByAuthor`、`clearFilters` 等辅助方法。

### `notificationsStore`

- 对通知相关 REST 接口做缓存与轮询封装。
- 维护未读计数（computed）、加载状态、错误信息，并提供 `startPolling`（默认 30 秒，在个人主页延长至 45 秒），登出时优雅停止并清理。
- 暴露 `markAsRead`、`markAllAsRead` 与状态更新工具函数。

## API 层

- `src/services/api.js`：共享 Axios 实例，baseURL 为 `/api`，请求前附加 Authorization 头，遇到 401 清理令牌；封装文章相关 CRUD、标签、热榜、点赞及评论分页接口。
- `src/services/auth.js`：为认证（`/api/auth`）与用户（`/api/users`）分别创建 Axios 客户端，手动注入令牌，支持 401 时基于刷新令牌的重试，并暴露 `login`、`register`、`logout`、`refreshToken`、`getUserById`、`uploadAvatar`、`validateToken`。
- `src/services/notifications.js`：面向 `/api/notifications` 的客户端，附带认证头，提供通知查询、单条已读、全部已读等操作。

## 路由与导航

- `router/index.js`：定义五个路由（首页 Feed、文章详情、个人主页、创建、编辑）。受保护路由在未认证时会跳转到 `/` 并弹出登录框，同时通过 `redirect` 查询参数保留原始目标。
- 顶部导航组件实时展示认证状态、未读徽章（来自 Pinia）与快速操作入口。

## 视图与功能流程

### `BlogView.vue`

- 作为社区首页，挂载时重置作者过滤并拉取已发布文章。
- 侧边栏包含热榜卡片（调用 `articleApi.getHotArticles`，统计 7 天、最多 6 条）、标签筛选与基础统计。
- 搜索输入做 500ms 防抖；清空时复位筛选。分页调用 store 获取数据，从而共享过滤条件。
- CTA 按钮引导用户查看更多精选或触发特定身份动作。

### `HomeView.vue`

- 个人控制台（需登录）；挂载/监听认证变化时进行用户信息初始化、设置 `currentAuthorId`、加载文章与标签、拉取通知并启动轮询。
- 侧栏提供头像管理：文件选择校验、`AvatarCropperDialog`（CropperJS）裁剪后再通过 store 上传。
- 集成 `NotificationsPanel`，支持刷新、单条/全部已读等操作；搜索与筛选复用 store 逻辑且保持作者范围。

### `ArticleDetailView.vue`

- 展示文章元信息、封面、经 DOMPurify 清洗的 Markdown 内容、标签、点赞与评论。
- 支持点赞/取消点赞的乐观更新，失败时回退，并在认证状态变化时刷新点赞状态。
- 评论区基于后台返回结构（`content`、`totalElements` 等）分页加载，提交表单校验有效，删除仅限评论作者；含时间格式化工具。
- 监听路由参数变化以重新获取文章与评论，确保认证切换后点赞信息同步。

### `CreateArticleView.vue`

- 表单采用向导式布局，结合 Element Plus 展示摘要/正文字数与卡片预览（为新增标签生成配色）。
- 校验标题、摘要、正文长度后，调用 `articleApi.createArticle` 并显式指定状态（`DRAFT` 或 `PUBLISHED`）。

### `EditArticleView.vue`

- 挂载时加载文章详情与标签列表，填充表单并沿用创建页的校验规则（摘要长度上限 200）。
- 封面提供错误兜底占位图，提交时可在发布/草稿间切换，成功后跳回首页。

## 可复用组件

- **GlobalNav.vue**：吸顶导航，展示品牌、路由链接、未读徽章（来自通知 store），登录状态下会主动拉取通知。
- **ArticleCard.vue**：通用文章卡片，支持可选的编辑/删除操作（携带 Element 确认提示）并在删除后向父组件抛出事件。
- **LoginModal.vue**：带 Tab 的登录/注册弹窗，直接绑定 auth store 的模态状态；打开时重置表单，引用了可选的 `auth.clearError()`（未实现但做了类型保护）。
- **NotificationsPanel.vue**：可折叠面板，提供图标/内容/操作插槽，内置刷新与已读控制，并展示未读数与时间格式化。
- **AvatarCropperDialog.vue**：封装 CropperJS，输出 512×512 的圆形 PNG 头像，提供缩放/旋转控制与画布预览，向调用方返回清洗后的 `File` 对象。

## 样式与体验

- 各视图自定义大量 CSS，实现渐变背景、卡片阴影、自适应栅格等效果，丰富默认 Element Plus 风格。
- `assets/main.css` 统一主题变量并重写按钮、标签、头像等组件样式以匹配品牌调性。
- 在列表、通知、详情等场景提供加载/错误态（Skeleton、`el-alert`、`el-empty`）。

## 安全与韧性考量

- Markdown 内容通过 DOMPurify 清洗，降低富文本 XSS 风险。
- Axios 响应拦截对 401 进行令牌清理，防止未授权请求反复发送。
- Auth store 在拉取用户资料失败时尝试刷新令牌，刷新失败则强制登出以清除过期凭证。
- 评论操作在未登录情况下会明确提示并唤起登录框，避免越权。

## 观察与改进空间

- `LoginModal` 引用的 `authStore.clearError` 尚未实现，可补充以便统一清理错误提示。
- 作者过滤下为补齐互动数据会额外发送详情请求，可考虑后端增强或提供批量统计接口。
- 缺乏统一的错误提示封装，可抽象 `ElMessage` 调用以提升一致性。
- 尚未配置自动化测试与 lint，可引入 vitest/eslint 以提升可维护性。
- 通知轮询完全由前端负责，如后端支持可尝试 SSE/WebSocket 以提升扩展性。
