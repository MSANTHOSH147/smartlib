import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Eye, EyeOff, Loader2 } from "lucide-react";
import { login } from "../services/authService";

function Login() {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    email: "",
    password: "",
  });

  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

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

    if (!emailPattern.test(form.email)) {
      setError("Please enter a valid email address.");
      return;
    }

    if (!form.password) {
      setError("Please enter your password.");
      return;
    }

    try {
      setLoading(true);

      const user = await login(
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
        "Unable to sign in. Please check your email and password.";

      setError(message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-card">

      <div className="auth-heading">
        <h1>Welcome back</h1>

        <p>
          Sign in to access your SmartLib account.
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

        <div className="password-label-row">
          <label>
            Password
          </label>

          <Link to="/forgot-password">
            Forgot password?
          </Link>
        </div>

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
            placeholder="Enter your password"
            autoComplete="current-password"
            required
          />

          <button
            type="button"
            className="password-toggle"
            onClick={() =>
              setShowPassword(!showPassword)
            }
            aria-label={
              showPassword
                ? "Hide password"
                : "Show password"
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
              Signing in...
            </>
          ) : (
            "Sign in"
          )}
        </button>

      </form>

      <div className="auth-switch">
        Don't have an account?
        <Link to="/register">
          Create an account
        </Link>
      </div>

    </div>
  );
}

export default Login;
