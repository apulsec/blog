<template>
  <div class="article-card" @click="handleCardClick">
    <div class="card-header">
      <div class="article-main">
        <h4 class="article-title">{{ article.title }}</h4>
        <p class="article-summary">{{ article.summary }}</p>
        <div class="article-meta">
          <div class="author-info">
            <el-avatar
              shape="circle"
              :src="article.author?.avatarUrl"
              :size="32"
            >
              <i class="fas fa-user"></i>
            </el-avatar>
            <span class="author-name">{{
              article.author?.username || "未知作者"
            }}</span>
          </div>
          <span class="meta-divider">•</span>
          <time class="publish-time">{{ formattedPublishTime }}</time>
          <span class="meta-divider">•</span>
          <span class="status-badge" :class="statusClass">
            <i :class="statusIcon"></i> {{ statusText }}
          </span>
        </div>
      </div>
      <div v-if="showActions" class="article-actions">
        <button class="action-btn" title="编辑" @click.stop="handleEdit">
          <i class="fas fa-edit"></i>
        </button>
        <button
          class="action-btn action-delete"
          title="删除"
          @click.stop="handleDelete"
        >
          <i class="fas fa-trash"></i>
        </button>
      </div>
    </div>

    <div v-if="article.coverImageUrl" class="article-cover">
      <el-image :src="article.coverImageUrl" fit="cover" class="cover-image" />
    </div>

    <div v-if="article.tags && article.tags.length > 0" class="article-tags">
      <el-tag
        v-for="tag in article.tags"
        :key="tag.id"
        :color="tag.color"
        effect="light"
        round
        class="tag-item"
      >
        <i class="fas fa-tag"></i> {{ tag.name }}
      </el-tag>
    </div>

    <div class="article-stats">
      <span class="stat-item" title="点赞数">
        <i class="fas fa-heart"></i>
        {{ likesCount }}
      </span>
      <span class="stat-item" title="评论数">
        <i class="fas fa-comment"></i>
        {{ commentsCount }}
      </span>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits, computed } from "vue";
import { useRouter } from "vue-router";
import { ElMessageBox, ElMessage } from "element-plus";
import { articleApi } from "@/services/api";

const props = defineProps({
  article: {
    type: Object,
    required: true,
  },
  showActions: {
    type: Boolean,
    default: true,
  },
});

const emit = defineEmits(["deleted"]);
const router = useRouter();

/**
 * Formats the publish time for display.
 * Converts ISO date string to locale date format.
 */
const formattedPublishTime = computed(() => {
  if (!props.article.publishTime) return "";
  const date = new Date(props.article.publishTime);
  return date.toLocaleDateString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
});

/**
 * Computes status text based on article status
 */
const statusText = computed(() => {
  const statusMap = {
    PUBLISHED: "已发布",
    DRAFT: "草稿",
    PRIVATE: "私密",
    PENDING: "待审核",
  };
  return statusMap[props.article.status] || "未知";
});

/**
 * Computes status CSS class
 */
const statusClass = computed(() => {
  const classMap = {
    PUBLISHED: "status-published",
    DRAFT: "status-draft",
    PRIVATE: "status-private",
    PENDING: "status-pending",
  };
  return classMap[props.article.status] || "";
});

/**
 * Computes status icon
 */
const statusIcon = computed(() => {
  const iconMap = {
    PUBLISHED: "fas fa-check-circle",
    DRAFT: "fas fa-file-alt",
    PRIVATE: "fas fa-lock",
    PENDING: "fas fa-clock",
  };
  return iconMap[props.article.status] || "fas fa-question-circle";
});

const likesCount = computed(() => props.article.likesCount ?? 0);
const commentsCount = computed(() => props.article.commentsCount ?? 0);

/**
 * Handle card click to view article detail
 */
const handleCardClick = () => {
  router.push(`/article/${props.article.id}`);
};

/**
 * Navigate to edit page
 */
const handleEdit = () => {
  router.push(`/edit/${props.article.id}`);
};

/**
 * Handle article deletion with confirmation
 */
const handleDelete = async () => {
  try {
    await ElMessageBox.confirm(
      "确定要删除这篇文章吗？此操作无法撤销。",
      "确认删除",
      {
        confirmButtonText: "删除",
        cancelButtonText: "取消",
        type: "warning",
        confirmButtonClass: "el-button--danger",
      }
    );

    // User confirmed, proceed with deletion
    await articleApi.deleteArticle(props.article.id);

    ElMessage.success("文章删除成功！");

    // Emit event to parent component to refresh the list
    emit("deleted", props.article.id);
  } catch (error) {
    // User cancelled or deletion failed
    if (error !== "cancel") {
      console.error("Failed to delete article:", error);
      ElMessage.error("删除文章失败，请稍后重试");
    }
  }
};
</script>

<style scoped>
.article-card {
  background: white;
  border-radius: 1rem;
  padding: 1.5rem;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
  transition: all 0.2s ease-in-out;
  border: 1px solid rgba(255, 255, 255, 0.5);
  cursor: pointer;
}

.article-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1);
  border-color: rgba(102, 126, 234, 0.3);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.article-main {
  flex: 1;
}

.article-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 0.75rem 0;
  cursor: pointer;
  transition: color 0.2s;
}

.article-title:hover {
  color: #3b82f6;
}

.article-summary {
  color: #6b7280;
  margin: 0 0 1rem 0;
  line-height: 1.6;
  font-size: 0.938rem;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: #9ca3af;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.author-name {
  color: #6b7280;
  font-weight: 500;
}

.meta-divider {
  color: #d1d5db;
}

.publish-time {
  color: #9ca3af;
}

.status-badge {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-weight: 500;
}

.status-published {
  color: #10b981;
}

.status-draft {
  color: #f59e0b;
}

.status-private {
  color: #6b7280;
}

.status-pending {
  color: #3b82f6;
}

.article-actions {
  display: flex;
  gap: 0.5rem;
  flex-shrink: 0;
}

.action-btn {
  padding: 0.5rem;
  border: none;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
  border-radius: 0.5rem;
  transition: all 0.2s;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-btn:hover {
  background: #f3f4f6;
  color: #3b82f6;
}

.action-btn.action-delete:hover {
  color: #ef4444;
}

.action-btn.action-save:hover {
  color: #10b981;
}

.article-cover {
  margin: 1rem 0;
  border-radius: 0.75rem;
  overflow: hidden;
}

.cover-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 1rem;
}

.tag-item {
  border-radius: 9999px;
  padding: 0.375rem 0.75rem;
  font-size: 0.813rem;
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  border: none;
  color: white;
  font-weight: 500;
}

.article-stats {
  display: flex;
  gap: 1rem;
  margin-top: 1rem;
  font-size: 0.875rem;
  color: #6b7280;
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.stat-item i {
  color: #f59e0b;
}

.stat-item:nth-child(2) i {
  color: #3b82f6;
}

@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
  }

  .article-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .article-cover {
    margin: 0.75rem 0;
  }

  .cover-image {
    height: 150px;
  }
}
</style>
