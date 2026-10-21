# WealthOS

English | [Español](README.es.md)

A self-hosted dashboard for your net worth, your investments and your taxes as a freelancer in Spain.

<!-- Add screenshot here -->

![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?logo=springboot&logoColor=white)
![Angular](https://img.shields.io/badge/Angular-19-DD0031?logo=angular&logoColor=white)
![H2](https://img.shields.io/badge/H2-embedded-1F4E79)
![Chart.js](https://img.shields.io/badge/Chart.js-4-FF6384?logo=chartdotjs&logoColor=white)
![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)

## What it does

You add your accounts (bank, broker, crypto wallet, cash) and what you hold in them, and WealthOS works out your net worth, gains, returns and how everything is split. Cash and invested money are kept apart and only added up per account. Every purchase, sale and transfer is kept as an operation, and the net worth chart is rebuilt from them, so something you bought two years ago shows up two years ago.

If you're an autónomo, you can also log the invoices you send and the expenses you pay. From those it estimates what you'll owe each quarter for IVA (modelo 303) and IRPF (modelo 130), tracks your cuota de autónomos and shows the upcoming filing dates.

Everything is stored in an H2 database file next to the app. Nothing is sent anywhere.

The interface is in English and in Spanish (Spain). You can switch from the sidebar.

## Who it's for

I built it for myself: I wanted one place for my investments and my quarterly taxes, without giving my bank login to yet another app. It should be useful for:

- anyone who wants to track a portfolio by hand and keep the data at home
- freelancers in Spain who want a rough idea of the 303 and the 130 before the gestor sends the numbers

## Quick start

You only need Java 17 or newer. Maven and Node are downloaded by the build the first time (into `~/.m2` and `frontend/node`), so there's nothing else to install.

```bash
git clone https://github.com/Feittoooo/WealthOS.git
cd wealthos
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run
```

Then open http://localhost:8080. The first run takes a few minutes (Maven, Node and the npm packages get downloaded); after that it starts in seconds. On an empty database you can create your first account or use **Data → Load sample data** to look around with a made-up portfolio.

Your data ends up in `./data/wealthos.mv.db`. That folder is git-ignored and survives restarts. **Data → Export investments** saves your accounts, assets and every operation to a JSON file that you can load back with **Data → Import investments**, on this machine or another one.

To build a jar:

```bash
./mvnw clean package
java -jar target/wealthos-0.0.1-SNAPSHOT.jar
```

### Working on the code

The backend and the frontend can be run separately so you get hot reload on the Angular side:

```bash
./mvnw spring-boot:run -Dskip.npm -Dskip.installnodenpm   # API on :8080, skips the Angular build
cd frontend && npm install && npm start                   # UI on :4200, proxies /api to :8080
```

Tests: `./mvnw test` runs the Java tests and the frontend unit tests (Vitest). `npm test` inside `frontend/` runs only the frontend ones.

### Settings

| Property / env var | Default | What it does |
|---|---|---|
| `PORT` | `8080` | HTTP port |
| `WEALTHOS_PASSWORD` | empty | Turns on HTTP Basic auth. Set it on anything public. |
| `WEALTHOS_USERNAME` | `admin` | Username for Basic auth |
| `WEALTHOS_DEBUG` | `true` | The Debug page and `/api/debug`. Turn it off on public deployments. |
| `WEALTHOS_PRICES` | `none` | Market price source. Only `none` exists for now (see below). |
| `wealthos.locale` | `es-ES` | How numbers and amounts are formatted (`en-IE`, `en-US`...) |

The UI language follows your browser the first time (Spanish if it asks for Spanish, English otherwise) and then whatever you pick in the sidebar.

## How it's built

```
src/main/java/com/wealthos/
  portfolio/    accounts, assets and their operations, sales, fund transfers, net worth history
  tax/          tax entries, 303/130 calculator, fiscal calendar
  activity/     the movement log shared by both modules
  dashboard/    puts the home page together from the other modules
  data/         export, import, sample data, delete all
  i18n/         language from Accept-Language, serves the texts to the frontend
  system/       app info and the debug endpoints (log buffer, request trace)
  common/       error responses, number formatting
frontend/src/app/
  core/         HTTP interceptors, translations, formatting, debug log, app state
  shared/ui/    dialog, chart, lock toggle, segmented control...
  layout/       sidebar, toasts, confirm dialog
  features/     one folder per page: dashboard, investments, tax, data, debug
src/main/resources/messages.properties, messages_es.properties   every text, both languages
```

The backend is a plain REST API (Spring Boot, JPA, H2). The Angular app is built by Maven into the jar, so it's still a single file to run. Controllers only translate HTTP to service calls; all the rules live in the services, and the tax maths in `TaxCalculator`, which doesn't touch the database.

All the texts live in `messages.properties` and `messages_es.properties`. The backend uses them for errors and validation, and the frontend downloads them from `/api/i18n/{lang}`, so nothing is written as plain text in templates or TypeScript. A test fails if a key exists in one language and not the other.

### Market prices

Assets have a lock. With the lock on, the current price is meant to come from a price source and be refreshed every evening; with it off you type it yourself. Prices and the date are optional when you buy or sell: an empty price is looked up for that date, and if only one of the two prices is known the other one takes its value (cost = market value).

Until there's a price source, the net worth chart only knows the prices of your own operations and today's price, and draws a straight line between them. It says so under the chart.

There's no real price source yet. Everything goes through the `PriceProvider` interface and the default `NoPriceProvider` never answers, so for now you type at least one price. Issue #1 adds a Yahoo Finance implementation; the lock, the empty-field rules, sales and transfers on past dates and the daily refresh will use it without other changes. `WithPriceSourceTest` already runs all of that against a fake provider.

### How gains are worked out

Each asset is replayed from its operations in date order, using average cost:

- A sale realises `units × (sale price − average cost)`. Fund transfers realise nothing; their cost moves to the destination fund.
- Units are stored with 8 decimals and never rounded, so small crypto amounts are exact. Money is rounded to cents only for display and for each sale's gain.
- Selling at exactly the average price gives a gain of exactly 0.00.
- Prices must be above zero. For a gift or an airdrop, enter its market value on the day you got it.
- Editing or deleting an old purchase recalculates every sale after it. A change that would leave you selling more than you held is refused.
- Each sale's gain is shown on the operation and added up per asset. Nothing is stored separately, so it can't go out of date.

Spain's IRPF uses FIFO, not average cost, for shares and funds, so the gains here are an estimate. FIFO is on the roadmap.

### Debugging

The **Debug** page in the sidebar shows:

- every API call the browser made, with status, time and the request and response bodies
- the same calls as the server saw them
- the live server log
- log levels you can change at runtime (e.g. `org.hibernate.SQL` at DEBUG to see every query)
- system info

Each call carries a request id (`X-Request-Id`) that shows up in all those places and in error messages, so you can follow one click from the browser to the SQL it ran. The API docs are at `/swagger-ui.html` and the H2 console at `/h2-console` (JDBC URL `jdbc:h2:file:./data/wealthos`, user `sa`, no password, localhost only).

## Deploying on Railway

[![Deploy on Railway](https://railway.com/button.svg)](https://railway.com/new/template?template=https://github.com/Feittoooo/WealthOS)

1. Fork the repo and create a Railway project from it (New Project, Deploy from GitHub repo).
2. Railway reads `railway.json` and the `Procfile`, builds with the Maven wrapper and starts the jar on `$PORT`.
3. Add a volume mounted at `/app/data`. Without it the database is wiped on every deploy.
4. Set `WEALTHOS_PASSWORD`, and `WEALTHOS_DEBUG=false`. There's no other login.
5. Generate a domain and open it.

The button only works once a Railway template is published. Until then, follow the steps above and point the badge at your own template.

## Features

What's in v0.1:

- Dashboard with net worth, change versus last month, allocation and recent activity
- Net worth chart over all time, 5, 3 or 1 years, this year or any two dates, with the money you put in drawn under it
- Tax deadlines on the dashboard only if you turn them on from the tax page (off by default)
- Accounts of four types, each with its own currency and cash balance; assets never move money in or out of that cash on their own
- Assets in stocks, ETFs, funds, crypto or anything else. Buying something the account already holds (same ISIN, or same ticker) adds to it instead of creating a new row.
- Each asset opens into its operations: buy more, sell, transfer, edit or delete the whole asset, or fix or delete a single purchase or sale. Everything is worked out again from the operations after a change, and a change that would leave you selling more than you had is refused.
- Purchases in units or money, with optional prices and date
- The price lock, kept when you edit the asset
- Every column in the tables can be sorted; the assets table can also be filtered and shows the date of the last operation
- Sales by units, by money or everything, with optional price and date and average-cost gains. Adding the proceeds to cash is a checkbox, off by default.
- Fund transfers (traspasos) into a fund you hold or a new one. The purchase cost moves with the units and nothing is realised.
- Export and import of the investment data as JSON
- Tax entries with IVA at 21, 10, 4 or 0% and IRPF withholding at 15, 7 or 0%
- Quarterly modelo 303 and cumulative modelo 130 estimates, with a hint when you might not need to file the 130
- Cuota de autónomos tracker; paid months are deducted in the 130
- Tax calendar with the 303/130 dates (20 April, 20 July, 20 October, 30 January), the 390 and the Renta
- Dark and light theme, English and Spanish
- In-app debug page, request tracing, API docs
- Java and frontend tests

Next up: the Yahoo price source, CSV import from brokers, currency conversion and PDF reports. The rest is in [ROADMAP.md](ROADMAP.md), and the planned work is in the [issues](https://github.com/Feittoooo/WealthOS/issues).

## Contributing

Pull requests are welcome, especially from people who know Spanish tax rules better than I do.

- Open an issue first for anything bigger than a small fix, or grab one from the [issues](https://github.com/Feittoooo/WealthOS/issues); the ones labelled `good first issue` are a good start.
- Keep each PR focused on one thing.
- Run `./mvnw test` before pushing and add tests for any new calculation.
- Keep the layers apart: controllers stay thin, rules go in services, components don't call `HttpClient` directly (use the feature's `*.api.ts`).
- Any new text goes in both `messages.properties` and `messages_es.properties` (Spanish from Spain), never hard-coded.
- Never commit real financial data. Test data and samples have to be made up.

## License

[MIT](LICENSE)

## Disclaimer

The tax module gives estimates only. Always check the numbers with a gestor or tax advisor before filing anything.

This isn't tax, legal or financial advice. The calculations assume estimación directa, the general IVA regime and a single activity. They don't handle prorrata, recargo de equivalencia, intra-EU operations, gastos de difícil justificación, regional holidays and plenty of other cases. Deadlines can change from one year to the next. I take no responsibility for decisions made with this software.

About privacy: WealthOS is for your own records only, and your data stays in a local H2 file on the machine running WealthOS. The client or supplier on a tax entry is just the name on your invoice. Fonts and charts are bundled with the app, so it makes no outside requests at all.
