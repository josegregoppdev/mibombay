# AGENTS.md — Sistema de Gestión para Restaurantes

## Stack
- Spring Boot 3.5.14 + Java 21 + Maven
- MySQL + JPA/Hibernate
- Spring Security con BCrypt
- Spring WebSocket (STOMP + SockJS) para notificaciones en tiempo real
- Apache POI 5.4 (Excel)
- OpenHTMLtoPDF 1.1.37 (PDF generation)
- SpringDoc OpenAPI 2.8.6 (Swagger)
- MapStruct 1.6.3 (Entity ↔ DTO)
- Thymeleaf + Bootstrap 5 (vistas server-side)
- Lombok
- Spring Validation

## Arquitectura
- **MVC**: controllers/, services/, repositories/, models/, DTO/, mapper/, exceptions/, config/, security/
- **Paquete base**: `com.mibombay.sistemaresurante`
- **Multi-tenant** por `empresaId` (campo en cada entidad + `TenantContext` + `TenantFilter` + `TenantInterceptor`)
- **Superadmin**: `esSuperadmin=true`, sin empresa asignada, login separado en `/superadmin/login`
- **Eliminación lógica**: campo `activo` boolean en todas las entidades

## Convenciones de código

### Nombrado
- **Clases**: PascalCase (`EmpresaService`, `UsuarioRepository`)
- **Métodos/variables**: camelCase (`findByEmpresaId`, `guardarUsuario`)
- **Constantes**: UPPER_SNAKE_CASE
- **Tablas**: snake_case plural (`venta_detalle`, `cierre_z`)
- **Columnas**: snake_case (`fecha_creacion`, `precio_venta`)
- **URLs**: kebab-case (`/api/v1/empresas`, `/productos/guardar`)
- **Nombres de métodos descriptivos**: evitar `listar`, `obtenerPorId` genéricos; usar `listarVentasConFiltros`, `obtenerVentaPorId`, etc.
- **Specification**: nombrar parámetros como `tabla` (root), `consulta` (query), `criteria` (CriteriaBuilder), `condiciones` (predicates)

### Entidades JPA
- `@Data @NoArgsConstructor @AllArgsConstructor` de Lombok
- `@Entity`, `@Table(name = "nombre_tabla")`
- `@SQLDelete(sql = "UPDATE ... SET activo = false WHERE id = ?")`
- `@Where(clause = "activo = true")`
- `@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})`
- Usar `Boolean` (wrapper) para `activo`, `Integer` para cantidades, `BigDecimal` para precios
- Relaciones: `FetchType.LAZY` salvo excepciones justificadas

### DTOs
- **Request/Response unificado**: cuando el CRUD es simple, usar clase con `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder` de Lombok + anotaciones `jakarta.validation` (ej: `IngredienteDTO`)
- **Separado**: `record` de Java para request (`@NotBlank`, `@NotNull`) y response (sin anotaciones)
- Mapper con MapStruct (`@Mapper(componentModel = "spring")`)

### Paginación y filtros
- Repositorios que requieran filtros dinámicos: extender `JpaSpecificationExecutor<T>` y construir `Specification<T>` normalmente en el service
- Controller recibe `@RequestParam(defaultValue = "0") int page` y usa `PageRequest.of(page, size)`
- Service devuelve `Page<DTO>`, template itera `page.content`
- Filtros se pasan como query params y se preservan en links de paginación (`@{/ruta(param=${valor}, page=${i})}`)
- Mostrar barra de paginación con `page.number`, `page.totalPages`, `page.totalElements`, `page.first`, `page.last`
- Para vistas combinadas con múltiples tablas (ej: inventario), construir `Specification<T>` inline en el controller y usar `Page<Entity>` directamente con repositorio; usar nombres de page param diferenciados (`pageIng`, `pageProd`) y preservar el page de la otra tabla en los links de paginación
- En expresiones SPEL de Thymeleaf NO usar referencias a clases Java como `BigDecimal.ZERO` (falla con `EL1007E`); usar `compareTo(0)` en su lugar (Java autoboxea `int` → `BigDecimal`)

### Enums de modelo
- Ubicación: `models/enums/` (ej: `UnidadMedida`, `Rol`)
- Valores en UPPER_SNAKE_CASE
- En templates se accede como `${enumValue}` → `toString()` retorna el nombre

