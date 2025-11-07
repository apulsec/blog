<template>
  <div class="article-detail-view">
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading" :size="40">
        <i class="fas fa-spinner fa-spin"></i>
      </el-icon>
      <p>加载中...</p>
    </div>

    <div v-else-if="error" class="error-container">
      <i class="fas fa-meteor error-icon"></i>
      <p class="error-message">{{ error }}</p>
      <div class="error-actions">
        <el-button type="primary" @click="fetchArticle">重新加载</el-button>
        <el-button @click="handleBack">返回</el-button>
      </div>
    </div>

    <div v-else class="article-detail-content">
      <section class="hero-card">
        <div class="hero-top">
          <button type="button" class="hero-back" @click="handleBack">
            <span class="btn-icon">⟵</span>
            返回
          </button>
          <div class="hero-actions" v-if="canEdit">
            <button type="button" class="hero-action-btn" @click="handleEdit">
              <span class="btn-icon">✏️</span>
              编辑文章
            </button>
            <button type="button" class="hero-action-btn danger" @click="handleDelete">
              <span class="btn-icon">🗑️</span>
              删除文章
            </button>
          </div>
        </div>

        <div class="hero-body">
          <el-avatar
            v-if="article?.author?.avatarUrl"
            :src="article.author.avatarUrl"
            :size="80"
            class="hero-avatar"
          />
          <el-avatar
            v-else
            :size="80"
            class="hero-avatar"
          >
            {{ article?.author?.username?.[0] || "访" }}
          </el-avatar>

          <div class="hero-info">
            <div class="hero-kicker">
              <span>{{ statusEmoji }}</span>
              <span>{{ statusText }}</span>
            </div>
            <h1 class="hero-title">{{ article?.title }}</h1>
            <div class="hero-meta">
              <span class="meta-item">
                <span class="meta-icon">🖋️</span>
                {{ article?.author?.username || "匿名作者" }}
              </span>
              <span class="meta-separator">•</span>
              <span class="meta-item">
                <span class="meta-icon">🗓️</span>
                {{ publishTimeFormatted }}
              </span>
              <span v-if="publishedRelative" class="meta-separator">•</span>
              <span v-if="publishedRelative" class="meta-item">
                <span class="meta-icon">⏳</span>
                {{ publishedRelative }}
              </span>
              <span v-if="readingTimeLabel" class="meta-separator">•</span>
              <span v-if="readingTimeLabel" class="meta-item">
                <span class="meta-icon">📖</span>
                {{ readingTimeLabel }}
              </span>
            </div>
          </div>

          <button
            type="button"
            class="hero-like"
            :class="{ liked }"
            :disabled="likeLoading"
            @click="toggleLike"
          >
            <span class="btn-icon">{{ liked ? "❤️" : "🤍" }}</span>
            {{ likeButtonText }}
          </button>
        </div>

        <p v-if="article?.summary" class="hero-summary">{{ article.summary }}</p>

        <div v-if="article?.tags?.length" class="hero-tags">
          <el-tag
            v-for="tag in article.tags"
            :key="tag.id || tag.name"
            class="hero-tag"
            :style="tag?.color ? { backgroundColor: tag.color } : {}"
            effect="dark"
          >
            #{{ tag.name }}
          </el-tag>
        </div>

        <div class="hero-stats">
          <div class="stat-card highlight">
            <span class="stat-label">阅读时长</span>
            <span class="stat-value">{{ readingTimeLabel }}</span>
            <span class="stat-hint">约 {{ wordCountDisplay }} 字</span>
          </div>
          <div class="stat-card">
            <span class="stat-label">获赞</span>
            <span class="stat-value">{{ likeCountDisplay }}</span>
            <span class="stat-hint">感谢每一次支持</span>
          </div>
          <div class="stat-card">
            <span class="stat-label">评论</span>
            <span class="stat-value">{{ commentCountDisplay }}</span>
            <span class="stat-hint">期待更多交流</span>
          </div>
          <div v-if="tagCount" class="stat-card">
            <span class="stat-label">标签</span>
            <span class="stat-value">{{ tagCountDisplay }}</span>
            <span class="stat-hint">多彩主题</span>
          </div>
        </div>
      </section>

      <section class="article-shell">
        <div v-if="article?.coverImageUrl" class="article-cover">
          <el-image
            class="cover-image"
            :src="article.coverImageUrl"
            :preview-src-list="[article.coverImageUrl]"
            fit="cover"
          />
        </div>

        <article class="article-body">
          <div class="article-content" v-html="renderedContent"></div>
        </article>
      </section>

      <section class="comment-panel">
        <header class="comment-header">
          <h2 class="section-title">评论区</h2>
          <span class="comment-count-badge">{{ commentTotal }} 条交流</span>
        </header>

        <p v-if="commentError" class="comment-error-text">
          <i class="fas fa-exclamation-triangle"></i>
          {{ commentError }}
        </p>

        <div v-if="isAuthenticated" class="comment-form-card">
          <el-input
            v-model="commentContent"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="写下你的想法..."
          />
          <div class="comment-form-actions">
            <el-button
              class="comment-submit-btn"
              type="primary"
              :loading="submittingComment"
              :disabled="!canSubmitComment"
              @click="submitComment"
            >
              发送评论
            </el-button>
          </div>
        </div>
        <div v-else class="comment-login-hint">
          <p>登录后即可加入讨论，一起交流想法。</p>
          <el-button type="primary" @click="openLogin">立即登录</el-button>
        </div>

        <div v-if="commentsLoading" class="comments-loading">
          <i class="fas fa-spinner fa-spin loading-spinner"></i>
          正在载入评论...
        </div>

        <el-empty
          v-else-if="!comments.length"
          class="comment-empty"
          description="还没有评论，期待你的第一条留言"
        />

        <ul v-else class="comment-list">
          <li v-for="comment in comments" :key="comment.id" class="comment-item">
            <div class="comment-card">
              <div class="comment-top">
                <div class="comment-author">
                  <el-avatar
                    v-if="comment?.author?.avatarUrl"
                    :size="48"
                    class="comment-avatar"
                    :src="comment.author.avatarUrl"
                  />
                  <el-avatar v-else :size="48" class="comment-avatar">
                    {{ comment?.author?.username?.[0] || "访" }}
                  </el-avatar>
                  <div class="comment-author-info">
                    <span class="comment-author-name">{{ comment?.author?.username || "访客" }}</span>
                    <span class="comment-time">{{ formatDateTime(comment?.createdAt) }}</span>
                  </div>
                </div>
                <el-button
                  v-if="canManageComment(comment)"
                  link
                  type="danger"
                  class="comment-delete-btn"
                  @click="deleteComment(comment.id)"
                >
                  删除
                </el-button>
              </div>
              <p class="comment-body">{{ comment?.content }}</p>
            </div>
          </li>
        </ul>

        <div v-if="commentTotal > commentPageSize" class="comment-pagination">
          <el-pagination
            background
            layout="prev, pager, next"
            :current-page="commentPage"
            :page-size="commentPageSize"
            :total="commentTotal"
            @current-change="handleCommentPageChange"
          />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { ElMessage, ElMessageBox } from "element-plus";
