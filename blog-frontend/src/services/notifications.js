import axios from "axios";

const notificationClient = axios.create({
  baseURL: "/api/notifications",
  headers: { "Content-Type": "application/json" },
});

notificationClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("access_token");
  if (token) {
    config.headers = config.headers || {};
    config.headers["Authorization"] = `Bearer ${token}`;
  }
  return config;
});

export const notificationsApi = {
  getNotifications({ limit = 20, unreadOnly = false } = {}) {
    const params = { limit };
    if (unreadOnly) {
      params.unreadOnly = true;
    }
    return notificationClient.get("/me", { params });
  },

  markAsRead(notificationId) {
    return notificationClient.post(`/${notificationId}/read`);
  },

  markAllAsRead() {
    return notificationClient.post("/read-all");
  },
};
