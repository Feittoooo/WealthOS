// Shapes returned by the Spring API. Keep in sync with the records in the *.dto packages.
// Dates come as ISO strings ("2026-10-19"), year-months as "2026-10", numbers as numbers.

export type AccountType = 'BANK' | 'BROKERAGE' | 'CRYPTO_WALLET' | 'CASH';
export type AssetType = 'STOCK' | 'ETF' | 'FUND' | 'CRYPTO' | 'OTHER';
export type AmountMode = 'UNITS' | 'MONEY' | 'ALL';
export type TaxEntryType = 'INGRESO' | 'GASTO';
export type TransactionType =
  | 'DEPOSIT' | 'WITHDRAWAL' | 'BUY' | 'SELL' | 'DIVIDEND' | 'INCOME' | 'EXPENSE' | 'TRANSFER' | 'ADJUSTMENT';

export const ACCOUNT_TYPES: AccountType[] = ['BANK', 'BROKERAGE', 'CRYPTO_WALLET', 'CASH'];
export const ASSET_TYPES: AssetType[] = ['STOCK', 'ETF', 'FUND', 'CRYPTO', 'OTHER'];
export const CURRENCIES = ['EUR', 'USD', 'GBP', 'CHF'];

export interface ApiError {
  status: number;
  code: string;
  message: string;
  requestId: string | null;
  fields: { field: string; message: string }[];
}

export interface AppInfo {
  languages: string[];
  numberLocale: string;
  baseCurrency: string;
  debugEnabled: boolean;
  priceSource: PriceSourceStatus;
}

export interface PriceSourceStatus {
  name: string;
  available: boolean;
}

export interface Saved<T> {
  item: T;
  warnings: string[];
}

// --- portfolio

/** One asset (the group): totals worked out from all its operations. */
export interface AssetView {
  id: number;
  accountId: number;
  accountName: string;
  ticker: string;
  isin: string | null;
  name: string;
  assetType: AssetType;
  currency: string;
  quantity: number;
  avgBuyPrice: number;
  currentPrice: number;
  costBasis: number;
  marketValue: number;
  gain: number;
  gainPct: number;
  realisedGain: number;
  weight: number;
  firstDate: string | null;
  lastDate: string | null;
  operationsCount: number;
  open: boolean;
  autoPrice: boolean;
  priceSymbol: string | null;
  priceUpdatedAt: string | null;
}

export type OperationType = 'BUY' | 'SELL' | 'TRANSFER_IN' | 'TRANSFER_OUT';

/** One buy, sale or transfer leg inside an asset. */
export interface OperationView {
  id: number;
  type: OperationType;
  date: string;
  units: number;
  price: number;
  amount: number;
  realisedGain: number | null;
  unitsAfter: number;
  transferId: string | null;
  counterpart: string | null;
  cashCredited: boolean;
}

export interface AssetDetail {
  asset: AssetView;
  operations: OperationView[];
}

export interface AccountView {
  id: number;
  name: string;
  type: AccountType;
  institution: string | null;
  currency: string;
  cashBalance: number;
  positionsValue: number;
  positionsCost: number;
  positionsGain: number;
  positionsCount: number;
  total: number;
  weight: number;
}

export interface PortfolioSummary {
  netWorth: number;
  totalCash: number;
  totalInvested: number;
  marketValue: number;
  totalReturn: number;
  totalReturnPct: number;
  accountsCount: number;
  positionsCount: number;
  bestPerformer: AssetView | null;
  worstPerformer: AssetView | null;
}

export interface Slice {
  key: string | null;
  label: string | null;
  value: number;
  color: string;
}

export interface PortfolioOverview {
  summary: PortfolioSummary;
  assets: AssetView[];
  accounts: AccountView[];
  byType: Slice[];
  byAccount: Slice[];
  priceSource: PriceSourceStatus;
}

export type NetWorthRange = 'ALL' | '5Y' | '3Y' | '1Y' | 'YTD' | 'CUSTOM';

export interface NetWorthSeries {
  from: string;
  to: string;
  step: 'day' | 'week' | 'month';
  estimated: boolean;
  points: { date: string; value: number; invested: number }[];
}

export interface AccountRequest {
  name: string;
  type: AccountType;
  institution: string | null;
  currency: string;
  balance: number;
}

export interface BuyRequest {
  accountId: number;
  ticker: string;
  isin: string | null;
  name: string | null;
  assetType: AssetType;
  amountMode: AmountMode;
  amount: number;
  price: number | null;
  currentPrice: number | null;
  date: string | null;
  autoPrice: boolean;
}

