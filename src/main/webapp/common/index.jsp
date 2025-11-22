
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Library Management System - Modern Digital Library</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800;900&display=swap" rel="stylesheet">
    <link href="<%= request.getContextPath() %>/src/main/webapp/css/generalStyling.css" rel="stylesheet" type="text/css">
</head>
<body>
<!-- Navigation -->
<nav class="navbar">
    <div class="nav-container">
        <a href="index.jsp" class="brand">
            <svg class="brand-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span class="brand-text">LibraryMS</span>
        </a>
        <div class="nav-menu">
            <a href="#hero" class="nav-link active">Home</a>
            <a href="#features" class="nav-link">Features</a>
            <a href="#services" class="nav-link">Services</a>
            <a href="#contact" class="nav-link">Contact</a>
            <a href="<%= request.getContextPath() %>/src/main/webapp/student/login.jsp" class="nav-link">Sign In</a>
            <a href="<%= request.getContextPath() %>/src/main/webapp/student/registration.jsp" class="nav-btn">Get Started</a>
        </div>
    </div>
</nav>

<!-- Hero Section -->
<section class="hero" id="hero">
    <div class="hero-content">
        <div class="hero-text">
            <span class="hero-badge">🚀 Next Generation Library System</span>
            <h1 class="hero-title">
                Transform Your Library
                <span class="gradient-text">Experience</span>
            </h1>
            <p class="hero-description">
                Empower your library with cutting-edge technology. Manage books, members,
                and transactions seamlessly with our intelligent platform.
            </p>
            <div class="hero-actions">
                <a href="<%= request.getContextPath() %>/src/main/webapp/student/registration.jsp" class="btn btn-primary">
                    Start Free Trial
                    <svg class="btn-icon" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M10.293 3.293a1 1 0 011.414 0l6 6a1 1 0 010 1.414l-6 6a1 1 0 01-1.414-1.414L14.586 11H3a1 1 0 110-2h11.586l-4.293-4.293a1 1 0 010-1.414z" clip-rule="evenodd"/>
                    </svg>
                </a>
                <a href="#features" class="btn btn-secondary">
                    <svg class="btn-icon-play" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M8 5v14l11-7z"/>
                    </svg>
                    Watch Demo
                </a>
            </div>
            <div class="hero-stats">
                <div class="stat-item">
                    <span class="stat-value">100+</span>
                    <span class="stat-label">Books</span>
                </div>
                <div class="stat-item">
                    <span class="stat-value">1K+</span>
                    <span class="stat-label">Members</span>
                </div>
                <div class="stat-item">
                    <span class="stat-value">99.9%</span>
                    <span class="stat-label">Uptime</span>
                </div>
            </div>
        </div>
        <div class="hero-image">
            <div class="image-wrapper">
                <img src="https://images.unsplash.com/photo-1507842217343-583bb7270b66?w=800&h=600&fit=crop" alt="Modern Library" class="main-image">
                <div class="floating-card card-1">
                    <div class="card-icon">📚</div>
                    <div class="card-text">
                        <div class="card-title">Digital Catalog</div>
                        <div class="card-subtitle">50,000+ Books</div>
                    </div>
                </div>
                <div class="floating-card card-2">
                    <div class="card-icon">⚡</div>
                    <div class="card-text">
                        <div class="card-title">Fast Search</div>
                        <div class="card-subtitle">0.3s response</div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</section>

