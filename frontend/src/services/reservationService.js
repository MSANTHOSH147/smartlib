import api from "./api";

const reservationService = {
  getMyReservations: async () => {
    const response = await api.get("/reservations/my");
    return response.data;
  },

  getMyActiveReservations: async () => {
    const response = await api.get(
      "/reservations/my/active"
    );

    return response.data;
  },

  reserveBook: async (bookId) => {
    const response = await api.post(
      "/reservations",
      { bookId }
    );

    return response.data;
  },

  cancelReservation: async (reservationId) => {
    const response = await api.post(
      `/reservations/${reservationId}/cancel`
    );

    return response.data;
  },
};

export default reservationService;