### Servicios
- Anotar con `@Transactional(readOnly = true)` a nivel clase
- `@Transactional` solo en métodos de escritura
- Inyectar dependencias por constructor (no `@Autowired`)
- Lanzar `BusinessException` o `ResourceNotFoundException` en lugar de devolver null
- **Receta**: `costo` por detalle = `cantidad × ingrediente.precioCompra`; `costoReceta` = suma de costos de detalles
- **Receta**: `calcularStock(Producto)` = `min(stockIngrediente / cantidad)` sobre todos los detalles
- **Receta**: al guardar, usar `receta.getDetalles().clear()` + `add()` en la colección gestionada, evitar `delete()` manual + `setDetalles()` (rompe `orphanRemoval`)
- **Compra**: al crear, `subtotal` y `tipo` (INGREDIENTES/PRODUCTOS/MIXTO) se calculan automáticamente en el service según los detalles; luego actualiza `stockActual` de cada Ingrediente o Producto
- **Compra (formulario)**: IVA 19% fijo, cálculo solo en frontend (JS). Usar dropdown unificado con `<optgroup>` para Ingredientes/Productos + atributo `data-tipo` para detectar tipo automáticamente. Template row (`<div id="template-row">`) dentro del `<form>` — quitar `name` y `required` de sus inputs, setearlos dinámicamente en JS al clonar para evitar errores de binding con Spring.
- **Compra**: al anular, se revierte el stock (resta lo sumado)
- **CompraDetalle**: usa `TipoItemCompra` enum (INGREDIENTE, PRODUCTO) + `itemId` genérico + `itemNombre` denormalizado
- **Producto**: `margenGanancia` = `((precioVenta - costo) / precioVenta) * 100`, calculado al listar y seteado en DTO (no persistido)
- **Controller**: siempre setear `empresaId` y `usuarioId` en el DTO antes de llamar al service en `actualizar()` (MapStruct sobrescribe)
- **MovimientoInventario**: se crea automáticamente desde `CompraService.crear()`, `.anular()` y `.actualizar()`. Registra `stockAnterior`, `stockPosterior`, `signo` (`+`/`-`), `usuarioId`, `fechaMovimiento`. Consulta paginada con `JpaSpecificationExecutor` y filtros por `movimientoTipo`, `itemTipo`, `itemId`
- **Venta**: entidad con campos `recibidoEfectivo`, `recibidoTransferencia`, `cambio` (BigDecimal) y `paraLlevar` (Boolean). `MetodoPago` = EFECTIVO, TRANSFERENCIA, MIXTO. `VentaDetalle` usa `ventaId` plain column (sin relación JPA) + campo `modificaciones` (TEXT) para exclusiones de ingredientes + campo `adicionales` (TEXT) para comentarios libres (ej: "picado a la mitad"). VentaService.crear() valida stock, descuenta ingredientes (productos con receta) o producto.stockActual, y crea MovimientoInventario tipo VENTA. Anular revierte stock y crea VENTA_ANULACION.
- **Venta (frontend POS)**: panel dinámico `.sd-pos-pago-detail` que muestra inputs según método seleccionado (EFECTIVO → "Recibido" + cambio; TRANSFERENCIA → "Monto transferencia"; MIXTO → ambos + cambio). Botón PAGAR se habilita solo si montos cubren el total. Cambio se calcula en servidor (`VentaService.calcularCambio()`), se persiste en BD. Inputs ocultos `recibidoEfectivo`/`recibidoTransferencia` sincronizados con los visibles. Toggle `.sd-toggle` "Para llevar" en el carrito, se persiste en BD como `Venta.paraLlevar`.
- **Venta adicionales y modificaciones**: Click en producto → se agrega directo al carrito. 🖊️ botón visible para TODOS los productos, abre modal con input de texto "Comentario / Adicionales" (texto libre, ej: "picado a la mitad, para llevar"). Si el producto tiene receta, debajo del input se muestran checkboxes de ingredientes. Al guardar, `adicionales` (texto) se persiste en `VentaDetalle`, `modificaciones` (texto) guarda exclusiones, ingredientes excluidos se pasan como `Set<Long> ingredientesExcluidos` a `descontarStockProducto()` y no se descuentan del stock. `ingredientesExcluidosIds` es transient (no persiste). `recetasJson` se carga en VentaController como raw JSON.
- **VentaSuspendida (órdenes en espera)**: Entidad `VentaSuspendida` (tabla `venta_suspendida`) que persiste carritos en espera por usuario. Permite al cajero pausar una venta (cliente se demora) y atender al siguiente sin perder el carrito. Cada tab del POS es una orden independiente con su propio carrito, cliente, método de pago, montos recibidos y toggle paraLlevar. **Multi-tab en POS**: pills `.sd-pos-tab` en el header del carrito con label auto-numerado ("Orden 1, 2..."), badge de ítems y total abreviado. Sync continuo debounced 800ms vía `POST /api/v1/ventas-suspendidas`. `beforeunload` flusha con `navigator.sendBeacon`. Al cobrar exitosamente, `VentaController.guardar()` elimina la orden suspendida (vía `suspendidaId` hidden input + `@RequestParam`). Si el cobro falla, la orden se conserva. **Por usuario**: cada cajero ve solo sus propias órdenes. **No reserva stock**: el stock se descuenta al cobrar (server valida). Sobrevive a refresh, navegación a otras páginas, reinicio y cambio de equipo. Máximo 8 tabs. `itemsJson` (TEXT) serializa el carrito como JSON. `ventasSuspendidasJson` se carga en VentaController como raw JSON.
- **Cliente**: DNI NOT NULL + `@UniqueConstraint(empresa_id, dni)`. Consumidor Final usa DNI="9999999".
- **Cuadre de Caja**: Vista en `GET /admin/cuadreCaja` (solo ADMIN). Muestra formulario tipo `sd-form-card` con inputs para auditor, cajero, turno, transferencia, otros y observaciones + tabla de denominaciones COP unificada (Billetes 7 + Monedas 5). Cálculos en JS client-side. Puede **guardarse en BD** (`CuadreCaja` entidad) y **generarse PDF** con OpenHTMLtoPDF. Tiene historial (`/admin/cuadreCaja/historial`), detalle read-only (`/admin/cuadreCaja/{id}`) y reimpresión (`/admin/cuadreCaja/{id}/pdf`).
- **Inventario Físico**: Entidades `InventarioFisico` (tabla `inventario_fisico`) + `InventarioFisicoDetalle` (tabla `inventario_fisico_detalle`). Tabla `inventario_fisico_detalle` tiene campos: `merma`, `desperdicio` (BigDecimal, default 0). Flujo de 2 pasos: (1) **Carga** (`GET /inventario-fisico/nuevo` → crea borrador automáticamente con todos los ingredientes + productos sin receta, redirige a `/editar`), el formulario tiene columnas: Stock Sistema, Stock Físico, Merma, Desperdicio. Binding vía `InventarioFisicoFormDTO.ItemStock` (campos: itemId, itemTipo, stockFisico, merma, desperdicio). (2) **Revisión** (`GET /inventario-fisico/{id}/revisar`) muestra tabla con Stock Sistema, Stock Físico, Merma, Desperdicio y **Dif. Inexplicada**; la diferencia inexplicada = (stockSistema − merma − desperdicio) − stockFisico. (3) **Confirmación** (`POST /inventario-fisico/{id}/confirmar`) proceso de 4 pasos por detalle: (a) merma crea `MovimientoInventario` tipo `MERMA` signo "-", reduce stock intermedio; (b) desperdicio crea `MovimientoInventario` tipo `DESPERDICIO` signo "-", reduce stock intermedio; (c) diferencia inexplicada solo si ≠ 0 crea `AJUSTE_INVENTARIO`; (d) stock final = stockFisico. **Una vez al día**: bloquea si ya existe CONFIRMADO hoy; permite retomar BORRADOR pendiente. **Solo ADMIN**. Items: solo ingredientes + productos sin receta. Exportación Excel con Apache POI 5.4 (`GET /inventario-fisico/{id}/excel`). Estados: `InventarioEstado` enum (BORRADOR, CONFIRMADO). `InventarioFisicoService.crearBorrador()` y `actualizarBorrador()` aceptan 3 maps: `stocksFisicos`, `mermas`, `desperdicios` (todos `Map<String, BigDecimal>` con key compuesta `itemTipo:itemId`). `confirmar()` usa `MovimientoTipo.MERMA` y `MovimientoTipo.DESPERDICIO`.

