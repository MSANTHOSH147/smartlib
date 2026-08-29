import { useEffect, useMemo, useState } from "react";
import {
  ArrowLeft,
  BookOpen,
  CalendarClock,
  CheckCircle2,
  Clock3,
  Search,
  Users,
  X,
} from "lucide-react";
import { Link } from "react-router-dom";

import adminService from "../../services/adminService";

import "./Reservations.css";

function Reservations() {
  const [reservations, setReservations] = useState([]);
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [updatingId, setUpdatingId] = useState(null);

  useEffect(() => {
    loadReservations();
  }, []);

  async function loadReservations() {
    try {
      setLoading(true);
      setError("");

      const data = await adminService.getReservations();

      setReservations(
        Array.isArray(data) ? data : []
      );
    } catch (err) {
      console.error(
        "Reservations loading error:",
        err
      );

      setError(
        err.response?.data?.message ||
          "Unable to load reservation records."
      );
    } finally {
      setLoading(false);
    }
  }

  const waitingCount = reservations.filter(
    (item) =>
      String(item.status).toUpperCase() ===
      "WAITING"
  ).length;

  const readyCount = reservations.filter(
    (item) =>
      String(item.status).toUpperCase() ===
      "READY"
  ).length;

  const completedCount = reservations.filter(
    (item) =>
      String(item.status).toUpperCase() ===
      "COMPLETED"
  ).length;

  const filteredReservations = useMemo(() => {
    const value = query.trim().toLowerCase();

    return reservations.filter((item) => {
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
  }, [reservations, query, filter]);

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

  function formatDateTime(value) {
    if (!value) return "—";

    return new Date(value).toLocaleString(
      "en-IN",
      {
        day: "numeric",
        month: "short",
        year: "numeric",
        hour: "numeric",
        minute: "2-digit",
      }
    );
  }

  function getStatusClass(status) {
    const value = String(
      status || ""
    ).toUpperCase();

    if (value === "READY") {
      return "reservation-status ready";
    }

    if (value === "COMPLETED") {
      return "reservation-status completed";
    }

    return "reservation-status waiting";
  }

  function getStatusLabel(status) {
    const value = String(
      status || ""
    ).toUpperCase();

    if (value === "READY") {
      return "Ready";
    }

    if (value === "COMPLETED") {
      return "Completed";
    }

    if (value === "WAITING") {
      return "Waiting";
    }

    return value || "Unknown";
  }

  async function updateStatus(id, status) {
    try {
      setUpdatingId(id);
      setError("");

      await adminService.updateReservationStatus(
        id,
        status
      );

      await loadReservations();
    } catch (err) {
      console.error(
        "Reservation status update error:",
        err
      );

      setError(
        err.response?.data?.message ||
          "Unable to update reservation."
      );
    } finally {
      setUpdatingId(null);
    }
  }

  return (
    <div className="admin-reservations-page">

      <header className="admin-reservations-header">

        <div className="reservations-topline">

          <Link
            to="/admin/dashboard"
            className="reservations-back"
          >
            <ArrowLeft size={17} />
            Dashboard
          </Link>

          <span className="reservations-section-label">
            SMARTLIB / OPERATIONS
          </span>

        </div>

        <div className="reservations-hero">

          <div>

            <p className="reservations-eyebrow">
              LIBRARY OPERATIONS
            </p>

            <h1>
              Reservations<span>.</span>
            </h1>

            <p>
              Manage member reservations,
              pickup readiness, and completed
              collection requests.
            </p>

          </div>

          <div className="reservations-hero-icon">
            <CalendarClock size={29} />
          </div>

        </div>

      </header>

      <main className="admin-reservations-content">

        <section className="reservation-stats">

          <div className="reservation-stat-card">

            <div className="stat-icon">
              <CalendarClock size={19} />
            </div>

            <span>TOTAL</span>

            <strong>{reservations.length}</strong>

            <small>Reservation records</small>

          </div>

          <div className="reservation-stat-card waiting-card">

            <div className="stat-icon">
              <Clock3 size={19} />
            </div>

            <span>WAITING</span>

            <strong>{waitingCount}</strong>

            <small>Awaiting availability</small>

          </div>

          <div className="reservation-stat-card ready-card">

            <div className="stat-icon">
              <BookOpen size={19} />
            </div>

            <span>READY</span>

            <strong>{readyCount}</strong>

            <small>Ready for pickup</small>

          </div>

          <div className="reservation-stat-card">

            <div className="stat-icon">
              <CheckCircle2 size={19} />
            </div>

            <span>COMPLETED</span>

            <strong>{completedCount}</strong>

            <small>Completed requests</small>

          </div>

        </section>

        {error && (

          <div className="reservations-error">

            <span>{error}</span>

            <button onClick={loadReservations}>
              Retry
            </button>

            <button
              className="error-close"
              onClick={() => setError("")}
            >
              <X size={16} />
            </button>

          </div>

        )}

        <section className="reservations-panel">

          <div className="reservations-panel-header">

            <div>

              <h2>
                Reservation Activity
              </h2>

              <p>
                {filteredReservations.length}{" "}
                {filteredReservations.length === 1
                  ? "reservation"
                  : "reservations"}{" "}
                displayed
              </p>

            </div>

            <div className="reservations-search">

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

          <div className="reservations-filters">

            {[
              ["ALL", "All"],
              ["WAITING", "Waiting"],
              ["READY", "Ready"],
              ["COMPLETED", "Completed"],
            ].map(([value, label]) => (

              <button
                key={value}
                className={
                  filter === value
                    ? "reservation-filter selected"
                    : "reservation-filter"
                }
                onClick={() => setFilter(value)}
              >
                {label}
              </button>

            ))}

          </div>

          {loading ? (

            <div className="reservations-loading">

              <div className="reservations-spinner" />

              <p>
                Loading reservations...
              </p>

            </div>

          ) : filteredReservations.length === 0 ? (

            <div className="reservations-empty">

              <div className="empty-icon">
                <CalendarClock size={27} />
              </div>

              <h3>
                No reservations found
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

            <div className="reservations-table-wrap">

              <table className="reservations-table">

                <thead>

                  <tr>
                    <th>BOOK</th>
                    <th>MEMBER</th>
                    <th>RESERVED</th>
                    <th>READY AT</th>
                    <th>STATUS</th>
                    <th>ACTION</th>
                  </tr>

                </thead>

                <tbody>

                  {filteredReservations.map(
                    (reservation) => {

                      const status =
                        String(
                          reservation.status || ""
                        ).toUpperCase();

                      return (

                        <tr key={reservation.id}>

                          <td>

                            <div className="book-cell">

                              <div className="book-cell-icon">
                                <BookOpen size={17} />
                              </div>

                              <div>

                                <strong>
                                  {reservation.bookTitle ||
                                    "Unknown book"}
                                </strong>

                                <span>
                                  Book #
                                  {reservation.bookId}
                                </span>

                              </div>

                            </div>

                          </td>

                          <td>

                            <div className="member-cell">

                              <div className="member-avatar">
                                {reservation.userName
                                  ?.charAt(0)
                                  ?.toUpperCase() ||
                                  "U"}
                              </div>

                              <div>

                                <strong>
                                  {reservation.userName ||
                                    "Unknown member"}
                                </strong>

                                <span>
                                  Member #
                                  {reservation.userId}
                                </span>

                              </div>

                            </div>

                          </td>

                          <td>

                            <span className="date-value">
                              {formatDateTime(
                                reservation.reservedAt
                              )}
                            </span>

                          </td>

                          <td>

                            <span className="date-value">
                              {formatDateTime(
                                reservation.readyAt
                              )}
                            </span>

                          </td>

                          <td>

                            <span
                              className={getStatusClass(
                                reservation.status
                              )}
                            >
                              <span className="status-dot" />

                              {getStatusLabel(
                                reservation.status
                              )}
                            </span>

                          </td>

                          <td>

                            {status === "WAITING" && (

                              <button
                                className="reservation-action ready-action"
                                disabled={
                                  updatingId ===
                                  reservation.id
                                }
                                onClick={() =>
                                  updateStatus(
                                    reservation.id,
                                    "READY"
                                  )
                                }
                              >
                                {updatingId ===
                                reservation.id
                                  ? "Updating..."
                                  : "Mark Ready"}
                              </button>

                            )}

                            {status === "READY" && (

                              <button
                                className="reservation-action complete-action"
                                disabled={
                                  updatingId ===
                                  reservation.id
                                }
                                onClick={() =>
                                  updateStatus(
                                    reservation.id,
                                    "COMPLETED"
                                  )
                                }
                              >
                                {updatingId ===
                                reservation.id
                                  ? "Updating..."
                                  : "Complete"}
                              </button>

                            )}

                            {status === "COMPLETED" && (

                              <span className="action-completed">
                                <CheckCircle2 size={15} />
                                Done
                              </span>

                            )}

                          </td>

                        </tr>

                      );
                    }
                  )}

                </tbody>

              </table>

            </div>

          )}

        </section>

        <section className="reservations-footer-info">

          <div>
            <Users size={19} />

            <span>
              Reservation activity is synchronized
              directly with the SmartLib library.
            </span>
          </div>

          <span>
            {reservations.length} TOTAL RECORDS
          </span>

        </section>

      </main>

    </div>
  );
}

export default Reservations;
