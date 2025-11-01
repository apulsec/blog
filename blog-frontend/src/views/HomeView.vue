<template>
  <div class="home-view">
    <AvatarCropperDialog
      v-model="cropperVisible"
      :image-url="selectedImageUrl"
      :file-name="selectedFileName"
      @confirm="handleCropConfirm"
      @cancel="handleCropCancel"
    />
    <!-- Header -->
    <header class="header-card">
      <div class="header-content">
        <h1 class="header-title">
          <i class="fas fa-user-circle"></i> 我的博客
        </h1>
        <p class="header-subtitle">
          管理你的创作、关注互动，并随时掌握通知动态。
        </p>
      </div>
    </header>

    <!-- Main Content -->
    <div class="content-wrapper">
      <!-- Sidebar -->
      <aside class="sidebar">
        <div class="profile-card" v-if="auth.isAuthenticated">
          <h3 class="sidebar-title"><i class="fas fa-id-card"></i> 个人资料</h3>
          <div class="profile-content">
            <el-avatar shape="circle" :size="72" :src="auth.user?.avatarUrl">
              {{ displayInitial }}
            </el-avatar>
            <div class="profile-info">
              <span class="profile-name">{{ displayName }}</span>
              <el-button
                type="primary"
                size="small"
                :loading="uploadingAvatar"
                :disabled="uploadingAvatar || cropperVisible"
                @click="triggerAvatarUpload"
              >
                <i class="fas fa-upload"></i> 更换头像
              </el-button>
              <p class="profile-tip">支持 JPG/PNG/GIF/WebP，最大 5MB</p>
            </div>
          </div>
          <input
            ref="avatarInput"
            type="file"
            accept="image/*"
            class="avatar-input"
            @change="handleAvatarSelected"
          />
        </div>

        <div class="sidebar-card">
          <h3 class="sidebar-title"><i class="fas fa-bars"></i> 文章管理</h3>
          <div class="sidebar-menu">
            <a
              @click.prevent="showAllArticles"
              class="menu-item"
              :class="{
                active: !store.currentStatus && store.currentTags.length === 0,
              }"
            >
              <i class="fas fa-list"></i> 全部文章
            </a>
            <a
              @click.prevent="showDrafts"
              class="menu-item"
              :class="{ active: store.currentStatus === 'DRAFT' }"
            >
              <i class="fas fa-file-alt"></i> 草稿箱
            </a>
            <a
              @click.prevent="showPublished"
              class="menu-item"
              :class="{ active: store.currentStatus === 'PUBLISHED' }"
            >
              <i class="fas fa-check-circle"></i> 已发布
            </a>
          </div>
        </div>

        <!-- Tags Card -->
        <div class="sidebar-card" v-if="store.availableTags.length > 0">
          <h3 class="sidebar-title"><i class="fas fa-tags"></i> 标签筛选</h3>
          <div class="tags-container">
            <el-tag
              v-for="tag in store.availableTags"
              :key="tag.id"
              :color="tag.color"
              class="tag-filter"
              :class="{ 'tag-active': store.isTagSelected(tag.name) }"
              @click="store.toggleTag(tag.name)"
              effect="dark"
            >
              {{ tag.name }}
              <i
                v-if="store.isTagSelected(tag.name)"
                class="fas fa-check tag-check"
              ></i>
            </el-tag>
          </div>
          <div v-if="store.currentTags.length > 0" class="tags-info">
            <span class="tags-count"
              >已选择 {{ store.currentTags.length }} 个标签</span
            >
            <el-button
              text
              type="primary"
              size="small"
              @click="store.clearFilters()"
            >
              清除筛选
            </el-button>
          </div>
        </div>

        <!-- Stats Card -->
        <div class="stats-card">
          <h3 class="stats-title">
            <i class="fas fa-chart-line"></i> 统计信息
          </h3>
          <div class="stats-content">
            <div class="stat-item">
              <span class="stat-label">总文章数</span>
              <span class="stat-value">{{ store.totalItems }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">当前页</span>
              <span class="stat-value">{{ store.currentPage }}</span>
            </div>
          </div>
        </div>
      </aside>

      <!-- Main Content Area -->
      <main class="main-content">
        <NotificationsPanel
          v-if="auth.isAuthenticated"
          v-model:open="notificationPanelOpen"
          :unread-count="notifications.unreadCount"
          :items="notifications.items"
          :loading="notifications.loading"
          :error="notifications.error"
          @refresh="handleNotificationsRefresh"
          @mark-all="handleMarkAllNotificationsRead"
          @mark-read="handleNotificationMarked"
        />
        <!-- Filters -->
        <div class="filters-card">
          <div class="filters-content">
            <div class="filter-group">
              <el-button
                type="primary"
                size="large"
                class="create-btn"
                @click="goToCreate"
              >
                <i class="fas fa-plus"></i> 创建新文章
              </el-button>

              <el-input
                v-model="searchQuery"
                placeholder="搜索文章标题或摘要..."
                class="search-input"
                :prefix-icon="Search"
                clearable
                @input="handleSearchInput"
                @clear="handleSearchClear"
              />
            </div>

            <div class="filter-actions">
              <el-button @click="handleRefresh" class="action-btn">
                <i class="fas fa-sync"></i> 刷新
              </el-button>
            </div>
          </div>
        </div>

        <!-- Loading State -->
        <div v-if="store.loading" class="loading-state">
          <el-skeleton :rows="3" animated />
        </div>

        <!-- Error State -->
        <el-alert
          v-if="store.error"
          :title="store.error"
          type="error"
          show-icon
          class="error-alert"
        />

        <!-- Article List -->
        <div v-if="!store.loading && !store.error" class="articles-container">
          <!-- Empty State -->
          <el-empty
            v-if="!store.articles || store.articles.length === 0"
            description="暂无文章"
            class="empty-state"
          />

          <!-- Articles -->
          <div v-else class="articles-list">
            <ArticleCard
              v-for="article in store.articles"
              :key="article.id"
              :article="article"
              class="article-item"
              @deleted="handleArticleDeleted"
            />
          </div>

          <!-- Pagination -->
          <div
            v-if="store.articles && store.articles.length > 0"
            class="pagination-wrapper"
          >
            <el-pagination
              background
              layout="prev, pager, next"
              :total="store.totalItems"
              :page-size="store.pageSize"
              :current-page="store.currentPage"
              @current-change="handlePageChange"
              class="modern-pagination"
            />
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed, onBeforeUnmount } from "vue";
import { useRouter } from "vue-router";
import { Search } from "@element-plus/icons-vue";
import { useArticleStore } from "@/stores/articleStore";
import { useAuthStore } from "@/stores/authStore";
import { useNotificationsStore } from "@/stores/notificationsStore";
import ArticleCard from "@/components/ArticleCard.vue";
import NotificationsPanel from "@/components/NotificationsPanel.vue";
import { ElMessage } from "element-plus";
import AvatarCropperDialog from "@/components/AvatarCropperDialog.vue";

