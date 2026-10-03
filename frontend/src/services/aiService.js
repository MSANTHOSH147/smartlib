import api from "./api";

/**
 * Service for communicating with the SmartLib AI Assistant API.
 * Endpoint: POST /api/ai/chat
 */
export async function sendChatMessage(message, history = []) {
  if (!message || !message.trim()) {
    throw new Error("Message cannot be empty.");
  }

  // Format and bound history payload to avoid oversized requests
  const formattedHistory = (Array.isArray(history) ? history : [])
    .slice(-16) // Keep last 16 messages max from client
    .map((item) => ({
      role: item.role === "assistant" ? "model" : item.role,
      content: item.content || item.text || "",
    }))
    .filter((item) => item.role && item.content.trim());

  try {
    const response = await api.post("/ai/chat", {
      message: message.trim(),
      history: formattedHistory,
    });

    if (response.data && response.data.success === false) {
      throw new Error(
        response.data.reply ||
          "SmartLib AI is temporarily unavailable. Please try again in a moment."
      );
    }

    return response.data;
  } catch (error) {
    if (error.response) {
      const status = error.response.status;
      const backendMessage = error.response.data?.message || error.response.data?.reply;

      switch (status) {
        case 400:
          throw new Error(
            backendMessage || "Please check your message and try again."
          );
        case 401:
          throw new Error(
            "Your session has expired. Please sign in again."
          );
        case 403:
          throw new Error(
            "Access denied. Please check your permissions."
          );
        case 429:
          throw new Error(
            "You've reached the AI request limit. Please try again shortly."
          );
        case 503:
          throw new Error(
            backendMessage ||
              "SmartLib AI is temporarily unavailable. Please try again in a moment."
          );
        case 500:
          throw new Error(
            "SmartLib AI is temporarily unavailable. Please try again."
          );
        default:
          throw new Error(
            backendMessage ||
              "SmartLib AI encountered an unexpected issue. Please try again."
          );
      }
    } else if (error.code === "ECONNABORTED" || (error.message && error.message.includes("timeout"))) {
      throw new Error(
        "Request timed out while waiting for SmartLib AI. Please try again."
      );
    } else if (error.request) {
      throw new Error(
        "Unable to reach SmartLib AI. Check your connection and try again."
      );
    } else {
      throw new Error(error.message || "Failed to send message to SmartLib AI.");
    }
  }
}

export async function getUserMemories() {
  try {
    const response = await api.get("/ai/memory");
    return response.data || [];
  } catch (error) {
    console.error("Failed to load user memories", error);
    return [];
  }
}

export async function deleteUserMemory(id) {
  try {
    const response = await api.delete(`/ai/memory/${id}`);
    return response.data;
  } catch (error) {
    console.error("Failed to delete user memory", error);
    throw error;
  }
}

export default {
  sendChatMessage,
  getUserMemories,
  deleteUserMemory,
};
