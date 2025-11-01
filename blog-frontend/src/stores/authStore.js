import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { authApi } from "@/services/auth";
import axios from "axios";

export const useAuthStore = defineStore("auth", () => {
  // 使用新的token命名: accessToken 和 refreshToken
  const accessToken = ref(localStorage.getItem("access_token") || "");
  const refreshToken = ref(localStorage.getItem("refresh_token") || "");
  const user = ref(null);
  const loading = ref(false);
  const error = ref("");
  const showLoginModal = ref(false);

  // Initialize user from storage
  try {
    const u = localStorage.getItem("auth_user");
    if (u) user.value = JSON.parse(u);
  } catch (e) {
    // ignore
  }

  if (accessToken.value) {
    axios.defaults.headers.common[
      "Authorization"
    ] = `Bearer ${accessToken.value}`;
  }

  const isAuthenticated = computed(() => !!accessToken.value && !!user.value);

  function openLogin() {
    showLoginModal.value = true;
  }

  function closeLogin() {
    showLoginModal.value = false;
    error.value = "";
  }

  async function login({ username, password }) {
    loading.value = true;
    error.value = "";
    try {
      const resp = await authApi.login({ username, password });
      // 新的后端返回格式: { accessToken, refreshToken }
      const { accessToken: at, refreshToken: rt } = resp.data;

      accessToken.value = at;
      refreshToken.value = rt;

      localStorage.setItem("access_token", at);
      localStorage.setItem("refresh_token", rt);

      // Set default header for all axios
      axios.defaults.headers.common["Authorization"] = `Bearer ${at}`;

      // 从JWT中解析用户信息 (简单实现,生产环境建议从后端获取完整用户信息)
      try {
        const payload = JSON.parse(atob(at.split(".")[1]));
        user.value = {
          username: payload.sub,
          id: payload.userId, // 使用 id 字段，与前端其他地方保持一致
          userId: payload.userId, // 保留 userId 以兼容
        };
        localStorage.setItem("auth_user", JSON.stringify(user.value));
      } catch (e) {
        console.error("Failed to parse JWT:", e);
      }

      try {
        await fetchMe();
      } catch (e) {
        console.warn("Failed to load user profile after login:", e);
      }

      closeLogin();
      return true;
    } catch (e) {
      console.error("登录失败:", e);
      // 提取后端返回的详细错误信息
      if (e?.response?.data?.message) {
        error.value = e.response.data.message;
      } else if (e?.response?.status === 401) {
        error.value = "用户名或密码错误";
      } else if (e?.response?.status === 404) {
        error.value = "用户不存在";
      } else {
        error.value = "登录失败，请检查网络连接";
      }
      return false;
    } finally {
      loading.value = false;
    }
  }

  async function register({ username, password, email }) {
    loading.value = true;
    error.value = "";
    try {
      // 注册接口返回UserDTO对象
      const resp = await authApi.register({
        username,
        password,
        email,
      });

      console.log("注册成功:", resp.data);

      // 注册成功后自动登录
      return await login({ username, password });
    } catch (e) {
      console.error("注册失败:", e);
      // 提取后端返回的详细错误信息
      if (e?.response?.data?.message) {
        // 后端返回的验证错误信息，格式化显示
        const message = e.response.data.message;
        // 将英文字段名翻译成中文
        error.value = message
          .replace(/username:/gi, "用户名:")
          .replace(/password:/gi, "密码:")
          .replace(/email:/gi, "邮箱:")
          .replace(/cannot be blank/gi, "不能为空")
          .replace(/must be between/gi, "长度必须在")
          .replace(/characters/gi, "个字符")
          .replace(/must be at least/gi, "至少需要")
          .replace(/characters long/gi, "个字符")
          .replace(/must be valid/gi, "格式不正确");
      } else if (e?.response?.status === 400) {
        error.value = "注册信息有误，请检查输入";
      } else {
        error.value = "注册失败，请稍后重试";
      }
      return false;
    } finally {
      loading.value = false;
    }
  }

  async function fetchMe() {
    if (!user.value?.userId) return null;

    try {
      // 使用用户ID获取用户信息
      const resp = await authApi.getUserById(user.value.userId);
      const userData = resp.data;

      // 更新用户信息
      user.value = {
        ...user.value,
        ...userData,
        id: userData.id,
        userId: userData.id,
      };
      localStorage.setItem("auth_user", JSON.stringify(user.value));
      return user.value;
    } catch (e) {
      console.error("Failed to fetch user info:", e);
      // 如果获取失败,可能token已过期,尝试刷新
      if (e?.response?.status === 401) {
        await tryRefreshToken();
      }
      return null;
    }
  }

  async function handleSessionExpired(message = "登录信息已过期，请重新登录") {
    error.value = message;
    await logout({ skipServer: true });
    showLoginModal.value = true;
  }

  async function tryRefreshToken() {
    if (!refreshToken.value) {
      await handleSessionExpired();
      return false;
    }

    try {
      const resp = await authApi.refreshToken(refreshToken.value);
      const { accessToken: at, refreshToken: rt } = resp.data;

      accessToken.value = at;
      refreshToken.value = rt;

      localStorage.setItem("access_token", at);
      localStorage.setItem("refresh_token", rt);

      axios.defaults.headers.common["Authorization"] = `Bearer ${at}`;
      return true;
    } catch (e) {
      console.error("Token refresh failed:", e);
      await handleSessionExpired();
      return false;
    }
  }

  async function logout({ skipServer = false } = {}) {
    // 调用后端登出接口 (将token加入黑名单)
    try {
      if (!skipServer && accessToken.value) {
        await authApi.logout();
      }
    } catch (e) {
      console.error("Logout API failed:", e);
    }

    // 清除本地状态
    accessToken.value = "";
    refreshToken.value = "";
    user.value = null;
    localStorage.removeItem("access_token");
    localStorage.removeItem("refresh_token");
    localStorage.removeItem("auth_user");
    delete axios.defaults.headers.common["Authorization"];
  }

  async function updateAvatar(file) {
    if (!file) {
      throw new Error("请选择要上传的图片");
    }

    const formData = new FormData();
    formData.append("file", file);

    try {
      const resp = await authApi.uploadAvatar(formData);
      const updated = resp.data;
      user.value = {
        ...user.value,
        ...updated,
        id: updated.id,
        userId: updated.id,
      };
      localStorage.setItem("auth_user", JSON.stringify(user.value));
      return updated;
    } catch (e) {
      console.error("Failed to upload avatar:", e);
      if (e?.response?.status === 401) {
        await handleSessionExpired();
        e.sessionExpired = true;
      }
      throw e;
    }
  }

  return {
    accessToken,
    refreshToken,
    user,
    loading,
    error,
    showLoginModal,
    isAuthenticated,
    openLogin,
    closeLogin,
    login,
    register,
    fetchMe,
    logout,
    tryRefreshToken,
    updateAvatar,
    handleSessionExpired,
  };
});
