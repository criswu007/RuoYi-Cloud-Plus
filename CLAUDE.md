# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**RuoYi-Cloud-Plus** is an enterprise-grade Spring Cloud microservices framework with multi-tenancy, distributed transactions, and comprehensive business modules. Version: 2.5.3, JDK: 17/21, Spring Boot: 3.5.

Key technologies: Spring Cloud, Dubbo 3.X, Nacos, Seata, Sentinel, MyBatis-Plus, Redis/Redisson, Sa-Token, SnailJob, Warm-Flow.

## Project Structure

### Core Services
- **ruoyi-auth**: Authentication service (port 9210)
- **ruoyi-gateway**: API Gateway (port 8080) - Spring Cloud Gateway with custom extensions
- **ruoyi-modules/**: Business modules
  - `ruoyi-system`: Core system module (port 9201) - user, role, menu, dept management
  - `ruoyi-gen`: Code generator (port 9202)
  - `ruoyi-job`: Scheduled tasks with SnailJob (port 9203)
  - `ruoyi-resource`: File/resource management (port 9204)
  - `ruoyi-workflow`: Warm-Flow workflow engine (port 9205)
  - `ruoyi-address`: Custom address management module

### Support Modules
- **ruoyi-common/**: 30+ common libraries organized as Spring Boot starters
  - Authentication: `ruoyi-common-satoken`, `ruoyi-common-security`
  - Data: `ruoyi-common-mybatis`, `ruoyi-common-redis`, `ruoyi-common-tenant`
  - Integration: `ruoyi-common-dubbo`, `ruoyi-common-seata`, `ruoyi-common-nacos`
  - Features: `ruoyi-common-translation`, `ruoyi-common-sensitive`, `ruoyi-common-encrypt`
  - Observability: `ruoyi-common-skylog`, `ruoyi-common-prometheus`, `ruoyi-common-logstash`

- **ruoyi-api/**: Dubbo RPC service interfaces organized by module
  - `ruoyi-api-system`, `ruoyi-api-resource`, `ruoyi-api-workflow`, `ruoyi-api-address`
  - `ruoyi-api-bom`: API dependency management

- **ruoyi-visual/**: Platform infrastructure
  - `ruoyi-nacos`: Nacos server (port 8848)
  - `ruoyi-seata-server`: Distributed transaction coordinator (port 8091)
  - `ruoyi-snailjob-server`: Job scheduling server (port 8800)
  - `ruoyi-monitor`: Spring Boot Admin monitoring (port 9100)

- **ruoyi-example/**: Demo modules for framework features

## Build & Run

### Maven Build Commands
```bash
# Build all modules (tests skipped by default)
mvn clean package

# Build with multiple threads (1 thread per CPU core)
mvn -T1C clean package

# Build specific service with dependencies
mvn -pl ruoyi-modules/ruoyi-system -am package

# Run service locally (requires Nacos running)
mvn -pl ruoyi-modules/ruoyi-system -am spring-boot:run

# Run tests explicitly (override default skip)
mvn -DskipTests=false test
```

### Maven Profiles
- **dev** (default): Development environment
- **prod**: Production environment

Configure Nacos address and credentials in root `pom.xml` under `<profiles>`.

### Local Development Setup
1. Start infrastructure with Docker: `cd script/docker && docker-compose up -d mysql nacos redis minio`
2. Import SQL: `script/sql/ry-cloud.sql` (main), `script/sql/ry-config.sql` (Nacos config)
3. Start services in order: Nacos → Gateway → Auth → System → Other modules
4. Access UI at http://localhost:80 (requires frontend setup, see README)

### Docker Deployment
Full stack deployment: `cd script/docker && docker-compose up -d`

Services include: MySQL, Nacos, Redis, MinIO, Seata, Nginx, all microservices

## Architecture Patterns

### Configuration Management
- **Nacos-based centralized config**: Each service loads from Nacos
- Service `application.yml` structure:
  ```yaml
  spring:
    application:
      name: ruoyi-{module}
    profiles:
      active: @profiles.active@  # Maven profile substitution
    cloud:
      nacos:
        server-addr: @nacos.server@
        discovery:
          group: @nacos.discovery.group@
        config:
          group: @nacos.config.group@
    config:
      import:
        - optional:nacos:application-common.yml    # Shared config
        - optional:nacos:datasource.yml            # DB config
        - optional:nacos:${spring.application.name}.yml  # Service-specific
  ```
- Nacos configs located in `script/config/nacos/`

### Service Communication
- **HTTP**: Through Gateway (Spring Cloud LoadBalancer)
- **RPC**: Dubbo 3.X for inter-service calls
  - API interfaces in `ruoyi-api/*`
  - Implementations with `@DubboService` in modules
  - Consumers use `@DubboReference`

### Data Access
- **MyBatis-Plus**: Primary ORM with plugins for pagination, multi-tenancy, data permissions
- **Dynamic DataSource**: Multi-database support via `@DS("dataSourceName")`
- **Entities**: Located in `domain/` packages
  - Bo (Business Object): Input DTOs
  - Vo (View Object): Output DTOs
  - Entity: Database entities with `@TableName`
- **Mapper**: XML files in `src/main/resources/mapper/{module}/`

### Authentication & Authorization
- **Sa-Token** for authentication (replaces Spring Security)
- Annotations: `@SaCheckPermission`, `@SaCheckRole`, `@SaIgnore`
- Token management via `StpUtil` utility class
- Multi-tenancy via `@TenantIgnore` to bypass tenant isolation

### Distributed Features
- **Transactions**: Seata AT mode with `@GlobalTransactional`
- **Caching**: Redisson-based, use `@Cacheable/@CachePut/@CacheEvict` or `RedisUtils`
- **Locking**: `@Lock4j` for distributed locks
- **Idempotency**: `@Idempotent` annotation
- **Rate Limiting**: Sentinel with `@SentinelResource`

## Code Generation

Use built-in generator at http://localhost/tool/gen:
1. Import table from connected datasource
2. Configure generation options (package, module, author)
3. Generate complete CRUD code including:
   - Entity, Bo, Vo classes
   - Mapper (interface + XML)
   - Service (interface + impl)
   - Controller with SpringDoc annotations
   - Frontend Vue3 pages

Generated code follows project conventions and includes pagination, export, permissions.

## Common Development Tasks

### Adding a New Module
1. Create under `ruoyi-modules/ruoyi-{name}/`
2. Follow structure: `domain/`, `mapper/`, `service/`, `controller/`, `dubbo/`
3. Add module to parent `pom.xml` and `ruoyi-modules/pom.xml`
4. Create `application.yml` following Nacos pattern
5. Add Nacos config: `script/config/nacos/ruoyi-{name}.yml`
6. Update Gateway routes if external access needed
7. Create SQL schema in `script/sql/`

### Adding a Dubbo API
1. Define interface in `ruoyi-api/ruoyi-api-{module}/src/main/java/.../api/`
2. Add to `ruoyi-api-bom` dependencies
3. Implement with `@DubboService` in business module
4. Consume with `@DubboReference` in other modules

### Working with Multi-tenancy
- Tenant ID automatically injected by `TenantPlugin` for MyBatis-Plus queries
- Bypass with `@TenantIgnore` on mapper methods or service methods
- Admin tenant (tenantId: "000000") has cross-tenant access

### Database Scripts
- Main schema: `script/sql/ry-cloud.sql`
- Nacos config: `script/sql/ry-config.sql`
- Oracle/PostgreSQL variants in `script/sql/{oracle,postgres}/`
- Module-specific: `script/sql/ruoyi-{module}.sql`

## Code Style

- **Java**: Alibaba Java Coding Guidelines, 4-space indent
- **Lombok**: Heavily used - `@Data`, `@RequiredArgsConstructor`, `@Slf4j`
- **Validation**: Use `javax.validation` annotations on Bo classes
- **Documentation**: SpringDoc via Javadoc (no `@ApiModel` needed)
- **Naming**:
  - Controllers: `{Entity}Controller` with `@RestController`
  - Services: `I{Entity}Service` (interface), `{Entity}ServiceImpl` (impl)
  - Mappers: `{Entity}Mapper` (interface), `{Entity}Mapper.xml` (SQL)

## Testing

- Test sources in `src/test/java/`, resources in `src/test/resources/`
- Use `@SpringBootTest` for integration tests
- Maven Surefire configured to run tests tagged with active profile (dev/prod)
- Exclude tests with `@Tag("exclude")`

## Key Dependencies & Versions

Managed in root `pom.xml`:
- Spring Boot: 3.5.9
- Spring Cloud: 2025.0.1
- MyBatis-Plus: 3.5.16
- Redisson: 3.52.0
- Sa-Token: 1.44.0
- SnailJob: 1.9.0
- Hutool: 5.8.43
- Warm-Flow: 1.8.4

## Documentation & Resources

- Official docs: https://plus-doc.dromara.org
- Architecture diagram in README.md
- Module-specific docs may be in `docs/` subdirectories
- Frontend repo: https://gitee.com/JavaLionLi/plus-ui
