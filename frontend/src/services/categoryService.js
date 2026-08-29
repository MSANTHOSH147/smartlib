import api from "./api";

const categoryService = {
  getAllCategories: async () => {
    const response = await api.get("/categories");
    return response.data;
  },
};

export default categoryService;
