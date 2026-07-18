# Mi Bombay — Sistema de Gestión para Restaurantes

> Plataforma integral multi-tenant para la gestión operativa de restaurantes: punto de venta, inventario, compras, food cost, dashboard en tiempo real y reportes financieros.

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.14-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Proprietary-blue?style=for-the-badge)](LICENSE)
[![Status](https://img.shields.io/badge/Status-Producción-success?style=for-the-badge)](https://github.com/josegregoppdev/mibombay)

</div>

---

## Tabla de Contenidos

- [Demo en Vivo](#-demo-en-vivo)
- [Características](#-características)
- [Stack Tecnológico](#-stack-tecnológico)
- [Módulos del Sistema](#-módulos-del-sistema)
- [Arquitectura](#-arquitectura)
- [Inicio Rápido](#-inicio-rápido)
- [Configuración](#-configuración)
- [Estructura del Proyecto](#-estructura-del-proyecto)
- [Reglas de Negocio Clave](#-reglas-de-negocio-clave)
- [Seguridad (OWASP)](#-seguridad-owasp)
- [Testing](#-testing)
- [Comandos Útiles](#-comandos-útiles)
- [Equipo](#-equipo)

---

## 🚀 Demo en Vivo

Accede a la demo con credenciales preconfiguradas:

| Campo | Valor |
|-------|-------|
| **Código de empresa** | `DEMO001` |
| **Usuario** | `demoadmin` |
| **Contraseña** | `demoadmin123` |

> 💡 También hay un usuario cajero disponible: `cajero` / `cajero123`
> La demo incluye 5 días de datos operativos (compras, ventas, cierres Z, food cost).

---

## ✨ Características

### Gestión Operativa
- 🍔 **Punto de Venta (POS)** con modificadores, adicionales, exclusiones de ingredientes y multi-tab de órdenes
- 🛒 **Compras mixtas** (ingredientes + productos) con actualización automática de stock
- 📦 **Inventario físico** con merma, desperdicio y diferencia inexplicada
- 💰 **Food Cost** diario con desglose por item y 3 gauges de rentabilidad
- 📊 **Reporte de Consumo** con 14 columnas y exportación a Excel
- 🔄 **Movimientos de inventario** con trazabilidad completa

### Multi-Tenant & Seguridad
- 🏢 **Multi-empresa** con aislamiento total de datos por `empresaId`
- 🔐 **Spring Security** con BCrypt y filtros personalizados
- 👥 **Roles**: `ADMIN`, `CAJERO`, `DEV` + `SUPERADMIN` global
- 🛡️ **OWASP A01** Broken Access Control: doble protección (URL + `@PreAuthorize` en service)
- 🎨 **Personalización visual** por empresa (colores, tipografía, tema)

### Reportes y Exportación
- 📄 **Reporte X** y **Cierre Z** en PDF (OpenHTMLtoPDF)
- 💵 **Cuadre de Caja** con denominaciones COP + PDF + historial
- 📈 **Exportación Excel** de inventarios, consumos y food cost
- 🔔 **Dashboard en tiempo real** con WebSocket + STOMP

### Experiencia de Usuario
- 📱 **Diseño responsive** (mobile-first, breakpoints en 768px y 1024px)
- 🎯 **POS drawer** en móvil con backdrop y gestos
- 🔍 **Filtros avanzados** con paginación preservada
- 💬 **Notificaciones toast** para ventas en tiempo real

---

## 🛠 Stack Tecnológico

### Backend
| Tecnología | Versión | Uso |
|------------|---------|-----|
| Java | 21 | Lenguaje principal |
| Spring Boot | 3.5.14 | Framework |
| Spring Security | 6.x | Autenticación y autorización |
| Spring Data JPA | 3.x | Persistencia |
| Hibernate | 6.x | ORM |
| Spring WebSocket | 6.x | Notificaciones en tiempo real |
| MapStruct | 1.6.3 | Mapeo Entity ↔ DTO |
| Apache POI | 5.4 | Exportación Excel |
| OpenHTMLtoPDF | 1.1.37 | Generación de PDFs |
| SpringDoc OpenAPI | 2.8.6 | Documentación Swagger |

### Frontend
| Tecnología | Versión | Uso |
|------------|---------|-----|
| Thymeleaf | 3.x | Motor de plantillas server-side |
| Bootstrap | 5.x | Framework CSS |
| JavaScript | ES6+ | Interactividad |
| SockJS + STOMP | — | Cliente WebSocket |
| Inter + Instrument Serif | — | Tipografías |

### Base de Datos
| Tecnología | Versión | Uso |
|------------|---------|-----|
| MySQL | 8.0+ | Persistencia principal |

### Herramientas
| Tecnología | Uso |
|------------|-----|
| Lombok | Reducción de boilerplate |
| Maven | Gestión de dependencias |
| JUnit 5 + Mockito | Testing |
| spring-security-test | Tests con `@WithMockUser` |

---

## 📦 Módulos del Sistema

**27 módulos completados** de 29 planeados (98%):

### Núcleo
1. ✅ Configuración inicial (Spring Boot, MySQL, multi-tenant)
2. ✅ Multi-tenant (TenantContext, TenantFilter, TenantInterceptor)
3. ✅ Seguridad (login empresa + login superadmin separados)
4. ✅ Landing page + registro público de empresas
5. ✅ CRUD Empresas (superadmin)
6. ✅ Dashboard admin con sidebar
7. ✅ Personalización visual (CSS variables por empresa)
8. ✅ CRUD Usuarios con roles (ADMIN, CAJERO, DEV)

### Gestión
9. ✅ CRUD Ingredientes (con flag `consumible`)
10. ✅ CRUD Productos + Recetas (stock calculado, margen)
11. ✅ CRUD Clientes (Consumidor Final por defecto)
12. ✅ CRUD Proveedores (Proveedor Genérico por defecto)
13. ✅ Módulo de Compras (tipo MIXTO, anulación con reversión)
14. ✅ Movimientos de Inventario (trazabilidad completa)
15. ✅ Inventario Actual (vista combinada)
16. ✅ Ventas POS (multi-método pago, modificaciones)
17. ✅ VentaSuspendida — Tabs de órdenes en espera

### Reportes
18. ✅ Cierre Z + Reporte X (PDF)
19. ✅ Cuadre de Caja (vista, persistencia, PDF, historial)
20. ✅ PDF generation con OpenHTMLtoPDF
21. ✅ Inventario físico + merma/desperdicio + Excel
22. ✅ Reporte de Consumo (14 columnas) + Excel
23. ✅ Food Cost Diario (3 gauges) + Excel
24. ✅ Consumo Indirecto (consumibles semanal/mensual)

### UX y Tiempo Real
25. ✅ Dashboard con estadísticas + WebSocket
26. ✅ Responsive design (sidebar mobile, POS drawer)
27. ✅ Seguridad OWASP A01 (auditoría + `@PreAuthorize` en services)

---

## 🏗 Arquitectura

### Patrón MVC Multi-Tenant

```
┌─────────────────────────────────────────┐
│         Empresa Tenant (N)              │
│                                         │
│  ┌──────────────────────────────────┐  │
│  │  Controllers (Thymeleaf/REST)    │  │
│  │  + @PreAuthorize por método      │  │
│  └──────────────┬───────────────────┘  │
│                 ↓                       │
│  ┌──────────────────────────────────┐  │
│  │  Services (@Transactional)       │  │
│  │  + @PreAuthorize doble check     │  │
│  └──────────────┬───────────────────┘  │
│                 ↓                       │
│  ┌──────────────────────────────────┐  │
│  │  Repositories (JPA)              │  │
│  │  + findByIdAndEmpresaId          │  │
│  └──────────────┬───────────────────┘  │
│                 ↓                       │
│  ┌──────────────────────────────────┐  │
│  │  MySQL (filtrado por empresaId)  │  │
│  └──────────────────────────────────┘  │
│                                         │
│  TenantContext (ThreadLocal)            │
│  TenantFilter (inyecta empresaId)       │
│  TenantInterceptor (valida empresa)     │
└─────────────────────────────────────────┘
```

### Defensa en Profundidad

La seguridad se aplica en **3 capas**:

1. **URL (SecurityConfig)**: Filtra por patrón de URL y rol
2. **Controller (`@PreAuthorize`)**: Verifica el rol antes de invocar el método
3. **Service (`@PreAuthorize`)**: Doble check a nivel de bean

### Roles y Seguridad

```
SUPERADMIN → Acceso global a todas las empresas
   ↓
ADMIN → Gestión completa dentro de su empresa
   ↓
CAJERO → POS, ventas, consultas limitadas
   ↓
DEV → Acceso técnico (logs, debug)
```

---

## 🚀 Inicio Rápido

### Requisitos Previos

- ☕ Java 21+
- 🗄 MySQL 8.0+
- 📦 Maven 3.9+ (o usar el wrapper incluido)

### Instalación

```bash
# 1. Clonar el repositorio
git clone https://github.com/josegregoppdev/mibombay.git
cd mibombay

# 2. Crear base de datos
mysql -u root -p
CREATE DATABASE mibombay CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EXIT;

# 3. Configurar variables de entorno (ver sección Configuración)
#    Agregar a tu ~/.bashrc:
#      export DB_URL="jdbc:mysql://localhost:3306/mibombay..."
#      export DB_USER=root
#      export DB_PASSWORD=tu_password
#      export SPRING_PROFILES_ACTIVE=dev
source ~/.bashrc

# 4. Compilar y ejecutar
./mvnw spring-boot:run
```

### Primer Acceso

1. Abre http://localhost:8080
2. Click en "Registrar empresa" para crear tu primera empresa
3. O usa las credenciales demo si importaste datos de ejemplo

---

## ⚙ Configuración

### Estructura de archivos `.properties`

```
src/main/resources/
├── application.properties                  (base/compartida, sin secretos)
├── application-dev.properties              (dev, lee de env vars)
├── application-prod.properties             (prod, sin defaults, todo via env vars)
└── application-local.properties.example    (plantilla para secretos, gitignored)
```

### Variables de Entorno

Las credenciales NO se incluyen en el repositorio. Configúralas como variables de entorno:

```bash
# Requeridas
export DB_URL="jdbc:mysql://localhost:3306/mibombay?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Guayaquil"
export DB_USER=root
export DB_PASSWORD=tu_password_real

# Recomendadas
export SPRING_PROFILES_ACTIVE=dev
export SERVER_PORT=8080
```

#### En Linux (permanente)

Edita `~/.bashrc` y agrega las variables, luego:

```bash
source ~/.bashrc
```

#### En Windows (permanente)

`Sistema > Configuración avanzada > Variables de entorno`

#### Solo para una sesión

```bash
DB_URL="jdbc:mysql://localhost:3306/mibombay" \
DB_USER=root \
DB_PASSWORD=tu_password \
SPRING_PROFILES_ACTIVE=dev \
./mvnw spring-boot:run
```

### Jerarquía de carga

Spring Boot busca los valores en este orden (de mayor a menor prioridad):

1. **Argumentos de línea de comandos**: `--DB_PASSWORD=xxx`
2. **Variables de entorno**: `export DB_PASSWORD=xxx`
3. **`application-{profile}.properties`**: archivo específico del perfil
4. **`application.properties`**: archivo base

### Secretos personales (gitignored)

Para overrides personales sin afectar al repo:

```bash
# Copiar plantilla
cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties

# Editar con tus secretos
nano src/main/resources/application-local.properties
```

---

## 📁 Estructura del Proyecto

```
sistemaresurante/
├── src/main/java/com/mibombay/sistemaresurante/
│   ├── config/              # Configuraciones (Security, Web, WebSocket)
│   ├── controllers/         # Controladores MVC y REST
│   ├── DTO/                 # Data Transfer Objects
│   ├── exceptions/          # Excepciones personalizadas + GlobalExceptionHandler
│   ├── mapper/              # Mappers MapStruct (Entity ↔ DTO)
│   ├── models/              # Entidades JPA
│   │   └── enums/           # Enumeraciones de modelo
│   ├── repositories/        # Repositorios JPA + Specifications
│   ├── security/            # Filtros y providers de seguridad
│   ├── services/            # Lógica de negocio (con @PreAuthorize)
│   └── tenant/              # Multi-tenant (Context, Filter, Interceptor)
├── src/main/resources/
│   ├── static/              # CSS, JS, imágenes
│   ├── templates/           # Vistas Thymeleaf
│   └── application*.properties
├── src/test/java/com/mibombay/sistemaresurante/
│   ├── security/owasp/      # Tests de seguridad OWASP A01
│   │   ├── UsuarioServiceSecurityTest.java
│   │   ├── MovimientoInventarioServiceSecurityTest.java
│   │   ├── EstiloConfiguracionServiceSecurityTest.java
│   │   ├── ReporteServiceSecurityTest.java
│   │   └── FoodCostServiceSecurityTest.java
│   └── SistemaresuranteApplicationTests.java
├── pom.xml
├── mvnw, mvnw.cmd
└── README.md
```

---

## 🛡 Seguridad (OWASP)

### OWASP A01: Broken Access Control (En progreso)

Auditoría de doble protección: cada endpoint tiene filtro por URL **Y** `@PreAuthorize` en controller/service.

#### Servicios protegidos con `@PreAuthorize`

| Servicio | Métodos | Rol |
|----------|---------|-----|
| `UsuarioService` | 8 | ADMIN |
| `MovimientoInventarioService` | 1 | ADMIN |
| `EstiloConfiguracionService` | 4 | ADMIN |
| `ReporteService` | 1 | ADMIN |
| `FoodCostService` | 10 | ADMIN |

#### Tests de seguridad (22 tests)

```bash
./mvnw test -Dtest="*ServiceSecurityTest"
```

Cada test verifica:
- ✅ ADMIN tiene acceso
- ❌ CAJERO es rechazado (`AccessDeniedException`)
- ❌ Sin autenticación es rechazado

### Reporte de hallazgos

La auditoría completa con gaps y fixes pendientes está en `docs/security-audit.md` (no versionado por contener información sensible).

---

## 🧪 Testing

### Estructura

```
src/test/java/com/mibombay/sistemaresurante/
└── security/owasp/        # Tests de seguridad (OWASP A01)
```

### Comandos

```bash
# Ejecutar todos los tests
./mvnw test

# Solo tests de seguridad
./mvnw test -Dtest="*ServiceSecurityTest"

# Test específico
./mvnw test -Dtest=UsuarioServiceSecurityTest

# Con reporte de cobertura
./mvnw test jacoco:report
```

### Pendiente de testing

- [ ] Tests unitarios de services críticos (Venta, Compra, Inventario)
- [ ] Tests de controllers (MockMvc)
- [ ] Tests de integración con H2
- [ ] Meta: 60% de cobertura en services

---

## 💼 Reglas de Negocio Clave

### Productos y Recetas
- 🍔 Producto **con receta** → stock = `min(stock_ingrediente / cantidad_receta)`, precioCompra = 0
- 🥤 Producto **sin receta** → stock manual (`stockActual`), precioCompra directo
- 📋 Una receta por producto (relación 1:1)
- ⚠️ Ingrediente no se elimina si está en alguna receta

### Compras
- 💵 Subtotal e IVA (19%) calculados en el service
- 📦 Al crear/editar compra, se actualiza `stockActual`
- 🔄 Al anular compra, se revierte el stock
- 📝 Cada cambio genera un `MovimientoInventario`

### Ventas
- 🛒 Bloquea ventas si no hay stock suficiente
- 📅 Requiere día sin cierre Z
- 💰 Métodos de pago: `EFECTIVO`, `TRANSFERENCIA`, `MIXTO`
- 🖊️ Modificadores y adicionales se persisten por línea
- 🔄 Anulación revierte stock y crea `VENTA_ANULACION`

### Inventario Físico
- 📊 Solo ADMIN puede crear/confirmar inventarios
- 🚫 Bloquea si ya existe CONFIRMADO hoy
- ✅ Confirmación en 4 pasos: merma → desperdicio → diferencia → stock final

### Food Cost
- 🍽️ **Food Cost Global** = Costo Ingredientes Vendidos / Ventas × 100
- 📊 **Costo Contable** = Vendido + Merma + Desperdicio
- 💯 **Costo Real** = Contable + Diferencia de inventario

---

## 🔧 Comandos Útiles

### Desarrollo

```bash
# Compilar
./mvnw compile

# Ejecutar con perfil dev (usando env vars)
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run

# O con argumento
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Ejecutar con perfil prod
SPRING_PROFILES_ACTIVE=prod DB_URL=... DB_USER=... DB_PASSWORD=... \
  ./mvnw spring-boot:run

# Build sin tests
./mvnw clean install -DskipTests
```

### Testing

```bash
# Todos los tests
./mvnw test

# Solo tests de seguridad OWASP
./mvnw test -Dtest="*ServiceSecurityTest"

# Con cobertura
./mvnw test jacoco:report
```

### Git

```bash
# Ver estado
git status

# Trabajar en rama de feature
git checkout -b feature/nombre-rama

# Commit y push
git add .
git commit -m "descripcion del cambio"
git push origin nombre-rama

# Crear Pull Request en GitHub
```

### Base de Datos

```bash
# Conectar a MySQL
mysql -u root -p mibombay

# Backup
mysqldump -u root -p mibombay > backup.sql

# Restaurar
mysql -u root -p mibombay < backup.sql
```

---

## 🤝 Equipo

**Desarrollador Principal**
- GitHub: [@josegregoppdev](https://github.com/josegregoppdev)

---

## 📄 Licencia

Este proyecto es **software propietario**. Todos los derechos reservados.

Uso no autorizado, copia o distribución sin permiso expreso está prohibido.

---

## 🙏 Agradecimientos

- Spring Boot team por el excelente framework
- Comunidad open source de Java
- OWASP Foundation por la guía de seguridad

---

<div align="center">

**⭐ Si este proyecto te resulta útil, considera darle una estrella en GitHub ⭐**

Hecho con ❤️ y mucho ☕

</div>
