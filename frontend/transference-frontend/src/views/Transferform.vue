<template>
  <div class="slip-page">
    <div class="slip">
      <div class="slip__perforation" aria-hidden="true">
        <span v-for="n in 24" :key="n" class="slip__perforation-dot"></span>
      </div>

      <header class="slip__header">
        <div class="slip__eyebrow">Transference &mdash; Transfer Slip</div>
        <h1 class="slip__title">New transfer</h1>

        <div class="role-toggle" role="radiogroup" aria-label="Acting role">
          <button
            type="button"
            class="role-toggle__option"
            :class="{ 'role-toggle__option--active': role === 'user' }"
            role="radio"
            :aria-checked="role === 'user'"
            @click="role = 'user'"
          >
            User
          </button>
          <button
            type="button"
            class="role-toggle__option role-toggle__option--admin"
            :class="{ 'role-toggle__option--active': role === 'admin' }"
            role="radio"
            :aria-checked="role === 'admin'"
            @click="role = 'admin'"
          >
            <span class="stamp" v-if="role === 'admin'">Admin</span>
            <span v-else>Admin</span>
          </button>
        </div>
      </header>

      <form v-if="role === 'user'" class="slip__form" @submit.prevent="submitTransfer">
        <p v-if="accountsError" class="form-error" role="alert">{{ accountsError }}</p>

        <div class="field">
          <label class="field__label" for="fromAccount">From account</label>
          <select
            id="fromAccount"
            v-model="form.fromAccount"
            class="field__control field__control--select"
            :disabled="accountsLoading"
            required
          >
            <option value="" disabled>
              {{ accountsLoading ? 'Loading accounts…' : 'Select an account' }}
            </option>
            <option v-for="account in visibleAccounts" :key="account.accountNumber" :value="account.accountNumber">
              {{ account.accountNumber }} &mdash; {{ account.balance }} &euro;
            </option>
          </select>
        </div>

        <div class="field">
          <label class="field__label" for="toAccount">To account</label>
          <select
            id="toAccount"
            v-model="form.toAccount"
            class="field__control field__control--select"
            :disabled="accountsLoading"
            required
          >
            <option value="" disabled>Select an account</option>
            <option
              v-for="account in destinationAccounts"
              :key="account.accountNumber"
              :value="account.accountNumber"
            >
              {{ account.accountNumber }} &mdash; {{ account.balance }} &euro;
            </option>
          </select>
        </div>

        <div class="field">
          <label class="field__label" for="amount">Amount</label>
          <div class="amount-control">
            <span class="amount-control__prefix">$</span>
            <input
              id="amount"
              v-model.number="form.amount"
              class="field__control field__control--mono field__control--amount"
              type="number"
              min="0.01"
              step="0.01"
              placeholder="0.00"
              required
            />
          </div>
        </div>

        <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>
        <p v-if="successMessage" class="form-success" role="status">{{ successMessage }}</p>

        <p v-if="transferStatus === 'processing'" class="status-line status-line--processing" role="status">
          Processing transfer&hellip;
        </p>
        <p v-if="transferStatus === 'completed'" class="status-line status-line--completed" role="status">
          Transfer finished — balance updated.
        </p>
        <p v-if="transferStatus === 'timeout'" class="status-line status-line--timeout" role="status">
          Still processing. It may be waiting on compliance review — check back or refresh.
        </p>

        <div class="slip__footer">
          <span class="slip__ref">Ref: {{ referenceCode }}</span>
          <div class="slip__footer-actions">
            <button
              type="button"
              class="refresh-btn"
              :disabled="accountsLoading"
              @click="refreshBalances"
            >
              {{ accountsLoading ? 'Refreshing…' : 'Refresh balances' }}
            </button>
            <button type="submit" class="submit-btn" :disabled="submitting || accountsLoading">
              {{ submitting ? 'Sending…' : 'Send transfer' }}
            </button>
          </div>
        </div>
      </form>

      <section v-else class="reviews">
        <div class="reviews__header">
          <span class="slip__eyebrow">Compliance &mdash; Pending Reviews</span>
          <button
            type="button"
            class="reviews__refresh"
            :disabled="reviewsLoading"
            @click="fetchPendingReviews"
          >
            {{ reviewsLoading ? 'Refreshing…' : 'Refresh' }}
          </button>
        </div>

        <p v-if="reviewsError" class="form-error" role="alert">{{ reviewsError }}</p>
        <p v-if="reviewDecisionError" class="form-error" role="alert">{{ reviewDecisionError }}</p>

        <p v-if="!reviewsLoading && pendingReviews.length === 0" class="reviews__empty">
          No transfers are currently awaiting review.
        </p>

        <div v-for="review in pendingReviews" :key="review.jobKey" class="review-card">
          <div class="review-card__row">
            <span class="review-card__label">From</span>
            <span class="review-card__value review-card__value--mono">{{ review.fromAccount }}</span>
          </div>
          <div class="review-card__row">
            <span class="review-card__label">To</span>
            <span class="review-card__value review-card__value--mono">{{ review.toAccount }}</span>
          </div>
          <div class="review-card__row">
            <span class="review-card__label">Amount</span>
            <span class="review-card__value review-card__value--mono">{{ formatAmount(review.amount) }}</span>
          </div>
          <div class="review-card__row">
            <span class="review-card__label">Risk score</span>
            <span class="review-card__value review-card__value--mono">{{ review.riskScore }}</span>
          </div>
          <div v-if="review.reason" class="review-card__row">
            <span class="review-card__label">Reason</span>
            <span class="review-card__value">{{ review.reason }}</span>
          </div>

          <div class="review-card__actions">
            <button
              type="button"
              class="review-btn review-btn--decline"
              :disabled="decidingJobKey === review.jobKey"
              @click="decideReview(review.jobKey, false)"
            >
              Decline
            </button>
            <button
              type="button"
              class="review-btn review-btn--approve"
              :disabled="decidingJobKey === review.jobKey"
              @click="decideReview(review.jobKey, true)"
            >
              {{ decidingJobKey === review.jobKey ? 'Submitting…' : 'Approve' }}
            </button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'