import { marked } from "marked";
import DOMPurify from "dompurify";
import { articleApi } from "@/services/api";
import { useAuthStore } from "@/stores/authStore";

const STATUS_TEXT = {
  PUBLISHED: "已发布",
  DRAFT: "草稿",
  PRIVATE: "仅自己可见",
  PENDING: "审核中",
};

const STATUS_EMOJI = {
  PUBLISHED: "🚀",
  DRAFT: "🛠️",
  PRIVATE: "🔒",
  PENDING: "⏳",
};

marked.setOptions({
  gfm: true,
  breaks: true,
});

const route = useRoute();
const router = useRouter();

const authStore = useAuthStore();
const { isAuthenticated, user } = storeToRefs(authStore);
const { openLogin } = authStore;

const loading = ref(true);
const error = ref("");
const article = ref(null);

const liked = ref(false);
const likeLoading = ref(false);

const comments = ref([]);
const commentTotal = ref(0);
const commentPage = ref(1);
const commentsLoading = ref(false);
const commentError = ref("");
const commentContent = ref("");
const submittingComment = ref(false);

const COMMENT_PAGE_SIZE = 6;
const commentPageSize = COMMENT_PAGE_SIZE;

const articleId = computed(() => route.params.id);

const renderedContent = computed(() => renderMarkdown(article.value?.content));
const wordCount = computed(() => computeWordCount(article.value?.content));
const wordCountDisplay = computed(() => formatNumber(wordCount.value));
const readingTimeLabel = computed(() => computeReadingTime(wordCount.value));
const tagCount = computed(() => article.value?.tags?.length ?? 0);
const tagCountDisplay = computed(() => formatNumber(tagCount.value));
const likeCount = computed(() => article.value?.likesCount ?? 0);
const likeCountDisplay = computed(() => formatNumber(likeCount.value));
const commentCount = computed(() => {
  const apiCount = article.value?.commentsCount ?? 0;
  return Math.max(apiCount, commentTotal.value);
});
const commentCountDisplay = computed(() => formatNumber(commentCount.value));
const statusText = computed(() => STATUS_TEXT[article.value?.status] ?? "未发布");
const statusEmoji = computed(() => STATUS_EMOJI[article.value?.status] ?? "📝");
const publishTimeFormatted = computed(() => formatDateTime(article.value?.publishTime));
const publishedRelative = computed(() => relativeTimeSince(article.value?.publishTime));
const likeButtonText = computed(() => {
  const labelCount = formatNumber(likeCount.value);
  return liked.value ? `已点赞 (${labelCount})` : `点赞 (${labelCount})`;
});
const canSubmitComment = computed(
  () => commentContent.value.trim().length > 0 && !submittingComment.value
);
const canEdit = computed(() => {
  const currentUserId = user.value?.id ?? user.value?.userId;
  const authorId = article.value?.author?.id;
  return !!currentUserId && !!authorId && currentUserId === authorId;
});

watch(isAuthenticated, (value) => {
  if (value && article.value) {
    fetchLikeStatus();
  } else {
    liked.value = false;
  }
});

watch(
  () => route.params.id,
  () => {
    resetViewState();
    fetchArticle();
    fetchComments(1);
  }
);

onMounted(() => {
  fetchArticle();
  fetchComments(1);
});

async function fetchArticle() {
  if (!articleId.value) {
    error.value = "未找到文章编号";
    loading.value = false;
    return;
  }

  loading.value = true;
  error.value = "";
  try {
    const response = await articleApi.getArticleById(articleId.value);
    const data = response?.data ?? null;
    if (!data) {
      error.value = "没有找到对应的文章";
      return;
    }
    article.value = {
      ...data,
      likesCount: data.likesCount ?? 0,
      commentsCount: data.commentsCount ?? 0,
    };
    if (isAuthenticated.value) {
      await fetchLikeStatus();
    }
  } catch (err) {
    console.error("Failed to load article:", err);
    error.value = err?.response?.data?.message ?? "加载文章失败，请稍后重试";
  } finally {
    loading.value = false;
  }
}

async function fetchLikeStatus() {
  if (!article.value) {
    return;
  }
  try {
    const response = await articleApi.getLikeStatus(article.value.id);
    liked.value = response?.data?.liked ?? false;
  } catch (err) {
    console.warn("Failed to fetch like status:", err);
  }
}

async function fetchComments(page = 1) {
  if (!articleId.value) {
    return;
  }

  commentsLoading.value = true;
  commentError.value = "";
  try {
    const response = await articleApi.getComments(
      articleId.value,
      page - 1,
      COMMENT_PAGE_SIZE
    );
    const data = response?.data ?? {};
    comments.value = data.content ?? [];
    commentTotal.value = data.totalElements ?? 0;
    commentPage.value = (data.number ?? page - 1) + 1;
    if (article.value) {
      article.value.commentsCount = commentTotal.value;
    }
  } catch (err) {
    console.error("Failed to load comments:", err);
    commentError.value = err?.response?.data?.message ?? "加载评论失败，请稍后重试";
  } finally {
    commentsLoading.value = false;
  }
}

async function toggleLike() {
  if (!article.value) {
    return;
  }
  if (!isAuthenticated.value) {
    openLogin();
    return;
  }
  if (likeLoading.value) {
    return;
  }

  likeLoading.value = true;
  try {
    if (liked.value) {
      await articleApi.unlikeArticle(article.value.id);
      liked.value = false;
      article.value.likesCount = Math.max(0, (article.value.likesCount ?? 1) - 1);
      ElMessage.success("已取消点赞");
    } else {
      await articleApi.likeArticle(article.value.id);
      liked.value = true;
      article.value.likesCount = (article.value.likesCount ?? 0) + 1;
      ElMessage.success("感谢点赞");
    }
  } catch (err) {
    console.error("Failed to toggle like:", err);
    ElMessage.error("操作失败，请稍后重试");
  } finally {
    likeLoading.value = false;
  }
}

async function submitComment() {
  if (!article.value) {
    return;
  }
  if (!isAuthenticated.value) {
    openLogin();
    return;
  }

  const content = commentContent.value.trim();
  if (!content) {
    ElMessage.warning("请先输入评论内容");
    return;
  }

  submittingComment.value = true;
  commentError.value = "";
  try {
    await articleApi.createComment(article.value.id, content);
    ElMessage.success("评论已发布");
    commentContent.value = "";
    await fetchComments(1);
    if (article.value) {
      article.value.commentsCount = commentTotal.value;
    }
  } catch (err) {
    console.error("Failed to submit comment:", err);
    commentError.value = err?.response?.data?.message ?? "发表评论失败，请稍后重试";
  } finally {
    submittingComment.value = false;
  }
}

async function deleteComment(commentId) {
  if (!article.value) {
    return;
  }
  try {
    await ElMessageBox.confirm("确定要删除这条评论吗？", "删除确认", {
      confirmButtonText: "删除",
      cancelButtonText: "取消",
      type: "warning",
    });
    await articleApi.deleteComment(article.value.id, commentId);
    ElMessage.success("评论已删除");
    const currentPage = commentPage.value;
    await fetchComments(currentPage);
    if (article.value) {
      article.value.commentsCount = commentTotal.value;
    }
  } catch (err) {
    if (err !== "cancel") {
      console.error("Failed to delete comment:", err);
      ElMessage.error("删除评论失败，请稍后重试");
    }
  }
}

