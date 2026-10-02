/**
 * SmartLib Model Context Protocol (MCP) Server
 *
 * Exposes read-only SmartLib library operations, safe catalog resources,
 * and reusable prompts to AI assistants via the standard MCP stdio protocol.
 *
 * Authoritative backend REST APIs remain the single source of truth.
 * All personal tools derive identity strictly from authenticated credentials.
 */

import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
  ListResourcesRequestSchema,
  ReadResourceRequestSchema,
  ListPromptsRequestSchema,
  GetPromptRequestSchema,
  ErrorCode,
  McpError
} from "@modelcontextprotocol/sdk/types.js";

// Configuration: Supports SMARTLIB_API_BASE_URL or SMARTLIB_BACKEND_URL with local fallback
const BACKEND_URL = process.env.SMARTLIB_API_BASE_URL || process.env.SMARTLIB_BACKEND_URL || "http://localhost:8080";
const AUTH_TOKEN = process.env.SMARTLIB_AUTH_TOKEN || "";

// Safe static policies
const LIBRARY_POLICIES = {
  maxBorrowLimit: 5,
  loanPeriodDays: 14,
  finePerDayCents: 50,
  maxRenewals: 2,
  gracePeriodDays: 1,
  lostBookFeeCents: 2500,
  reservationExpiryDays: 3
};

// Safe categories
const LIBRARY_CATEGORIES = [
  "COMPUTER_SCIENCE",
  "FICTION",
  "NON_FICTION",
  "SCIENCE",
  "HISTORY",
  "BUSINESS",
  "BIOGRAPHY",
  "MATHEMATICS",
  "PHILOSOPHY"
];

// Initialize Server
const server = new Server(
  {
    name: "smartlib-mcp-server",
    version: "1.0.0"
  },
  {
    capabilities: {
      tools: {},
      resources: {},
      prompts: {}
    }
  }
);

// ---------------------------------------------------------------------------
// MCP TOOLS LIST
// ---------------------------------------------------------------------------
server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
    tools: [
      {
        name: "search_books",
        description: "Search books in SmartLib using title, author, or keyword (hybrid search).",
        inputSchema: {
          type: "object",
          properties: {
            query: { type: "string", description: "Search query (1-200 characters)" },
            limit: { type: "number", description: "Max results to return (1-20)", default: 5 }
          },
          required: ["query"]
        }
      },
      {
        name: "get_book_details",
        description: "Retrieve comprehensive details for a book by its SmartLib ID.",
        inputSchema: {
          type: "object",
          properties: {
            bookId: { type: "number", description: "SmartLib book ID (positive integer)" }
          },
          required: ["bookId"]
        }
      },
      {
        name: "check_book_availability",
        description: "Check physical copy counts, availability status, and shelf location for a book.",
        inputSchema: {
          type: "object",
          properties: {
            bookId: { type: "number", description: "SmartLib book ID" },
            title: { type: "string", description: "Book title if ID is not known" }
          }
        }
      },
      {
        name: "get_similar_books",
        description: "Find semantically similar books in the catalog based on vector embeddings.",
        inputSchema: {
          type: "object",
          properties: {
            bookId: { type: "number", description: "Reference book ID" },
            limit: { type: "number", description: "Max results (1-10)", default: 5 }
          },
          required: ["bookId"]
        }
      },
      {
        name: "get_my_borrowings",
        description: "Get active borrowings for the authenticated user. Requires authentication context.",
        inputSchema: {
          type: "object",
          properties: {}
        }
      },
      {
        name: "get_my_overdue_books",
        description: "Get currently overdue books for the authenticated user. Requires authentication context.",
        inputSchema: {
          type: "object",
          properties: {}
        }
      },
      {
        name: "get_my_reservations",
        description: "Get active reservations for the authenticated user. Requires authentication context.",
        inputSchema: {
          type: "object",
          properties: {}
        }
      },
      {
        name: "get_my_fines",
        description: "Get outstanding fines for the authenticated user. Requires authentication context.",
        inputSchema: {
          type: "object",
          properties: {}
        }
      },
      {
        name: "get_personalized_recommendations",
        description: "Get tailored book recommendations based on user borrowing history and preferences.",
        inputSchema: {
          type: "object",
          properties: {
            limit: { type: "number", description: "Number of recommendations (1-10)", default: 5 }
          }
        }
      },
      {
        name: "get_my_memories",
        description: "Get active preferences and reading memories for the authenticated user.",
        inputSchema: {
          type: "object",
          properties: {}
        }
      }
    ]
  };
});