<!-- Features Section -->
<section class="features" id="features">
    <div class="container">
        <div class="section-header">
            <span class="section-badge">Features</span>
            <h2 class="section-title">Everything You Need in One Place</h2>
            <p class="section-description">Powerful features designed to streamline library operations and enhance user experience</p>
        </div>

        <div class="features-grid">
            <a href="../student/registration.jsp" class="feature-card">
                <div class="feature-image">
                    <img src="https://images.unsplash.com/photo-1553729459-efe14ef6055d?w=600&h=400&fit=crop" alt="User student.Registration">
                    <div class="feature-overlay">
                        <span class="feature-tag">Essential</span>
                    </div>
                </div>
                <div class="feature-content">
                    <h3 class="feature-title">Smart Registration</h3>
                    <p class="feature-description">Streamlined onboarding process with instant verification and automated member card generation.</p>
                    <div class="feature-link">
                        Learn more
                        <svg class="arrow-icon" viewBox="0 0 20 20" fill="currentColor">
                            <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
                        </svg>
                    </div>
                </div>
            </a>

            <a href="#" class="feature-card">
                <div class="feature-image">
                    <img src="https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=600&h=400&fit=crop" alt="Book Management">
                    <div class="feature-overlay">
                        <span class="feature-tag">Core</span>
                    </div>
                </div>
                <div class="feature-content">
                    <h3 class="feature-title">Book Management</h3>
                    <p class="feature-description">Advanced cataloging system with ISBN scanning, automated categorization, and inventory tracking.</p>
                    <div class="feature-link">
                        Learn more
                        <svg class="arrow-icon" viewBox="0 0 20 20" fill="currentColor">
                            <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
                        </svg>
                    </div>
                </div>
            </a>

            <a href="#" class="feature-card">
                <div class="feature-image">
                    <img src="https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=600&h=400&fit=crop" alt="Borrowing System">
                    <div class="feature-overlay">
                        <span class="feature-tag">Popular</span>
                    </div>
                </div>
                <div class="feature-content">
                    <h3 class="feature-title">Smart Borrowing</h3>
                    <p class="feature-description">QR code-based checkout with automatic due dates, renewal options, and overdue notifications.</p>
                    <div class="feature-link">
                        Learn more
                        <svg class="arrow-icon" viewBox="0 0 20 20" fill="currentColor">
                            <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
                        </svg>
                    </div>
                </div>
            </a>

            <a href="#" class="feature-card">
                <div class="feature-image">
                    <img src="https://images.unsplash.com/photo-1432821596592-e2c18b78144f?w=600&h=400&fit=crop" alt="Search Engine">
                    <div class="feature-overlay">
                        <span class="feature-tag">Advanced</span>
                    </div>
                </div>
                <div class="feature-content">
                    <h3 class="feature-title">AI-Powered Search</h3>
                    <p class="feature-description">Lightning-fast search with intelligent recommendations and natural language processing.</p>
                    <div class="feature-link">
                        Learn more
                        <svg class="arrow-icon" viewBox="0 0 20 20" fill="currentColor">
                            <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
                        </svg>
                    </div>
                </div>
            </a>

            <a href="#" class="feature-card">
                <div class="feature-image">
                    <img src="https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=600&h=400&fit=crop" alt="Analytics">
                    <div class="feature-overlay">
                        <span class="feature-tag">Premium</span>
                    </div>
                </div>
                <div class="feature-content">
                    <h3 class="feature-title">Analytics Dashboard</h3>
                    <p class="feature-description">Real-time insights with interactive charts, usage patterns, and predictive analytics.</p>
                    <div class="feature-link">
                        Learn more
                        <svg class="arrow-icon" viewBox="0 0 20 20" fill="currentColor">
                            <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
                        </svg>
                    </div>
                </div>
            </a>

            <a href="#" class="feature-card">
                <div class="feature-image">
                    <img src="https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600&h=400&fit=crop" alt="Digital Library">
                    <div class="feature-overlay">
                        <span class="feature-tag">New</span>
                    </div>
                </div>
                <div class="feature-content">
                    <h3 class="feature-title">Digital Library</h3>
                    <p class="feature-description">Access thousands of e-books and audiobooks from any device, anywhere, anytime.</p>
                    <div class="feature-link">
                        Learn more
                        <svg class="arrow-icon" viewBox="0 0 20 20" fill="currentColor">
                            <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
                        </svg>
                    </div>
                </div>
            </a>
        </div>
    </div>
</section>

