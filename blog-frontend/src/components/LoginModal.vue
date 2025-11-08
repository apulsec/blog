<template>
  <el-dialog
    v-model="visible"
    width="480px"
    :show-close="false"
    align-center
    class="auth-dialog"
  >
    <div class="auth-card">
      <div class="auth-top-glow" />
      <header class="auth-header">
        <div class="logo-mark">
          <span>B</span>
        </div>
        <h3>{{ activeTab === "login" ? "欢迎回来" : "加入 Blog Hub" }}</h3>
        <p>{{ subtitleText }}</p>
      </header>
      <el-tabs v-model="activeTab" stretch class="auth-tabs">
        <el-tab-pane label="登录" name="login">
          <el-form
            :model="loginForm"
            label-position="top"
            class="auth-form"
            @submit.prevent
          >
            <el-form-item label="用户名">
              <el-input
                v-model="loginForm.username"
                autocomplete="username"
                placeholder="请输入用户名"
                :prefix-icon="User"
              />
            </el-form-item>
            <el-form-item label="密码">
              <el-input
                v-model="loginForm.password"
                type="password"
                autocomplete="current-password"
                placeholder="请输入密码"
                show-password
                :prefix-icon="Lock"
              />
            </el-form-item>
            <el-alert
              v-if="auth.error"
              :title="auth.error"
              type="error"
              show-icon
              class="form-alert"
            />
            <el-button
              class="submit-btn"
              type="primary"
              :loading="auth.loading"
              @click="handleLogin"
              >立即登录</el-button
            >
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form
            :model="registerForm"
            label-position="top"
            class="auth-form"
            @submit.prevent
          >
            <el-form-item label="用户名">
              <el-input
                v-model="registerForm.username"
                placeholder="设置你的昵称"
                :prefix-icon="User"
              />
            </el-form-item>
            <el-form-item label="密码">
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="设置登录密码"
                show-password
                :prefix-icon="Lock"
              />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input
                v-model="registerForm.email"
                type="email"
                placeholder="用于接收通知"
                :prefix-icon="Message"
              />
            </el-form-item>
            <el-alert
              v-if="auth.error"
              :title="auth.error"
              type="error"
              show-icon
              class="form-alert"
            />
            <el-button
              class="submit-btn"
              type="primary"
              :loading="auth.loading"
              @click="handleRegister"
              >注册并登录</el-button
            >
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <footer class="auth-footer">
        <el-button text :disabled="auth.loading" @click="auth.closeLogin"
          >暂不登录</el-button
        >
      </footer>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from "vue";
import { useRouter, useRoute } from "vue-router";
import { User, Lock, Message } from "@element-plus/icons-vue";
import { useAuthStore } from "@/stores/authStore";

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();

const visible = computed({
  get: () => auth.showLoginModal,
  set: (v) => (v ? auth.openLogin() : auth.closeLogin()),
});

const activeTab = ref("login");
const loginFormDefaults = { username: "", password: "" };
const registerFormDefaults = {
  username: "",
  password: "",
  email: "",
};
const loginForm = ref({ ...loginFormDefaults });
const registerForm = ref({ ...registerFormDefaults });

const subtitleText = computed(() =>
  activeTab.value === "login"
    ? "输入账号密码，快速进入你的创作空间"
    : "一分钟完成注册，加入创作者社区"
);

async function handleLogin() {
  const ok = await auth.login(loginForm.value);
  if (ok) {
    const redirect = route.query.redirect || "/";
    router.replace(redirect);
  }
}

async function handleRegister() {
  const ok = await auth.register(registerForm.value);
  if (ok) {
    const redirect = route.query.redirect || "/";
    router.replace(redirect);
  }
}

function resetForms() {
  loginForm.value = { ...loginFormDefaults };
  registerForm.value = { ...registerFormDefaults };
  activeTab.value = "login";
  if (typeof auth.clearError === "function") {
    auth.clearError();
  }
}

