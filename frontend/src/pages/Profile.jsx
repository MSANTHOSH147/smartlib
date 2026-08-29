import { useEffect, useState } from "react";
import {
  ArrowLeft,
  Check,
  LockKeyhole,
  LogOut,
  Mail,
  ShieldCheck,
  UserRound,
} from "lucide-react";
import { Link, useNavigate } from "react-router-dom";

import {
  changePassword,
  getCurrentUser,
  getStoredUser,
  logout,
} from "../services/authService";

import "./Profile.css";

function Profile() {
  const navigate = useNavigate();

  const [user, setUser] = useState(
    getStoredUser() || {}
  );

  const [loading, setLoading] = useState(false);
  const [savingPassword, setSavingPassword] = useState(false);

  const [currentPassword, setCurrentPassword] =
    useState("");

  const [newPassword, setNewPassword] =
    useState("");

  const [confirmPassword, setConfirmPassword] =
    useState("");

  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    loadUser();
  }, []);

  async function loadUser() {
    try {
      setLoading(true);

      const data = await getCurrentUser();

      setUser(data || {});
    } catch (err) {
      console.error("Profile loading error:", err);

      const stored = getStoredUser();

      if (stored) {
        setUser(stored);
      }
    } finally {
      setLoading(false);
    }
  }

  async function handlePasswordChange(event) {
    event.preventDefault();

    setMessage("");
    setError("");

    if (!currentPassword || !newPassword) {
      setError(
        "Please enter your current and new password."
      );
      return;
    }

    if (newPassword.length < 6) {
      setError(
        "New password must contain at least 6 characters."
      );
      return;
    }

    if (newPassword !== confirmPassword) {
      setError(
        "New password and confirmation do not match."
      );
      return;
    }

    try {
      setSavingPassword(true);

      await changePassword(
        currentPassword,
        newPassword
      );

      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");

      setMessage(
        "Your password has been changed successfully."
      );
    } catch (err) {
      console.error(
        "Password change error:",
        err
      );

      setError(
        err.response?.data?.message ||
          "Unable to change your password."
      );
    } finally {
      setSavingPassword(false);
    }
  }

  function handleLogout() {
    logout();

    navigate("/login", {
      replace: true,
    });
  }

  const displayName =
    user.name ||
    user.fullName ||
    "SmartLib Member";

  const email =
    user.email ||
    "No email available";

  const role =
    user.role ||
    "MEMBER";

  const initials = displayName
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join("")
    .toUpperCase();

  return (
    <div className="profile-page">

      {/* BACK */}

      <Link
        to="/dashboard"
        className="profile-back"
      >
        <ArrowLeft size={16} />
        Dashboard
      </Link>


      {/* HEADER */}

      <header className="profile-header">

        <div>

          <div className="profile-eyebrow">
            ACCOUNT
          </div>

          <h1>
            Profile
          </h1>

          <p>
            Manage your SmartLib account and security
            settings.
          </p>

        </div>

        <div className="profile-header-mark">
          <UserRound size={28} />
        </div>

      </header>


      {/* PROFILE HERO */}

      <section className="profile-hero">

        <div className="profile-avatar-large">
          {initials || "M"}
        </div>

        <div className="profile-identity">

          <h2>
            {displayName}
          </h2>

          <div className="profile-email">
            <Mail size={15} />
            {email}
          </div>

          <div className="profile-role">
            <ShieldCheck size={14} />
            {role}
          </div>

        </div>

        <div className="profile-status">

          <span className="profile-status-dot" />

          Account active

        </div>

      </section>


      {/* GRID */}

      <div className="profile-grid">


        {/* ACCOUNT INFORMATION */}

        <section className="profile-card">

          <div className="profile-card-header">

            <div className="profile-card-icon">
              <UserRound size={19} />
            </div>

            <div>

              <h2>
                Account Information
              </h2>

              <p>
                Your SmartLib account details.
              </p>

            </div>

          </div>


          <div className="profile-details">

            <div className="profile-detail">

              <span>
                Full name
              </span>

              <strong>
                {displayName}
              </strong>

            </div>


            <div className="profile-detail">

              <span>
                Email address
              </span>

              <strong>
                {email}
              </strong>

            </div>


            <div className="profile-detail">

              <span>
                Account role
              </span>

              <strong>
                {role}
              </strong>

            </div>

          </div>

        </section>


        {/* SECURITY */}

        <section className="profile-card">

          <div className="profile-card-header">

            <div className="profile-card-icon">
              <LockKeyhole size={19} />
            </div>

            <div>

              <h2>
                Security
              </h2>

              <p>
                Keep your account protected.
              </p>

            </div>

          </div>


          <form
            className="password-form"
            onSubmit={handlePasswordChange}
          >

            <label>
              Current password

              <input
                type="password"
                value={currentPassword}
                onChange={(event) =>
                  setCurrentPassword(
                    event.target.value
                  )
                }
                placeholder="Enter current password"
                autoComplete="current-password"
              />

            </label>


            <label>
              New password

              <input
                type="password"
                value={newPassword}
                onChange={(event) =>
                  setNewPassword(
                    event.target.value
                  )
                }
                placeholder="Enter new password"
                autoComplete="new-password"
              />

            </label>


            <label>
              Confirm new password

              <input
                type="password"
                value={confirmPassword}
                onChange={(event) =>
                  setConfirmPassword(
                    event.target.value
                  )
                }
                placeholder="Confirm new password"
                autoComplete="new-password"
              />

            </label>


            <button
              type="submit"
              className="profile-save-button"
              disabled={savingPassword}
            >

              {savingPassword ? (
                "Updating..."
              ) : (
                <>
                  <Check size={16} />
                  Update Password
                </>
              )}

            </button>

          </form>

        </section>

      </div>


      {/* MESSAGE */}

      {(message || error) && (

        <div
          className={
            message
              ? "profile-message"
              : "profile-error"
          }
        >

          {message || error}

        </div>

      )}


      {/* LOGOUT */}

      <section className="profile-danger">

        <div>

          <h2>
            Sign out
          </h2>

          <p>
            Sign out of your SmartLib account on this
            device.
          </p>

        </div>

        <button
          onClick={handleLogout}
          className="profile-logout"
        >
          <LogOut size={16} />
          Sign out
        </button>

      </section>

    </div>
  );
}

export default Profile;