// Base URL for your transfer-backend API. Adjust if you're proxying
// through Vite (e.g. leave as '' if using a dev-server proxy for /api).
const API_BASE = import.meta.env.VITE_API_BASE_URL ?? ''

const accounts = ref([])
const accountsLoading = ref(false)
const accountsError = ref('')

const role = ref('user')

const form = reactive({
  fromAccount: '',
  toAccount: '',
  amount: null,
})

const submitting = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

// Tracks a submitted transfer until its balance change shows up, since
// completion happens asynchronously (fraud check, possibly a compliance
// review) after the POST returns.
const transferStatus = ref('idle') // 'idle' | 'processing' | 'completed' | 'timeout'
let pollIntervalId = null
let pollTimeoutId = null
const POLL_INTERVAL_MS = 3000
const POLL_TIMEOUT_MS = 60000

async function fetchAccounts({ silent = false } = {}) {
  if (!silent) accountsLoading.value = true
  accountsError.value = ''
  try {
    const response = await fetch(`${API_BASE}/accounts`)
    if (!response.ok) {
      throw new Error(`Request failed with status ${response.status}`)
    }
    // Expected shape: [{ accountNumber, balance }, ...]
    accounts.value = await response.json()
  } catch (err) {
    if (!silent) accountsError.value = 'Could not load accounts. Check that transfer-backend is running.'
  } finally {
    if (!silent) accountsLoading.value = false
  }
}

onMounted(fetchAccounts)

const visibleAccounts = computed(() => accounts.value)

// Destination list excludes whichever account is currently selected as the source
const destinationAccounts = computed(() =>
  accounts.value.filter((account) => account.accountNumber !== form.fromAccount)
)