### Controladores
- **Thymeleaf**: `@Controller`, devuelven String de la vista
- **REST**: `@RestController`, mapean a `/api/v1/...`
- Endpoints REST devuelven `ResponseEntity<?>`
- Usar `@Valid` en requests
- **Cuadre de Caja**: endpoint `GET /admin/cuadreCaja` en `CierreZController` (solo ADMIN). No usa service adicional, solo `CierreZService.obtenerReporteX()` para obtener el total de efectivo del sistema.

### Vistas Thymeleaf (admin con sidebar)
- Usar clases `sd-*` para el layout con sidebar (`sd-page-header`, `sd-page-title`, `sd-section-card`, `sd-form-input`, `sd-btn`, `sd-toggle`, etc.)
- Usar clases `sa-*` para componentes tipo tabla (`sa-table-wrapper`, `sa-table`, `sa-badge`, `sa-btn-icon`, etc.)
- Badges de margen: `sa-badge--profit-high` (verde, ≥40%), `sa-badge--profit-mid` (amarillo, 20-40%), `sa-badge--profit-low` (rojo, <20%)
- Los estilos `sa-*` originalmente de superadmin se reutilizan en admin por compartir paleta oscura
- Fragmentos comunes en `templates/fragments/layout.html` (sidebar + navbar)
- Sidebar en `templates/fragments/sidebar.html`
- Para vistas públicas (login, registro, landing): usar clases específicas (`login-*`, `registro-*`, `landing-*`)
- Personalización visual vía CSS variables (`--accent`, `--sidebar-bg`, `--fondo`, `--boton`, `--font-heading`, `--font-body`)
- Botón de receta (`sa-btn-icon--recipe`) en listado de productos: renderizar solo con `th:if="${p.tieneReceta}"`
- Checkbox booleano: usar toggle switch `.sd-toggle` con `<label for="id" class="sd-toggle-slider">` y `th:field` (patrón `productos/form.html`). **NO** usar `.sd-toggle` con `<span class="sd-toggle-slider">` dentro de `<label>` cuando se usa `th:field` (falla el click). Para casos problemáticos, usar checkbox simple.
- **Ingrediente consumible**: checkbox simple (`.sd-toggle` falla con `th:field` en boolean). Al editar un consumible existente, el checkbox se oculta y se muestra un banner informativo + hidden input.
- **Ingrediente consumible (backend)**: una vez creado como `consumible=true`, no se puede desmarcar. `Mapper.updateEntity()` ignora `consumible` (`@Mapping(target = "consumible", ignore = true)`). `IngredienteService.actualizar()` lanza `BusinessException` si intenta cambiarlo.
- Pasar listas Java a JS: usar `<script th:inline="javascript" th:each="item : ${list}">array.push({id: [[${item.id}]]})</script>` — NO usar `/*[#th:each...]*/` (rompe parseo anidado de comentarios)

