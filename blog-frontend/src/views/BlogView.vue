<template>
  <div class="blog-view">
    <!-- Header -->
    <header class="header-card">
      <div class="header-content">
        <div class="header-main">
          <h1 class="header-title"><i class="fas fa-blog"></i> 博客社区</h1>
          <p class="header-subtitle">发现优质文章，结识热爱创作的伙伴</p>
          <div class="header-actions">
            <el-button type="primary" class="cta-btn" @click="scrollToMain">
              <i class="fas fa-compass"></i> 探索精选
            </el-button>
            <el-button
              class="ghost-btn"
              @click="auth.isAuthenticated ? goMyBlog() : auth.openLogin()"
            >
              <i class="fas fa-pen"></i>
              {{ auth.isAuthenticated ? "管理内容" : "立即创作" }}
            </el-button>
          </div>
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
        <div class="sidebar-card hot-rank-card">
          <div class="hot-rank-header">
            <h3 class="sidebar-title"><i class="fas fa-fire"></i> 热榜</h3>
            <span class="hot-rank-subtitle">近七天</span>
          </div>
          <div v-if="hotLoading" class="hot-rank-loading">
            <el-skeleton :rows="4" animated />
          </div>
          <div v-else-if="hotError" class="hot-rank-error">
            {{ hotError }}
          </div>
          <ul v-else-if="hotArticles.length > 0" class="hot-rank-list">
            <li
              v-for="(item, index) in hotArticles"
              :key="item.articleId"
              :class="['hot-rank-item', getRankClass(index)]"
            >
              <button
                type="button"
                class="hot-rank-button"
                @click="navigateToArticle(item.articleId)"
              >
                <span class="rank-index">{{ index + 1 }}</span>
                <div class="rank-body">
                  <span class="rank-title" :title="item.title">
                    {{ item.title }}
                  </span>
                  <span class="rank-meta">
                    <i class="fas fa-bolt"></i>
                    热度值
                  </span>
                </div>
                <span class="rank-score">
                  <i class="fas fa-fire"></i>
                  {{ formatHotScore(item.hotScore) }}
                </span>
              </button>
            </li>
          </ul>
          <p v-else class="hot-rank-empty">近七天暂无热门文章</p>
        </div>

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
              class="clear-filters-btn"
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
      <main ref="mainContentRef" class="main-content">
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
import { ref, onMounted, onBeforeUnmount } from "vue";
import { Search } from "@element-plus/icons-vue";
import { useArticleStore } from "@/stores/articleStore";
import { useAuthStore } from "@/stores/authStore";
import ArticleCard from "@/components/ArticleCard.vue";
import { useRouter } from "vue-router";
import { articleApi } from "@/services/api";

const store = useArticleStore();
const auth = useAuthStore();
const searchQuery = ref("");
let searchTimeout = null;
const router = useRouter();
const mainContentRef = ref(null);

const HOT_RANK_DAYS = 7;
const HOT_RANK_LIMIT = 6;
const hotArticles = ref([]);
const hotLoading = ref(false);
const hotError = ref(null);

const fetchHotArticles = async () => {
  hotLoading.value = true;
  hotError.value = null;
  try {
    const response = await articleApi.getHotArticles(
      HOT_RANK_DAYS,
      HOT_RANK_LIMIT
    );
    hotArticles.value = Array.isArray(response.data) ? response.data : [];
  } catch (err) {
    hotArticles.value = [];
    hotError.value = err?.response?.data?.message || "无法加载热榜，请稍后重试";
  } finally {
    hotLoading.value = false;
  }
};

const formatHotScore = (score) => Math.max(0, Math.round(score ?? 0));

const getRankClass = (index) => {
  if (index === 0) {
    return "is-first";
  }
  if (index === 1) {
    return "is-second";
  }
  if (index === 2) {
    return "is-third";
  }
  return "";
};

const navigateToArticle = (articleId) => {
  if (!articleId) {
    return;
  }
  router.push({ name: "article-detail", params: { id: articleId } });
};

