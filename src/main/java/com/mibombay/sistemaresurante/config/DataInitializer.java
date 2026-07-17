package com.mibombay.sistemaresurante.config;

import com.mibombay.sistemaresurante.models.*;
import com.mibombay.sistemaresurante.models.enums.*;
import com.mibombay.sistemaresurante.repositories.*;
import com.mibombay.sistemaresurante.services.EstiloConfiguracionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final IngredienteRepository ingredienteRepository;
    private final ProductoRepository productoRepository;
    private final RecetaRepository recetaRepository;
    private final RecetaDetalleRepository recetaDetalleRepository;
    private final ClienteRepository clienteRepository;
    private final ProveedorRepository proveedorRepository;
    private final CompraRepository compraRepository;
    private final CompraDetalleRepository compraDetalleRepository;
    private final VentaRepository ventaRepository;
    private final VentaDetalleRepository ventaDetalleRepository;
    private final CierreZRepository cierreZRepository;
    private final CostoComidaDiariaRepository costoComidaDiariaRepository;
    private final CostoComidaDiariaItemRepository costoComidaDiariaItemRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final InventarioFisicoRepository inventarioFisicoRepository;
    private final InventarioFisicoDetalleRepository inventarioFisicoDetalleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EstiloConfiguracionService estiloConfiguracionService;

    public DataInitializer(EmpresaRepository empresaRepository,
                           UsuarioRepository usuarioRepository,
                           IngredienteRepository ingredienteRepository,
                           ProductoRepository productoRepository,
                           RecetaRepository recetaRepository,
                           RecetaDetalleRepository recetaDetalleRepository,
                           ClienteRepository clienteRepository,
                           ProveedorRepository proveedorRepository,
                           CompraRepository compraRepository,
                           CompraDetalleRepository compraDetalleRepository,
                           VentaRepository ventaRepository,
                           VentaDetalleRepository ventaDetalleRepository,
                           CierreZRepository cierreZRepository,
                           CostoComidaDiariaRepository costoComidaDiariaRepository,
                           CostoComidaDiariaItemRepository costoComidaDiariaItemRepository,
                           MovimientoInventarioRepository movimientoInventarioRepository,
                           InventarioFisicoRepository inventarioFisicoRepository,
                           InventarioFisicoDetalleRepository inventarioFisicoDetalleRepository,
                           PasswordEncoder passwordEncoder,
                           EstiloConfiguracionService estiloConfiguracionService) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.ingredienteRepository = ingredienteRepository;
        this.productoRepository = productoRepository;
        this.recetaRepository = recetaRepository;
        this.recetaDetalleRepository = recetaDetalleRepository;
        this.clienteRepository = clienteRepository;
        this.proveedorRepository = proveedorRepository;
        this.compraRepository = compraRepository;
        this.compraDetalleRepository = compraDetalleRepository;
        this.ventaRepository = ventaRepository;
        this.ventaDetalleRepository = ventaDetalleRepository;
        this.cierreZRepository = cierreZRepository;
        this.costoComidaDiariaRepository = costoComidaDiariaRepository;
        this.costoComidaDiariaItemRepository = costoComidaDiariaItemRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.inventarioFisicoRepository = inventarioFisicoRepository;
        this.inventarioFisicoDetalleRepository = inventarioFisicoDetalleRepository;
        this.passwordEncoder = passwordEncoder;
        this.estiloConfiguracionService = estiloConfiguracionService;
    }

    @Override
    @Transactional
    public void run(String... args) {

        if (usuarioRepository.count() == 0) {
            crearSuperadmin();
            Empresa empresa = crearEmpresaDemo();
            crearUsuariosDemo(empresa.getId());
            log.info("=== Inicialización completada ===");
        }

        if (ingredienteRepository.count() == 0) {
            sembrarDatosRestaurante();
        }

        if (ventaRepository.count() == 0) {
            Empresa empresa = empresaRepository.findBySubdominio("demo")
                    .orElseGet(() -> empresaRepository.findAll().stream()
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("No hay empresas para sembrar datos operativos")));
            sembrarDatosOperativos(empresa.getId());
        }
    }

    private void crearSuperadmin() {
        Usuario superadmin = Usuario.builder()
                .username("superadmin")
                .nombre("Super Administrador")
                .password(passwordEncoder.encode("admin123"))
                .rol(Rol.ADMIN)
                .activo(true)
                .esSuperadmin(true)
                .build();
        usuarioRepository.save(superadmin);
        log.info("Superadmin creado: superadmin / admin123");
    }

    private Empresa crearEmpresaDemo() {
        Empresa empresa = Empresa.builder()
                .nombre("Mi Bombay Demo")
                .subdominio("demo")
                .activo(true)
                .build();
        empresa = empresaRepository.save(empresa);
        log.info("Empresa demo creada: '{}' con subdominio '{}'", empresa.getNombre(), empresa.getSubdominio());
        estiloConfiguracionService.crearPorDefecto(empresa.getId());
        return empresa;
    }

    private void crearUsuariosDemo(Long empresaId) {
        Usuario admin = Usuario.builder()
                .empresaId(empresaId)
                .nombre("Demo Admin")
                .username("demoadmin")
                .password(passwordEncoder.encode("demoadmin123"))
                .rol(Rol.ADMIN)
                .activo(true)
                .esSuperadmin(false)
                .build();
        usuarioRepository.save(admin);

        Usuario cajero = Usuario.builder()
                .empresaId(empresaId)
                .nombre("Cajero Demo")
                .username("cajero")
                .password(passwordEncoder.encode("cajero123"))
                .rol(Rol.CAJERO)
                .activo(true)
                .esSuperadmin(false)
                .build();
        usuarioRepository.save(cajero);

        log.info("Usuarios demo creados: demoadmin / demoadmin123, cajero / cajero123");
    }

    private void sembrarDatosRestaurante() {
        log.info("=== Sembrando ingredientes de demostración ===");

        Empresa empresa = empresaRepository.findBySubdominio("demo")
                .orElseGet(() -> empresaRepository.findAll().stream()
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No hay empresas para sembrar datos")));

        Long empresaId = empresa.getId();
        Long usuarioId = 2L;

        // ═══ Ingredientes para menú completo ═══
        List<Ingrediente> ingredientes = List.of(
                // Perros calientes
                crearIngrediente(empresaId, usuarioId, "Pan de perro caliente", UnidadMedida.UNIDAD, new BigDecimal("700"), new BigDecimal("50"), false),
                crearIngrediente(empresaId, usuarioId, "Salchicha para hot dog", UnidadMedida.UNIDAD, new BigDecimal("1200"), new BigDecimal("60"), false),
                crearIngrediente(empresaId, usuarioId, "Cebolla", UnidadMedida.UNIDAD, new BigDecimal("1000"), new BigDecimal("15"), false),
                crearIngrediente(empresaId, usuarioId, "Salsa de tomate", UnidadMedida.LITRO, new BigDecimal("6000"), new BigDecimal("5"), false),
                crearIngrediente(empresaId, usuarioId, "Mostaza", UnidadMedida.LITRO, new BigDecimal("5000"), new BigDecimal("5"), false),
                // Hamburguesas
                crearIngrediente(empresaId, usuarioId, "Carne de res molida", UnidadMedida.KILOGRAMO, new BigDecimal("18000"), new BigDecimal("10"), false),
                crearIngrediente(empresaId, usuarioId, "Pan de hamburguesa", UnidadMedida.UNIDAD, new BigDecimal("800"), new BigDecimal("40"), false),
                crearIngrediente(empresaId, usuarioId, "Queso americano", UnidadMedida.UNIDAD, new BigDecimal("1500"), new BigDecimal("50"), false),
                crearIngrediente(empresaId, usuarioId, "Lechuga", UnidadMedida.KILOGRAMO, new BigDecimal("4000"), new BigDecimal("3"), false),
                crearIngrediente(empresaId, usuarioId, "Tomate", UnidadMedida.KILOGRAMO, new BigDecimal("5000"), new BigDecimal("4"), false),
                crearIngrediente(empresaId, usuarioId, "Tocino", UnidadMedida.KILOGRAMO, new BigDecimal("25000"), new BigDecimal("2"), false),
                crearIngrediente(empresaId, usuarioId, "Pechuga de pollo", UnidadMedida.KILOGRAMO, new BigDecimal("15000"), new BigDecimal("5"), false),
                crearIngrediente(empresaId, usuarioId, "Mayonesa", UnidadMedida.LITRO, new BigDecimal("8000"), new BigDecimal("3"), false),
                // Papas y consumibles
                crearIngrediente(empresaId, usuarioId, "Papas congeladas", UnidadMedida.KILOGRAMO, new BigDecimal("6000"), new BigDecimal("10"), false),
                crearIngrediente(empresaId, usuarioId, "Aceite vegetal", UnidadMedida.LITRO, new BigDecimal("7000"), new BigDecimal("5"), true)
        );
        /* ─── Ingredientes comentados (disponibles para activar después) ───
        "Carne de res molida", "Pan de hamburguesa", "Queso americano",
        "Lechuga", "Tomate", "Papas congeladas", "Aceite vegetal",
        "Pechuga de pollo", "Tocino", "Huevos", "Harina de trigo",
        "Mayonesa", "Aguacate"
        */
        ingredienteRepository.saveAll(ingredientes);
        log.info("{} ingredientes creados", ingredientes.size());

        // ─── Productos simples (bebidas) ───
        List<Producto> productosSimples = List.of(
                crearProductoSimple(empresaId, usuarioId, "Refresco de Cola 355ml", new BigDecimal("2500"), new BigDecimal("1200"), new BigDecimal("50")),
                crearProductoSimple(empresaId, usuarioId, "Refresco de Naranja 355ml", new BigDecimal("2500"), new BigDecimal("1200"), new BigDecimal("50")),
                crearProductoSimple(empresaId, usuarioId, "Agua embotellada 500ml", new BigDecimal("2000"), new BigDecimal("800"), new BigDecimal("50"))
        );
        productoRepository.saveAll(productosSimples);
        log.info("{} productos simples creados", productosSimples.size());

        // ─── Productos con receta ───
        java.util.Map<String, Ingrediente> ingMap = new java.util.HashMap<>();
        for (Ingrediente i : ingredienteRepository.findAllByEmpresaIdAndActivoTrueOrderByNombreAsc(empresaId)) {
            ingMap.put(i.getNombre(), i);
        }

        // Perros calientes
        crearProductoConReceta(empresaId, usuarioId, ingMap, "Perro Caliente Sencillo", new BigDecimal("7000"),
                List.of(
                        new ItemReceta("Pan de perro caliente", new BigDecimal("1")),
                        new ItemReceta("Salchicha para hot dog", new BigDecimal("1")),
                        new ItemReceta("Cebolla", new BigDecimal("0.05")),
                        new ItemReceta("Salsa de tomate", new BigDecimal("0.03")),
                        new ItemReceta("Mostaza", new BigDecimal("0.015"))
                ));

        crearProductoConReceta(empresaId, usuarioId, ingMap, "Perro Caliente Especial", new BigDecimal("10000"),
                List.of(
                        new ItemReceta("Pan de perro caliente", new BigDecimal("1")),
                        new ItemReceta("Salchicha para hot dog", new BigDecimal("1")),
                        new ItemReceta("Tocino", new BigDecimal("0.03")),
                        new ItemReceta("Queso americano", new BigDecimal("1")),
                        new ItemReceta("Cebolla", new BigDecimal("0.05")),
                        new ItemReceta("Salsa de tomate", new BigDecimal("0.03")),
                        new ItemReceta("Mostaza", new BigDecimal("0.015"))
                ));

        // Hamburguesas
        crearProductoConReceta(empresaId, usuarioId, ingMap, "Hamburguesa Clásica", new BigDecimal("14000"),
                List.of(
                        new ItemReceta("Pan de hamburguesa", new BigDecimal("1")),
                        new ItemReceta("Carne de res molida", new BigDecimal("0.15")),
                        new ItemReceta("Queso americano", new BigDecimal("1")),
                        new ItemReceta("Lechuga", new BigDecimal("0.03")),
                        new ItemReceta("Tomate", new BigDecimal("0.05")),
                        new ItemReceta("Salsa de tomate", new BigDecimal("0.02")),
                        new ItemReceta("Mayonesa", new BigDecimal("0.015"))
                ));

        crearProductoConReceta(empresaId, usuarioId, ingMap, "Hamburguesa BBQ con Tocino", new BigDecimal("18000"),
                List.of(
                        new ItemReceta("Pan de hamburguesa", new BigDecimal("1")),
                        new ItemReceta("Carne de res molida", new BigDecimal("0.18")),
                        new ItemReceta("Tocino", new BigDecimal("0.05")),
                        new ItemReceta("Queso americano", new BigDecimal("2")),
                        new ItemReceta("Lechuga", new BigDecimal("0.02")),
                        new ItemReceta("Tomate", new BigDecimal("0.04")),
                        new ItemReceta("Salsa de tomate", new BigDecimal("0.03"))
                ));

        crearProductoConReceta(empresaId, usuarioId, ingMap, "Hamburguesa de Pollo", new BigDecimal("15000"),
                List.of(
                        new ItemReceta("Pan de hamburguesa", new BigDecimal("1")),
                        new ItemReceta("Pechuga de pollo", new BigDecimal("0.15")),
                        new ItemReceta("Lechuga", new BigDecimal("0.04")),
                        new ItemReceta("Tomate", new BigDecimal("0.05")),
                        new ItemReceta("Mayonesa", new BigDecimal("0.02"))
                ));

        // Papas
        crearProductoConReceta(empresaId, usuarioId, ingMap, "Porción de Papas Fritas", new BigDecimal("6000"),
                List.of(
                        new ItemReceta("Papas congeladas", new BigDecimal("0.2")),
                        new ItemReceta("Aceite vegetal", new BigDecimal("0.02"))
                ));

        if (clienteRepository.findByEmpresaIdAndEsConsumidorFinalTrueAndActivoTrue(empresaId).isEmpty()) {
            Cliente consumidorFinal = Cliente.builder()
                    .empresaId(empresaId)
                    .nombres("Consumidor")
                    .apellidos("Final")
                    .dni("9999999")
                    .esConsumidorFinal(true)
                    .build();
            clienteRepository.save(consumidorFinal);
            log.info("Consumidor Final creado para empresa {}", empresaId);
        }

        if (proveedorRepository.findByEmpresaIdAndEsProveedorDefectoTrueAndActivoTrue(empresaId).isEmpty()) {
            Proveedor proveedorDefecto = Proveedor.builder()
                    .empresaId(empresaId)
                    .razonSocial("Proveedor Genérico")
                    .contacto("Sin contacto")
                    .esProveedorDefecto(true)
                    .build();
            proveedorRepository.save(proveedorDefecto);
            log.info("Proveedor por defecto creado para empresa {}", empresaId);
        }

        // ─── Inventario físico inicial (punto de partida) ───
        if (inventarioFisicoRepository.countByEmpresaIdAndActivoTrue(empresaId) == 0) {
            cargarInventarioFisicoInicial(empresaId, usuarioId);
        }

        log.info("=== Siembra de datos completada ===");
    }

    private Ingrediente crearIngrediente(Long empresaId, Long usuarioId, String nombre, UnidadMedida unidad, BigDecimal precioCompra, BigDecimal stock, boolean consumible) {
        return Ingrediente.builder()
                .empresaId(empresaId)
                .usuarioId(usuarioId)
                .nombre(nombre)
                .unidadMedida(unidad)
                .precioCompra(precioCompra)
                .stockActual(stock)
                .stockMinimo(BigDecimal.ZERO)
                .consumible(consumible)
                .build();
    }

    private Producto crearProductoSimple(Long empresaId, Long usuarioId, String nombre, BigDecimal precioVenta, BigDecimal precioCompra, BigDecimal stockActual) {
        return Producto.builder()
                .empresaId(empresaId)
                .usuarioId(usuarioId)
                .nombre(nombre)
                .precioVenta(precioVenta)
                .precioCompra(precioCompra)
                .tieneReceta(false)
                .stockActual(stockActual)
                .build();
    }

    private void crearProductoConReceta(Long empresaId, Long usuarioId, java.util.Map<String, Ingrediente> ingMap,
                                        String nombre, BigDecimal precioVenta, List<ItemReceta> items) {
        Producto producto = Producto.builder()
                .empresaId(empresaId)
                .usuarioId(usuarioId)
                .nombre(nombre)
                .precioVenta(precioVenta)
                .precioCompra(BigDecimal.ZERO)
                .tieneReceta(true)
                .stockActual(BigDecimal.ZERO)
                .build();
        producto = productoRepository.save(producto);

        BigDecimal costoTotal = BigDecimal.ZERO;
        for (ItemReceta item : items) {
            Ingrediente ing = ingMap.get(item.nombreIngrediente());
            if (ing == null) {
                log.warn("Ingrediente '{}' no encontrado para producto '{}'", item.nombreIngrediente(), nombre);
                continue;
            }
            BigDecimal precio = ing.getPrecioCompra() != null ? ing.getPrecioCompra() : BigDecimal.ZERO;
            BigDecimal costo = item.cantidad().multiply(precio).setScale(2, java.math.RoundingMode.HALF_UP);
            costoTotal = costoTotal.add(costo);
        }

        Receta receta = Receta.builder()
                .empresaId(empresaId)
                .productoId(producto.getId())
                .nombreReceta("Receta de " + nombre)
                .costoReceta(costoTotal)
                .build();
        receta = recetaRepository.save(receta);

        for (ItemReceta item : items) {
            Ingrediente ing = ingMap.get(item.nombreIngrediente());
            if (ing == null) continue;
            BigDecimal precio = ing.getPrecioCompra() != null ? ing.getPrecioCompra() : BigDecimal.ZERO;
            BigDecimal costo = item.cantidad().multiply(precio).setScale(2, java.math.RoundingMode.HALF_UP);
            RecetaDetalle detalle = RecetaDetalle.builder()
                    .recetaId(receta.getId())
                    .ingredienteId(ing.getId())
                    .cantidad(item.cantidad())
                    .costo(costo)
                    .build();
            recetaDetalleRepository.save(detalle);
        }

        log.info("Producto con receta creado: '{}' → ${}/u (costo: ${})", nombre, precioVenta, costoTotal);
    }

    private record ItemReceta(String nombreIngrediente, BigDecimal cantidad) {}

    private void cargarInventarioFisicoInicial(Long empresaId, Long usuarioId) {
        InventarioFisico inventario = InventarioFisico.builder()
                .empresaId(empresaId)
                .usuarioId(usuarioId)
                .fecha(LocalDate.now())
                .estado(InventarioEstado.CONFIRMADO)
                .observaciones("Inventario físico inicial — creado al sembrar datos")
                .build();
        inventario = inventarioFisicoRepository.save(inventario);

        List<InventarioFisicoDetalle> detalles = new ArrayList<>();
        for (Ingrediente ing : ingredienteRepository.findAllByEmpresaIdAndActivoTrueAndConsumibleFalseOrderByNombreAsc(empresaId)) {
            BigDecimal stock = ing.getStockActual() != null ? ing.getStockActual() : BigDecimal.ZERO;
            detalles.add(InventarioFisicoDetalle.builder()
                    .inventarioFisicoId(inventario.getId())
                    .itemTipo("INGREDIENTE")
                    .itemId(ing.getId())
                    .itemNombre(ing.getNombre())
                    .unidadMedida(ing.getUnidadMedida() != null ? ing.getUnidadMedida().name() : null)
                    .stockSistema(stock)
                    .stockFisico(stock)
                    .merma(BigDecimal.ZERO)
                    .desperdicio(BigDecimal.ZERO)
                    .diferencia(BigDecimal.ZERO)
                    .build());
        }
        for (Producto p : productoRepository.findAllByEmpresaIdAndTieneRecetaFalseAndActivoTrueOrderByNombreAsc(empresaId)) {
            BigDecimal stock = p.getStockActual() != null ? p.getStockActual() : BigDecimal.ZERO;
            detalles.add(InventarioFisicoDetalle.builder()
                    .inventarioFisicoId(inventario.getId())
                    .itemTipo("PRODUCTO")
                    .itemId(p.getId())
                    .itemNombre(p.getNombre())
                    .unidadMedida("UNIDAD")
                    .stockSistema(stock)
                    .stockFisico(stock)
                    .merma(BigDecimal.ZERO)
                    .desperdicio(BigDecimal.ZERO)
                    .diferencia(BigDecimal.ZERO)
                    .build());
        }
        inventarioFisicoDetalleRepository.saveAll(detalles);
        log.info("Inventario físico inicial creado con {} items", detalles.size());
    }

    // ═══════════════════════════════════════════════════════════════
    //  DATOS OPERATIVOS — 5 DÍAS (compras, ventas, cierre Z, food cost)
    // ═══════════════════════════════════════════════════════════════

    private void sembrarDatosOperativos(Long empresaId) {
        log.info("=== Sembrando datos operativos de 5 días ===");
        Long adminId = 2L;
        Long cajeroId = 3L;

        // ── Cargar todos los productos y recetas ──
        List<Producto> todosProductos = productoRepository.findAllByEmpresaIdAndActivoTrueOrderByNombreAsc(empresaId);
        List<Producto> productosConReceta = todosProductos.stream().filter(p -> Boolean.TRUE.equals(p.getTieneReceta())).toList();
        List<Producto> productosSimples = todosProductos.stream().filter(p -> !Boolean.TRUE.equals(p.getTieneReceta())).toList();

        Map<Long, List<RecetaDetalle>> recetasPorProducto = new HashMap<>();
        for (Producto p : productosConReceta) {
            Receta receta = recetaRepository.findByProductoIdAndActivoTrue(p.getId()).orElse(null);
            if (receta != null) {
                recetasPorProducto.put(p.getId(), recetaDetalleRepository.findByRecetaId(receta.getId()));
            }
        }

        Map<String, Ingrediente> ingPorNombre = new HashMap<>();
        Map<Long, Ingrediente> ingPorId = new HashMap<>();
        for (Ingrediente ing : ingredienteRepository.findAllByEmpresaIdAndActivoTrueOrderByNombreAsc(empresaId)) {
            ingPorNombre.put(ing.getNombre(), ing);
            ingPorId.put(ing.getId(), ing);
        }

        // ── Clientes (50) ──
        String[][] clientesData = {
            {"Carlos","Mendoza","3010"},{"María","Rodríguez","3011"},{"José","García","3012"},
            {"Ana","López","3013"},{"Luis","Martínez","3014"},{"Carmen","Álvarez","3015"},
            {"Andrés","González","3016"},{"Patricia","Díaz","3017"},{"Fernando","Torres","3018"},
            {"Laura","Ramírez","3019"},{"Pedro","Sánchez","3020"},{"Sofía","Cruz","3021"},
            {"Miguel","Ortiz","3022"},{"Valentina","Reyes","3023"},{"Alejandro","Morales","3024"},
            {"Isabella","Vargas","3025"},{"Diego","Castro","3026"},{"Camila","Medina","3027"},
            {"Javier","Ríos","3028"},{"Gabriela","Arias","3029"},{"Ricardo","Peña","3030"},
            {"Daniela","Flores","3031"},{"Santiago","Campos","3032"},{"Liliana","Mora","3033"},
            {"Felipe","Rojas","3034"},{"Paula","Navarro","3035"},{"Juan","Herrera","3036"},
            {"Carolina","Jiménez","3037"},{"Alberto","Acosta","3038"},{"Natalia","Suárez","3039"},
            {"Gustavo","Moreno","3040"},{"Andrea","Paredes","3041"},{"Víctor","Guerrero","3042"},
            {"Lorena","Delgado","3043"},{"Héctor","Salazar","3044"},{"Rosa","Bautista","3045"},
            {"Mario","Espinoza","3046"},{"Diana","Cárdenas","3047"},{"Óscar","Castillo","3048"},
            {"Verónica","Peralta","3049"},{"Roberto","Maldonado","3050"},{"Katherine","Guzmán","3051"},
            {"Eduardo","Velasco","3052"},{"Mónica","Montero","3053"},{"Christian","Quintero","3054"},
            {"Tatiana","Rendón","3055"},{"Jorge","Botero","3056"},{"Adriana","Cifuentes","3057"},
            {"Pablo","Duarte","3058"},{"Alejandra","Pineda","3059"}
        };
        List<Cliente> todosClientes = new ArrayList<>();
        for (String[] cd : clientesData) {
            todosClientes.add(clienteRepository.save(Cliente.builder()
                    .empresaId(empresaId).nombres(cd[0]).apellidos(cd[1]).dni(cd[2])
                    .telefono("3" + String.format("%07d", 1000000 + todosClientes.size()))
                    .build()));
        }
        log.info("{} clientes creados", todosClientes.size());

        // ── Proveedores ──
        List<Proveedor> proveedores = List.of(
                crearProveedor(empresaId, "Distribuidora La Esperanza", "Calle 10 #20-30"),
                crearProveedor(empresaId, "Carnes Premium S.A.", "Av. 45 #12-08"),
                crearProveedor(empresaId, "Abarrotes El Sol", "Carrera 8 #34-12")
        );
        log.info("{} proveedores creados", proveedores.size());

        // ── Plan de compras: {diasAtras, {{nombreIng, cantidad, idxProveedor}}}
        Object[][] comprasPlan = {
            {5, new Object[][]{{"Pan de perro caliente", new BigDecimal("25"), 2}, {"Salchicha para hot dog", new BigDecimal("20"), 2}, {"Salsa de tomate", new BigDecimal("2"), 2}, {"Pan de hamburguesa", new BigDecimal("30"), 2}, {"Carne de res molida", new BigDecimal("5"), 2}}},
            {5, new Object[][]{{"Pan de perro caliente", new BigDecimal("20"), 0}, {"Salchicha para hot dog", new BigDecimal("20"), 0}, {"Queso americano", new BigDecimal("20"), 0}}},
            {4, new Object[][]{{"Pan de perro caliente", new BigDecimal("15"), 0}, {"Salchicha para hot dog", new BigDecimal("15"), 0}, {"Mostaza", new BigDecimal("1"), 0}, {"Tocino", new BigDecimal("1"), 0}, {"Papas congeladas", new BigDecimal("5"), 0}}},
            {3, new Object[][]{{"Pan de hamburguesa", new BigDecimal("25"), 1}, {"Carne de res molida", new BigDecimal("4"), 1}, {"Lechuga", new BigDecimal("2"), 1}, {"Tomate", new BigDecimal("2"), 1}, {"Pechuga de pollo", new BigDecimal("3"), 1}}},
            {2, new Object[][]{{"Pan de perro caliente", new BigDecimal("25"), 1}, {"Salchicha para hot dog", new BigDecimal("25"), 1}, {"Cebolla", new BigDecimal("5"), 1}, {"Mayonesa", new BigDecimal("1"), 1}}},
            {1, new Object[][]{{"Pan de perro caliente", new BigDecimal("20"), 2}, {"Salchicha para hot dog", new BigDecimal("20"), 2}, {"Pan de hamburguesa", new BigDecimal("20"), 2}, {"Carne de res molida", new BigDecimal("3"), 2}, {"Queso americano", new BigDecimal("15"), 2}}}
        };

        // ── Bucle principal: procesar cada día ──
        Random rnd = new Random(42);
        int numFactura = 1;

        for (int diaIdx = 0; diaIdx < 5; diaIdx++) {
            int diasAtras = 5 - diaIdx;
            LocalDate dia = LocalDate.now().minusDays(diasAtras);
            log.info("--- Día {} ({}) ---", diasAtras, dia);

            // Snapshot stock inicial
            Map<Long, BigDecimal> stockInicial = new HashMap<>();
            for (Ingrediente ing : ingPorId.values()) {
                stockInicial.put(ing.getId(), ing.getStockActual());
            }

            // ── Compras del día ──
            BigDecimal comprasValorDia = BigDecimal.ZERO;
            for (Object[] plan : comprasPlan) {
                if ((int) plan[0] != diasAtras) continue;
                Object[][] items = (Object[][]) plan[1];

                Compra compra = compraRepository.save(Compra.builder()
                        .empresaId(empresaId).usuarioId(adminId)
                        .proveedorId(proveedores.get((int) items[0][2]).getId())
                        .fechaCompra(LocalDateTime.of(dia, LocalTime.of(8, 0)))
                        .numeroFactura("FAC-00" + (numFactura++)).build());

                BigDecimal subtotalCompra = BigDecimal.ZERO;
                for (Object[] item : items) {
                    String ingNombre = (String) item[0];
                    BigDecimal cant = (BigDecimal) item[1];
                    Ingrediente ing = ingPorNombre.get(ingNombre);
                    BigDecimal pu = ing.getPrecioCompra() != null ? ing.getPrecioCompra() : BigDecimal.ZERO;
                    BigDecimal sub = cant.multiply(pu).setScale(2, RoundingMode.HALF_UP);
                    subtotalCompra = subtotalCompra.add(sub);

                    compraDetalleRepository.save(CompraDetalle.builder()
                            .compraId(compra.getId()).itemTipo(TipoItemCompra.INGREDIENTE)
                            .itemId(ing.getId()).itemNombre(ingNombre)
                            .cantidad(cant).precioUnitario(pu).subtotal(sub).build());

                    BigDecimal stockAntes = ing.getStockActual() != null ? ing.getStockActual() : BigDecimal.ZERO;
                    ing.setStockActual(stockAntes.add(cant));
                    ingredienteRepository.save(ing);
                    crearMovimiento(empresaId, adminId, "INGREDIENTE", ing.getId(), ingNombre,
                            MovimientoTipo.COMPRA, compra.getId(), cant, "+", stockAntes, ing.getStockActual(),
                            LocalDateTime.of(dia, LocalTime.of(8, 0)), null);
                }
                compra.setSubtotal(subtotalCompra);
                compra.setTotal(subtotalCompra);
                compra.setTipo("INGREDIENTES");
                compraRepository.save(compra);
                comprasValorDia = comprasValorDia.add(subtotalCompra);
                log.info("  Compra {}: ${}", compra.getNumeroFactura(), subtotalCompra);
            }

            // ── Ventas del día (12-15 ventas variadas) ──
            BigDecimal totalVentasDia = BigDecimal.ZERO;
            int cantidadVentasDia = 0;
            BigDecimal totalEfectivoDia = BigDecimal.ZERO;
            BigDecimal totalTransferenciaDia = BigDecimal.ZERO;
            Map<Long, BigDecimal> consumoPorIng = new HashMap<>();
            Map<Long, Integer> consumoProductoSimple = new HashMap<>();

            int numVentasDia = 12 + rnd.nextInt(4);

            for (int v = 0; v < numVentasDia; v++) {
                int hora = 11 + rnd.nextInt(10);
                int minuto = rnd.nextInt(60);

                MetodoPago metodo;
                if (v < 7) metodo = MetodoPago.EFECTIVO;
                else if (v < 11) metodo = MetodoPago.TRANSFERENCIA;
                else metodo = MetodoPago.MIXTO;

                boolean paraLlevar = rnd.nextDouble() < 0.3;
                Long clienteId = todosClientes.get((diaIdx * numVentasDia + v) % todosClientes.size()).getId();
                LocalDateTime fechaVenta = LocalDateTime.of(dia, LocalTime.of(hora, minuto));

                // Seleccionar productos aleatorios para esta venta (1-3 items)
                int numItems = 1 + rnd.nextInt(3);
                BigDecimal totalLinea = BigDecimal.ZERO;
                List<VentaDetalle> detallesVenta = new ArrayList<>();

                for (int item = 0; item < numItems; item++) {
                    Producto prod;
                    BigDecimal cantidad;
                    BigDecimal precioUnitario;

                    // 70% productos con receta, 30% productos simples
                    if (rnd.nextDouble() < 0.7 && !productosConReceta.isEmpty()) {
                        prod = productosConReceta.get(rnd.nextInt(productosConReceta.size()));
                        cantidad = new BigDecimal(1 + rnd.nextInt(3));
                    } else if (!productosSimples.isEmpty()) {
                        prod = productosSimples.get(rnd.nextInt(productosSimples.size()));
                        cantidad = new BigDecimal(1 + rnd.nextInt(2));
                    } else {
                        prod = productosConReceta.get(rnd.nextInt(productosConReceta.size()));
                        cantidad = new BigDecimal(1 + rnd.nextInt(3));
                    }
                    precioUnitario = prod.getPrecioVenta();

                    BigDecimal subtotalItem = precioUnitario.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
                    totalLinea = totalLinea.add(subtotalItem);

                    VentaDetalle vd = VentaDetalle.builder()
                            .productoId(prod.getId())
                            .productoNombre(prod.getNombre())
                            .cantidad(cantidad)
                            .precioUnitario(precioUnitario)
                            .subtotal(subtotalItem)
                            .build();
                    detallesVenta.add(vd);

                    // Descontar stock
                    if (Boolean.TRUE.equals(prod.getTieneReceta())) {
                        List<RecetaDetalle> recetaDetalles = recetasPorProducto.getOrDefault(prod.getId(), Collections.emptyList());
                        for (RecetaDetalle rd : recetaDetalles) {
                            Ingrediente ing = ingPorId.get(rd.getIngredienteId());
                            if (ing == null) continue;
                            BigDecimal qty = rd.getCantidad().multiply(cantidad);
                            BigDecimal stockAntes = ing.getStockActual() != null ? ing.getStockActual() : BigDecimal.ZERO;
                            ing.setStockActual(stockAntes.subtract(qty));
                            if (ing.getStockActual().compareTo(BigDecimal.ZERO) < 0) ing.setStockActual(BigDecimal.ZERO);
                            ingredienteRepository.save(ing);
                            consumoPorIng.merge(ing.getId(), qty, BigDecimal::add);
                        }
                    } else {
                        BigDecimal stockAntes = prod.getStockActual() != null ? prod.getStockActual() : BigDecimal.ZERO;
                        prod.setStockActual(stockAntes.subtract(cantidad));
                        if (prod.getStockActual().compareTo(BigDecimal.ZERO) < 0) prod.setStockActual(BigDecimal.ZERO);
                        productoRepository.save(prod);
                        consumoProductoSimple.merge(prod.getId(), cantidad.intValue(), Integer::sum);
                    }
                }

                // Calcular pagos
                BigDecimal efectivo = BigDecimal.ZERO;
                BigDecimal transferencia = BigDecimal.ZERO;
                BigDecimal cambio = BigDecimal.ZERO;
                if (metodo == MetodoPago.EFECTIVO) {
                    efectivo = totalLinea.add(new BigDecimal(500 + rnd.nextInt(2000)));
                    cambio = efectivo.subtract(totalLinea);
                } else if (metodo == MetodoPago.TRANSFERENCIA) {
                    transferencia = totalLinea;
                } else {
                    efectivo = totalLinea.multiply(new BigDecimal("0.6")).setScale(0, RoundingMode.HALF_UP)
                            .add(new BigDecimal(500 + rnd.nextInt(1000)));
                    transferencia = totalLinea.subtract(
                            totalLinea.multiply(new BigDecimal("0.6")).setScale(0, RoundingMode.HALF_UP));
                    cambio = efectivo.add(transferencia).subtract(totalLinea);
                    if (cambio.compareTo(BigDecimal.ZERO) < 0) cambio = BigDecimal.ZERO;
                }

                Venta venta = ventaRepository.save(Venta.builder()
                        .empresaId(empresaId).usuarioId(cajeroId).clienteId(clienteId)
                        .tipoVenta(TipoVenta.BARRA).fechaVenta(fechaVenta)
                        .subtotal(totalLinea).total(totalLinea).metodoPago(metodo)
                        .recibidoEfectivo(efectivo).recibidoTransferencia(transferencia).cambio(cambio)
                        .paraLlevar(paraLlevar).build());

                for (VentaDetalle vd : detallesVenta) {
                    vd.setVentaId(venta.getId());
                    ventaDetalleRepository.save(vd);
                }

                // Registrar movimientos de inventario para ingredientes
                if (!detallesVenta.isEmpty()) {
                    for (Map.Entry<Long, BigDecimal> entry : consumoPorIng.entrySet()) {
                        Ingrediente ing = ingPorId.get(entry.getKey());
                        if (ing == null) continue;
                        crearMovimiento(empresaId, cajeroId, "INGREDIENTE", ing.getId(), ing.getNombre(),
                                MovimientoTipo.VENTA, venta.getId(), entry.getValue(), "-", 
                                ing.getStockActual(), ing.getStockActual(),
                                fechaVenta, null);
                    }
                }

                totalVentasDia = totalVentasDia.add(totalLinea);
                cantidadVentasDia++;
                if (metodo == MetodoPago.EFECTIVO || metodo == MetodoPago.MIXTO)
                    totalEfectivoDia = totalEfectivoDia.add(efectivo.subtract(cambio));
                if (metodo == MetodoPago.TRANSFERENCIA || metodo == MetodoPago.MIXTO)
                    totalTransferenciaDia = totalTransferenciaDia.add(transferencia);
            }

            log.info("  Ventas {}: ${} ({} ventas, efectivo ${}, transf ${})",
                    dia, totalVentasDia, cantidadVentasDia, totalEfectivoDia, totalTransferenciaDia);

            // ── Cierre Z ──
            cierreZRepository.save(CierreZ.builder()
                    .empresaId(empresaId).usuarioId(cajeroId).fecha(dia)
                    .totalVentas(totalVentasDia).cantidadVentas(cantidadVentasDia)
                    .totalEfectivo(totalEfectivoDia).totalTransferencia(totalTransferenciaDia).build());
            log.info("  CierreZ creado");

            // ── Costo Comida Diaria ──
            Map<Long, BigDecimal> stockFinal = new HashMap<>();
            for (Ingrediente ing : ingPorId.values()) {
                stockFinal.put(ing.getId(), ing.getStockActual() != null ? ing.getStockActual() : BigDecimal.ZERO);
            }

            BigDecimal invInicialValor = BigDecimal.ZERO;
            BigDecimal invFinalValor = BigDecimal.ZERO;
            BigDecimal costoSinIVA = BigDecimal.ZERO;

            for (Ingrediente ing : ingPorId.values()) {
                BigDecimal pc = ing.getPrecioCompra() != null ? ing.getPrecioCompra() : BigDecimal.ZERO;
                invInicialValor = invInicialValor.add(stockInicial.getOrDefault(ing.getId(), BigDecimal.ZERO).multiply(pc));
                invFinalValor = invFinalValor.add(stockFinal.getOrDefault(ing.getId(), BigDecimal.ZERO).multiply(pc));
            }

            BigDecimal costoAlimentosContable = invInicialValor.add(comprasValorDia).subtract(invFinalValor);
            if (costoAlimentosContable.compareTo(BigDecimal.ZERO) < 0) costoAlimentosContable = BigDecimal.ZERO;

            for (Map.Entry<Long, BigDecimal> entry : consumoPorIng.entrySet()) {
                Ingrediente ing = ingPorId.get(entry.getKey());
                if (ing == null) continue;
                BigDecimal pc = ing.getPrecioCompra() != null ? ing.getPrecioCompra() : BigDecimal.ZERO;
                costoSinIVA = costoSinIVA.add(entry.getValue().multiply(pc));
            }

            BigDecimal foodCostPct = totalVentasDia.compareTo(BigDecimal.ZERO) > 0
                    ? costoSinIVA.multiply(new BigDecimal("100")).divide(totalVentasDia, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal foodCostContablePct = totalVentasDia.compareTo(BigDecimal.ZERO) > 0
                    ? costoAlimentosContable.multiply(new BigDecimal("100")).divide(totalVentasDia, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal diferenciaValor = costoSinIVA.subtract(costoAlimentosContable);
            BigDecimal diferenciaPct = totalVentasDia.compareTo(BigDecimal.ZERO) > 0
                    ? diferenciaValor.multiply(new BigDecimal("100")).divide(totalVentasDia, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            CostoComidaDiaria ccd = costoComidaDiariaRepository.save(CostoComidaDiaria.builder()
                    .empresaId(empresaId).fecha(dia)
                    .ventasTotales(totalVentasDia)
                    .costoIngredientesVendidos(costoSinIVA)
                    .foodCostPorcentaje(foodCostPct)
                    .inventarioInicialValor(invInicialValor)
                    .comprasValor(comprasValorDia)
                    .inventarioFinalValor(invFinalValor)
                    .costoAlimentosContable(costoAlimentosContable)
                    .foodCostContablePorcentaje(foodCostContablePct)
                    .diferenciaValor(diferenciaValor)
                    .diferenciaPorcentaje(diferenciaPct)
                    .mermaValor(BigDecimal.ZERO)
                    .mermaPorcentaje(BigDecimal.ZERO)
                    .desperdicioValor(BigDecimal.ZERO)
                    .desperdicioPorcentaje(BigDecimal.ZERO)
                    .consumoIndirectoValor(BigDecimal.ZERO)
                    .costoRealValor(costoSinIVA)
                    .costoRealPorcentaje(foodCostPct)
                    .build());

            for (Map.Entry<Long, BigDecimal> entry : consumoPorIng.entrySet()) {
                Ingrediente ing = ingPorId.get(entry.getKey());
                if (ing == null) continue;
                BigDecimal pc = ing.getPrecioCompra() != null ? ing.getPrecioCompra() : BigDecimal.ZERO;
                BigDecimal costoGrupo = entry.getValue().multiply(pc);
                BigDecimal pctItem = totalVentasDia.compareTo(BigDecimal.ZERO) > 0
                        ? costoGrupo.multiply(new BigDecimal("100")).divide(totalVentasDia, 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                costoComidaDiariaItemRepository.save(CostoComidaDiariaItem.builder()
                        .costoComidaDiaria(ccd)
                        .itemId(ing.getId()).itemNombre(ing.getNombre()).itemTipo("INGREDIENTE")
                        .unidadMedida(ing.getUnidadMedida().name())
                        .cantidadConsumida(entry.getValue())
                        .precioCostoUnitario(pc)
                        .costoGrupo(costoGrupo)
                        .porcentajeDelCosto(pctItem)
                        .build());
            }

            log.info("  Food Cost: ${} ({}%) — Contable: ${} ({}%)",
                    costoSinIVA, foodCostPct, costoAlimentosContable, foodCostContablePct);
        }

        log.info("=== Datos operativos de 5 días completados ===");
    }

    private Proveedor crearProveedor(Long empresaId, String razonSocial, String direccion) {
        return proveedorRepository.save(Proveedor.builder()
                .empresaId(empresaId).razonSocial(razonSocial).direccion(direccion)
                .contacto("Contacto " + razonSocial).build());
    }

    private void crearMovimiento(Long empresaId, Long usuarioId, String itemTipo, Long itemId, String itemNombre,
                                  MovimientoTipo movTipo, Long referenciaId, BigDecimal cantidad, String signo,
                                  BigDecimal stockAnterior, BigDecimal stockPosterior,
                                  LocalDateTime fechaMov, String observacion) {
        movimientoInventarioRepository.save(MovimientoInventario.builder()
                .empresaId(empresaId).usuarioId(usuarioId)
                .itemTipo(itemTipo).itemId(itemId).itemNombre(itemNombre)
                .movimientoTipo(movTipo).referenciaId(referenciaId)
                .cantidad(cantidad).signo(signo)
                .stockAnterior(stockAnterior).stockPosterior(stockPosterior)
                .fechaMovimiento(fechaMov != null ? fechaMov : LocalDateTime.now())
                .observacion(observacion).build());
    }
}
