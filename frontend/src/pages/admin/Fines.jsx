import { useEffect, useMemo, useState } from "react";
import {
  ArrowLeft,
  CheckCircle2,
  CircleDollarSign,
  CreditCard,
  Search,
  UserRound,
  X,
} from "lucide-react";
import { Link } from "react-router-dom";

import adminService from "../../services/adminService";

import "./Fines.css";

function Fines() {
  const [fines, setFines] = useState([]);
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [payingId, setPayingId] = useState(null);

  useEffect(() => {
    loadFines();
  }, []);

  async function loadFines() {
    try {
      setLoading(true);
      setError("");

      const data = await adminService.getFines();

      setFines(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error("Fines loading error:", err);

      setError(
        err.response?.data?.message ||
          "Unable to load fine records."
      );
    } finally {
      setLoading(false);
    }
  }

  function isPaid(fine) {
    return (
      String(fine.status || "").toUpperCase() ===
      "PAID"
    );
  }

  const unpaidFines = fines.filter(
    (fine) => !isPaid(fine)
  );

  const paidFines = fines.filter(
    (fine) => isPaid(fine)
  );

  const totalAmount = fines.reduce(
    (sum, fine) =>
      sum + Number(fine.amount || 0),
    0
  );

  const unpaidAmount = unpaidFines.reduce(
    (sum, fine) =>
      sum + Number(fine.amount || 0),
    0
  );

  const paidAmount = paidFines.reduce(
    (sum, fine) =>
      sum + Number(fine.amount || 0),
    0
  );

  const filteredFines = useMemo(() => {
    const value = query.trim().toLowerCase();

    return fines.filter((fine) => {
      const status = String(
        fine.status || ""
      ).toUpperCase();

      const matchesSearch =
        !value ||
        String(fine.bookTitle || "")
          .toLowerCase()
          .includes(value) ||
        String(fine.userName || "")
          .toLowerCase()
          .includes(value) ||
        String(fine.id || "").includes(value) ||
        String(fine.borrowingId || "").includes(value);

      const matchesFilter =
        filter === "ALL" ||
        status === filter;

      return (
        matchesSearch &&
        matchesFilter
      );
    });
  }, [fines, query, filter]);

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

  async function handlePayFine(id) {
    try {
      setPayingId(id);
      setError("");

      await adminService.payFine(id);

      await loadFines();
    } catch (err) {
      console.error(
        "Fine payment error:",
        err
      );

      setError(
        err.response?.data?.message ||
          "Unable to mark fine as paid."
      );
    } finally {
      setPayingId(null);
    }
  }

  return (
    <div className="admin-fines-page">

      <header className="admin-fines-header">

        <div className="fines-topline">

          <Link
            to="/admin/dashboard"
            className="fines-back"
          >
            <ArrowLeft size={17} />
            Dashboard
          </Link>

          <span className="fines-section-label">
            SMARTLIB / OPERATIONS
          </span>

        </div>

        <div className="fines-hero">

          <div>

            <p className="fines-eyebrow">
              LIBRARY OPERATIONS
            </p>

            <h1>
              Fines<span>.</span>
            </h1>

            <p>
              Monitor outstanding charges and
              manage member payments.
            </p>

          </div>

          <div className="fines-hero-icon">
            <CircleDollarSign size={30} />
          </div>

        </div>

      </header>

      <main className="admin-fines-content">

        <section className="fine-stats">

          <div className="fine-stat-card">

            <div className="fine-stat-icon">
              <CircleDollarSign size={19} />
            </div>

            <span>TOTAL FINES</span>

            <strong>
              ₹{totalAmount.toFixed(2)}
            </strong>

            <small>
              All recorded fines
            </small>

          </div>

          <div className="fine-stat-card unpaid-stat">

            <div className="fine-stat-icon">
              <CreditCard size={19} />
            </div>

            <span>UNPAID</span>

            <strong>
              ₹{unpaidAmount.toFixed(2)}
            </strong>

            <small>
              Outstanding amount
            </small>

          </div>

          <div className="fine-stat-card paid-stat">

            <div className="fine-stat-icon">
              <CheckCircle2 size={19} />
            </div>

            <span>PAID</span>

            <strong>
              ₹{paidAmount.toFixed(2)}
            </strong>

            <small>
              Successfully settled
            </small>

          </div>

          <div className="fine-stat-card">

            <div className="fine-stat-icon">
              <UserRound size={19} />
            </div>

            <span>RECORDS</span>

            <strong>
              {fines.length}
            </strong>

            <small>
              Fine records
            </small>

          </div>

        </section>

        {error && (

          <div className="fines-error">

            <span>{error}</span>

            <button onClick={loadFines}>
              Retry
            </button>

            <button
              className="fines-error-close"
              onClick={() => setError("")}
            >
              <X size={16} />
            </button>

          </div>

        )}

        <section className="fines-panel">

          <div className="fines-panel-header">

            <div>

              <h2>
                Fine Activity
              </h2>

              <p>
                {filteredFines.length}{" "}
                {filteredFines.length === 1
                  ? "record"
                  : "records"}{" "}
                displayed
              </p>

            </div>

            <div className="fines-search">

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

          <div className="fines-filters">

            {[
              ["ALL", "All"],
              ["UNPAID", "Unpaid"],
              ["PAID", "Paid"],
            ].map(([value, label]) => (

              <button
                key={value}
                className={
                  filter === value
                    ? "fine-filter selected"
                    : "fine-filter"
                }
                onClick={() => setFilter(value)}
              >
                {label}
              </button>

            ))}

          </div>

          {loading ? (

            <div className="fines-loading">

              <div className="fines-spinner" />

              <p>
                Loading fines...
              </p>

            </div>

          ) : filteredFines.length === 0 ? (

            <div className="fines-empty">

              <div className="fines-empty-icon">
                <CircleDollarSign size={28} />
              </div>

              <h3>
                No fine records found
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

            <div className="fines-table-wrap">

              <table className="fines-table">

                <thead>

                  <tr>
                    <th>BOOK</th>
                    <th>MEMBER</th>
                    <th>AMOUNT</th>
                    <th>ISSUED</th>
                    <th>PAID AT</th>
                    <th>STATUS</th>
                    <th>ACTION</th>
                  </tr>

                </thead>

                <tbody>

                  {filteredFines.map((fine) => {

                    const paid = isPaid(fine);

                    return (

                      <tr key={fine.id}>

                        <td>

                          <div className="fine-book-cell">

                            <div className="fine-book-icon">
                              <CircleDollarSign
                                size={17}
                              />
                            </div>

                            <div>

                              <strong>
                                {fine.bookTitle ||
                                  "Library Fine"}
                              </strong>

                              <span>
                                Borrowing #
                                {fine.borrowingId}
                              </span>

                            </div>

                          </div>

                        </td>

                        <td>

                          <div className="fine-member-cell">

                            <div className="fine-avatar">
                              {fine.userName
                                ?.charAt(0)
                                ?.toUpperCase() ||
                                "U"}
                            </div>

                            <div>

                              <strong>
                                {fine.userName ||
                                  "Unknown member"}
                              </strong>

                              <span>
                                Member #{fine.userId}
                              </span>

                            </div>

                          </div>

                        </td>

                        <td>

                          <strong className="fine-amount">
                            ₹
                            {Number(
                              fine.amount || 0
                            ).toFixed(2)}
                          </strong>

                        </td>

                        <td>

                          <span className="fine-date">
                            {formatDate(
                              fine.createdAt
                            )}
                          </span>

                        </td>

                        <td>

                          <span className="fine-date">
                            {formatDate(
                              fine.paidAt
                            )}
                          </span>

                        </td>

                        <td>

                          <span
                            className={
                              paid
                                ? "fine-status paid"
                                : "fine-status unpaid"
                            }
                          >
                            <span className="fine-status-dot" />
                            {fine.status}
                          </span>

                        </td>

                        <td>

                          {paid ? (

                            <span className="fine-paid-action">
                              <CheckCircle2
                                size={15}
                              />
                              Paid
                            </span>

                          ) : (

                            <button
                              className="pay-fine-button"
                              disabled={
                                payingId === fine.id
                              }
                              onClick={() =>
                                handlePayFine(
                                  fine.id
                                )
                              }
                            >
                              {payingId === fine.id
                                ? "Updating..."
                                : "Mark Paid"}
                            </button>

                          )}

                        </td>

                      </tr>

                    );
                  })}

                </tbody>

              </table>

            </div>

          )}

        </section>

        <section className="fines-footer-info">

          <div>
            <CircleDollarSign size={18} />

            <span>
              Fine activity is synchronized
              directly with the SmartLib library.
            </span>
          </div>

          <span>
            {fines.length} TOTAL RECORDS
          </span>

        </section>

      </main>

    </div>
  );
}

export default Fines;
