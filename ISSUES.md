# First batch of issues

English | [Español](ISSUES.es.md)

Twelve issues to open on GitHub. Effort is a rough guess: S is a day or less, M two to four days, L one or two weeks, XL more than that.

With the GitHub CLI each one is `gh issue create --title "..." --label "..." --body "..."`.

---

## 1. [Enhancement] Automatic stock price fetching via Yahoo Finance API

Labels: `enhancement`, `investments`, `phase-2`, `external-api`
Effort: M

Everything around it is already in place: the `PriceProvider` interface (`portfolio/price`), the lock on each asset, the rules for empty prices, sales and transfers on past dates, the daily `PriceRefreshJob` and a "Refresh prices now" button on the Debug page. `WithPriceSourceTest` runs all of it against a fake provider.

What's left is a `YahooPriceProvider` that implements `latest` and `on(date)`, annotated with `@ConditionalOnProperty(name = "wealthos.prices.provider", havingValue = "yahoo")`. Notes:

- Off by default. The README must say that turning it on sends tickers to an outside service.
- Assets already have `priceSymbol` for when the ticker isn't Yahoo's symbol (e.g. `IWDA.AS`), and the asset edit dialog has a field for it. When it's empty the ISIN is used, then the ticker.
- On failure, return empty and let the existing fallbacks work; log a warning.
- Tests with a stubbed HTTP client, no real calls.

## 2. [Enhancement] CSV transaction import from DEGIRO / Interactive Brokers

Labels: `enhancement`, `investments`, `import`, `phase-2`
Effort: L

Read the DEGIRO "Transactions" export and an IBKR Flex Query CSV and turn them into positions and activity. Before saving anything, show a preview with the rows, the positions it found and possible duplicates. Importing the same file twice must not duplicate anything (hash each row). Test fixtures have to be invented data.

## 3. [Enhancement] PDF export of quarterly tax summary

Labels: `enhancement`, `tax`, `export`, `phase-2`
Effort: M

An "Export PDF" button on the tax page that produces one page for the selected quarter: the 303 and 130 breakdowns plus the list of entries. Something you can email to your gestor as is. Generate it on the server from a Thymeleaf template (OpenPDF or Flying Saucer) and include the "estimates only" note and the generation date.

## 4. [Enhancement] Currency conversion support

Labels: `enhancement`, `investments`, `phase-2`
Effort: M

Accounts can be in USD, GBP or CHF, but totals currently add everything up at face value, which is wrong. Needs:

- a configurable base currency
- an `ExchangeRate` table with daily rates (manual entry first, ECB download as an option)
- totals and the net worth chart converted to the base currency, while each asset still shows its own currency

Once this is in, remove the footer note about amounts being added without conversion.

## 5. [Enhancement] Full modelo 130 calculation engine

Labels: `enhancement`, `tax`, `phase-3`, `help-wanted`
Effort: L

`TaxCalculator` should follow the real form box by box (casillas 01 to 19). That means gastos de difícil justificación with a configurable percentage and cap, the housing loan deduction (casilla 16), negative results from earlier quarters (casilla 15), and the 70% withholding exemption checked against the previous year instead of the current one. Ideally the tests get checked against numbers from a real gestor, anonymised.

## 6. [Enhancement] Full modelo 303 IVA calculation

Labels: `enhancement`, `tax`, `phase-3`, `help-wanted`
Effort: XL

The big one. Separate bases for each rate and for exempt operations, intra-EU purchases and services (reverse charge), exports and imports, general and special prorrata, carrying negative results to the next quarter and asking for a refund in Q4. The data model will need changes; existing entries must survive the migration.

## 7. [Enhancement] Multi-year tax history view

Labels: `enhancement`, `tax`, `phase-3`
Effort: M

A `/tax/history` page with one row per year: income, expenses, net income, total 303, total 130 and effective rate. A small chart comparing years, and each row links to that year's overview.

## 8. [Enhancement] Portfolio benchmark comparison vs IBEX/S&P500

Labels: `enhancement`, `investments`, `phase-4`
Effort: L

Compute a time-weighted return from the operations and cash flows (the net worth series already rebuilds the value on any day), and plot it next to an index series (manual CSV to start with) normalised to 100. Use the same range buttons as the net worth chart.

## 9. [Enhancement] Email/Telegram alerts for fiscal deadlines

Labels: `enhancement`, `notifications`, `phase-4`
Effort: M

A `Notifier` interface with SMTP and Telegram bot implementations, configured with environment variables. Send reminders N days before each deadline (7 and 1 by default) with the estimated amount. Off by default, and the message should only include what the user has agreed to share.

## 10. [Bug] Validate edge cases in gain/loss calculation when position is partially sold

Labels: `bug`, `investments`, `calculations`, `good-first-issue`
Effort: S

`TradeService.sell` uses average cost. Partial sales, selling everything, selling by money and selling more than you hold are covered in `TradeServiceTest`. Still open:

- selling a fraction of a crypto asset with 8 decimals: rounding of what's left and of the realised gain
- selling at exactly the average price should give a gain of exactly 0.00
- a purchase at price 0 (airdrops, gifts) can't be recorded today because prices must be above zero; decide whether to allow it
- editing an old purchase after a partial sale changes the gain of that sale, because everything is replayed; decide whether that's right or the sale should keep its gain
- realised gains only live in the activity log; a `RealizedGain` table would help for the Renta

Done when each case has a test in `TradeServiceTest`, the expected behaviour is written down, and nothing else breaks.

## 11. [Enhancement] Open Banking PSD2 integration for automatic bank sync

Labels: `enhancement`, `integrations`, `phase-5`, `security`
Effort: XL

Sync balances (and optionally transactions) through an aggregator, starting with GoCardless Bank Account Data. Needs the OAuth consent flow, tokens encrypted at rest with a key from the environment, a written threat model, and an update to the privacy section of the README. Strictly opt-in.

## 12. [Enhancement] AI assistant for tax guidance

Labels: `enhancement`, `ai`, `tax`, `phase-4`
Effort: L

A chat panel on the tax page. By default it talks to a local model through Ollama so nothing leaves the machine. A hosted model only with an API key the user adds and an explicit consent switch. It should only see the aggregates of the selected quarter unless the user allows more, treat entry text as data (not instructions), and every answer has to carry the "estimates only, check with your gestor" note with links to the relevant AEAT pages.
