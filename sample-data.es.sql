-- Datos de ejemplo de WealthOS (español). La versión en inglés es sample-data.sql.
--
-- Todo es inventado: bancos, brókers, tickers, clientes, precios e importes.
-- Si algo se parece a una empresa real, es casualidad.
--
-- Lo más fácil es abrir el panel con la base de datos vacía y pulsar
-- "Cargar datos de ejemplo" con la aplicación en español. Así además se marcan
-- como pagadas las cuotas de este año.
-- También se puede lanzar a mano desde /h2-console (JDBC URL jdbc:h2:file:./data/wealthos,
-- usuario sa, sin contraseña), pero solo con la base de datos vacía.
--
-- Las fechas van en función de CURRENT_DATE para que los gráficos siempre parezcan recientes.
-- No hay ids fijos: las claves ajenas se buscan por nombre.

INSERT INTO accounts (name, type, institution, currency, balance, created_at) VALUES
  ('Cuenta corriente',      'BANK',          'Banco Norte',   'EUR',  8450.20, DATEADD('MONTH', -14, CURRENT_TIMESTAMP)),
  ('Colchón de emergencia', 'BANK',          'Banco Norte',   'EUR', 15000.00, DATEADD('MONTH', -14, CURRENT_TIMESTAMP)),
  ('Atlas Bróker',          'BROKERAGE',     'Atlas Markets', 'EUR',  1230.55, DATEADD('MONTH', -13, CURRENT_TIMESTAMP)),
  ('Lumen Fondos',          'BROKERAGE',     'Lumen Invest',  'EUR',   420.00, DATEADD('MONTH', -12, CURRENT_TIMESTAMP)),
  ('Monedero frío',         'CRYPTO_WALLET', 'Autocustodia',  'EUR',     0.00, DATEADD('MONTH', -12, CURRENT_TIMESTAMP)),
  ('Efectivo en cartera',   'CASH',          NULL,            'EUR',   300.00, DATEADD('MONTH', -12, CURRENT_TIMESTAMP));

-- activos: uno por cada cosa que tienes en una cuenta; la cantidad y el coste salen de las operaciones de abajo
INSERT INTO assets (account_id, ticker, isin, name, asset_type, current_price, price_updated_at, auto_price, price_symbol, created_at) VALUES
  ((SELECT id FROM accounts WHERE name = 'Atlas Bróker'), 'ACME',    NULL,           'Acme Robotics Corp.',          'STOCK',    104.30, DATEADD('DAY', -1, CURRENT_TIMESTAMP),  FALSE, NULL, DATEADD('MONTH', -11, CURRENT_TIMESTAMP)),
  ((SELECT id FROM accounts WHERE name = 'Atlas Bróker'), 'GLBX',    NULL,           'Globex Systems Inc.',          'STOCK',    128.75, DATEADD('DAY', -1, CURRENT_TIMESTAMP),  FALSE, NULL, DATEADD('MONTH', -10, CURRENT_TIMESTAMP)),
  ((SELECT id FROM accounts WHERE name = 'Atlas Bróker'), 'INIT',    NULL,           'Initech Software',             'STOCK',    245.60, DATEADD('DAY', -1, CURRENT_TIMESTAMP),  FALSE, NULL, DATEADD('MONTH', -9,  CURRENT_TIMESTAMP)),
  ((SELECT id FROM accounts WHERE name = 'Atlas Bróker'), 'SOLR',    NULL,           'Solaris Renovables',           'STOCK',     19.85, DATEADD('DAY', -12, CURRENT_TIMESTAMP), FALSE, NULL, DATEADD('MONTH', -8,  CURRENT_TIMESTAMP)),
  ((SELECT id FROM accounts WHERE name = 'Atlas Bróker'), 'WRLD',    NULL,           'Atlas World Equity UCITS ETF', 'ETF',       91.45, DATEADD('DAY', -1, CURRENT_TIMESTAMP),  FALSE, NULL, DATEADD('MONTH', -13, CURRENT_TIMESTAMP)),
  ((SELECT id FROM accounts WHERE name = 'Atlas Bróker'), 'AUR',     NULL,           'Aurum Oro Físico ETC',      'OTHER',    201.00, DATEADD('DAY', -4, CURRENT_TIMESTAMP),  FALSE, NULL, DATEADD('MONTH', -7,  CURRENT_TIMESTAMP)),
  ((SELECT id FROM accounts WHERE name = 'Lumen Fondos'),     'LMN-GLB', 'ZZ00LUMEN012', 'Lumen Fondo Indexado Global',      'FUND',     168.92, DATEADD('DAY', -3, CURRENT_TIMESTAMP),  FALSE, NULL, DATEADD('MONTH', -12, CURRENT_TIMESTAMP)),
  ((SELECT id FROM accounts WHERE name = 'Lumen Fondos'),     'LMN-BND', 'ZZ00LUMEN029', 'Lumen Fondo Renta Fija Euro',         'FUND',      99.80, DATEADD('DAY', -3, CURRENT_TIMESTAMP),  FALSE, NULL, DATEADD('MONTH', -12, CURRENT_TIMESTAMP)),
  ((SELECT id FROM accounts WHERE name = 'Lumen Fondos'),     'HZEM',    'ZZ00HRZEM034', 'Horizon Mercados Emergentes ETF', 'ETF',       27.15, DATEADD('DAY', -3, CURRENT_TIMESTAMP),  FALSE, NULL, DATEADD('MONTH', -6,  CURRENT_TIMESTAMP)),
  ((SELECT id FROM accounts WHERE name = 'Monedero frío'),     'ORBC',    NULL,           'OrbCoin',                      'CRYPTO', 52400.00, DATEADD('HOUR', -5, CURRENT_TIMESTAMP),  FALSE, NULL, DATEADD('MONTH', -11, CURRENT_TIMESTAMP)),
  ((SELECT id FROM accounts WHERE name = 'Monedero frío'),     'NOVA',    NULL,           'NovaChain',                    'CRYPTO',  2310.00, DATEADD('HOUR', -5, CURRENT_TIMESTAMP),  FALSE, NULL, DATEADD('MONTH', -5,  CURRENT_TIMESTAMP));

