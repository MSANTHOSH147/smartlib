import {
  NavLink,
  Navigate,
  Outlet,
  useNavigate,
} from "react-router-dom";

import {
  LayoutDashboard,
  BookOpen,
  Library,
  CalendarDays,
  WalletCards,
  UserRound,
  ScanLine,
  LogOut,
  Menu,
  X,
  Sparkles,
} from "lucide-react";

import { useState } from "react";

import {
  isAuthenticated,
  logout,
} from "../services/authService";

import AIFloatingWidget from "../components/AIAssistant/AIFloatingWidget";
import "./ProtectedLayout.css";


function ProtectedLayout() {

  const navigate = useNavigate();

  const [mobileOpen, setMobileOpen] = useState(false);


  // ============================================================
  // AUTH CHECK
  // ============================================================

  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }


  // ============================================================
  // MEMBER NAVIGATION
  // ============================================================

  const navigation = [

    {
      label: "Dashboard",
      path: "/dashboard",
      icon: LayoutDashboard,
    },

    {
      label: "Browse Books",
      path: "/books",
      icon: BookOpen,
    },

    {
      label: "AI Assistant",
      path: "/ai",
      icon: Sparkles,
    },

    {
      label: "Scan QR",
      path: "/qr-scanner",
      icon: ScanLine,
    },

    {
      label: "My Borrowings",
      path: "/borrowings",
      icon: Library,
    },

    {
      label: "Reservations",
      path: "/reservations",
      icon: CalendarDays,
    },

    {
      label: "Fines",
      path: "/fines",
      icon: WalletCards,
    },

    {
      label: "Profile",
      path: "/profile",
      icon: UserRound,
    },

  ];


  // ============================================================
  // LOGOUT
  // ============================================================

  const handleLogout = () => {

    logout();

    navigate("/login");

  };


  // ============================================================
  // RENDER
  // ============================================================

  return (

    <div className="protected-shell">


      {/* ======================================================
          MOBILE HEADER
      ====================================================== */}

      <header className="mobile-header">

        <button
          className="mobile-menu-button"
          onClick={() =>
            setMobileOpen(!mobileOpen)
          }
          aria-label="Toggle navigation"
        >

          {mobileOpen ? (
            <X size={21} />
          ) : (
            <Menu size={21} />
          )}

        </button>


        <div className="mobile-brand">

          <span>SMART</span>
          LIB

        </div>

      </header>


      {/* ======================================================
          SIDEBAR
      ====================================================== */}

      <aside
        className={`smartlib-sidebar ${
          mobileOpen
            ? "mobile-open"
            : ""
        }`}
      >


        {/* BRAND */}

        <div className="sidebar-brand">

          <div className="brand-mark">
            SL
          </div>


          <div>

            <div className="brand-name">
              SmartLib
            </div>

            <div className="brand-caption">
              LIBRARY SYSTEM
            </div>

          </div>

        </div>


        {/* SECTION */}

        <div className="sidebar-section-label">
          LIBRARY
        </div>


        {/* NAVIGATION */}

        <nav className="sidebar-navigation">

          {navigation.map((item) => {

            const Icon = item.icon;

            return (

              <NavLink
                key={item.path}
                to={item.path}
                onClick={() =>
                  setMobileOpen(false)
                }
                className={({ isActive }) =>
                  `sidebar-link ${
                    isActive
                      ? "active"
                      : ""
                  }`
                }
              >

                <Icon
                  size={18}
                  strokeWidth={1.8}
                />

                <span>
                  {item.label}
                </span>

              </NavLink>

            );

          })}

        </nav>


        {/* ====================================================
            BOTTOM ACCOUNT
        ==================================================== */}

        <div className="sidebar-bottom">

          <div className="sidebar-divider" />


          <div className="sidebar-account">

            <div className="account-avatar">
              S
            </div>


            <div className="account-info">

              <strong>
                SmartLib Member
              </strong>

              <span>
                Library account
              </span>

            </div>

          </div>


          <button
            className="sidebar-logout"
            type="button"
            onClick={handleLogout}
          >

            <LogOut size={17} />

            <span>
              Sign out
            </span>

          </button>

        </div>

      </aside>


      {/* ======================================================
          MOBILE OVERLAY
      ====================================================== */}

      {mobileOpen && (

        <div
          className="sidebar-overlay"
          onClick={() =>
            setMobileOpen(false)
          }
        />

      )}


      {/* ======================================================
          PAGE CONTENT
      ====================================================== */}

      <main className="protected-content">

        <Outlet />

      </main>

      <AIFloatingWidget />

    </div>

  );

}


export default ProtectedLayout;