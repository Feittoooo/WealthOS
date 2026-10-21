# Hoja de ruta

[English](ROADMAP.md) | Español

Más o menos en el orden en que quiero hacer las cosas. Sin fechas, que es un proyecto personal. Muchas de estas tareas tienen su [issue](https://github.com/Feittoooo/WealthOS/issues) con más detalle y una estimación de esfuerzo.

## Fase 1: Base (MVP) - hecha

- Seguimiento manual de la cartera: cuentas, activos que salen de sus operaciones, plusvalías a coste medio, pesos y ventas parciales
- Estimaciones fiscales básicas: 303 trimestral, 130 acumulado, control de la cuota y calendario fiscal
- Tema claro y oscuro
- Interfaz en inglés y en español
- Histórico del patrimonio reconstruido a partir de las operaciones, con periodos Todo / 5A / 3A / 1A / Este año / personalizado
- Despliegue en Railway con autenticación Basic opcional
- Compras por participaciones o por importe, precios y fecha opcionales, el candado de precio; comprar algo que ya tienes se suma a ello (por ISIN o ticker)
- Cada compra, venta y traspaso se puede editar o borrar por separado
- Ventas por participaciones, importe o todo, en cualquier fecha; traspasos entre fondos que mantienen el coste
- Exportación e importación de los datos de inversión
- API REST + frontend en Angular, página de depuración con traza de peticiones

## Fase 2: Datos y automatización

- Precios automáticos de acciones, ETF, fondos y cripto desde la API de Yahoo Finance, como implementación de `PriceProvider`. El candado, los precios vacíos, las ventas en fechas pasadas y la actualización diaria ya están conectados a esa interfaz. Opcional, porque sería lo primero que hable con el exterior.
- Importación de CSV con los movimientos del bróker. Primero DEGIRO e Interactive Brokers, luego una pantalla genérica para "emparejar columnas".
- Exportar a PDF el resumen trimestral, listo para mandárselo al gestor.
- Conversión de divisas entre EUR, USD y GBP con los tipos diarios del BCE, guardados para que los valores pasados no cambien.
- Plusvalías FIFO junto al coste medio. El IRPF usa FIFO para acciones y fondos; las operaciones necesarias ya se guardan.
- Copia de seguridad y restauración también de los datos fiscales (la exportación de inversiones ya existe).
- Migraciones de base de datos con Flyway en lugar de dejar que Hibernate actualice el esquema.

## Fase 3: Fiscalidad avanzada

- El modelo 130 completo con todas las deducciones: gastos de difícil justificación, la deducción por préstamo de vivienda, rendimientos negativos de trimestres anteriores y la comprobación del 70 % de retención contra el año anterior, como dice la norma.
- El modelo 303 completo con toda la casuística del IVA: prorrata, operaciones intracomunitarias (inversión del sujeto pasivo), exportaciones, recargo de equivalencia, compensar resultados negativos y pedir la devolución en el 4T.
- Modelo 390, generado a partir de los cuatro 303 del año.
- PDF prerrellenados que siguen las casillas oficiales.
- Histórico fiscal de varios años.
- Festivos autonómicos que mueven los plazos, y los regímenes del País Vasco y Navarra.

## Fase 4: Inteligencia

- Un asistente para dudas fiscales. Por defecto, un modelo local con Ollama para que no salga nada de tu equipo; un modelo en la nube solo si tú añades una clave de API.
- Categorías sugeridas para los gastos, con reglas sencillas: un apunte con el mismo proveedor o un concepto parecido recibe la categoría que usaste la última vez. No se entrena ningún modelo.
- Sugerencias para pagar menos impuestos de forma legal: deducciones que se te escapan, cuánto apartar, retención del 7 % o del 15 %.
- Comparativa con índices como el IBEX 35, el S&P 500 o el MSCI World.
- Avisos por correo o Telegram de plazos, movimientos fuertes de precio, precios desactualizados y la cuota.
- Rentabilidad ponderada por dinero (TIR), para que las aportaciones y retiradas no distorsionen las cifras.

## Fase 5: Integraciones

- Open Banking / PSD2 para sincronizar los saldos del banco a través de un agregador, con los tokens cifrados en disco.
- APIs de brókers: DEGIRO (no oficial) e Interactive Brokers.
- Descargar documentos de Hacienda con tu certificado digital: datos fiscales, declaraciones presentadas y notificaciones.
- Sincronización de solo lectura con exchanges y monederos cripto.

## Lo que no va a cambiar

WealthOS es para tus propios registros: tus cuentas, tus inversiones y tu propia contabilidad de autónomo. En los apuntes fiscales, el cliente o proveedor es solo el nombre que figura en tu factura. Las nóminas, los modelos fiscales de otras personas y las vistas que mezclan las finanzas de varias personas quedan fuera.

Tus datos se quedan en tu equipo. Todo lo que se conecte a un servicio externo será opcional y estará documentado. Las cifras fiscales siempre dirán que son estimaciones y qué dan por hecho. Y seguirá siendo un único jar que se arranca teniendo solo Java instalado.
