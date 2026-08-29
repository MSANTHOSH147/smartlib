import api from "./api";

const bookService = {
  getAllBooks: async () => {
    const response = await api.get("/books");
    return response.data;
  },

  getBookById: async (id) => {
    const response = await api.get(`/books/${id}`);
    return response.data;
  },

  searchBooks: async (query) => {
    const response = await api.get("/books/search", {
      params: { query },
    });

    return response.data;
  },

  getAvailableBooks: async () => {
    const response = await api.get("/books/available");
    return response.data;
  },

  getBooksByAuthor: async (name) => {
    const response = await api.get("/books/author", {
      params: { name },
    });

    return response.data;
  },

  getBooksByCategory: async (categoryId) => {
    const response = await api.get(
      `/books/category/${categoryId}`
    );

    return response.data;
  },
};

export default bookService;
