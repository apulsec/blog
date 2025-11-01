import axios from "axios";

/**
 * Axios instance configured for API calls.
 * Base URL is set to /api which will be proxied by Vite to the backend service.
 */
const apiClient = axios.create({
  baseURL: "/api",
  headers: {
    "Content-Type": "application/json",
  },
});

// Attach Authorization header if token exists
apiClient.interceptors.request.use((config) => {
  try {
    const token = localStorage.getItem("access_token");
    if (token) {
      config.headers = config.headers || {};
      config.headers["Authorization"] = `Bearer ${token}`;
    }
  } catch (e) {
    // ignore
  }
  return config;
});

// Basic 401 handling
apiClient.interceptors.response.use(
  (resp) => resp,
  (error) => {
    if (error?.response?.status === 401) {
      // Token invalid/expired, clear and reload or redirect
      localStorage.removeItem("access_token");
      localStorage.removeItem("refresh_token");
      localStorage.removeItem("auth_user");
    }
    return Promise.reject(error);
  }
);

/**
 * API service for article-related endpoints.
 */
export const articleApi = {
  /**
   * Fetches a paginated list of articles.
   *
   * @param {number} page - Zero-based page number (default: 0)
   * @param {number} size - Number of items per page (default: 10)
   * @param {string} status - Optional status filter (PUBLISHED, DRAFT)
   * @param {string} tag - Optional tag name filter
   * @param {string} keyword - Optional keyword search (title or summary)
   * @param {number} authorId - Optional author ID filter
   * @returns {Promise} Promise resolving to axios response with article data
   */
  getArticles(
    page = 0,
    size = 10,
    status = null,
    tag = null,
    keyword = null,
    authorId = null
  ) {
    const params = { page, size };
    if (status) params.status = status;
    if (tag) params.tag = tag;
    if (keyword) params.keyword = keyword;
    if (authorId) params.authorId = authorId;

    return apiClient.get("/articles", { params });
  },

  /**
   * Creates a new article.
   *
   * @param {Object} articleData - Article data
   * @param {number} articleData.authorId - Author ID
   * @param {string} articleData.title - Article title
   * @param {string} articleData.summary - Article summary
   * @param {string} articleData.coverImageUrl - Cover image URL
   * @param {string} articleData.content - Article content (Markdown)
   * @param {string} articleData.status - Article status (DRAFT or PUBLISHED)
   * @returns {Promise} Promise resolving to axios response with created article
   */
  createArticle(articleData) {
    return apiClient.post("/articles", articleData);
  },

  /**
   * Fetches a single article by ID.
   *
   * @param {number} id - Article ID
   * @returns {Promise} Promise resolving to axios response with article details
   */
  getArticleById(id) {
    return apiClient.get(`/articles/${id}`);
  },

  /**
   * Updates an existing article.
   *
   * @param {number} id - Article ID
   * @param {Object} articleData - Updated article data
   * @returns {Promise} Promise resolving to axios response with updated article
   */
  updateArticle(id, articleData) {
    return apiClient.put(`/articles/${id}`, articleData);
  },

  /**
   * Deletes an article by ID.
   *
   * @param {number} id - Article ID
   * @returns {Promise} Promise resolving to axios response
   */
  deleteArticle(id) {
    return apiClient.delete(`/articles/${id}`);
  },

  /**
   * Fetches all available tags.
   *
   * @returns {Promise} Promise resolving to axios response with tags array
   */
  getAllTags() {
    return apiClient.get("/articles/tags");
  },

  /**
   * Likes the specified article for the current user.
   *
   * @param {number|string} articleId - Article ID
   * @returns {Promise}
   */
  likeArticle(articleId) {
    return apiClient.post(`/articles/${articleId}/likes`);
  },

  /**
   * Removes the like of the specified article for the current user.
   *
   * @param {number|string} articleId - Article ID
   * @returns {Promise}
   */
  unlikeArticle(articleId) {
    return apiClient.delete(`/articles/${articleId}/likes`);
  },

  /**
   * Fetches like status for the current user on the specified article.
   *
   * @param {number|string} articleId - Article ID
   * @returns {Promise}
   */
  getLikeStatus(articleId) {
    return apiClient.get(`/articles/${articleId}/likes/status`);
  },

  /**
   * Retrieves paginated comments for an article.
   *
   * @param {number|string} articleId - Article ID
   * @param {number} page - Zero-based page index
   * @param {number} size - Page size
   * @returns {Promise}
   */
  getComments(articleId, page = 0, size = 10) {
    return apiClient.get(`/articles/${articleId}/comments`, {
      params: { page, size },
    });
  },

  /**
   * Creates a new comment for an article.
   *
   * @param {number|string} articleId - Article ID
   * @param {string} content - Comment content
   * @param {number|string|null} parentId - Optional parent comment ID
   * @returns {Promise}
   */
  createComment(articleId, content, parentId = null) {
    const payload = { content };
    if (parentId !== null && parentId !== undefined) {
      payload.parentId = parentId;
    }
    return apiClient.post(`/articles/${articleId}/comments`, payload);
  },

  /**
   * Deletes an existing comment.
   *
   * @param {number|string} articleId - Article ID
   * @param {number|string} commentId - Comment ID
   * @returns {Promise}
   */
  deleteComment(articleId, commentId) {
    return apiClient.delete(`/articles/${articleId}/comments/${commentId}`);
  },
};
