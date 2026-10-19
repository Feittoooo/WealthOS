-- WealthOS demo data (English). The Spanish version is sample-data.es.sql.
--
-- Everything here is made up: banks, brokers, tickers, clients, prices and amounts.
-- If something looks like a real company, it's a coincidence.
--
-- Easiest way to load it: open the dashboard on an empty database and click
-- "Load sample data". That also marks this year's cuotas as paid.
-- You can also run it by hand from /h2-console (JDBC URL jdbc:h2:file:./data/wealthos,
-- user sa, no password), but only on an empty database.
--
-- Dates are relative to CURRENT_DATE so the charts always look recent.
-- No ids are hard-coded; foreign keys are looked up by name.

INSERT INTO accounts (name, type, institution, currency, balance, created_at) VALUES
  ('Everyday account', 'BANK',          'Northwind Bank', 'EUR',  8450.20, DATEADD('MONTH', -14, CURRENT_TIMESTAMP)),
  ('Emergency fund',   'BANK',          'Northwind Bank', 'EUR', 15000.00, DATEADD('MONTH', -14, CURRENT_TIMESTAMP)),
  ('Atlas Brokerage',  'BROKERAGE',     'Atlas Markets',  'EUR',  1230.55, DATEADD('MONTH', -13, CURRENT_TIMESTAMP)),
  ('Lumen Funds',      'BROKERAGE',     'Lumen Invest',   'EUR',   420.00, DATEADD('MONTH', -12, CURRENT_TIMESTAMP)),
  ('Cold wallet',      'CRYPTO_WALLET', 'Self-custody',   'EUR',     0.00, DATEADD('MONTH', -12, CURRENT_TIMESTAMP)),
  ('Wallet cash',      'CASH',          NULL,             'EUR',   300.00, DATEADD('MONTH', -12, CURRENT_TIMESTAMP));

-- assets: one per thing held in an account; quantity and cost come from the operations below
INSERT INTO assets (account_id, ticker, isin, name, asset_type, current_price, price_updated_at, auto_price, price_symbol, created_at) VALUES
  ((SELECT id FROM accounts WHERE name = 'Atlas Brokerage'), 'ACME',    NULL,           'Acme Robotics Corp.',          'STOCK',    104.30, DATEADD('DAY', -1, CURRENT_TIMESTAMP),  FALSE, NULL, DATEADD('MONTH', -11, CURRENT_TIMESTAMP)),
  ((SELECT id FROM accounts WHERE name = 'Atlas Brokerage'), 'GLBX',    NULL,           'Globex Systems Inc.',          'STOCK',    128.75, DATEADD('DAY', -1, CURRENT_TIMESTAMP),  FALSE, NULL, DATEADD('MONTH', -10, CURRENT_TIMESTAMP)),
  ((SELECT id FROM accounts WHERE name = 'Atlas Brokerage'), 'INIT',    NULL,           'Initech Software',             'STOCK',    245.60, DATEADD('DAY', -1, CURRENT_TIMESTAMP),  FALSE, NULL, DATEADD('MONTH', -9,  CURRENT_TIMESTAMP)),
  ((SELECT id FROM accounts WHERE name = 'Atlas Brokerage'), 'SOLR',    NULL,           'Solaris Renewables',           'STOCK',     19.85, DATEADD('DAY', -12, CURRENT_TIMESTAMP), FALSE, NULL, DATEADD('MONTH', -8,  CURRENT_TIMESTAMP)),
  ((SELECT id FROM accounts WHERE name = 'Atlas Brokerage'), 'WRLD',    NULL,           'Atlas World Equity UCITS ETF', 'ETF',       91.45, DATEADD('DAY', -1, CURRENT_TIMESTAMP),  FALSE, NULL, DATEADD('MONTH', -13, CURRENT_TIMESTAMP)),
  ((SELECT id FROM accounts WHERE name = 'Atlas Brokerage'), 'AUR',     NULL,           'Aurum Physical Gold ETC',      'OTHER',    201.00, DATEADD('DAY', -4, CURRENT_TIMESTAMP),  FALSE, NULL, DATEADD('MONTH', -7,  CURRENT_TIMESTAMP)),
  ((SELECT id FROM accounts WHERE name = 'Lumen Funds'),     'LMN-GLB', 'ZZ00LUMEN012', 'Lumen Global Index Fund',      'FUND',     168.92, DATEADD('DAY', -3, CURRENT_TIMESTAMP),  FALSE, NULL, DATEADD('MONTH', -12, CURRENT_TIMESTAMP)),
  ((SELECT id FROM accounts WHERE name = 'Lumen Funds'),     'LMN-BND', 'ZZ00LUMEN029', 'Lumen Euro Bond Fund',         'FUND',      99.80, DATEADD('DAY', -3, CURRENT_TIMESTAMP),  FALSE, NULL, DATEADD('MONTH', -12, CURRENT_TIMESTAMP)),
  ((SELECT id FROM accounts WHERE name = 'Lumen Funds'),     'HZEM',    'ZZ00HRZEM034', 'Horizon Emerging Markets ETF', 'ETF',       27.15, DATEADD('DAY', -3, CURRENT_TIMESTAMP),  FALSE, NULL, DATEADD('MONTH', -6,  CURRENT_TIMESTAMP)),
  ((SELECT id FROM accounts WHERE name = 'Cold wallet'),     'ORBC',    NULL,           'OrbCoin',                      'CRYPTO', 52400.00, DATEADD('HOUR', -5, CURRENT_TIMESTAMP),  FALSE, NULL, DATEADD('MONTH', -11, CURRENT_TIMESTAMP)),
  ((SELECT id FROM accounts WHERE name = 'Cold wallet'),     'NOVA',    NULL,           'NovaChain',                    'CRYPTO',  2310.00, DATEADD('HOUR', -5, CURRENT_TIMESTAMP),  FALSE, NULL, DATEADD('MONTH', -5,  CURRENT_TIMESTAMP));

