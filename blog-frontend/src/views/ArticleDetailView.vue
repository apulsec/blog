<template>
  <div class="article-detail-view">
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading" :size="40">
        <i class="fas fa-spinner fa-spin"></i>
      </el-icon>
      <p>加载中...</p>
    </div>

    <div v-else-if="error" class="error-container">
      <el-icon :size="60" color="#f56c6c">
        <i class="fas fa-exclamation-circle"></i>
      </el-icon>
      <p class="error-message">{{ error }}</p>
      <el-button type="primary" @click="router.back()">返回</el-button>
    </div>

    <div v-else-if="article" class="article-detail-content">
      <!-- Header Navigation -->
      <div class="detail-header">
        <el-button @click="router.back()" class="back-btn">
          <i class="fas fa-arrow-left"></i> 返回
        </el-button>
        <div class="header-actions">
          <el-button
            v-if="canEdit"
            type="primary"
            @click="handleEdit"
            class="edit-btn"
          >
            <i class="fas fa-edit"></i> 编辑
          </el-button>
          <el-button
            v-if="canEdit"
            type="danger"
            @click="handleDelete"
            class="delete-btn"
          >
            <i class="fas fa-trash"></i> 删除
          </el-button>
        </div>
      </div>

      <!-- Article Header -->
      <article class="article-container">
        <header class="article-header">
          <h1 class="article-title">{{ article.title }}</h1>

          <div class="article-meta">
            <div class="author-section">
              <el-avatar
                shape="circle"
                :src="article.author?.avatarUrl"
                :size="48"
              >
                <i class="fas fa-user"></i>
              </el-avatar>
              <div class="author-details">
                <span class="author-name">{{
                  article.author?.username || "未知作者"
                }}</span>
                <div class="meta-info">
                  <time class="publish-time">
                    <i class="fas fa-clock"></i>
                    {{ formattedPublishTime }}
                  </time>
                  <span class="meta-divider">•</span>
                  <span class="status-badge" :class="statusClass">
                    <i :class="statusIcon"></i> {{ statusText }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div v-if="article.summary" class="article-summary">
            <i class="fas fa-quote-left"></i>
            {{ article.summary }}
          </div>

          <div
            v-if="article.tags && article.tags.length > 0"
            class="article-tags"
          >
            <el-tag
              v-for="tag in article.tags"
              :key="tag.id"
              :color="tag.color"
              effect="light"
              round
              size="large"
              class="tag-item"
            >
              <i class="fas fa-tag"></i> {{ tag.name }}
            </el-tag>
          </div>

          <div class="article-engagement">
            <button
              class="like-button"
              :class="{ liked }"
              :disabled="likeLoading"
              @click="handleToggleLike"
            >
              <i :class="likeIcon"></i>
              <span>{{ liked ? "已点赞" : "点赞" }}</span>
            </button>

            <div class="engagement-stats">
              <span class="stat" title="点赞数">
                <i class="fas fa-heart"></i>
                {{ likesCount }}
              </span>
              <span class="stat" title="评论数">
                <i class="fas fa-comment"></i>
                {{ commentsCount }}
              </span>
            </div>
          </div>
        </header>

        <!-- Cover Image -->
        <div v-if="article.coverImageUrl" class="article-cover">
          <el-image
            :src="article.coverImageUrl"
            fit="cover"
            class="cover-image"
            :preview-src-list="[article.coverImageUrl]"
          />
        </div>

        <!-- Article Content -->
        <div class="article-content" v-html="renderedContent"></div>
      </article>

      <section class="comment-section">
        <h2 class="section-title">评论</h2>

        <p v-if="commentsError" class="comment-error-text">
          <i class="fas fa-exclamation-triangle"></i>
          {{ commentsError }}
        </p>

        <div v-if="isAuthenticated" class="comment-form">
          <el-input
            v-model="newComment"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="写下你的评论..."
          />
          <div class="comment-form-actions">
            <el-button
              type="primary"
              :loading="commentSubmitting"
              @click="submitComment"
            >
              发送评论
            </el-button>
          </div>
        </div>
        <div v-else class="comment-login-hint">
          <p>登录后即可参与评论。</p>
          <el-button type="primary" @click="promptLogin">立即登录</el-button>
        </div>

        <div v-if="commentsLoading" class="comments-loading">
          <el-icon class="is-loading" :size="24">
            <i class="fas fa-spinner fa-spin"></i>
          </el-icon>
          <span>加载评论中...</span>
        </div>

        <el-empty
          v-else-if="!comments.length"
          description="暂无评论，抢沙发吧！"
        />

        <ul v-else class="comment-list">
          <li
            v-for="comment in comments"
            :key="comment.id"
            class="comment-item"
          >
            <div class="comment-header">
              <div class="comment-author">
                <el-avatar
                  shape="circle"
                  :size="32"
                  :src="comment.author?.avatarUrl"
                >
                  <i class="fas fa-user"></i>
                </el-avatar>
                <div class="comment-author-info">
                  <span class="comment-author-name">
                    {{ comment.author?.username || "用户 #" + comment.userId }}
                  </span>
                  <time class="comment-time">
                    {{ formatCommentDate(comment.createdAt) }}
                  </time>
                </div>
              </div>
              <el-button
                v-if="comment.userId === currentUserId"
                type="danger"
                link
                :loading="isDeletingComment(comment.id)"
                @click="handleDeleteComment(comment.id)"
              >
                删除
              </el-button>
            </div>
            <p class="comment-body">{{ comment.content }}</p>
          </li>
        </ul>

        <el-pagination
          v-if="commentTotal > commentPageSize"
          class="comment-pagination"
          layout="prev, pager, next"
          :background="true"
          :page-size="commentPageSize"
          :current-page="commentPage"
          :total="commentTotal"
          @current-change="handleCommentPageChange"
        />
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { articleApi } from "@/services/api";
import { useAuthStore } from "@/stores/authStore";
import { marked } from "marked";
import DOMPurify from "dompurify";
import { storeToRefs } from "pinia";

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const { isAuthenticated, user } = storeToRefs(auth);

const article = ref(null);
const loading = ref(true);
const error = ref("");
const liked = ref(false);
const likeLoading = ref(false);
const comments = ref([]);
const commentsLoading = ref(false);
const commentsError = ref("");
const commentPage = ref(1);
const commentPageSize = ref(10);
const commentTotal = ref(0);
const newComment = ref("");
const commentSubmitting = ref(false);
const deletingCommentIds = ref([]);

// Configure marked options
marked.setOptions({
  breaks: true,
  gfm: true,
  headerIds: true,
  mangle: false,
});

/**
 * Fetch article details
 */
const fetchArticle = async () => {
  loading.value = true;
  error.value = "";

  try {
    const articleId = route.params.id;
    const response = await articleApi.getArticleById(articleId);
    article.value = response.data;
    article.value.likesCount = article.value.likesCount ?? 0;
    article.value.commentsCount = article.value.commentsCount ?? 0;
  } catch (err) {
    console.error("Failed to fetch article:", err);
    error.value =
      err?.response?.data?.message ||
      err?.message ||
      "获取文章失败，请稍后重试";
  } finally {
    loading.value = false;
  }
};

const fetchLikeStatus = async () => {
  if (!isAuthenticated.value) {
    liked.value = false;
    return;
  }

  try {
    const response = await articleApi.getLikeStatus(route.params.id);
    liked.value = !!response.data?.liked;
  } catch (err) {
    console.error("Failed to fetch like status:", err);
  }
};

const handleToggleLike = async () => {
  if (!article.value) return;

  if (!isAuthenticated.value) {
    ElMessage.warning("请先登录后再点赞");
    promptLogin();
    return;
  }

  if (likeLoading.value) return;

  likeLoading.value = true;
  try {
    if (liked.value) {
      await articleApi.unlikeArticle(article.value.id);
      liked.value = false;
      article.value.likesCount = Math.max(
        (article.value.likesCount ?? 1) - 1,
        0
      );
    } else {
      await articleApi.likeArticle(article.value.id);
      liked.value = true;
      article.value.likesCount = (article.value.likesCount ?? 0) + 1;
    }
  } catch (err) {
    console.error("Failed to toggle like:", err);
    ElMessage.error("操作失败，请稍后重试");
  } finally {
    likeLoading.value = false;
  }
};

const fetchComments = async (page = 1) => {
  commentsLoading.value = true;
  commentsError.value = "";

  try {
    const response = await articleApi.getComments(
      route.params.id,
      page - 1,
      commentPageSize.value
    );
    const data = response.data || {};
    comments.value = data.content ?? [];
    commentTotal.value =
      typeof data.totalElements === "number"
        ? data.totalElements
        : comments.value.length;
    commentPageSize.value = data.size ?? commentPageSize.value;
    commentPage.value = (data.number ?? page - 1) + 1;
  } catch (err) {
    console.error("Failed to load comments:", err);
    commentsError.value =
      err?.response?.data?.message || "加载评论失败，请稍后重试";
  } finally {
    commentsLoading.value = false;
  }
};

const handleCommentPageChange = (page) => {
  fetchComments(page);
};

const submitComment = async () => {
  if (!isAuthenticated.value) {
    ElMessage.warning("登录后才能发表评论");
    promptLogin();
    return;
  }

  if (!article.value) return;

  const content = newComment.value.trim();
  if (!content) {
    ElMessage.warning("评论内容不能为空");
    return;
  }

  if (commentSubmitting.value) return;

  commentSubmitting.value = true;
  try {
    await articleApi.createComment(article.value.id, content);
    ElMessage.success("评论发布成功");
    newComment.value = "";
    article.value.commentsCount = (article.value.commentsCount ?? 0) + 1;
    await fetchComments(1);
  } catch (err) {
    console.error("Failed to create comment:", err);
    const message = err?.response?.data?.message || "发表评论失败，请稍后重试";
    ElMessage.error(message);
  } finally {
    commentSubmitting.value = false;
  }
};

const setDeletingFlag = (commentId, active) => {
  const list = deletingCommentIds.value;
  const index = list.indexOf(commentId);
  if (active) {
    if (index === -1) list.push(commentId);
  } else if (index > -1) {
    list.splice(index, 1);
  }
};

const handleDeleteComment = async (commentId) => {
  if (!isAuthenticated.value) {
    ElMessage.warning("登录后才能删除评论");
    promptLogin();
    return;
  }

  if (!article.value) return;

  try {
    await ElMessageBox.confirm("确定要删除这条评论吗？", "删除评论", {
      confirmButtonText: "删除",
      cancelButtonText: "取消",
      type: "warning",
    });

    setDeletingFlag(commentId, true);
    await articleApi.deleteComment(article.value.id, commentId);
    ElMessage.success("评论已删除");
    article.value.commentsCount = Math.max(
      (article.value.commentsCount ?? 1) - 1,
      0
    );
    await fetchComments(commentPage.value);
  } catch (err) {
    if (err !== "cancel") {
      console.error("Failed to delete comment:", err);
      const message =
        err?.response?.data?.message || "删除评论失败，请稍后重试";
      ElMessage.error(message);
    }
  } finally {
    setDeletingFlag(commentId, false);
  }
};

const formatCommentDate = (value) => {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
};

const promptLogin = () => {
  auth.openLogin();
};

const isDeletingComment = (commentId) =>
  deletingCommentIds.value.includes(commentId);

/**
 * Render markdown content to HTML
 */
const renderedContent = computed(() => {
  if (!article.value?.content) return "<p>暂无内容</p>";

  // Convert markdown to HTML
  const rawHtml = marked(article.value.content);

  // Sanitize HTML to prevent XSS attacks
  return DOMPurify.sanitize(rawHtml);
});

/**
 * Format publish time
 */
const formattedPublishTime = computed(() => {
  if (!article.value?.publishTime) return "";
  const date = new Date(article.value.publishTime);
  return date.toLocaleString("zh-CN", {
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
});

/**
 * Status display text
 */
const statusText = computed(() => {
  const statusMap = {
    PUBLISHED: "已发布",
    DRAFT: "草稿",
    PRIVATE: "私密",
    PENDING: "待审核",
  };
  return statusMap[article.value?.status] || "未知";
});

/**
 * Status CSS class
 */
const statusClass = computed(() => {
  const classMap = {
    PUBLISHED: "status-published",
    DRAFT: "status-draft",
    PRIVATE: "status-private",
    PENDING: "status-pending",
  };
  return classMap[article.value?.status] || "";
});

/**
 * Status icon
 */
const statusIcon = computed(() => {
  const iconMap = {
    PUBLISHED: "fas fa-check-circle",
    DRAFT: "fas fa-file-alt",
    PRIVATE: "fas fa-lock",
    PENDING: "fas fa-clock",
  };
  return iconMap[article.value?.status] || "fas fa-question-circle";
});

const likesCount = computed(() => article.value?.likesCount ?? 0);
const commentsCount = computed(() => article.value?.commentsCount ?? 0);
const likeIcon = computed(() =>
  liked.value ? "fas fa-heart" : "far fa-heart"
);
const currentUserId = computed(() => user.value?.id ?? null);

/**
 * Check if current user can edit this article
 */
const canEdit = computed(() => {
  return isAuthenticated.value && article.value?.author?.id === user.value?.id;
});

/**
 * Handle edit action
 */
const handleEdit = () => {
  router.push(`/edit/${article.value.id}`);
};

/**
 * Handle delete action
 */
const handleDelete = async () => {
  try {
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