const router = useRouter();
const store = useArticleStore();
const searchQuery = ref("");
const auth = useAuthStore();
const notifications = useNotificationsStore();
const notificationPanelOpen = ref(true);
let searchTimeout = null; // 用于防抖

const avatarInput = ref(null);
const uploadingAvatar = ref(false);
const cropperVisible = ref(false);
const selectedImageUrl = ref("");
const selectedFileName = ref("");
let fileObjectUrl = null;

const revokeObjectUrl = () => {
  if (fileObjectUrl) {
    URL.revokeObjectURL(fileObjectUrl);
    fileObjectUrl = null;
  }
};

const displayName = computed(() => auth.user?.username || "未命名用户");

const displayInitial = computed(() => {
  const name = displayName.value;
  return name && name.length > 0 ? name.charAt(0).toUpperCase() : "U";
});

const bootstrapPersonalHome = async ({ resetPage = false } = {}) => {
  await auth.fetchMe().catch(() => {});
  if (!auth.user?.id) {
    return;
  }

  const targetPage = resetPage ? 1 : store.currentPage;
  store.currentAuthorId = auth.user.id;

  await store.fetchArticles(targetPage);
  await store.fetchTags();

  await notifications.fetchNotifications().catch(() => {});
  notifications.startPolling(45000);
  notificationPanelOpen.value = true;
};

const handleNotificationsRefresh = async () => {
  try {
    await notifications.fetchNotifications();
    ElMessage.success("通知已更新");
  } catch (err) {
    const message =
      notifications.error || err?.response?.data?.message || "刷新通知失败";
    ElMessage.error(message);
  }
};

const handleMarkAllNotificationsRead = async () => {
  if (notifications.unreadCount === 0) {
    return;
  }
  try {
    await notifications.markAllAsRead();
    ElMessage.success("通知已全部标记为已读");
  } catch (err) {
    const message =
      notifications.error ||
      err?.response?.data?.message ||
      "操作失败，请稍后重试";
    ElMessage.error(message);
  }
};

