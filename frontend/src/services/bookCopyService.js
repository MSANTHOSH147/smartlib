import api from "./api";

const bookCopyService = {

  // ============================================================
  // GET COPY BY QR
  // GET /api/book-copies/qr/{qrToken}
  // ============================================================

  getByQrToken: async (qrToken) => {
    const response = await api.get(
      `/book-copies/qr/${encodeURIComponent(qrToken)}`
    );

    return response.data;
  },


  // ============================================================
  // GET ALL COPIES FOR A BOOK
  // GET /api/book-copies/book/{bookId}
  // ============================================================

  getByBookId: async (bookId) => {
    const response = await api.get(
      `/book-copies/book/${bookId}`
    );

    return response.data;
  },


  // ============================================================
  // MEMBER BORROW BY QR
  //
  // BACKEND:
  // POST /api/borrowings/qr
  //
  // BODY:
  // {
  //   "qrToken": "..."
  // }
  // ============================================================

  borrowByQr: async (qrToken) => {

    if (!qrToken || !qrToken.trim()) {
      throw new Error("QR token is required.");
    }

    console.log(
      "BOOK COPY SERVICE - BORROW QR:",
      qrToken
    );

    const response = await api.post(
      "/borrowings/qr",
      {
        qrToken: qrToken.trim(),
      }
    );

    console.log(
      "BOOK COPY SERVICE - BORROW RESPONSE:",
      response.data
    );

    return response.data;
  },


  // ============================================================
  // MEMBER RETURN BY QR
  //
  // BACKEND:
  // POST /api/borrowings/qr/return
  //
  // BODY:
  // {
  //   "qrToken": "..."
  // }
  // ============================================================

  returnByQr: async (qrToken) => {

    if (!qrToken || !qrToken.trim()) {
      throw new Error("QR token is required.");
    }

    console.log(
      "BOOK COPY SERVICE - RETURN QR:",
      qrToken
    );

    const response = await api.post(
      "/borrowings/qr/return",
      {
        qrToken: qrToken.trim(),
      }
    );

    console.log(
      "BOOK COPY SERVICE - RETURN RESPONSE:",
      response.data
    );

    return response.data;
  },


  // ============================================================
  // ADMIN QR LOOKUP
  //
  // GET /api/admin/book-copies/qr/{qrToken}
  // ============================================================

  adminGetByQrToken: async (qrToken) => {

    if (!qrToken || !qrToken.trim()) {
      throw new Error("QR token is required.");
    }

    const response = await api.get(
      `/admin/book-copies/qr/${encodeURIComponent(
        qrToken.trim()
      )}`
    );

    return response.data;
  },


  // ============================================================
  // ADMIN RETURN BY QR
  //
  // POST /api/admin/book-copies/qr/{qrToken}/return
  // ============================================================

  adminReturnByQr: async (qrToken) => {

    if (!qrToken || !qrToken.trim()) {
      throw new Error("QR token is required.");
    }

    console.log(
      "BOOK COPY SERVICE - ADMIN RETURN QR:",
      qrToken
    );

    const response = await api.post(
      `/admin/book-copies/qr/${encodeURIComponent(
        qrToken.trim()
      )}/return`
    );

    console.log(
      "BOOK COPY SERVICE - ADMIN RETURN RESPONSE:",
      response.data
    );

    return response.data;
  },

    // ============================================================
  // ADMIN DEACTIVATE COPY
  //
  // DELETE /api/admin/book-copies/{id}
  // ============================================================

  deactivateCopy: async (copyId) => {

    if (!copyId) {
      throw new Error("Book copy ID is required.");
    }

    console.log(
      "BOOK COPY SERVICE - DEACTIVATE:",
      copyId
    );

    const response = await api.delete(
      `/admin/book-copies/${copyId}`
    );

    return response.data;
  },
  // ============================================================
// ADMIN UPDATE PHYSICAL COPY
//
// PUT /api/admin/book-copies/{id}
// ============================================================

updateCopy: async (copyId, data) => {

  if (!copyId) {
    throw new Error(
      "Book copy ID is required."
    );
  }

  console.log(
    "BOOK COPY SERVICE - UPDATE COPY:",
    copyId,
    data
  );

  const response = await api.put(
    `/admin/book-copies/${copyId}`,
    {
      condition: data.condition,
      location: data.location,
    }
  );

  console.log(
    "BOOK COPY SERVICE - UPDATE RESPONSE:",
    response.data
  );

  return response.data;
},

};

export default bookCopyService;