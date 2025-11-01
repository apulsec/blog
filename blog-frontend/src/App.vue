<template>
  <div class="app-shell">
    <GlobalNav />
    <main class="app-main">
      <router-view />
    </main>
    <LoginModal />
  </div>
</template>

<script setup>
import GlobalNav from "@/components/GlobalNav.vue";
import LoginModal from "@/components/LoginModal.vue";
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: #f3f4f6;
  color: #1f2937;
}

.app-main {
  min-height: calc(100vh - 56px);
}
</style>
