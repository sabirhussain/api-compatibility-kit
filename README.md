# API Compatibility Kit

## Introduction
This project provides a **backward‑compatibility kit** for implementing **Stripe API versioning** in production environments.  
It enables support for multiple API versions simultaneously, ensuring seamless updates without breaking existing integrations.  
Customers gain the flexibility to migrate to newer versions at their own pace.

## Features
- 🔄 **Multi‑version support** — run multiple API versions side by side.  
- 🛡️ **Safe upgrades** — roll out new API updates without disrupting existing clients.  
- ⚙️ **Spring Boot integration** — designed as a Spring starter dependency for easy adoption.  
- 🎯 **Customizable versioning** — define and resolve your own version enums.  

## Requirements
- Java 17+  
- Spring Boot framework  

## Installation
Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.xprevel.util.api</groupId>
    <artifactId>api-compatibility-kit-spring-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Configuration
```yaml
api:
  compatibility:
    version-header: X-API-Version
    default-version: VER_2026_02_01
```
- **version-header**: The HTTP header used to specify the API version in client requests.
- **default-version**: The fallback version used when no explicit version is provided.

## Usage

### 1. Define Custom Versions
Create your own enum to represent supported API versions:
```java
public enum MyAppVersion {
    VER_2026_02_01,
    VER_U_2026_08_01
}
```

### 2. Configure the Resolver
Override the `ApiVersionResolver` to bind your custom version enum:
```java
@Configuration
class ApiVersionConfig {

    @Bean
    ApiVersionResolver<MyAppVersion> apiVersionResolver(ApiCompatibilityProperties properties) {
        MyAppVersion version = MyAppVersion.valueOf(properties.getDefaultVersion());
        return new ApiVersionResolver<>(version);
    }
}
```

### 3. Access Version Context
Use `ApiVersionContext` to retrieve the API version from the incoming request:
```java
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentResource {
    private final ApiVersionContext<MyAppVersion> context;
    private final PaymentService paymentService;
    private final PaymentResponseMapperFactory mapperFactory;

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDto> getPaymentInfo(@PathVariable long id) {
        Payment payment = paymentService.getPaymentInfo(id);
        PaymentResponseMapper mapper = mapperFactory.getPaymentResponseMapper(context.getVersion());

        return ResponseEntity.ok(mapper.toDto(payment));
    }
}
```

## Release Preparation

Use the provided `release-prepare.sh` script to tag a release. It strips `-SNAPSHOT`, verifies the build, creates a local release commit, tags it, then reverts the commit so development continues on SNAPSHOT.

```bash
# Validate without making permanent changes
./release-prepare.sh --dry-run

# Prepare the release (creates tag, reverts pom changes)
./release-prepare.sh
```

Once the tag is created locally, push it to trigger the release:

```bash
git push origin <version>   # e.g. git push origin 1.0.0
```

> **Note:** Do not push the release commit — only the tag is needed. Developers continue working on the SNAPSHOT version as normal.

## Contributing
Contributions are welcome!
- Fork the repository
- Create a feature branch
- Submit a pull request

Please ensure your code follows project conventions and includes relevant tests.

## License
This project is licensed under the GNU General Public License v3.0.  
See the [LICENSE](LICENSE) file for details.
