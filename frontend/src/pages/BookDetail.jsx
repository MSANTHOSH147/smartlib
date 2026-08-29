import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  ArrowLeft,
  BookOpen,
  CalendarDays,
  CheckCircle2,
  ChevronRight,
  Loader2,
  Star,
  UserRound,
} from "lucide-react";

import bookService from "../services/bookService";
import transactionService from "../services/transactionService";
import reservationService from "../services/reservationService";
import reviewService from "../services/reviewService";

import "./BookDetail.css";

function BookDetail() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [book, setBook] = useState(null);
  const [reviews, setReviews] = useState([]);

  const [loading, setLoading] = useState(true);
  const [reviewsLoading, setReviewsLoading] = useState(true);

  const [actionLoading, setActionLoading] = useState(false);

  const [error, setError] = useState("");
  const [reviewError, setReviewError] = useState("");
  const [message, setMessage] = useState("");

  // ============================================================
  // LOAD BOOK
  // ============================================================

  async function loadBook() {
    try {
      setLoading(true);
      setError("");

      const bookData = await bookService.getBookById(id);

      setBook(bookData);

    } catch (err) {
      console.error("Book loading error:", err);

      setBook(null);

      setError(
        err.response?.data?.message ||
        "Unable to load this book."
      );
    } finally {
      setLoading(false);
    }
  }

  // ============================================================
  // LOAD REVIEWS
  // Reviews are independent from the book request.
  // A review failure must NOT make the book disappear.
  // ============================================================

  async function loadReviews() {
    try {
      setReviewsLoading(true);
      setReviewError("");

      const reviewData =
        await reviewService.getBookReviews(id);

      setReviews(
        Array.isArray(reviewData)
          ? reviewData
          : []
      );

    } catch (err) {
      console.error(
        "Review loading error:",
        err
      );

      setReviews([]);

      setReviewError(
        "Reviews are temporarily unavailable."
      );

    } finally {
      setReviewsLoading(false);
    }
  }

  // ============================================================
  // INITIAL LOAD
  // ============================================================

  useEffect(() => {
    loadBook();
    loadReviews();
  }, [id]);

  // ============================================================
  // BORROW
  // ============================================================

  async function handleBorrow() {
    try {
      setActionLoading(true);
      setMessage("");
      setError("");

      await transactionService.borrowBook(
        Number(id)
      );

      setMessage(
        "Book borrowed successfully."
      );

      // Refresh book availability
      await loadBook();

    } catch (err) {
      console.error(
        "Borrowing error:",
        err
      );

      setError(
        err.response?.data?.message ||
        "Unable to borrow this book."
      );

    } finally {
      setActionLoading(false);
    }
  }

  // ============================================================
  // RESERVE
  // ============================================================

  async function handleReserve() {
    try {
      setActionLoading(true);
      setMessage("");
      setError("");

      await reservationService.reserveBook(
        Number(id)
      );

      setMessage(
        "Book reserved successfully."
      );

    } catch (err) {
      console.error(
        "Reservation error:",
        err
      );

      setError(
        err.response?.data?.message ||
        "Unable to reserve this book."
      );

    } finally {
      setActionLoading(false);
    }
  }

  // ============================================================
  // LOADING
  // ============================================================

  if (loading) {
    return (
      <div className="book-detail-loading">

        <Loader2
          size={24}
          className="book-detail-spinner"
        />

        <span>
          Loading book details...
        </span>

      </div>
    );
  }

  // ============================================================
  // BOOK ERROR
  // ============================================================

  if (!book) {
    return (
      <div className="book-detail-error-page">

        <BookOpen size={36} />

        <h2>
          Book unavailable
        </h2>

        <p>
          {error ||
            "Unable to load this book."}
        </p>

        <button
          onClick={() =>
            navigate("/books")
          }
        >
          Back to Books
        </button>

      </div>
    );
  }

  // ============================================================
  // AVAILABILITY
  // ============================================================

  const availableCopies =
    Number(book.availableCopies || 0);

  const isAvailable =
    availableCopies > 0;

  // ============================================================
  // RENDER
  // ============================================================

  return (
    <div className="book-detail-page">

      {/* ======================================================
          BACK
      ====================================================== */}

      <Link
        to="/books"
        className="book-detail-back"
      >
        <ArrowLeft size={16} />
        Back to Books
      </Link>

      {/* ======================================================
          SUCCESS MESSAGE
      ====================================================== */}

      {message && (
        <div className="book-detail-success">

          <CheckCircle2 size={17} />

          {message}

        </div>
      )}

      {/* ======================================================
          BOOK ACTION ERROR
      ====================================================== */}

      {error && (
        <div className="book-detail-alert">
          {error}
        </div>
      )}

      {/* ======================================================
          MAIN BOOK
      ====================================================== */}

      <section className="book-detail-main">

        {/* COVER */}

        <div className="book-detail-cover">

          {book.coverImageUrl ? (
            <img
              src={book.coverImageUrl}
              alt={book.title}
            />
          ) : (
            <BookOpen size={60} />
          )}

        </div>

        {/* INFO */}

        <div className="book-detail-info">

          <div className="book-detail-category">
            {book.categoryName ||
              "General"}
          </div>

          <h1>
            {book.title}
          </h1>

          <p className="book-detail-author">
            by{" "}
            <strong>
              {book.author}
            </strong>
          </p>

          {/* RATING */}

          <div className="book-detail-rating">

            <Star
              size={17}
              fill="currentColor"
            />

            <strong>
              {book.averageRating
                ? Number(
                    book.averageRating
                  ).toFixed(1)
                : "No rating"}
            </strong>

            <span>
              {book.totalRatings || 0} reviews
            </span>

          </div>

          {/* DESCRIPTION */}

          <p className="book-detail-description">
            {book.description ||
              "No description is available for this book."}
          </p>

          {/* ACTIONS */}

          <div className="book-detail-actions">

            {isAvailable ? (

              <button
                className="book-action-primary"
                onClick={handleBorrow}
                disabled={actionLoading}
              >

                {actionLoading ? (
                  <>
                    <Loader2
                      size={17}
                      className="book-action-spinner"
                    />

                    Processing...
                  </>
                ) : (
                  <>
                    <BookOpen size={17} />

                    Borrow Book
                  </>
                )}

              </button>

            ) : (

              <button
                className="book-action-primary"
                onClick={handleReserve}
                disabled={actionLoading}
              >

                {actionLoading ? (
                  <>
                    <Loader2
                      size={17}
                      className="book-action-spinner"
                    />

                    Processing...
                  </>
                ) : (
                  <>
                    <CalendarDays size={17} />

                    Reserve Book
                  </>
                )}

              </button>

            )}

          </div>

          {/* AVAILABILITY */}

          <div className="book-detail-availability">

            <span
              className={
                isAvailable
                  ? "availability-dot available"
                  : "availability-dot unavailable"
              }
            />

            {isAvailable
              ? `${availableCopies} copies available`
              : "Currently unavailable"}

          </div>

        </div>

      </section>

      {/* ======================================================
          METADATA
      ====================================================== */}

      <section className="book-detail-meta">

        <div>
          <span>ISBN</span>

          <strong>
            {book.isbn ||
              "Not available"}
          </strong>
        </div>

        <div>
          <span>Publisher</span>

          <strong>
            {book.publisher ||
              "Not available"}
          </strong>
        </div>

        <div>
          <span>Published</span>

          <strong>
            {book.publicationYear ||
              "Not available"}
          </strong>
        </div>

        <div>
          <span>Total copies</span>

          <strong>
            {book.totalCopies ?? "—"}
          </strong>
        </div>

      </section>

      {/* ======================================================
          REVIEWS
      ====================================================== */}

      <section className="book-reviews">

        <div className="book-section-heading">

          <div>

            <h2>
              Reader Reviews
            </h2>

            <p>
              What other SmartLib members think.
            </p>

          </div>

          <span>

            {reviews.length}{" "}

            {reviews.length === 1
              ? "review"
              : "reviews"}

          </span>

        </div>

        {/* REVIEW ERROR */}

        {reviewError ? (

          <div className="reviews-empty">

            <Star size={28} />

            <h3>
              Reviews unavailable
            </h3>

            <p>
              {reviewError}
            </p>

            <button
              onClick={loadReviews}
              disabled={reviewsLoading}
            >
              {reviewsLoading
                ? "Loading..."
                : "Retry"}
            </button>

          </div>

        ) : reviewsLoading ? (

          <div className="reviews-empty">

            <Loader2
              size={28}
              className="book-detail-spinner"
            />

            <h3>
              Loading reviews...
            </h3>

          </div>

        ) : reviews.length === 0 ? (

          <div className="reviews-empty">

            <Star size={28} />

            <h3>
              No reviews yet
            </h3>

            <p>
              Be the first member to review
              this book after borrowing it.
            </p>

          </div>

        ) : (

          <div className="reviews-list">

            {reviews.map((review) => (

              <article
                className="review-card"
                key={review.id}
              >

                <div className="review-avatar">

                  <UserRound size={17} />

                </div>

                <div className="review-body">

                  <div className="review-top">

                    <strong>
                      {review.userName}
                    </strong>

                    <span>
                      {review.createdAt
                        ? new Date(
                            review.createdAt
                          ).toLocaleDateString()
                        : ""}
                    </span>

                  </div>

                  <div className="review-stars">

                    {Array.from(
                      { length: 5 },
                      (_, index) => (

                        <Star
                          key={index}
                          size={14}
                          fill={
                            index <
                            Number(
                              review.rating || 0
                            )
                              ? "currentColor"
                              : "none"
                          }
                        />

                      )
                    )}

                  </div>

                  {review.comment && (
                    <p>
                      {review.comment}
                    </p>
                  )}

                </div>

                <ChevronRight
                  size={16}
                  className="review-arrow"
                />

              </article>

            ))}

          </div>

        )}

      </section>

    </div>
  );
}

export default BookDetail;