<!-- Services Section -->
<section id="services" class="features" style="background:#ffffff;">
    <div class="container">
        <div class="section-header">
            <span class="section-badge">Our Services</span>
            <h2 class="section-title">Library Services</h2>
            <p class="section-description">Simple tools that help you discover, borrow, and manage books.</p>
        </div>
        <div class="features-grid">
            <div class="feature-card">
                <div class="feature-content">
                    <h3 class="feature-title">Borrow Books</h3>
                    <p class="feature-description">Search titles and borrow instantly. Track due dates and extend time when allowed.</p>
                </div>
            </div>
            <div class="feature-card">
                <div class="feature-content">
                    <h3 class="feature-title">Return & Renew</h3>
                    <p class="feature-description">Quick return process. Renew borrowed books if they are still available.</p>
                </div>
            </div>
            <div class="feature-card">
                <div class="feature-content">
                    <h3 class="feature-title">Registration</h3>
                    <p class="feature-description">Create a student account to access personalized history and notifications.</p>
                </div>
            </div>
            <div class="feature-card">
                <div class="feature-content">
                    <h3 class="feature-title">Account History</h3>
                    <p class="feature-description">Review all borrow and return activity with sortable, searchable records.</p>
                </div>
            </div>
            <div class="feature-card">
                <div class="feature-content">
                    <h3 class="feature-title">My Books</h3>
                    <p class="feature-description">See what you currently have, return directly, or extend borrowing time.</p>
                </div>
            </div>
            <div class="feature-card">
                <div class="feature-content">
                    <h3 class="feature-title">Notifications</h3>
                    <p class="feature-description">Stay informed on due dates, new arrivals, and important updates.</p>
                </div>
            </div>
        </div>
    </div>
</section>


<!-- Contact Section -->
<section id="contact" class="contact-section" style="padding:120px 2rem;background:#ffffff;">
    <div class="container">
        <div class="section-header">
            <span class="section-badge">Contact</span>
            <h2 class="section-title">Get in Touch</h2>
            <p class="section-description">Questions about LibraryMS, reporting an issue, or suggesting a feature? Reach out and the team will respond promptly.</p>
        </div>

        <div class="contact-grid" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(340px,1fr));gap:2rem;">
            <!-- Info Card -->
            <div class="contact-card" style="background:#f8fafc;border:2px solid #e5e7eb;border-radius:20px;padding:2rem;display:flex;flex-direction:column;gap:1.25rem;">
                <h3 style="margin:0;font-size:1.5rem;font-weight:700;color:#1a1a1a;">Support Channels</h3>
                <p style="color:#64748b;line-height:1.6;">Reach us by email or send a quick message with the form. We typically respond within 1 business day.</p>
                <div style="display:flex;flex-direction:column;gap:0.75rem;font-size:0.95rem;">
                    <div style="display:flex;align-items:center;gap:0.75rem;">
                        <span style="width:40px;height:40px;border-radius:12px;background:#6366f1;color:#fff;display:flex;align-items:center;justify-content:center;font-weight:700;cursor:default;user-select:none;">@</span>
                        <div>
                            <strong>Email</strong><br>
                            <a href="mailto:support@libraryms.local" style="color:#6366f1;text-decoration:none;">support@libraryms.local</a>
                        </div>
                    </div>
                    <div style="display:flex;align-items:center;gap:0.75rem;">
                        <span style="width:40px;height:40px;border-radius:12px;background:#10b981;color:#fff;display:flex;align-items:center;justify-content:center;font-weight:700;cursor:default;user-select:none;">⏱</span>
                        <div>
                            <strong>Hours</strong><br>
                            Mon–Fri | 9:00–17:00
                        </div>
                    </div>
                    <div style="display:flex;align-items:center;gap:0.75rem;">
                        <span style="width:40px;height:40px;border-radius:12px;background:#f59e0b;color:#fff;display:flex;align-items:center;justify-content:center;font-weight:700;cursor:default;user-select:none;">💬</span>
                        <div>
                            <strong>Response Time</strong><br>
                            Usually within 24h
                        </div>
                    </div>
                </div>
            </div>

            <!-- Form Card -->
            <form id="messageForm" action="${pageContext.request.contextPath}/common/sendMessage" method="post" class="contact-card"
                  style="background:#ffffff;border:2px solid #e5e7eb;border-radius:20px;padding:2rem;display:flex;flex-direction:column;gap:1rem;">

                <h3 style="margin:0 0 0.25rem;font-size:1.5rem;font-weight:700;color:#1a1a1a;">Send a Message</h3>
                <p style="color:#64748b;font-size:0.95rem;margin:0 0 1rem;">Fill in the details below and we will reply via email.</p>

                <div style="display:flex;flex-direction:column;gap:0.5rem;">
                    <label for="contactName" style="font-weight:600;font-size:0.85rem;color:#1a1a1a;">Name *</label>
                    <input id="contactName" name="name" type="text" required
                           style="padding:0.875rem 1rem;border:2px solid #e5e7eb;border-radius:12px;font:inherit;background:#f9fafb;">
                </div>

                <div style="display:flex;flex-direction:column;gap:0.5rem;">
                    <label for="contactStudentNumber" style="font-weight:600;font-size:0.85rem;color:#1a1a1a;">Student Number *</label>
                    <input id="contactStudentNumber" name="studentNumber" type="text" required
                           style="padding:0.875rem 1rem;border:2px solid #e5e7eb;border-radius:12px;font:inherit;background:#f9fafb;">
                </div>

                <div style="display:flex;flex-direction:column;gap:0.5rem;">
                    <label for="contactSubject" style="font-weight:600;font-size:0.85rem;color:#1a1a1a;">Subject *</label>
                    <input id="contactSubject" name="subject" type="text" required
                           style="padding:0.875rem 1rem;border:2px solid #e5e7eb;border-radius:12px;font:inherit;background:#f9fafb;">
                </div>

                <div style="display:flex;flex-direction:column;gap:0.5rem;">
                    <label for="contactMessage" style="font-weight:600;font-size:0.85rem;color:#1a1a1a;">Message *</label>
                    <textarea id="contactMessage" name="message" rows="6" required
                              style="padding:0.875rem 1rem;border:2px solid #e5e7eb;border-radius:12px;font:inherit;resize:vertical;background:#f9fafb;"></textarea>
                </div>

                <button type="submit"
                        style="margin-top:0.5rem;padding:1rem;border:none;border-radius:12px;font-weight:700;font-size:0.95rem;cursor:pointer;color:#fff;background:linear-gradient(135deg,#6366f1 0%,#8b5cf6 100%);box-shadow:0 10px 25px rgba(99,102,241,0.25);">
                    Send Message
                </button>

                <small style="color:#64748b;font-size:0.75rem;align-self:center;">We never share your details.</small>
            </form>
        </div>
    </div>
