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
    SNumber VARCHAR(50) NOT NULL,
    book_id INT NOT NULL,
    borrowDate TIMESTAMP,
    dueDate TIMESTAMP,
    expectedReturnDate TIMESTAMP,
    actualReturnDate TIMESTAMP,
    returnDate TIMESTAMP,
    status VARCHAR(20),
    FOREIGN KEY (SNumber) REFERENCES Students(SNumber),
    FOREIGN KEY (book_id) REFERENCES Books(book_id)
    );



CREATE TABLE IF NOT EXISTS Fines (
    fine_id INT AUTO_INCREMENT PRIMARY KEY,
    studentNumber VARCHAR(50) NOT NULL,
    record_id INT,
    amount DECIMAL(10,2),
    paid BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (record_id) REFERENCES BorrowReturnHist(record_id)
);


CREATE TABLE IF NOT EXISTS Messages (
    message_id INT AUTO_INCREMENT PRIMARY KEY,
    studentNumber VARCHAR(50),
    name VARCHAR(100),
    subject VARCHAR(255),
    message TEXT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );