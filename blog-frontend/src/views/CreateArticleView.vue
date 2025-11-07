<template>
  <div class="create-article-page">
    <div class="container">
      <header class="page-header">
        <div class="page-header__content">
          <div class="page-header__text">
            <span class="header-kicker">写文章</span>
            <h1 class="header-title">✍️ 创建新文章</h1>
            <p class="header-subtitle">
              分享你的想法和知识，让读者更快遇到你的灵感。
            </p>
          </div>
          <div class="header-stats">
            <div class="header-stat">
              <span class="stat-label"
                ><span class="stat-icon">📝</span> 摘要字数</span
              >
              <span class="stat-value">{{ summaryCharCount }}</span>
            </div>
            <div class="header-stat">
              <span class="stat-label"
                ><span class="stat-icon">⌨️</span> 正文字数</span
              >
              <span class="stat-value">{{ contentCharCount }}</span>
            </div>
            <div class="header-stat">
              <span class="stat-label"
                ><span class="stat-icon">🏷️</span> 标签</span
              >
              <span class="stat-value">{{ articleForm.tags.length }}</span>
            </div>
          </div>
        </div>
        <div class="header-actions">
          <el-button class="ghost-btn" @click="goBack">
            <span class="btn-icon">←</span>
            返回列表
          </el-button>
          <el-button class="ghost-btn" plain @click="resetForm">
            <span class="btn-icon">🧹</span>
            重置表单
          </el-button>
        </div>
      </header>

      <div class="form-shell">
        <div class="form-layout">
          <el-form
            ref="articleFormRef"
            :model="articleForm"
            :rules="formRules"
            label-position="top"
            class="article-form"
          >
            <section class="form-section">
              <div class="section-header">
                <span class="section-icon">🧭</span>
                <div>
                  <h2 class="section-title">基础信息</h2>
                  <p class="section-subtitle">
                    标题与摘要决定读者是否点开，务必清晰且富有吸引力。
                  </p>
                </div>
              </div>
              <div class="field-grid">
                <el-form-item
                  label="文章标题"
                  prop="title"
                  class="field-span-2"
                >
                  <el-input
                    v-model="articleForm.title"
                    placeholder="输入一个吸引人的标题..."
                    size="large"
                    maxlength="100"
                    show-word-limit
                  />
                </el-form-item>
                <el-form-item
                  label="文章摘要"
                  prop="summary"
                  class="field-span-2"
                >
                  <el-input
                    v-model="articleForm.summary"
                    type="textarea"
                    placeholder="简要描述文章内容（将显示在列表中）..."
                    :rows="4"
                    maxlength="500"
                    show-word-limit
                  />
                </el-form-item>
              </div>
            </section>

            <section class="form-section">
              <div class="section-header">
                <span class="section-icon">🖼️</span>
                <div>
                  <h2 class="section-title">封面与标签</h2>
                  <p class="section-subtitle">
                    合适的封面能吸引目光，标签帮助文章准确触达读者。
                  </p>
                </div>
              </div>
              <div class="field-grid two-column">
                <el-form-item label="封面图片 URL" prop="coverImageUrl">
                  <el-input
                    v-model="articleForm.coverImageUrl"
                    placeholder="https://example.com/image.jpg"
                  />
                  <p class="field-hint">
                    建议使用 16:9 比例高清图，支持 PNG/JPG/WebP。
                  </p>
                </el-form-item>
                <el-form-item label="文章标签">
                  <el-select
                    v-model="articleForm.tags"
                    multiple
                    filterable
                    allow-create
                    default-first-option
                    placeholder="选择或输入标签"
                    class="tag-select"
                  >
                    <el-option
                      v-for="tag in availableTags"
                      :key="tag.id"
                      :label="tag.name"
                      :value="tag.name"
                    />
                  </el-select>
                  <div class="tag-hint">
                    可以从列表中选择已有标签，或输入新标签后按回车创建。
                  </div>
                </el-form-item>
              </div>
            </section>

            <section class="form-section">
              <div class="section-header">
                <span class="section-icon">📝</span>
                <div>
                  <h2 class="section-title">文章内容</h2>
                  <p class="section-subtitle">
                    支持 Markdown 语法，可直接粘贴已有内容。
                  </p>
                </div>
              </div>
              <el-form-item label="文章内容 (支持 Markdown)" prop="content">
                <el-input
                  v-model="articleForm.content"
                  type="textarea"
                  placeholder="# 标题&#10;&#10;在这里输入文章内容，支持 Markdown 格式..."
                  :rows="16"
                  class="content-editor"
                />
              </el-form-item>
              <div class="editor-meter">
                <span
                  ><span class="inline-icon">⌨️</span> 当前字数
                  {{ contentCharCount }}</span
                >
                <span
                  ><span class="inline-icon">ℹ️</span>
                  发布后仍可再次编辑文章内容</span
                >
              </div>
            </section>

            <div class="form-footer">
              <div class="footer-info">
                <span class="inline-icon">🛡️</span>
                所有草稿都会妥善保存，仅你可见。
              </div>
              <div class="footer-actions">
                <el-button
                  class="ghost-btn"
                  :loading="submitting"
                  @click="submitArticle('DRAFT')"
                >
                  <span class="btn-icon">💾</span>
                  <span>{{ submitting ? "保存中..." : "保存草稿" }}</span>
                </el-button>
                <el-button
                  type="primary"
                  class="primary-btn"
                  size="large"
                  :loading="submitting"
                  @click="submitArticle('PUBLISHED')"
                >
                  <span class="btn-icon">📤</span>
                  <span>{{ submitting ? "发布中..." : "发布文章" }}</span>
                </el-button>
              </div>
            </div>
          </el-form>

          <aside class="preview-panel">
            <div class="preview-card">
              <div
                class="preview-cover"
                :class="{ 'has-image': !!previewCover }"
                :style="{
                  backgroundImage: previewCover
                    ? 'url(' + previewCover + ')'
                    : '',
                }"
              >
                <div v-if="!previewCover" class="cover-placeholder">
                  <span class="cover-icon">🖼️</span>
                  <span>封面预览</span>
                </div>
              </div>
              <div class="preview-body">
                <div v-if="previewTags.length" class="preview-tags">
                  <span
                    v-for="tag in previewTags"
                    :key="tag.name"
                    class="preview-tag"
                    :style="{ backgroundColor: tag.color }"
                  >
                    #{{ tag.name }}
                  </span>
                </div>
                <h3 class="preview-title">{{ previewTitle }}</h3>
                <p class="preview-summary">{{ previewSummary }}</p>
                <div class="preview-divider"></div>
                <div class="preview-content">
                  <p>{{ previewContent }}</p>
                </div>
              </div>
            </div>
            <div class="preview-hint">
              <span class="inline-icon">💡</span>
              实时预览帮助你把控文章信息展示效果。
            </div>
          </aside>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { articleApi } from "@/services/api";

