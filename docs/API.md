# API Reference

Base URL (local): `http://localhost:8081`

All requests/responses are JSON.

---

## Accounts

### `GET /accounts`

Returns every account, used by the frontend to populate the "From" and "To"
dropdowns.

**Response `200`**

```json
[
  { "accountNumber": "ACC-100001", "balance": 4230.55 },
  { "accountNumber": "ACC-100002", "balance": 12890.10 },
  { "accountNumber": "ACC-900001", "balance": 500000.00 }
]
```

---

## Transfers

### `POST /transfers`

Starts a new transfer. This creates a Camunda process instance that runs
balance check → fraud detection → (conditionally) compliance review →
execution. See [`WORKFLOW.md`](WORKFLOW.md) for the full flow.

**Request body**

```json
{
  "fromAccount": "ACC-100001",
  "toAccount": "ACC-100002",
  "amount": 150.00,
  "referenceCode": "TRF-AB12CD"
}
```

| Field | Type | Notes |
|---|---|---|
| `fromAccount` | string | Must differ from `toAccount` |
| `toAccount` | string | |
| `amount` | number | |
| `referenceCode` | string | Client-generated reference, for display/tracing only |

**Response `200`** — the process instance was created. The transfer's actual
outcome (approved/rejected/completed) happens asynchronously; the frontend
polls `GET /accounts` to detect a balance change rather than blocking on this
call.

**Error responses** — non-2xx with an optional message body:

```json
{ "message": "Insufficient balance on account ACC-100001 at execution time" }
```

---

## Compliance reviews

Transfers flagged by fraud detection pause at a `compliance-review` job that
stays open until a decision is submitted through these endpoints (see
[`WORKFLOW.md`](WORKFLOW.md) for why this uses a manually-completed job
rather than a native Zeebe user task).

### `GET /reviews`

Lists every transfer currently awaiting a compliance decision.

**Response `200`**

```json
[
  {
    "jobKey": 2251799813685895,
    "fromAccount": "ACC-100001",
    "toAccount": "ACC-100002",
    "amount": 12000.00,
    "riskScore": 85,
    "reason": "High transfer velocity; Amount exceeds high-value threshold; ",
    "receivedAt": "2026-08-26T10:15:30Z"
  }
]
```

### `POST /reviews/{jobKey}/decision`

Submits a decision for a pending review, completing the underlying Camunda
job and unblocking the process instance.

**Request body**

```json
{
  "approved": true,
  "reviewer": "admin",
  "notes": "Confirmed with customer by phone"
}
```

| Field | Type | Notes |
|---|---|---|
| `approved` | boolean | `true` continues to execution, `false` rejects the transfer |
| `reviewer` | string | Not currently tied to real authentication  |
| `notes` | string | Optional |

**Response `200`** — the job was completed successfully.

**Error `4xx`** if `jobKey` doesn't correspond to a currently pending review
(e.g. already decided, or the backend restarted and lost track of it — see
the in-memory storage note in `WORKFLOW.md`).

