# Frontend Component Architecture Guidelines (Thymeleaf + PulsePoint v2)

## Goal & Design Philosophy

To ensure template code remains maintainable, readable, and structured similarly to React components without introducing client-side JavaScript frameworks (React/Vue/Angular), templates are organized into modular, server-rendered Thymeleaf component fragments (`th:fragment`).

---

## Directory Conventions

```text
src/main/resources/templates/
├── <page_name>.html                         # Root Page Templates ONLY (e.g. tasks.html, users.html, login.html)
└── components/
    ├── ui/                                  # Reusable Atomic UI Primitives
    │   ├── button.html                      # Primary, secondary, & action button fragments
    │   ├── input.html                       # Text, textarea, & select field fragments
    │   ├── alert.html                       # Error & success alert fragments
    │   └── badge.html                       # Status & priority badge fragments
    │
    └── <page_name>/                         # Page-Specific Feature Component Fragments
        ├── <page>_header.html               # Page navigation & header bar
        ├── <page>_<feature>_card.html       # Specific form / widget cards
        └── <page>_list_section.html         # Data tables / card grids
```

---

## Rules & Standards

### 1. Root Templates (`templates/*.html`)
- Keep root templates clean, under 150 lines.
- Contain page-level structural wrappers, layout tags, and high-level `th:replace` component inclusions.
- Enclose reactive boundaries in `<template pp-component="<id>">` containing `<div pp-component="<id>">`.

### 2. UI Component Fragments (`templates/components/ui/*.html`)
- Store reusable input fields, buttons, alerts, badges, modals, and spinners here.
- Pass parameters via Thymeleaf fragment signatures: `th:fragment="primary (text, loadingText, isSubmittingExpr, iconPath)"`.
- Keep markup strictly styled with standard precompiled CSS classes.

### 3. Page Feature Components (`templates/components/<page_name>/*.html`)
- Store feature-specific cards, tables, header navigation bars, and section layouts here.
- Delegate UI element rendering (inputs, buttons, alerts) to `components/ui/` fragments using `th:replace="~{components/ui/button :: action(...)}"`.

### 4. Performance & Performance Overhead
- **Server Overhead**: 0ms. Thymeleaf compiles and caches fragment AST nodes at application startup (`spring.thymeleaf.cache=true`).
- **Network Overhead**: 0 bytes. Server renders the combined HTML tree before response streaming; payload size is identical to monolithic files.
- **Client Hydration**: PulsePoint v2 scans `pp-component` boundaries seamlessly in ~1ms.
