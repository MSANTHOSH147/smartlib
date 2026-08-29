import api from "./api";

const recommendationService = {
  getRecommendations: async () => {
    const response = await api.get(
      "/recommendations"
    );

    return response.data;
  },
};

export default recommendationService;