// ---------------------------------------------------------------------------
// MCP TOOL EXECUTION HANDLER
// ---------------------------------------------------------------------------
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;
  const toolArgs = (args || {}) as Record<string, any>;

  // Security check: reject explicit cross-user identity selectors
  const forbiddenSelectors = ["userid", "memberid", "user_id", "member_id", "email", "targetuser"];
  for (const key of Object.keys(toolArgs)) {
    if (forbiddenSelectors.includes(key.toLowerCase())) {
      throw new McpError(
        ErrorCode.InvalidParams,
        "Security violation: Identity selectors (userId, memberId, email) are not accepted. Identity must derive from authentication context."
      );
    }
  }

  // Personal tools list
  const personalTools = [
    "get_my_borrowings",
    "get_my_overdue_books",
    "get_my_reservations",
    "get_my_fines",
    "get_personalized_recommendations",
    "get_my_memories"
  ];

  // If invoking a personal tool, verify authentication
  if (personalTools.includes(name)) {
    if (!AUTH_TOKEN || AUTH_TOKEN.trim().length === 0) {
      throw new McpError(
        ErrorCode.InvalidRequest,
        "Authentication required: Personal library tools require a valid authenticated session (SMARTLIB_AUTH_TOKEN)."
      );
    }
  }

  switch (name) {
    case "search_books": {
      const query = toolArgs.query;
      if (!query || typeof query !== "string" || query.trim().length === 0) {
        throw new McpError(ErrorCode.InvalidParams, "Search query is required and must be non-empty.");
      }
      if (query.length > 200) {
        throw new McpError(ErrorCode.InvalidParams, "Search query exceeds maximum length of 200 characters.");
      }
      const limit = Math.min(Math.max(Number(toolArgs.limit) || 5, 1), 20);

      const res = await callBackend(`/api/books/search?query=${encodeURIComponent(query)}&limit=${limit}`);
      return {
        content: [{ type: "text", text: JSON.stringify(res, null, 2) }]
      };
    }

    case "get_book_details": {
      const bookId = Number(toolArgs.bookId);
      if (!bookId || bookId <= 0 || !Number.isInteger(bookId)) {
        throw new McpError(ErrorCode.InvalidParams, "Invalid book ID: Must be a positive integer.");
      }
      const res = await callBackend(`/api/books/${bookId}`);
      return {
        content: [{ type: "text", text: JSON.stringify(res, null, 2) }]
      };
    }

    case "check_book_availability": {
      const bookId = toolArgs.bookId ? Number(toolArgs.bookId) : null;
      const title = toolArgs.title ? String(toolArgs.title).trim() : null;

      if (!bookId && !title) {
        throw new McpError(ErrorCode.InvalidParams, "Either bookId or title must be provided.");
      }
      const endpoint = bookId
        ? `/api/books/${bookId}/availability`
        : `/api/books/availability?title=${encodeURIComponent(title!)}`;

      const res = await callBackend(endpoint);
      return {
        content: [{ type: "text", text: JSON.stringify(res, null, 2) }]
      };
    }

    case "get_similar_books": {
      const bookId = Number(toolArgs.bookId);
      if (!bookId || bookId <= 0 || !Number.isInteger(bookId)) {
        throw new McpError(ErrorCode.InvalidParams, "Invalid book ID: Must be a positive integer.");
      }
      const limit = Math.min(Math.max(Number(toolArgs.limit) || 5, 1), 10);
      const res = await callBackend(`/api/books/${bookId}/similar?limit=${limit}`);
      return {
        content: [{ type: "text", text: JSON.stringify(res, null, 2) }]
      };
    }

    case "get_my_borrowings": {
      const res = await callBackend("/api/borrowings/my", true);
      return {
        content: [{ type: "text", text: JSON.stringify(res, null, 2) }]
      };
    }

    case "get_my_overdue_books": {
      const res = await callBackend("/api/borrowings/my/overdue", true);
      return {
        content: [{ type: "text", text: JSON.stringify(res, null, 2) }]
      };
    }

    case "get_my_reservations": {
      const res = await callBackend("/api/reservations/my", true);
      return {
        content: [{ type: "text", text: JSON.stringify(res, null, 2) }]
      };
    }

    case "get_my_fines": {
      const res = await callBackend("/api/fines/my", true);
      return {
        content: [{ type: "text", text: JSON.stringify(res, null, 2) }]
      };
    }

    case "get_personalized_recommendations": {
      const limit = Math.min(Math.max(Number(toolArgs.limit) || 5, 1), 10);
      const res = await callBackend(`/api/recommendations/my?limit=${limit}`, true);
      return {
        content: [{ type: "text", text: JSON.stringify(res, null, 2) }]
      };
    }

    case "get_my_memories": {
      const res = await callBackend("/api/ai/memory", true);
      return {
        content: [{ type: "text", text: JSON.stringify(res, null, 2) }]
      };
    }

    default:
      throw new McpError(ErrorCode.MethodNotFound, `Unknown tool '${name}'. Tool is not supported or not in allowlist.`);
  }
});

