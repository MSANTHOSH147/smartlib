import { useEffect, useMemo, useState } from "react";
import {
  ArrowLeft,
  BookOpen,
  CalendarDays,
  CheckCircle2,
  Clock3,
  Search,
  Users,
  X,
} from "lucide-react";
import { Link } from "react-router-dom";

import adminService from "../../services/adminService";

import "./Borrowings.css";

function Borrowings() {
  const [borrowings, setBorrowings] = useState([]);
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    loadBorrowings();
  }, []);

  async function loadBorrowings() {
    try {
      setLoading(true);
      setError("");

      const data = await adminService.getBorrowings();

      setBorrowings(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error("Borrowings loading error:", err);

      setError(
        err.response?.data?.message ||
          err.userMessage ||
          err.message ||
          "Unable to load borrowing records."
      );
    } finally {
      setLoading(false);
    }
  }

  const activeCount = borrowings.filter(
    (item) =>
      String(item.status).toUpperCase() === "BORROWED"
  ).length;

  const overdueCount = borrowings.filter(
    (item) =>
      String(item.status).toUpperCase() === "OVERDUE"
  ).length;

  const returnedCount = borrowings.filter(
    (item) =>
      String(item.status).toUpperCase() === "RETURNED"
  ).length;

  const filteredBorrowings = useMemo(() => {
    const value = query.trim().toLowerCase();

    return borrowings.filter((item) => {
      const status = String(
        item.status || ""
      ).toUpperCase();

      const matchesSearch =
        !value ||
        String(item.bookTitle || "")
          .toLowerCase()
          .includes(value) ||
        String(item.userName || "")
          .toLowerCase()
          .includes(value) ||
        String(item.id || "").includes(value);

      const matchesFilter =
        filter === "ALL" ||
        status === filter;

      return matchesSearch && matchesFilter;
    });
  }, [borrowings, query, filter]);

  function formatDate(value) {
    if (!value) return "—";

    return new Date(value).toLocaleDateString(
      "en-IN",
      {
        day: "numeric",
        month: "short",
        year: "numeric",
      }
    );
  }

  function getStatusClass(status) {
    const value = String(
      status || ""
    ).toUpperCase();

    if (value === "OVERDUE") {
      return "borrowing-status overdue";
    }

    if (value === "RETURNED") {
      return "borrowing-status returned";
    }

    return "borrowing-status borrowed";
  }

  function getStatusLabel(status) {
    const value = String(
      status || ""
    ).toUpperCase();

    if (value === "BORROWED") {
      return "Borrowed";
    }

    if (value === "OVERDUE") {
      return "Overdue";
    }

    if (value === "RETURNED") {
      return "Returned";
    }

    return value || "Unknown";
  }

  return (
    <div className="admin-borrowings-page">

      <header className="admin-borrowings-header">

        <div className="borrowings-topline">

          <Link
            to="/admin/dashboard"
            className="borrowings-back"
          >
            <ArrowLeft size={17} />
            Dashboard
          </Link>

          <span className="borrowings-section-label">
            SMARTLIB / OPERATIONS
          </span>

        </div>

        <div className="borrowings-hero">

          <div>

            <p className="borrowings-eyebrow">
              LIBRARY OPERATIONS
            </p>

            <h1>
              Borrowings<span>.</span>
            </h1>

            <p>
              Monitor books currently with members,
              upcoming returns, and overdue activity.
            </p>

          </div>

          <div className="borrowings-hero-icon">
            <BookOpen size={29} />
          </div>

        </div>

      </header>

      <main className="admin-borrowings-content">

        <section className="borrowing-stats">

          <div className="borrowing-stat-card">

            <div className="stat-icon">
              <BookOpen size={19} />
            </div>

            <span>TOTAL</span>

            <strong>{borrowings.length}</strong>

            <small>Borrowing records</small>

          </div>

          <div className="borrowing-stat-card">

            <div className="stat-icon">
              <Clock3 size={19} />
            </div>

            <span>ACTIVE</span>

            <strong>{activeCount}</strong>

            <small>Currently borrowed</small>

          </div>

          <div className="borrowing-stat-card overdue-card">

            <div className="stat-icon">
              <CalendarDays size={19} />
            </div>

            <span>OVERDUE</span>

            <strong>{overdueCount}</strong>

            <small>Require attention</small>

          </div>

          <div className="borrowing-stat-card">

            <div className="stat-icon">
              <CheckCircle2 size={19} />
            </div>

            <span>RETURNED</span>

            <strong>{returnedCount}</strong>

            <small>Completed returns</small>

          </div>

        </section>

        {error && (

          <div className="borrowings-error">

            <span>{error}</span>

            <button onClick={loadBorrowings}>
              Retry
            </button>

            <button
              onClick={() => setError("")}
              className="error-close"
            >
              <X size={16} />
            </button>

          </div>

        )}

        <section className="borrowings-panel">

          <div className="borrowings-panel-header">

            <div>

              <h2>
                Borrowing Activity
              </h2>

              <p>
                {filteredBorrowings.length}{" "}
                {filteredBorrowings.length === 1
                  ? "record"
                  : "records"}{" "}
                displayed
              </p>

            </div>

            <div className="borrowings-search">

              <Search size={18} />

              <input
                value={query}
                onChange={(event) =>
                  setQuery(event.target.value)
                }
                placeholder="Search member or book..."
              />

              {query && (

                <button
                  onClick={() => setQuery("")}
                >
                  <X size={15} />
                </button>

              )}

            </div>

          </div>

          <div className="borrowings-filters">

            {[
              ["ALL", "All"],
              ["BORROWED", "Active"],
              ["OVERDUE", "Overdue"],
              ["RETURNED", "Returned"],
            ].map(([value, label]) => (

              <button
                key={value}
                className={
                  filter === value
                    ? "borrowing-filter selected"
                    : "borrowing-filter"
                }
                onClick={() => setFilter(value)}
              >
                {label}
              </button>

            ))}

          </div>

          {loading ? (

            <div className="borrowings-loading">

              <div className="borrowings-spinner" />

              <p>
                Loading borrowing activity...
              </p>

            </div>

          ) : filteredBorrowings.length === 0 ? (

            <div className="borrowings-empty">

              <div className="empty-icon">
                <BookOpen size={27} />
              </div>

              <h3>
                No borrowing records found
              </h3>

              <p>
                Try adjusting your search or filters.
              </p>

              <button
                onClick={() => {
                  setQuery("");
                  setFilter("ALL");
                }}
              >
                Clear filters
              </button>

            </div>

          ) : (

            <div className="borrowings-table-wrap">

              <table className="borrowings-table">

                <thead>

                  <tr>
                    <th>BOOK</th>
                    <th>MEMBER</th>
                    <th>BORROWED</th>
                    <th>DUE DATE</th>
                    <th>RETURNED</th>
                    <th>STATUS</th>
                  </tr>

                </thead>

                <tbody>

                  {filteredBorrowings.map(
                    (borrowing) => (

                      <tr key={borrowing.id}>

                        <td>

                          <div className="book-cell">

                            <div className="book-cell-icon">
                              <BookOpen size={17} />
                            </div>

                            <div>

                              <strong>
                                {borrowing.bookTitle ||
                                  "Unknown book"}
                              </strong>

                              <span>
                                Book #{borrowing.bookId}
                              </span>

                            </div>

                          </div>

                        </td>

                        <td>

                          <div className="member-cell">

                            <div className="member-avatar">
                              {borrowing.userName
                                ?.charAt(0)
                                ?.toUpperCase() ||
                                "U"}
                            </div>

                            <div>

                              <strong>
                                {borrowing.userName ||
                                  "Unknown member"}
                              </strong>

                              <span>
                                Member #
                                {borrowing.userId}
                              </span>

                            </div>

                          </div>

                        </td>

                        <td>

                          <span className="date-value">
                            {formatDate(
                              borrowing.borrowDate
                            )}
                          </span>

                        </td>

                        <td>

                          <span
                            className={
                              String(
                                borrowing.status
                              ).toUpperCase() ===
                              "OVERDUE"
                                ? "date-value overdue-date"
                                : "date-value"
                            }
                          >
                            {formatDate(
                              borrowing.dueDate
                            )}
                          </span>

                        </td>

                        <td>

                          <span className="date-value">
                            {formatDate(
                              borrowing.returnDate
                            )}
                          </span>

                        </td>

                        <td>

                          <span
                            className={getStatusClass(
                              borrowing.status
                            )}
                          >
                            <span className="status-dot" />

                            {getStatusLabel(
                              borrowing.status
                            )}
                          </span>

                        </td>

                      </tr>

                    )
                  )}

                </tbody>

              </table>

            </div>

          )}

        </section>

        <section className="borrowings-footer-info">

          <div>
            <Users size={19} />

            <span>
              Member borrowing activity is updated
              directly from the SmartLib system.
            </span>
          </div>

          <span>
            {borrowings.length} TOTAL RECORDS
          </span>

        </section>

      </main>

    </div>
  );
}

export default Borrowings;