const scrollToMain = () => {
  if (mainContentRef.value) {
    mainContentRef.value.scrollIntoView({ behavior: "smooth", block: "start" });
  }
};

/**
 * Fetch articles and tags when component is mounted.
 */
onMounted(() => {
  // 博客首页显示所有用户的已发布文章
  store.currentAuthorId = null;
  store.fetchArticles();
  store.fetchTags();
  fetchHotArticles();
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
  fetchHotArticles();
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

onBeforeUnmount(() => {
  if (searchTimeout) {
    clearTimeout(searchTimeout);
    searchTimeout = null;
  }
});

const goMyBlog = () => {
  router.push("/my-blog");
};
</script>

<style scoped>
.blog-view {
  position: relative;
  max-width: 1280px;
  margin: 0 auto;
  padding: 2.75rem 1.5rem 3rem;
  min-height: 100vh;
  z-index: 0;
}

.blog-view::before {
  content: "";
  position: absolute;
  inset: 0;
  background: radial-gradient(
      circle at 10% 15%,
      rgba(59, 130, 246, 0.18),
      transparent 55%
    ),
    radial-gradient(
      circle at 90% 10%,
      rgba(129, 140, 248, 0.15),
      transparent 45%
    ),
    linear-gradient(
      180deg,
      rgba(249, 250, 251, 0.9) 0%,
      rgba(238, 242, 255, 0.65) 60%,
      rgba(255, 255, 255, 0.9) 100%
    );
  filter: blur(0.2px);
  z-index: -1;
}

/* Header Styles */
.header-card {
  position: relative;
  overflow: hidden;
  background: linear-gradient(
    135deg,
    rgba(59, 130, 246, 0.12),
    rgba(167, 139, 250, 0.12)
  );
  border-radius: 1.5rem;
  padding: 2.25rem 2.5rem;
  margin-bottom: 2rem;
  box-shadow: 0 28px 60px -40px rgba(30, 64, 175, 0.6);
  border: 1px solid rgba(99, 102, 241, 0.16);
}

.header-card::before,
.header-card::after {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.header-card::before {
  background: radial-gradient(
      circle at 12% 15%,
      rgba(59, 130, 246, 0.28),
      transparent 45%
    ),
    radial-gradient(
      circle at 88% 20%,
      rgba(168, 85, 247, 0.24),
      transparent 50%
    );
  opacity: 0.9;
}

.header-card::after {
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.15),
    transparent 65%
  );
}

.header-content {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 1.25rem;
  z-index: 1;
}

.header-main {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 1.25rem;
  flex: 1 1 360px;
  min-width: 0;
}

.auth-area {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 0.6rem 0.75rem;
  background: rgba(255, 255, 255, 0.55);
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 12px 40px -26px rgba(30, 64, 175, 0.6);
}

:deep(.auth-area .el-button--primary) {
  border-radius: 999px;
  padding: 0.55rem 1.3rem;
  background: linear-gradient(120deg, #2563eb, #6366f1);
  border: none;
  box-shadow: 0 16px 34px -24px rgba(37, 99, 235, 0.55);
}

.user-box {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  padding: 0.35rem 0.6rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
}

.user-name {
  color: #1f2937;
  font-weight: 600;
}

.header-title {
  font-size: 2rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.65rem;
  letter-spacing: -0.01em;
}

.header-title i {
  font-size: 1.6rem;
  color: #2563eb;
  filter: drop-shadow(0 10px 25px rgba(37, 99, 235, 0.4));
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.cta-btn,
.ghost-btn {
  border-radius: 999px;
  padding: 0.65rem 1.4rem;
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  font-weight: 600;
  letter-spacing: 0.01em;
}

.cta-btn {
  background: linear-gradient(120deg, #2563eb, #6366f1);
  border: none;
  color: #fff !important;
  box-shadow: 0 18px 40px -22px rgba(37, 99, 235, 0.6);
}

.ghost-btn {
  border: 1px solid rgba(99, 102, 241, 0.32);
  background: rgba(255, 255, 255, 0.8);
  color: #4338ca !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.ghost-btn:hover {
  border-color: rgba(99, 102, 241, 0.5);
}

.header-subtitle {
  margin: 0.25rem 0 0 0;
  color: #334155;
  font-size: 1rem;
  max-width: 36rem;
  line-height: 1.7;
}

/* Content Layout */
.content-wrapper {
  display: grid;
  grid-template-columns: minmax(0, 260px) minmax(0, 1fr);
  gap: 1.75rem;
  align-items: start;
}

.sidebar {
  position: sticky;
  top: 2rem;
  align-self: start;
}

.main-content {
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
  min-width: 0;
}

@media (max-width: 1024px) {
  .blog-view {
    padding: 2rem 1.1rem 2.5rem;
  }

  .content-wrapper {
    grid-template-columns: 1fr;
  }

  .sidebar {
    display: none;
  }

  .auth-area {
    width: 100%;
    justify-content: center;
    background: rgba(255, 255, 255, 0.82);
  }

  .header-main {
    flex: 1 1 100%;
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

  .header-actions {
    width: 100%;
    justify-content: flex-start;
  }
}

/* Sidebar Styles */
.sidebar-card,
.stats-card {
  background: rgba(255, 255, 255, 0.86);
  border-radius: 1.25rem;
  padding: 1.65rem;
  box-shadow: 0 18px 44px -32px rgba(30, 64, 175, 0.45);
  margin-bottom: 1.75rem;
  border: 1px solid rgba(148, 163, 184, 0.2);
  backdrop-filter: blur(8px);
}

.sidebar-title,
.stats-title {
  font-size: 1.02rem;
  font-weight: 700;
  color: #1e1b4b;
  margin: 0 0 1.1rem 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.hot-rank-card {
  background: linear-gradient(140deg, #eef2ff 0%, #ffffff 55%);
  border: 1px solid rgba(99, 102, 241, 0.18);
  box-shadow: 0 24px 60px -30px rgba(79, 70, 229, 0.6);
  position: relative;
  overflow: hidden;
}

.hot-rank-card::before {
  content: "";
  position: absolute;
  inset: -35% 45% 40% -15%;
  background: radial-gradient(
    circle,
    rgba(79, 70, 229, 0.24) 0%,
    rgba(79, 70, 229, 0.12) 35%,
    transparent 70%
  );
  pointer-events: none;
}

.hot-rank-card::after {
  content: "";
  position: absolute;
  width: 120px;
  height: 120px;
  right: -30px;
  bottom: -30px;
  background: radial-gradient(
    circle,
    rgba(59, 130, 246, 0.18),
    transparent 70%
  );
  pointer-events: none;
}

.hot-rank-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 1.25rem;
  position: relative;
  z-index: 1;
}

.hot-rank-header .sidebar-title {
  margin: 0;
  color: #312e81;
  font-size: 1.05rem;
}

.hot-rank-header .sidebar-title i {
  color: #6366f1;
  text-shadow: 0 10px 25px rgba(99, 102, 241, 0.35);
}

.hot-rank-subtitle {
  background: rgba(99, 102, 241, 0.15);
  color: #3730a3;
  padding: 0.15rem 0.6rem;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.hot-rank-loading {
  padding: 0.5rem 0;
}

.hot-rank-error,
.hot-rank-empty {
  position: relative;
  z-index: 1;
  margin: 0;
  padding: 0.9rem 1rem;
  border-radius: 0.9rem;
  text-align: center;
  font-size: 0.86rem;
  color: #4338ca;
  background: rgba(99, 102, 241, 0.1);
}

.hot-rank-error {
  color: #b91c1c;
  background: rgba(248, 113, 113, 0.12);
}

.hot-rank-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
  position: relative;
  z-index: 1;
}

.hot-rank-item {
  position: relative;
}

.hot-rank-button {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.9rem 1.05rem;
  background: linear-gradient(
    120deg,
    rgba(255, 255, 255, 0.82),
    rgba(255, 255, 255, 0.62)
  );
  border: 1px solid rgba(99, 102, 241, 0.12);
  border-radius: 1rem;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border 0.2s ease;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.45);
  text-align: left;
  color: #1f2937;
  font-family: inherit;
}

.hot-rank-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 30px -20px rgba(99, 102, 241, 0.45);
  border-color: rgba(99, 102, 241, 0.3);
}

.hot-rank-button:focus-visible {
  outline: 3px solid rgba(99, 102, 241, 0.35);
  outline-offset: 2px;
}

.rank-index {
  width: 2.1rem;
  height: 2.1rem;
  border-radius: 0.7rem;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1.05rem;
  background: rgba(99, 102, 241, 0.12);
  color: #4c1d95;
}

.rank-body {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.rank-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.25rem;
  max-height: 2.5rem;
}

.rank-meta {
  font-size: 0.8rem;
  color: #5b21b6;
  display: flex;
  align-items: center;
  gap: 0.35rem;
  letter-spacing: 0.01em;
  font-weight: 600;
}

.rank-meta i {
  font-size: 0.75rem;
}

.rank-score {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background: rgba(129, 140, 248, 0.18);
  color: #3730a3;
  padding: 0.35rem 0.65rem;
  border-radius: 999px;
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.rank-score i {
  font-size: 0.8rem;
}

.hot-rank-item.is-first .hot-rank-button {
  background: linear-gradient(
    130deg,
    rgba(59, 130, 246, 0.18),
    rgba(96, 165, 250, 0.12)
  );
  border-color: rgba(59, 130, 246, 0.35);
  box-shadow: 0 18px 45px -24px rgba(37, 99, 235, 0.68);
}

.hot-rank-item.is-first .rank-index {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #fff;
  box-shadow: 0 12px 25px rgba(37, 99, 235, 0.4);
}

.hot-rank-item.is-first .rank-score {
  background: rgba(37, 99, 235, 0.15);
  color: #1d4ed8;
}

.hot-rank-item.is-second .hot-rank-button {
  background: linear-gradient(
    130deg,
    rgba(129, 140, 248, 0.18),
    rgba(165, 180, 252, 0.12)
  );
  border-color: rgba(129, 140, 248, 0.32);
  box-shadow: 0 16px 40px -26px rgba(99, 102, 241, 0.55);
}

.hot-rank-item.is-second .rank-index {
  background: linear-gradient(135deg, #7c3aed, #8b5cf6);
  color: #fefefe;
  box-shadow: 0 12px 25px rgba(124, 58, 237, 0.4);
}

.hot-rank-item.is-second .rank-score {
  background: rgba(124, 58, 237, 0.14);
  color: #5b21b6;
}

.hot-rank-item.is-third .hot-rank-button {
  background: linear-gradient(
    130deg,
    rgba(251, 191, 36, 0.26),
    rgba(253, 224, 71, 0.14)
  );
  border-color: rgba(251, 191, 36, 0.35);
  box-shadow: 0 14px 38px -24px rgba(202, 138, 4, 0.5);
}

.hot-rank-item.is-third .rank-index {
  background: linear-gradient(135deg, #f59e0b, #fbbf24);
  color: #78350f;
  box-shadow: 0 12px 25px rgba(217, 119, 6, 0.35);
}

.hot-rank-item.is-third .rank-score {
  background: rgba(217, 119, 6, 0.14);
  color: #b45309;
}

.hot-rank-item:not(.is-first):not(.is-second):not(.is-third)
  .hot-rank-button:hover {
  border-color: rgba(99, 102, 241, 0.45);
}

.hot-rank-item:not(.is-first):not(.is-second):not(.is-third) .rank-score {
  background: rgba(99, 102, 241, 0.12);
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
  box-shadow: 0 10px 24px -18px rgba(30, 64, 175, 0.55);
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

.clear-filters-btn {
  color: #fff !important;
}

:deep(.clear-filters-btn .el-button__text) {
  color: #fff !important;
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
  border-bottom: 1px dashed rgba(148, 163, 184, 0.35);
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
  position: relative;
  overflow: hidden;
  background: linear-gradient(
    120deg,
    rgba(255, 255, 255, 0.95),
    rgba(239, 246, 255, 0.85)
  );
  border-radius: 1.35rem;
  padding: 1.75rem 1.9rem;
  box-shadow: 0 22px 46px -34px rgba(30, 64, 175, 0.5);
  border: 1px solid rgba(191, 219, 254, 0.5);
}

.filters-card::after {
  content: "";
  position: absolute;
  width: 180px;
  height: 180px;
  right: -60px;
  top: -80px;
  background: radial-gradient(
    circle,
    rgba(129, 140, 248, 0.18),
    transparent 70%
  );
  pointer-events: none;
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
  align-items: center;
}

.search-input {
  flex: 1;
  max-width: 420px;
}

.filter-actions {
  display: flex;
  gap: 0.75rem;
}

:deep(.filters-card .el-input__wrapper) {
  border-radius: 999px;
  padding: 0.1rem 1.05rem;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(255, 255, 255, 0.95);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.5);
}

:deep(.filters-card .el-input__wrapper.is-focus) {
  border-color: rgba(99, 102, 241, 0.55);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.18);
}

:deep(.filters-card .el-input__inner) {
  font-size: 0.95rem;
  letter-spacing: 0.01em;
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

  .filters-card {
    padding: 1.5rem;
  }
}

.action-btn {
  border-radius: 999px;
  padding: 0.55rem 1.4rem;
  border: 1px solid rgba(99, 102, 241, 0.28);
  background: rgba(255, 255, 255, 0.9);
  color: #4338ca;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.6);
}

.action-btn i {
  margin-right: 0.35rem;
}

/* Articles Container */
.articles-container {
  min-height: 400px;
}

.articles-list {
  column-count: 2;
  column-gap: 1.75rem;
  column-fill: balance;
}

.article-item {
  break-inside: avoid;
  margin-bottom: 1.75rem;
  transition: transform 0.25s ease, filter 0.25s ease;
  display: block;
}

.article-item:hover {
  transform: translateY(-4px);
}

.article-item :deep(.article-card) {
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.98),
    rgba(248, 250, 252, 0.92)
  );
  box-shadow: 0 28px 60px -36px rgba(30, 64, 175, 0.5);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.article-item:hover :deep(.article-card) {
  box-shadow: 0 32px 70px -34px rgba(30, 64, 175, 0.55);
}

/* Loading & Error States */
.loading-state {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 1.25rem;
  padding: 2.25rem;
  box-shadow: 0 24px 56px -36px rgba(30, 64, 175, 0.45);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.error-alert {
  border-radius: 1.25rem;
  margin-bottom: 1.5rem;
  border: 1px solid rgba(248, 113, 113, 0.35);
  background: rgba(254, 226, 226, 0.65);
}

.empty-state {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 1.25rem;
  padding: 3rem;
  box-shadow: 0 24px 56px -36px rgba(30, 64, 175, 0.45);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

/* Pagination */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 2rem;
  padding: 1.75rem;
  background: rgba(255, 255, 255, 0.88);
  border-radius: 1.35rem;
  box-shadow: 0 22px 48px -34px rgba(30, 64, 175, 0.5);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

:deep(.modern-pagination .el-pager li) {
  border-radius: 0.5rem;
  margin: 0 0.25rem;
}

:deep(.modern-pagination .el-pager li.is-active) {
  background: linear-gradient(120deg, #2563eb, #6366f1);
  box-shadow: 0 12px 24px -16px rgba(37, 99, 235, 0.55);
}

:deep(.modern-pagination .btn-prev),
:deep(.modern-pagination .btn-next) {
  border-radius: 0.5rem;
  background: rgba(248, 250, 252, 0.9);
}

@media (max-width: 900px) {
  .articles-list {
    column-count: 1;
  }
}
</style>