</section>


<!-- CTA Section -->
<section class="cta-section">
    <div class="cta-container">
        <div class="cta-content">
            <h2 class="cta-title">Ready to modernize your library?</h2>
            <p class="cta-description">Join thousands of libraries worldwide using our platform</p>
        </div>
        <div class="cta-actions">
            <a href="../student/registration.jsp" class="btn btn-white">Get Started Free</a>
            <a href="#contact" class="btn btn-outline">Contact Sales</a>
        </div>
    </div>
</section>

<!-- Footer -->
<footer class="footer">
    <div class="footer-content">
        <div class="footer-brand">
            <svg class="footer-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span>LibraryMS</span>
        </div>
        <div class="footer-links">
            <a href="#features">Features</a>
            <a href="#services">Services</a>
            <a href="#contact">Contact</a>
            <a href="#">Privacy</a>
            <a href="#">Terms</a>
            <a href="${pageContext.request.contextPath}/src/main/webapp/staff/adminLogin.jsp" class="admin-portal-link">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                    <circle cx="8.5" cy="8.5" r="1.5"></circle>
                    <polyline points="21 15 16 10 5 21"></polyline>
                </svg>
                Admin Portal
            </a>
        </div>
        <p class="footer-copyright">© 2025 LibraryMS. Built with Jakarta EE & MongoDB. All rights reserved.</p>
    </div>
</footer>

<script> window.APP_CTX = '<%= request.getContextPath() %>'; </script>
<script src="<%= request.getContextPath() %>/src/main/webapp/scripts/scripts.js"></script>
</body>
</html>