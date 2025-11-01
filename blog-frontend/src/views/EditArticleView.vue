<template>
  <div
    class="min-h-screen bg-gradient-to-br from-blue-50 to-white py-12 px-4 sm:px-6 lg:px-8"
  >
    <div class="max-w-4xl mx-auto">
      <div class="bg-white rounded-2xl shadow-xl p-8 border border-blue-100">
        <!-- Header -->
        <div class="mb-8">
          <h1 class="text-3xl font-bold text-gray-900 mb-2">编辑文章</h1>
          <p class="text-gray-600">修改文章内容并重新发布</p>
        </div>

        <!-- Loading State -->
        <div v-if="loading" class="text-center py-12">
          <el-icon class="is-loading text-4xl text-blue-500 mb-4"
            ><Loading
          /></el-icon>
          <p class="text-gray-600">正在加载文章...</p>
        </div>

        <!-- Form -->
        <el-form
          v-else
          :model="form"
          :rules="rules"
          ref="formRef"
          label-width="100px"
          label-position="top"
          class="space-y-6"
        >
          <!-- Title -->
          <el-form-item label="文章标题" prop="title">
            <el-input
              v-model="form.title"
              placeholder="输入文章标题"
              size="large"
              class="rounded-lg"
            />
          </el-form-item>

          <!-- Summary -->
          <el-form-item label="文章摘要" prop="summary">
            <el-input
              v-model="form.summary"
              type="textarea"
              :rows="3"
              placeholder="简要描述文章内容"
              maxlength="200"
              show-word-limit
              class="rounded-lg"
            />
          </el-form-item>

          <!-- Cover Image -->
          <el-form-item label="封面图片URL" prop="coverImageUrl">
            <el-input
              v-model="form.coverImageUrl"
              placeholder="输入图片URL"
              size="large"
              class="rounded-lg"
            />
            <div v-if="form.coverImageUrl" class="mt-4">
              <img
                :src="form.coverImageUrl"
                alt="Cover Preview"
                class="w-full max-w-md h-48 object-cover rounded-lg shadow-md border border-gray-200"
                @error="handleImageError"
              />
            </div>
          </el-form-item>

          <!-- Content -->
          <el-form-item label="文章内容 (Markdown)" prop="content">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="20"
              placeholder="使用 Markdown 编写文章内容"
              class="rounded-lg font-mono"
            />
            <div class="mt-2 text-sm text-gray-500">
              支持 Markdown
              语法：**粗体**、*斜体*、`代码`、[链接](url)、![图片](url) 等
            </div>
          </el-form-item>

          <!-- Tags -->
          <el-form-item label="文章标签">
            <el-select
              v-model="form.tags"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="选择或输入标签"
              class="w-full"
            >
              <el-option
                v-for="tag in availableTags"
                :key="tag.id"
                :label="tag.name"
                :value="tag.name"
              />
            </el-select>
            <div class="mt-2 text-sm text-gray-500">
              可以从列表中选择已有标签,或输入新标签后按回车创建
            </div>
          </el-form-item>

          <!-- Actions -->
          <el-form-item>
            <div class="flex gap-4">
              <el-button
                type="primary"
                size="large"
                @click="submitForm('PUBLISHED')"
                :loading="submitting"
                class="flex-1 bg-gradient-to-r from-blue-500 to-blue-600 border-0 hover:from-blue-600 hover:to-blue-700"
              >
                <el-icon class="mr-2"><Select /></el-icon>
                更新并发布
              </el-button>
              <el-button
                size="large"
                @click="submitForm('DRAFT')"
                :loading="submitting"
                class="flex-1"
              >
                <el-icon class="mr-2"><Document /></el-icon>
                保存为草稿
              </el-button>
              <el-button
                size="large"
                @click="goBack"
                :disabled="submitting"
                class="flex-1"
              >
                <el-icon class="mr-2"><Back /></el-icon>
                取消
              </el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { Loading, Select, Document, Back } from "@element-plus/icons-vue";
import { articleApi } from "@/services/api";

const router = useRouter();
const route = useRoute();
const formRef = ref(null);
const loading = ref(true);
const submitting = ref(false);
const availableTags = ref([]);

const form = ref({
  title: "",
  summary: "",
  coverImageUrl: "",
  content: "",
  tags: [], // Tags array
});

const rules = {
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
      max: 200,
      message: "摘要长度应在 10 到 200 个字符之间",
      trigger: "blur",
    },
  ],
  coverImageUrl: [
    { required: true, message: "请输入封面图片URL", trigger: "blur" },
    { type: "url", message: "请输入有效的URL", trigger: "blur" },
  ],
  content: [
    { required: true, message: "请输入文章内容", trigger: "blur" },
    { min: 50, message: "文章内容至少需要 50 个字符", trigger: "blur" },
  ],
};

// Load article data
onMounted(async () => {
  const articleId = route.params.id;
  try {
    // Load tags
    const tagsResponse = await articleApi.getAllTags();
    availableTags.value = tagsResponse.data;

    // Load article
    const response = await articleApi.getArticleById(articleId);
    const article = response.data;

    form.value = {
      title: article.title,
      summary: article.summary,
      coverImageUrl: article.coverImageUrl,
      content: article.content || "",
      tags: article.tags ? article.tags.map((t) => t.name) : [],
    };

    loading.value = false;
  } catch (error) {
    console.error("Failed to load article:", error);
    ElMessage.error("加载文章失败，请稍后重试");
    router.push("/");
  }
});
const handleImageError = (e) => {
  e.target.src = "https://via.placeholder.com/800x400?text=Image+Not+Found";
};

const submitForm = async (status) => {
  if (!formRef.value) return;

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true;
      try {
        const articleId = route.params.id;
        const articleData = {
          ...form.value,
          status,
        };

        await articleApi.updateArticle(articleId, articleData);

        ElMessage.success({
          message:
            status === "PUBLISHED" ? "文章更新成功！" : "文章已保存为草稿",
          duration: 2000,
        });

        // Navigate back after a short delay
        setTimeout(() => {
          router.push("/");
        }, 1500);
      } catch (error) {
        console.error("Failed to update article:", error);
        ElMessage.error("更新文章失败，请稍后重试");
      } finally {
        submitting.value = false;
      }
    } else {
      ElMessage.warning("请填写所有必填字段");
    }
  });
};

const goBack = () => {
  router.push("/");
};
</script>

<style scoped>
:deep(.el-form-item__label) {
  font-weight: 600;
  color: #374151;
  font-size: 0.875rem;
  margin-bottom: 0.5rem;
}

:deep(.el-input__wrapper) {
  border-radius: 0.5rem;
  box-shadow: 0 1px 2px 0 rgb(0 0 0 / 0.05);
}

:deep(.el-textarea__inner) {
  border-radius: 0.5rem;
  box-shadow: 0 1px 2px 0 rgb(0 0 0 / 0.05);
}

:deep(.el-input__wrapper:hover),
:deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px #3b82f6;
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 2px #3b82f6;
}
</style>