### Excepciones
- `ResourceNotFoundException` (404) — entidad no encontrada
- `BusinessException` (400) — violación de regla de negocio
- `GlobalExceptionHandler` con `@RestControllerAdvice`

### Seguridad
- `UserDetailsServiceImpl` — `loadUserForNormalLogin(username, empresaId)` y `loadUserForSuperAdmin(username)`
- `CustomAuthenticationFilter` + `CustomAuthenticationProvider` + `CustomAuthenticationToken`
- `CustomUserDetails` con `isEsSuperadmin()`, getEmpresaId(), getNombre()
- `TenantContext` (ThreadLocal) + `TenantFilter` inyecta `empresaId` en cada request
- `TenantInterceptor` verifica que la empresa esté activa en cada request; si no, redirige al login con error `empresa_inactiva`
- Proteger endpoints por rol: `ADMIN`, `CAJERO`, `DEV`
- Al crear usuario desde empresa, el admin solo puede asignar `CAJERO` o `ADMIN`
- Superadmin usa `hasRole('SUPERADMIN')` y cadena de autenticación separada en SecurityConfig
- Superadmin redirigido a `/superadmin/dashboard` post-login
- `@EnableMethodSecurity` en `SecurityConfig` para habilitar `@PreAuthorize`
- Usar `@PreAuthorize("hasRole('ADMIN')")` en **controller y service** para doble protección (URL + bean)
- Tenant isolation reforzada en service: método privado `verificarPerteneceAEmpresa(Ingrediente)` que compara `TenantContext.getEmpresaId()` con la entidad y lanza `ResourceNotFoundException` si no coincide

### Testing
- **Stack**: JUnit 5 + Mockito (`spring-boot-starter-test` lo incluye)
- **Patrón preferido**: tests unitarios con Mockito puro (`@ExtendWith(MockitoExtension.class)`)
  - Rápidos (~1s vs ~15s con `@SpringBootTest`)
  - No conectan a BD, todo es mock
  - No evalúan `@PreAuthorize` (eso lo cubre el filtro de URL en `SecurityConfig`)
- **NO usar `@SpringBootTest`** salvo que se necesite contexto real (ej: tests de integración con BD)
- **Estructura de archivos** (paquete `src/test/java/com/mibombay/sistemaresurante/`):
  - `services/` → `XxxServiceTest.java` con los tests del service
  - `testdata/` → `XxxTestData.java` con datos fake reusables (Object Mother pattern)
- **Patrón de cada test** (3 pasos):
  ```java
  @Test
  void metodoEscenario_resultadoEsperado() {
      // 1) preparar: configurar mocks (when...)
      when(repo.metodoX(args)).thenReturn(resultado);

      // 2) ejecutar: llamar al metodo del service
      T result = service.metodoX(args);

      // 3) verificar: assertEquals / assertThrows / verify(mock)
      assertEquals(esperado, result);
  }
  ```
- **Mocks estándar** (en cada test class):
  ```java
  @Mock private XxxRepository repository;
  @Mock private XxxMapper mapper;
  @InjectMocks private XxxService service;
  ```
- **Multi-tenant en tests**: setear `TenantContext.setEmpresaId(1L)` en `@BeforeEach`, limpiar en `@AfterEach`
- **Test data class** (Object Mother, todos métodos `static`):
  ```java
  public class XxxTestData {
      public static Xxx crearXxx(Long id, ...) { return Xxx.builder()...build(); }
      public static XxxRequest crearRequestValido() { ... }
  }
  ```
- **Nombres**: `metodoEscenario_resultadoEsperado` (ej: `obtenerUsuarioPorId_otroTenant_lanza404`)
- **Imports clave**:
  - `org.junit.jupiter.api.{Test,BeforeEach,AfterEach,DisplayName}`
  - `org.mockito.junit.jupiter.MockitoExtension`
  - `org.mockito.{Mock,InjectMocks}`
  - `static org.mockito.Mockito.{when,verify,never}`
  - `static org.mockito.ArgumentMatchers.{any,anyLong,eq}`

