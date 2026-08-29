\# SmartLib 📚



A full-stack digital library management system built with React, Spring Boot, and MySQL.



SmartLib manages both digital library operations and physical book circulation, including QR-based borrowing and returning of physical book copies.



\## 🚀 Features



\### Authentication \& Security

\- User registration and login

\- JWT-based authentication

\- Role-based access control

\- Member and administrator accounts

\- Password reset functionality

\- Change password functionality



\### 📚 Book Management

\- Browse books

\- Search and explore library books

\- Book categories

\- Book details

\- Book availability

\- Book reviews



\### 📦 Physical Inventory

\- Manage physical book copies

\- Automatic copy numbering

\- QR token generation

\- QR label generation

\- Download QR labels

\- Print QR labels

\- Track copy status

\- Track physical condition

\- Track physical location

\- Activate/deactivate physical copies

\- Prevent modification of borrowed copies



\### 📱 QR-Based Library Circulation

\- Scan physical book QR codes

\- Identify individual physical copies

\- Member QR borrowing

\- Member QR return

\- Administrator QR return

\- Prevent borrowing unavailable copies

\- Prevent members from returning books they do not own

\- Track exact physical copy circulation



\### 🔄 Borrowing

\- Borrow books

\- Return books

\- Active borrowing history

\- Due dates

\- Overdue tracking

\- Physical-copy tracking



\### 📅 Reservations

\- Create reservations

\- View reservations

\- Waiting reservations

\- Ready reservations

\- Reservation status management



\### 💰 Fine Management

\- Automatic overdue fine calculation

\- View fines

\- Unpaid fines

\- Fine payment

\- Administrator fine management



\### 👨‍💼 Administration

\- Admin dashboard

\- Member management

\- Book management

\- Physical inventory management

\- Borrowing management

\- Reservation management

\- Fine management

\- QR-based physical book returns



\### 📧 Notifications

\- Email service integration

\- Password reset emails

\- Library-related notifications



\## 🛠️ Technology Stack



\### Frontend

\- React

\- Vite

\- React Router

\- Axios

\- Lucide React

\- QR scanning

\- Responsive UI



\### Backend

\- Java

\- Spring Boot

\- Spring Security

\- Spring Data JPA

\- Hibernate

\- JWT

\- Maven



\### Database

\- MySQL



\## 🏗️ Architecture



```text

&#x20;                   SmartLib

&#x20;                      │

&#x20;         ┌────────────┴────────────┐

&#x20;         │                         │

&#x20;     Frontend                   Backend

&#x20;      React                  Spring Boot

&#x20;       │                         │

&#x20;       │ REST API                │

&#x20;       └──────────────┬──────────┘

&#x20;                      │

&#x20;                    MySQL

