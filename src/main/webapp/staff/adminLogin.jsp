
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Login - LibraryMS</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800;900&display=swap" rel="stylesheet">
    <link href="<%= request.getContextPath() %>/css/generalStyling.css" rel="stylesheet" type="text/css">
</head>
<body class="admin-login-page">
<!-- Navigation -->
<nav class="navbar">
    <div class="nav-container">
        <a href="<%= request.getContextPath() %>/common/index.jsp" class="brand">
            <svg class="brand-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span>LibraryMS</span>
        </a>
        <div class="nav-menu">
            <a href="<%= request.getContextPath() %>/common/index.jsp" class="nav-link">← Back to Home</a>
        </div>
    </div>
</nav>

<!-- Admin Login Content -->
<div class="admin-login-wrapper">
    <div class="admin-form-container">
        <div class="admin-badge">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
            </svg>
        </div>

        <div class="form-header">
            <h2 class="form-title">Administrator Login</h2>
            <p class="form-subtitle">Access the admin control panel</p>
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

        <form action="<%= request.getContextPath() %>/admin/login" method="post" class="admin-login-form" id="adminLoginForm">
            <div class="form-group">
                <label class="form-label" for="username">
                    Username <span class="required">*</span>
                </label>
                <div class="input-icon">
                    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
                    </svg>
                    <input
                            type="text"
                            id="username"
                            name="username"
                            class="form-input"
                            placeholder="Enter admin username"
                            required
                            autocomplete="username"
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
                            placeholder="Enter admin password"
                            required
                            autocomplete="current-password"
                    >
                </div>
            </div>

            <button type="submit" class="submit-btn admin-btn">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"></path>
                    <polyline points="10 17 15 12 10 7"></polyline>
                    <line x1="15" y1="12" x2="3" y2="12"></line>
                </svg>
                Sign In as Admin
            </button>
        </form>

        <div class="form-footer">
            <a href="<%= request.getContextPath() %>/login">Sign in as Student instead</a>
        </div>
    </div>
</div>

<script>
    document.getElementById('adminLoginForm').addEventListener('submit', function(e) {
        const btn = this.querySelector('.submit-btn');
        btn.classList.add('loading');
        btn.innerHTML = '<span>Authenticating...</span>';
    });

    if (data.success) {
        showToast('✓ Login successful!', 'success');
        setTimeout(() => {
            window.location.href = data.redirect;
        }, 1500);
    } else {
        showToast('✗ ' + (data.error || 'Invalid credentials'), 'error');
    }
</script>
</body>
</html>