import { Outlet } from "react-router-dom";

function AuthLayout() {
  return (
    <div className="auth-shell">
      <div className="auth-brand">
        <div className="auth-brand-mark">S</div>
        <span>SmartLib</span>
      </div>

      <main className="auth-content">
        <Outlet />
      </main>

      <footer className="auth-footer">
        SmartLib Library Management System
      </footer>
    </div>
  );
}

export default AuthLayout;
