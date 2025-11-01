<template>
  <div class="create-article-page">
    <div class="container">
      <!-- 页面标题 -->
      <div class="page-header">
        <h1>✍️ 创建新文章</h1>
        <p>分享你的想法和知识</p>
      </div>

      <!-- 文章表单 -->
      <el-form
        ref="articleFormRef"
        :model="articleForm"
        :rules="formRules"
        label-position="top"
        class="article-form"
      >
        <!-- 标题 -->
        <el-form-item label="文章标题" prop="title">
          <el-input
            v-model="articleForm.title"
            placeholder="输入一个吸引人的标题..."
            size="large"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <!-- 摘要 -->
        <el-form-item label="文章摘要" prop="summary">
          <el-input
            v-model="articleForm.summary"
            type="textarea"
            placeholder="简要描述文章内容（将显示在列表中）..."
            :rows="3"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <!-- 封面图片 URL -->
        <el-form-item label="封面图片 URL" prop="coverImageUrl">
          <el-input
            v-model="articleForm.coverImageUrl"
            placeholder="https://example.com/image.jpg"
          />
          <div v-if="articleForm.coverImageUrl" class="image-preview">
            <img :src="articleForm.coverImageUrl" alt="封面预览" />
          </div>
        </el-form-item>

        <!-- 文章内容 (Markdown) -->
        <el-form-item label="文章内容 (支持 Markdown)" prop="content">
          <el-input
            v-model="articleForm.content"
            type="textarea"
            placeholder="# 标题&#10;&#10;在这里输入文章内容，支持 Markdown 格式..."
            :rows="15"
            class="content-editor"
          />
        </el-form-item>

        <!-- 标签 -->
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
            可以从列表中选择已有标签,或输入新标签后按回车创建
          </div>
        </el-form-item>

        <!-- 操作按钮 -->
        <el-form-item>
          <div class="form-actions">
            <el-button
              type="primary"
              size="large"
              :loading="submitting"
              @click="submitArticle('PUBLISHED')"
            >
              <span v-if="!submitting">🚀 发布文章</span>
              <span v-else>发布中...</span>
            </el-button>
            <el-button
              size="large"
              :loading="submitting"
              @click="submitArticle('DRAFT')"
            >
              <span v-if="!submitting">📝 保存草稿</span>
              <span v-else>保存中...</span>
            </el-button>
            <el-button size="large" @click="resetForm"> 🔄 重置 </el-button>
            <el-button size="large" @click="goBack"> ← 返回列表 </el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { articleApi } from "@/services/api";

const router = useRouter();
const articleFormRef = ref(null);
const submitting = ref(false);
const availableTags = ref([]);

// 表单数据
const articleForm = reactive({
  title: "",
  summary: "",
  coverImageUrl: "",
  content: "",
  status: "PUBLISHED",
  tags: [], // 标签列表
});

// Load available tags
onMounted(async () => {
  try {
    const response = await articleApi.getAllTags();
    availableTags.value = response.data;
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
    const response = await articleApi.createArticle(articleForm);

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
  min-height: 100vh;
  padding: 2rem 1rem;
}

.container {
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 3rem;
  padding-bottom: 2rem;
  border-bottom: 2px solid #e0f2fe;
}

.page-header h1 {
  font-size: 2.5rem;
  color: #1e40af;
  margin-bottom: 0.5rem;
  font-weight: 700;
}

.page-header p {
  font-size: 1.1rem;
  color: #64748b;
}

.article-form {
  background: white;
  padding: 2.5rem;
  border-radius: 1rem;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

:deep(.el-form-item__label) {
  font-size: 1rem;
  font-weight: 600;
  color: #334155;
}

:deep(.el-input__inner),
:deep(.el-textarea__inner) {
  border-radius: 0.5rem;
  border: 2px solid #e2e8f0;
  transition: all 0.2s ease-in-out;
}

:deep(.el-input__inner:focus),
:deep(.el-textarea__inner:focus) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.content-editor {
  font-family: "Consolas", "Monaco", "Courier New", monospace;
  font-size: 0.95rem;
}

.image-preview {
  margin-top: 1rem;
  border-radius: 0.5rem;
  overflow: hidden;
  max-width: 400px;
}

.image-preview img {
  width: 100%;
  height: auto;
  display: block;
  border: 2px solid #e2e8f0;
  border-radius: 0.5rem;
}

.tag-select {
  width: 100%;
}

.tag-hint {
  margin-top: 0.5rem;
  font-size: 0.813rem;
  color: #6b7280;
}

.form-actions {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  margin-top: 2rem;
  padding-top: 2rem;
  border-top: 2px solid #e0f2fe;
}

:deep(.el-button) {
  border-radius: 0.5rem;
  font-weight: 600;
  padding: 0.75rem 1.5rem;
  transition: all 0.2s ease-in-out;
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border: none;
}

:deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

:deep(.el-button--default:hover) {
  background: #f1f5f9;
  border-color: #cbd5e1;
  transform: translateY(-1px);
}

@media (max-width: 768px) {
  .page-header h1 {
    font-size: 2rem;
  }

  .article-form {
    padding: 1.5rem;
  }

  .form-actions {
    flex-direction: column;
  }

  .form-actions .el-button {
    width: 100%;
  }
}
</style>