const referenceCode = computed(() => {
  const stamp = Date.now().toString(36).toUpperCase().slice(-6)
  return `TRF-${stamp}`
})

function formatAmount(value) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value)
}

async function submitTransfer() {
  errorMessage.value = ''
  successMessage.value = ''
  stopPolling()

  if (form.fromAccount === form.toAccount) {
    errorMessage.value = 'The destination account must be different from the source account.'
    return
  }

  const sourceAccountNumber = form.fromAccount
  const balanceBefore = accounts.value.find(
    (account) => account.accountNumber === sourceAccountNumber
  )?.balance

  submitting.value = true
  try {
    const response = await fetch(`${API_BASE}/transfers`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        fromAccount: form.fromAccount,
        toAccount: form.toAccount,
        amount: form.amount,
        referenceCode: referenceCode.value,
      }),
    })

    if (!response.ok) {
      const body = await response.json().catch(() => null)
      throw new Error(body?.message || `Request failed with status ${response.status}`)
    }

    successMessage.value = 'Transfer submitted. The fraud and balance checks are now running.'
    form.toAccount = ''
    form.amount = null

    startPolling(sourceAccountNumber, balanceBefore)
  } catch (err) {
    errorMessage.value = err.message || 'The transfer could not be sent. Try again in a moment.'
  } finally {
    submitting.value = false
  }
}

function startPolling(sourceAccountNumber, balanceBefore) {
  transferStatus.value = 'processing'

  pollIntervalId = setInterval(async () => {
    await fetchAccounts({ silent: true })
    const current = accounts.value.find(
      (account) => account.accountNumber === sourceAccountNumber
    )?.balance

    if (current !== undefined && current !== balanceBefore) {
      transferStatus.value = 'completed'
      stopPolling()
    }
  }, POLL_INTERVAL_MS)

  pollTimeoutId = setTimeout(() => {
    if (transferStatus.value === 'processing') {
      transferStatus.value = 'timeout'
    }
    stopPolling()
  }, POLL_TIMEOUT_MS)
}

function stopPolling() {
  if (pollIntervalId) {
    clearInterval(pollIntervalId)
    pollIntervalId = null
  }
  if (pollTimeoutId) {
    clearTimeout(pollTimeoutId)
    pollTimeoutId = null
  }
}

onUnmounted(stopPolling)

async function refreshBalances() {
  await fetchAccounts()
}

// --- Compliance review queue (admin mode) ---

const pendingReviews = ref([])
const reviewsLoading = ref(false)
const reviewsError = ref('')
const reviewDecisionError = ref('')
const decidingJobKey = ref(null)

async function fetchPendingReviews() {
  reviewsLoading.value = true
  reviewsError.value = ''
  try {
    const response = await fetch(`${API_BASE}/reviews`)
    if (!response.ok) {
      throw new Error(`Request failed with status ${response.status}`)
    }
    // Expected shape: [{ jobKey, fromAccount, toAccount, amount, riskScore, reason, receivedAt }, ...]
    pendingReviews.value = await response.json()
  } catch (err) {
    reviewsError.value = 'Could not load pending reviews. Check that transfer-backend is running.'
  } finally {
    reviewsLoading.value = false
  }
}

async function decideReview(jobKey, approved) {
  reviewDecisionError.value = ''
  decidingJobKey.value = jobKey
  try {
    const response = await fetch(`${API_BASE}/reviews/${jobKey}/decision`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        approved,
        reviewer: 'admin',
      }),
    })

    if (!response.ok) {
      const body = await response.json().catch(() => null)
      throw new Error(body?.message || `Request failed with status ${response.status}`)
    }

    // Remove it locally rather than waiting on a full refetch
    pendingReviews.value = pendingReviews.value.filter((review) => review.jobKey !== jobKey)
  } catch (err) {
    reviewDecisionError.value = err.message || 'The decision could not be submitted. Try again.'
  } finally {
    decidingJobKey.value = null
  }
}

