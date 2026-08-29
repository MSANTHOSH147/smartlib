import api from "./api";

const TOKEN_KEY = "smartlib_token";
const USER_KEY = "smartlib_user";

export async function login(email, password) {
  const response = await api.post("/auth/login", {
    email,
    password,
  });

  const data = response.data;

  localStorage.setItem(TOKEN_KEY, data.token);
  localStorage.setItem(USER_KEY, JSON.stringify(data));

  return data;
}

export async function register(name, email, password) {
  const response = await api.post("/auth/register", {
    name,
    email,
    password,
  });

  const data = response.data;

  localStorage.setItem(TOKEN_KEY, data.token);
  localStorage.setItem(USER_KEY, JSON.stringify(data));

  return data;
}

export async function getCurrentUser() {
  const response = await api.get("/auth/me");

  const user = response.data;

  const existing = JSON.parse(
    localStorage.getItem(USER_KEY) || "{}"
  );

  const updatedUser = {
    ...existing,
    ...user,
  };

  localStorage.setItem(
    USER_KEY,
    JSON.stringify(updatedUser)
  );

  return updatedUser;
}

export async function changePassword(
  currentPassword,
  newPassword
) {
  const response = await api.post(
    "/auth/change-password",
    {
      currentPassword,
      newPassword,
    }
  );

  return response.data;
}

export async function forgotPassword(email) {
  const response = await api.post(
    "/auth/forgot-password",
    {
      email,
    }
  );

  return response.data;
}

export async function resetPassword(
  token,
  newPassword
) {
  const response = await api.post(
    "/auth/reset-password",
    {
      token,
      newPassword,
    }
  );

  return response.data;
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function getStoredUser() {
  try {
    return JSON.parse(
      localStorage.getItem(USER_KEY) || "null"
    );
  } catch {
    return null;
  }
}

export function isAuthenticated() {
  return Boolean(getToken());
}

export function logout() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}
