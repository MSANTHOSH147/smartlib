import { useState, useRef, useEffect } from "react";
import {
  Send,
  Sparkles,
  RotateCcw,
  Loader2,
  AlertCircle,
  BookOpen,
  HelpCircle,
  Compass,
  CalendarDays,
  X,
  Minus,
  Globe,
  ExternalLink,
  Brain,
  Trash2,
  Check,
} from "lucide-react";
import { sendChatMessage, getUserMemories, deleteUserMemory } from "../../services/aiService";
import "./AIAssistant.css";

const QUICK_PROMPTS = [
  {
    icon: BookOpen,
    label: "Do we have Clean Code?",
    prompt: "Do we have Clean Code in SmartLib and is it available?",
  },
  {
    icon: Compass,
    label: "Personalized Recommendations",
    prompt: "Recommend books based on my borrowing history.",
  },
  {
    icon: CalendarDays,
    label: "My Overdue Books",
    prompt: "What books do I currently have overdue?",
  },
  {
    icon: BookOpen,
    label: "Is 1984 available?",
    prompt: "Is 1984 available in the library right now?",
  },
  {
    icon: HelpCircle,
    label: "Who wrote The Great Gatsby?",
    prompt: "Who wrote The Great Gatsby and what is it about?",
  },
  {
    icon: Sparkles,
    label: "Books by George Orwell",
    prompt: "What books by George Orwell are in SmartLib?",
  },
];

const TOOL_FRIENDLY_LABELS = {
  searchBooks: "Searched SmartLib catalog",
  semanticSearchBooks: "Searched library catalog semantically",
  getBookDetails: "Looked up book details",
  checkBookAvailability: "Checked book availability & shelf location",
  getSimilarBooks: "Found similar titles",
  getMyBorrowings: "Checked your borrowings",
  getMyOverdueBooks: "Checked your overdue books",
  getMyReservations: "Checked your reservations",
  getMyFines: "Checked your fines & balances",
  getPersonalizedRecommendations: "Analyzed your reading history",
  getMyMemories: "Checked your remembered preferences",
  rememberPreference: "Saved your preference",
  forgetMyMemory: "Forgot preference from memory",
};