## Reglas de negocio clave
1. Producto con receta → stock = min(stock_ingrediente / cantidad); precioCompra = BigDecimal.ZERO (no null, para evitar null en BD, se ignora en cálculos)
2. Producto sin receta → stock manual (`stockActual` en Producto), precioCompra directo. Ambos campos siempre con valor (BigDecimal.ZERO mínimo) para evitar null en BD.
3. Una receta por producto (1:1)
4. Ingrediente no se elimina si está en recetas
5. RecetaDetalle.costo = cantidad × ingrediente.precioCompra; Receta.costoReceta = suma de todos
6. Compra → actualiza stock de Ingrediente o Producto automáticamente
7. CompraDetalle → puede comprar Ingredientes o Productos sin receta en la misma compra (tipo MIXTO)
8. Compra anulada → se reversa el stock (resta lo sumado)
9. Venta → bloquea stock 0, requiere día sin cierre Z
10. Cierre Z → bloquea ventas posteriores del mismo día
11. Superadmin sin filtro tenant
12. MovimientoInventario registra cada cambio de stock con trazabilidad: usuario, fecha/hora, stock anterior y posterior, tipo de movimiento, ID de referencia (compra/venta)
13. CompraService inyecta movimientos automáticamente al crear, editar o anular una compra
14. VentaService descuenta ingredientes (productos con receta) o producto.stockActual (sin receta). Anulación revierte el descuento y crea VENTA_ANULACION. MetodoPago disponible: EFECTIVO, TRANSFERENCIA, MIXTO
15. Venta adicionales y modificaciones: ingredientes excluidos via 🖊️ en POS. `modificaciones` (TEXT) se persiste en VentaDetalle. `ingredientesExcluidosIds` es transient. Stock no descuenta ingredientes excluidos. `adicionales` (TEXT) guarda comentario libre del cajero (ej: "picado a la mitad").
16. Cambio (vuelto) calculado server-side: `max(0, recibidoEfectivo + recibidoTransferencia - total)`. Siempre 0 si metodo pago es TRANSFERENCIA.
17. Venta.paraLlevar (Boolean): toggle en POS que indica si el pedido completo es para llevar. Se persiste en BD, se muestra en recibo y en historial con columna y filtro.
18. Cuadre de Caja: vista independiente del Cierre Z. Muestra formulario con denominaciones COP (billetes y monedas separados). Total sistema = efectivo neto (recibidoEfectivo - cambio). Diferencia calculada como totalContado - totalSistema. Sin persistencia aún.
19. VentaSuspendida (órdenes en espera): persiste carritos pausados por usuario en BD (`venta_suspendida`). Multi-tab en POS (máx 8). Sync debounced 800ms + `sendBeacon` en `beforeunload`. No reserva stock (se valida al cobrar). Al cobrar exitosamente, `VentaController.guardar()` elimina la orden vía `suspendidaId`. Si el cobro falla, la orden se conserva. Por usuario (cada cajero ve solo sus órdenes). Sobrevive a refresh, navegación, reinicio y cambio de equipo.
20. Inventario físico: merma y desperdicio se registran en el formulario de carga diaria. `confirmar()` primero procesa merma (MERMA, signo "-"), luego desperdicio (DESPERDICIO, signo "-"), y solo la diferencia inexplicada ((stockSistema − merma − desperdicio) − stockFisico) ≠ 0 crea AJUSTE_INVENTARIO. Stock final = stockFisico. **Signo del AJUSTE_INVENTARIO**: `stockActual − stockFisico > 0` (faltante) → signo `"-"` (reduce stock); `stockActual − stockFisico < 0` (sobrante) → signo `"+"` (aumenta stock). **Convención de display**: en inventario y reporte de consumo se muestra positiva = sobrante (ahorro); en Food Cost se usa positiva = faltante (costo). **DataInitializer** crea un inventario físico inicial CONFIRMADO al sembrar datos, estableciendo el punto de partida del stock.
24. **Reporte de Consumo**: tabla con 14 columnas (Stock DESDE, **Compras**, Consumo, Merma, $ Merma, Desperdicio, $ Desperdicio, Dif. Inexplicada, $ Diferencia, Stock HASTA, Stock Real). Filtro DESDE/HASTA. Cálculos: **(a) Stock HASTA** = último inventario físico confirmado en o antes de `hasta` + `netoCambio(fechaInv+1 .. hasta)` (ajusta movimientos posteriores al inventario físico); **(b) Stock DESDE** = siempre `Stock HASTA − netoCambio(desde..hasta)`; **(c) Compras** = `sum(COMPRA) − sum(COMPRA_ANULACION)` (neto, no duplica al editar compras); **(d) Consumo** = suma VENTA. Merma/desperdicio/diferencia = sumas de InventarioFisicoDetalle de inventarios CONFIRMADO en el período. Costos = cantidad × precioCompra. Exportación Excel con `ConsumoExcelService`. **Repositorio**: `findUltimoConfirmadoByItem` y `findUltimoConfirmadoByItemAntesDe` retornan `List<InventarioFisicoDetalle>` (no `Optional`) para evitar `NonUniqueResultException`. **Convención de signo en diferencia**: `diferencia = stockEsperado − stockFisico` → positiva = faltante (pérdida), negativa = sobrante (ahorro). **Resumen Costo Comida** rediseñado con 3 gauges (Food Cost Global, Costo Real, Costo Contable), 6 stats cards (Ventas, Costo Ingredientes, Merma, Desperdicio, Diferencia, Costo Real) y footer inline — misma estructura visual que el módulo Food Cost. `ConsumoResumenDTO` con campos: `costoRealValor` (Vendido + Merma + Desperdicio + Diferencia), `costoAlimentosContable` (Vendido + Merma + Desperdicio).
25. Key compuesta: para evitar colisión de IDs entre ingredientes y productos (ambos auto-increment), todas las referencias en inventario físico y reportes usan `itemTipo + ":" + itemId`.
26. Tablas anchas: `.sa-table-wrapper` usa `overflow-x: auto` para scroll horizontal.
 27. **Ingrediente consumible** (`consumible` Boolean): ingredientes de uso indirecto (aceite, harina, sal, especias) que no se pueden medir por receta. Se gestionan por períodos semanales/mensuales en `ConsumoPeriodo`. No aparecen en el inventario diario (`InventarioFisicoService` filtra `consumible=false`). **Una vez creado como consumible, no se puede desmarcar** (el checkbox se oculta al editar, el mapper ignora el campo y el service lanza `BusinessException`).
