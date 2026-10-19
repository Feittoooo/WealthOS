# Primera tanda de issues

[English](ISSUES.md) | Español

Doce issues para abrir en GitHub. El esfuerzo es una estimación a ojo: S es un día o menos, M de dos a cuatro días, L una o dos semanas, XL más que eso.

Con la CLI de GitHub, cada una es `gh issue create --title "..." --label "..." --body "..."`. Los títulos se dejan en inglés para que el tracker sea uno solo.

---

## 1. [Enhancement] Automatic stock price fetching via Yahoo Finance API

Obtener precios automáticamente desde la API de Yahoo Finance.

Etiquetas: `enhancement`, `investments`, `phase-2`, `external-api`
Esfuerzo: M

Todo lo de alrededor ya está hecho: la interfaz `PriceProvider` (`portfolio/price`), el candado de cada activo, las reglas para precios vacíos, las ventas y traspasos en fechas pasadas, el `PriceRefreshJob` diario y un botón "Actualizar precios ahora" en la página de depuración. `WithPriceSourceTest` lo comprueba todo con un proveedor de pega.

Falta un `YahooPriceProvider` que implemente `latest` y `on(fecha)`, anotado con `@ConditionalOnProperty(name = "wealthos.prices.provider", havingValue = "yahoo")`. Notas:

- Desactivado por defecto. El README tiene que decir que activarlo envía los tickers a un servicio externo.
- Los activos ya tienen `priceSymbol` para cuando el ticker no coincide con el de Yahoo (p. ej. `IWDA.AS`), y el diálogo de edición del activo tiene un campo para ello. Si está vacío se usa el ISIN y, si no, el ticker.
- Si falla, devolver vacío y dejar que actúen los respaldos que ya existen; registrar un aviso en el log.
- Tests con un cliente HTTP simulado, sin llamadas reales.

## 2. [Enhancement] CSV transaction import from DEGIRO / Interactive Brokers

Importar movimientos desde CSV de DEGIRO e Interactive Brokers.

Etiquetas: `enhancement`, `investments`, `import`, `phase-2`
Esfuerzo: L

Leer el export de "Transacciones" de DEGIRO y un CSV de Flex Query de IBKR, y convertirlos en posiciones y actividad. Antes de guardar nada, una vista previa con las filas, las posiciones detectadas y los posibles duplicados. Importar dos veces el mismo fichero no puede duplicar nada (hash por fila). Los ficheros de prueba, con datos inventados.

## 3. [Enhancement] PDF export of quarterly tax summary

Exportar a PDF el resumen fiscal del trimestre.

Etiquetas: `enhancement`, `tax`, `export`, `phase-2`
Esfuerzo: M

Un botón "Exportar PDF" en la página de impuestos que genere una página para el trimestre elegido: el desglose del 303 y del 130 y la lista de apuntes. Algo que puedas mandarle al gestor tal cual. Se genera en el servidor a partir de una plantilla Thymeleaf (OpenPDF o Flying Saucer) e incluye el aviso de "solo estimaciones" y la fecha.

## 4. [Enhancement] Currency conversion support

Conversión de divisas.

Etiquetas: `enhancement`, `investments`, `phase-2`
Esfuerzo: M

Las cuentas pueden estar en USD, GBP o CHF, pero ahora los totales lo suman todo tal cual, y eso no está bien. Hace falta:

- una divisa base configurable
- una tabla `ExchangeRate` con tipos diarios (primero a mano, y como opción descarga del BCE)
- los totales y el gráfico de patrimonio convertidos a la divisa base, aunque cada activo siga mostrando su divisa

Cuando esté, hay que quitar la nota del pie que dice que los importes se suman sin convertir.

## 5. [Enhancement] Full modelo 130 calculation engine

Cálculo completo del modelo 130.

Etiquetas: `enhancement`, `tax`, `phase-3`, `help-wanted`
Esfuerzo: L

`TaxCalculator` debería seguir el modelo real casilla a casilla (de la 01 a la 19). Eso incluye los gastos de difícil justificación con porcentaje y tope configurables, la deducción por préstamo de vivienda (casilla 16), los rendimientos negativos de trimestres anteriores (casilla 15) y la exención por el 70 % de retención comprobada contra el año anterior y no contra el actual. Lo ideal es contrastar los tests con cifras reales de un gestor, anonimizadas.

## 6. [Enhancement] Full modelo 303 IVA calculation

Cálculo completo del modelo 303.

