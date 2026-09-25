# PulsePoint Spring Boot Integration Documentation

Welcome to the documentation directory for the **Spring Boot 3.x + PulsePoint v2** integration and reference demo.

This directory contains technical guides, architectural evaluation reports, issue analyses, implementation roadmaps, and visual verification evidence.

---

## 📑 Documentation Index

### 1. Architectural & Implementation Guides

| Document | Description |
|---|---|
| [**`PROPOSED_PULSEPOINT_SPRING_BOOT_DOCS.md`**](PROPOSED_PULSEPOINT_SPRING_BOOT_DOCS.md) | **Turnkey Official Integration Guide**: Step-by-step instructions for Java & Spring Boot developers integrating PulsePoint v2 (Security, CSRF cookie-to-header bridging, RPC registration, SSE streaming, and WebSockets). |
| [**`WALKTHROUGH.md`**](WALKTHROUGH.md) | **Technical Implementation Walkthrough**: In-depth breakdown of the monolithic Spring Boot backend, the wire protocol filter, 22 automated tests, Tailwind CSS integration without npm, and interactive E2E browser verification across mobile, tablet, and desktop. |

### 2. Evaluation Reports & Research

| Document | Description |
|---|---|
| [**`PULSEPOINT_JAVA_REPORT.md`**](PULSEPOINT_JAVA_REPORT.md) | **Comprehensive Evaluation Report**: Executive analysis answering primary feasibility questions, architectural tradeoffs, comparison with HTMX / Vaadin, and long-term viability. |
| [**`PULSEPOINT_JAVA_ISSUES.md`**](PULSEPOINT_JAVA_ISSUES.md) | **Issue & Friction Catalog**: Detailed analysis of 6 discovered friction points & edge cases (JSON error expectations, CSRF cookie naming, Jackson LocalDateTime serialization, heartbeat timeouts) with root causes, workarounds, and suggested framework enhancements. |
| [**`PULSEPOINT_AGENTIC_TEST.md`**](PULSEPOINT_AGENTIC_TEST.md) | **AI Agent Usability & Complexity Scorecard**: Evaluation matrix tracking how an autonomous AI coding agent navigated documentation, implemented protocols, and debugged edge cases, with a 14-dimension usability scorecard. |

### 3. Implementation Plans & Roadmaps

| Document | Description |
|---|---|
| [**`PLAN_SSE_WEBSOCKET.md`**](PLAN_SSE_WEBSOCKET.md) | **SSE & WebSocket Roadmap**: Architecture design and verification plan for Server-Sent Events (SSE) streaming and collaborative multi-tab WebSockets. |
| [**`PLAN.md`**](PLAN.md) | **Initial Architectural Blueprint**: Foundation planning document outlining the domain model, services, Spring Security session auth, and phase milestones. |

### 4. Wire Protocols & Specifications

| Document | Description |
|---|---|
| [**`llms.md`**](llms.md) | **PulsePoint v2 Specification**: The core wire protocol specification, template directives, runtime hooks, and RPC contracts. |

---

## 📸 Visual Verification Artifacts

Visual evidence from interactive Chromium end-to-end testing is preserved in the [`screenshots/`](screenshots/) directory:

### Session Recordings
- **Tailwind CSS & Mobile/Desktop Responsive Verification:** [**`tailwind_responsive_test_1790321816150.webp`**](screenshots/tailwind_responsive_test_1790321816150.webp)
- **Full Monolith Interactive Verification:** [**`full_interactive_verification_1790159186402.webp`**](screenshots/full_interactive_verification_1790159186402.webp)

### Screenshots
- **Modernized Login Page:** [`login_page_1790321882096.png`](screenshots/login_page_1790321882096.png)
- **Responsive Tasks Dashboard (Desktop):** [`tasks_page_dashboard_1790321972386.png`](screenshots/tasks_page_dashboard_1790321972386.png)
- **Task Created via RPC:** [`task_created_success_1790322134999.png`](screenshots/task_created_success_1790322134999.png)
- **Status Mutation to IN_PROGRESS:** [`status_updated_in_progress_1790322166624.png`](screenshots/status_updated_in_progress_1790322166624.png)
- **Live SSE Audit Streaming:** [`sse_audit_progress_1790322214884.png`](screenshots/sse_audit_progress_1790322214884.png)
- **Mobile Viewport (375x667 Header & Form):** [`mobile_view_top_1790322286184.png`](screenshots/mobile_view_top_1790322286184.png)
- **Mobile Viewport (375x667 Task Cards):** [`mobile_view_portrait_1790322262913.png`](screenshots/mobile_view_portrait_1790322262913.png)
- **Tablet Viewport (768x1024):** [`tablet_view_1790322311058.png`](screenshots/tablet_view_1790322311058.png)
- **Multipart Upload Progress Tracking:** [`file_upload_progress_1790162452519.png`](screenshots/file_upload_progress_1790162452519.png)
- **Multi-Tab WebSocket Live Sync:** [`websocket_live_sync_1790163508917.png`](screenshots/websocket_live_sync_1790163508917.png)