-- cada compra y venta, para que el gráfico del patrimonio se pueda reconstruir mes a mes
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

-- base_amount es la base imponible (sin IVA ni retención)
INSERT INTO tax_entries (entry_date, concept, counterparty, category, type, base_amount, iva_rate, irpf_retention_rate, deductible, created_at) VALUES
  (DATEADD('DAY',   -6, CURRENT_DATE), 'Auditoría de UI de app móvil',                 'Kestrel Labs SL',    'Servicios', 'INGRESO', 1850.00, 21, 15, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -15, CURRENT_DATE), 'Mantenimiento del sistema de diseño (mensual)', 'Bluebird Studio SL', 'Servicios', 'INGRESO', 1200.00, 21, 15, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -28, CURRENT_DATE), 'Rediseño de landing page',                     'Pinecone Media SL',  'Servicios', 'INGRESO', 2400.00, 21, 15, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -45, CURRENT_DATE), 'Mantenimiento del sistema de diseño (mensual)', 'Bluebird Studio SL', 'Servicios', 'INGRESO', 1200.00, 21, 15, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -70, CURRENT_DATE), 'Taller de estrategia de marca (2 días)',       'Orbital Apps SL',    'Servicios', 'INGRESO', 1600.00, 21, 15, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -78, CURRENT_DATE), 'Mantenimiento del sistema de diseño (mensual)', 'Bluebird Studio SL', 'Servicios', 'INGRESO', 1200.00, 21, 15, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY', -105, CURRENT_DATE), 'Investigación UX para panel de control',       'Kestrel Labs SL',    'Servicios', 'INGRESO', 2750.00, 21, 15, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY', -120, CURRENT_DATE), 'Mantenimiento del sistema de diseño (mensual)', 'Bluebird Studio SL', 'Servicios', 'INGRESO', 1200.00, 21, 15, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY', -150, CURRENT_DATE), 'Rediseño del proceso de compra de la tienda online', 'Pinecone Media SL', 'Servicios', 'INGRESO', 3100.00, 21, 15, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY', -172, CURRENT_DATE), 'Mantenimiento del sistema de diseño (mensual)', 'Bluebird Studio SL', 'Servicios', 'INGRESO', 1200.00, 21, 15, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY', -200, CURRENT_DATE), 'Entradas a taller online',                     NULL,                 'Productos', 'INGRESO',  450.00, 21,  0, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY', -215, CURRENT_DATE), 'Mantenimiento del sistema de diseño (mensual)', 'Bluebird Studio SL', 'Servicios', 'INGRESO', 1200.00, 21, 15, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY', -240, CURRENT_DATE), 'Auditoría de accesibilidad web',               'Orbital Apps SL',    'Servicios', 'INGRESO',  980.00, 21, 15, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY', -268, CURRENT_DATE), 'Mantenimiento del sistema de diseño (mensual)', 'Bluebird Studio SL', 'Servicios', 'INGRESO', 1200.00, 21, 15, FALSE, CURRENT_TIMESTAMP),

  (DATEADD('DAY',   -3, CURRENT_DATE), 'Herramienta de diseño, licencia anual', 'Vectorly Software',    'Software y suscripciones', 'GASTO',  144.00, 21, 0, TRUE,  CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -12, CURRENT_DATE), 'Puesto en coworking (mensual)',         'Hive Coworking',       'Oficina y coworking',      'GASTO',  165.00, 21, 0, TRUE,  CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -35, CURRENT_DATE), 'Puesto en coworking (mensual)',         'Hive Coworking',       'Oficina y coworking',      'GASTO',  165.00, 21, 0, TRUE,  CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -40, CURRENT_DATE), 'Portátil para diseño',                  'TechNova Store',       'Equipos',                  'GASTO', 1449.00, 21, 0, TRUE,  CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -60, CURRENT_DATE), 'Móvil y fibra (línea de trabajo)',      'Telco Uno',            'Teléfono e internet',      'GASTO',   48.50, 21, 0, TRUE,  CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -90, CURRENT_DATE), 'Entrada a congreso de UX',              'Design Summit Events', 'Formación',                'GASTO',  220.00, 21, 0, TRUE,  CURRENT_TIMESTAMP),
  (DATEADD('DAY', -130, CURRENT_DATE), 'Gestoría, presentaciones trimestrales', 'Gestoría Ejemplo SL',  'Servicios profesionales',  'GASTO',  120.00, 21, 0, TRUE,  CURRENT_TIMESTAMP),
  (DATEADD('DAY', -160, CURRENT_DATE), 'Suscripción a banco de imágenes',       'Pixabundle',           'Software y suscripciones', 'GASTO',   29.00, 21, 0, TRUE,  CURRENT_TIMESTAMP),
  (DATEADD('DAY', -190, CURRENT_DATE), 'Cena familiar (no deducible)',          'La Taberna Inventada', 'Otros',                    'GASTO',   85.00, 10, 0, FALSE, CURRENT_TIMESTAMP),
  (DATEADD('DAY', -230, CURRENT_DATE), 'Silla de oficina ergonómica',           'Muebles Sitwell',      'Equipos',                  'GASTO',  310.00, 21, 0, TRUE,  CURRENT_TIMESTAMP),
  (DATEADD('DAY', -255, CURRENT_DATE), 'Dominio y hosting (anual)',             'Cloudnest Hosting',    'Software y suscripciones', 'GASTO',   96.00, 21, 0, TRUE,  CURRENT_TIMESTAMP);

