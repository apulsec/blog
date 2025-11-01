<template>
  <div class="blog-view">
    <!-- Header -->
    <header class="header-card">
      <div class="header-content">
        <div class="header-main">
          <h1 class="header-title"><i class="fas fa-blog"></i> 博客社区</h1>
          <p class="header-subtitle">发现优质文章，结识热爱创作的伙伴</p>
        </div>
        <div class="auth-area">
          <template v-if="auth.isAuthenticated">
            <div class="user-box">
              <el-avatar shape="circle" :size="32" :src="auth.user?.avatarUrl"
                ><i class="fas fa-user"></i
              ></el-avatar>
              <span class="user-name">{{ auth.user?.username }}</span>
              <el-dropdown>
                <span class="el-dropdown-link">
                  <i class="fas fa-caret-down"></i>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="goMyBlog"
                      ><i class="fas fa-user"></i> 我的主页</el-dropdown-item
                    >
                    <el-dropdown-item divided @click="auth.logout"
                      ><i class="fas fa-sign-out-alt"></i>
                      退出登录</el-dropdown-item
                    >
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
          <template v-else>
            <el-button type="primary" @click="auth.openLogin">登录</el-button>
          </template>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <div class="content-wrapper">
      <!-- Sidebar -->
      <aside class="sidebar">
        <!-- Tags Card -->
        <div class="sidebar-card" v-if="store.availableTags.length > 0">
          <h3 class="sidebar-title"><i class="fas fa-tags"></i> 热门标签</h3>
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
              <span class="stat-label">文章总数</span>
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
        <!-- Filters -->
        <div class="filters-card">
          <div class="filters-content">
            <div class="filter-group">
              <el-input
                v-model="searchQuery"
                placeholder="搜索文章..."
                class="search-input"
                :prefix-icon="Search"
                @input="handleSearchInput"
                clearable
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
              :show-actions="false"
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
import { ref, onMounted } from "vue";
import { Search } from "@element-plus/icons-vue";
import { useArticleStore } from "@/stores/articleStore";
import { useAuthStore } from "@/stores/authStore";
import ArticleCard from "@/components/ArticleCard.vue";
import { useRouter } from "vue-router";

const store = useArticleStore();
const auth = useAuthStore();
const searchQuery = ref("");
let searchTimeout = null;
const router = useRouter();

/**
 * Fetch articles and tags when component is mounted.
 */
onMounted(() => {
  // 博客首页显示所有用户的已发布文章
  store.currentAuthorId = null;
  store.fetchArticles();
  store.fetchTags();
});

/**
 * Handle pagination page change.
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
  if (searchTimeout) {
    clearTimeout(searchTimeout);
  }

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

const goMyBlog = () => {
  router.push("/my-blog");
};
</script>

<style scoped>
.blog-view {
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
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.header-main {
  display: flex;
  flex: 1 1 auto;
  flex-wrap: wrap;
  align-items: center;
  gap: 1rem;
  min-width: 0;
}

.auth-area {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
  flex: 0 0 auto;
  min-width: 0;
}

.user-box {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.user-name {
  color: #374151;
  font-weight: 600;
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
  margin: 0.35rem 0 0 0;
  color: #6b7280;
  font-size: 0.95rem;
  max-width: 32rem;
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
  .blog-view {
    padding: 0 1rem 2rem;
  }

  .content-wrapper {
    grid-template-columns: 1fr;
  }

  .sidebar {
    display: none;
  }

  .auth-area {
    width: 100%;
    justify-content: flex-end;
  }
}

@media (max-width: 640px) {
  .auth-area {
    justify-content: center;
  }

  .header-main {
    flex-direction: column;
    align-items: flex-start;
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

.search-input {
  flex: 1;
  max-width: 400px;
}

.filter-actions {
  display: flex;
  gap: 0.75rem;
}

@media (max-width: 768px) {
  .filter-group {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    max-width: none;
  }

  .filter-actions {
    width: 100%;
    justify-content: flex-end;
  }
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
