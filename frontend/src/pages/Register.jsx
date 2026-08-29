import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Eye, EyeOff, Loader2 } from "lucide-react";
import { register } from "../services/authService";

function Register() {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
  });

  const [showPassword, setShowPassword] =
    useState(false);

  const [loading, setLoading] =
    useState(false);

  const [error, setError] =
    useState("");

  function handleChange(event) {
    setForm({
      ...form,
      [event.target.name]: event.target.value,
    });

    setError("");
  }

  async function handleSubmit(event) {
    event.preventDefault();

    const emailPattern =
      /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

    if (!form.name.trim()) {
      setError("Please enter your name.");
      return;
    }

    if (!emailPattern.test(form.email)) {
      setError(
        "Please enter a valid email address."
      );
      return;
    }

    if (form.password.length < 6) {
      setError(
        "Password must contain at least 6 characters."
      );
      return;
    }

    try {
      setLoading(true);

      const user = await register(
        form.name.trim(),
        form.email.trim(),
        form.password
      );

      if (user.role === "ADMIN") {
        navigate("/admin/dashboard", {
          replace: true,
        });
      } else {
        navigate("/dashboard", {
          replace: true,
        });
      }
    } catch (err) {
      const message =
        err.response?.data?.message ||
        err.response?.data?.error ||
        "Unable to create your account.";

      setError(message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-card">

      <div className="auth-heading">
        <h1>Create your account</h1>

        <p>
          Join SmartLib and manage your library
          account in one place.
        </p>
      </div>

      {error && (
        <div className="auth-error">
          {error}
        </div>
      )}

      <form
        className="auth-form"
        onSubmit={handleSubmit}
      >

        <label>
          Full name
          <input
            type="text"
            name="name"
            value={form.name}
            onChange={handleChange}
            placeholder="Your full name"
            autoComplete="name"
            required
          />
        </label>

        <label>
          Email address
          <input
            type="email"
            name="email"
            value={form.email}
            onChange={handleChange}
            placeholder="you@example.com"
            autoComplete="email"
            required
          />
        </label>

        <label>
          Password
        </label>

        <div className="password-field">

          <input
            type={
              showPassword
                ? "text"
                : "password"
            }
            name="password"
            value={form.password}
            onChange={handleChange}
            placeholder="At least 6 characters"
            autoComplete="new-password"
            required
          />

          <button
            type="button"
            className="password-toggle"
            onClick={() =>
              setShowPassword(!showPassword)
            }
          >
            {showPassword ? (
              <EyeOff size={18} />
            ) : (
              <Eye size={18} />
            )}
          </button>

        </div>

        <button
          className="auth-submit"
          type="submit"
          disabled={loading}
        >
          {loading ? (
            <>
              <Loader2
                size={18}
                className="spin"
              />
              Creating account...
            </>
          ) : (
            "Create account"
          )}
        </button>

      </form>

      <div className="auth-switch">
        Already have an account?
        <Link to="/login">
          Sign in
        </Link>
      </div>

    </div>
  );
}

export default Register;