28. **ConsumoPeriodo** (semanal/mensual): flujo: borrador → formulario (Stock Sistema, Merma, Desperdicio, Stock Final) → confirmar. Al confirmar se crean 3 movimientos por detalle: `CONSUMO`, `MERMA`, `DESPERDICIO`. El consumido se calcula como `stockSistema - stockFinal - merma - desperdicio`. Stock final = `stockFinal`.
29. **Food Cost Diario**: reporte en `/admin/food-cost` que muestra:
    - Sin selector de fecha (siempre día actual). Vista histórica en `/admin/food-cost/historico` con selector de fecha
    - Persistencia de items: entidad `CostoComidaDiariaItem` con relación `@OneToMany` a `CostoComidaDiaria`. Al guardar los costos del día se persisten todos los items (ingredientes expandidos desde recetas + productos sin receta + consumibles)
    - Si es hoy → items calculados en tiempo real (+ resumen desde BD si existe)
    - Si no es hoy (histórico) → items cargados desde BD
    - **3 gauges**: Food Cost Global (Vendido/Ventas), **Costo Real** (Vendido + Merma + Desperdicio + Diferencia)/Ventas, **Costo Contable** (Vendido + Merma + Desperdicio)/Ventas
    - Food Cost por item: desglose de ingredientes (expandidos desde recetas de productos vendidos) + productos sin receta + consumibles (CONSUMO del día). Columnas: Item, Tipo, Cant. Consumida, Unidad, Precio Costo Unit., Grupo ($), % del Costo (% sobre ventas totales)
    - Merma y Desperdicio muestran $ y % por separado (calculado como: valor / ventasTotales × 100)
    - Diferencia de inventario en $ y % (brecha entre Costo Real y Costo Contable; positiva = faltante, negativa = sobrante)
    - Convención de signo en `InventarioFisicoDetalle.diferencia`: `stockEsperado − stockFisico` (positiva = faltante, negativa = sobrante)
    - `FoodCostService.calcularConsumoIndirectoValor()` suma movimientos CONSUMO × precioCompra del día
    - `FoodCostService.calcularPorItem()` expande ventas a ingredientes + productos sin receta + consumibles
    - `FoodCostItemDTO` con: itemId, itemNombre, itemTipo (INGREDIENTE/CONSUMIBLE/PRODUCTO), unidadMedida, cantidadConsumida, precioCostoUnitario, costoGrupo, porcentajeDelCosto
    - `FoodCostResumenDTO` con campos: mermaPorcentaje, desperdicioPorcentaje, diferenciaInventarioPorcentaje
    - Exportación Excel con `FoodCostExcelService` (incluye % junto a $)
30. **Dashboard con estadísticas y notificaciones en tiempo real**: Vista en `GET /dashboard` con 4 stat cards dinámicas (total ventas del día, número de ventas, productos con stock bajo ≤10 unidades, ID empresa) + módulos del sistema con links a secciones principales. **WebSocket** para notificaciones push: `WebSocketConfig` con STOMP broker `/topic`, endpoint `/ws` (SockJS). `NotificacionService` publica en `/topic/empresa/{empresaId}/ventas` al crear/anular venta. `DashboardService` consulta stats del día. `DashboardStatsDTO` con: totalVentasDia, numeroVentasDia, productosStockBajo, empresaId. `VentaNotificacionDTO` con: ventaId, total, metodoPago, fechaVenta, nombreUsuario, nombreCliente, tipo (NUEVA_VENTA/VENTA_ANULADA), statsActualizadas. Dashboard se suscribe vía SockJS + STOMP, actualiza stats en tiempo real y muestra toast notifications (solo ADMIN). Endpoint REST `GET /api/v1/dashboard/stats` para refresh manual.
31. **Responsive design**: Layout mobile-first con breakpoints consolidados (768px tablet, 1024px desktop pequeño). **Sidebar**: overlay con backdrop en mobile (≤768px), toggle vía botón hamburguesa, se cierra al hacer click en link o backdrop. **POS mobile**: cart drawer desde abajo con handle visual, backdrop oscuro, altura 75vh, se abre/cierra con click en header. Cart actions compacto en mobile (cliente row apilado, pago cards sin descripción). **Filtros**: flex-wrap en desktop, columna en mobile. **Tablas**: scroll horizontal + primera columna (avatar) oculta en mobile. **Forms**: padding reducido en mobile. CSS variables `--ease-out-expo` para transiciones suaves.

