import api from "./api";

const analyticsService = {
  getDashboard: async () => {
    const response = await api.get(
      "/analytics/dashboard"
    );

    return response.data;
  },
};

export default analyticsService;