Etiquetas: `enhancement`, `tax`, `phase-3`, `help-wanted`
Esfuerzo: XL

La gorda. Bases separadas por tipo y para operaciones exentas, adquisiciones y servicios intracomunitarios (inversión del sujeto pasivo), exportaciones e importaciones, prorrata general y especial, compensar resultados negativos en el trimestre siguiente y pedir la devolución en el 4T. El modelo de datos va a cambiar y los apuntes que ya existen tienen que sobrevivir a la migración.

## 7. [Enhancement] Multi-year tax history view

Histórico fiscal de varios años.

Etiquetas: `enhancement`, `tax`, `phase-3`
Esfuerzo: M

Una página `/tax/history` con una fila por año: ingresos, gastos, rendimiento neto, total del 303, total del 130 y tipo efectivo. Un gráfico pequeño para comparar años, y cada fila enlaza al resumen de ese año.

## 8. [Enhancement] Portfolio benchmark comparison vs IBEX/S&P500

Comparar la cartera con el IBEX 35 y el S&P 500.

Etiquetas: `enhancement`, `investments`, `phase-4`
Esfuerzo: L

Calcular la rentabilidad ponderada por tiempo a partir de las operaciones y los flujos de dinero (la serie del patrimonio ya reconstruye el valor de cualquier día), y dibujarla junto a la serie de un índice (al principio, un CSV a mano) normalizada a 100. Con los mismos botones de periodo que el gráfico del patrimonio.

## 9. [Enhancement] Email/Telegram alerts for fiscal deadlines

Avisos de plazos fiscales por correo o Telegram.

Etiquetas: `enhancement`, `notifications`, `phase-4`
Esfuerzo: M

Una interfaz `Notifier` con implementaciones para SMTP y para un bot de Telegram, configuradas con variables de entorno. Recordatorios N días antes de cada plazo (7 y 1 por defecto) con el importe estimado. Desactivado por defecto, y el mensaje solo debe incluir lo que el usuario haya aceptado compartir.

## 10. [Bug] Validate edge cases in gain/loss calculation when position is partially sold

Revisar casos límite de las plusvalías en ventas parciales.

Etiquetas: `bug`, `investments`, `calculations`, `good-first-issue`
Esfuerzo: S

`TradeService.sell` usa el coste medio. Las ventas parciales, vender todo, vender por importe y vender más de lo que tienes ya están cubiertas en `TradeServiceTest`. Queda pendiente:

- vender una parte de un activo cripto con 8 decimales: redondeo de lo que queda y de la plusvalía
- vender justo al precio medio tiene que dar una plusvalía de 0,00 exactos
- hoy no se puede registrar una compra a precio 0 (airdrops, regalos) porque los precios tienen que ser mayores que cero; decidir si se permite
- editar una compra antigua después de una venta parcial cambia la plusvalía de esa venta, porque se recalcula todo; decidir si es correcto o si la venta debe conservar su plusvalía
- las plusvalías realizadas solo quedan en la actividad; una tabla `RealizedGain` ayudaría para la Renta

Está terminada cuando cada caso tiene su test en `TradeServiceTest`, el comportamiento esperado está escrito y no se rompe nada más.

## 11. [Enhancement] Open Banking PSD2 integration for automatic bank sync

Sincronización con el banco mediante Open Banking (PSD2).

Etiquetas: `enhancement`, `integrations`, `phase-5`, `security`
Esfuerzo: XL

Sincronizar saldos (y, si se quiere, movimientos) a través de un agregador, empezando por GoCardless Bank Account Data. Hace falta el flujo de consentimiento OAuth, los tokens cifrados en disco con una clave del entorno, un modelo de amenazas por escrito y actualizar la parte de privacidad del README. Solo si el usuario lo activa.

## 12. [Enhancement] AI assistant for tax guidance

Asistente de IA para dudas fiscales.

Etiquetas: `enhancement`, `ai`, `tax`, `phase-4`
Esfuerzo: L

Un panel de chat en la página de impuestos. Por defecto habla con un modelo local a través de Ollama para que no salga nada del equipo. Un modelo en la nube solo con una clave de API que añada el usuario y un interruptor de consentimiento explícito. Solo debe ver los totales del trimestre elegido salvo que el usuario permita más, tratar el texto de los apuntes como datos (no como instrucciones), y cada respuesta tiene que llevar el aviso de "solo estimaciones, consúltalo con tu gestor" con enlaces a las páginas de la AEAT que correspondan.
