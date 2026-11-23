# Wallet Management System

A secure RESTful Wallet Management System built using Java and Spring Boot. The application allows users to register, authenticate, manage their wallet balance, add money, transfer money to other wallets, and view transaction history. It also provides administrative APIs to view all wallets and transactions.

## 1. Tech Stack

| Technology | Purpose |
|---|---|
| Java 26 | Programming language |
| Spring Boot 4.2.0 | Backend framework |
| Spring Security | Authentication and authorization |
| JWT | Stateless authentication |
| Spring Data JPA | Database operations |
| Hibernate | ORM |
| MySQL / H2 | Database |
| Maven | Dependency management and build |
| JUnit 5 | Unit testing |
| Mockito | Mocking dependencies in tests |
| JaCoCo | Test coverage reporting |
| Postman | API testing |

## 2. Features

### User Management
- User registration and login.
- Password hashing using Spring Security's password encoder.
- JWT-based authentication.
- Role-based authorization.

### Wallet Management
- Automatic wallet creation during registration.
- View logged-in user's wallet balance.
- Add money to a wallet.
- Transfer money between wallets.
- Prevent unauthorized access to other users' wallets.

### Transaction Management
- Record wallet transactions.
- Maintain transaction types and statuses.
- View personal transaction history.
- Pagination and sorting support.
- Idempotency key support for add-money and transfer operations.

### Admin Management
- Retrieve all registered wallets.
- Retrieve all wallet transactions.
- Paginated administrative results.

## 3. Project Structure