function canManageComment(comment) {
  const currentUserId = user.value?.id ?? user.value?.userId;
  if (!currentUserId || !comment) {
    return false;
  }
  if (comment.userId === currentUserId) {
    return true;
  }
  return article.value?.author?.id === currentUserId;
}

function handleCommentPageChange(page) {
  fetchComments(page);
}

function handleEdit() {
  if (article.value) {
    router.push({ name: "edit-article", params: { id: article.value.id } });
  }
}

async function handleDelete() {
  if (!article.value) {
    return;
  }
  try {
    await ElMessageBox.confirm("确定要删除这篇文章吗？", "删除确认", {
      confirmButtonText: "删除",
      cancelButtonText: "取消",
      type: "warning",
    });
    await articleApi.deleteArticle(article.value.id);
    ElMessage.success("文章已删除");
    router.push("/my-blog");
  } catch (err) {
    if (err !== "cancel") {
      console.error("Failed to delete article:", err);
      ElMessage.error("删除失败，请稍后重试");
    }
  }
}

function handleBack() {
  router.back();
}

function resetViewState() {
  liked.value = false;
  comments.value = [];
  commentTotal.value = 0;
  commentPage.value = 1;
  commentError.value = "";
  commentContent.value = "";
}

function renderMarkdown(content) {
  if (!content) {
    return "<p>暂无内容</p>";
  }
  try {
    return DOMPurify.sanitize(marked.parse(content));
  } catch (err) {
    console.error("Failed to render markdown:", err);
    return "<p>内容渲染失败</p>";
  }
}

function computeWordCount(content) {
  if (!content) {
    return 0;
  }
  const plain = content
    .replace(/```[\s\S]*?```/g, " ")
    .replace(/`[^`]*`/g, " ")
    .replace(/!\[[^\]]*\]\([^\)]*\)/g, " ")
    .replace(/\[[^\]]*\]\([^\)]*\)/g, " ")
    .replace(/[#,>*_~`-]/g, " ")
    .replace(/\d+\./g, " ")
    .replace(/\r?\n+/g, " ")
    .trim();
  if (!plain) {
    return 0;
  }
  const asciiWords = plain.split(/\s+/).filter(Boolean);
  const chineseChars = plain.replace(/[a-zA-Z0-9\s]/g, "");
  return Math.max(asciiWords.length, chineseChars.length);
}

function computeReadingTime(words) {
  if (!words) {
    return "少于1分钟阅读";
  }
  const minutes = Math.max(1, Math.round(words / 350));
  return `${minutes} 分钟阅读`;
}

function formatNumber(value) {
  const numeric = Number(value ?? 0);
  return Number.isFinite(numeric) ? numeric.toLocaleString("zh-CN") : "0";
}

function formatDateTime(value) {
  if (!value) {
    return "时间未知";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "时间未知";
  }
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function relativeTimeSince(value) {
  if (!value) {
    return "";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "";
  }
  const diff = Date.now() - date.getTime();
  if (diff < 0) {
    return "即将发布";
  }
  const minute = 60 * 1000;
  const hour = 60 * minute;
  const day = 24 * hour;
  if (diff < minute) {
    return "刚刚发布";
  }
  if (diff < hour) {
    const minutes = Math.round(diff / minute);
    return `${minutes} 分钟前`;
  }
  if (diff < day) {
    const hours = Math.round(diff / hour);
    return `${hours} 小时前`;
  }
  const days = Math.round(diff / day);
  if (days < 30) {
    return `${days} 天前`;
  }
  const months = Math.round(days / 30);
  if (months < 12) {
    return `${months} 个月前`;
  }
  const years = Math.round(days / 365);
  return `${years} 年前`;
}
</script>

<style scoped>
.article-detail-view {
  position: relative;
  min-height: 100vh;
  padding: 3.5rem 1rem 4rem;
  background: linear-gradient(135deg, #f5f6ff 0%, #eef2ff 35%, #fdf2f8 100%);
}

.article-detail-view::before {
  content: "";
  position: absolute;
  inset: 0;
  background: radial-gradient(900px circle at 12% 10%, rgba(99, 102, 241, 0.14), transparent),
    radial-gradient(780px circle at 88% 8%, rgba(244, 114, 182, 0.12), transparent),
    linear-gradient(180deg, rgba(255, 255, 255, 0.5), transparent 65%);
  pointer-events: none;
}

.loading-container,
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  color: #1f2937;
  gap: 1rem;
  text-align: center;
}

.error-icon {
  font-size: 2.5rem;
  color: #ef4444;
}

.error-actions {
  display: flex;
  gap: 1rem;
}

.error-message {
  font-size: 1.1rem;
  margin: 0.5rem 0 1.5rem;
}

.article-detail-content {
  position: relative;
  z-index: 1;
  max-width: 1100px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 2.5rem;
}

.hero-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95), rgba(248, 250, 255, 0.9));
  border-radius: 2rem;
  padding: 3rem;
  border: 1px solid rgba(99, 102, 241, 0.18);
  box-shadow: 0 32px 80px -45px rgba(79, 70, 229, 0.55);
  backdrop-filter: blur(18px);
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.hero-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  font-size: 1.05rem;
}

.hero-back,
.hero-action-btn {
  border-radius: 999px;
  padding: 0.75rem 1.6rem;
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.65);
  border: 1px solid rgba(148, 163, 184, 0.25);
  color: #1f2937;
  transition: all 0.25s ease;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.85);
  cursor: pointer;
}

.hero-back:hover,
.hero-action-btn:hover {
  border-color: rgba(99, 102, 241, 0.35);
  color: #312e81;
  transform: translateY(-1px);
  box-shadow: 0 12px 28px -18px rgba(99, 102, 241, 0.55);
}

.hero-action-btn.danger {
  color: #b91c1c;
  border-color: rgba(248, 113, 113, 0.4);
}

.hero-action-btn.danger:hover {
  color: #991b1b;
  border-color: rgba(248, 113, 113, 0.55);
  box-shadow: 0 12px 28px -18px rgba(248, 113, 113, 0.55);
}

.hero-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.hero-body {
  display: flex;
  align-items: center;
  gap: 1.75rem;
  flex-wrap: wrap;
}

.hero-avatar {
  box-shadow: 0 20px 40px -25px rgba(99, 102, 241, 0.45);
}

.hero-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  min-width: 260px;
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.35rem 0.9rem;
  border-radius: 999px;
  font-weight: 600;
  color: #4338ca;
  background: rgba(99, 102, 241, 0.12);
  width: fit-content;
}

.hero-title {
  margin: 0;
  font-size: 2.6rem;
  font-weight: 800;
  color: #1e1b4b;
  line-height: 1.25;
  letter-spacing: -0.02em;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  align-items: center;
  color: #475569;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.95rem;
  font-weight: 500;
}