const handleNotificationMarked = async (item) => {
  if (!item?.id) {
    return;
  }
  try {
    await notifications.markAsRead(item.id);
    ElMessage.success("已标记为已读");
  } catch (err) {
    const message =
      notifications.error ||
      err?.response?.data?.message ||
      "标记失败，请稍后重试";
    ElMessage.error(message);
  }
};

/**
 * Fetch data when component is mounted.
 */
onMounted(async () => {
  if (auth.isAuthenticated) {
    await bootstrapPersonalHome({ resetPage: true });
  }
});

// When user logs in after redirection from guard
watch(
  () => auth.isAuthenticated,
  async (v) => {
    if (v) {
      await bootstrapPersonalHome({ resetPage: true });
    } else {
      store.currentAuthorId = null;
      store.articles = [];
      store.totalItems = 0;
      store.currentPage = 1;
      store.currentStatus = null;
      store.currentTags = [];
      store.currentKeyword = "";
      notificationPanelOpen.value = false;
      notifications.clear();
      router.push("/");
    }
  }
);

watch(
  () => notificationPanelOpen.value,
  (open) => {
    if (open) {
      notifications.fetchNotifications().catch(() => {});
    }
  }
);

/**
 * Handle pagination page change.
 * @param {number} newPage - The new page number to fetch
 */
const handlePageChange = (newPage) => {
  store.fetchArticles(newPage);
};

/**
 * Handle refresh button click
 */
const handleRefresh = () => {
  store.fetchArticles(store.currentPage);
};

/**
 * Handle search input with debounce
 */
const handleSearchInput = () => {
  // 清除之前的定时器
  if (searchTimeout) {
    clearTimeout(searchTimeout);
  }

  // 设置新的定时器，500ms后执行搜索
  searchTimeout = setTimeout(() => {
    if (searchQuery.value.trim()) {
      store.searchArticles(searchQuery.value.trim());
    } else {
      store.clearFilters();
    }
  }, 500);
};

/**
 * Handle search clear
 */
const handleSearchClear = () => {
  searchQuery.value = "";
  store.clearFilters();
};

/**
 * Show all articles (for current user in personal homepage)
 * Only clears status, tags and keyword filters, preserves author filter
 */
const showAllArticles = () => {
  store.currentStatus = null;
  store.currentTags = [];
  store.currentKeyword = "";
  // 保持 currentAuthorId，只刷新数据
  store.fetchArticles(1);
};

/**
 * Show only draft articles
 */
const showDrafts = () => {
  store.filterByStatus("DRAFT");
};

/**
 * Show only published articles
 */
const showPublished = () => {
  store.filterByStatus("PUBLISHED");
};

/**
 * Navigate to create article page
 */
const goToCreate = () => {
  router.push("/create");
};

/**
 * Handle article deletion
 * Refresh the article list after an article is deleted
 */
const handleArticleDeleted = () => {
  store.fetchArticles(store.currentPage);
};

const triggerAvatarUpload = () => {
  if (uploadingAvatar.value) return;
  avatarInput.value?.click();
};

const handleAvatarSelected = (event) => {
  const files = event.target?.files;
  if (!files || files.length === 0) {
    return;
  }

  const file = files[0];
  if (!file.type.startsWith("image/")) {
    ElMessage.error("请选择图片文件");
    event.target.value = "";
    return;
  }

  const maxSize = 5 * 1024 * 1024;
  if (file.size > maxSize) {
    ElMessage.error("图片大小不能超过 5MB");
    event.target.value = "";
    return;
  }

  revokeObjectUrl();
  fileObjectUrl = URL.createObjectURL(file);
  selectedImageUrl.value = fileObjectUrl;
  selectedFileName.value = file.name;
  cropperVisible.value = true;
  event.target.value = "";
};

const resetCropperState = () => {
  revokeObjectUrl();
  selectedImageUrl.value = "";
  selectedFileName.value = "";
  cropperVisible.value = false;
  if (avatarInput.value) {
    avatarInput.value.value = "";
  }
};

const handleCropCancel = () => {
  uploadingAvatar.value = false;
  resetCropperState();
};

const handleCropConfirm = async (file) => {
  uploadingAvatar.value = true;
  try {
    await auth.updateAvatar(file);
    await auth.fetchMe().catch(() => {});
    ElMessage.success("头像更新成功");
    resetCropperState();
  } catch (e) {
    if (e?.sessionExpired || e?.response?.status === 401) {
      resetCropperState();
      ElMessage.warning("登录已过期，请重新登录后再试");
    } else {
      const message = e?.response?.data?.message || "头像上传失败，请稍后重试";
      ElMessage.error(message);
      cropperVisible.value = true;
    }
  } finally {
    uploadingAvatar.value = false;
  }
};