watch(
  () => auth.showLoginModal,
  (isOpen) => {
    if (isOpen) {
      resetForms();
    }
  }
);

watch(
  () => auth.isAuthenticated,
  (v) => {
    if (v) visible.value = false;
  }
);
</script>

<style scoped>
.auth-dialog {
  border-radius: 28px;
  border: 1.5px solid rgba(148, 163, 184, 0.2);
  box-shadow: 0 32px 60px rgba(15, 23, 42, 0.2);
  backdrop-filter: blur(24px);
  padding: 0;
  overflow: hidden;
  background: radial-gradient(
    circle at top,
    rgba(79, 70, 229, 0.16),
    rgba(248, 250, 252, 0.92)
  );
}

.auth-dialog :deep(.el-dialog__header) {
  display: none;
}

.auth-dialog :deep(.el-dialog__body) {
  padding: 28px 32px 32px;
  background: transparent;
}

.auth-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 22px;
  padding: 36px 32px 32px;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(22px);
  box-shadow: 0 26px 48px rgba(15, 23, 42, 0.18);
  border: 1.5px solid rgba(148, 163, 184, 0.15);
}

.auth-top-glow {
  position: absolute;
  inset: -70px auto auto 50%;
  width: 220px;
  height: 220px;
  transform: translateX(-50%);
  background: radial-gradient(
    circle,
    rgba(99, 102, 241, 0.35),
    transparent 70%
  );
  filter: blur(26px);
  pointer-events: none;
  z-index: 0;
}

.auth-header {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  text-align: center;
  margin-top: 4px;
}

.logo-mark {
  width: 52px;
  height: 52px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  background: linear-gradient(140deg, #4338ca, #6366f1);
  color: #ffffff;
  font-weight: 700;
  font-size: 20px;
  letter-spacing: 0.5px;
  box-shadow: 0 10px 20px rgba(79, 70, 229, 0.35);
}

.auth-header h3 {
  font-size: 24px;
  font-weight: 600;
  color: #0f172a;
}

.auth-header p {
  font-size: 14px;
  color: #64748b;
}

.auth-tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
}

.auth-tabs :deep(.el-tabs__item) {
  font-weight: 500;
}

.auth-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  border-radius: 999px;
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.auth-form :deep(.el-input__wrapper) {
  padding: 0 14px;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
  border: 1px solid transparent;
  background-image: linear-gradient(#ffffff, #ffffff),
    linear-gradient(120deg, rgba(79, 70, 229, 0.25), rgba(14, 165, 233, 0.25));
  background-origin: border-box;
  background-clip: padding-box, border-box;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 10px 30px rgba(59, 130, 246, 0.18);
  transform: translateY(-1px);
}

.auth-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #1e293b;
}

.submit-btn {
  width: 100%;
  border-radius: 16px;
  font-weight: 600;
  letter-spacing: 0.4px;
  background: linear-gradient(
    135deg,
    #4338ca 0%,
    #6366f1 35%,
    #6366f1 65%,
    #2563eb 100%
  );
  border: none;
  box-shadow: 0 18px 32px rgba(79, 70, 229, 0.35);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.submit-btn :deep(span) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 22px 40px rgba(79, 70, 229, 0.4);
}

.submit-btn.is-loading {
  transform: none;
  box-shadow: 0 18px 32px rgba(79, 70, 229, 0.35);
}

.form-alert {
  margin-bottom: 16px;
}

.auth-footer {
  display: flex;
  justify-content: center;
}

.auth-footer :deep(.el-button.is-text) {
  font-size: 13px;
  color: #475569;
}

@media (max-width: 640px) {
  .auth-dialog {
    width: 92% !important;
  }

  .auth-dialog :deep(.el-dialog__body) {
    padding: 20px 18px 24px;
  }

  .auth-card {
    padding: 28px 22px 24px;
  }
}
</style>
