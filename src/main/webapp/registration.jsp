
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Member Registration - LibraryMS</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800;900&display=swap" rel="stylesheet">
    <link href="generalStyling.css" rel="stylesheet" type="text/css">

</head>
<body class="registration-page">
<!-- Navigation -->
<nav class="navbar">
    <div class="nav-container">
        <a href="index.jsp" class="brand">
            <svg class="brand-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span>LibraryMS</span>
        </a>
        <div class="nav-menu">
            <a href="index.jsp" class="nav-link">← Back to Home</a>
        </div>
    </div>
</nav>

<!-- Registration Content -->
<div class="registration-wrapper">
    <!-- Left Side - Information -->
    <div class="info-section">
        <span class="info-badge">📚 Join Our Community</span>
        <h1 class="info-title">Start Your Journey With Us</h1>
        <p class="info-description">
            Become a member today and unlock access to thousands of books, digital resources,
            and exclusive library services. Registration is quick, easy, and completely free!
        </p>

        <div class="info-features">
            <div class="feature-item">
                <div class="feature-icon">⚡</div>
                <div class="feature-text">
                    <h3>Instant Access</h3>
                    <p>Get immediate access to our entire collection</p>
                </div>
            </div>
            <div class="feature-item">
                <div class="feature-icon">📖</div>
                <div class="feature-text">
                    <h3>50,000+ Books</h3>
                    <p>Browse through our extensive library catalog</p>
                </div>
            </div>
            <div class="feature-item">
                <div class="feature-icon">💻</div>
                <div class="feature-text">
                    <h3>Digital Resources</h3>
                    <p>Access e-books and audiobooks anytime, anywhere</p>
                </div>
            </div>
        </div>

        <div class="benefits-grid">
            <div class="benefit-card">
                <div class="benefit-card-icon">🎯</div>
                <h4>Smart Recommendations</h4>
                <p>AI-powered book suggestions based on your interests</p>
            </div>
            <div class="benefit-card">
                <div class="benefit-card-icon">🔔</div>
                <h4>Notifications</h4>
                <p>Stay updated with due dates and new arrivals</p>
            </div>
            <div class="benefit-card">
                <div class="benefit-card-icon">📱</div>
                <h4>Mobile App</h4>
                <p>Manage your account on the go</p>
            </div>
            <div class="benefit-card">
                <div class="benefit-card-icon">🏆</div>
                <h4>Rewards Program</h4>
                <p>Earn points for every book you borrow</p>
            </div>
        </div>
    </div>

    <!-- Right Side - Registration Form -->
    <div class="form-container">
        <div class="form-header">
            <h2 class="form-title">Create Your Account</h2>
            <p class="form-subtitle">Fill in your details to get started</p>
        </div>


        <form action="<%= request.getContextPath() %>/registration" method="post" class="registration-form" id="registrationForm">
            <div class="form-group">
                <label class="form-label" for="fname">
                    First Name <span class="required">*</span>
                </label>
                <div class="input-icon">
                    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
                    </svg>
                    <input
                            type="text"
                            id="fname"
                            name="fname"
                            class="form-input"
                            placeholder="John"
                            required
                    >
                </div>
            </div>

            <div class="form-group">
                <label class="form-label" for="lname">
                    Last Name <span class="required">*</span>
                </label>
                <div class="input-icon">
                    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
                    </svg>
                    <input
                            type="text"
                            id="lname"
                            name="lname"
                            class="form-input"
                            placeholder="Doe"
                            required
                    >
                </div>
            </div>

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
                            placeholder="e.g., 2025001"
                            required
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
                            placeholder="Create a secure password"
                            minlength="6"
                            required
                    >
                </div>
                <p style="font-size: 0.875rem; color: #6b7280; margin-top: 0.5rem;">
                    Password must be at least 6 characters long
                </p>
            </div>

            <button type="submit" class="submit-btn">
                <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
                </svg>
                Complete Registration
            </button>
        </form>

        <div class="form-footer">
            Already have an account? <a href="index.jsp">Sign in here</a>
        </div>
    </div>
</div>

<script>
    // Form submission handler
    document.getElementById('registrationForm').addEventListener('submit', function(e) {
        const btn = this.querySelector('.submit-btn');
        btn.classList.add('loading');
        btn.textContent = 'Processing...';
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