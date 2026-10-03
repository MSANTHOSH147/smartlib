import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080/api",
  timeout: 30000, // 30-second timeout allows Render cold-start without hanging indefinitely
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("smartlib_token");

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.code === "ECONNABORTED" || (error.message && error.message.includes("timeout"))) {
      error.userMessage = "Request timed out. The server may be starting up — please try again.";
    }
    return Promise.reject(error);
  }
);

export default api;
