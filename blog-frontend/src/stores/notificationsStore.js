import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { notificationsApi } from "@/services/notifications";

export const useNotificationsStore = defineStore("notifications", () => {
  const items = ref([]);
  const loading = ref(false);
  const error = ref("");
  const lastFetched = ref(null);
  let pollingTimer = null;

  const unreadCount = computed(() =>
    items.value.reduce((count, item) => (item.read ? count : count + 1), 0)
  );

  function setItems(data) {
    if (!Array.isArray(data)) {
      items.value = [];
      return;
    }
    items.value = data
      .slice()
      .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  }

  async function fetchNotifications({ limit = 20, unreadOnly = false } = {}) {
    const token = localStorage.getItem("access_token");
    if (!token) {
      clear();
      return;
    }

    loading.value = true;
    error.value = "";
    try {
      const response = await notificationsApi.getNotifications({
        limit,
        unreadOnly,
      });
      const payload = Array.isArray(response.data)
        ? response.data
        : response.data
        ? [response.data]
        : [];
      setItems(payload);
      lastFetched.value = new Date();
    } catch (err) {
      error.value = err?.response?.data?.message || "加载通知失败，请稍后再试";
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function markAsRead(notificationId) {
    if (!notificationId) return;
    try {
      await notificationsApi.markAsRead(notificationId);
      items.value = items.value.map((item) =>
        item.id === notificationId ? { ...item, read: true } : item
      );
    } catch (err) {
      error.value =
        err?.response?.data?.message || "标记通知为已读失败，请稍后重试";
      throw err;
    }
  }

  async function markAllAsRead() {
    try {
      await notificationsApi.markAllAsRead();
      items.value = items.value.map((item) => ({ ...item, read: true }));
    } catch (err) {
      error.value =
        err?.response?.data?.message || "全部标记为已读失败，请稍后重试";
      throw err;
    }
  }

  function startPolling(interval = 30000) {
    stopPolling();
    pollingTimer = setInterval(() => {
      fetchNotifications().catch(() => {
        /* errors handled in store */
      });
    }, interval);
  }

  function stopPolling() {
    if (pollingTimer) {
      clearInterval(pollingTimer);
      pollingTimer = null;
    }
  }

  function clear() {
    stopPolling();
    items.value = [];
    error.value = "";
    lastFetched.value = null;
  }

  return {
    items,
    loading,
    error,
    unreadCount,
    lastFetched,
    fetchNotifications,
    markAsRead,
    markAllAsRead,
    startPolling,
    stopPolling,
    clear,
  };
});
