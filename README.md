# AI Software Engineering Assistant

A full-stack AI-powered software engineering assistant built with Spring Boot (backend) and React + Vite (frontend).

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 19, TypeScript, Vite, TailwindCSS |
| Backend | Spring Boot 3.3, Java 17, Spring Security, JWT |
| Database | PostgreSQL 18 |
| AI Provider | OpenRouter API (GPT-4o Mini, Gemini 2.5 Flash, Claude, Llama) |

---

## Prerequisites

- Java 17+
- Node.js 18+
- PostgreSQL 18+
- Maven (via `mvnw`)

---

## Setup Instructions

### 1. Clone the repository

```bash
git clone https://github.com/your-username/ai-software-engineering-assistant.git
cd ai-software-engineering-assistant
```

### 2. Create PostgreSQL database

```sql
CREATE DATABASE ai_assistant;
```

### 3. Configure Backend Environment Variables

The backend reads config from environment variables. Set these (or use the defaults):

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/ai_assistant` | Database URL |
| `DB_USERNAME` | `postgres` | DB username |
| `DB_PASSWORD` | *(set yours)* | DB password |
| `OPENROUTER_API_KEY` | *(set yours)* | OpenRouter API key |
| `JWT_SECRET` | *(auto-generated default)* | JWT signing secret |

### 4. Configure Frontend Environment

Copy the example env file and fill in your values:

```bash
cd frontend
cp .env.example .env.local
```

Edit `.env.local`:
```
VITE_API_BASE_URL=http://localhost:8080
VITE_OPENROUTER_API_KEY=your_openrouter_key_here
```

### 5. Run the Backend

```bash
cd backend
.\mvnw.cmd spring-boot:run   # Windows
./mvnw spring-boot:run       # Linux/Mac
```

Backend starts at: **http://localhost:8080**

### 6. Run the Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend starts at: **http://localhost:5173**

---

## Features

- 🔐 **JWT Authentication** — Register / Login / Google OAuth
- 📁 **Project Workspaces** — Multiple projects per user
- 🤖 **AI Chat Panel** — Multi-provider (OpenRouter, Gemini, OpenAI)
- 🧠 **RAG (Document Ingestion)** — Upload docs for context-aware AI responses
- 🤖 **Agent Workspace** — AI agent pipeline for code tasks
- 🔗 **GitHub Integration** — Repo browsing and analysis
- 🌗 **Dark / Light Theme**

---

## Security Note

**Never commit your API keys or database password to git.**  
All sensitive values are loaded from environment variables or `.env.local` files which are gitignored.

---

## License

MIT
