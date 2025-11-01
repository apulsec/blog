<template>
  <el-dialog
    v-model="visible"
    width="420px"
    :show-close="!auth.loading"
    title="账户登录"
  >
    <el-tabs v-model="activeTab">
      <el-tab-pane label="登录" name="login">
        <el-form :model="loginForm" @submit.prevent>
          <el-form-item label="用户名">
            <el-input v-model="loginForm.username" autocomplete="username" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="loginForm.password"
              type="password"
              autocomplete="current-password"
            />
          </el-form-item>
          <el-alert
            v-if="auth.error"
            :title="auth.error"
            type="error"
            show-icon
            class="mb-2"
          />
          <el-button
            type="primary"
            :loading="auth.loading"
            @click="handleLogin"
            class="w-full"
            >登录</el-button
          >
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="注册" name="register">
        <el-form :model="registerForm" @submit.prevent>
          <el-form-item label="用户名">
            <el-input v-model="registerForm.username" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="registerForm.password" type="password" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="registerForm.email" type="email" />
          </el-form-item>
          <el-alert
            v-if="auth.error"
            :title="auth.error"
            type="error"
            show-icon
            class="mb-2"
          />
          <el-button
            type="primary"
            :loading="auth.loading"
            @click="handleRegister"
            class="w-full"
            >注册并登录</el-button
          >
        </el-form>
      </el-tab-pane>
    </el-tabs>
    <template #footer>
      <el-button :disabled="auth.loading" @click="auth.closeLogin"
        >取消</el-button
      >
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useAuthStore } from "@/stores/authStore";

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();

const visible = computed({
  get: () => auth.showLoginModal,
  set: (v) => (v ? auth.openLogin() : auth.closeLogin()),
});

const activeTab = ref("login");
const loginForm = ref({ username: "", password: "" });
const registerForm = ref({
  username: "",
  password: "",
  email: "",
});

async function handleLogin() {
  const ok = await auth.login(loginForm.value);
  if (ok) {
    // After login, if trying to access a protected route, redirect
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

watch(
  () => auth.isAuthenticated,
  (v) => {
    if (v) visible.value = false;
  }
);
</script>

<style scoped>
.w-full {
  width: 100%;
}
.mb-2 {
  margin-bottom: 12px;
}
</style>