.meta-icon {
  font-size: 1rem;
}

.meta-separator {
  color: rgba(71, 85, 105, 0.45);
  font-size: 0.9rem;
}

.hero-summary {
  margin: 0;
  padding: 1.4rem 1.6rem;
  border-radius: 1.2rem;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1), rgba(244, 114, 182, 0.1));
  color: #374151;
  line-height: 1.7;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

:deep(.hero-tag.el-tag) {
  border-radius: 999px;
  padding: 0.45rem 1rem;
  font-weight: 600;
  border: none;
  box-shadow: 0 12px 24px -18px rgba(79, 70, 229, 0.6);
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 1.25rem;
}

.stat-card {
  padding: 1.4rem;
  border-radius: 1.2rem;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(148, 163, 184, 0.22);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.6);
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.stat-card.highlight {
  background: linear-gradient(135deg, rgba(233, 213, 255, 0.85), rgba(191, 219, 254, 0.85));
  border: 1px solid rgba(147, 197, 253, 0.6);
  align-items: flex-start;
}

.stat-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #6366f1;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.stat-value {
  font-size: 1.6rem;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.01em;
}

.stat-hint {
  font-size: 0.85rem;
  color: #64748b;
}

.hero-like {
  border: none;
  border-radius: 999px;
  padding: 0.75rem 1.5rem;
  background: rgba(255, 255, 255, 0.85);
  color: #dc2626;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  cursor: pointer;
  transition: all 0.24s ease;
}

.hero-like:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 14px 28px -20px rgba(220, 38, 38, 0.65);
}

.hero-like.liked {
  background: #dc2626;
  color: #fff;
  box-shadow: 0 16px 32px -20px rgba(220, 38, 38, 0.6);
}

.hero-like:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.article-shell {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.article-cover {
  border-radius: 2rem;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.25);
  box-shadow: 0 26px 60px -40px rgba(15, 23, 42, 0.4);
}

.cover-image {
  width: 100%;
  max-height: 520px;
  object-fit: cover;
  transition: transform 0.4s ease;
  cursor: pointer;
}

.cover-image:hover {
  transform: scale(1.02);
}

.article-body {
  padding: 2.75rem;
  border-radius: 2rem;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(148, 163, 184, 0.22);
  box-shadow: 0 32px 80px -45px rgba(15, 23, 42, 0.45);
}

.article-content {
  font-size: 1.1rem;
  line-height: 1.8;
  color: #1f2937;
}

.article-content :deep(h1),
.article-content :deep(h2),
.article-content :deep(h3),
.article-content :deep(h4),
.article-content :deep(h5),
.article-content :deep(h6) {
  font-weight: 700;
  margin-top: 2rem;
  margin-bottom: 1rem;
  color: #0f172a;
}

.article-content :deep(h1) {
  font-size: 2.1rem;
  border-bottom: 3px solid rgba(99, 102, 241, 0.35);
  padding-bottom: 0.6rem;
}

.article-content :deep(h2) {
  font-size: 1.8rem;
  border-bottom: 2px solid rgba(148, 163, 184, 0.35);
  padding-bottom: 0.45rem;
}

.article-content :deep(h3) {
  font-size: 1.5rem;
}

.article-content :deep(h4) {
  font-size: 1.25rem;
}

.article-content :deep(p) {
  margin-bottom: 1.25rem;
}

.article-content :deep(a) {
  color: #6366f1;
  text-decoration: none;
  border-bottom: 1px solid rgba(99, 102, 241, 0.5);
  transition: all 0.25s ease;
}

.article-content :deep(a:hover) {
  color: #7c3aed;
  border-bottom-color: rgba(124, 58, 237, 0.6);
}

.article-content :deep(code) {
  background: rgba(241, 245, 249, 0.9);
  padding: 0.25rem 0.45rem;
  border-radius: 0.5rem;
  font-family: "Fira Code", "JetBrains Mono", monospace;
  font-size: 0.95em;
  color: #db2777;
}

.article-content :deep(pre) {
  background: #1f2937;
  color: #f8fafc;
  padding: 1.6rem;
  border-radius: 1.1rem;
  overflow-x: auto;
  margin: 1.75rem 0;
  line-height: 1.6;
}

.article-content :deep(blockquote) {
  border-left: 4px solid rgba(99, 102, 241, 0.35);
  padding: 1rem 1.5rem;
  border-radius: 0.9rem;
  background: rgba(248, 250, 255, 0.9);
  color: #475569;
  font-style: italic;
  margin: 1.75rem 0;
}

.article-content :deep(ul),
.article-content :deep(ol) {
  margin: 1.5rem 0;
  padding-left: 2.1rem;
}

.article-content :deep(li) {
  margin-bottom: 0.85rem;
}

.article-content :deep(img) {
  max-width: 100%;
  border-radius: 1rem;
  margin: 1.5rem 0;
  box-shadow: 0 18px 40px -32px rgba(15, 23, 42, 0.5);
}

.article-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1.75rem 0;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 1rem;
  overflow: hidden;
  box-shadow: 0 18px 38px -32px rgba(15, 23, 42, 0.35);
}

.article-content :deep(th),
.article-content :deep(td) {
  padding: 1rem 1.25rem;
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
}

.article-content :deep(th) {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.9), rgba(124, 58, 237, 0.9));
  color: #f8fafc;
  font-weight: 600;
}

.article-content :deep(tr:hover) {
  background: rgba(248, 250, 255, 0.85);
}

.article-content :deep(hr) {
  border: none;
  border-top: 2px solid rgba(226, 232, 240, 0.8);
  margin: 2rem 0;
}

.comment-panel {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(250, 245, 255, 0.96));
  border-radius: 2rem;
  padding: 2.5rem;
  border: 1px solid rgba(209, 213, 219, 0.45);
  box-shadow: 0 28px 70px -45px rgba(124, 58, 237, 0.35);
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.section-title {
  margin: 0;
  font-size: 1.6rem;
  font-weight: 700;
  color: #1e1b4b;
}

.comment-count-badge {
  padding: 0.45rem 1rem;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.12);
  color: #4338ca;
  font-weight: 600;
}

.comment-error-text {
  margin: 0;
  padding: 0.95rem 1.2rem;
  border-radius: 1rem;
  background: rgba(254, 226, 226, 0.9);
  color: #b91c1c;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.comment-form-card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 1.5rem;
  padding: 1.2rem 1.4rem;
  border: 1px solid rgba(148, 163, 184, 0.25);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.6);
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

:deep(.comment-form-card .el-textarea__inner) {
  border-radius: 1.2rem;
  border: 1px solid rgba(148, 163, 184, 0.32);
  background: rgba(248, 250, 255, 0.85);
  padding: 1rem 1.2rem;
  font-size: 1rem;
  transition: box-shadow 0.2s ease;
}

:deep(.comment-form-card .el-textarea__inner:focus) {
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.18);
  border-color: transparent;
}

.comment-form-actions {
  display: flex;
  justify-content: flex-end;
}

.comment-submit-btn {
  border-radius: 999px;
  padding: 0.65rem 1.6rem;
  font-weight: 600;
}

