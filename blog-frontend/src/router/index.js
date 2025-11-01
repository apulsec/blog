import { createRouter, createWebHistory } from "vue-router";
import BlogView from "../views/BlogView.vue";
import HomeView from "../views/HomeView.vue";
import CreateArticleView from "../views/CreateArticleView.vue";
import EditArticleView from "../views/EditArticleView.vue";
import ArticleDetailView from "../views/ArticleDetailView.vue";
import { useAuthStore } from "@/stores/authStore";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/",
      name: "blog",
      component: BlogView,
    },
    {
      path: "/article/:id",
      name: "article-detail",
      component: ArticleDetailView,
    },
    {
      path: "/my-blog",
      name: "my-blog",
      component: HomeView,
      meta: { requiresAuth: true },
    },
    {
      path: "/create",
      name: "create-article",
      component: CreateArticleView,
      meta: { requiresAuth: true },
    },
    {
      path: "/edit/:id",
      name: "edit-article",
      component: EditArticleView,
      meta: { requiresAuth: true },
    },
  ],
});

// Global navigation guard for auth-protected routes
router.beforeEach((to, from, next) => {
  const auth = useAuthStore();
  if (to.meta?.requiresAuth && !auth.isAuthenticated) {
    // Open login modal and stay on current page; also set redirect
    auth.openLogin();
    if (to.fullPath !== "/") {
      next({ path: "/", query: { redirect: to.fullPath } });
    } else {
      next(false);
    }
  } else {
    next();
  }
});

export default router;
