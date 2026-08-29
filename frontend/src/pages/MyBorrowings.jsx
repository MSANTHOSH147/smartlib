import { useEffect, useState } from "react";
import {
  ArrowLeft,
  BookOpen,
  CalendarDays,
  CheckCircle2,
  Clock3,
  Loader2,
  RotateCcw,
} from "lucide-react";
import { Link } from "react-router-dom";

import transactionService from "../services/transactionService";

import "./MyBorrowings.css";

function MyBorrowings() {
  const [borrowings, setBorrowings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [returningId, setReturningId] = useState(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  async function loadBorrowings() {
    try {
      setLoading(true);
      setError("");

      const data =
        await transactionService.getMyBorrowings();

      setBorrowings(
        Array.isArray(data) ? data : []
      );
    } catch (err) {
      console.error("Borrowings error:", err);

      setError(
        err.response?.data?.message ||
        "Unable to load your borrowings."
      );
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadBorrowings();
  }, []);

  async function handleReturn(id) {
    try {
      setReturningId(id);
      setError("");
      setMessage("");

      await transactionService.returnBook(id);

      setMessage("Book returned successfully.");

      await loadBorrowings();
    } catch (err) {
      console.error("Return error:", err);

      setError(
        err.response?.data?.message ||
        "Unable to return this book."
      );
    } finally {
      setReturningId(null);
    }
  }

  function getStatusClass(status) {
    if (status === "RETURNED") {
      return "borrow-status returned";
    }

    if (status === "OVERDUE") {
      return "borrow-status overdue";
    }

    return "borrow-status borrowed";
  }

  function formatDate(value) {
    if (!value) return "—";

    return new Date(value).toLocaleDateString(
      "en-IN",
      {
        day: "2-digit",
        month: "short",
        year: "numeric",
      }
    );
  }

  const activeBorrowings =
    borrowings.filter(
      (item) =>
        item.status === "BORROWED" ||
        item.status === "OVERDUE"
    );

  const returnedBorrowings =
    borrowings.filter(
      (item) => item.status === "RETURNED"
    );

  if (loading) {
    return (
      <div className="borrowings-loading">
        <Loader2 size={25} />
        <span>Loading your borrowings...</span>
      </div>
    );
  }

  return (
    <div className="borrowings-page">

      <Link
        to="/dashboard"
        className="borrowings-back"
      >
        <ArrowLeft size={16} />
        Dashboard
      </Link>

      <header className="borrowings-header">

        <div>
          <div className="borrowings-eyebrow">
            MY LIBRARY
          </div>

          <h1>My Borrowings</h1>

          <p>
            Keep track of books you've borrowed
            and their return dates.
          </p>
        </div>

        <Link
          to="/books"
          className="borrowings-browse"
        >
          <BookOpen size={17} />
          Browse Books
        </Link>

      </header>

      {message && (
        <div className="borrowings-success">
          <CheckCircle2 size={17} />
          {message}
        </div>
      )}

      {error && (
        <div className="borrowings-error">
          {error}
        </div>
      )}

      <section className="borrowings-summary">

        <div className="borrow-summary-card">
          <span>Currently Borrowed</span>
          <strong>{activeBorrowings.length}</strong>
        </div>

        <div className="borrow-summary-card">
          <span>Returned</span>
          <strong>{returnedBorrowings.length}</strong>
        </div>

        <div className="borrow-summary-card">
          <span>Overdue</span>
          <strong>
            {
              activeBorrowings.filter(
                (item) => item.status === "OVERDUE"
              ).length
            }
          </strong>
        </div>

      </section>

      <section className="borrowings-section">

        <div className="borrowings-section-heading">
          <div>
            <h2>Current Borrowings</h2>
            <p>
              Books currently with you.
            </p>
          </div>

          <span>
            {activeBorrowings.length} books
          </span>
        </div>

        {activeBorrowings.length === 0 ? (

          <div className="borrowings-empty">

            <BookOpen size={32} />

            <h3>No books borrowed</h3>

            <p>
              Find something interesting from the
              SmartLib collection.
            </p>

            <Link to="/books">
              Browse Books
            </Link>

          </div>

        ) : (

          <div className="borrowing-list">

            {activeBorrowings.map((item) => (

              <article
                className="borrowing-card"
                key={item.id}
              >

                <div className="borrowing-icon">
                  <BookOpen size={21} />
                </div>

                <div className="borrowing-info">

                  <h3>
                    {item.bookTitle}
                  </h3>

                  <div className="borrowing-user">
                    Borrowed by {item.userName}
                  </div>

                  <div className="borrowing-dates">

                    <span>
                      <CalendarDays size={14} />
                      Borrowed{" "}
                      {formatDate(item.borrowDate)}
                    </span>

                    <span>
                      <Clock3 size={14} />
                      Due{" "}
                      {formatDate(item.dueDate)}
                    </span>

                  </div>

                </div>

                <div className="borrowing-actions">

                  <span
                    className={getStatusClass(
                      item.status
                    )}
                  >
                    {item.status}
                  </span>

                  <button
                    onClick={() =>
                      handleReturn(item.id)
                    }
                    disabled={
                      returningId === item.id
                    }
                  >
                    {returningId === item.id ? (
                      <>
                        <Loader2
                          size={15}
                          className="return-spinner"
                        />
                        Returning...
                      </>
                    ) : (
                      <>
                        <RotateCcw size={15} />
                        Return Book
                      </>
                    )}
                  </button>

                </div>

              </article>

            ))}

          </div>

        )}

      </section>

      {returnedBorrowings.length > 0 && (
        <section className="borrowings-section returned-section">

          <div className="borrowings-section-heading">
            <div>
              <h2>Borrowing History</h2>
              <p>
                Books you've already returned.
              </p>
            </div>

            <span>
              {returnedBorrowings.length} books
            </span>
          </div>

          <div className="borrowing-list">

            {returnedBorrowings.map((item) => (

              <article
                className="borrowing-card returned-card"
                key={item.id}
              >

                <div className="borrowing-icon">
                  <CheckCircle2 size={20} />
                </div>

                <div className="borrowing-info">

                  <h3>
                    {item.bookTitle}
                  </h3>

                  <div className="borrowing-dates">

                    <span>
                      Borrowed{" "}
                      {formatDate(item.borrowDate)}
                    </span>

                    <span>
                      Returned{" "}
                      {formatDate(item.returnDate)}
                    </span>

                  </div>

                </div>

                <span className="borrow-status returned">
                  RETURNED
                </span>

              </article>

            ))}

          </div>

        </section>
      )}

    </div>
  );
}

export default MyBorrowings;
