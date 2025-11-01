<template>
  <section class="notifications-card">
    <header
      class="notifications-header"
      :class="{ 'is-collapsible': collapsible }"
      @click="handleToggle"
    >
      <div class="notifications-title">
        <slot name="icon">
          <i class="fas fa-bell"></i>
        </slot>
        <span>{{ title }}</span>
        <span v-if="unreadCount > 0" class="notifications-counter">
          {{ unreadCount > 99 ? "99+" : unreadCount }}
        </span>
      </div>
      <div class="notifications-header-actions" @click.stop>
        <el-button text type="primary" size="small" @click="emitRefresh">
          刷新
        </el-button>
        <el-button
          text
          type="success"
          size="small"
          :disabled="unreadCount === 0"
          @click="emitMarkAll"
        >
          全部已读
        </el-button>
        <i
          v-if="collapsible"
          class="toggle-icon fas"
          :class="isOpen ? 'fa-chevron-up' : 'fa-chevron-down'"
        ></i>
      </div>
    </header>

    <transition name="notifications-toggle">
      <div v-show="isOpen" class="notifications-body">
        <el-skeleton :rows="3" animated v-if="loading" />
        <el-alert
          v-else-if="error"
          :title="error"
          type="error"
          show-icon
          class="notifications-error"
        />
        <el-empty
          v-else-if="items.length === 0"
          :description="emptyDescription"
          class="notifications-empty"
        />
        <ul v-else class="notifications-list">
          <li
            v-for="item in items"
            :key="item.id"
            :class="['notification-item', { unread: !item.read }]"
          >
            <div class="notification-main">
              <div class="notification-type">
                <i :class="resolveIcon(item.type)"></i>
                <span>{{ resolveTypeLabel(item.type) }}</span>
              </div>
              <div class="notification-content">
                <slot name="content" :item="item">
                  {{ item.content }}
                </slot>
              </div>
              <div class="notification-meta">
                <span v-if="item.actorUsername" class="notification-actor">
                  来自 {{ item.actorUsername }}
                </span>
                <span class="notification-time">
                  {{ resolveDateTime(item.createdAt) }}
                </span>
              </div>
            </div>
            <div class="notification-actions">
              <slot name="actions" :item="item" :mark-read="emitMarkSingle">
                <el-button
                  v-if="!item.read"
                  text
                  type="primary"
                  size="small"
                  @click="emitMarkSingle(item)"
                >
                  标记已读
                </el-button>
              </slot>
            </div>
          </li>
        </ul>
      </div>
    </transition>
  </section>
</template>

<script setup>
import { computed, ref, watch } from "vue";

const props = defineProps({
  title: { type: String, default: "通知" },
  unreadCount: { type: Number, default: 0 },
  items: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  error: { type: String, default: "" },
  emptyDescription: { type: String, default: "暂无通知" },
  collapsible: { type: Boolean, default: true },
  defaultOpen: { type: Boolean, default: true },
  open: { type: Boolean, default: undefined },
});

const emit = defineEmits(["refresh", "mark-all", "mark-read", "update:open"]);

const internalOpen = ref(props.collapsible ? props.defaultOpen : true);
const isControlled = computed(() => props.open !== undefined);

const isOpen = computed({
  get: () =>
    props.collapsible
      ? isControlled.value
        ? props.open
        : internalOpen.value
      : true,
  set: (value) => {
    if (!props.collapsible) return;
    if (!isControlled.value) {
      internalOpen.value = value;
    }
    emit("update:open", value);
  },
});

watch(
  () => props.open,
  (value) => {
    if (isControlled.value && value !== undefined) {
      internalOpen.value = value;
    }
  }
);

const handleToggle = () => {
  if (!props.collapsible) {
    return;
  }
  isOpen.value = !isOpen.value;
};

const emitRefresh = () => {
  emit("refresh");
};

const emitMarkAll = () => {
  emit("mark-all");
};

const emitMarkSingle = (item) => {
  if (!item?.id) return;
  emit("mark-read", item);
};

const resolveIcon = (type) => {
  switch (type) {
    case "ARTICLE_COMMENT":
      return "fas fa-comment-dots";
    case "ARTICLE_LIKE":
      return "fas fa-heart";
    default:
      return "fas fa-bell";
  }
};

const resolveTypeLabel = (type) => {
  switch (type) {
    case "ARTICLE_COMMENT":
      return "评论提醒";
    case "ARTICLE_LIKE":
      return "点赞提醒";
    default:
      return "系统通知";
  }
};

const resolveDateTime = (value) => {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
};
</script>

<style scoped>
.notifications-card {
  background: white;
  border-radius: 1rem;
  padding: 1.25rem 1.5rem;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.notifications-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  cursor: default;
}

.notifications-header.is-collapsible {
  cursor: pointer;
}

.notifications-header.is-collapsible .notifications-header-actions {
  cursor: default;
}

.notifications-title {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: #1f2937;
}

.notifications-counter {
  background: #ef4444;
  color: white;
  border-radius: 999px;
  padding: 0 0.5rem;
  font-size: 0.75rem;
  line-height: 1.5;
  font-weight: 600;
}

.notifications-header-actions {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.toggle-icon {
  color: #6b7280;
  transition: transform 0.2s ease;
}

.notifications-body {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.notifications-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.notification-item {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem;
  border-radius: 0.75rem;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
}

.notification-item.unread {
  border-color: #3b82f6;
  background: #eff6ff;
}

.notification-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.notification-type {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: #1f2937;
}

.notification-content {
  color: #374151;
}

.notification-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  font-size: 0.8125rem;
  color: #6b7280;
}

.notification-actor {
  font-weight: 500;
}

.notification-time {
  color: #9ca3af;
}

.notification-actions {
  display: flex;
  align-items: flex-start;
}

.notifications-error {
  margin: 0;
}

.notifications-empty {
  padding: 1rem 0;
}

.notifications-toggle-enter-active,
.notifications-toggle-leave-active {
  transition: all 0.2s ease;
}

.notifications-toggle-enter-from,
.notifications-toggle-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

@media (max-width: 768px) {
  .notifications-card {
    padding: 1rem;
  }

  .notifications-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .notifications-header-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .notification-item {
    flex-direction: column;
  }

  .notification-actions {
    justify-content: flex-end;
  }
}
</style>