.comment-login-hint {
  border: 1px dashed rgba(148, 163, 184, 0.45);
  border-radius: 1.5rem;
  padding: 1.4rem;
  text-align: center;
  color: #64748b;
}

.comment-login-hint p {
  margin-bottom: 0.75rem;
}

.comments-loading {
  display: inline-flex;
  align-items: center;
  gap: 0.65rem;
  color: #64748b;
}

.loading-spinner {
  font-size: 1.1rem;
}

.comment-empty {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 1.5rem;
  padding: 2rem 0;
}

.comment-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.comment-item {
  margin: 0;
}

.comment-card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 1.5rem;
  padding: 1.4rem 1.6rem;
  border: 1px solid rgba(209, 213, 219, 0.4);
  box-shadow: 0 16px 40px -30px rgba(30, 64, 175, 0.35);
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.comment-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.comment-author {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.comment-avatar {
  box-shadow: 0 12px 24px -16px rgba(79, 70, 229, 0.45);
}

.comment-author-info {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.comment-author-name {
  font-weight: 600;
  color: #1f2937;
}

.comment-time {
  font-size: 0.85rem;
  color: #94a3b8;
}

.comment-delete-btn {
  font-weight: 600;
}

.comment-body {
  margin: 0;
  color: #475569;
  line-height: 1.7;
  white-space: pre-wrap;
}

.comment-pagination {
  align-self: flex-end;
}

.comment-pagination :deep(.el-pagination.is-background .el-pager li.is-active) {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  box-shadow: 0 12px 24px -20px rgba(99, 102, 241, 0.6);
}

@media (max-width: 1024px) {
  .hero-card {
    padding: 2.4rem;
  }

  .article-body {
    padding: 2.25rem;
  }
}

@media (max-width: 768px) {
  .article-detail-view {
    padding: 2.5rem 0.75rem 3rem;
  }

  .hero-top {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-stats {
    grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  }

  .hero-title {
    font-size: 2.1rem;
  }

  .article-body {
    padding: 1.8rem;
  }

  .comment-panel {
    padding: 1.8rem;
  }
}

@media (max-width: 560px) {
  .hero-body {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-stats {
    grid-template-columns: 1fr;
  }

  .hero-title {
    font-size: 1.85rem;
  }

  .hero-action-btn,
  .hero-back {
    width: 100%;
    justify-content: center;
  }

  .comment-top {
    flex-direction: column;
    align-items: flex-start;
  }

  .comment-pagination {
    align-self: stretch;
  }
}
</style><template>
  <div class="article-detail-view">
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading" :size="40">
        <i class="fas fa-spinner fa-spin"></i>
      </el-icon>
      <p>加载中...</p>
    </div>

    <style scoped>
    .article-detail-view {
      position: relative;
      min-height: 100vh;
      padding: 3.5rem 1rem 4rem;
      background: linear-gradient(135deg, #f5f6ff 0%, #eef2ff 35%, #fdf2f8 100%);
    }

    .article-detail-view::before {
      content: "";
      position: absolute;
      inset: 0;
      background: radial-gradient(900px circle at 12% 10%, rgba(99, 102, 241, 0.14), transparent),
        radial-gradient(780px circle at 88% 8%, rgba(244, 114, 182, 0.12), transparent),
        linear-gradient(180deg, rgba(255, 255, 255, 0.5), transparent 65%);
      pointer-events: none;
    }

    .loading-container,
    .error-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 60vh;
      color: #1f2937;
      gap: 1rem;
    }

    .error-message {
      font-size: 1.1rem;
      margin: 1rem 0;
    }

    .article-detail-content {
      position: relative;
      z-index: 1;
      max-width: 1100px;
      margin: 0 auto;
      display: flex;
      flex-direction: column;
      gap: 2.5rem;
    }

    .hero-card {
      background: linear-gradient(135deg, rgba(255, 255, 255, 0.95), rgba(248, 250, 255, 0.9));
      border-radius: 2rem;
      padding: 3rem;
      border: 1px solid rgba(99, 102, 241, 0.18);
      box-shadow: 0 32px 80px -45px rgba(79, 70, 229, 0.55);
      backdrop-filter: blur(18px);
      display: flex;
      flex-direction: column;
      gap: 2rem;
    }

    .hero-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 1rem;
    }

    .btn-icon {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      line-height: 1;
      font-size: 1.05rem;
    }

    .hero-back,
    .hero-action-btn {
      border-radius: 999px;
      padding: 0.75rem 1.6rem;
      display: inline-flex;
      align-items: center;
      gap: 0.45rem;
      font-weight: 600;
      background: rgba(255, 255, 255, 0.65);
      border: 1px solid rgba(148, 163, 184, 0.25);
      color: #1f2937;
      transition: all 0.25s ease;
      box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.85);
    }

    .hero-back:hover,
    .hero-action-btn:hover {
      border-color: rgba(99, 102, 241, 0.35);
      color: #312e81;
      transform: translateY(-1px);
      box-shadow: 0 12px 28px -18px rgba(99, 102, 241, 0.55);
    }

    .hero-action-btn.danger {
      color: #b91c1c;
      border-color: rgba(248, 113, 113, 0.4);
    }

    .hero-action-btn.danger:hover {
      color: #991b1b;
      border-color: rgba(248, 113, 113, 0.55);
      box-shadow: 0 12px 28px -18px rgba(248, 113, 113, 0.55);
    }

    .hero-actions {
      display: flex;
      gap: 0.75rem;
      flex-wrap: wrap;
    }

    .hero-body {
      display: flex;
      align-items: center;
      gap: 1.75rem;
      flex-wrap: wrap;
    }

    .hero-avatar {
      box-shadow: 0 20px 40px -25px rgba(99, 102, 241, 0.45);
    }

    .hero-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 1rem;
      min-width: 260px;
    }

    .hero-kicker {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      padding: 0.35rem 0.9rem;
      border-radius: 999px;
      font-weight: 600;
      color: #4338ca;
      background: rgba(99, 102, 241, 0.12);
      width: fit-content;
    }

    .hero-title {
      margin: 0;
      font-size: 2.6rem;
      font-weight: 800;
      color: #1e1b4b;
      line-height: 1.25;
      letter-spacing: -0.02em;
    }

    .hero-meta {
      display: flex;
      flex-wrap: wrap;
      gap: 0.65rem;
      align-items: center;
      color: #475569;
    }

    .meta-item {
      display: inline-flex;
      align-items: center;
      gap: 0.35rem;
      font-size: 0.95rem;
      font-weight: 500;
    }

    .meta-icon {
      font-size: 1rem;
    }

    .meta-separator {
      color: rgba(71, 85, 105, 0.45);
      font-size: 0.9rem;
    }

    .status-pill {
      padding: 0.3rem 0.85rem;
      border-radius: 999px;
      font-weight: 600;
      background: rgba(99, 102, 241, 0.16);
      color: #3730a3;
    }

    .status-published {
      background: rgba(34, 197, 94, 0.18);
      color: #166534;
    }

    .status-draft {
      background: rgba(251, 191, 36, 0.2);
      color: #92400e;
    }

    .status-private {
      background: rgba(248, 113, 113, 0.2);
      color: #b91c1c;
    }

    .status-pending {
      background: rgba(20, 184, 166, 0.18);
      color: #0f766e;
    }

    .hero-summary {
      margin: 0;
      padding: 1.4rem 1.6rem;
      border-radius: 1.2rem;
      background: linear-gradient(135deg, rgba(59, 130, 246, 0.1), rgba(244, 114, 182, 0.1));
      color: #374151;
      line-height: 1.7;
    }

    .hero-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 0.65rem;
    }

    :deep(.hero-tag.el-tag) {
      border-radius: 999px;
      padding: 0.45rem 1rem;
      font-weight: 600;
      border: none;
      box-shadow: 0 12px 24px -18px rgba(79, 70, 229, 0.6);
    }

    .hero-stats {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 1.25rem;
    }

    .stat-card {
      padding: 1.4rem;
      border-radius: 1.2rem;
      background: rgba(255, 255, 255, 0.8);
      border: 1px solid rgba(148, 163, 184, 0.22);
      box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.6);
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }

    .stat-card.highlight {
      background: linear-gradient(135deg, rgba(233, 213, 255, 0.85), rgba(191, 219, 254, 0.85));
      border: 1px solid rgba(147, 197, 253, 0.6);
      align-items: flex-start;
    }

    .stat-label {
      font-size: 0.85rem;
      font-weight: 600;
      color: #6366f1;
      text-transform: uppercase;
      letter-spacing: 0.06em;
    }

    .stat-value {
      font-size: 1.6rem;
      font-weight: 700;
      color: #0f172a;
      letter-spacing: -0.01em;
    }

    .stat-hint {
      font-size: 0.85rem;
      color: #64748b;
    }

    .hero-like {
      border: none;
      border-radius: 999px;
      padding: 0.75rem 1.5rem;
      background: rgba(255, 255, 255, 0.85);
      color: #dc2626;
      font-weight: 700;
      display: inline-flex;
      align-items: center;
      gap: 0.45rem;
      cursor: pointer;
      transition: all 0.24s ease;
    }

    .hero-like:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 14px 28px -20px rgba(220, 38, 38, 0.65);
    }

    .hero-like.liked {
      background: #dc2626;
      color: #fff;
      box-shadow: 0 16px 32px -20px rgba(220, 38, 38, 0.6);
    }

    .hero-like:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .article-shell {
      display: flex;
      flex-direction: column;
      gap: 2rem;
    }

    .article-cover {
      border-radius: 2rem;
      overflow: hidden;
      border: 1px solid rgba(148, 163, 184, 0.25);
      box-shadow: 0 26px 60px -40px rgba(15, 23, 42, 0.4);
    }

    .cover-image {
      width: 100%;
      max-height: 520px;
      object-fit: cover;
      transition: transform 0.4s ease;
      cursor: pointer;
    }

    .cover-image:hover {
      transform: scale(1.02);
    }

    .article-body {
      padding: 2.75rem;
      border-radius: 2rem;
      background: rgba(255, 255, 255, 0.96);
      border: 1px solid rgba(148, 163, 184, 0.22);
      box-shadow: 0 32px 80px -45px rgba(15, 23, 42, 0.45);
    }

    .article-content {
      font-size: 1.1rem;
      line-height: 1.8;
      color: #1f2937;
    }

    .article-content :deep(h1),
    .article-content :deep(h2),
    .article-content :deep(h3),
    .article-content :deep(h4),
    .article-content :deep(h5),
    .article-content :deep(h6) {
      font-weight: 700;
      margin-top: 2rem;
      margin-bottom: 1rem;
      color: #0f172a;
    }

    .article-content :deep(h1) {
      font-size: 2.1rem;
      border-bottom: 3px solid rgba(99, 102, 241, 0.35);
      padding-bottom: 0.6rem;
    }

    .article-content :deep(h2) {
      font-size: 1.8rem;
      border-bottom: 2px solid rgba(148, 163, 184, 0.35);
      padding-bottom: 0.45rem;
    }

    .article-content :deep(h3) {
      font-size: 1.5rem;
    }

    .article-content :deep(h4) {
      font-size: 1.25rem;
    }

    .article-content :deep(p) {
      margin-bottom: 1.25rem;
    }

    .article-content :deep(a) {
      color: #6366f1;
      text-decoration: none;
      border-bottom: 1px solid rgba(99, 102, 241, 0.5);
      transition: all 0.25s ease;
    }

    .article-content :deep(a:hover) {
      color: #7c3aed;
      border-bottom-color: rgba(124, 58, 237, 0.6);
    }

    .article-content :deep(code) {
      background: rgba(241, 245, 249, 0.9);
      padding: 0.25rem 0.45rem;
      border-radius: 0.5rem;
      font-family: "Fira Code", "JetBrains Mono", monospace;
      font-size: 0.95em;
      color: #db2777;
    }

    .article-content :deep(pre) {
      background: #1f2937;
      color: #f8fafc;
      padding: 1.6rem;
      border-radius: 1.1rem;
      overflow-x: auto;
      margin: 1.75rem 0;
      line-height: 1.6;
    }

    .article-content :deep(pre code) {
      background: transparent;
      color: inherit;
      padding: 0;
    }

    .article-content :deep(blockquote) {
      border-left: 4px solid rgba(99, 102, 241, 0.35);
      padding: 1rem 1.5rem;
      border-radius: 0.9rem;
      background: rgba(248, 250, 255, 0.9);
      color: #475569;
      font-style: italic;
      margin: 1.75rem 0;
    }

    .article-content :deep(ul),
    .article-content :deep(ol) {
      margin: 1.5rem 0;
      padding-left: 2.1rem;
    }

    .article-content :deep(li) {
      margin-bottom: 0.85rem;
    }

    .article-content :deep(img) {
      max-width: 100%;
      border-radius: 1rem;
      margin: 1.5rem 0;
      box-shadow: 0 18px 40px -32px rgba(15, 23, 42, 0.5);
    }

    .article-content :deep(table) {
      width: 100%;
      border-collapse: collapse;
      margin: 1.75rem 0;
      background: rgba(255, 255, 255, 0.95);
      border-radius: 1rem;
      overflow: hidden;
      box-shadow: 0 18px 38px -32px rgba(15, 23, 42, 0.35);
    }

    .article-content :deep(th),
    .article-content :deep(td) {
      padding: 1rem 1.25rem;
      border-bottom: 1px solid rgba(226, 232, 240, 0.8);
    }

    .article-content :deep(th) {
      background: linear-gradient(135deg, rgba(99, 102, 241, 0.9), rgba(124, 58, 237, 0.9));
      color: #f8fafc;
      font-weight: 600;
    }

    .article-content :deep(tr:hover) {
      background: rgba(248, 250, 255, 0.85);
    }

    .article-content :deep(hr) {
      border: none;
      border-top: 2px solid rgba(226, 232, 240, 0.8);
      margin: 2rem 0;
    }

    .comment-panel {
      background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(250, 245, 255, 0.96));
      border-radius: 2rem;
      padding: 2.5rem;
      border: 1px solid rgba(209, 213, 219, 0.45);
      box-shadow: 0 28px 70px -45px rgba(124, 58, 237, 0.35);
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
    }

    .comment-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 1rem;
    }

    .section-title {
      margin: 0;
      font-size: 1.6rem;
      font-weight: 700;
      color: #1e1b4b;
    }

    .comment-count-badge {
      padding: 0.45rem 1rem;
      border-radius: 999px;
      background: rgba(99, 102, 241, 0.12);
      color: #4338ca;
      font-weight: 600;
    }

    .comment-error-text {
      margin: 0;
      padding: 0.95rem 1.2rem;
      border-radius: 1rem;
      background: rgba(254, 226, 226, 0.9);
      color: #b91c1c;
    }

    .comment-form-card {
      background: rgba(255, 255, 255, 0.9);
      border-radius: 1.5rem;
      padding: 1.2rem 1.4rem;
      border: 1px solid rgba(148, 163, 184, 0.25);
      box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.6);
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    :deep(.comment-form-card .el-textarea__inner) {
      border-radius: 1.2rem;
      border: 1px solid rgba(148, 163, 184, 0.32);
      background: rgba(248, 250, 255, 0.85);
      padding: 1rem 1.2rem;
      font-size: 1rem;
      transition: box-shadow 0.2s ease;
    }

    :deep(.comment-form-card .el-textarea__inner:focus) {
      box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.18);
      border-color: transparent;
    }

    .comment-form-actions {
      display: flex;
      justify-content: flex-end;
    }

    .comment-submit-btn {
      border-radius: 999px;
      padding: 0.65rem 1.6rem;
      font-weight: 600;
    }

    .comment-login-hint {
      border: 1px dashed rgba(148, 163, 184, 0.45);
      border-radius: 1.5rem;
      padding: 1.4rem;
      text-align: center;
      color: #64748b;
    }

    .comment-login-hint p {
      margin-bottom: 0.75rem;
    }

    .comments-loading {
      display: inline-flex;
      align-items: center;
      gap: 0.65rem;
      color: #64748b;
    }

    .loading-spinner {
      font-size: 1.1rem;
    }

    .comment-empty {
      background: rgba(255, 255, 255, 0.9);
      border-radius: 1.5rem;
      padding: 2rem 0;
    }

    .comment-list {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
    }

    .comment-item {
      margin: 0;
    }

    .comment-card {
      background: rgba(255, 255, 255, 0.9);
      border-radius: 1.5rem;
      padding: 1.4rem 1.6rem;
      border: 1px solid rgba(209, 213, 219, 0.4);
      box-shadow: 0 16px 40px -30px rgba(30, 64, 175, 0.35);
      display: flex;
      flex-direction: column;
      gap: 0.85rem;
    }

    .comment-top {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 1rem;
    }

    .comment-author {
      display: flex;
      align-items: center;
      gap: 0.85rem;
    }

    .comment-avatar {
      box-shadow: 0 12px 24px -16px rgba(79, 70, 229, 0.45);
    }

    .comment-author-info {
      display: flex;
      flex-direction: column;
      gap: 0.2rem;
    }

    .comment-author-name {
      font-weight: 600;
      color: #1f2937;
    }

    .comment-time {
      font-size: 0.85rem;
      color: #94a3b8;
    }

    .comment-delete-btn {
      color: #b91c1c;
      font-weight: 600;
    }

    .comment-body {
      margin: 0;
      color: #475569;
      line-height: 1.7;
      white-space: pre-wrap;
    }

    .comment-pagination {
      align-self: flex-end;
    }

    .comment-pagination :deep(.el-pagination.is-background .el-pager li.is-active) {
      background: linear-gradient(135deg, #6366f1, #8b5cf6);
      box-shadow: 0 12px 24px -20px rgba(99, 102, 241, 0.6);
    }

    @media (max-width: 1024px) {
      .hero-card {
        padding: 2.4rem;
      }

      .article-body {
        padding: 2.25rem;
      }
    }

    @media (max-width: 768px) {
      .article-detail-view {
        padding: 2.5rem 0.75rem 3rem;
      }

      .hero-top {
        flex-direction: column;
        align-items: flex-start;
      }

      .hero-stats {
        grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
      }

      .hero-title {
        font-size: 2.1rem;
      }

      .article-body {
        padding: 1.8rem;
      }

      .comment-panel {
        padding: 1.8rem;
      }
    }

    @media (max-width: 560px) {
      .hero-body {
        flex-direction: column;
        align-items: flex-start;
      }

      .hero-stats {
        grid-template-columns: 1fr;
      }

      .hero-title {
        font-size: 1.85rem;
      }

      .hero-action-btn,
      .hero-back {
        width: 100%;
        justify-content: center;
      }

      .comment-top {
        flex-direction: column;
        align-items: flex-start;
      }

      .comment-pagination {
        align-self: stretch;
      }
    }
    </style>
    await ElMessageBox.confirm("确定要删除这篇文章吗？", "确认删除", {
      confirmButtonText: "删除",
      cancelButtonText: "取消",
      type: "warning",
    });

    await articleApi.deleteArticle(article.value.id);
    ElMessage.success("文章已删除");
    router.push("/my-blog");
  } catch (err) {
    if (err !== "cancel") {
      console.error("Delete failed:", err);
      ElMessage.error("删除失败，请稍后重试");
    }
  }
};