```text
wallet-system/
│
├── src/
│   ├── main/
│   │   ├── java/com/wallet/system/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   │   ├── request/
│   │   │   │   └── response/
│   │   │   ├── entity/
│   │   │   ├── enums/
│   │   │   ├── exception/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   ├── service/
│   │   │   └── WalletSystemApplication.java
│   │   │
│   │   └── resources/
│   │       └── application.yml
│   │
│   └── test/
│       ├── java/com/wallet/system/
│       │   ├── controller/
│       │   └── service/
│       └── resources/
│           └── application.yml
│
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

## 4. Project Setup and Run Instructions

### Prerequisites

Install the following:

- Java 26
- Git
- IDE such as IntelliJ IDEA
- Postman (for API testing)

### Step 1: Clone the repository

```bash
git clone <YOUR_REPOSITORY_URL>
```

Navigate into the project:

```bash
cd wallet-system
```

### Step 2: Configure environment variables

The application requires a JWT secret.

Set the `JWT_SECRET` environment variable according to the configuration referenced in your `application.yml` or `application.properties`.

Example for PowerShell:

```powershell
$env:JWT_SECRET="YOUR_BASE64_ENCODED_SECRET"
```

Use a strong, securely generated secret. Never commit production secrets to Git.

### Step 3: Configure the database

The project uses an H2 in-memory database for development/testing.

If you use MySQL for your local or production environment, configure the database URL, username, password, and Hibernate settings in your application configuration.

### Step 4: Build the application

Using the Maven wrapper:

```powershell
.\mvnw.cmd clean install
```

### Step 5: Run the application

```powershell
.\mvnw.cmd spring-boot:run
```

The application runs at:

```text
http://localhost:8080
```

## 5. API Documentation

Base URL:

```text
http://localhost:8080
```

### Authentication APIs

| Method | Endpoint | Description | Authentication |
|---|---|---|---|
| POST | `/auth/register` | Register a new user | Not required |
| POST | `/auth/login` | Authenticate user and generate JWT | Not required |

**Register request:**

```json
{
  "email": "user@gmail.com",
  "password": "Password@123"
}
```

Successful registration creates a user and an associated wallet.

**Login request:**

```json
{
  "email": "user@gmail.com",
  "password": "Password@123"
}
```

The response contains an authentication token and token type.

### Wallet APIs

All wallet APIs require a valid JWT.

| Method | Endpoint | Description |
|---|---|---|
| GET | `/wallet` | Get logged-in user's wallet |
| POST | `/wallet/add` | Add money to wallet |
| POST | `/wallet/transfer` | Transfer money to another wallet |
| GET | `/wallet/transactions` | Get personal transaction history |

**Add money request:**

```http
POST /wallet/add
Authorization: Bearer <JWT_TOKEN>
Idempotency-Key: unique-add-key
Content-Type: application/json
```

```json
{
  "amount": 100.00
}
```

**Transfer money request:**

```http
POST /wallet/transfer
Authorization: Bearer <JWT_TOKEN>
Idempotency-Key: unique-transfer-key
Content-Type: application/json
```

```json
{
  "receiverWalletId": 2,
  "amount": 50.00
}
```

**Transaction history:**

```http
GET /wallet/transactions?page=0&size=10
Authorization: Bearer <JWT_TOKEN>
```

Transaction history supports pagination and sorting.

### Admin APIs

These endpoints require an authenticated administrator, according to the application's role-based authorization configuration.

| Method | Endpoint | Description |
|---|---|---|
| GET | `/admin/wallets` | Retrieve all wallets |
| GET | `/admin/transactions` | Retrieve all transactions |

Example:

```http
GET /admin/wallets?page=0&size=10
Authorization: Bearer <ADMIN_JWT_TOKEN>
```

```http
GET /admin/transactions?page=0&size=10
Authorization: Bearer <ADMIN_JWT_TOKEN>
```

## 6. API Request Flow

### A. User Registration and Login

1. The client sends registration details to `/auth/register`.
2. The backend validates the request.
3. The email is normalized and checked for duplicates.
4. The password is securely encoded.
5. A new user is saved with the USER role.
6. A wallet is automatically created with an initial zero balance.
7. The user logs in through `/auth/login`.
8. Spring Security authenticates the credentials.
9. A JWT is generated and returned to the client.

### B. Add Money Flow

1. The client sends an add-money request with a JWT and idempotency key.
2. JWT authentication identifies the logged-in user.
3. The backend validates the requested amount.
4. The idempotency key is checked to prevent duplicate processing.
5. The wallet balance is updated.
6. A transaction record is created.
7. The transaction response is returned.

### C. Money Transfer Flow

1. The sender submits the receiver wallet ID and transfer amount.
2. JWT authentication identifies the sender.
3. The backend validates the amount and receiver wallet.
4. The sender's available balance is checked.
5. The idempotency key is checked.
6. The sender's balance is debited.
7. The receiver's balance is credited.
8. A transaction record is created.
9. The response is returned to the client.

The transfer operation should be executed transactionally to ensure that both wallet balance updates succeed or are rolled back together.

### D. Transaction History Flow

1. The authenticated user requests `/wallet/transactions`.
2. The backend identifies the logged-in user.
3. Transactions associated with that user's wallet are retrieved.
4. Pagination and sorting are applied.
5. The transaction page is returned.

### E. Admin Flow

1. An administrator authenticates and obtains a JWT.
2. The administrator accesses an admin endpoint.
3. Spring Security checks the user's role.
4. The AdminService retrieves the requested wallet or transaction records.
5. Paginated results are returned.

## 7. Assumptions and Design Decisions

### Authentication and Authorization
- JWT is used for stateless authentication.
- Passwords are stored as encoded hashes, never as plain text.
- Users receive the USER role by default.
- Administrative endpoints are intended for ADMIN users.
- Protected APIs identify the current user from the authenticated security context.

### Wallet Management
- Each registered user receives an associated wallet.
- A newly created wallet starts with a zero balance.
- Users can access their own wallet through authenticated APIs.
- Users cannot transfer money without sufficient balance.
- Monetary values use `BigDecimal` to avoid floating-point precision problems.

### Transactions and Idempotency
- Every successful wallet operation should have an associated transaction record.
- Idempotency keys are used for add-money and transfer requests.
- Idempotency keys are protected by a database uniqueness constraint.
- Repeated requests using the same key should not create duplicate financial operations.
- Transfer operations should maintain atomicity using database transactions.

### Database and Architecture
- Spring Data JPA and Hibernate are used for persistence.
- Controller, service, and repository layers separate API handling, business logic, and database access.
- DTOs separate API request/response structures from database entities.
- Custom exceptions and centralized exception handling are used for error management.
- Pagination is used for transaction and administrative listing APIs.

## 8. Testing

The project uses JUnit 5 and Mockito for unit testing.

Test coverage includes:

- Authentication service tests.
- Wallet service tests.
- Admin service tests.
- Authentication controller tests.
- Wallet controller tests.
- Admin controller tests.

### Run all tests

```powershell
.\mvnw.cmd test
```

### Generate the JaCoCo coverage report

```powershell
.\mvnw.cmd clean verify
```

Open the report:

```text
target/site/jacoco/index.html
```

The report provides line and branch coverage metrics for the tested application code.

## 9. Error Handling

The application uses request validation and exception handling to manage errors such as:

- Duplicate email registration.
- Invalid login credentials.
- Insufficient wallet balance.
- Invalid transaction amount.
- Invalid or missing authentication token.
- Unauthorized access.
- Invalid receiver wallet.
- Duplicate idempotency key.

The API returns appropriate HTTP error responses based on the application's exception-handling configuration.

## 10. Future Enhancements

- Integrate payment gateways for real-money deposits.
- Add refresh-token functionality.
- Add transaction filtering by date, amount, and status.
- Add integration tests using Testcontainers.
- Integrate SonarQube for static code quality analysis.
- Add CI/CD automation using GitHub Actions.
- Add audit logging and monitoring.
- Add rate limiting and additional security controls.

---

## Author

**Prasad Zalake**

Java | Spring Boot | REST APIs | Spring Security | Hibernate | SQL
