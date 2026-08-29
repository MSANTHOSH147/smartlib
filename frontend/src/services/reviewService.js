import api from "./api";

const reviewService = {
  getBookReviews: async (bookId) => {
    const response = await api.get(
      `/reviews/book/${bookId}`
    );

    return response.data;
  },

  createReview: async (bookId, rating, comment) => {
    const response = await api.post(
      "/reviews",
      {
        bookId,
        rating,
        comment,
      }
    );

    return response.data;
  },

  updateReview: async (
    reviewId,
    rating,
    comment
  ) => {
    const response = await api.put(
      `/reviews/${reviewId}`,
      {
        rating,
        comment,
      }
    );

    return response.data;
  },

  deleteReview: async (reviewId) => {
    const response = await api.delete(
      `/reviews/${reviewId}`
    );

    return response.data;
  },
};

export default reviewService;
