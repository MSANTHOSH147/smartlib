import { useEffect, useState } from "react";
import {
  ArrowLeft,
  CalendarDays,
  Clock3,
  LoaderCircle,
  X,
} from "lucide-react";
import { Link } from "react-router-dom";

import reservationService from "../services/reservationService";

import "./Reservations.css";

function Reservations() {
  const [reservations, setReservations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [cancellingId, setCancellingId] = useState(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  useEffect(() => {
    loadReservations();
  }, []);

  async function loadReservations() {
    try {
      setLoading(true);
      setError("");

      const data = await reservationService.getMyReservations();

      setReservations(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error("Reservations loading error:", err);

      setError(
        err.response?.data?.message ||
          "Unable to load your reservations."
      );
    } finally {
      setLoading(false);
    }
  }

  async function handleCancel(id) {
    const confirmed = window.confirm(
      "Are you sure you want to cancel this reservation?"
    );

    if (!confirmed) {
      return;
    }

    try {
      setCancellingId(id);
      setError("");
      setMessage("");

      await reservationService.cancelReservation(id);

      setMessage("Reservation cancelled successfully.");

      await loadReservations();
    } catch (err) {
      console.error("Reservation cancellation error:", err);

      setError(
        err.response?.data?.message ||
          "Unable to cancel the reservation."
      );
    } finally {
      setCancellingId(null);
    }
  }

  const activeReservations = reservations.filter(
    (reservation) =>
      reservation.status !== "CANCELLED" &&
      reservation.status !== "COMPLETED"
  );

  const completedReservations = reservations.filter(
    (reservation) =>
      reservation.status === "CANCELLED" ||
      reservation.status === "COMPLETED"
  );

  function formatDate(value) {
    if (!value) {
      return "—";
    }

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
    if (!value) {
      return "—";
    }

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
    const normalized = String(status || "")
      .toLowerCase();

    if (
      normalized.includes("ready") ||
      normalized.includes("approved")
    ) {
      return "reservation-status ready";
    }

    if (normalized.includes("cancel")) {
      return "reservation-status cancelled";
    }

    if (normalized.includes("complete")) {
      return "reservation-status completed";
    }

    return "reservation-status pending";
  }

  return (
    <div className="reservations-page">

      <Link
        to="/dashboard"
        className="reservations-back"
      >
        <ArrowLeft size={16} />
        Dashboard
      </Link>

      <header className="reservations-header">

        <div>
          <div className="reservations-eyebrow">
            MY LIBRARY
          </div>

          <h1>Reservations</h1>

          <p>
            Keep track of books you've reserved and
            their availability.
          </p>
        </div>

        <Link
          to="/books"
          className="reservations-browse"
        >
          Browse Books
        </Link>

      </header>

      {message && (
        <div className="reservation-message">
          {message}
        </div>
      )}

      {error && (
        <div className="reservation-error">
          <span>{error}</span>

          <button onClick={loadReservations}>
            Retry
          </button>
        </div>
      )}

      {loading ? (

        <div className="reservations-loading">
          <LoaderCircle size={30} />
          <span>Loading reservations...</span>
        </div>

      ) : (

        <>

          <div className="reservation-summary">

            <div className="reservation-summary-card">
              <span>Active Reservations</span>
              <strong>{activeReservations.length}</strong>
            </div>

            <div className="reservation-summary-card">
              <span>Ready for Pickup</span>
              <strong>
                {
                  activeReservations.filter(
                    (reservation) =>
                      String(
                        reservation.status
                      ).toUpperCase() === "READY"
                  ).length
                }
              </strong>
            </div>

            <div className="reservation-summary-card">
              <span>Total Reservations</span>
              <strong>{reservations.length}</strong>
            </div>

          </div>

          <section className="reservations-section">

            <div className="reservations-section-heading">

              <div>
                <h2>Current Reservations</h2>

                <p>
                  Books you've reserved from the library.
                </p>
              </div>

              <span>
                {activeReservations.length}{" "}
                {activeReservations.length === 1
                  ? "reservation"
                  : "reservations"}
              </span>

            </div>

            {activeReservations.length === 0 ? (

              <div className="reservations-empty">

                <div className="reservation-empty-icon">
                  <CalendarDays size={24} />
                </div>

                <h3>No active reservations</h3>

                <p>
                  Reserve a book when it's currently
                  unavailable and we'll keep track of it
                  for you.
                </p>

                <Link to="/books">
                  Explore the Collection
                </Link>

              </div>

            ) : (

              <div className="reservation-list">

                {activeReservations.map(
                  (reservation) => (

                    <article
                      className="reservation-card"
                      key={reservation.id}
                    >

                      <div className="reservation-book-icon">
                        <CalendarDays size={22} />
                      </div>

                      <div className="reservation-info">

                        <div className="reservation-title-row">

                          <div>
                            <h3>
                              {
                                reservation.bookTitle ||
                                "Reserved Book"
                              }
                            </h3>

                            <p>
                              {reservation.bookAuthor ||
                                "Library collection"}
                            </p>
                          </div>

                          <span
                            className={getStatusClass(
                              reservation.status
                            )}
                          >
                            {reservation.status ||
                              "PENDING"}
                          </span>

                        </div>

                        <div className="reservation-meta">

                          <span>
                            <CalendarDays size={14} />

                            Reserved{" "}
                            {formatDateTime(
                              reservation.reservedAt
                            )}
                          </span>

                          <span>
                            <Clock3 size={14} />

                            Ready{" "}
                            {formatDate(
                              reservation.readyAt
                            )}
                          </span>

                        </div>

                      </div>

                      <button
                        className="reservation-cancel"
                        onClick={() =>
                          handleCancel(
                            reservation.id
                          )
                        }
                        disabled={
                          cancellingId ===
                          reservation.id
                        }
                      >

                        {cancellingId ===
                        reservation.id ? (
                          <LoaderCircle
                            size={15}
                            className="reservation-spinner"
                          />
                        ) : (
                          <X size={15} />
                        )}

                        {cancellingId ===
                        reservation.id
                          ? "Cancelling..."
                          : "Cancel"}

                      </button>

                    </article>
                  )
                )}

              </div>
            )}

          </section>

          {completedReservations.length > 0 && (

            <section className="reservations-section reservation-history">

              <div className="reservations-section-heading">

                <div>
                  <h2>Reservation History</h2>

                  <p>
                    Your completed and cancelled
                    reservations.
                  </p>
                </div>

                <span>
                  {completedReservations.length}{" "}
                  records
                </span>

              </div>

              <div className="reservation-list">

                {completedReservations.map(
                  (reservation) => (

                    <article
                      className="reservation-card reservation-history-card"
                      key={reservation.id}
                    >

                      <div className="reservation-book-icon">
                        <CalendarDays size={22} />
                      </div>

                      <div className="reservation-info">

                        <div className="reservation-title-row">

                          <div>
                            <h3>
                              {
                                reservation.bookTitle ||
                                "Reserved Book"
                              }
                            </h3>

                            <p>
                              Reservation record
                            </p>
                          </div>

                          <span
                            className={getStatusClass(
                              reservation.status
                            )}
                          >
                            {reservation.status}
                          </span>

                        </div>

                        <div className="reservation-meta">

                          <span>
                            <CalendarDays size={14} />

                            Reserved{" "}
                            {formatDateTime(
                              reservation.reservedAt
                            )}
                          </span>

                        </div>

                      </div>

                    </article>

                  )
                )}

              </div>

            </section>

          )}

        </>

      )}

    </div>
  );
}

export default Reservations;