-- every purchase and sale, so the net worth chart can be rebuilt month by month
INSERT INTO asset_operations (asset_id, type, op_date, units, price, carried_cost, transfer_id, cash_credited, created_at) VALUES
  ((SELECT id FROM assets WHERE ticker = 'ACME'),    'BUY',  DATEADD('MONTH', -11, CURRENT_DATE),   20,      78.00, NULL, NULL, FALSE, CURRENT_TIMESTAMP),
  ((SELECT id FROM assets WHERE ticker = 'ACME'),    'BUY',  DATEADD('MONTH', -5, CURRENT_DATE),    20,      87.00, NULL, NULL, FALSE, CURRENT_TIMESTAMP),
  ((SELECT id FROM assets WHERE ticker = 'GLBX'),    'BUY',  DATEADD('MONTH', -10, CURRENT_DATE),   25,     140.00, NULL, NULL, FALSE, CURRENT_TIMESTAMP),
  ((SELECT id FROM assets WHERE ticker = 'INIT'),    'BUY',  DATEADD('MONTH', -9, CURRENT_DATE),    10,     200.00, NULL, NULL, FALSE, CURRENT_TIMESTAMP),
  ((SELECT id FROM assets WHERE ticker = 'INIT'),    'BUY',  DATEADD('MONTH', -2, CURRENT_DATE),     5,     230.00, NULL, NULL, FALSE, CURRENT_TIMESTAMP),
  ((SELECT id FROM assets WHERE ticker = 'SOLR'),    'BUY',  DATEADD('MONTH', -8, CURRENT_DATE),    70,      22.10, NULL, NULL, FALSE, CURRENT_TIMESTAMP),
  ((SELECT id FROM assets WHERE ticker = 'SOLR'),    'SELL', DATEADD('DAY', -31, CURRENT_DATE),     10,      21.40, NULL, NULL, FALSE, CURRENT_TIMESTAMP),
  ((SELECT id FROM assets WHERE ticker = 'WRLD'),    'BUY',  DATEADD('MONTH', -13, CURRENT_DATE),   40,      72.00, NULL, NULL, FALSE, CURRENT_TIMESTAMP),
  ((SELECT id FROM assets WHERE ticker = 'WRLD'),    'BUY',  DATEADD('MONTH', -9, CURRENT_DATE),    40,      78.90, NULL, NULL, FALSE, CURRENT_TIMESTAMP),
  ((SELECT id FROM assets WHERE ticker = 'WRLD'),    'BUY',  DATEADD('MONTH', -6, CURRENT_DATE),    30,      81.90, NULL, NULL, FALSE, CURRENT_TIMESTAMP),
  ((SELECT id FROM assets WHERE ticker = 'WRLD'),    'BUY',  DATEADD('DAY', -4, CURRENT_DATE),      10,      89.10, NULL, NULL, FALSE, CURRENT_TIMESTAMP),
  ((SELECT id FROM assets WHERE ticker = 'AUR'),     'BUY',  DATEADD('MONTH', -7, CURRENT_DATE),    10,     160.00, NULL, NULL, FALSE, CURRENT_TIMESTAMP),
  ((SELECT id FROM assets WHERE ticker = 'LMN-GLB'), 'BUY',  DATEADD('MONTH', -12, CURRENT_DATE),   40,     130.00, NULL, NULL, FALSE, CURRENT_TIMESTAMP),
  ((SELECT id FROM assets WHERE ticker = 'LMN-GLB'), 'BUY',  DATEADD('MONTH', -6, CURRENT_DATE),    25,     145.00, NULL, NULL, FALSE, CURRENT_TIMESTAMP),
  ((SELECT id FROM assets WHERE ticker = 'LMN-GLB'), 'BUY',  DATEADD('MONTH', -2, CURRENT_DATE),    20.432, 162.24, NULL, NULL, FALSE, CURRENT_TIMESTAMP),
  ((SELECT id FROM assets WHERE ticker = 'LMN-BND'), 'BUY',  DATEADD('MONTH', -12, CURRENT_DATE),   50,     101.30, NULL, NULL, FALSE, CURRENT_TIMESTAMP),
  ((SELECT id FROM assets WHERE ticker = 'HZEM'),    'BUY',  DATEADD('MONTH', -6, CURRENT_DATE),   200,      24.60, NULL, NULL, FALSE, CURRENT_TIMESTAMP),
  ((SELECT id FROM assets WHERE ticker = 'ORBC'),    'BUY',  DATEADD('MONTH', -11, CURRENT_DATE),    0.16, 37000.00, NULL, NULL, FALSE, CURRENT_TIMESTAMP),
  ((SELECT id FROM assets WHERE ticker = 'ORBC'),    'BUY',  DATEADD('DAY', -20, CURRENT_DATE),      0.02, 46000.00, NULL, NULL, FALSE, CURRENT_TIMESTAMP),
  ((SELECT id FROM assets WHERE ticker = 'NOVA'),    'BUY',  DATEADD('MONTH', -5, CURRENT_DATE),     4.2,   1850.00, NULL, NULL, FALSE, CURRENT_TIMESTAMP);

