# Frontend Component Architecture Rules

When authoring or modifying HTML templates in this project:

1. **Root Templates**: Store only full-page HTML files directly under `src/main/resources/templates/` (e.g., `tasks.html`, `users.html`, `login.html`).
2. **UI Component Primitives**: Store atomic reusable UI fragments (buttons, inputs, textareas, alerts, badges) under `src/main/resources/templates/components/ui/`.
3. **Page Components**: Store feature-specific section fragments under `src/main/resources/templates/components/<page_name>/` (e.g., `task_header.html`, `user_table.html`).
4. **Fragment Inclusions**: Use Thymeleaf fragment syntax (`th:replace="~{components/ui/button :: primary(...)}"`) to assemble components into root templates or page section components.
5. **PulsePoint Boundaries**: Maintain explicit `<template pp-component="id"><div pp-component="id">...</div></template>` boundaries around reactive sections.
