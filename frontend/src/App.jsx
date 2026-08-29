import {
  BrowserRouter,
  Navigate,
  Route,
  Routes,
} from "react-router-dom";


// ============================================================
// LAYOUTS
// ============================================================

import AuthLayout from "./layouts/AuthLayout";
import ProtectedLayout from "./layouts/ProtectedLayout";
import AdminLayout from "./layouts/AdminLayout";


// ============================================================
// AUTH PAGES
// ============================================================

import Login from "./pages/Login";
import Register from "./pages/Register";


// ============================================================
// MEMBER PAGES
// ============================================================

import MemberDashboard from "./pages/member/Dashboard";

import BookList from "./pages/BookList";
import BookDetail from "./pages/BookDetail";

import MyBorrowings from "./pages/MyBorrowings";

import Reservations from "./pages/Reservations";

import Fines from "./pages/Fines";

import Profile from "./pages/Profile";

import QRScanner from "./pages/QRScanner";


// ============================================================
// ADMIN PAGES
// ============================================================

import AdminDashboard from "./pages/admin/Dashboard";

import AdminBooks from "./pages/admin/Books";

import UsersPage from "./pages/admin/Users";

import AdminBorrowings from "./pages/admin/Borrowings";

import AdminReservations from "./pages/admin/Reservations";

import AdminFines from "./pages/admin/Fines";


// ============================================================
// TEST PAGE
// ============================================================

import TestQR from "./pages/TestQR";


// ============================================================
// PLACEHOLDER
// ============================================================

function Placeholder({ title }) {

  return (

    <div
      style={{
        padding: "48px",
        fontFamily:
          "Inter, Arial, sans-serif",
      }}
    >

      <h1>
        {title}
      </h1>

      <p>
        This SmartLib page is ready
        to be connected.
      </p>

    </div>

  );

}


// ============================================================
// APP
// ============================================================

function App() {

  return (

    <BrowserRouter>

      <Routes>


        {/* ====================================================
            PUBLIC AUTHENTICATION
        ==================================================== */}

        <Route element={<AuthLayout />}>

          <Route
            path="/login"
            element={<Login />}
          />

          <Route
            path="/register"
            element={<Register />}
          />

          <Route
            path="/forgot-password"
            element={
              <Placeholder
                title="Forgot Password"
              />
            }
          />

          <Route
            path="/reset-password"
            element={
              <Placeholder
                title="Reset Password"
              />
            }
          />

        </Route>


        {/* ====================================================
            MEMBER APPLICATION
        ==================================================== */}

        <Route element={<ProtectedLayout />}>


          {/* DASHBOARD */}

          <Route
            path="/dashboard"
            element={
              <MemberDashboard />
            }
          />


          {/* BOOKS */}

          <Route
            path="/books"
            element={
              <BookList />
            }
          />


          {/* BOOK DETAIL */}

          <Route
            path="/books/:id"
            element={
              <BookDetail />
            }
          />


          {/* QR SCANNER */}

          <Route
            path="/qr-scanner"
            element={
              <QRScanner />
            }
          />


          {/* BORROWINGS */}

          <Route
            path="/borrowings"
            element={
              <MyBorrowings />
            }
          />


          {/* RESERVATIONS */}

          <Route
            path="/reservations"
            element={
              <Reservations />
            }
          />


          {/* FINES */}

          <Route
            path="/fines"
            element={
              <Fines />
            }
          />


          {/* PROFILE */}

          <Route
            path="/profile"
            element={
              <Profile />
            }
          />

        </Route>


        {/* ====================================================
            ADMIN APPLICATION
        ==================================================== */}

        <Route element={<AdminLayout />}>


          {/* ADMIN DASHBOARD */}

          <Route
            path="/admin/dashboard"
            element={
              <AdminDashboard />
            }
          />


          {/* ADMIN BOOKS */}

          <Route
            path="/admin/books"
            element={
              <AdminBooks />
            }
          />

          {/* ====================================================
    ADMIN QR SCANNER
==================================================== */}

<Route
  path="/admin/qr-scanner"
  element={
    <QRScanner />
  }
/>


          {/* ADMIN USERS */}

          <Route
            path="/admin/users"
            element={
              <UsersPage />
            }
          />


          {/* ADMIN BORROWINGS */}

          <Route
            path="/admin/borrowings"
            element={
              <AdminBorrowings />
            }
          />


          {/* ADMIN RESERVATIONS */}

          <Route
            path="/admin/reservations"
            element={
              <AdminReservations />
            }
          />


          {/* ADMIN FINES */}

          <Route
            path="/admin/fines"
            element={
              <AdminFines />
            }
          />


          {/* TEST QR */}

          <Route
            path="/test-qr"
            element={
              <TestQR />
            }
          />

        </Route>


        {/* ====================================================
            DEFAULT
        ==================================================== */}

        <Route
          path="/"
          element={
            <Navigate
              to="/dashboard"
              replace
            />
          }
        />


        {/* ====================================================
            UNKNOWN ROUTES
        ==================================================== */}

        <Route
          path="*"
          element={
            <Navigate
              to="/login"
              replace
            />
          }
        />

      </Routes>

    </BrowserRouter>

  );

}


export default App;