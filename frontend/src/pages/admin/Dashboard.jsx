import { useEffect, useState } from "react";
import {
  AlertCircle,
  ArrowRight,
  BookOpen,
  CalendarDays,
  CircleDollarSign,
  Clock3,
  LayoutDashboard,
  LoaderCircle,
  Users,
} from "lucide-react";
import { Link } from "react-router-dom";

import adminService from "../../services/adminService";

import "./Dashboard.css";

function AdminDashboard() {
  const [dashboard, setDashboard] = useState(null);
  const [library, setLibrary] = useState(null);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    loadDashboard();
  }, []);

  async function loadDashboard() {
  try {
    setLoading(true);
    setError("");

    const dashboardData =
      await adminService.getDashboard();

    setDashboard(dashboardData);

    // Library summary is optional.
    // Dashboard should still work if this endpoint fails.
    try {
      const libraryData =
        await adminService.getLibraryOperations();

      setLibrary(libraryData);
    } catch (libraryError) {
      console.warn(
        "Library summary unavailable:",
        libraryError
      );

      setLibrary(null);
    }

  } catch (err) {
    console.error(
      "Admin dashboard error:",
      err
    );

    setError(
      err.response?.data?.message ||
        err.response?.data ||
        "Unable to load admin dashboard."
    );
  } finally {
    setLoading(false);
  }
}

  if (loading) {
    return (
      <div className="admin-loading">
        <LoaderCircle size={32} />
        <span>Loading SmartLib Admin...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="admin-error-page">

        <AlertCircle size={38} />

        <h2>
          Unable to load dashboard
        </h2>

        <p>
          {error}
        </p>

        <button onClick={loadDashboard}>
          Retry
        </button>

      </div>
    );
  }

  const stats = [
    {
      label: "Total Books",
      value: dashboard?.totalBooks ?? 0,
      icon: BookOpen,
      className: "books",
    },
    {
      label: "Members",
      value: dashboard?.totalMembers ?? 0,
      icon: Users,
      className: "members",
    },
    {
      label: "Active Borrowings",
      value: dashboard?.activeBorrowings ?? 0,
      icon: Clock3,
      className: "borrowings",
    },
    {
      label: "Overdue",
      value: dashboard?.overdueBorrowings ?? 0,
      icon: AlertCircle,
      className: "overdue",
    },
    {
      label: "Reservations",
      value:
        dashboard?.pendingReservations ?? 0,
      icon: CalendarDays,
      className: "reservations",
    },
    {
      label: "Unpaid Fines",
      value:
        dashboard?.unpaidFines ?? 0,
      icon: CircleDollarSign,
      className: "fines",
    },
  ];

  return (
    <div className="admin-dashboard">

      {/* HERO */}

      <section className="admin-hero">

        <div>

          <div className="admin-eyebrow">
            SMARTLIB ADMINISTRATION
          </div>

          <h1>
            Library
            <br />
            Overview
          </h1>

          <p>
            Monitor your library, manage members,
            and keep daily operations running
            smoothly.
          </p>

        </div>

        <div className="admin-hero-symbol">
          <LayoutDashboard size={46} />
        </div>

      </section>


      {/* STATS */}

      <section className="admin-stats">

        {stats.map((stat) => {

          const Icon = stat.icon;

          return (
            <div
              className={`admin-stat-card ${stat.className}`}
              key={stat.label}
            >

              <div className="admin-stat-top">

                <span>
                  {stat.label}
                </span>

                <div className="admin-stat-icon">
                  <Icon size={18} />
                </div>

              </div>

              <strong>
                {stat.value}
              </strong>

            </div>
          );
        })}

      </section>


      {/* MAIN CONTENT */}

      <section className="admin-content-grid">


        {/* LIBRARY OPERATIONS */}

        <div className="admin-panel">

          <div className="admin-panel-header">

            <div>

              <div className="admin-panel-label">
                OPERATIONS
              </div>

              <h2>
                Library Activity
              </h2>

            </div>

            <div className="admin-panel-mark">
              <BookOpen size={18} />
            </div>

          </div>


          <div className="admin-operation-list">

            <div className="admin-operation-row">

              <div className="operation-icon">
                <Clock3 size={17} />
              </div>

              <div>
                <strong>
                  Active Borrowings
                </strong>

                <span>
                  Books currently with members
                </span>
              </div>

              <b>
                {dashboard?.activeBorrowings ?? 0}
              </b>

            </div>


            <div className="admin-operation-row">

              <div className="operation-icon">
                <AlertCircle size={17} />
              </div>

              <div>
                <strong>
                  Overdue Books
                </strong>

                <span>
                  Returns requiring attention
                </span>
              </div>

              <b>
                {dashboard?.overdueBorrowings ?? 0}
              </b>

            </div>


            <div className="admin-operation-row">

              <div className="operation-icon">
                <CalendarDays size={17} />
              </div>

              <div>
                <strong>
                  Pending Reservations
                </strong>

                <span>
                  Reservations waiting for action
                </span>
              </div>

              <b>
                {dashboard?.pendingReservations ?? 0}
              </b>

            </div>


            <div className="admin-operation-row">

              <div className="operation-icon">
                <CircleDollarSign size={17} />
              </div>

              <div>
                <strong>
                  Unpaid Fines
                </strong>

                <span>
                  Outstanding member fines
                </span>
              </div>

              <b>
                {dashboard?.unpaidFines ?? 0}
              </b>

            </div>

          </div>

        </div>


        {/* QUICK ACTIONS */}

        <div className="admin-panel">

          <div className="admin-panel-header">

            <div>

              <div className="admin-panel-label">
                MANAGEMENT
              </div>

              <h2>
                Quick Actions
              </h2>

            </div>

          </div>


          <div className="admin-actions">

            <Link
              to="/admin/books"
              className="admin-action"
            >

              <div>
                <BookOpen size={18} />
              </div>

              <span>
                Manage Books
              </span>

              <ArrowRight size={16} />

            </Link>


            <Link
              to="/admin/users"
              className="admin-action"
            >

              <div>
                <Users size={18} />
              </div>

              <span>
                Manage Members
              </span>

              <ArrowRight size={16} />

            </Link>


            <Link
              to="/admin/borrowings"
              className="admin-action"
            >

              <div>
                <Clock3 size={18} />
              </div>

              <span>
                View Borrowings
              </span>

              <ArrowRight size={16} />

            </Link>


            <Link
              to="/admin/reservations"
              className="admin-action"
            >

              <div>
                <CalendarDays size={18} />
              </div>

              <span>
                Reservations
              </span>

              <ArrowRight size={16} />

            </Link>


            <Link
              to="/admin/fines"
              className="admin-action"
            >

              <div>
                <CircleDollarSign size={18} />
              </div>

              <span>
                Manage Fines
              </span>

              <ArrowRight size={16} />

            </Link>

          </div>

        </div>

      </section>


      {/* LIBRARY SUMMARY */}

      {library && (
        <section className="admin-library-summary">

          <div>

            <span>
              COLLECTION
            </span>

            <strong>
              {dashboard?.totalBooks ?? 0}
            </strong>

            <p>
              books in the SmartLib collection
            </p>

          </div>

          <div>

            <span>
              MEMBERS
            </span>

            <strong>
              {dashboard?.totalMembers ?? 0}
            </strong>

            <p>
              registered library members
            </p>

          </div>

          <div>

            <span>
              SYSTEM STATUS
            </span>

            <strong>
              Active
            </strong>

            <p>
              SmartLib library services online
            </p>

          </div>

        </section>
      )}

    </div>
  );
}

export default AdminDashboard;