export default function AIAssistant({
  isEmbedded = false,
  onClose,
  initialPrompt = "",
}) {
  const [messages, setMessages] = useState([]);
  const [inputValue, setInputValue] = useState(initialPrompt);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [showMemoryModal, setShowMemoryModal] = useState(false);
  const [memories, setMemories] = useState([]);
  const [loadingMemories, setLoadingMemories] = useState(false);
  const [memoryActionStatus, setMemoryActionStatus] = useState("");

  const messagesEndRef = useRef(null);
  const textareaRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, loading]);

  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = "auto";
      textareaRef.current.style.height = `${Math.min(
        textareaRef.current.scrollHeight,
        140
      )}px`;
    }
  }, [inputValue]);

  const handleSendMessage = async (textToSend) => {
    const query = (textToSend !== undefined ? textToSend : inputValue).trim();
    if (!query || loading) return;

    if (query.length > 1000) {
      setErrorMessage("Message must not exceed 1000 characters.");
      return;
    }

    setErrorMessage("");
    const userMessage = {
      id: Date.now().toString(),
      role: "user",
      content: query,
      timestamp: new Date().toLocaleTimeString([], {
        hour: "2-digit",
        minute: "2-digit",
      }),
    };

    // Keep history bounded to last 16 messages
    const nextHistory = [...messages, userMessage].slice(-16);
    setMessages((prev) => [...prev, userMessage]);
    setInputValue("");
    setLoading(true);

    try {
      // Send chat request to backend
      const result = await sendChatMessage(query, messages);

      const assistantMessage = {
        id: (Date.now() + 1).toString(),
        role: "model",
        content:
          result.reply ||
          "I received an empty response. Please try rephrasing your question.",
        success: result.success !== false,
        toolsExecuted: result.toolsExecuted || [],
        sources: Array.isArray(result.sources) ? result.sources : [],
        timestamp: new Date().toLocaleTimeString([], {
          hour: "2-digit",
          minute: "2-digit",
        }),
      };

      setMessages((prev) => [...prev, assistantMessage]);
    } catch (err) {
      console.error("AI Assistant error:", err);
      setErrorMessage(
        err.message ||
          "Unable to communicate with SmartLib AI. Please try again."
      );
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  const handleClearConversation = () => {
    if (loading) return;
    setMessages([]);
    setErrorMessage("");
    setInputValue("");
  };

  const loadMemories = async () => {
    setLoadingMemories(true);
    setMemoryActionStatus("");
    try {
      const data = await getUserMemories();
      setMemories(data);
    } catch (err) {
      console.error("Failed to fetch memories", err);
    } finally {
      setLoadingMemories(false);
    }
  };

  const handleToggleMemory = () => {
    if (!showMemoryModal) {
      loadMemories();
    }
    setShowMemoryModal((prev) => !prev);
  };

  const handleDeleteMemory = async (id) => {
    try {
      await deleteUserMemory(id);
      setMemories((prev) => prev.filter((m) => m.id !== id));
      setMemoryActionStatus("Preference forgotten");
      setTimeout(() => setMemoryActionStatus(""), 3000);
    } catch (err) {
      console.error("Failed to delete memory", err);
    }
  };

  const charCount = inputValue.length;
  const isOverLimit = charCount > 1000;

  return (
    <div
      className={`sl-ai-assistant ${
        isEmbedded ? "sl-ai-embedded" : "sl-ai-panel"
      }`}
      role="region"
      aria-label="SmartLib AI Assistant"
    >
      {/* HEADER */}
      <header className="sl-ai-header">
        <div className="sl-ai-header-info">
          <div className="sl-ai-avatar">
            <Sparkles size={18} className="sl-ai-sparkle-icon" />
          </div>
          <div>
            <h2 className="sl-ai-title">SmartLib AI Assistant</h2>
            <div className="sl-ai-status">
              <span className="sl-ai-status-dot" aria-hidden="true" />
              <span>Library Intelligence & Catalog</span>
            </div>
          </div>
        </div>

        <div className="sl-ai-header-actions">
          <button
            type="button"
            className={`sl-ai-action-btn ${showMemoryModal ? "sl-ai-btn-active" : ""}`}
            onClick={handleToggleMemory}
            title="Saved Preferences & Memory"
            aria-label="Saved Preferences & Memory"
          >
            <Brain size={16} />
            <span className="sl-ai-action-label">Memory</span>
          </button>

          {messages.length > 0 && (
            <button
              type="button"
              className="sl-ai-action-btn"
              onClick={handleClearConversation}
              title="Clear conversation"
              aria-label="Clear conversation"
              disabled={loading}
            >
              <RotateCcw size={16} />
              <span className="sl-ai-action-label">Clear</span>
            </button>
          )}

          {!isEmbedded && onClose && (
            <button
              type="button"
              className="sl-ai-action-btn sl-ai-close-btn"
              onClick={onClose}
              title="Close Assistant"
              aria-label="Close Assistant"
            >
              <X size={18} />
            </button>
          )}
        </div>
      </header>

      {/* USER MEMORY MODAL / DRAWER */}
      {showMemoryModal && (
        <div className="sl-ai-memory-overlay" role="dialog" aria-modal="true" aria-label="Saved Preferences">
          <div className="sl-ai-memory-modal">
            <div className="sl-ai-memory-header">
              <div className="sl-ai-memory-title-wrap">
                <Brain size={18} className="sl-ai-memory-icon" />
                <h3 className="sl-ai-memory-title">Preferences & Memory</h3>
              </div>
              <button
                type="button"
                className="sl-ai-action-btn sl-ai-close-btn"
                onClick={() => setShowMemoryModal(false)}
                aria-label="Close preferences modal"
              >
                <X size={16} />
              </button>
            </div>
            <p className="sl-ai-memory-desc">
              SmartLib AI remembers your favorite genres, authors, and reading preferences across conversations to personalize recommendations. You have complete control to inspect or forget them.
            </p>

            {memoryActionStatus && (
              <div className="sl-ai-memory-status-alert">
                <Check size={14} />
                <span>{memoryActionStatus}</span>
              </div>
            )}

            <div className="sl-ai-memory-list">
              {loadingMemories ? (
                <div className="sl-ai-memory-loading">
                  <Loader2 size={18} className="sl-spinner" />
                  <span>Loading remembered preferences...</span>
                </div>
              ) : memories.length === 0 ? (
                <div className="sl-ai-memory-empty">
                  <p>No remembered preferences yet.</p>
                  <span>Mention favorite authors, topics, or reading styles in conversation and they will appear here.</span>
                </div>
              ) : (
                memories.map((m) => (
                  <div key={m.id} className="sl-ai-memory-item">
                    <div className="sl-ai-memory-info">
                      <span className="sl-ai-memory-type-badge">
                        {m.memoryType || "PREFERENCE"}
                      </span>
                      <div className="sl-ai-memory-val-wrap">
                        <strong className="sl-ai-memory-key">{m.key?.replace(/_/g, " ")}:</strong>{" "}
                        <span className="sl-ai-memory-val">{m.value}</span>
                      </div>
                    </div>
                    <button
                      type="button"
                      className="sl-ai-memory-forget-btn"
                      onClick={() => handleDeleteMemory(m.id)}
                      title="Forget this preference"
                      aria-label={`Forget ${m.key}`}
                    >
                      <Trash2 size={13} />
                      <span>Forget</span>
                    </button>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      )}

      {/* CONVERSATION BODY */}
      <div
        className="sl-ai-messages"
        aria-live="polite"
        role="log"
        aria-label="Conversation history"
      >
        {messages.length === 0 ? (
          <div className="sl-ai-empty-state">
            <div className="sl-ai-empty-badge">
              <Sparkles size={20} />
            </div>
            <h3 className="sl-ai-empty-title">How can I help you today?</h3>
            <p className="sl-ai-empty-subtitle">
              Ask about library catalog books, current physical availability,
              your borrowings and fines, or general literature and authors.
            </p>

            <div className="sl-ai-quick-prompts-grid">
              {QUICK_PROMPTS.map((item, index) => {
                const IconComponent = item.icon;
                return (
                  <button
                    key={index}
                    type="button"
                    className="sl-ai-quick-btn"
                    onClick={() => handleSendMessage(item.prompt)}
                    disabled={loading}
                  >
                    <IconComponent size={15} className="sl-ai-quick-icon" />
                    <span>{item.label}</span>
                  </button>
                );
              })}
            </div>
          </div>
        ) : (
          <div className="sl-ai-message-list">
            {messages.map((msg) => (
              <div
                key={msg.id}
                className={`sl-ai-message-row ${
                  msg.role === "user" ? "sl-user-row" : "sl-bot-row"
                }`}
              >
                {msg.role === "model" && (
                  <div className="sl-msg-avatar" aria-hidden="true">
                    <Sparkles size={14} />
                  </div>
                )}

                <div
                  className={`sl-ai-bubble ${
                    msg.role === "user" ? "sl-user-bubble" : "sl-bot-bubble"
                  }`}
                >
                  <div className="sl-ai-text">{msg.content}</div>

                  {/* TOOL EXECUTION SIGNALS */}
                  {msg.toolsExecuted && msg.toolsExecuted.length > 0 && (
                    <div
                      className="sl-ai-tools-list"
                      aria-label="Library tools used"
                    >
                      {msg.toolsExecuted.map((toolName, idx) => (
                        <span key={idx} className="sl-ai-tool-pill">
                          <span className="sl-ai-pill-dot" />
                          {TOOL_FRIENDLY_LABELS[toolName] ||
                            "Retrieved library data"}
                        </span>
                      ))}
                    </div>
                  )}

                  {/* WEB SOURCES & CITATIONS */}
                  {msg.sources && msg.sources.length > 0 && (
                    <div
                      className="sl-ai-sources-section"
                      aria-label="Web sources and citations"
                    >
                      <div className="sl-ai-sources-header">
                        <Globe size={13} className="sl-ai-sources-icon" />
                        <span className="sl-ai-sources-title">Sources</span>
                      </div>
                      <div className="sl-ai-sources-list">
                        {msg.sources.map((source, idx) => {
                          if (!source || !source.url) return null;
                          const displayName =
                            source.title && source.title.trim()
                              ? source.title.trim()
                              : source.domain || "Web Source";
                          return (
                            <a
                              key={idx}
                              href={source.url}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="sl-ai-source-link"
                              title={`${displayName} (${source.url})`}
                            >
                              <span className="sl-ai-source-bullet" aria-hidden="true">•</span>
                              <span className="sl-ai-source-name">{displayName}</span>
                              {source.domain && (
                                <span className="sl-ai-source-badge">
                                  {source.domain}
                                </span>
                              )}
                              <ExternalLink size={10} className="sl-ai-source-ext-icon" aria-hidden="true" />
                            </a>
                          );
                        })}
                      </div>
                    </div>
                  )}

                  <div className="sl-ai-meta">
                    <span className="sl-ai-time">{msg.timestamp}</span>
                  </div>
                </div>
              </div>
            ))}

            {loading && (
              <div className="sl-ai-message-row sl-bot-row">
                <div className="sl-msg-avatar" aria-hidden="true">
                  <Sparkles size={14} />
                </div>
                <div className="sl-ai-bubble sl-bot-bubble sl-ai-typing">
                  <div className="sl-ai-typing-indicator" aria-label="Thinking">
                    <span />
                    <span />
                    <span />
                  </div>
                  <span className="sl-ai-typing-text">
                    Consulting SmartLib catalog...
                  </span>
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>
        )}
      </div>

      {/* ERROR BANNER */}
      {errorMessage && (
        <div className="sl-ai-error-banner" role="alert">
          <AlertCircle size={16} className="sl-ai-error-icon" />
          <span className="sl-ai-error-text">{errorMessage}</span>
          <button
            type="button"
            className="sl-ai-error-dismiss"
            onClick={() => setErrorMessage("")}
            aria-label="Dismiss error"
          >
            <X size={14} />
          </button>
        </div>
      )}

      {/* INPUT AREA */}
      <footer className="sl-ai-footer">
        <form
          className="sl-ai-form"
          onSubmit={(e) => {
            e.preventDefault();
            handleSendMessage();
          }}
        >
          <div className="sl-ai-input-wrap">
            <textarea
              ref={textareaRef}
              className={`sl-ai-textarea ${isOverLimit ? "sl-input-error" : ""}`}
              placeholder="Ask about books, availability, your borrowings, fines..."
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              onKeyDown={handleKeyDown}
              disabled={loading}
              rows={1}
              aria-label="Chat message input"
              maxLength={1050}
            />

            <div className="sl-ai-input-footer">
              <span
                className={`sl-ai-counter ${
                  isOverLimit || charCount > 900 ? "sl-counter-warn" : ""
                }`}
              >
                {charCount} / 1000
              </span>

              <button
                type="submit"
                className="sl-ai-send-btn"
                disabled={!inputValue.trim() || loading || isOverLimit}
                aria-label="Send message"
                title="Send message (Enter)"
              >
                {loading ? (
                  <Loader2 size={16} className="sl-spinner" />
                ) : (
                  <Send size={16} />
                )}
              </button>
            </div>
          </div>
        </form>
      </footer>
    </div>
  );
}