const router = useRouter();
const articleFormRef = ref(null);
const submitting = ref(false);
const availableTags = ref([]);
const fallbackTagColors = [
  "#f472b6",
  "#60a5fa",
  "#34d399",
  "#facc15",
  "#f97316",
  "#a855f7",
];

// 表单数据
const articleForm = reactive({
  title: "",
  summary: "",
  coverImageUrl: "",
  content: "",
  status: "PUBLISHED",
  tags: [], // 标签列表
});

const summaryCharCount = computed(() =>
  articleForm.summary
    ? articleForm.summary.replace(/\s+/g, " ").trim().length
    : 0
);

const contentCharCount = computed(() =>
  articleForm.content
    ? articleForm.content.replace(/\s+/g, " ").trim().length
    : 0
);

const previewCover = computed(() => articleForm.coverImageUrl?.trim() || "");

const previewTitle = computed(
  () => articleForm.title?.trim() || "文章标题预览"
);

const previewSummary = computed(
  () =>
    articleForm.summary?.trim() ||
    "你的摘要会展示在这里，尝试用一两句话勾勒文章亮点。"
);

const previewContent = computed(() => {
  if (!articleForm.content?.trim()) {
    return "正文预览区域会展示内容的前几行，保持段落清晰，让读者迅速了解主题。";
  }

  const sanitized = articleForm.content
    .replace(/[>#*_`]+/g, " ")
    .replace(/\s+/g, " ")
    .trim();
  return sanitized.length > 220 ? `${sanitized.slice(0, 220)}...` : sanitized;
});

const previewTags = computed(() =>
  (articleForm.tags || []).slice(0, 6).map((name, index) => {
    const matched = availableTags.value.find((tag) => tag.name === name);
    return {
      name,
      color:
        matched?.color || fallbackTagColors[index % fallbackTagColors.length],
    };
  })
);

// Load available tags
onMounted(async () => {
  try {
    const { data } = await articleApi.getAllTags();
    availableTags.value = data;
  } catch (error) {
    console.error("Failed to load tags:", error);
  }
});

// 表单验证规则
const formRules = {
  title: [
    { required: true, message: "请输入文章标题", trigger: "blur" },
    {
      min: 5,
      max: 100,
      message: "标题长度应在 5 到 100 个字符之间",
      trigger: "blur",
    },
  ],
  summary: [
    { required: true, message: "请输入文章摘要", trigger: "blur" },
    {
      min: 10,
      max: 500,
      message: "摘要长度应在 10 到 500 个字符之间",
      trigger: "blur",
    },
  ],
  content: [
    { required: true, message: "请输入文章内容", trigger: "blur" },
    { min: 50, message: "内容至少需要 50 个字符", trigger: "blur" },
  ],
};

// 提交文章
const submitArticle = async (status) => {
  if (!articleFormRef.value) return;

  try {
    // 验证表单
    await articleFormRef.value.validate();

    submitting.value = true;
    articleForm.status = status;

    // 调用 API 创建文章
    await articleApi.createArticle(articleForm);

    ElMessage.success({
      message: status === "PUBLISHED" ? "文章发布成功！" : "草稿保存成功！",
      duration: 2000,
    });

    // 延迟跳转，让用户看到成功提示
    setTimeout(() => {
      router.push("/");
    }, 1500);
  } catch (error) {
    console.error("提交文章失败:", error);
    ElMessage.error("操作失败：" + (error.message || "未知错误"));
  } finally {
    submitting.value = false;
  }
};

// 重置表单
const resetForm = () => {
  if (articleFormRef.value) {
    articleFormRef.value.resetFields();
  }
};

// 返回列表
const goBack = () => {
  router.push("/");
};
</script>

<style scoped>
.create-article-page {
  position: relative;
  min-height: 100vh;
  padding: 3.5rem 1rem 4rem;
  background: linear-gradient(135deg, #f5f6ff 0%, #f8fbff 40%, #eef2ff 100%);
}

.create-article-page::before {
  content: "";
  position: absolute;
  inset: 0;
  background: radial-gradient(
      900px circle at 10% 10%,
      rgba(59, 130, 246, 0.08),
      transparent
    ),
    radial-gradient(
      800px circle at 90% 15%,
      rgba(236, 72, 153, 0.08),
      transparent
    );
  pointer-events: none;
}

.container {
  position: relative;
  z-index: 1;
  max-width: 1180px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
  padding: 2.75rem;
  border-radius: 1.75rem;
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.92),
    rgba(248, 250, 255, 0.96)
  );
  border: 1px solid rgba(148, 163, 184, 0.22);
  box-shadow: 0 25px 65px -35px rgba(15, 23, 42, 0.35);
  margin-bottom: 2.5rem;
  backdrop-filter: blur(16px);
}

.page-header__content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 2.5rem;
  flex-wrap: wrap;
}

