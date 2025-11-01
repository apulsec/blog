<template>
  <nav class="global-nav" :class="{ 'with-shadow': shadow }">
    <div class="nav-inner">
      <router-link to="/" class="brand-link">
        <i class="fas fa-feather"></i>
        <span class="brand-text">博客平台</span>
      </router-link>
      <div class="nav-links">
        <router-link to="/" class="nav-link" active-class="active">
          <i class="fas fa-home"></i>
          <span>博客首页</span>
        </router-link>
        <router-link to="/my-blog" class="nav-link" active-class="active">
          <el-badge
            :value="notifications.unreadCount"
            :hidden="!auth.isAuthenticated || notifications.unreadCount === 0"
            :max="99"
            class="nav-badge"
            type="danger"
          >
            <span class="nav-link-inner">
              <i class="fas fa-user"></i>
              <span>个人主页</span>
            </span>
          </el-badge>
        </router-link>
        <router-link to="/create" class="nav-link" active-class="active">
          <i class="fas fa-edit"></i>
          <span>写文章</span>
        </router-link>
      </div>
      <slot name="extra"></slot>
    </div>
  </nav>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useAuthStore } from "@/stores/authStore";
import { useNotificationsStore } from "@/stores/notificationsStore";

const props = defineProps({
  shadow: { type: Boolean, default: true },
});

const auth = useAuthStore();
const notifications = useNotificationsStore();
const shadow = ref(props.shadow);

onMounted(() => {
  if (auth.isAuthenticated && notifications.items.length === 0) {
    notifications.fetchNotifications().catch(() => {});
  }
});
</script>

<style scoped>
.global-nav {
  position: sticky;
  top: 0;
  z-index: 1000;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid rgba(229, 231, 235, 0.8);
  padding: 0.75rem 1.5rem;
}

.global-nav.with-shadow {
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.08);
}

.nav-inner {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  min-height: 48px;
}

.brand-link {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  color: #1f2937;
  text-decoration: none;
  font-weight: 600;
  font-size: 1.1rem;
}

.brand-link i {
  color: #2563eb;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.nav-link {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.45rem 0.85rem;
  border-radius: 999px;
  color: #4b5563;
  text-decoration: none;
  transition: all 0.2s ease;
  font-weight: 500;
}

.nav-link:hover,
.nav-link.active {
  color: #1d4ed8;
  background: rgba(37, 99, 235, 0.1);
}

.nav-badge :deep(.el-badge__content.is-fixed) {
  transform: translate(60%, -40%);
}

.nav-link-inner {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

@media (max-width: 768px) {
  .global-nav {
    padding: 0.75rem 1rem;
  }

  .nav-inner {
    flex-wrap: wrap;
    justify-content: center;
    gap: 0.75rem;
  }

  .brand-link {
    order: 1;
  }

  .nav-links {
    order: 2;
    width: 100%;
    justify-content: center;
    gap: 0.5rem;
  }

  .nav-link {
    flex: 1 1 30%;
    justify-content: center;
  }
}
</style>