onBeforeUnmount(() => {
  resetCropperState();
  notifications.stopPolling();
});
</script>

<style scoped>
.home-view {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 1.25rem 2.5rem;
}

/* Header Styles */
.header-card {
  background: white;
  border-radius: 1rem;
  padding: 1.5rem;
  margin-bottom: 1.5rem;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
}

.header-content {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.75rem;
}

.header-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.header-subtitle {
  margin: 0;
  color: #6b7280;
  font-size: 0.95rem;
  max-width: 36rem;
  line-height: 1.5;
}

/* Content Layout */
.content-wrapper {
  display: grid;
  grid-template-columns: minmax(0, 260px) minmax(0, 1fr);
  gap: 1.5rem;
  align-items: start;
}

.main-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  min-width: 0;
}

@media (max-width: 1024px) {
  .home-view {
    padding: 0 1rem 2rem;
  }

  .content-wrapper {
    grid-template-columns: 1fr;
  }

  .sidebar {
    display: none;
  }
}

/* Sidebar Styles */
.sidebar-card,
.stats-card {
  background: white;
  border-radius: 1rem;
  padding: 1.5rem;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
  margin-bottom: 1.5rem;
}

.profile-card {
  background: white;
  border-radius: 1rem;
  padding: 1.5rem;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
  margin-bottom: 1.5rem;
}

.profile-content {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.profile-info {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.profile-name {
  font-weight: 600;
  color: #1f2937;
  font-size: 1.1rem;
}

.profile-tip {
  margin: 0;
  font-size: 0.75rem;
  color: #9ca3af;
}

.avatar-input {
  display: none;
}

.sidebar-title,
.stats-title {
  font-size: 1rem;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 1rem 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.sidebar-menu {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.menu-item {
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  color: #6b7280;
  text-decoration: none;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
}

.menu-item:hover,
.menu-item.active {
  background: #eff6ff;
  color: #3b82f6;
}

/* Tags Container */
.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.tag-filter {
  cursor: pointer;
  transition: all 0.2s;
  color: white;
  border: none;
  font-weight: 500;
  padding: 0.5rem 0.875rem;
  position: relative;
  user-select: none;
}

.tag-filter:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.tag-active {
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.3);
  transform: translateY(-2px);
  font-weight: 600;
}

.tag-check {
  margin-left: 0.5rem;
  font-size: 0.75rem;
}

.tags-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #f3f4f6;
}

.tags-count {
  font-size: 0.875rem;
  color: #6b7280;
  font-weight: 500;
}

/* Stats */
.stats-content {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 0;
  border-bottom: 1px solid #f3f4f6;
}

.stat-item:last-child {
  border-bottom: none;
}

.stat-label {
  color: #6b7280;
  font-size: 0.875rem;
}

.stat-value {
  color: #3b82f6;
  font-weight: 600;
}

/* Filters Card */

.filters-card {
  background: white;
  border-radius: 1rem;
  padding: 1.5rem;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
}

.filters-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.filter-group {
  display: flex;
  gap: 1rem;
  flex: 1;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .filter-group {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-actions {
    width: 100%;
    justify-content: flex-end;
  }
}

.create-btn {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border: none;
  border-radius: 0.5rem;
  font-weight: 600;
  transition: all 0.2s ease-in-out;
}

.create-btn:hover {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.create-btn i {
  margin-right: 0.5rem;
}

.filter-select {
  width: 150px;
}

.search-input {
  flex: 1;
  max-width: 400px;
}

.filter-actions {
  display: flex;
  gap: 0.75rem;
}

.action-btn {
  border-radius: 0.5rem;
}

/* Articles Container */
.articles-container {
  min-height: 400px;
}

.articles-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.article-item {
  transition: all 0.2s ease-in-out;
}

/* Loading & Error States */
.loading-state {
  background: white;
  border-radius: 1rem;
  padding: 2rem;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
}

.error-alert {
  border-radius: 1rem;
  margin-bottom: 1.5rem;
}

.empty-state {
  background: white;
  border-radius: 1rem;
  padding: 3rem;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
}

/* Pagination */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 2rem;
  padding: 1.5rem;
  background: white;
  border-radius: 1rem;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
}

:deep(.modern-pagination .el-pager li) {
  border-radius: 0.5rem;
  margin: 0 0.25rem;
}

:deep(.modern-pagination .el-pager li.is-active) {
  background-color: #3b82f6;
}

:deep(.modern-pagination .btn-prev),
:deep(.modern-pagination .btn-next) {
  border-radius: 0.5rem;
}
</style>