export interface BuyResult {
  asset: AssetView;
  merged: boolean;
  warnings: string[];
}

export interface AssetUpdateRequest {
  ticker: string;
  isin: string | null;
  name: string | null;
  assetType: AssetType;
  currentPrice: number | null;
  autoPrice: boolean;
  priceSymbol: string | null;
}

export interface OperationUpdateRequest {
  date: string;
  units: number;
  price: number;
}
export interface SellRequest {
  mode: AmountMode;
  amount: number | null;
  price: number | null;
  date: string | null;
  creditCash: boolean;
}

export interface SellResult {
  ticker: string;
  date: string;
  quantity: number;
  price: number;
  proceeds: number;
  realisedGain: number;
  closed: boolean;
  cashCredited: boolean;
}

export interface TransferRequest {
  mode: AmountMode;
  amount: number | null;
  date: string | null;
  originPrice: number | null;
  destinationAssetId: number | null;
  newFund: {
    accountId: number; ticker: string; isin: string | null; name: string | null; autoPrice: boolean; priceSymbol: string | null
  } | null;
  destinationPrice: number | null;
}

export interface TransferResult {
  originTicker: string;
  destinationTicker: string;
  destinationAssetId: number;
  date: string;
  unitsOut: number;
  value: number;
  unitsIn: number;
  costCarried: number;
  originClosed: boolean;
}

export interface ActivityItem {
  id: number;
  date: string;
  type: TransactionType;
  direction: number;
  messageCode: string | null;
  args: unknown[];
  description: string;
  amount: number;
  currency: string;
  accountName: string | null;
}

// --- tax

export interface QuarterSummary {
  year: number;
  quarter: number;
  from: string;
  to: string;
  filingDeadline: string;
  entryCount: number;
  income: number;
  expenses: number;
  deductibleExpenses: number;
  cuota: number;
  netIncome: number;
  ivaRepercutido: number;
  ivaSoportado: number;
  ivaResult: number;
  retentions: number;
  ytdIncome: number;
  ytdDeductible: number;
  ytdNetIncome: number;
  ytdRetentions: number;
  ytdIncomeWithRetentionPct: number;
  modelo130Gross: number;
  previousPayments: number;
  modelo130Result: number;
  ivaToPay: number;
  modelo130ToPay: number;
  totalToPay: number;
  hasData: boolean;
  likely130Exempt: boolean;
}

export interface FiscalDeadline {
  date: string;
  titleKey: string;
  descriptionKey: string;
  periodKey: string;
  periodArgs: unknown[];
  estimate: number | null;
  daysLeft: number;
}

export interface TaxEntryView {
  id: number;
  date: string;
  concept: string;
  counterparty: string | null;
  category: string | null;
  type: TaxEntryType;
  baseAmount: number;
  ivaRate: number;
  ivaAmount: number;
  irpfRetentionRate: number;
  retentionAmount: number;
  deductible: boolean;
  total: number;
}

export interface TaxEntryRequest {
  date: string;
  concept: string;
  counterparty: string | null;
  category: string | null;
  type: TaxEntryType;
  baseAmount: number;
  ivaRate: number;
  irpfRetentionRate: number;
  deductible: boolean;
}

export interface TaxSettings {
  monthlyCuota: number;
  defaultRetentionRate: number;
  cuotaDeductible: boolean;
  showOnDashboard: boolean;
}

export interface CuotaMonth {
  month: string;
  paid: boolean;
  current: boolean;
  future: boolean;
}

export interface TaxOverview {
  year: number;
  quarter: number;
  currentYear: number;
  currentQuarter: number;
  today: string;
  summary: QuarterSummary;
  yearSummaries: QuarterSummary[];
  yearTotalToPay: number;
  entries: TaxEntryView[];
  deadlines: FiscalDeadline[];
  settings: TaxSettings;
  cuotaMonths: CuotaMonth[];
  cuotaPaidInYear: number;
}

// --- dashboard

export interface DashboardView {
  empty: boolean;
  summary: PortfolioSummary;
  changeSinceLastMonth: number | null;
  changeSinceLastMonthPct: number | null;
  allocation: Slice[];
  recentActivity: ActivityItem[];
  showTax: boolean;
  deadlines: FiscalDeadline[];
}

// --- debug

export interface ServerLogEntry {
  id: number;
  time: string;
  level: string;
  logger: string;
  thread: string;
  requestId: string | null;
  message: string;
  stackTrace: string | null;
}

export interface RequestTrace {
  id: string;
  time: string;
  method: string;
  path: string;
  status: number;
  durationMs: number;
  language: string | null;
}

export interface LoggerLevel {
  name: string;
  level: string;
}