watch(isAuthenticated, (value) => {
  if (value) {
    fetchLikeStatus();
  } else {
    liked.value = false;
  }
});

watch(
  () => route.params.id,
  () => {
    liked.value = false;
    fetchArticle();
    fetchComments(1);
    if (isAuthenticated.value) {
      fetchLikeStatus();
    }
  }
);

onMounted(() => {
  fetchArticle();
  fetchComments(1);
  if (isAuthenticated.value) {
    fetchLikeStatus();
  }
});
</script>

<style scoped>
.article-detail-view {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 2rem 1rem;
}

.loading-container,
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  color: white;
  gap: 1rem;
}

.error-message {
  font-size: 1.1rem;
  margin: 1rem 0;
}

.article-detail-content {
  max-width: 900px;
  margin: 0 auto;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.back-btn {
  background: rgba(255, 255, 255, 0.9);
  border: none;
  font-weight: 600;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background: white;
  transform: translateX(-4px);
}

.header-actions {
  display: flex;
  gap: 0.5rem;
}

.edit-btn,
.delete-btn {
  font-weight: 600;
}

.article-container {
  background: white;
  border-radius: 16px;
  padding: 3rem;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
  animation: fadeInUp 0.6s ease;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.article-header {
  margin-bottom: 2rem;
  padding-bottom: 2rem;
  border-bottom: 2px solid #f0f0f0;
}

.article-title {
  font-size: 2.5rem;
  font-weight: 800;
  color: #1a1a2e;
  margin-bottom: 1.5rem;
  line-height: 1.3;
}

.article-meta {
  margin-bottom: 1.5rem;
}

.author-section {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.author-details {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.author-name {
  font-size: 1.1rem;
  font-weight: 600;
  color: #333;
}

.meta-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  color: #666;
}

.publish-time {
  display: flex;
  align-items: center;
  gap: 0.3rem;
}

.meta-divider {
  color: #ccc;
}

.status-badge {
  padding: 0.3rem 0.8rem;
  border-radius: 12px;
  font-size: 0.85rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
}

.status-published {
  background: #d4edda;
  color: #155724;
}

.status-draft {
  background: #fff3cd;
  color: #856404;
}

.status-private {
  background: #f8d7da;
  color: #721c24;
}

.status-pending {
  background: #d1ecf1;
  color: #0c5460;
}

.article-summary {
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
  border-left: 4px solid #667eea;
  padding: 1.5rem;
  border-radius: 8px;
  font-size: 1.1rem;
  line-height: 1.6;
  color: #555;
  font-style: italic;
  margin-bottom: 1.5rem;
}

.article-summary i {
  color: #667eea;
  margin-right: 0.5rem;
  opacity: 0.6;
}

.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.8rem;
}

.tag-item {
  font-size: 0.95rem;
  padding: 0.5rem 1rem;
  font-weight: 600;
  color: #fff;
}

:deep(.tag-item .el-tag__content) {
  color: #fff;
}

.tag-item i {
  color: #fff;
}

.article-engagement {
  margin-top: 1.5rem;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 1.5rem;
}

.like-button {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: 999px;
  border: 1px solid #f87171;
  background: #fff7f7;
  color: #dc2626;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.like-button:hover:not(:disabled) {
  background: #fecaca;
}

.like-button.liked {
  background: #dc2626;
  color: white;
  border-color: #dc2626;
}

.like-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.engagement-stats {
  display: inline-flex;
  gap: 1rem;
  color: #6b7280;
  font-size: 0.95rem;
}

.engagement-stats .stat {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.engagement-stats .stat:first-child i {
  color: #f97316;
}

.engagement-stats .stat:last-child i {
  color: #3b82f6;
}

.comment-section {
  margin-top: 2.5rem;
  background: white;
  border-radius: 16px;
  padding: 2rem;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.08);
}

.section-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 1.5rem;
}

.comment-error-text {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #b91c1c;
  background: #fee2e2;
  border-radius: 8px;
  padding: 0.75rem 1rem;
  margin-bottom: 1rem;
}

.comment-form {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}

.comment-form-actions {
  display: flex;
  justify-content: flex-end;
}

.comment-login-hint {
  border: 1px dashed #d1d5db;
  border-radius: 12px;
  padding: 1.25rem;
  text-align: center;
  color: #6b7280;
  margin-bottom: 1.5rem;
}

.comment-login-hint p {
  margin-bottom: 0.75rem;
}

.comments-loading {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  color: #6b7280;
  padding: 1rem 0;
}

.comment-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.comment-item {
  border-bottom: 1px solid #e5e7eb;
  padding-bottom: 1.5rem;
}

.comment-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.75rem;
}

