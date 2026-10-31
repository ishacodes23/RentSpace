# RentSpace — Property Rental Management System

A full-stack Spring Boot web application for browsing, booking, and managing rental properties. The application enables users to explore rental listings, make bookings with real-time availability checks, and allows administrators to manage properties and monitor booking activity through a dedicated dashboard.

---

## Overview

RentSpace is built using the Spring Boot MVC architecture with Thymeleaf as the frontend template engine and MySQL as the database. The application provides a complete rental booking workflow, including user authentication, property management, booking validation, and an administrative dashboard.

---

## Features

### User Features

- User registration and login with BCrypt password hashing
- Session-based authentication
- Browse rental properties by type, location, keyword, and price range
- View detailed property information
- Check property availability before booking
- Date-range booking with overlap validation (prevents double bookings)
- Automatic rental price calculation (Rent + Security Deposit + Maintenance + GST)
- View and manage personal bookings
- Update user profile information

### Admin Features

- Dedicated admin dashboard
- Add, update, and delete property listings
- Search and filter properties
- View booking history
- Dashboard analytics including:
    - Total Properties
    - Total Bookings
    - Total Revenue
    - Property Type Distribution

---

## Tech Stack

| Layer | Technology |
|--------|------------|
| Backend | Java, Spring Boot, Spring MVC |
| Frontend | Thymeleaf, HTML, CSS, Bootstrap |
| Database | MySQL |
| ORM | Spring Data JPA (Hibernate) |
| Security | BCrypt Password Hashing, Session-based Authentication |
| Build Tool | Maven |

---

## Architecture

The project follows a layered MVC architecture:

```
Controller
     ↓
Service
     ↓
Repository
     ↓
Database (MySQL)
```

### Project Layers

- **Controllers** handle HTTP requests and route users to the appropriate pages.
- **Services** contain the core business logic, including booking validation, availability checking, and rental price calculation.
- **Repositories** interact with the database using Spring Data JPA and custom JPQL queries.
- **Entities** represent the application's domain models:
    - User
    - Property
    - Booking

The project also includes a REST endpoint (`/api/properties`) through `PropertyRestController`.

---

## Key Functionalities

- Booking overlap detection to prevent double-booking
- Dynamic property filtering and searching
- Automatic booking price calculation
- Password encryption using BCrypt
- Property availability management
- Dashboard statistics and revenue tracking

---

## Getting Started

### Prerequisites

- Java 17 or later
- Maven
- MySQL Server

### Installation

#### 1. Clone the repository

```bash
git clone https://github.com/ishacodes23/RentSpace.git
cd RentSpace
```

#### 2. Create the database

```sql
CREATE DATABASE sb_rentaldb;
```

#### 3. Configure the database

Update the database credentials in:

```
src/main/resources/application.properties
```

Example:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/sb_rentaldb
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
spring.jpa.hibernate.ddl-auto=update
```

#### 4. Run the project

Using Maven Wrapper:

```bash
./mvnw spring-boot:run
```

or

```bash
mvn spring-boot:run
```

#### 5. Open the application

```
http://localhost:8080/login
```

---

## Project Structure

```
src
├── main
│   ├── java
│   │   ├── controller
│   │   ├── service
│   │   ├── repository
│   │   ├── model
│   │   ├── security
│   │   └── rest
│   │
│   └── resources
│       ├── templates
│       ├── static
│       └── application.properties
│
└── pom.xml
```

---


## License

This project was developed for academic and learning purposes.

---

## Author

**Isha Kadam**

MCA Student | Java Full Stack Developer | Spring Boot Developer
CI/CD test