// ---------------------------------------------------------------------------
// MCP RESOURCES LIST & READ
// ---------------------------------------------------------------------------
server.setRequestHandler(ListResourcesRequestSchema, async () => {
  return {
    resources: [
      {
        uri: "smartlib://categories",
        name: "SmartLib Book Categories",
        description: "Official genres and classification categories supported in the library catalog.",
        mimeType: "application/json"
      },
      {
        uri: "smartlib://library/policies",
        name: "SmartLib Library Policies",
        description: "Official borrowing limits, loan durations, and fine schedules.",
        mimeType: "application/json"
      }
    ]
  };
});

server.setRequestHandler(ReadResourceRequestSchema, async (request) => {
  const uri = request.params.uri;

  if (uri === "smartlib://categories") {
    return {
      contents: [
        {
          uri,
          mimeType: "application/json",
          text: JSON.stringify({ categories: LIBRARY_CATEGORIES }, null, 2)
        }
      ]
    };
  }

  if (uri === "smartlib://library/policies") {
    return {
      contents: [
        {
          uri,
          mimeType: "application/json",
          text: JSON.stringify(LIBRARY_POLICIES, null, 2)
        }
      ]
    };
  }

  // Check for smartlib://books/{id}
  const bookMatch = uri.match(/^smartlib:\/\/books\/(\d+)$/);
  if (bookMatch) {
    const bookId = Number(bookMatch[1]);
    const res = await callBackend(`/api/books/${bookId}`);
    return {
      contents: [
        {
          uri,
          mimeType: "application/json",
          text: JSON.stringify(res, null, 2)
        }
      ]
    };
  }

  throw new McpError(
    ErrorCode.InvalidParams,
    `Resource URI '${uri}' not recognized. Only safe smartlib:// resources are supported.`
  );
});

// ---------------------------------------------------------------------------
// MCP PROMPTS LIST & GET
// ---------------------------------------------------------------------------
server.setRequestHandler(ListPromptsRequestSchema, async () => {
  return {
    prompts: [
      {
        name: "smartlib_book_research",
        description: "Research a topic or author in the SmartLib catalog and synthesize findings.",
        arguments: [
          {
            name: "topic",
            description: "The literary subject, technology, or author to research",
            required: true
          }
        ]
      },
      {
        name: "smartlib_recommend_books",
        description: "Formulate reading recommendations matching user interest against SmartLib's physical catalog.",
        arguments: [
          {
            name: "interest",
            description: "Genre or subject interest (e.g. Distributed Systems, Sci-Fi)",
            required: true
          }
        ]
      }
    ]
  };
});

server.setRequestHandler(GetPromptRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;
  const promptArgs = (args || {}) as Record<string, string>;

  if (name === "smartlib_book_research") {
    const topic = promptArgs.topic || "General";
    return {
      messages: [
        {
          role: "user",
          content: {
            type: "text",
            text: `Please research books in SmartLib on the topic: "${topic}". Use 'search_books' to locate relevant titles and 'check_book_availability' to check physical copy counts before providing recommendations.`
          }
        }
      ]
    };
  }

  if (name === "smartlib_recommend_books") {
    const interest = promptArgs.interest || "Any";
    return {
      messages: [
        {
          role: "user",
          content: {
            type: "text",
            text: `Recommend 3-5 high-quality books matching the interest: "${interest}". Verify that each recommended book exists in SmartLib and summarize why it fits the interest.`
          }
        }
      ]
    };
  }

  throw new McpError(ErrorCode.MethodNotFound, `Prompt '${name}' is not recognized.`);
});

// ---------------------------------------------------------------------------
// BACKEND API CLIENT HELPER
// ---------------------------------------------------------------------------
async function callBackend(path: string, requiresAuth = false): Promise<any> {
  const headers: Record<string, string> = {
    "Accept": "application/json"
  };

  if (requiresAuth && AUTH_TOKEN) {
    headers["Authorization"] = `Bearer ${AUTH_TOKEN}`;
  }

  try {
    const response = await fetch(`${BACKEND_URL}${path}`, {
      method: "GET",
      headers
    });

    if (!response.ok) {
      if (response.status === 401 || response.status === 403) {
        throw new McpError(ErrorCode.InvalidRequest, "Backend rejected request: Unauthorized.");
      }
      if (response.status === 404) {
        throw new McpError(ErrorCode.InvalidParams, "Resource or entity not found in SmartLib backend.");
      }
      throw new McpError(ErrorCode.InternalError, `Backend returned HTTP status ${response.status}`);
    }

    return await response.json();
  } catch (error: any) {
    if (error instanceof McpError) throw error;
    // Fallback/offline mock data if backend server is not running during local dev
    return {
      status: "mock_response",
      path,
      message: "SmartLib backend endpoint reached or mocked for local MCP operation."
    };
  }
}

// ---------------------------------------------------------------------------
// SERVER STARTUP
// ---------------------------------------------------------------------------
async function run() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("SmartLib MCP Server running on stdio.");
}

run().catch((error) => {
  console.error("Fatal error starting SmartLib MCP server:", error);
  process.exit(1);
});
