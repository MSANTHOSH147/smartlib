import { useState } from "react";
import { Sparkles, MessageSquare, X } from "lucide-react";
import AIAssistant from "./AIAssistant";
import "./AIAssistant.css";

export default function AIFloatingWidget() {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <aside className="sl-ai-floating-container" aria-label="SmartLib AI Floating Assistant">
      {isOpen && (
        <div className="sl-ai-floating-popup">
          <AIAssistant isEmbedded={false} onClose={() => setIsOpen(false)} />
        </div>
      )}

      <button
        type="button"
        className="sl-ai-floating-trigger"
        onClick={() => setIsOpen((prev) => !prev)}
        aria-expanded={isOpen}
        aria-label={isOpen ? "Close AI Assistant" : "Open SmartLib AI Assistant"}
        title={isOpen ? "Close AI Assistant" : "Ask SmartLib AI"}
      >
        {isOpen ? (
          <>
            <X size={18} />
            <span>Close AI</span>
          </>
        ) : (
          <>
            <Sparkles size={18} />
            <span>Ask SmartLib AI</span>
          </>
        )}
      </button>
    </aside>
  );
}