.header-kicker {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.35rem 0.85rem;
  font-weight: 600;
  color: #2563eb;
  background: rgba(37, 99, 235, 0.12);
  border-radius: 999px;
  font-size: 0.875rem;
}

.header-title {
  margin: 0.6rem 0 0.5rem;
  font-size: 2.65rem;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.04em;
}

.header-subtitle {
  margin: 0;
  max-width: 460px;
  font-size: 1.05rem;
  color: #475569;
  line-height: 1.7;
}

.header-stats {
  display: grid;
  gap: 0.75rem;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  min-width: 260px;
}

.header-stat {
  padding: 0.95rem 1.1rem;
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgba(148, 163, 184, 0.25);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.4);
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  color: #1f2937;
}

.stat-label {
  font-size: 0.813rem;
  font-weight: 600;
  color: #64748b;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.stat-icon,
.inline-icon,
.btn-icon,
.cover-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.stat-icon {
  font-size: 0.95rem;
}

.inline-icon {
  font-size: 1.05rem;
  margin-right: 0.35rem;
}

.btn-icon {
  font-size: 1.05rem;
  margin-right: 0.5rem;
}

.cover-icon {
  font-size: 1.75rem;
  margin-bottom: 0.25rem;
}

.stat-value {
  font-size: 1.4rem;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.85rem;
}

.form-shell {
  position: relative;
}

.form-layout {
  display: grid;
  gap: 2rem;
  grid-template-columns: minmax(0, 2.25fr) minmax(0, 1.1fr);
  align-items: start;
}

.article-form {
  padding: 2.5rem;
  border-radius: 1.75rem;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.22);
  box-shadow: 0 25px 65px -40px rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(18px);
}