// Load the review queue whenever admin mode is entered
watch(role, (newRole) => {
  if (newRole === 'admin') {
    fetchPendingReviews()
  }
})
</script>

<style scoped>
.slip-page {
  --ink: #1b2a41;
  --paper: #faf9f4;
  --paper-shadow: #ece8dd;
  --ledger-green: #2f6f4e;
  --ledger-green-dark: #234f38;
  --stamp-amber: #c97c1c;
  --graphite: #6b7280;
  --alert: #b3261e;
  --hairline: rgba(27, 42, 65, 0.14);

  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 20px;
  background: transparent;
  font-family: 'Inter', system-ui, sans-serif;
  color: var(--ink);
}

.slip {
  position: relative;
  width: 100%;
  max-width: 480px;
  background: #fffdf8;
  border: 1px solid var(--hairline);
  box-shadow: 0 24px 60px -30px rgba(27, 42, 65, 0.45);
  padding: 40px 36px 32px;
}

.slip__perforation {
  position: absolute;
  top: -1px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  padding: 0 12px;
  transform: translateY(-50%);
}

.slip__perforation-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--paper);
  box-shadow: 0 0 0 1px var(--hairline);
}

.slip__eyebrow {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 11px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--graphite);
  margin-bottom: 6px;
}

.slip__title {
  font-family: 'Fraunces', Georgia, serif;
  font-weight: 600;
  font-size: 30px;
  margin: 0 0 22px;
  letter-spacing: -0.01em;
}

.role-toggle {
  display: inline-flex;
  border: 1px solid var(--hairline);
  padding: 3px;
  gap: 3px;
  margin-bottom: 8px;
}

.role-toggle__option {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 12px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  padding: 7px 16px;
  background: transparent;
  border: none;
  cursor: pointer;
  color: var(--graphite);
  transition: background 0.15s ease, color 0.15s ease;
}

.role-toggle__option--active {
  background: var(--ink);
  color: var(--paper);
}

.role-toggle__option--admin.role-toggle__option--active {
  background: transparent;
  color: var(--stamp-amber);
  position: relative;
}

.stamp {
  display: inline-block;
  border: 2px solid var(--stamp-amber);
  border-radius: 3px;
  padding: 2px 8px;
  transform: rotate(-4deg);
  font-weight: 700;
}

.slip__form {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-top: 12px;
  border-top: 1px dashed var(--hairline);
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field__label {
  font-family: 'Fraunces', Georgia, serif;
  font-size: 13px;
  color: var(--graphite);
}

.field__control {
  font-family: 'Inter', system-ui, sans-serif;
  font-size: 15px;
  padding: 11px 12px;
  background: var(--paper);
  border: 1px solid var(--hairline);
  color: var(--ink);
  outline: none;
  transition: border-color 0.15s ease;
}

.field__control:focus-visible {
  border-color: var(--ledger-green);
  box-shadow: 0 0 0 3px rgba(47, 111, 78, 0.18);
}

.field__control--mono {
  font-family: 'IBM Plex Mono', monospace;
  letter-spacing: 0.02em;
}

.field__control--select {
  appearance: none;
  background-image: linear-gradient(45deg, transparent 50%, var(--graphite) 50%),
    linear-gradient(135deg, var(--graphite) 50%, transparent 50%);
  background-position: calc(100% - 18px) center, calc(100% - 13px) center;
  background-size: 5px 5px, 5px 5px;
  background-repeat: no-repeat;
}

.amount-control {
  display: flex;
  align-items: center;
  border: 1px solid var(--hairline);
  background: var(--paper);
}

.amount-control:focus-within {
  border-color: var(--ledger-green);
  box-shadow: 0 0 0 3px rgba(47, 111, 78, 0.18);
}

.amount-control__prefix {
  padding: 0 0 0 12px;
  color: var(--graphite);
  font-family: 'IBM Plex Mono', monospace;
}

.field__control--amount {
  border: none;
  background: transparent;
  box-shadow: none !important;
}

.admin-field .field__label {
  color: var(--stamp-amber);
}

.reviews {
  padding-top: 12px;
  border-top: 1px dashed var(--hairline);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.reviews__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.reviews__refresh {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 11px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  background: transparent;
  border: 1px solid var(--hairline);
  color: var(--graphite);
  padding: 6px 12px;
  cursor: pointer;
  transition: border-color 0.15s ease, color 0.15s ease;
}

.reviews__refresh:hover:not(:disabled) {
  border-color: var(--ink);
  color: var(--ink);
}

.reviews__refresh:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.reviews__empty {
  font-size: 13px;
  color: var(--graphite);
  margin: 0;
}

.review-card {
  position: relative;
  border: 1px solid var(--hairline);
  padding: 16px 16px 14px;
  background: var(--paper);
}

.review-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--stamp-amber);
}

