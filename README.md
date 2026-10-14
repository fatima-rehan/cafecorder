# CafeCorder

A logbook for Toronto cafe visits. Log what you ordered, what you paid and how you'd rate it, mark study sessions (wifi, outlets, seating, noise), add a photo, and share your favourites on a public feed.

Visit @ https://cafecorder.vercel.app/

## Features

- **Your logbook:** add, edit and delete visits. Filter by neighbourhood and sort by newest or rating.
- **Study spots:** mark a visit as a study session and note wifi, outlets, seating, laptop policy and noise level.
- **Stats:** total visits, unique cafes, total spent and average rating.
- **Extras:** add anything else you ordered, like a pastry on the side, each with its own price. Totals include them.
- **Photos:** upload a photo, then drag and zoom to frame it. It's cropped and resized in the browser before upload.
- **Explore:** a public feed of shared visits and a leaderboard of the top-rated cafes.
- **Accounts:** sign up and log in. Every visit is private to its owner unless shared.

## Tech stack

| Layer | Tech |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Web MVC, Spring Data JPA (Hibernate), Bean Validation |
| Auth | Spring Security, stateless JWT, BCrypt password hashing |
| Database | PostgreSQL (Supabase) |
| Frontend | React + Vite, plain JavaScript and CSS |
| Tests | JUnit 5, Mockito, MockMvc |

## How it's built

```
React frontend  --HTTPS + JWT-->  Spring Boot REST API  --JDBC-->  PostgreSQL

controller  ->  service  ->  repository
```

- Controllers handle HTTP and validation, services hold the logic, and repositories talk to the database.
- Regular visits and study sessions share one `cafe_visits` table using JPA single-table inheritance (`StudyCafe extends Cafe`).
- The schema is plain SQL in `schema.sql`. Queries use Spring Data, JPQL and native PostgreSQL for the leaderboard.
- Errors come back as consistent JSON (`ProblemDetail`) with per-field messages.

## API

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/register` | Create an account |
| POST | `/api/auth/login` | Log in, returns a token |
| GET | `/api/auth/me` | Current user |
| GET | `/api/visits` | My visits (`?neighbourhood=`, `?sort=RECENT\|RATING`) |
| GET | `/api/visits/{id}` | One visit |
| POST | `/api/visits` | Log a visit |
| PUT | `/api/visits/{id}` | Update a visit |
| DELETE | `/api/visits/{id}` | Delete a visit |
| GET | `/api/visits/stats` | My stats |
| GET | `/api/visits/neighbourhoods` | Neighbourhoods I've visited |
| GET | `/api/feed/photos` | Public feed (`?sort=RECENT\|TOP`) |
| GET | `/api/feed/top-cafes` | Top-rated cafes |

## Running locally

Backend (needs Java 21 and a PostgreSQL database):

```
cd backend
cp .env.example .env   # fill in your database details and a JWT secret
./mvnw spring-boot:run
```

Frontend:

```
cd frontend
cp .env.example .env
npm install
npm run dev
```

Tests: `cd backend && ./mvnw test` (no database needed).