-- actividad. message_code + message_args permiten mostrarla en cualquiera de los dos idiomas.
INSERT INTO transactions (tx_date, description, message_code, message_args, type, amount, currency, account_name, created_at) VALUES
  (DATEADD('DAY', -31, CURRENT_DATE), 'Venta de 10 SOLR a 21,40 € (plusvalía -7,00 €)', 'activity.sold',     '10|SOLR|21,40 €|-7,00 €',                                                 'SELL',      214.00, 'EUR', 'Atlas Bróker',          CURRENT_TIMESTAMP),
  (DATEADD('DAY', -20, CURRENT_DATE), 'Compra de 0,02 ORBC a 46.000,00 €',              'activity.bought',   '0,02|ORBC|46.000,00 €',                                                   'BUY',       920.00, 'EUR', 'Monedero frío',         CURRENT_TIMESTAMP),
  (DATEADD('DAY', -15, CURRENT_DATE), 'Factura: Mantenimiento del sistema de diseño (mensual) - Bluebird Studio SL', 'activity.invoice', 'Mantenimiento del sistema de diseño (mensual)| - Bluebird Studio SL', 'INCOME', 1272.00, 'EUR', NULL, CURRENT_TIMESTAMP),
  (DATEADD('DAY', -12, CURRENT_DATE), 'Gasto: Puesto en coworking (mensual) - Hive Coworking', 'activity.expense', 'Puesto en coworking (mensual)| - Hive Coworking',                'EXPENSE',   199.65, 'EUR', NULL,                    CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -9, CURRENT_DATE), 'Traspaso a Colchón de emergencia',               'activity.transfer', 'Colchón de emergencia',                                                   'DEPOSIT',   500.00, 'EUR', 'Colchón de emergencia', CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -6, CURRENT_DATE), 'Factura: Auditoría de UI de app móvil - Kestrel Labs SL', 'activity.invoice', 'Auditoría de UI de app móvil| - Kestrel Labs SL',              'INCOME',   1961.00, 'EUR', NULL,                    CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -4, CURRENT_DATE), 'Compra de 10 WRLD a 89,10 €',                    'activity.bought',   '10|WRLD|89,10 €',                                                         'BUY',       891.00, 'EUR', 'Atlas Bróker',          CURRENT_TIMESTAMP),
  (DATEADD('DAY',  -1, CURRENT_DATE), 'Dividendo: WRLD',                                'activity.dividend', 'WRLD',                                                                    'DIVIDEND',   38.40, 'EUR', 'Atlas Bróker',          CURRENT_TIMESTAMP);
