# SmartLib 📚

A full-stack Library Management System designed to manage books, physical book copies, members, borrowing, returns, reservations, fines, and QR-based circulation through a modern web application.

## 🌐 Live Demo

**Frontend:**  
https://smartlib-frontend-nmml.onrender.com

**Backend API:**  
https://smartlib-backend-62iz.onrender.com

**GitHub:**  
https://github.com/MSANTHOSH147/smartlib

---

## 📌 Overview

SmartLib is a modern library management platform built to digitize everyday library operations.

Instead of treating a book as a single database record, SmartLib tracks individual physical copies, allowing libraries to manage availability, borrowing, returns, reservations, and QR-based circulation more accurately.

The system provides separate workflows for administrators and library members.

---

## ✨ Features

### 🔐 Authentication

- User registration and login
- JWT-based authentication
- Role-based access control
- Protected admin routes
- Password reset workflow
- Email verification support

### 📚 Book Management

- Add and manage books
- Search and browse books
- Book details
- Category management
- Physical copy tracking

### 📦 Physical Book Copies

Each physical copy is tracked independently.

Supported copy states include:

- AVAILABLE
- BORROWED
- RESERVED
- DAMAGED
- LOST
- MAINTENANCE

This allows the system to maintain accurate real-world inventory.

### 📱 QR-Based Circulation

SmartLib supports QR-based identification of individual physical book copies.

Workflow:

```text
Physical Book
      ↓
QR Code
      ↓
Scan
      ↓
Identify Book Copy
      ↓
Borrow / Return
      ↓
Update Inventory