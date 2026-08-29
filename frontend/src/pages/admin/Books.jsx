import { useEffect, useMemo, useState } from "react";

import {
  ArrowLeft,
  BookOpen,
  Check,
  Edit3,
  Plus,
  Search,
  Star,
  Trash2,
  X,
  QrCode,
} from "lucide-react";

import {
  Link,
  useNavigate,
} from "react-router-dom";

import BookCopyManager from "../../components/BookCopyManager";

import adminService from "../../services/adminService";
import categoryService from "../../services/categoryService";

import "./Books.css";


const EMPTY_FORM = {
  title: "",
  author: "",
  isbn: "",
  publisher: "",
  publicationYear: "",
  description: "",
  coverImageUrl: "",
  totalCopies: "",
  categoryId: "",
};


function Books() {

  // ============================================================
  // NAVIGATION
  // ============================================================

  const navigate = useNavigate();


  // ============================================================
  // BOOK DATA
  // ============================================================

  const [books, setBooks] = useState([]);

  const [categories, setCategories] =
    useState([]);

  const [loading, setLoading] =
    useState(true);

  const [categoriesLoading, setCategoriesLoading] =
    useState(true);

  const [error, setError] =
    useState("");

  const [categoryError, setCategoryError] =
    useState("");


  // ============================================================
  // SEARCH / FILTER
  // ============================================================

  const [query, setQuery] =
    useState("");

  const [availabilityFilter, setAvailabilityFilter] =
    useState("ALL");


  // ============================================================
  // ADD / EDIT BOOK
  // ============================================================

  const [showModal, setShowModal] =
    useState(false);

  const [editingBook, setEditingBook] =
    useState(null);

  const [form, setForm] =
    useState(EMPTY_FORM);

  const [saving, setSaving] =
    useState(false);


  // ============================================================
  // DELETE BOOK
  // ============================================================

  const [deleteBook, setDeleteBook] =
    useState(null);

  const [deleting, setDeleting] =
    useState(false);


  // ============================================================
  // BOOK COPY / QR MANAGER
  // ============================================================

  const [selectedBookCopies, setSelectedBookCopies] =
    useState(null);


  // ============================================================
  // INITIAL LOAD
  // ============================================================

  useEffect(() => {

    loadBooks();

    loadCategories();

  }, []);


  // ============================================================
  // LOAD BOOKS
  // ============================================================

  async function loadBooks() {

    try {

      setLoading(true);

      setError("");

      const data =
        await adminService.getBooks();

      setBooks(
        Array.isArray(data)
          ? data
          : []
      );

    } catch (err) {

      console.error(
        "Admin books loading error:",
        err
      );

      setError(
        err.response?.data?.message ||
        "Unable to load books."
      );

    } finally {

      setLoading(false);

    }
  }


  // ============================================================
  // LOAD CATEGORIES
  // ============================================================

  async function loadCategories() {

    try {

      setCategoriesLoading(true);

      setCategoryError("");

      const data =
        await categoryService.getAllCategories();

      setCategories(
        Array.isArray(data)
          ? data
          : []
      );

    } catch (err) {

      console.error(
        "Categories loading error:",
        err
      );

      setCategoryError(
        err.response?.data?.message ||
        "Unable to load categories."
      );

      setCategories([]);

    } finally {

      setCategoriesLoading(false);

    }
  }


  // ============================================================
  // OPEN CREATE MODAL
  // ============================================================

  function openCreateModal() {

    setEditingBook(null);

    setForm({
      ...EMPTY_FORM,
    });

    setShowModal(true);
  }


  // ============================================================
  // OPEN EDIT MODAL
  // ============================================================

  function openEditModal(book) {

    setEditingBook(book);

    setForm({

      title:
        book.title || "",

      author:
        book.author || "",

      isbn:
        book.isbn || "",

      publisher:
        book.publisher || "",

      publicationYear:
        book.publicationYear || "",

      description:
        book.description || "",

      coverImageUrl:
        book.coverImageUrl || "",

      totalCopies:
        book.totalCopies || "",

      categoryId:
        book.categoryId || "",

    });

    setShowModal(true);
  }


  // ============================================================
  // CLOSE BOOK MODAL
  // ============================================================

  function closeModal() {

    if (saving) return;

    setShowModal(false);

    setEditingBook(null);

    setForm({
      ...EMPTY_FORM,
    });
  }


  // ============================================================
  // FORM CHANGE
  // ============================================================

  function handleChange(event) {

    const {
      name,
      value,
    } = event.target;

    setForm((current) => ({

      ...current,

      [name]: value,

    }));
  }


  // ============================================================
  // SAVE BOOK
  // ============================================================

  async function handleSubmit(event) {

    event.preventDefault();

    if (!form.categoryId) {

      setError(
        "Please select a category."
      );

      return;
    }

    try {

      setSaving(true);

      setError("");

      const payload = {

        title:
          form.title.trim(),

        author:
          form.author.trim(),

        isbn:
          form.isbn.trim(),

        publisher:
          form.publisher.trim(),

        publicationYear:
          form.publicationYear
            ? Number(
                form.publicationYear
              )
            : null,

        description:
          form.description.trim(),

        coverImageUrl:
          form.coverImageUrl.trim(),

        totalCopies:
          Number(
            form.totalCopies
          ),

        categoryId:
          Number(
            form.categoryId
          ),

      };


      if (editingBook) {

        await adminService.updateBook(
          editingBook.id,
          payload
        );

      } else {

        await adminService.createBook(
          payload
        );

      }


      closeModal();

      await loadBooks();

    } catch (err) {

      console.error(
        "Book save error:",
        err
      );

      setError(
        err.response?.data?.message ||
        "Unable to save book."
      );

    } finally {

      setSaving(false);

    }
  }


  // ============================================================
  // DELETE
  // ============================================================

  function askDelete(book) {

    setDeleteBook(book);

  }


  function closeDeleteModal() {

    if (deleting) return;

    setDeleteBook(null);

  }


  async function confirmDelete() {

    if (!deleteBook) return;

    try {

      setDeleting(true);

      setError("");

      await adminService.deleteBook(
        deleteBook.id
      );

      setDeleteBook(null);

      await loadBooks();

    } catch (err) {

      console.error(
        "Book delete error:",
        err
      );

      setError(
        err.response?.data?.message ||
        "Unable to delete book."
      );

    } finally {

      setDeleting(false);

    }
  }


  // ============================================================
  // FILTER BOOKS
  // ============================================================

  const filteredBooks =
    useMemo(() => {

      const normalizedQuery =
        query
          .trim()
          .toLowerCase();

      return books.filter(
        (book) => {

          const matchesSearch =
            !normalizedQuery ||

            String(
              book.title || ""
            )
              .toLowerCase()
              .includes(
                normalizedQuery
              ) ||

            String(
              book.author || ""
            )
              .toLowerCase()
              .includes(
                normalizedQuery
              ) ||

            String(
              book.isbn || ""
            )
              .toLowerCase()
              .includes(
                normalizedQuery
              );


          const available =
            Number(
              book.availableCopies || 0
            ) > 0;


          const matchesAvailability =

            availabilityFilter ===
              "ALL" ||

            (
              availabilityFilter ===
                "AVAILABLE" &&
              available
            ) ||

            (
              availabilityFilter ===
                "UNAVAILABLE" &&
              !available
            );


          return (
            matchesSearch &&
            matchesAvailability
          );

        }
      );

    }, [
      books,
      query,
      availabilityFilter,
    ]);


  // ============================================================
  // STATISTICS
  // ============================================================

  const totalBooks =
    books.length;


  const availableBooks =
    books.filter(
      (book) =>
        Number(
          book.availableCopies || 0
        ) > 0
    ).length;


  const unavailableBooks =
    books.filter(
      (book) =>
        Number(
          book.availableCopies || 0
        ) <= 0
    ).length;


  const totalCopies =
    books.reduce(
      (total, book) =>
        total +
        Number(
          book.totalCopies || 0
        ),
      0
    );


  // ============================================================
  // RENDER
  // ============================================================

  return (

    <div className="admin-books-page">


      {/* ======================================================
          HEADER
      ====================================================== */}

      <div className="admin-books-header">

        <div className="admin-books-topline">

          <Link
            to="/admin/dashboard"
            className="admin-back-link"
          >

            <ArrowLeft size={16} />

            Dashboard

          </Link>


          <span className="admin-section-label">

            SMARTLIB / MANAGEMENT

          </span>

        </div>


        <section className="admin-books-hero">

          <div>

            <p className="eyebrow">
              LIBRARY MANAGEMENT
            </p>


            <h1>
              Books<span>.</span>
            </h1>


            <p className="admin-books-subtitle">

              Manage your library catalogue,
              monitor inventory, and keep the
              SmartLib collection organized.

            </p>

          </div>


          {/* ==================================================
              HEADER ACTIONS
          ================================================== */}

          <div className="admin-books-header-actions">

            {/* ================================================
                SCAN PHYSICAL BOOK
            ================================================= */}

            <button
              type="button"
              className="admin-qr-button"
              onClick={() =>
                navigate("/admin/qr-scanner")
              }
            >

              <QrCode size={17} />

              Scan Physical Book

            </button>


            {/* ================================================
                ADD BOOK
            ================================================= */}

            <button
              type="button"
              className="add-book-button"
              onClick={
                openCreateModal
              }
            >

              <Plus size={17} />

              Add Book

            </button>

          </div>

        </section>

      </div>


      {/* ======================================================
          MAIN
      ====================================================== */}

      <main className="admin-books-content">


        {/* ====================================================
            ERROR
        ==================================================== */}

        {error && (

          <div className="admin-books-error">

            <span>
              {error}
            </span>


            <button
              type="button"
              onClick={loadBooks}
            >
              Retry
            </button>

          </div>

        )}


        {/* ====================================================
            STATISTICS
        ==================================================== */}

        <section className="book-stats">


          <div className="book-stat-card">

            <div>

              <span>
                COLLECTION
              </span>

              <strong>
                {totalBooks}
              </strong>

              <small>
                books in the library
              </small>

            </div>

            <BookOpen size={22} />

          </div>


          <div className="book-stat-card">

            <div>

              <span>
                AVAILABLE
              </span>

              <strong>
                {availableBooks}
              </strong>

              <small>
                currently available
              </small>

            </div>

            <Check size={22} />

          </div>


          <div className="book-stat-card">

            <div>

              <span>
                UNAVAILABLE
              </span>

              <strong>
                {unavailableBooks}
              </strong>

              <small>
                currently unavailable
              </small>

            </div>

            <BookOpen size={22} />

          </div>


          <div className="book-stat-card results-card">

            <div>

              <span>
                TOTAL COPIES
              </span>

              <strong>
                {totalCopies}
              </strong>

              <small>
                physical copies
              </small>

            </div>

            <BookOpen size={22} />

          </div>

        </section>


        {/* ====================================================
            TOOLBAR
        ==================================================== */}

        <div className="books-toolbar">


          <div className="books-search">

            <Search size={18} />


            <input
              type="text"
              value={query}
              onChange={(event) =>
                setQuery(
                  event.target.value
                )
              }
              placeholder="Search title, author or ISBN..."
            />


            {query && (

              <button
                type="button"
                className="clear-search"
                onClick={() =>
                  setQuery("")
                }
                aria-label="Clear search"
              >

                <X size={16} />

              </button>

            )}

          </div>


          <div className="books-filters">

            <select
              value={
                availabilityFilter
              }
              onChange={(event) =>
                setAvailabilityFilter(
                  event.target.value
                )
              }
              className="books-filter-select"
            >

              <option value="ALL">
                All availability
              </option>

              <option value="AVAILABLE">
                Available
              </option>

              <option value="UNAVAILABLE">
                Unavailable
              </option>

            </select>


            {(query ||
              availabilityFilter !==
                "ALL") && (

              <button
                type="button"
                className="clear-filters"
                onClick={() => {

                  setQuery("");

                  setAvailabilityFilter(
                    "ALL"
                  );

                }}
              >
                Clear filters
              </button>

            )}

          </div>

        </div>


        {/* ====================================================
            RESULTS HEADER
        ==================================================== */}

        <div className="books-results-heading">

          <div>

            <p className="eyebrow">
              COLLECTION
            </p>

            <h2>

              {filteredBooks.length}{" "}

              {
                filteredBooks.length ===
                1
                  ? "book"
                  : "books"
              }

            </h2>

          </div>


          <span>

            Showing{" "}
            {filteredBooks.length}{" "}
            of{" "}
            {books.length}

          </span>

        </div>


        {categoryError && (

          <div className="category-warning">

            {categoryError}

          </div>

        )}


        {/* ====================================================
            LOADING
        ==================================================== */}

        {loading ? (

          <div className="admin-books-loading">

            <div className="loading-spinner" />

            <p>
              Loading collection...
            </p>

          </div>


        ) : filteredBooks.length === 0 ? (

          <div className="admin-books-empty">

            <BookOpen size={42} />


            <h2>
              No books found
            </h2>


            <p>
              Try changing your search
              or availability filter.
            </p>


            <button
              type="button"
              className="empty-add-button"
              onClick={
                openCreateModal
              }
            >

              <Plus size={16} />

              Add Book

            </button>

          </div>


        ) : (

          <div className="admin-book-grid">

            {filteredBooks.map(
              (book) => {

                const isAvailable =
                  Number(
                    book.availableCopies ||
                    0
                  ) > 0;


                return (

                  <article
                    key={book.id}
                    className="admin-book-card"
                  >


                    {/* ========================================
                        COVER
                    ======================================== */}

                    <div className="admin-book-cover">

                      {book.coverImageUrl ? (

                        <img
                          src={
                            book.coverImageUrl
                          }
                          alt={
                            book.title
                          }
                        />

                      ) : (

                        <div className="cover-placeholder">

                          <BookOpen
                            size={42}
                          />

                        </div>

                      )}


                      <span
                        className={`availability ${
                          isAvailable
                            ? "available"
                            : "unavailable"
                        }`}
                      >

                        {isAvailable
                          ? `${book.availableCopies} available`
                          : "Unavailable"}

                      </span>

                    </div>


                    {/* ========================================
                        BOOK INFORMATION
                    ======================================== */}

                    <div className="admin-book-info">


                      <div className="book-category">

                        {(
                          book.categoryName ||
                          "GENERAL"
                        ).toUpperCase()}

                      </div>


                      <h2>
                        {book.title}
                      </h2>


                      <p className="book-author">
                        {book.author}
                      </p>


                      {/* ======================================
                          RATING
                      ====================================== */}

                      <div className="book-rating">

                        <Star
                          size={14}
                          fill="currentColor"
                        />


                        <span>

                          {book.averageRating
                            ? Number(
                                book.averageRating
                              ).toFixed(1)
                            : "No rating"}

                        </span>


                        {book.totalRatings >
                          0 && (

                          <small>

                            (
                            {
                              book.totalRatings
                            }
                            )

                          </small>

                        )}

                      </div>


                      {/* ======================================
                          DETAILS
                      ====================================== */}

                      <div className="book-details">


                        <div>

                          <span>
                            ISBN
                          </span>

                          <strong>
                            {book.isbn || "—"}
                          </strong>

                        </div>


                        <div>

                          <span>
                            YEAR
                          </span>

                          <strong>
                            {book.publicationYear ||
                              "—"}
                          </strong>

                        </div>


                        <div>

                          <span>
                            COPIES
                          </span>

                          <strong>

                            {book.availableCopies ||
                              0}

                            /

                            {book.totalCopies ||
                              0}

                          </strong>

                        </div>

                      </div>


                      {/* ======================================
                          ACTIONS
                      ====================================== */}

                      <div className="book-card-actions">


                        {/* ==================================
                            QR / COPIES
                        ================================== */}

                        <button
                          type="button"
                          className="manage-copies-button"
                          onClick={() =>
                            setSelectedBookCopies(
                              book
                            )
                          }
                        >

                          <QrCode
                            size={15}
                          />

                          Copies

                        </button>


                        {/* ==================================
                            EDIT
                        ================================== */}

                        <button
                          type="button"
                          className="edit-book-button"
                          onClick={() =>
                            openEditModal(
                              book
                            )
                          }
                        >

                          <Edit3
                            size={15}
                          />

                          Edit

                        </button>


                        {/* ==================================
                            DELETE
                        ================================== */}

                        <button
                          type="button"
                          className="delete-book-button"
                          onClick={() =>
                            askDelete(
                              book
                            )
                          }
                          aria-label={`Delete ${book.title}`}
                        >

                          <Trash2
                            size={17}
                          />

                        </button>

                      </div>

                    </div>

                  </article>

                );

              }
            )}

          </div>

        )}

      </main>


      {/* ======================================================
          ADD / EDIT MODAL
      ====================================================== */}

      {showModal && (

        <div
          className="book-modal-overlay"
          onMouseDown={(event) => {

            if (
              event.target ===
              event.currentTarget
            ) {

              closeModal();

            }

          }}
        >

          <div className="book-modal">


            <div className="modal-header">

              <div>

                <span>

                  {editingBook
                    ? "EDIT COLLECTION"
                    : "NEW COLLECTION ITEM"}

                </span>


                <h2>

                  {editingBook
                    ? "Edit book"
                    : "Add book"}

                </h2>

              </div>


              <button
                type="button"
                className="modal-close"
                onClick={
                  closeModal
                }
                disabled={saving}
                aria-label="Close"
              >

                <X size={18} />

              </button>

            </div>


            <form
              onSubmit={
                handleSubmit
              }
            >


              <div className="form-grid">


                <label>

                  <span>
                    TITLE *
                  </span>

                  <input
                    name="title"
                    value={
                      form.title
                    }
                    onChange={
                      handleChange
                    }
                    placeholder="Clean Code"
                    required
                  />

                </label>


                <label>

                  <span>
                    AUTHOR *
                  </span>

                  <input
                    name="author"
                    value={
                      form.author
                    }
                    onChange={
                      handleChange
                    }
                    placeholder="Robert C. Martin"
                    required
                  />

                </label>


                <label>

                  <span>
                    ISBN
                  </span>

                  <input
                    name="isbn"
                    value={
                      form.isbn
                    }
                    onChange={
                      handleChange
                    }
                    placeholder="9780132350884"
                  />

                </label>


                <label>

                  <span>
                    PUBLISHER
                  </span>

                  <input
                    name="publisher"
                    value={
                      form.publisher
                    }
                    onChange={
                      handleChange
                    }
                    placeholder="Prentice Hall"
                  />

                </label>


                <label>

                  <span>
                    PUBLICATION YEAR
                  </span>

                  <input
                    type="number"
                    name="publicationYear"
                    value={
                      form.publicationYear
                    }
                    onChange={
                      handleChange
                    }
                    placeholder="2008"
                  />

                </label>


                <label>

                  <span>
                    TOTAL COPIES *
                  </span>

                  <input
                    type="number"
                    min="1"
                    name="totalCopies"
                    value={
                      form.totalCopies
                    }
                    onChange={
                      handleChange
                    }
                    placeholder="5"
                    required
                  />

                </label>


                <label>

                  <span>
                    CATEGORY *
                  </span>

                  <select
                    name="categoryId"
                    value={
                      form.categoryId
                    }
                    onChange={
                      handleChange
                    }
                    required
                    disabled={
                      categoriesLoading
                    }
                  >

                    <option value="">

                      {categoriesLoading
                        ? "Loading categories..."
                        : "Select category"}

                    </option>


                    {categories.map(
                      (category) => (

                        <option
                          key={
                            category.id
                          }
                          value={
                            category.id
                          }
                        >

                          {
                            category.name
                          }

                        </option>

                      )
                    )}

                  </select>

                </label>


                <label>

                  <span>
                    COVER IMAGE URL
                  </span>

                  <input
                    type="url"
                    name="coverImageUrl"
                    value={
                      form.coverImageUrl
                    }
                    onChange={
                      handleChange
                    }
                    placeholder="https://..."
                  />

                </label>

              </div>


              <label className="description-field">

                <span>
                  DESCRIPTION
                </span>


                <textarea
                  name="description"
                  value={
                    form.description
                  }
                  onChange={
                    handleChange
                  }
                  rows="5"
                  placeholder="Enter a short description of the book..."
                />

              </label>


              <div className="modal-actions">


                <button
                  type="button"
                  className="cancel-modal-button"
                  onClick={
                    closeModal
                  }
                  disabled={saving}
                >

                  Cancel

                </button>


                <button
                  type="submit"
                  className="save-book-button"
                  disabled={saving}
                >

                  {saving
                    ? "Saving..."
                    : editingBook
                    ? "Save Changes"
                    : "Add Book"}

                </button>

              </div>

            </form>

          </div>

        </div>

      )}


      {/* ======================================================
          DELETE CONFIRMATION
      ====================================================== */}

      {deleteBook && (

        <div
          className="delete-modal-overlay"
          onMouseDown={(event) => {

            if (
              event.target ===
              event.currentTarget
            ) {

              closeDeleteModal();

            }

          }}
        >

          <div className="delete-modal">


            <div className="delete-icon">

              <Trash2 size={21} />

            </div>


            <span className="delete-eyebrow">

              REMOVE FROM COLLECTION

            </span>


            <h2>
              Delete this book?
            </h2>


            <p>

              <strong>
                {deleteBook.title}
              </strong>{" "}

              will be removed from the
              SmartLib collection.

            </p>


            <div className="delete-modal-actions">


              <button
                type="button"
                className="delete-cancel-button"
                onClick={
                  closeDeleteModal
                }
                disabled={
                  deleting
                }
              >

                Cancel

              </button>


              <button
                type="button"
                className="delete-confirm-button"
                onClick={
                  confirmDelete
                }
                disabled={
                  deleting
                }
              >

                {deleting
                  ? "Deleting..."
                  : "Delete Book"}

              </button>

            </div>

          </div>

        </div>

      )}


      {/* ======================================================
          BOOK COPY / QR MANAGER
      ====================================================== */}

      {selectedBookCopies && (

        <BookCopyManager
          book={
            selectedBookCopies
          }
          onClose={() =>
            setSelectedBookCopies(
              null
            )
          }
        />

      )}

    </div>

  );
}


export default Books;