.review-card__row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 4px 0;
  font-size: 13px;
}

.review-card__label {
  color: var(--graphite);
  font-family: 'Fraunces', Georgia, serif;
}

.review-card__value {
  color: var(--ink);
  text-align: right;
}

.review-card__value--mono {
  font-family: 'IBM Plex Mono', monospace;
}

.review-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--hairline);
}

.review-btn {
  font-family: 'Inter', system-ui, sans-serif;
  font-size: 13px;
  font-weight: 600;
  padding: 9px 16px;
  border: none;
  cursor: pointer;
  transition: background 0.15s ease, opacity 0.15s ease;
}

.review-btn--approve {
  background: var(--ledger-green, #2f6f4e);
  color: #ffffff;
}

.review-btn--approve:hover:not(:disabled) {
  background: var(--ledger-green-dark, #234f38);
}

.review-btn--decline {
  background: transparent;
  color: var(--alert, #b3261e);
  border: 1px solid var(--alert, #b3261e);
}

.review-btn--decline:hover:not(:disabled) {
  background: rgba(179, 38, 30, 0.08);
}

.review-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-error {
  margin: 0;
  font-size: 13px;
  color: var(--alert);
}

.form-success {
  margin: 0;
  font-size: 13px;
  color: var(--ledger-green-dark);
}

.slip__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px dashed var(--hairline);
}

.slip__footer-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.refresh-btn {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 11px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  background: transparent;
  border: 1px solid var(--hairline, rgba(27, 42, 65, 0.14));
  color: var(--graphite, #6b7280);
  padding: 10px 14px;
  cursor: pointer;
  transition: border-color 0.15s ease, color 0.15s ease;
}

.refresh-btn:hover:not(:disabled) {
  border-color: var(--ink, #1b2a41);
  color: var(--ink, #1b2a41);
}

.refresh-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.status-line {
  margin: 0;
  font-size: 13px;
  font-family: 'IBM Plex Mono', monospace;
}

.status-line--processing {
  color: var(--graphite, #6b7280);
}

.status-line--completed {
  color: var(--ledger-green-dark, #234f38);
  font-weight: 600;
}

.status-line--timeout {
  color: var(--stamp-amber, #c97c1c);
}

.slip__ref {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 12px;
  color: var(--graphite);
}

.submit-btn {
  font-family: 'Inter', system-ui, sans-serif;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  background: var(--ledger-green, #2f6f4e);
  border: none;
  padding: 12px 22px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.submit-btn:hover:not(:disabled) {
  background: var(--ledger-green-dark, #234f38);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (prefers-reduced-motion: reduce) {
  .role-toggle__option,
  .submit-btn {
    transition: none;
  }
}

@media (max-width: 420px) {
  .slip {
    padding: 32px 22px 26px;
  }
}
</style>