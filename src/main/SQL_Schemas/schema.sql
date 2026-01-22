-- MySQL connection details --
-- username: root --
-- password: LibraryMS2026 --
-- default connection url: jdbc:mysql://127.0.0.1:3306/dbLibraryMS --



CREATE DATABASE IF NOT EXISTS dblibraryms
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE dblibraryms;


CREATE TABLE IF NOT EXISTS admin (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active TINYINT(1) DEFAULT 1
    );

INSERT INTO admin (username, password, role, created_at, is_active)
VALUES ('admin', 'admin123', 'admin', '2025-11-09 00:00:00', 1);



CREATE TABLE IF NOT EXISTS Books (
    book_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(100) NOT NULL,
    isbn VARCHAR(20) NOT NULL,
    category VARCHAR(50) NOT NULL,
    publisher VARCHAR(100),
    publicationYear INT,
    quantity INT NOT NULL,
    available INT NOT NULL,
    description TEXT
    );


CREATE TABLE IF NOT EXISTS Students (
    student_id INT AUTO_INCREMENT PRIMARY KEY,
    FName VARCHAR(100) NOT NULL,
    LName VARCHAR(100) NOT NULL,
    SNumber VARCHAR(50) UNIQUE NOT NULL,
    Password VARCHAR(255) NOT NULL,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );



CREATE TABLE IF NOT EXISTS BorrowReturnHist (
    record_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    isbn VARCHAR(20) NOT NULL,
    SNumber VARCHAR(50) NOT NULL,
    book_id INT NOT NULL,
    firstName VARCHAR(100),
    lastName VARCHAR(100),
    borrowDate TIMESTAMP,
    expectedReturnDate TIMESTAMP,
    actualReturnDate TIMESTAMP,
    status VARCHAR(20) DEFAULT 'borrowed',
    finePaid BOOLEAN DEFAULT FALSE,
    finePaidDate TIMESTAMP,
    FOREIGN KEY (SNumber) REFERENCES Students(SNumber),
    FOREIGN KEY (book_id) REFERENCES Books(book_id)
    );


CREATE TABLE IF NOT EXISTS Fines (
    fine_id INT AUTO_INCREMENT PRIMARY KEY,
    SNumber VARCHAR(50) NOT NULL,
    fullName VARCHAR(200),
    totalAmount DECIMAL(10,2),
    status VARCHAR(20),
    expectedPaymentDate TIMESTAMP,
    actualPaymentDate TIMESTAMP,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    adjustedByAdmin BOOLEAN DEFAULT FALSE,
    adjustmentDate TIMESTAMP,
    FOREIGN KEY (SNumber) REFERENCES Students(SNumber)
    );

-- FineBooks table to store books array associated with each fine. --
-- unlike Mongodb, we need a separate table for this in SQL as arrays cannot be stored directly --

CREATE TABLE IF NOT EXISTS FineBooks (
    id INT AUTO_INCREMENT PRIMARY KEY,
    fine_id INT NOT NULL,
    title VARCHAR(255),
    isbn VARCHAR(50),
    FOREIGN KEY (fine_id) REFERENCES Fines(fine_id) ON DELETE CASCADE
    );



CREATE TABLE IF NOT EXISTS Messages (
    message_id INT AUTO_INCREMENT PRIMARY KEY,
    studentNumber VARCHAR(50) NOT NULL,
    name VARCHAR(200),
    subject VARCHAR(255),
    message TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (studentNumber) REFERENCES Students(SNumber)
    );