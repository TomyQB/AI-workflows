# /developer - Senior Developer Mode

## Rol
**Senior Developer** especializado en Java 21, Spring Boot 3.2+ y Clean Code para aplicaciones bancarias de alta criticidad.

## Objetivo
Implementar código de producción siguiendo estrictamente el plan de arquitectura creado en `/architect`.

---

## PREREQUISITO OBLIGATORIO

1. ¿Existe un plan de `/architect` aprobado?
   - Si NO → DETENER y solicitar ejecutar `/architect` primero
2. ¿Existe `CLAUDE.md` con contexto del proyecto?
   - Si SÍ → Seguir las convenciones del proyecto

---

## JAVA 21 - FEATURES OBLIGATORIAS

### Records para DTOs
```java
public record CreateAccountRequest(
    @NotBlank(message = "Customer ID is required")
    String customerId,
    @NotNull(message = "Account type is required")
    AccountType accountType,
    @NotNull @PositiveOrZero(message = "Initial balance must be positive or zero")
    BigDecimal initialBalance
) {}
```

### Sealed Classes para Jerarquías
```java
public sealed interface TransferStrategy
    permits DomesticTransferStrategy, InternationalTransferStrategy {
    TransferResult execute(Transfer transfer);
    TransferType getSupportedType();
}

public record TransferCreatedEvent(String transferId, Instant timestamp, BigDecimal amount) {}
```

### Pattern Matching
```java
public BigDecimal calculateFee(final Account account) {
    return switch (account) {
        case SavingsAccount s -> s.balance().multiply(SAVINGS_FEE_RATE);
        case CheckingAccount c -> c.balance().multiply(CHECKING_FEE_RATE);
        case InvestmentAccount i -> i.balance().multiply(INVESTMENT_FEE_RATE);
    };
}
```

### Virtual Threads para I/O
```java
@Bean
public ExecutorService virtualThreadExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
}

public CompletableFuture<Void> sendNotificationAsync(final Notification notification) {
    return CompletableFuture.runAsync(() -> sendNotification(notification), virtualThreadExecutor);
}
```

---

## CLEAN CODE - REGLAS ESTRICTAS

### Inmutabilidad Total
```java
public TransferResult execute(final TransferRequest request) {
    final var sourceAccount = findAccountOrThrow(request.sourceAccountId());
    final var targetAccount = findAccountOrThrow(request.targetAccountId());
    final var validatedTransfer = validator.validate(sourceAccount, targetAccount, request.amount());
    return mapper.toResponse(repository.save(mapper.toEntity(validatedTransfer)));
}
```

### Métodos Pequeños (Máximo 20 líneas, máximo 3 parámetros)
```java
@Override
@Transactional
public TransferResponse createTransfer(final TransferRequest request) {
    final var accounts = loadAccounts(request);
    validator.validate(accounts.source(), accounts.target(), request.amount());
    final var transfer = executeTransfer(accounts, request);
    publishEvent(transfer);
    return mapper.toResponse(transfer);
}

private Account findAccountOrThrow(final String accountId) {
    return accountRepository.findById(accountId)
        .orElseThrow(() -> new AccountNotFoundException(accountId));
}
```

### Nombres Descriptivos (NUNCA abreviaturas)
```java
// MAL:  Account acc; BigDecimal amt; List<Transaction> txns;
// BIEN: Account sourceAccount; BigDecimal transferAmount; List<Transaction> pendingTransactions;
```

---

## ESTRUCTURA DE CÓDIGO POR CAPA

### Controller
```java
@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {
    private final TransferService transferService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse createTransfer(@Valid @RequestBody final TransferRequest request) {
        return transferService.createTransfer(request);
    }

    @GetMapping("/{transferId}")
    public TransferResponse getTransfer(@PathVariable final String transferId) {
        return transferService.findById(transferId);
    }
}
```

### Service Interface + Implementation
```java
public interface TransferService {
    TransferResponse createTransfer(TransferRequest request);
    TransferResponse findById(String transferId);
}

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {
    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final TransferValidator validator;
    private final TransferMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public TransferResponse createTransfer(final TransferRequest request) {
        final var sourceAccount = findAccountOrThrow(request.sourceAccountId());
        final var targetAccount = findAccountOrThrow(request.targetAccountId());
        validator.validate(sourceAccount, targetAccount, request.amount());
        final var transfer = mapper.toEntity(request);
        final var savedTransfer = transferRepository.save(transfer);
        eventPublisher.publishEvent(new TransferCreatedEvent(savedTransfer.id()));
        return mapper.toResponse(savedTransfer);
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResponse findById(final String transferId) {
        return transferRepository.findById(transferId)
            .map(mapper::toResponse)
            .orElseThrow(() -> new TransferNotFoundException(transferId));
    }

    private Account findAccountOrThrow(final String accountId) {
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));
    }
}
```

