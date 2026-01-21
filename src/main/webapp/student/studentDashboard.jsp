<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    // Check if user is logged in
    Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");
    if (loggedIn == null || !loggedIn) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }
    String firstName = (String) session.getAttribute("userFName");
    String lastName = (String) session.getAttribute("userLName");
    String studentNumber = (String) session.getAttribute("userSNumber");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard - LibraryMS</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800;900&display=swap" rel="stylesheet">
    <link href="<%= request.getContextPath() %>/css/generalStyling.css" rel="stylesheet" type="text/css">


</head>
<body>
<!-- Navigation -->
<nav class="navbar">
    <div class="nav-container">
        <a href="../common/index.jsp" class="brand">
            <svg class="brand-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span>LibraryMS</span>
        </a>
        <div class="nav-menu">
            <a href="#" class="nav-link active">Dashboard</a>
            <a href="#" class="nav-link">My Books</a>
            <a href="#" class="nav-link">Browse</a>
            <a href="<%= request.getContextPath() %>/logout" id="logout">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75" />
                </svg>
                Logout
            </a>
        </div>
    </div>
</nav>

<!-- Fines Notification -->
<div id="finesNotification" class="fines-notification" style="display: none;">
    <div class="fines-notification-content">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
        </svg>
        <div class="fines-notification-text">
            <strong>Overdue Books Alert!</strong>
            <span>You have <span id="overdueCount">0</span> overdue book(s). Total fine: £<span id="totalFine">0</span></span>
        </div>
        <button class="btn-pay-fines" onclick="openFinesModal()">Pay Fines</button>
        <button class="fines-notification-close" onclick="dismissFinesNotification()">&times;</button>
    </div>
</div>


<!-- Dashboard Content -->
<div class="dashboard-container">
    <div class="success-card">
        <div class="success-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                <polyline points="22 4 12 14.01 9 11.01"></polyline>
            </svg>
        </div>
        <h1 class="success-title">Welcome Back, <%= firstName %>! 🎉</h1>
        <p class="success-message">
            You have successfully signed in to your LibraryMS account.
        </p>

        <div class="user-info">
            <div class="info-item">
                <span class="info-label">Full Name:</span>
                <span class="info-value"><%= firstName %> <%= lastName %></span>
            </div>
            <div class="info-item">
                <span class="info-label">Student Number:</span>
                <span class="info-value"><%= studentNumber %></span>
            </div>
            <div class="info-item">
                <span class="info-label">Account Status:</span>
                <span class="info-value status-active">Active</span>
            </div>
        </div>

        <div class="dashboard-actions">
            <a href="#" class="action-card">
                <div class="action-icon">📚</div>
                <h3>Browse Books</h3>
                <p>Explore our collection</p>
            </a>
            <a href="#" class="action-card">
                <div class="action-icon">📖</div>
                <h3>My Books</h3>
                <p>View borrowed items</p>
            </a>
            <a href="#" class="action-card">
                <div class="action-icon">📋</div>
                <h3>Account History</h3>
                <p>View borrow history</p>
            </a>
            <a href="#" class="action-card" onclick="openFinesModal()">
                <div class="action-icon">💳</div>
                <h3>Pay Fines</h3>
                <p>Manage your fines</p>
            </a>
        </div>
    </div>
</div>

<!-- Browse Books Modal -->
<div id="booksModal" class="modal">
    <div class="modal-content">
        <div class="modal-header">
            <h2>📚 Browse Books</h2>
            <span class="close-modal">&times;</span>
        </div>
        <div class="modal-body">
            <div class="search-bar">
                <input type="text" id="searchInput" placeholder="Search books by title, author, or ISBN...">
            </div>
            <div id="booksContainer" class="books-grid">
                <div class="loading">Loading books...</div>
            </div>
        </div>
    </div>
</div>


<!-- Fines Payment Modal -->
<div id="finesModal" class="modal">
    <div class="modal-content fines-modal-content">
        <div class="modal-header">
            <h2>💳 Pay Fines</h2>
            <button class="modal-close" onclick="closeFinesModal()">&times;</button>
        </div>

        <div class="fines-modal-body">
            <!-- Tabs -->
            <div class="fines-tabs">
                <button class="fines-tab active" data-tab="overdue" onclick="switchFinesTab('overdue')">
                    Overdue Books
                </button>
                <button class="fines-tab" data-tab="history" onclick="switchFinesTab('history')">
                    Payment History
                </button>
            </div>

            <!-- Overdue Books Tab -->
            <div id="overdueTab" class="fines-tab-content active">
                <div class="fines-info-banner">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="10"></circle>
                        <path d="M12 6v6l4 2"></path>
                    </svg>
                    <span>Fine rate: <strong>£10 per overdue book</strong></span>
                </div>

                <div id="overdueBooksList" class="overdue-books-list">
                    <!-- Dynamically populated -->
                </div>

                <div class="fines-summary">
                    <div class="fines-summary-row">
                        <span>Selected Books:</span>
                        <strong id="selectedCount">0</strong>
                    </div>
                    <div class="fines-summary-row fines-total">
                        <span>Total Fine:</span>
                        <strong>£<span id="selectedTotal">0</span></strong>
                    </div>
                </div>

                <div class="fines-actions">
                    <button class="btn-cancel" onclick="closeFinesModal()">Cancel</button>
                    <button class="btn-submit" id="btnPayFines" onclick="processFinePayment()" disabled>
                        Pay Selected Fines
                    </button>
                </div>
            </div>

            <!-- Payment History Tab -->
            <div id="historyTab" class="fines-tab-content">
                <div id="paymentHistoryList" class="payment-history-list">
                    <!-- Dynamically populated -->
                </div>
            </div>
        </div>
    </div>
</div>





<!-- Footer -->
<div class="footer">
    <div class="footer-content">
        <div class="footer-brand">
            <svg class="footer-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span>LibraryMS</span>
        </div>
        <div class="footer-links">
            <a href="#">About</a>
            <a href="#">Services</a>
            <a href="#">Contact</a>
            <a href="#">Privacy</a>
        </div>
        <p class="footer-copyright">© 2025 LibraryMS. All rights reserved.</p>
    </div>
</div>

<script>window.APP_CTX = '<%= request.getContextPath() %>';</script>
<script src="<%= request.getContextPath() %>/scripts/scripts.js"></script>
</body>
</html>