-- base_amount is the base imponible (before IVA and withholding)
INSERT INTO tax_entries (entry_date, concept, counterparty, category, type, base_amount, iva_rate, irpf_retention_rate, deductible, created_at) VALUES
  (DATEADD('DAY',   -6, CURRENT_DATE), 'Mobile app UI audit',                 'Kestrel Labs SL',    'Services', 'INGRESO', 1850.00, 21, 15, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -15, CURRENT_DATE), 'Design system maintenance (monthly)', 'Bluebird Studio SL', 'Services', 'INGRESO', 1200.00, 21, 15, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -28, CURRENT_DATE), 'Landing page redesign',               'Pinecone Media SL',  'Services', 'INGRESO', 2400.00, 21, 15, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -45, CURRENT_DATE), 'Design system maintenance (monthly)', 'Bluebird Studio SL', 'Services', 'INGRESO', 1200.00, 21, 15, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -70, CURRENT_DATE), 'Brand strategy workshop (2 days)',    'Orbital Apps SL',    'Services', 'INGRESO', 1600.00, 21, 15, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -78, CURRENT_DATE), 'Design system maintenance (monthly)', 'Bluebird Studio SL', 'Services', 'INGRESO', 1200.00, 21, 15, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY', -105, CURRENT_DATE), 'Dashboard UX research',               'Kestrel Labs SL',    'Services', 'INGRESO', 2750.00, 21, 15, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY', -120, CURRENT_DATE), 'Design system maintenance (monthly)', 'Bluebird Studio SL', 'Services', 'INGRESO', 1200.00, 21, 15, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY', -150, CURRENT_DATE), 'E-commerce checkout redesign',        'Pinecone Media SL',  'Services', 'INGRESO', 3100.00, 21, 15, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY', -172, CURRENT_DATE), 'Design system maintenance (monthly)', 'Bluebird Studio SL', 'Services', 'INGRESO', 1200.00, 21, 15, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY', -200, CURRENT_DATE), 'Online workshop tickets',             NULL,                 'Products', 'INGRESO',  450.00, 21,  0, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY', -215, CURRENT_DATE), 'Design system maintenance (monthly)', 'Bluebird Studio SL', 'Services', 'INGRESO', 1200.00, 21, 15, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY', -240, CURRENT_DATE), 'Website accessibility audit',         'Orbital Apps SL',    'Services', 'INGRESO',  980.00, 21, 15, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY', -268, CURRENT_DATE), 'Design system maintenance (monthly)', 'Bluebird Studio SL', 'Services', 'INGRESO', 1200.00, 21, 15, FALSE, CURRENT_TIMESTAMP),

  (DATEADD('DAY',   -3, CURRENT_DATE), 'Design tool, yearly licence',     'Vectorly Software',    'Software and subscriptions', 'GASTO',  144.00, 21, 0, TRUE,  CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -12, CURRENT_DATE), 'Coworking desk (monthly)',        'Hive Coworking',       'Office and coworking',       'GASTO',  165.00, 21, 0, TRUE,  CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -35, CURRENT_DATE), 'Coworking desk (monthly)',        'Hive Coworking',       'Office and coworking',       'GASTO',  165.00, 21, 0, TRUE,  CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -40, CURRENT_DATE), 'Laptop for design work',          'TechNova Store',       'Equipment',                  'GASTO', 1449.00, 21, 0, TRUE,  CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -60, CURRENT_DATE), 'Mobile and fibre (work line)',    'Telco Uno',            'Phone and internet',         'GASTO',   48.50, 21, 0, TRUE,  CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -90, CURRENT_DATE), 'UX conference ticket',            'Design Summit Events', 'Training',                   'GASTO',  220.00, 21, 0, TRUE,  CURRENT_TIMESTAMP),
  (DATEADD('DAY', -130, CURRENT_DATE), 'Gestoria, quarterly filings',     'Gestoria Ejemplo SL',  'Professional services',      'GASTO',  120.00, 21, 0, TRUE,  CURRENT_TIMESTAMP),
  (DATEADD('DAY', -160, CURRENT_DATE), 'Stock photo subscription',        'Pixabundle',           'Software and subscriptions', 'GASTO',   29.00, 21, 0, TRUE,  CURRENT_TIMESTAMP),
  (DATEADD('DAY', -190, CURRENT_DATE), 'Family dinner (not deductible)',  'La Taberna Inventada', 'Other',                      'GASTO',   85.00, 10, 0, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY', -230, CURRENT_DATE), 'Ergonomic office chair',          'Sitwell Furniture',    'Equipment',                  'GASTO',  310.00, 21, 0, TRUE,  CURRENT_TIMESTAMP),
  (DATEADD('DAY', -255, CURRENT_DATE), 'Domain and hosting (yearly)',     'Cloudnest Hosting',    'Software and subscriptions', 'GASTO',   96.00, 21, 0, TRUE,  CURRENT_TIMESTAMP);

-- activity feed. message_code + message_args let the app show it in either language.
INSERT INTO transactions (tx_date, description, message_code, message_args, type, amount, currency, account_name, created_at) VALUES
  (DATEADD('DAY', -31, CURRENT_DATE), 'Sold 10 SOLR at 21,40 € (realised -7,00 €)', 'activity.sold',     '10|SOLR|21,40 €|-7,00 €',                                   'SELL',      214.00, 'EUR', 'Atlas Brokerage', CURRENT_TIMESTAMP),
  (DATEADD('DAY', -20, CURRENT_DATE), 'Bought 0,02 ORBC at 46.000,00 €',             'activity.bought',   '0,02|ORBC|46.000,00 €',                                     'BUY',       920.00, 'EUR', 'Cold wallet',     CURRENT_TIMESTAMP),
  (DATEADD('DAY', -15, CURRENT_DATE), 'Invoice: Design system maintenance (monthly) - Bluebird Studio SL', 'activity.invoice', 'Design system maintenance (monthly)| - Bluebird Studio SL', 'INCOME', 1272.00, 'EUR', NULL, CURRENT_TIMESTAMP),
  (DATEADD('DAY', -12, CURRENT_DATE), 'Expense: Coworking desk (monthly) - Hive Coworking', 'activity.expense', 'Coworking desk (monthly)| - Hive Coworking',           'EXPENSE',   199.65, 'EUR', NULL,              CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -9, CURRENT_DATE), 'Transfer to Emergency fund',                  'activity.transfer', 'Emergency fund',                                            'DEPOSIT',   500.00, 'EUR', 'Emergency fund',  CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -6, CURRENT_DATE), 'Invoice: Mobile app UI audit - Kestrel Labs SL', 'activity.invoice', 'Mobile app UI audit| - Kestrel Labs SL',               'INCOME',   1961.00, 'EUR', NULL,              CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -4, CURRENT_DATE), 'Bought 10 WRLD at 89,10 €',                   'activity.bought',   '10|WRLD|89,10 €',                                           'BUY',       891.00, 'EUR', 'Atlas Brokerage', CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -1, CURRENT_DATE), 'Dividend: WRLD',                              'activity.dividend', 'WRLD',                                                      'DIVIDEND',   38.40, 'EUR', 'Atlas Brokerage', CURRENT_TIMESTAMP);
