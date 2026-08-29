import api from "./api";

const transactionService = {

  // ============================================================
  // MY BORROWINGS
  // ============================================================

  getMyBorrowings: async () => {

    const response =
      await api.get("/borrowings/my");

    return response.data;
  },


  // ============================================================
  // MY ACTIVE BORROWINGS
  // ============================================================

  getMyActiveBorrowings: async () => {

    const response =
      await api.get("/borrowings/my/active");

    return response.data;
  },


  // ============================================================
  // NORMAL BORROW
  // ============================================================

  borrowBook: async (bookId) => {

    const response =
      await api.post(
        "/borrowings",
        {
          bookId,
        }
      );

    return response.data;
  },


  // ============================================================
  // QR BORROW
  // ============================================================

  borrowBookByQr: async (qrToken) => {

    if (!qrToken) {
      throw new Error(
        "QR token is required."
      );
    }

    const response =
      await api.post(
        "/borrowings/qr",
        {
          qrToken: qrToken.trim(),
        }
      );

    return response.data;
  },


  // ============================================================
  // NORMAL RETURN
  // ============================================================

  returnBook: async (borrowingId) => {

    if (!borrowingId) {
      throw new Error(
        "Borrowing ID is required."
      );
    }

    const response =
      await api.post(
        `/borrowings/${borrowingId}/return`
      );

    return response.data;
  },


  // ============================================================
  // QR RETURN
  // ============================================================

  returnBookByQr: async (qrToken) => {

    if (!qrToken) {
      throw new Error(
        "QR token is required."
      );
    }

    const response =
      await api.post(
        "/borrowings/qr/return",
        {
          qrToken: qrToken.trim(),
        }
      );

    return response.data;
  },


  // ============================================================
  // FINES
  // ============================================================

  getMyFines: async () => {

    const response =
      await api.get("/fines/my");

    return response.data;
  },


  getMyUnpaidFines: async () => {

    const response =
      await api.get("/fines/my/unpaid");

    return response.data;
  },


  calculateFine: async (borrowingId) => {

    const response =
      await api.post(
        `/fines/borrowing/${borrowingId}/calculate`
      );

    return response.data;
  },


  payFine: async (fineId) => {

    const response =
      await api.post(
        `/fines/${fineId}/pay`
      );

    return response.data;
  },

};

export default transactionService;