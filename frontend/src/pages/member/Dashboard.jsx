import { useEffect, useState } from "react";
import {
  ArrowRight,
  BookOpen,
  CalendarDays,
  CheckCircle2,
  Clock3,
  CircleDollarSign,
  Loader2,
  QrCode,
  Search,
} from "lucide-react";
import { Link } from "react-router-dom";

import { getStoredUser } from "../../services/authService";
import transactionService from "../../services/transactionService";
import reservationService from "../../services/reservationService";
import bookService from "../../services/bookService";

import "./Dashboard.css";

function Dashboard() {
  const user = getStoredUser();

  const [borrowings, setBorrowings] = useState([]);
  const [reservations, setReservations] = useState([]);
  const [fines, setFines] = useState([]);
  const [books, setBooks] = useState([]);

  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadDashboard() {
      try {
        const [
          borrowingData,
          reservationData,
          fineData,
          bookData,
        ] = await Promise.allSettled([
          transactionService.getMyBorrowings(),
          reservationService.getMyReservations(),
          transactionService.getMyFines(),
          bookService.getAllBooks(),
        ]);

        setBorrowings(
          borrowingData.status === "fulfilled" &&
          Array.isArray(borrowingData.value)
            ? borrowingData.value
            : []
        );

        setReservations(
          reservationData.status === "fulfilled" &&
          Array.isArray(reservationData.value)
            ? reservationData.value
            : []
        );

        setFines(
          fineData.status === "fulfilled" &&
          Array.isArray(fineData.value)
            ? fineData.value
            : []
        );

        setBooks(
          bookData.status === "fulfilled" &&
          Array.isArray(bookData.value)
            ? bookData.value
            : []
        );
      } catch (error) {
        console.error("Dashboard loading error:", error);
      } finally {
        setLoading(false);
      }
    }

    loadDashboard();
  }, []);

  const activeBorrowings = borrowings.filter(
    (item) =>
      item.status === "BORROWED" ||
      item.status === "OVERDUE"
  );

  const overdueBorrowings = borrowings.filter(
    (item) => item.status === "OVERDUE"
  );

  const activeReservations = reservations.filter(
    (item) =>
      item.status === "WAITING" ||
      item.status === "READY"
  );

  const unpaidFines = fines.filter(
    (item) => item.status === "UNPAID"
  );

  const outstandingFine = unpaidFines.reduce(
    (total, fine) =>
      total + Number(fine.amount || 0),
    0
  );

  const dueSoon = activeBorrowings.filter((item) => {
    if (!item.dueDate) return false;

    const today = new Date();
    const due = new Date(item.dueDate);

    const difference =
      (due - today) /
      (1000 * 60 * 60 * 24);

    return difference >= 0 && difference <= 7;
  });

  const recommendedBooks = books
    .filter((book) => book.availableCopies > 0)
    .slice(0, 4);

  const currentBooks = activeBorrowings.slice(0, 2);

  function formatDate(value) {
    if (!value) return "—";

    return new Date(value).toLocaleDateString(
      "en-IN",
      {
        day: "2-digit",
        month: "short",
      }
    );
  }

  function greeting() {
    const hour = new Date().getHours();

    if (hour < 12) return "Good morning";
    if (hour < 17) return "Good afternoon";

    return "Good evening";
  }

  function getCover(book) {
    return (
      book?.coverImageUrl ||
      "https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&w=700&q=80"
    );
  }

  if (loading) {
    return (
      <div className="dashboard-loading">
        <Loader2 size={28} />
        <span>Loading your library...</span>
      </div>
    );
  }

  return (
    <div className="member-dashboard">

      {/* Header */}

      <div className="dashboard-heading">

  <div>
    <div className="dashboard-kicker">
      LIBRARY
    </div>

    <h1>
      {greeting()},{" "}
      <span>
        {user?.name?.split(" ")[0] || "Member"}
      </span>
    </h1>

    <p>
      Here's what's happening with your
      library account.
    </p>
  </div>

  <div
    className="dashboard-header-actions"
    style={{
      display: "flex",
      gap: "10px",
      alignItems: "center",
      flexWrap: "wrap",
    }}
  >

    {/* QR SCANNER */}

    <Link
      to="/qr-scanner"
      className="dashboard-primary-button"
    >
      <QrCode size={17} />
      Scan QR
    </Link>

    {/* BROWSE BOOKS */}

    <Link
      to="/books"
      className="dashboard-primary-button"
    >
      <Search size={17} />
      Browse Books
    </Link>

  </div>

</div>

      {/* Statistics */}

      <div className="dashboard-stats">

        <Link
          to="/borrowings"
          className="dashboard-stat-card stat-borrowed"
        >
          <div className="stat-top">
            <span>Currently Borrowed</span>
            <BookOpen size={18} />
          </div>

          <strong>
            {activeBorrowings.length}
          </strong>

          <small>
            {overdueBorrowings.length > 0
              ? `${overdueBorrowings.length} overdue`
              : "Books with you"}
          </small>
        </Link>

        <Link
          to="/borrowings"
          className="dashboard-stat-card stat-due"
        >
          <div className="stat-top">
            <span>Due Soon</span>
            <Clock3 size={18} />
          </div>

          <strong>{dueSoon.length}</strong>

          <small>
            Within the next 7 days
          </small>
        </Link>

        <Link
          to="/reservations"
          className="dashboard-stat-card stat-reservation"
        >
          <div className="stat-top">
            <span>Reservations</span>
            <CalendarDays size={18} />
          </div>

          <strong>
            {activeReservations.length}
          </strong>

          <small>
            Active reservations
          </small>
        </Link>

        <Link
          to="/fines"
          className={`dashboard-stat-card ${
            outstandingFine > 0
              ? "stat-fine has-fine"
              : "stat-fine"
          }`}
        >
          <div className="stat-top">
            <span>Outstanding Fine</span>
            <CircleDollarSign size={18} />
          </div>

          <strong>
            ₹{outstandingFine.toLocaleString("en-IN")}
          </strong>

          <small>
            {outstandingFine > 0
              ? "Payment required"
              : "All clear"}
          </small>
        </Link>

      </div>

      {/* Main content */}

      <div className="dashboard-content-grid">

        {/* Continue Reading */}

        <section className="dashboard-panel continue-panel">

          <div className="dashboard-panel-heading">

            <div>
              <h2>Continue Reading</h2>
              <p>
                Your currently borrowed books
              </p>
            </div>

            <Link to="/borrowings">
              View all
              <ArrowRight size={14} />
            </Link>

          </div>

          {currentBooks.length === 0 ? (

            <div className="dashboard-empty">

              <div className="empty-icon">
                <BookOpen size={23} />
              </div>

              <h3>
                Nothing borrowed yet
              </h3>

              <p>
                Find something interesting from
                the library.
              </p>

              <Link to="/books">
                Browse Books
              </Link>

            </div>

          ) : (

            <div className="reading-grid">

              {currentBooks.map((item) => (

                <Link
                  key={item.id}
                  to={`/books/${item.bookId}`}
                  className="reading-card"
                >

                  <div className="reading-cover">
                    <div className="reading-cover-placeholder">
                      <BookOpen size={28} />
                    </div>
                  </div>

                  <div className="reading-details">

                    <h3>
                      {item.bookTitle}
                    </h3>

                    <p>
                      Due {formatDate(item.dueDate)}
                    </p>

                    <div className="reading-progress">

                      <div className="progress-label">
                        <span>
                          {item.status}
                        </span>

                        <span>
                          {item.status === "OVERDUE"
                            ? "Overdue"
                            : "Reading"}
                        </span>
                      </div>

                      <div className="progress-track">
                        <div
                          className={`progress-fill ${
                            item.status === "OVERDUE"
                              ? "progress-overdue"
                              : ""
                          }`}
                        />
                      </div>

                    </div>

                    <span className="reading-view">
                      View Book
                      <ArrowRight size={13} />
                    </span>

                  </div>

                </Link>

              ))}

            </div>

          )}

        </section>

        {/* Due dates */}

        <section className="dashboard-panel due-panel">

          <div className="dashboard-panel-heading">

            <div>
              <h2>Upcoming Due Dates</h2>
              <p>
                Keep track of returns
              </p>
            </div>

            <CalendarDays size={18} />

          </div>

          {dueSoon.length === 0 ? (

            <div className="due-empty">

              <CheckCircle2 size={24} />

              <strong>
                No upcoming due dates
              </strong>

              <span>
                You're all caught up.
              </span>

            </div>

          ) : (

            <div className="due-list">

              {dueSoon.slice(0, 4).map((item) => (

                <Link
                  key={item.id}
                  to={`/books/${item.bookId}`}
                  className="due-item"
                >

                  <div className="due-book-icon">
                    <BookOpen size={17} />
                  </div>

                  <div className="due-book-info">

                    <strong>
                      {item.bookTitle}
                    </strong>

                    <span>
                      Due {formatDate(item.dueDate)}
                    </span>

                  </div>

                  <div className="due-warning">
                    Soon
                  </div>

                </Link>

              ))}

            </div>

          )}

        </section>

      </div>

      {/* Recommendations */}

      <section className="recommendation-section">

        <div className="dashboard-panel-heading">

          <div>
            <h2>Explore the Collection</h2>
            <p>
              Discover something new to read.
            </p>
          </div>

          <Link to="/books">
            Browse all
            <ArrowRight size={14} />
          </Link>

        </div>

        {recommendedBooks.length === 0 ? (

          <div className="recommendation-empty">
            Books will appear here once they're
            added to the library.
          </div>

        ) : (

          <div className="recommendation-grid">

            {recommendedBooks.map((book) => (

              <Link
                key={book.id}
                to={`/books/${book.id}`}
                className="recommendation-card"
              >

                <div className="recommendation-cover">

                  <img
                    src={getCover(book)}
                    alt={book.title}
                  />

                  <span
                    className={
                      book.availableCopies > 0
                        ? "available-label"
                        : "unavailable-label"
                    }
                  >
                    {book.availableCopies > 0
                      ? `${book.availableCopies} available`
                      : "Unavailable"}
                  </span>

                </div>

                <div className="recommendation-info">

                  <div className="recommendation-rating">
                    ★{" "}
                    {Number(
                      book.averageRating || 0
                    ).toFixed(1)}
                  </div>

                  <h3>{book.title}</h3>

                  <p>{book.author}</p>

                </div>

              </Link>

            ))}

          </div>

        )}

      </section>

    </div>
  );
}

export default Dashboard;