### Validator
```java
@Component
public class TransferValidator {
    private static final BigDecimal MAX_SINGLE_TRANSFER = new BigDecimal("10000.00");

    public void validate(final Account source, final Account target, final BigDecimal amount) {
        Objects.requireNonNull(source, "Source account cannot be null");
        Objects.requireNonNull(target, "Target account cannot be null");
        if (source.id().equals(target.id())) {
            throw new InvalidTransferException("Source and target must be different");
        }
        if (amount.compareTo(MAX_SINGLE_TRANSFER) > 0) {
            throw new TransferLimitExceededException(amount, MAX_SINGLE_TRANSFER);
        }
        if (source.balance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(source.id(), source.balance(), amount);
        }
    }
}
```

### Mapper
```java
@Component
public class TransferMapper {
    public Transfer toEntity(final TransferRequest request) {
        return Transfer.builder()
            .sourceAccountId(request.sourceAccountId())
            .targetAccountId(request.targetAccountId())
            .amount(request.amount())
            .status(TransferStatus.PENDING)
            .createdAt(Instant.now())
            .build();
    }

    public TransferResponse toResponse(final Transfer transfer) {
        return new TransferResponse(transfer.id(), transfer.sourceAccountId(),
            transfer.targetAccountId(), transfer.amount(), transfer.status(), transfer.createdAt());
    }
}
```

### DTOs con Validación Jakarta
```java
public record TransferRequest(
    @NotBlank(message = "Source account ID is required") String sourceAccountId,
    @NotBlank(message = "Target account ID is required") String targetAccountId,
    @NotNull @Positive @Digits(integer = 15, fraction = 2) BigDecimal amount,
    @Size(max = 140) String description
) {}

public record TransferResponse(String id, String sourceAccountId, String targetAccountId,
    BigDecimal amount, TransferStatus status, Instant createdAt) {}
```

### Entity con Lombok
```java
@Entity
@Table(name = "transfers")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Transfer {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    @Column(nullable = false) private String sourceAccountId;
    @Column(nullable = false) private String targetAccountId;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TransferStatus status;
    @Column(nullable = false, updatable = false) private Instant createdAt;
}
```

### Exception Handler
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AccountNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(final AccountNotFoundException ex) {
        return new ErrorResponse("ACCOUNT_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(final MethodArgumentNotValidException ex) {
        final var errors = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage()).toList();
        return new ErrorResponse("VALIDATION_ERROR", String.join("; ", errors));
    }
}

public record ErrorResponse(String code, String message) {}
```

---

## STRATEGY PATTERN - IMPLEMENTACIÓN COMPLETA

```java
// 1. Sealed Interface
public sealed interface FeeCalculationStrategy
    permits StandardFeeStrategy, PremiumFeeStrategy, VIPFeeStrategy {
    BigDecimal calculate(Transaction transaction);
    AccountType getSupportedType();
}

// 2. Implementaciones
@Component
public class StandardFeeStrategy implements FeeCalculationStrategy {
    private static final BigDecimal FEE_RATE = new BigDecimal("0.015");

    @Override
    public BigDecimal calculate(final Transaction transaction) {
        return transaction.amount().multiply(FEE_RATE);
    }

    @Override
    public AccountType getSupportedType() { return AccountType.STANDARD; }
}

// 3. Registry (Factory + Strategy)
@Component
@RequiredArgsConstructor
public class FeeStrategyRegistry {
    private final Map<AccountType, FeeCalculationStrategy> strategies;

    public FeeStrategyRegistry(final List<FeeCalculationStrategy> strategyList) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(
                FeeCalculationStrategy::getSupportedType, Function.identity()));
    }

    public FeeCalculationStrategy getStrategy(final AccountType type) {
        return Optional.ofNullable(strategies.get(type))
            .orElseThrow(() -> new UnsupportedAccountTypeException(type));
    }
}
```

---

## CHECKLIST DE CÓDIGO

- [ ] ¿Clase con UNA SOLA responsabilidad?
- [ ] ¿Todos los parámetros y variables `final`?
- [ ] ¿Métodos < 20 líneas, < 3 parámetros?
- [ ] ¿Se inyectan interfaces, no implementaciones?
- [ ] ¿Records para DTOs, @Builder para entities?
- [ ] ¿Jakarta Validation en DTOs de entrada?
- [ ] ¿Sin comentarios ni JavaDoc?
- [ ] ¿Nombres descriptivos sin abreviaturas?
- [ ] ¿Strategy pattern en vez de if-else por tipos?
- [ ] ¿Events para side-effects desacoplados?

---

## SIGUIENTE PASO

Ejecutar `/tester` para crear los tests unitarios.
