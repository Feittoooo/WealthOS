# Roadmap

English | [Español](ROADMAP.es.md)

Roughly in the order I want to do things. No dates, this is a side project. Effort estimates for many of these are in [ISSUES.md](ISSUES.md).

## Phase 1: Core (MVP) - done

- Manual portfolio tracking: accounts, assets built from their operations, average-cost gains, weights, partial sales
- Basic tax estimates: quarterly 303, cumulative 130, cuota tracker, tax calendar
- Dark and light mode
- English and Spanish interface
- Net worth history rebuilt from the operations, with All time / 5Y / 3Y / 1Y / YTD / custom ranges
- Railway deployment with optional Basic auth
- Purchases by units or money, optional prices and date, the price lock; buying what you already hold adds to it (matched by ISIN or ticker)
- Every buy, sale and transfer can be edited or deleted on its own
- Sales by units, money or everything, on any date; fund transfers (traspasos) that carry the cost
- Export and import of the investment data
- REST API + Angular frontend, in-app debug page with request tracing

## Phase 2: Data and automation

- Automatic prices for stocks, ETFs, funds and crypto from the Yahoo Finance API, as an implementation of `PriceProvider`. The lock, empty prices, past-date sales and the daily refresh are already wired to that interface. Opt-in, because it's the first thing that would talk to the outside world.
- CSV import of broker transactions. DEGIRO and Interactive Brokers first, then a generic "map the columns" screen.
- PDF export of the quarterly tax summary, ready to send to the gestor.
- Currency conversion between EUR, USD and GBP using the ECB daily rates, stored so past values don't change.
- FIFO gains next to average cost. Spain's IRPF uses FIFO for shares and funds; the operations needed for it are already stored.
- Backup and restore of the tax data too (the investments export already exists).
- Database migrations with Flyway instead of letting Hibernate update the schema.

## Phase 3: Advanced tax

- The full modelo 130 with every deduction: gastos de difícil justificación, the housing loan deduction, negative results from earlier quarters, and the 70% withholding check done against the previous year like the law says.
- The full modelo 303 with all the IVA cases: prorrata, intra-EU operations (reverse charge), exports, recargo de equivalencia, carrying negative results forward and asking for the refund in Q4.
- Modelo 390, built from the four 303s of the year.
- Pre-filled PDFs that follow the official boxes (casillas).
- Tax history across several years.
- Modelos 111 and 115, for autónomos with employees or a rented office.
- Regional holidays that move deadlines, and the Basque Country and Navarre regimes.

## Phase 4: Intelligence

- An assistant for tax questions. By default a local model through Ollama so nothing leaves your machine; a hosted model only if you add an API key yourself.
- Automatic categories for expenses, learning from how you've labelled things before.
- Suggestions to pay less tax legally: missed deductions, how much to set aside, 7% vs 15% withholding.
- Comparison against indexes like the IBEX 35, the S&P 500 or the MSCI World.
- Alerts by email or Telegram for deadlines, big price moves, stale prices and the cuota.
- Money-weighted return (XIRR), so deposits and withdrawals don't distort the numbers.

## Phase 5: Integrations

- Open Banking / PSD2 to sync bank balances through an aggregator, with the tokens encrypted on disk.
- Broker APIs: DEGIRO (unofficial) and Interactive Brokers.
- Downloading documents from Hacienda with your digital certificate: datos fiscales, filed returns, notifications.
- Read-only sync for crypto exchanges and wallets.
- Several users or a shared household view, with real authentication.

## Things that won't change

Your data stays on your machine. Anything that connects to an outside service will be optional and documented. Tax numbers will always say they're estimates and what they assume. And it stays a single jar you can run with nothing but Java installed.
