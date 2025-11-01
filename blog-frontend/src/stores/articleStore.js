import { defineStore } from "pinia";
import { ref } from "vue";
import { articleApi } from "@/services/api";

/**
 * Pinia store for managing article state.
 * Handles article data, pagination, loading states, and API calls.
 */
export const useArticleStore = defineStore("article", () => {
  // State
  const articles = ref([]);
  const currentPage = ref(1);
  const pageSize = ref(10);
  const totalItems = ref(0);
  const loading = ref(false);
  const error = ref(null);
  const currentStatus = ref(null); // Current status filter
  const currentTags = ref([]); // Current selected tags (multiple)
  const currentKeyword = ref(""); // Current search keyword
  const currentAuthorId = ref(null); // Current author ID filter
  const availableTags = ref([]); // Available tags

  /**
   * Fetches articles from the backend API.
   * Updates the store state with the fetched data.
   *
   * @param {number} page - Page number to fetch (1-indexed for UI)
   * @param {number} size - Number of items per page
   * @param {string} status - Optional status filter
   * @param {Array<string>} tags - Optional tags filter (multiple)
   * @param {string} keyword - Optional search keyword
   * @param {number} authorId - Optional author ID filter
   */
  async function fetchArticles(
    page = currentPage.value,
    size = pageSize.value,
    status = currentStatus.value,
    tags = currentTags.value,
    keyword = currentKeyword.value,
    authorId = currentAuthorId.value
  ) {
    loading.value = true;
    error.value = null;

    try {
      // API uses 0-indexed pages, so subtract 1
      // Note: Backend currently only supports single tag, we'll fetch and filter client-side for multiple tags
      const response = await articleApi.getArticles(
        page - 1,
        size,
        status,
        tags.length > 0 ? tags[0] : null, // For now, use first tag
        keyword || null,
        authorId
      );

      let filteredArticles = response.data.records;

      // Client-side filtering for multiple tags if needed
      if (tags.length > 1) {
        filteredArticles = filteredArticles.filter((article) =>
          tags.every((tag) =>
            article.tags?.some((articleTag) => articleTag.name === tag)
          )
        );
      }

      // Update state with response data
      articles.value = filteredArticles;
      currentPage.value = response.data.current;
      pageSize.value = response.data.size;
      totalItems.value =
        tags.length > 1 ? filteredArticles.length : response.data.total;
      currentStatus.value = status;
      currentTags.value = tags;
      currentKeyword.value = keyword;
      currentAuthorId.value = authorId;
    } catch (err) {
      error.value = "Failed to fetch articles. Please try again later.";
      console.error("Error fetching articles:", err);
    } finally {
      loading.value = false;
    }
  }

  /**
   * Fetches all available tags.
   */
  async function fetchTags() {
    try {
      const response = await articleApi.getAllTags();
      availableTags.value = response.data;
    } catch (err) {
      console.error("Error fetching tags:", err);
    }
  }

  /**
   * Sets status filter and fetches articles.
   * Preserves currentAuthorId filter.
   */
  function filterByStatus(status) {
    currentStatus.value = status;
    currentTags.value = [];
    currentKeyword.value = "";
    // 保持 currentAuthorId 不变，不要清除
    fetchArticles(1);
  }

  /**
   * Toggle tag filter (add/remove tag from selection).
   */
  function toggleTag(tagName) {
    const index = currentTags.value.indexOf(tagName);
    if (index > -1) {
      // Tag already selected, remove it
      currentTags.value.splice(index, 1);
    } else {
      // Tag not selected, add it
      currentTags.value.push(tagName);
    }
    currentStatus.value = null;
    currentKeyword.value = "";
    fetchArticles(1);
  }

  /**
   * Check if a tag is currently selected.
   */
  function isTagSelected(tagName) {
    return currentTags.value.includes(tagName);
  }

  /**
   * Search articles by keyword.
   */
  function searchArticles(keyword) {
    currentKeyword.value = keyword;
    currentStatus.value = null;
    currentTags.value = [];
    fetchArticles(1);
  }

  /**
   * Sets author ID filter and fetches articles.
   */
  function filterByAuthor(authorId) {
    currentAuthorId.value = authorId;
    fetchArticles(1);
  }

  /**
   * Clears all filters and fetches articles.
   */
  function clearFilters() {
    currentStatus.value = null;
    currentTags.value = [];
    currentKeyword.value = "";
    currentAuthorId.value = null;
    fetchArticles(1);
  }

  return {
    articles,
    currentPage,
    pageSize,
    totalItems,
    loading,
    error,
    currentStatus,
    currentTags,
    currentKeyword,
    currentAuthorId,
    availableTags,
    fetchArticles,
    fetchTags,
    filterByStatus,
    filterByAuthor,
    toggleTag,
    isTagSelected,
    searchArticles,
    clearFilters,
  };
});