## Orden de implementación
1. Config inicial (pom.xml, application.yml, paquetes base) ✅
2. Entidades JPA + repositorios + migraciones ✅ (parcial)
3. Multi-tenant (TenantContext, TenantFilter, TenantInterceptor) ✅
4. Seguridad (Spring Security, login empresa, login superadmin) ✅
5. Landing page + registro público de empresas ✅
6. CRUD Empresas (superadmin) ✅
7. Dashboard admin + sidebar con navegación ✅
8. Personalización (colores, tipografía por empresa) ✅
9. CRUD Usuarios (admin dentro de empresa) con diseño dark ✅
10. CRUD Ingredientes (con paginación + filtros, seguridad multi-tenant) ✅
11. CRUD Productos + Recetas (stock calculado, costos automáticos, margen, toggle receta) ✅
12. DataInitializer con ingredientes, productos simples y con receta (precios COP) ✅
13. CRUD Clientes (con Consumidor Final por defecto, no editable) ✅
14. CRUD Proveedores (con Proveedor Genérico por defecto, no editable) ✅
15. Módulo de Compras + MovimientoInventario (Compra + CompraDetalle, actualización de stock, tipo MIXTO, editar/anular con reversión de stock y trazabilidad) ✅
16. Módulo de Movimientos de Inventario (listado paginado con filtros por tipo/item, sidebar) ✅
17. Módulo de Inventario Actual (vista combinada ingredientes + productos con filtros por nombre, unidad, tipo producto; paginación independiente por tabla) ✅
18. Módulo de Ventas (PDV) ✅
19. Cierre Z + Reporte X PDF ✅
20. Cuadre de Caja (vista + persistencia BD + PDF + historial + reimpresión) ✅
21. PDF generation con OpenHTMLtoPDF (Reporte X, Cierre Z, Cuadre de Caja) ✅
22. VentaSuspendida — Tabs de órdenes en espera en POS (multi-tab, sync backend, sobrevive navegación/reinicio) ✅
23. Inventario físico + merma/desperdicio + Excel ✅
24. Reporte de Consumo + Excel ✅
25. Food Cost Diario (resumen diario, desglose por item, % merma/desperdicio/diferencia, Excel). **Cálculo usa `MovimientoInventario`** (no expande `RecetaDetalle`) para respetar exclusiones de ingredientes en ventas. ✅
26. Consumo Indirecto (ingredientes consumibles con carga semanal/mensual + merma + desperdicio) ✅
27. Dashboard con estadísticas + notificaciones WebSocket en tiempo real ✅
28. Responsive design (sidebar mobile, POS drawer, filtros, tablas) ✅
29. 🆕 **Reorganización de `.properties` + Variables de entorno** (2026-07-17) ✅
30. 🆕 **OWASP A01: Broken Access Control** (auditoría + `@PreAuthorize` en services) — en progreso

## Configuración de Properties y Variables de Entorno (2026-07-17)

### Estructura de archivos `.properties`
```
src/main/resources/
├── application.properties                  (base/compartida, sin secretos)
├── application-dev.properties              (dev, lee de env vars)
├── application-prod.properties             (prod, sin defaults, todo via env vars)
└── application-local.properties.example    (plantilla, gitignored)
```

### Nombres de env vars (alineados con el usuario)
- `DB_URL` — JDBC connection string
- `DB_USER` — usuario de MySQL
- `DB_PASSWORD` — contraseña (NO subir al repo)
- `SPRING_PROFILES_ACTIVE` — perfil activo (dev/prod)

### Sintaxis de placeholders
- `${DB_URL:jdbc:mysql://localhost:3306/mibombay}` — con default
- `${DB_PASSWORD}` — sin default, falla si no existe (usar en prod)

### Configurar env vars permanentes en Linux
```bash
echo 'export DB_URL="..."' >> ~/.bashrc
echo 'export DB_USER=root' >> ~/.bashrc
echo 'export DB_PASSWORD=tu_password' >> ~/.bashrc
echo 'export SPRING_PROFILES_ACTIVE=dev' >> ~/.bashrc
source ~/.bashrc
```

### Jerarquía de precedencia
1. Argumentos CLI (`--DB_PASSWORD=xxx`)
2. Variables de entorno (`export DB_PASSWORD=xxx`)
3. `application-{profile}.properties`
4. `application.properties` (base)

### Archivos gitignored
- `application-local.properties` (secretos personales)
- `planDesarrollo` (notas de seguimiento)
- `docs/` (auditorías internas)
- `*.txt` (notas de trabajo, excepto `AGENTS.md`)

