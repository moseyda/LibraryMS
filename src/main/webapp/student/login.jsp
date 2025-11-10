<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sign In - LibraryMS</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800;900&display=swap" rel="stylesheet">
    <link href="<%= request.getContextPath() %>/css/generalStyling.css" rel="stylesheet" type="text/css">

</head>
<body class="login-page">
<!-- Navigation -->
<nav class="navbar">
    <div class="nav-container">
        <a href="<%= request.getContextPath() %>/src/main/webapp/common/index.jsp" class="brand">
            <svg class="brand-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span>LibraryMS</span>
        </a>
        <div class="nav-menu">
            <a href="<%= request.getContextPath() %>/src/main/webapp/common/index.jsp" class="nav-link">← Back to Home</a>
        </div>
    </div>
</nav>

<!-- student.Login Content -->
<div class="login-wrapper">
    <!-- Left Side - Information -->
    <div class="info-section">
        <span class="info-badge">🔐 Welcome Back</span>
        <h1 class="info-title">Sign In to Your Account</h1>
        <p class="info-description">
            Access your personalized library experience. Manage your borrowed books,
            explore new titles, and track your reading journey.
        </p>

        <div class="info-features">
            <div class="feature-item">
                <div class="feature-icon">📚</div>
                <div class="feature-text">
                    <h3>Your Library</h3>
                    <p>Access your borrowed books and reading history</p>
                </div>
            </div>
            <div class="feature-item">
                <div class="feature-icon">⭐</div>
                <div class="feature-text">
                    <h3>Personalized</h3>
                    <p>Get book recommendations based on your interests</p>
                </div>
            </div>
            <div class="feature-item">
                <div class="feature-icon">🔔</div>
                <div class="feature-text">
                    <h3>Notifications</h3>
                    <p>Stay updated with due dates and new arrivals</p>
                </div>
            </div>
        </div>

        <div class="login-benefits">
            <div class="benefit-item">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
                <span>Secure login system</span>
            </div>
            <div class="benefit-item">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
                <span>24/7 access to digital resources</span>
            </div>
            <div class="benefit-item">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
                <span>Quick book search and reservation</span>
            </div>
        </div>
    </div>

    <!-- Right Side - student.Login Form -->
    <div class="form-container">
        <div class="form-header">
            <h2 class="form-title">Sign In</h2>
            <p class="form-subtitle">Enter your credentials to access your account</p>
        </div>

        <%-- Display error message if login fails --%>
        <%
            String errorMessage = (String) request.getAttribute("errorMessage");
            if (errorMessage != null) {
        %>
        <div class="error-message">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
            <span><%= errorMessage %></span>
        </div>
        <% } %>

        <form action="<%= request.getContextPath() %>/login" method="post" class="login-form" id="loginForm">
            <div class="form-group">
                <label class="form-label" for="snumber">
                    Student Number <span class="required">*</span>
                </label>
                <div class="input-icon">
                    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V8a2 2 0 00-2-2h-5m-4 0V5a2 2 0 114 0v1m-4 0a2 2 0 104 0m-5 8a2 2 0 100-4 2 2 0 000 4zm0 0c1.306 0 2.417.835 2.83 2M9 14a3.001 3.001 0 00-2.83 2M15 11h3m-3 4h2"/>
                    </svg>
                    <input
                            type="text"
                            id="snumber"
                            name="snumber"
                            class="form-input"
                            placeholder="Enter your student number"
                            required
                            value="<%= request.getParameter("snumber") != null ? request.getParameter("snumber") : "" %>"
                    >
                </div>
            </div>

            <div class="form-group">
                <label class="form-label" for="password">
                    Password <span class="required">*</span>
                </label>
                <div class="input-icon">
                    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
                    </svg>
                    <input
                            type="password"
                            id="password"
                            name="password"
                            class="form-input"
                            placeholder="Enter your password"
                            required
                    >
                </div>
            </div>

            <div class="form-options">
                <label class="checkbox-label">
                    <input type="checkbox" name="remember">
                    <span>Remember me</span>
                </label>
                <a href="#" class="forgot-link">Forgot password?</a>
            </div>

            <button type="submit" class="submit-btn">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"></path>
                    <polyline points="10 17 15 12 10 7"></polyline>
                    <line x1="15" y1="12" x2="3" y2="12"></line>
                </svg>
                Sign In
            </button>
        </form>

        <div class="form-footer">
            Don't have an account? <a href="<%= request.getContextPath() %>/src/main/webapp/student/registration.jsp">Register here</a>
        </div>
    </div>
</div>

<script>
    // Form submission handler
    document.getElementById('loginForm').addEventListener('submit', function(e) {
        const btn = this.querySelector('.submit-btn');
        btn.classList.add('loading');
        btn.innerHTML = '<span>Signing in...</span>';
    });

    // Input validation feedback
    const inputs = document.querySelectorAll('.form-input');
    inputs.forEach(input => {
        input.addEventListener('blur', function() {
            if (this.value.trim() !== '') {
                this.style.borderColor = '#10b981';
            }
        });

        input.addEventListener('focus', function() {
            this.style.borderColor = '#6366f1';
        });
    });
</script>
</body>
</html>