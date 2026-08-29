import { useEffect, useMemo, useState } from "react";
import {
  ArrowLeft,
  CircleDollarSign,
  Clock3,
  CreditCard,
  LoaderCircle,
  ReceiptText,
} from "lucide-react";
import { Link } from "react-router-dom";

import transactionService from "../services/transactionService";

import "./Fines.css";

function Fines() {
  const [fines, setFines] = useState([]);
  const [loading, setLoading] = useState(true);
  const [payingId, setPayingId] = useState(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  useEffect(() => {
    loadFines();
  }, []);

  async function loadFines() {
    try {
      setLoading(true);
      setError("");

      const data = await transactionService.getMyFines();

      setFines(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error("Fines loading error:", err);

      setError(
        err.response?.data?.message ||
          "Unable to load your fines."
      );
    } finally {
      setLoading(false);
    }
  }

  async function handlePay(fineId) {
    const confirmed = window.confirm(
      "Are you sure you want to pay this fine?"
    );

    if (!confirmed) {
      return;
    }

    try {
      setPayingId(fineId);
      setError("");
      setMessage("");

      await transactionService.payFine(fineId);

      setMessage("Fine paid successfully.");

      await loadFines();
    } catch (err) {
      console.error("Fine payment error:", err);

      setError(
        err.response?.data?.message ||
          "Unable to pay this fine."
      );
    } finally {
      setPayingId(null);
    }
  }

  function getFineAmount(fine) {
    return fine.amount || 0;
  }

  function getBookTitle(fine) {
    return fine.bookTitle || "Library Fine";
  }

  function isPaid(fine) {
    return (
      String(fine.status || "").toUpperCase() === "PAID"
    );
  }

  function formatDate(value) {
    if (!value) {
      return "—";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return "—";
    }

    return date.toLocaleDateString("en-IN", {
      day: "numeric",
      month: "short",
      year: "numeric",
    });
  }

  const unpaidFines = fines.filter(
    (fine) => !isPaid(fine)
  );

  const paidFines = fines.filter(
    (fine) => isPaid(fine)
  );

  const outstandingAmount = useMemo(() => {
    return unpaidFines.reduce(
      (total, fine) =>
        total + Number(getFineAmount(fine)),
      0
    );
  }, [unpaidFines]);

  const totalAmount = useMemo(() => {
    return fines.reduce(
      (total, fine) =>
        total + Number(getFineAmount(fine)),
      0
    );
  }, [fines]);

  return (
    <div className="fines-page">

      {/* Back */}

      <Link
        to="/dashboard"
        className="fines-back"
      >
        <ArrowLeft size={16} />
        Dashboard
      </Link>


      {/* Header */}

      <header className="fines-header">

        <div>

          <div className="fines-eyebrow">
            MY LIBRARY
          </div>

          <h1>Fines</h1>

          <p>
            Review outstanding charges and keep your
            library account clear.
          </p>

        </div>

        <div className="fines-header-icon">
          <CircleDollarSign size={28} />
        </div>

      </header>


      {/* Success */}

      {message && (
        <div className="fines-message">
          {message}
        </div>
      )}


      {/* Error */}

      {error && (
        <div className="fines-error">

          <span>
            {error}
          </span>

          <button onClick={loadFines}>
            Retry
          </button>

        </div>
      )}


      {/* Loading */}

      {loading ? (

        <div className="fines-loading">

          <LoaderCircle size={30} />

          <span>
            Loading your fines...
          </span>

        </div>

      ) : (

        <>

          {/* Overview */}

          <section className="fines-overview">

            <div className="fine-overview-main">

              <div className="fine-overview-label">
                Outstanding balance
              </div>

              <div className="fine-overview-amount">
                ₹{outstandingAmount.toFixed(2)}
              </div>

              <p>
                {unpaidFines.length === 0
                  ? "Your account is clear."
                  : `${unpaidFines.length} unpaid ${
                      unpaidFines.length === 1
                        ? "fine"
                        : "fines"
                    } require your attention.`}
              </p>

            </div>


            <div className="fine-overview-side">

              <div>

                <span>
                  Total fines
                </span>

                <strong>
                  ₹{totalAmount.toFixed(2)}
                </strong>

              </div>

              <div>

                <span>
                  Unpaid
                </span>

                <strong>
                  {unpaidFines.length}
                </strong>

              </div>

              <div>

                <span>
                  Paid
                </span>

                <strong>
                  {paidFines.length}
                </strong>

              </div>

            </div>

          </section>


          {/* Outstanding Fines */}

          <section className="fines-section">

            <div className="fines-section-heading">

              <div>

                <h2>
                  Outstanding Fines
                </h2>

                <p>
                  Charges that are currently unpaid.
                </p>

              </div>

              <span>
                {unpaidFines.length}{" "}
                {unpaidFines.length === 1
                  ? "fine"
                  : "fines"}
              </span>

            </div>


            {unpaidFines.length === 0 ? (

              <div className="fines-empty">

                <div className="fine-empty-icon">
                  <ReceiptText size={24} />
                </div>

                <h3>
                  No outstanding fines
                </h3>

                <p>
                  You're all clear. There are no
                  unpaid charges on your SmartLib
                  account.
                </p>

                <Link to="/books">
                  Browse Books
                </Link>

              </div>

            ) : (

              <div className="fine-list">

                {unpaidFines.map((fine) => (

                  <article
                    className="fine-card"
                    key={fine.id}
                  >

                    {/* Icon */}

                    <div className="fine-icon">
                      <ReceiptText size={22} />
                    </div>


                    {/* Information */}

                    <div className="fine-info">

                      <div className="fine-title-row">

                        <div>

                          <h3>
                            {getBookTitle(fine)}
                          </h3>

                          <p>
                            Borrowing #{fine.borrowingId}
                          </p>

                        </div>

                        <strong className="fine-card-amount">
                          ₹
                          {Number(
                            getFineAmount(fine)
                          ).toFixed(2)}
                        </strong>

                      </div>


                      <div className="fine-meta">

                        <span>
                          <Clock3 size={14} />

                          Issued{" "}
                          {formatDate(
                            fine.createdAt
                          )}
                        </span>

                        <span className="fine-unpaid-status">
                          {fine.status}
                        </span>

                      </div>

                    </div>


                    {/* Pay */}

                    <button
                      className="fine-pay-button"
                      onClick={() =>
                        handlePay(fine.id)
                      }
                      disabled={
                        payingId === fine.id
                      }
                    >

                      {payingId === fine.id ? (

                        <LoaderCircle
                          size={15}
                          className="fine-spinner"
                        />

                      ) : (

                        <CreditCard size={15} />

                      )}

                      {payingId === fine.id
                        ? "Processing..."
                        : "Pay Fine"}

                    </button>

                  </article>

                ))}

              </div>

            )}

          </section>


          {/* Payment History */}

          {paidFines.length > 0 && (

            <section className="fines-section fine-history">

              <div className="fines-section-heading">

                <div>

                  <h2>
                    Payment History
                  </h2>

                  <p>
                    Previously paid library charges.
                  </p>

                </div>

                <span>
                  {paidFines.length} records
                </span>

              </div>


              <div className="fine-list">

                {paidFines.map((fine) => (

                  <article
                    className="fine-card fine-paid-card"
                    key={fine.id}
                  >

                    <div className="fine-icon">
                      <ReceiptText size={22} />
                    </div>


                    <div className="fine-info">

                      <div className="fine-title-row">

                        <div>

                          <h3>
                            {getBookTitle(fine)}
                          </h3>

                          <p>
                            Borrowing #{fine.borrowingId}
                          </p>

                        </div>

                        <strong className="fine-card-amount">
                          ₹
                          {Number(
                            getFineAmount(fine)
                          ).toFixed(2)}
                        </strong>

                      </div>


                      <div className="fine-meta">

                        <span>
                          <Clock3 size={14} />

                          Issued{" "}
                          {formatDate(
                            fine.createdAt
                          )}
                        </span>

                        {fine.paidAt && (

                          <span>
                            Paid{" "}
                            {formatDate(
                              fine.paidAt
                            )}
                          </span>

                        )}

                      </div>

                    </div>


                    <span className="fine-paid-status">
                      {fine.status}
                    </span>

                  </article>

                ))}

              </div>

            </section>

          )}

        </>

      )}

    </div>
  );
}

export default Fines;
