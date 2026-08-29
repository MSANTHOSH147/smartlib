import { NavLink, Outlet, Link } from "react-router-dom";
import {
  LayoutDashboard,
  BookOpen,
  Users,
  Clock3,
  CalendarDays,
  CircleDollarSign,
  ArrowLeft,
  LogOut,
  ScanLine,
} from "lucide-react";

import "./AdminLayout.css";

function AdminLayout() {
  const navItems = [
    {
      path: "/admin/dashboard",
      label: "Dashboard",
      icon: LayoutDashboard,
    },
    {
      path: "/admin/books",
      label: "Books",
      icon: BookOpen,
    },
    {
    path: "/admin/qr-scanner",
    label: "Scan QR",
    icon: ScanLine,
  },
    {
      path: "/admin/users",
      label: "Members",
      icon: Users,
    },
    {
      path: "/admin/borrowings",
      label: "Borrowings",
      icon: Clock3,
    },
    {
      path: "/admin/reservations",
      label: "Reservations",
      icon: CalendarDays,
    },
    {
      path: "/admin/fines",
      label: "Fines",
      icon: CircleDollarSign,
    },
  ];

  return (
    <div className="admin-shell">

      {/* SIDEBAR */}
      <aside className="admin-sidebar">

        <div className="admin-brand">
          <div className="admin-brand-mark">
            S
          </div>

          <div>
            <strong>SMARTLIB</strong>
            <span>ADMINISTRATION</span>
          </div>
        </div>

        <div className="admin-nav-label">
          MANAGEMENT
        </div>

        <nav className="admin-nav">

          {navItems.map((item) => {
            const Icon = item.icon;

            return (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) =>
                  `admin-nav-link ${
                    isActive ? "active" : ""
                  }`
                }
              >
                <Icon size={18} />

                <span>
                  {item.label}
                </span>
              </NavLink>
            );
          })}

        </nav>

        <div className="admin-sidebar-bottom">

          <Link
            to="/dashboard"
            className="admin-back-library"
          >
            <ArrowLeft size={17} />
            <span>Back to Library</span>
          </Link>

          <button
            className="admin-logout"
            type="button"
            onClick={() => {
              localStorage.removeItem("token");
              localStorage.removeItem("user");
              window.location.href = "/login";
            }}
          >
            <LogOut size={17} />
            <span>Logout</span>
          </button>

        </div>

      </aside>


      {/* MAIN */}
      <main className="admin-main">
        <Outlet />
      </main>

    </div>
  );
}

export default AdminLayout;