.comment-author {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.comment-author-info {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.comment-author-name {
  font-weight: 600;
  color: #374151;
}

.comment-time {
  font-size: 0.85rem;
  color: #9ca3af;
}

.comment-body {
  margin: 0;
  color: #4b5563;
  line-height: 1.6;
  white-space: pre-wrap;
}

.comment-pagination {
  margin-top: 1.5rem;
  display: flex;
  justify-content: flex-end;
}

.article-cover {
  margin: 2rem 0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.cover-image {
  width: 100%;
  max-height: 500px;
  object-fit: cover;
  transition: transform 0.3s ease;
  cursor: pointer;
}

.cover-image:hover {
  transform: scale(1.02);
}

.article-content {
  font-size: 1.1rem;
  line-height: 1.8;
  color: #333;
  margin-top: 2rem;
}

/* Markdown content styling */
.article-content :deep(h1),
.article-content :deep(h2),
.article-content :deep(h3),
.article-content :deep(h4),
.article-content :deep(h5),
.article-content :deep(h6) {
  font-weight: 700;
  margin-top: 2rem;
  margin-bottom: 1rem;
  color: #1a1a2e;
}

.article-content :deep(h1) {
  font-size: 2rem;
  border-bottom: 3px solid #667eea;
  padding-bottom: 0.5rem;
}

.article-content :deep(h2) {
  font-size: 1.75rem;
  border-bottom: 2px solid #ddd;
  padding-bottom: 0.4rem;
}

.article-content :deep(h3) {
  font-size: 1.5rem;
}

.article-content :deep(h4) {
  font-size: 1.25rem;
}

.article-content :deep(p) {
  margin-bottom: 1.2rem;
}

.article-content :deep(a) {
  color: #667eea;
  text-decoration: none;
  border-bottom: 1px solid #667eea;
  transition: all 0.3s ease;
}

.article-content :deep(a:hover) {
  color: #764ba2;
  border-bottom-color: #764ba2;
}

.article-content :deep(code) {
  background: #f5f5f5;
  padding: 0.2rem 0.4rem;
  border-radius: 4px;
  font-family: "Courier New", Courier, monospace;
  font-size: 0.95em;
  color: #e83e8c;
}

.article-content :deep(pre) {
  background: #2d2d2d;
  color: #f8f8f2;
  padding: 1.5rem;
  border-radius: 8px;
  overflow-x: auto;
  margin: 1.5rem 0;
  line-height: 1.5;
}

.article-content :deep(pre code) {
  background: transparent;
  color: inherit;
  padding: 0;
}

.article-content :deep(blockquote) {
  border-left: 4px solid #667eea;
  padding-left: 1.5rem;
  margin: 1.5rem 0;
  color: #666;
  font-style: italic;
  background: #f9f9f9;
  padding: 1rem 1.5rem;
  border-radius: 4px;
}

.article-content :deep(ul),
.article-content :deep(ol) {
  margin: 1.5rem 0;
  padding-left: 2rem;
}

.article-content :deep(li) {
  margin-bottom: 0.8rem;
}

.article-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  margin: 1.5rem 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.article-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1.5rem 0;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border-radius: 8px;
  overflow: hidden;
}

.article-content :deep(th),
.article-content :deep(td) {
  padding: 1rem;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.article-content :deep(th) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-weight: 600;
}

.article-content :deep(tr:hover) {
  background: #f9f9f9;
}

.article-content :deep(hr) {
  border: none;
  border-top: 2px solid #eee;
  margin: 2rem 0;
}

@media (max-width: 768px) {
  .article-detail-view {
    padding: 1rem 0.5rem;
  }

  .article-container {
    padding: 1.5rem;
    border-radius: 12px;
  }

  .article-title {
    font-size: 1.8rem;
  }

  .detail-header {
    flex-direction: column;
    gap: 1rem;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
  }

  .edit-btn,
  .delete-btn {
    flex: 1;
  }

  .article-content {
    font-size: 1rem;
  }

  .article-engagement {
    flex-direction: column;
    align-items: stretch;
    gap: 1rem;
  }

  .like-button {
    justify-content: center;
  }

  .engagement-stats {
    justify-content: space-between;
  }

  .comment-section {
    padding: 1.25rem;
  }

  .comment-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.75rem;
  }

  .comment-pagination {
    justify-content: center;
  }
}
</style>
