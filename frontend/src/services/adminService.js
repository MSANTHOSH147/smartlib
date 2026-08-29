import api from "./api";

const adminService = {
  getDashboard: async () => {
    const response = await api.get("/admin/dashboard");
    return response.data;
  },

  getLibraryOperations: async () => {
    const response = await api.get("/admin/library");
    return response.data;
  },

  getUsers: async () => {
    const response = await api.get("/admin/users");
    return response.data;
  },

  getBooks: async () => {
    const response = await api.get("/admin/books");
    return response.data;
  },

  createBook: async (book) => {
    const response = await api.post(
      "/admin/books",
      book
    );

    return response.data;
  },

  updateBook: async (id, book) => {
    const response = await api.put(
      `/admin/books/${id}`,
      book
    );

    return response.data;
  },

  deleteBook: async (id) => {
    const response = await api.delete(
      `/admin/books/${id}`
    );

    return response.data;
  },

  getBorrowings: async () => {
    const response = await api.get(
      "/admin/borrowings"
    );

    return response.data;
  },

  getActiveBorrowings: async () => {
    const response = await api.get(
      "/admin/borrowings/active"
    );

    return response.data;
  },

  getOverdueBorrowings: async () => {
    const response = await api.get(
      "/admin/borrowings/overdue"
    );

    return response.data;
  },

  getReservations: async () => {
    const response = await api.get(
      "/admin/reservations"
    );

    return response.data;
  },

  getWaitingReservations: async () => {
    const response = await api.get(
      "/admin/reservations/waiting"
    );

    return response.data;
  },

  getReadyReservations: async () => {
    const response = await api.get(
      "/admin/reservations/ready"
    );

    return response.data;
  },

  getFines: async () => {
    const response = await api.get(
      "/admin/fines"
    );

    return response.data;
  },

  getUnpaidFines: async () => {
    const response = await api.get(
      "/admin/fines/unpaid"
    );

    return response.data;
  },

  payFine: async (id) => {
    const response = await api.put(
      `/admin/fines/${id}/pay`
    );

    return response.data;
  },

  updateReservationStatus: async (
    id,
    status
  ) => {
    const response = await api.put(
      `/admin/reservations/${id}/status`,
      null,
      {
        params: { status },
      }
    );

    return response.data;
  },

  updateUserStatus: async (
    id,
    active
  ) => {
    const response = await api.put(
      `/admin/users/${id}/status`,
      null,
      {
        params: { active },
      }
    );

    return response.data;
  },

  updateUserRole: async (
    id,
    role
  ) => {
    const response = await api.put(
      `/admin/users/${id}/role`,
      null,
      {
        params: { role },
      }
    );

    return response.data;
  },

  deleteUser: async (id) => {
    const response = await api.delete(
      `/admin/users/${id}`
    );

    return response.data;
  },
};

export default adminService;
