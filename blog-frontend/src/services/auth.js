import axios from "axios";

// 用户服务客户端 - 用于注册和用户信息管理
const userServiceClient = axios.create({
  baseURL: "/api/users",
});

// 认证服务客户端 - 用于登录、登出、token管理
const authServiceClient = axios.create({
  baseURL: "/api/auth",
  headers: { "Content-Type": "application/json" },
});

// Add token to auth service requests
authServiceClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("access_token");
  if (token) {
    config.headers = config.headers || {};
    config.headers["Authorization"] = `Bearer ${token}`;
  }
  return config;
});

// Add token to user service requests (for protected endpoints)
userServiceClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("access_token");
  if (token) {
    config.headers = config.headers || {};
    config.headers["Authorization"] = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截器 - 自动刷新token
authServiceClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // 如果是401错误且未重试过,尝试刷新token
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = localStorage.getItem("refresh_token");
        if (refreshToken) {
          const response = await authServiceClient.post("/refresh", {
            refreshToken,
          });
          const { accessToken, refreshToken: newRefreshToken } = response.data;

          localStorage.setItem("access_token", accessToken);
          localStorage.setItem("refresh_token", newRefreshToken);

          // 重试原请求
          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          return authServiceClient(originalRequest);
        }
      } catch (refreshError) {
        // 刷新失败,清除所有token
        localStorage.removeItem("access_token");
        localStorage.removeItem("refresh_token");
        localStorage.removeItem("auth_user");
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export const authApi = {
  // 登录 - 调用认证服务
  // Login with username and password
  login(credentials) {
    // credentials: { username, password }
    return authServiceClient.post("/login", credentials);
  },

  // 注册 - 调用用户服务
  // Register a new user
  register(payload) {
    // payload: { username, password, email? }
    return userServiceClient.post("/register", payload);
  },

  // 获取用户信息 - 调用用户服务
  // Get current user info by token
  getUserById(userId) {
    return userServiceClient.get(`/${userId}`);
  },

  // 登出 - 调用认证服务
  logout() {
    return authServiceClient.post("/logout");
  },

  // 刷新token - 调用认证服务
  refreshToken(refreshToken) {
    return authServiceClient.post("/refresh", { refreshToken });
  },

  // 上传头像 - 调用用户服务
  uploadAvatar(formData) {
    return userServiceClient.post("/me/avatar", formData);
  },

  // 验证token - 调用认证服务
  validateToken(token) {
    return authServiceClient.get("/validate", {
      params: { token },
    });
  },
};
