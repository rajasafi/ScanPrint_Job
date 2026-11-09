# PrintDoc backend gap audit

This audit compares the eight-step setup flow currently presented by PrintDoc with the local Spring API. The REA MCP server was not available in the active OpenCode session, so live browser MCP observations could not be refreshed in this pass. The comparison uses the previously observed `/how-to-setup` route, the local frontend calls, and the backend source. It does not treat marketing copy as proof of production behavior.

## Current coverage

| Setup step | Current backend | Gap | State |
|---|---|---|---|
| 1. Register shop | `POST /api/v1/auth/shop/register`, JWT login, shop code generation, phone/printer brand capture, 48-hour trial timestamps | Production account verification, password reset, email/phone verification | Implemented locally |
| 2. Download desktop agent | None | Signed Windows agent artifact, version manifest, update channel, download authorization | Missing |
| 3. PC binding/config | Public fingerprint approval polling, owner approve/reject/revoke, one-time BCrypt-hashed agent secret, REST registration/heartbeat, and configurable Socket.IO v4 gateway | Reverse-proxy/public URL integration and refresh-token/device-session hardening | Partial |
| 4. Map printers | None | Printer/device model, capability discovery, routing persistence, agent bridge | Missing |
| 5. Rates/payments | Shop B&W/color/duplex rates are persisted; UPI and cash are only states | Payment provider integration, webhook signature verification, settlement/reconciliation, payment preference storage | Partial |
| 6. Counter QR standee | Shop code and public kiosk routes exist; QR is decorative in the UI | Stable customer URL/QR payload endpoint and generated/downloadable standee asset | Partial |
| 7. Customer self-orders | Temporary uploads, quote/job creation, public status endpoint | Real browser kiosk integration, multi-file upload, server-side page counting, payment confirmation | Partial |
| 8. Auto-print/shop live | Owner can transition demo job records | Authenticated agent queue consumer, lease/ack/retry semantics, printer execution, print result callbacks | Missing |

## Changes made in this pass

- Added authenticated `GET /api/v1/owner/setup-status`, which reports readiness for all eight steps and names the blocking components instead of implying that the visual setup tour is operational.
- Added `api.setupStatus()` to the frontend API client for future dashboard wiring.
- Public job creation now rejects inactive shops consistently with upload creation.
- Print job transitions now have a domain state machine, preventing arbitrary status jumps at the entity boundary.
- Added `agent_devices` and `device_audit_events` persistence, public device approval polling, owner device controls, one-time agent-secret rotation, authenticated REST registration/heartbeat, and a configurable Socket.IO v4 gateway compatible with the observed agent event names.
- Extended shop registration with optional phone/printer brand fields and a recorded 48-hour trial window.

## What is still required for a genuinely full deployment

1. A separately distributed, signed Windows agent. Spring cannot discover or execute a shop's local USB/LAN printers by itself.
2. Finalize Socket.IO deployment topology, public URL/reverse-proxy routing, and agent-side API/socket URL configuration; then add printer registration/capability records and queue lease semantics.
3. Printer registration/capability records and a durable queue worker with idempotent claim, acknowledgement, retry, and dead-letter behavior.
4. A real payment provider chosen by the operator, including webhook verification and a payment-to-queue authorization rule. Never mark a UPI order paid from a browser request.
5. QR configuration and asset generation tied to an active shop, plus rate limiting and abuse controls on public upload/order endpoints.
6. Production document handling: content sniffing rather than trusting browser MIME, antivirus/content policy, page-count extraction, object storage, encryption, retention, and deletion verification.
7. Operational controls: database migrations for every schema change, backups, metrics/log redaction, alerting, secret management, and end-to-end tests against the actual agent/payment boundaries.

The current code is therefore a safe local prototype with a working account/upload/job demo, not a production print network. Ghidra, Hopper, and other desktop/binary-analysis tools are not needed for this web/backend gap audit.