.form-section + .form-section {
  margin-top: 2.5rem;
  padding-top: 2.5rem;
  border-top: 1px dashed rgba(148, 163, 184, 0.35);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.section-icon {
  font-size: 1.5rem;
  filter: drop-shadow(0 6px 14px rgba(59, 130, 246, 0.25));
}

.section-title {
  margin: 0;
  font-size: 1.35rem;
  font-weight: 700;
  color: #0f172a;
}

.section-subtitle {
  margin: 0.25rem 0 0;
  font-size: 0.92rem;
  color: #64748b;
  line-height: 1.6;
}

.field-grid {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.field-grid.two-column {
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
}

.field-span-2 {
  grid-column: 1 / -1;
}

.field-hint {
  margin-top: 0.6rem;
  font-size: 0.78rem;
  color: #64748b;
}

.tag-hint {
  margin-top: 0.65rem;
  font-size: 0.82rem;
  color: #6366f1;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.content-editor {
  font-family: "Fira Code", "JetBrains Mono", "Consolas", monospace;
  font-size: 0.94rem;
  line-height: 1.6;
}

.editor-meter {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  justify-content: space-between;
  padding: 0.95rem 1.2rem;
  border-radius: 1rem;
  background: rgba(241, 245, 249, 0.85);
  border: 1px solid rgba(148, 163, 184, 0.25);
  color: #475569;
  font-size: 0.85rem;
}

.form-footer {
  margin-top: 3rem;
  padding-top: 2rem;
  border-top: 1px dashed rgba(148, 163, 184, 0.35);
  display: flex;
  flex-wrap: wrap;
  gap: 1.5rem;
  justify-content: space-between;
  align-items: center;
}

.footer-info {
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  color: #475569;
  font-size: 0.9rem;
}

.footer-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.85rem;
}

.ghost-btn {
  border-radius: 999px;
  padding: 0.8rem 1.4rem;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: rgba(255, 255, 255, 0.55);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.65);
  color: #1e293b;
  transition: all 0.24s ease;
}

.ghost-btn:hover {
  border-color: rgba(99, 102, 241, 0.4);
  box-shadow: 0 12px 30px -18px rgba(99, 102, 241, 0.7);
  transform: translateY(-2px);
  color: #3730a3;
}