## OWASP A01: Broken Access Control (En progreso)

**Doble protección obligatoria**: `@PreAuthorize` en controller Y en service.

### Servicios protegidos con `@PreAuthorize` (al 2026-07-17)
| Servicio | Métodos | Rol |
|----------|---------|-----|
| ClienteService | 8 | ADMIN+CAJERO |
| ProductoService | 5 | ADMIN |
| VentaService | 4 | ADMIN+CAJERO |
| ProveedorService | 6 | ADMIN |
| IngredienteService | 6 | ADMIN |
| InventarioFisicoService | 9 | ADMIN |
| CompraService | 3 | ADMIN |
| CierreZService | 4 | mixto |
| RecetaService | 4 | ADMIN |
| ConsumoPeriodoService | 3 | ADMIN |
| CuadreCajaService | 3 | ADMIN |
| VentaSuspendidaService | 3 | ADMIN+CAJERO |
| DashboardService | 1 | ADMIN+CAJERO |
| **UsuarioService** | 8 | ADMIN (agregado 2026-07-17) |
| **MovimientoInventarioService** | 1 | ADMIN (agregado 2026-07-17) |
| **EstiloConfiguracionService** | 4 | ADMIN (agregado 2026-07-17) |
| **ReporteService** | 1 | ADMIN (agregado 2026-07-17) |
| **FoodCostService** | 10 | ADMIN (agregado 2026-07-17) |

### Tests de seguridad (22 tests)
```
src/test/java/com/mibombay/sistemaresurante/security/owasp/
├── UsuarioServiceSecurityTest.java              (5 tests)
├── MovimientoInventarioServiceSecurityTest.java (3 tests)
├── EstiloConfiguracionServiceSecurityTest.java  (3 tests)
├── ReporteServiceSecurityTest.java              (3 tests)
└── FoodCostServiceSecurityTest.java             (8 tests)
```

### Patrón de test
```java
@SpringBootTest
class UsuarioServiceSecurityTest {
    @Autowired private UsuarioService service;

    @Test @WithMockUser(roles = "ADMIN")
    void adminPuedeListar() {
        assertDoesNotThrow(() -> service.listarPorEmpresa(1L));
    }

    @Test @WithMockUser(roles = "CAJERO")
    void cajeroNoPuedeListar() {
        assertThrows(AccessDeniedException.class,
            () -> service.listarPorEmpresa(1L));
    }
}
```

### Pendiente OWASP A01
- [ ] Fase 2: Multi-tenant isolation (verificar `empresaId` en queries)
- [ ] Fase 3: Method-level security (revisar services que faltan)
- [ ] Fase 4: IDOR (tests cross-tenant)
- [ ] Fase 5: Path traversal
- [ ] Fase 6: CORS, CSRF, session
- [ ] Fase 7: Logging de eventos sensibles

## Problemas conocidos

### Stock en 0 al editar ingredientes y productos sin receta
Al abrir el formulario de edición (`/ingredientes/{id}/editar` o editar producto sin receta), el campo `stockActual` se muestra como 0 en lugar del valor real persistido en BD. No se ha determinado si es problema de binding del formulario o de persistencia.

### Contraseña BD expuesta en primer commit (resuelto parcialmente)
La contraseña `Kristoff_Mora26123009` quedó visible en el primer commit público de GitHub. Se limpió el historial local pero sigue en el commit remoto. **Recomendado**: rotar la contraseña en MySQL.

## Refactor pendiente — FoodCostService

**Archivo**: `FoodCostService.java` (552 líneas, 16 métodos)

### Problemas detectados
1. **N+1 queries**: ~420 queries por día con 50 ingredientes + 20 productos (6 queries por item)
2. **Duplicación**: loops de ingredientes y productos son casi idénticos (~20 líneas duplicadas)
3. **Query de ventas duplicada**: `calcularDiario` y `calcularPorItemFromMovimientos` ejecutan la misma query
4. **Construcción de FoodCostItemDTO**: repetida en 3 lugares distintos
5. **Sumas repetitivas**: `calcularResumenConsumo` tiene 13 sumas de BigDecimal con patrón idéntico

### Plan de refactor propuesto
1. Extraer helper `acumularValoresItem()` para eliminar duplicación de loops (prioridad alta)
2. Extraer mapper para construcción de `FoodCostItemDTO` (3 lugares)
3. Analizar `calcularPorItemFromMovimientos` para eliminar duplicación con `calcularDiario`
4. Reducir `calcularResumenConsumo` (112 líneas → stream + reduce)
5. Optimizar queries N+1 con queries agregadas en repositorios (cuando haya tiempo)

## Comandos

```bash
# Compilar
./mvnw compile

# Tests
./mvnw test
./mvnw test -Dtest="*ServiceSecurityTest"

# Build sin tests
./mvnw clean install -DskipTests

# Ejecutar con perfil dev (lee env vars)
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run

# O con argumento
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Git - trabajar en rama
git checkout -b feature/nombre
git add .
git commit -m "descripcion"
git push origin feature/nombre
```
