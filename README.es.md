# WealthOS

[English](README.md) | Español

Un panel que instalas en tu propio equipo para controlar tu patrimonio, tus inversiones y tus impuestos como autónomo.

<!-- Añadir captura aquí -->

![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?logo=springboot&logoColor=white)
![Angular](https://img.shields.io/badge/Angular-19-DD0031?logo=angular&logoColor=white)
![H2](https://img.shields.io/badge/H2-embebida-1F4E79)
![Chart.js](https://img.shields.io/badge/Chart.js-4-FF6384?logo=chartdotjs&logoColor=white)
![Licencia: MIT](https://img.shields.io/badge/Licencia-MIT-blue.svg)

## Qué hace

Das de alta tus cuentas (banco, bróker, monedero cripto, efectivo) y lo que tienes en cada una, y WealthOS calcula tu patrimonio, las plusvalías, la rentabilidad y cómo está repartido todo. El efectivo y lo invertido van por separado y solo se suman por cuenta. Cada compra, venta y traspaso se guarda como una operación, y el gráfico del patrimonio se reconstruye a partir de ellas, así que algo que compraste hace dos años aparece hace dos años.

Si eres autónomo, también puedes apuntar las facturas que emites y los gastos que pagas. Con eso estima lo que te tocará pagar cada trimestre de IVA (modelo 303) y de IRPF (modelo 130), lleva la cuenta de la cuota de autónomos y te enseña los próximos plazos.

Todo se guarda en un fichero de base de datos H2 junto a la aplicación. No se envía nada a ningún sitio.

La interfaz está en inglés y en español de España. Se cambia desde la barra lateral.

## Para quién es

Lo hice para mí: quería tener en un solo sitio mis inversiones y mis impuestos trimestrales sin darle las claves del banco a otra app más. Le puede servir a:

- quien quiera llevar su cartera a mano y tener los datos en casa
- autónomos que quieran hacerse una idea del 303 y del 130 antes de que el gestor les pase las cifras

## Arranque rápido

Solo necesitas Java 17 o superior. Maven y Node los descarga la propia compilación la primera vez (en `~/.m2` y en `frontend/node`), así que no hay que instalar nada más.

```bash
git clone https://github.com/Feittoooo/WealthOS.git
cd wealthos
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run
```

Abre http://localhost:8080. La primera vez tarda unos minutos (se descargan Maven, Node y los paquetes de npm); después arranca en segundos. Con la base de datos vacía puedes crear tu primera cuenta o usar **Datos → Cargar datos de ejemplo** para curiosear con una cartera inventada.

Los datos se guardan en `./data/wealthos.mv.db`. Esa carpeta está en el `.gitignore` y se mantiene entre reinicios. **Datos → Exportar inversiones** guarda tus cuentas, activos y todas sus operaciones en un JSON que puedes volver a cargar con **Datos → Importar inversiones**, en este equipo o en otro.

Para generar un jar:

```bash
./mvnw clean package
java -jar target/wealthos-0.0.1-SNAPSHOT.jar
```

### Para tocar el código

El backend y el frontend se pueden arrancar por separado para tener recarga en caliente en Angular:

```bash
./mvnw spring-boot:run -Dskip.npm -Dskip.installnodenpm   # API en :8080, sin compilar Angular
cd frontend && npm install && npm start                   # interfaz en :4200, redirige /api a :8080
```

Tests: `./mvnw test` pasa los tests de Java y los del frontend (Vitest). `npm test` dentro de `frontend/` pasa solo los del frontend.

### Configuración

| Propiedad / variable de entorno | Por defecto | Para qué sirve |
|---|---|---|
| `PORT` | `8080` | Puerto HTTP |
| `WEALTHOS_PASSWORD` | vacía | Activa la autenticación HTTP Basic. Ponla en cualquier despliegue público. |
| `WEALTHOS_USERNAME` | `admin` | Usuario de la autenticación Basic |
| `WEALTHOS_DEBUG` | `true` | La página de depuración y `/api/debug`. Desactívala en despliegues públicos. |
| `WEALTHOS_PRICES` | `none` | Fuente de precios de mercado. De momento solo existe `none` (ver más abajo). |
| `wealthos.locale` | `es-ES` | Formato de números e importes (`en-IE`, `en-US`...) |

El idioma sigue al navegador la primera vez (español si lo pide, inglés en cualquier otro caso) y luego lo que elijas en la barra lateral.

## Cómo está hecho

```
src/main/java/com/wealthos/
  portfolio/    cuentas, activos y sus operaciones, ventas, traspasos, histórico del patrimonio
  tax/          apuntes fiscales, cálculo del 303/130, calendario fiscal
  activity/     el historial de movimientos que comparten los dos módulos
  dashboard/    monta el panel con lo que dan los otros módulos
  data/         exportar, importar, datos de ejemplo, borrar todo
  i18n/         idioma según Accept-Language, sirve los textos al frontend
  system/       información de la app y los endpoints de depuración (logs en memoria, traza de peticiones)
  common/       respuestas de error, formato de números
frontend/src/app/
  core/         interceptores HTTP, traducciones, formato, log de depuración, estado de la app
  shared/ui/    diálogo, gráfico, candado, selector segmentado...
  layout/       barra lateral, avisos, diálogo de confirmación
  features/     una carpeta por página: dashboard, investments, tax, data, debug
src/main/resources/messages.properties, messages_es.properties   todos los textos, en los dos idiomas
```

El backend es una API REST normal (Spring Boot, JPA, H2). Maven compila la app de Angular y la mete en el jar, así que sigue siendo un único fichero para arrancar. Los controladores solo traducen HTTP a llamadas a servicios; todas las reglas están en los servicios, y el cálculo fiscal en `TaxCalculator`, que no toca la base de datos.

Todos los textos están en `messages.properties` y `messages_es.properties`. El backend los usa para errores y validaciones, y el frontend los descarga de `/api/i18n/{lang}`, así que no hay texto escrito a mano en plantillas ni en TypeScript. Un test falla si una clave existe en un idioma y no en el otro.

### Precios de mercado

Los activos tienen un candado. Con el candado puesto, el precio actual debe venir de una fuente de precios y actualizarse cada tarde; sin él, lo escribes tú. Los precios y la fecha son opcionales al comprar o vender: un precio vacío se busca para esa fecha y, si solo se conoce uno de los dos precios, el otro toma su valor (coste = valor de mercado).

Mientras no haya fuente de precios, el gráfico de patrimonio solo conoce los precios de tus operaciones y el de hoy, y traza una línea recta entre ellos. Lo indica debajo del gráfico.

Todavía no hay una fuente de precios real. Todo pasa por la interfaz `PriceProvider`, y la implementación por defecto, `NoPriceProvider`, nunca responde, así que de momento hay que escribir al menos un precio. La issue #1 añade una implementación con Yahoo Finance; el candado, las reglas de los campos vacíos, las ventas y traspasos en fechas pasadas y la actualización diaria la usarán sin más cambios. `WithPriceSourceTest` ya comprueba todo eso con un proveedor de pega.

### Depuración

La página **Depuración** de la barra lateral muestra:

- cada llamada a la API que ha hecho el navegador, con estado, tiempo y los cuerpos de petición y respuesta
- las mismas llamadas vistas desde el servidor
- el log del servidor en directo
- los niveles de log, que se pueden cambiar en caliente (por ejemplo `org.hibernate.SQL` en DEBUG para ver cada consulta)
- información del sistema

Cada llamada lleva un id de petición (`X-Request-Id`) que aparece en todos esos sitios y en los mensajes de error, así que puedes seguir un clic desde el navegador hasta el SQL que ejecutó. La documentación de la API está en `/swagger-ui.html` y la consola de H2 en `/h2-console` (JDBC URL `jdbc:h2:file:./data/wealthos`, usuario `sa`, sin contraseña, solo desde localhost).

## Despliegue en Railway

[![Deploy on Railway](https://railway.com/button.svg)](https://railway.com/new/template?template=https://github.com/Feittoooo/WealthOS)

1. Haz un fork del repositorio y crea un proyecto en Railway a partir de él (New Project, Deploy from GitHub repo).
2. Railway lee `railway.json` y el `Procfile`, compila con el wrapper de Maven y arranca el jar en `$PORT`.
3. Añade un volumen montado en `/app/data`. Sin él, la base de datos se borra en cada despliegue.
4. Define `WEALTHOS_PASSWORD` y `WEALTHOS_DEBUG=false`. No hay otro login.
5. Genera un dominio y entra.

El botón solo funciona cuando hay una plantilla de Railway publicada. Mientras tanto, sigue los pasos de arriba y cambia el enlace del botón por el de tu plantilla.

## Funcionalidades

Lo que trae la v0.1:

- Panel con el patrimonio, la variación respecto al mes anterior, el reparto y la actividad reciente
- Gráfico del patrimonio de todo el periodo, 5, 3 o 1 años, este año o entre dos fechas, con lo aportado dibujado debajo
- Los plazos fiscales solo salen en el panel si los activas desde la página de impuestos (por defecto, ocultos)
- Cuentas de cuatro tipos, cada una con su divisa y su saldo en efectivo; los activos nunca meten ni sacan dinero de ese efectivo por su cuenta
- Activos en acciones, ETF, fondos, cripto u otros. Si compras algo que la cuenta ya tiene (mismo ISIN o mismo ticker), se suma a lo que hay en vez de crear otra fila.
- Cada activo se abre y muestra sus operaciones: comprar más, vender, traspasar, editar o borrar el activo entero, o corregir o borrar una compra o venta concreta. Tras cada cambio todo se recalcula a partir de las operaciones, y se rechaza cualquier cambio que te deje vendiendo más de lo que tenías.
- Compras por participaciones o por importe, con precios y fecha opcionales
- El candado de precio, que se mantiene al editar el activo
- Todas las columnas de las tablas se pueden ordenar; la de activos también se puede filtrar y muestra la fecha de la última operación
- Ventas por participaciones, por importe o de todo, con precio y fecha opcionales y plusvalías a coste medio. Sumar lo obtenido al efectivo es una casilla, desmarcada por defecto.
- Traspasos entre fondos, a uno que ya tienes o a uno nuevo. El coste de compra pasa con las participaciones y no se realiza nada.
- Exportación e importación de los datos de inversión en JSON
- Apuntes fiscales con IVA al 21, 10, 4 o 0 % y retención de IRPF del 15, 7 o 0 %
- Estimación trimestral del modelo 303 y acumulada del 130, con aviso cuando puede que no tengas que presentar el 130
- Control de la cuota de autónomos; los meses pagados se restan en el 130
- Calendario fiscal con las fechas del 303 y el 130 (20 de abril, 20 de julio, 20 de octubre y 30 de enero), el 390 y la Renta
- Tema claro y oscuro, inglés y español
- Página de depuración, traza de peticiones, documentación de la API
- Tests de Java y del frontend

Lo siguiente: la fuente de precios de Yahoo, importar CSV de brókers, conversión de divisas e informes en PDF. El resto está en [ROADMAP.es.md](ROADMAP.es.md), y en [ISSUES.es.md](ISSUES.es.md) está la primera tanda de issues.

## Contribuir

Las pull requests son bienvenidas, sobre todo de gente que sepa de fiscalidad española más que yo.

- Para cualquier cosa más grande que un arreglo pequeño, abre antes una issue o coge una de ISSUES.es.md.
- Que cada PR haga una sola cosa.
- Pasa `./mvnw test` antes de subir nada y añade tests para cualquier cálculo nuevo.
- Respeta las capas: los controladores, finos; las reglas, en los servicios; los componentes no llaman a `HttpClient` directamente (para eso está el `*.api.ts` de cada funcionalidad).
- Cualquier texto nuevo va en `messages.properties` y en `messages_es.properties` (español de España), nunca escrito a mano.
- No subas nunca datos financieros reales. Los datos de prueba y de ejemplo tienen que ser inventados.

## Licencia

[MIT](LICENSE)

## Aviso

El módulo de impuestos solo da estimaciones. Revisa siempre las cifras con tu gestor o asesor fiscal antes de presentar nada.

Esto no es asesoramiento fiscal, legal ni financiero. Los cálculos dan por hecho estimación directa, régimen general de IVA y una sola actividad. No contemplan la prorrata, el recargo de equivalencia, las operaciones intracomunitarias, los gastos de difícil justificación, los festivos autonómicos ni muchos otros casos. Los plazos pueden cambiar de un año a otro. No me hago responsable de las decisiones que se tomen con este software.

Sobre la privacidad: tus datos se quedan en un fichero H2 local en el equipo donde corre WealthOS. Las fuentes y los gráficos van incluidos en la aplicación, así que no hace ninguna petición al exterior.
