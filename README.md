# 🤖 AI Software Engineering Assistant

> An AI-powered full-stack development assistant that brings software engineering tasks into one workspace.

[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=white)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.x-3178C6?logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://www.java.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)

## ✨ Overview

The **AI Software Engineering Assistant** is designed to support developers across multiple stages of software development. Instead of using separate tools for every task, the platform brings AI-assisted development workflows into a single application.

### What it helps with

- 🔍 **Requirement Analysis** — understand and break down software requirements
- 💻 **Code Generation** — assist with implementation from requirements
- 🛡️ **Security-Focused Code Review** — identify potential security concerns
- 🗄️ **Schema Generation** — assist with designing application data structures
- 🧪 **Automated Testing** — support test generation and verification workflows
- 🤖 **AI Agent Workspace** — organize development tasks into an AI-assisted workflow
- 🔗 **GitHub Integration** — browse and analyze repository content
- 📄 **RAG / Document Context** — use uploaded documents as context for AI responses

## 🏗️ Architecture

```text
┌───────────────────────────────┐
│       React + TypeScript      │
│          Frontend             │
└───────────────┬───────────────┘
                │ REST API
                ▼
┌───────────────────────────────┐
│       Spring Boot / Java      │
│          Backend              │
└───────┬───────────┬───────────┘
        │           │
        ▼           ▼
┌─────────────┐  ┌────────────────┐
│ PostgreSQL  │  │   AI Provider  │
│   Database  │  │   / AI Agents  │
└─────────────┘  └────────────────┘

        GitHub Integration
               │
               ▼
        Repository Analysis
```

## 🧠 AI Workflow

The application is organized around specialized software-engineering tasks:

```text
Requirements
     │
     ▼
Requirement Analysis
     │
     ├──────────────► Code Generation
     │
     ├──────────────► Schema Generation
     │
     ├──────────────► Security Review
     │
     └──────────────► Automated Testing
```

The project uses an AI-assisted workflow to keep these responsibilities modular and easier to extend.

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 19, TypeScript, Vite, Tailwind CSS |
| Backend | Spring Boot 3.3, Java 17, Spring Security |
| Authentication | JWT, Google OAuth |
| Database | PostgreSQL 18 |
| AI | OpenRouter API and supported LLM providers |
| AI Context | RAG / document ingestion |
| Integration | GitHub repository browsing and analysis |

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Node.js 18+
- PostgreSQL 18+
- Maven

### 1. Clone

```bash
git clone https://github.com/muthuvel-24/AI-Software-Engineering.git
cd AI-Software-Engineering
```

### 2. Create the database

```sql
CREATE DATABASE ai_assistant;
```

### 3. Configure environment variables

Configure the backend with your database credentials and AI provider credentials. Keep secrets in environment variables and never commit them to Git.

### 4. Start the backend

```bash
cd backend
.\mvnw.cmd spring-boot:run
```

Backend: `http://localhost:8080`

### 5. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend: `http://localhost:5173`

## 🔐 Security

- Keep API keys and database passwords outside source control.
- Use environment variables for secrets.
- Never expose private credentials in client-side code.
- Review authentication and authorization before deploying publicly.

## 📌 Why this project?

This project combines **full-stack development + AI + software engineering automation** in one application. It demonstrates how AI can be integrated into practical development workflows rather than being used only as a standalone chatbot.

## 🔮 Future Improvements

- More specialized development agents
- Better workflow observability
- More automated test-generation strategies
- Improved repository-level code analysis
- Production cloud deployment and monitoring

## 📄 License

MIT License
