# SmartLib AI — Production Deployment & Operations Guide

## 1. Production Architecture Deployment Map
- **Backend**: Render Web Service (Docker runtime, multi-stage build, Java 21)
- **Frontend**: Render Static Site (Vite production build, Node 20+)
- **Transactional Database**: Aiven MySQL (Managed MySQL with SSL)
- **Vector Database**: Qdrant Cloud (Managed Qdrant cluster, 768d Cosine)
- **Model Context Protocol (MCP)**: Local developer stdio gateway (requires SSE adapter for public internet hosting)

---

## 2. Backend Service Deployment (Render Docker Web Service)

### Service Settings
- **Environment**: `Docker`
- **Region**: Oregon (US West) or Frankfurt (EU Central)
- **Branch**: `feature/ai-library-assistant`
- **Health Check Path**: `/actuator/health`
- **Auto-Deploy**: Disabled (Manual deployment triggered only after verification)

### Environment Variables
Configure the following in the Render Dashboard (**Environment** tab):

```env
# Server
PORT=10000
ENVIRONMENT=production
FRONTEND_URL=https://smartlib-frontend-nmml.onrender.com

# Database (Aiven MySQL)
DB_URL=jdbc:mysql://<aiven-host>:<port>/defaultdb?sslMode=REQUIRED
DB_USERNAME=<aiven-username>
DB_PASSWORD=<aiven-password>

# Security
JWT_SECRET=<secure-random-256-bit-string-min-32-characters>

# Primary AI (Gemini)
GEMINI_API_KEY=<google-ai-studio-api-key>
GEMINI_MODEL=gemini-3.8-flash

# Fallback AI (Groq)
GROQ_API_KEY=<groq-cloud-api-key>
GROQ_MODEL=llama-3.3-70b-versatile

# Vector Database (Qdrant Cloud)
QDRANT_URL=https://<qdrant-cluster-id>.us-east4-0.gcp.cloud.qdrant.io:6333
QDRANT_API_KEY=<qdrant-api-key>
QDRANT_COLLECTION=smartlib_books

# Features & Governance
AI_RATE_LIMIT_PER_MINUTE=10
AI_RERANKING_ENABLED=true
AI_WEB_GROUNDING_ENABLED=true
AI_MEMORY_ENABLED=true

# Outbound Mail (Optional)
MAIL_USERNAME=<gmail-account>
MAIL_PASSWORD=<google-app-password>
```

---

## 3. Frontend Deployment (Render Static Site)

### Service Settings
- **Build Command**: `npm install && npm run build`
- **Publish Directory**: `dist`
- **Root Directory**: `frontend`

### Environment Variables
```env
VITE_API_URL=https://smartlib-backend-04o3.onrender.com/api
```

---

## 4. MCP Deployment Policy & Clarification
- **Current Status**: The current implementation of `mcp-server` uses the standard Model Context Protocol over `stdio` transport.
- **Local Developer Integration**: Works out of the box with Claude Desktop and developer agents by configuring `claude_desktop_config.json`:
  ```json
  {
    "mcpServers": {
      "smartlib": {
        "command": "node",
        "args": ["c:/path/to/smartlib-project-AI/mcp-server/dist/index.js"],
        "env": {
          "SMARTLIB_API_BASE_URL": "https://smartlib-backend-04o3.onrender.com",
          "SMARTLIB_AUTH_TOKEN": "<optional-user-token>"
        }
      }
    }
  }
  ```
- **Cloud Hosting Note**: Direct cloud hosting on Render for MCP requires an SSE (Server-Sent Events) network adapter. In this release, MCP is documented as a local-developer and IDE agent gateway.

---

## 5. Health Checks & Verification
Verify the deployment using the public health endpoint:
```bash
curl -I https://smartlib-backend-04o3.onrender.com/actuator/health
```
Expected response:
```http
HTTP/1.1 200 OK
Content-Type: application/vnd.spring-boot.actuator.v3+json
{"status":"UP"}
```

---

## 6. Disaster Recovery & Rollback Procedure
1. **Application Rollback**: In Render Dashboard, click **Deploys** -> Select the previous stable commit -> Click **Rollback to this deploy**.
2. **Qdrant Collection Revert**: If an embedding migration causes retrieval drift, set `QDRANT_COLLECTION=smartlib_books_previous` in environment variables and trigger zero-downtime redeploy.
3. **Database Restore**: Aiven MySQL point-in-time recovery (PITR) is accessible from the Aiven console.