.primary-btn {
  border-radius: 999px;
  padding: 0.85rem 1.9rem;
  background: linear-gradient(135deg, #6366f1, #3b82f6, #10b981);
  box-shadow: 0 18px 40px -24px rgba(37, 99, 235, 0.85);
  border: none;
}

.primary-btn:hover {
  box-shadow: 0 24px 46px -24px rgba(59, 130, 246, 0.9);
  transform: translateY(-2px);
}

.preview-panel {
  position: sticky;
  top: 2rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.preview-card {
  border-radius: 1.6rem;
  overflow: hidden;
  background: rgba(15, 23, 42, 0.92);
  color: #e2e8f0;
  border: 1px solid rgba(15, 23, 42, 0.45);
  box-shadow: 0 28px 60px -35px rgba(15, 23, 42, 0.85);
}

.preview-cover {
  position: relative;
  width: 100%;
  padding-top: 56.25%;
  background: linear-gradient(
    135deg,
    rgba(37, 99, 235, 0.4),
    rgba(236, 72, 153, 0.35)
  );
  background-size: cover;
  background-position: center;
  transition: transform 0.3s ease;
}

.preview-cover.has-image {
  filter: saturate(115%);
}

.cover-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.6rem;
  color: rgba(226, 232, 240, 0.85);
  background: linear-gradient(
    135deg,
    rgba(30, 64, 175, 0.65),
    rgba(236, 72, 153, 0.55)
  );
  font-size: 0.95rem;
}

.preview-body {
  padding: 1.8rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.preview-tag {
  padding: 0.3rem 0.75rem;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 600;
  color: #0f172a;
  background: rgba(255, 255, 255, 0.8);
}

.preview-title {
  margin: 0;
  font-size: 1.45rem;
  font-weight: 700;
  line-height: 1.4;
  color: #f8fafc;
}

.preview-summary {
  margin: 0;
  font-size: 0.95rem;
  color: rgba(226, 232, 240, 0.75);
  line-height: 1.6;
}

.preview-divider {
  height: 1px;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(148, 163, 184, 0.5),
    transparent
  );
}

.preview-content {
  font-size: 0.9rem;
  color: rgba(226, 232, 240, 0.85);
  line-height: 1.7;
  max-height: 220px;
  overflow: hidden;
}

.preview-content p {
  margin: 0;
}

.preview-hint {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 1rem 1.25rem;
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.65);
  border: 1px solid rgba(148, 163, 184, 0.2);
  color: #475569;
  font-size: 0.9rem;
}

:deep(.article-form .el-form-item__label) {
  font-size: 0.95rem;
  font-weight: 600;
  color: #334155;
}

:deep(.article-form .el-input__wrapper) {
  border-radius: 0.9rem;
  background: rgba(248, 250, 255, 0.8);
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.25);
  transition: all 0.24s ease;
  padding: 0 0.85rem;
}

:deep(.article-form .el-input__wrapper.is-focus),
:deep(.article-form .el-textarea__inner:focus) {
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
  border-color: transparent;
}

:deep(.article-form .el-textarea__inner) {
  border-radius: 1.1rem;
  background: rgba(248, 250, 255, 0.82);
  border: 1px solid rgba(148, 163, 184, 0.25);
  transition: all 0.24s ease;
  padding: 1rem 1.2rem;
}

:deep(.article-form .el-select__wrapper) {
  border-radius: 0.9rem;
  background: rgba(248, 250, 255, 0.82);
  border: 1px solid rgba(148, 163, 184, 0.25);
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.12);
}

:deep(.tag-select .el-select__tags-text) {
  font-weight: 600;
  color: #1f2937;
}

:deep(.el-select__input) {
  padding: 0.35rem 0.4rem;
}

:deep(.el-button.is-loading .el-icon) {
  margin-right: 0.4rem;
}

@media (max-width: 1160px) {
  .form-layout {
    grid-template-columns: 1fr;
  }

  .preview-panel {
    position: static;
  }
}

@media (max-width: 768px) {
  .page-header {
    padding: 2.2rem;
  }

  .header-title {
    font-size: 2.1rem;
  }

  .header-subtitle {
    max-width: 100%;
  }

  .article-form {
    padding: 2rem;
  }

  .editor-meter {
    flex-direction: column;
    align-items: flex-start;
  }

  .form-footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .footer-actions {
    width: 100%;
  }

  .footer-actions .el-button,
  .header-actions .el-button {
    width: 100%;
    justify-content: center;
  }
}

@media (max-width: 540px) {
  .page-header {
    padding: 1.75rem;
  }

  .header-title {
    font-size: 1.85rem;
  }

  .article-form {
    padding: 1.6rem;
  }

  .preview-body {
    padding: 1.5rem;
  }
}
</style>
