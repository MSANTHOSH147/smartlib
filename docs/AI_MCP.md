# SmartLib AI — Phase 10: Model Context Protocol (MCP)

## 1. Why MCP Exists
The Model Context Protocol (MCP) is an open standard that allows LLMs and AI clients (such as Claude Desktop, IDE AI extensions, and autonomous agents) to safely discover and interact with external data repositories, tools, and prompts using a standardized JSON-RPC protocol over `stdio` or HTTP/SSE transports.

For SmartLib, MCP provides an integration surface that allows external AI assistants to discover library catalog resources, research books, inspect availability, and view authorized user records without requiring ad-hoc scraping or custom REST integration code.

## 2. MCP Architecture
The SmartLib MCP architecture is designed around the principle that **the backend application and database remain the sole authoritative source of truth**:

```text
External AI Client (Claude Desktop, IDE, Agent)
                      │ (stdio / JSON-RPC)
                      ▼
            SmartLib MCP Server (Node/TypeScript)
                      │
     ┌────────────────┴────────────────┐
     ▼                                 ▼
Public Tools & Resources       Personal Tools (Auth Token Required)
     │                                 │
     └────────────────┬────────────────┘
                      ▼ (HTTP REST with Bearer Token)
            SmartLib Spring Boot Backend
                      │
          ┌───────────┴───────────┐
          ▼                       ▼
      MySQL DB                Qdrant DB
```

The MCP server acts as a thin, secure gateway. It never connects directly to MySQL or Qdrant and never replicates domain business logic.

## 3. MCP Tools (Read-Only)
The following read-oriented tools are exposed:
- `search_books`: Hybrid catalog search (parameters: `query` [1–200 chars], `limit` [1–20]).
- `get_book_details`: Comprehensive book details by positive integer ID (parameter: `bookId`).
- `check_book_availability`: Physical copy count, checkout state, and shelf location (parameters: `bookId` or `title`).
- `get_similar_books`: Vector-based similar book recommendations (parameters: `bookId`, `limit` [1–10]).
- `get_my_borrowings`: User's currently active loans (requires authenticated session context).
- `get_my_overdue_books`: User's overdue loans (requires authenticated session context).
- `get_my_reservations`: User's pending reservations (requires authenticated session context).
- `get_my_fines`: Outstanding fines (requires authenticated session context).
- `get_personalized_recommendations`: Recommendations tailored to user loan history (requires authenticated session context).
- `get_my_memories`: Saved reading preferences (requires authenticated session context).

## 4. MCP Resources
Safe structured resources exposed under the `smartlib://` protocol scheme:
- `smartlib://books/{id}`: Detailed catalog metadata and copy status for a specific book.
- `smartlib://categories`: List of official genres and classifications.
- `smartlib://library/policies`: Static structured library rules (loan duration, fine rates, renewals).

### Forbidden Resources
The MCP server strictly prohibits:
- Database connection strings and credentials
- Raw database tables and SQL dumps
- User passwords, password hashes, and JWT tokens
- Internal environment variables
- Filesystem or operating system files (`file://`, path traversal)

## 5. MCP Prompts
Reusable, standardized prompt templates:
- `smartlib_book_research`: Structured research prompt taking a `topic` argument to search the catalog and check copy availability.
- `smartlib_recommend_books`: Recommendation formulation prompt taking an `interest` argument.

## 6. Authentication & Personal Data Isolation
Security regarding user identity is absolute:
- **No Identity Selectors in Arguments**: The MCP server strictly forbids accepting `userId`, `memberId`, or `email` as tool arguments. Any attempt to supply an identity argument is rejected as an invalid parameter and security violation.
- **Context-Derived Identity**: All personal tools (`get_my_*`) derive identity solely from the authenticated session credentials (`SMARTLIB_AUTH_TOKEN` / Bearer token).
- **Anonymous Protection**: If an unauthenticated caller attempts to invoke a personal tool, the MCP server returns an authorization error without executing any backend calls.

## 7. Tool Validation
All tool invocations are validated prior to execution:
- Book IDs must be positive integers (`bookId > 0`).
- Search queries are constrained to 1–200 characters.
- Result limits are bounded between 1 and 20.
- Tool names must exist in the explicit allowlist.

## 8. Security & Input Sanitization
- All arguments and strings are treated as inert text. Prompt injection attempts cannot bypass tool boundaries.
- No dynamic SQL execution or shell execution is permitted.
- Cross-user data leakage is structurally impossible because user IDs are never passed from the client.

## 9. Read-Only Policy
In this initial release, MCP is strictly **read-only**. Write, administrative, and inventory-altering operations are intentionally disabled:
- No borrowing or returning books
- No canceling or creating reservations
- No modifying book catalog or copy inventory
- No fine adjustments or payments

## 10. Future Write Operations
Future phases will introduce write operations only under strict conditions:
1. Multi-factor confirmation or explicit client consent tokens.
2. Fine-grained OAuth 2.0 scopes (e.g. `smartlib:borrow:write`).
3. Comprehensive audit logging and immutable change history.

## 11. Known Limitations
- The MCP server requires network connectivity to the running SmartLib Spring Boot backend (`http://localhost:8080`).
- When operating in offline mock mode (backend unreachable), only mock catalog responses and static policies are available.
