
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.mongodb.client.*, org.bson.Document, java.util.ArrayList, java.util.List" %>
<%
    // Check if admin is logged in
    Boolean adminLoggedIn = (Boolean) session.getAttribute("adminLoggedIn");
    if (adminLoggedIn == null || !adminLoggedIn) {
        response.sendRedirect(request.getContextPath() + "/admin/login");
        return;
    }

    String adminUsername = (String) session.getAttribute("adminUsername");
    String adminRole = (String) session.getAttribute("adminRole");

    // Fetch books from database
    List<Document> books = new ArrayList<>();
    try (MongoClient mongo = MongoClients.create("mongodb://localhost:27017")) {
        MongoDatabase database = mongo.getDatabase("dbLibraryMS");
        MongoCollection<Document> collection = database.getCollection("Books");
        collection.find().into(books);
    } catch (Exception e) {
        e.printStackTrace();
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Dashboard - LibraryMS</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800;900&display=swap" rel="stylesheet">
    <link href="<%= request.getContextPath() %>/css/generalStyling.css" rel="stylesheet" type="text/css">
    <script src="https://cdn.jsdelivr.net/npm/echarts@5/dist/echarts.min.js"></script>
</head>
<body class="admin-dashboard-page" data-ctx="${pageContext.request.contextPath}">
<!-- Navigation -->
<nav class="navbar admin-navbar">
    <div class="nav-container">
        <a href="<%= request.getContextPath() %>/staff/adminDashboard.jsp" class="brand">
            <svg class="brand-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span>LibraryMS Admin</span>
        </a>
        <div class="nav-menu">
            <div class="admin-user-badge">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
                <%= adminUsername %> (<%= adminRole %>)

            </div>
            <a href="<%= request.getContextPath() %>/logout" id="logout">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75" />
                </svg>
                Logout
            </a>
        </div>
    </div>
</nav>

<!-- Admin Dashboard Content -->

<!-- Toast Notification -->
<div id="toast" class="toast"></div>

<button class="btn-messages" onclick="openMessagesModal()">
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
    </svg>
</button>


<!-- Messages Modal -->
<div id="messagesModal" class="modal">
    <div class="modal-content messages-modal-content">
        <div class="messages-container">
            <!-- Left Panel - Users List -->
            <div class="messages-users-panel">
                <div class="messages-header">
                    <h2>Messages</h2>
                    <button class="close-modal" onclick="closeMessagesModal()">&times;</button>
                </div>
                <div class="messages-search">
                    <input type="text" id="userSearch" placeholder="Search students..." onkeyup="filterUsers()">
                </div>
                <div class="messages-users-list" id="usersList">
                    <!-- Dynamically populated -->
                </div>
            </div>

            <!-- Right Panel - Conversation -->
            <div class="messages-chat-panel">
                <div class="messages-chat-header" id="chatHeader">
                    <div class="empty-chat-state">
                        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
                        </svg>
                        <p>Select a student to view messages</p>
                    </div>
                </div>
                <div class="messages-chat-body" id="chatBody">
                    <!-- Messages appear here -->
                </div>
            </div>
        </div>
    </div>
</div>


<section class="admin-container">

    <!-- KPI CARDS -->
    <div class="kpi-grid">

        <div class="kpi-card">
            <div class="kpi-label">Total Copies</div>
            <div class="kpi-value" id="kpiTotalBooks">—</div>
        </div>

        <div class="kpi-card">
            <div class="kpi-label">Available Now</div>
            <div class="kpi-value" id="kpiAvailable">—</div>
            <div class="kpi-sub" id="kpiAvailabilityRate">—</div>
            <div class="kpi-progress"><span id="kpiAvailabilityBar"></span></div>
        </div>

        <div class="kpi-card">
            <div class="kpi-label">Active Loans</div>
            <div class="kpi-value" id="kpiActiveLoans">—</div>
        </div>

        <div class="kpi-card">
            <div class="kpi-label">Overdue</div>
            <div class="kpi-value" id="kpiOverdue">—</div>
        </div>

    </div>

    <!-- ACTIVITY CHART PANEL -->
    <div class="panel">
        <div class="panel-header">Borrow Activity</div>

        <div class="live-indicator">Live updating…</div>

        <div class="chart-filter-container">
            <button class="chart-filter-btn active" data-days="7">7 Days</button>
            <button class="chart-filter-btn" data-days="14">14 Days</button>
            <button class="chart-filter-btn" data-days="30">30 Days</button>
        </div>
        <div id="activityChart" style="width:100%; height:200px;"></div>
    </div>

</section>





<div class="admin-container">
    <div class="admin-header">
        <h1>📚 Book Management</h1>
        <button class="btn-add-book" onclick="openAddModal()">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
            Add New Book
        </button>
    </div>

    <!-- Books Table -->
    <div class="books-table-container">
        <table class="books-table">
            <thead>
            <tr>
                <th>Title</th>
                <th>Author</th>
                <th>ISBN</th>
                <th>Category</th>
                <th>Quantity</th>
                <th>Available</th>
                <th>Actions</th>
            </tr>
            </thead>
            <tbody>
            <% if (books.isEmpty()) { %>
            <tr>
                <td colspan="7" class="empty-state">
                    <div class="empty-icon">🕮</div>
                    <p>No books in the library yet. Click "Add New Book" to get started.</p>
                </td>
            </tr>
            <% } else {
                for (Document book : books) { %>
            <tr>
                <td><strong><%= book.getString("title") %></strong></td>
                <td><%= book.getString("author") %></td>
                <td><%= book.getString("isbn") %></td>
                <td><span class="category-badge"><%= book.getString("category") %></span></td>
                <td><%= book.getInteger("quantity", 0) %></td>
                <td><%= book.getInteger("available", 0) %></td>
                <td class="action-buttons">
                    <button class="btn-edit" onclick='openEditModal(<%= book.toJson() %>)'>
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                        </svg>
                        Edit
                    </button>
                    <button class="btn-delete" onclick="deleteBook('<%= book.getObjectId("_id").toString() %>', '<%= book.getString("title") %>')">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <polyline points="3 6 5 6 21 6"></polyline>
                            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                        </svg>
                        Delete
                    </button>
                </td>
            </tr>
            <% } } %>
            </tbody>
        </table>
    </div>
</div>

<!-- Add Book Modal -->
<div id="addModal" class="modal">
    <div class="modal-content">
        <div class="modal-header">
            <h2>Add New Book</h2>
            <button class="modal-close" onclick="closeAddModal()">&times;</button>
        </div>
        <form action="<%= request.getContextPath() %>/admin/addBook" method="post" id="addBookForm">
            <div class="form-group">
                <label class="form-label">Title <span class="required">*</span></label>
                <input type="text" name="title" class="form-input" required>
            </div>
            <div class="form-group">
                <label class="form-label">Author <span class="required">*</span></label>
                <input type="text" name="author" class="form-input" required>
            </div>
            <div class="form-group">
                <label class="form-label">ISBN <span class="required">*</span></label>
                <input type="text" name="isbn" class="form-input" required>
            </div>
            <div class="form-group select-wrapper">
                <label class="form-label" for="add_category">Category <span class="required">*</span></label>
                <select name="category" id="add_category" class="form-input custom-select" required>
                    <option value="">Select Category</option>
                    <option value="Fiction">Fiction</option>
                    <option value="Non-Fiction">Non-Fiction</option>
                    <option value="Science">Science</option>
                    <option value="Technology">Technology</option>
                    <option value="History">History</option>
                    <option value="Biography">Biography</option>
                    <option value="Reference">Reference</option>
                    <option value="Fantasy">Fantasy</option>
                    <option value="Science Fiction">Science Fiction</option>
                    <option value="Thriller">Thriller</option>
                    <option value="Adventure">Adventure</option>
                    <option value="Mystery">Mystery</option>
                    <option value="Romance">Romance</option>
                    <option value="Horror">Horror</option>
                    <option value="Philosophy">Philosophy</option>
                    <option value="Poetry">Poetry</option>
                    <option value="Art & Design">Art & Design</option>
                    <option value="Self-Help">Self-Help</option>
                    <option value="Health & Wellness">Health & Wellness</option>
                    <option value="Business & Economics">Business & Economics</option>
                    <option value="Education">Education</option>
                    <option value="Politics & Society">Politics & Society</option>
                    <option value="Religion & Spirituality">Religion & Spirituality</option>
                    <option value="Travel">Travel</option>
                    <option value="Children's Books">Children's Books</option>
                    <option value="Young Adult">Young Adult</option>
                    <option value="Comics & Graphic Novels">Comics & Graphic Novels</option>
                    <option value="Cooking & Food">Cooking & Food</option>
                    <option value="Environmental Studies">Environmental Studies</option>
                </select>
            </div>
            <div class="form-group">
                <label class="form-label">Publisher</label>
                <input type="text" name="publisher" class="form-input">
            </div>
            <div class="form-group">
                <label class="form-label">Publication Year</label>
                <input type="number" name="publicationYear" class="form-input" min="1800" max="2025">
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label class="form-label">Quantity <span class="required">*</span></label>
                    <input type="number" name="quantity" class="form-input" required min="1" value="1">
                </div>
                <div class="form-group">
                    <label class="form-label">Available <span class="required">*</span></label>
                    <input type="number" name="available" class="form-input" required min="0" value="1">
                </div>
            </div>
            <div class="form-group">
                <label class="form-label">Description</label>
                <textarea name="description" class="form-input" rows="3"></textarea>
            </div>
            <div class="modal-actions">
                <button type="button" class="btn-cancel" onclick="closeAddModal()">Cancel</button>
                <button type="submit" class="btn-submit">Add Book</button>
            </div>
        </form>
    </div>
</div>

<!-- Edit Book Modal -->
<div id="editModal" class="modal">
    <div class="modal-content">
        <div class="modal-header">
            <h2>Edit Book</h2>
            <button class="modal-close" onclick="closeEditModal()">&times;</button>
        </div>
        <form action="<%= request.getContextPath() %>/admin/updateBook" method="post" id="editBookForm">
            <input type="hidden" name="bookId" id="edit_bookId">
            <div class="form-group">
                <label class="form-label">Title <span class="required">*</span></label>
                <input type="text" name="title" id="edit_title" class="form-input" required>
            </div>
            <div class="form-group">
                <label class="form-label">Author <span class="required">*</span></label>
                <input type="text" name="author" id="edit_author" class="form-input" required>
            </div>
            <div class="form-group">
                <label class="form-label">ISBN <span class="required">*</span></label>
                <input type="text" name="isbn" id="edit_isbn" class="form-input" required>
            </div>
            <div class="form-group">
                <label class="form-label">Category <span class="required">*</span></label>
                <select name="category" id="edit_category" class="form-input" required>
                    <option value="">Select Category</option>
                    <option value="Fiction">Fiction</option>
                    <option value="Non-Fiction">Non-Fiction</option>
                    <option value="Science">Science</option>
                    <option value="Technology">Technology</option>
                    <option value="History">History</option>
                    <option value="Biography">Biography</option>
                    <option value="Reference">Reference</option>
                    <option value="Fantasy">Fantasy</option>
                    <option value="Science Fiction">Science Fiction</option>
                    <option value="Thriller">Thriller</option>
                    <option value="Adventure">Adventure</option>
                    <option value="Mystery">Mystery</option>
                    <option value="Romance">Romance</option>
                    <option value="Horror">Horror</option>
                    <option value="Philosophy">Philosophy</option>
                    <option value="Poetry">Poetry</option>
                    <option value="Art & Design">Art & Design</option>
                    <option value="Self-Help">Self-Help</option>
                    <option value="Health & Wellness">Health & Wellness</option>
                    <option value="Business & Economics">Business & Economics</option>
                    <option value="Education">Education</option>
                    <option value="Politics & Society">Politics & Society</option>
                    <option value="Religion & Spirituality">Religion & Spirituality</option>
                    <option value="Travel">Travel</option>
                    <option value="Children's Books">Children's Books</option>
                    <option value="Young Adult">Young Adult</option>
                    <option value="Comics & Graphic Novels">Comics & Graphic Novels</option>
                    <option value="Cooking & Food">Cooking & Food</option>
                    <option value="Environmental Studies">Environmental Studies</option>
                </select>
            </div>
            <div class="form-group">
                <label class="form-label">Publisher</label>
                <input type="text" name="publisher" id="edit_publisher" class="form-input">
            </div>
            <div class="form-group">
                <label class="form-label">Publication Year</label>
                <input type="number" name="publicationYear" id="edit_publicationYear" class="form-input" min="1800" max="2025">
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label class="form-label">Quantity <span class="required">*</span></label>
                    <input type="number" name="quantity" id="edit_quantity" class="form-input" required min="1">
                </div>
                <div class="form-group">
                    <label class="form-label">Available <span class="required">*</span></label>
                    <input type="number" name="available" id="edit_available" class="form-input" required min="0">
                </div>
            </div>
            <div class="form-group">
                <label class="form-label">Description</label>
                <textarea name="description" id="edit_description" class="form-input" rows="3"></textarea>
            </div>
            <div class="modal-actions">
                <button type="button" class="btn-cancel" onclick="closeEditModal()">Cancel</button>
                <button type="submit" class="btn-submit">Update Book</button>
            </div>
        </form>
    </div>
</div>

<!-- Delete Confirmation Modal -->
<div id="deleteModal" class="modal">
    <div class="modal-content modal-small">
        <div class="modal-header modal-header-danger">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
            <h2>Confirm Deletion</h2>
        </div>
        <div class="modal-body">
            <p>Are you sure you want to delete this book?</p>
            <p class="delete-book-title"></p>
            <p class="warning-text">This action cannot be undone.</p>
        </div>
        <div class="modal-actions">
            <button type="button" class="btn-cancel" onclick="closeDeleteModal()">Cancel</button>
            <button type="button" class="btn-delete-confirm" onclick="confirmDelete()">Delete Book</button>
        </div>
    </div>
</div>

<!-- Loans Activity -->
<section class="admin-container">
    <div class="admin-header">
        <h1>🕓 Loans Activity</h1>
    </div>

    <div class="books-table-container">
        <table class="books-table">
            <thead>
            <tr>
                <th>Title</th>
                <th>ISBN</th>
                <th>Borrower</th>
                <th>Borrowed</th>
                <th>Expected Return</th>
                <th>Status</th>
                <th>Actions</th>
            </tr>
            </thead>
            <tbody id="loansTableBody">
            <tr><td colspan="7" style="padding:1rem;color:#64748b;">Loading...</td></tr>
            </tbody>
        </table>
    </div>
</section>


<!-- Fines Activity -->
<section class="admin-container">
    <div class="admin-header">
        <h1>💰 Fines Activity</h1>
    </div>

    <div class="books-table-container">
        <table class="books-table">
            <thead>
            <tr>
                <th>Receipt ID</th>
                <th>Student Number</th>
                <th>Full Name</th>
                <th>Books Count</th>
                <th>Total Amount</th>
                <th>Status</th>
                <th>Payment Date</th>
                <th>Actions</th>
            </tr>
            </thead>
            <tbody id="finesTableBody">
            <tr><td colspan="8" style="padding:1rem;color:#64748b;">Loading...</td></tr>
            </tbody>
        </table>
    </div>
</section>

<!-- Adjust Fine Modal -->
<div id="adjustFineModal" class="modal">
    <div class="modal-content modal-small">
        <div class="modal-header">
            <h2>Adjust Fine Amount</h2>
            <button class="modal-close" onclick="closeAdjustFineModal()">&times;</button>
        </div>
        <form id="adjustFineForm">
            <input type="hidden" id="adjust_fineId">
            <div class="form-group">
                <label class="form-label">Current Amount</label>
                <input type="text" id="adjust_currentAmount" class="form-input" readonly>
            </div>
            <div class="form-group">
                <label class="form-label">New Amount (£) <span class="required">*</span></label>
                <input type="number" id="adjust_newAmount" class="form-input" step="0.01" min="0" required>
            </div>
            <div class="modal-actions">
                <button type="button" class="btn-cancel" onclick="closeAdjustFineModal()">Cancel</button>
                <button type="submit" class="btn-submit">Update Amount</button>
            </div>
        </form>
    </div>
</div>





<script>
    let deleteBookId = null;
    let deleteBookTitle = null;

    function showToast(message, type = 'success') {
        const toast = document.getElementById('toast');
        toast.textContent = message;
        toast.className = 'toast toast-' + type + ' toast-show';

        setTimeout(() => {
            toast.className = 'toast';
        }, 3000);
    }

    document.addEventListener('DOMContentLoaded', function() {
        // Handle Add Book Form
        const addForm = document.getElementById('addBookForm');
        addForm.addEventListener('submit', function(e) {
            e.preventDefault();

            const formData = new FormData(addForm);
            const params = new URLSearchParams(formData);

            fetch('<%= request.getContextPath() %>/admin/addBook', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: params.toString()
            })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        showToast('✓ Book added successfully!', 'success');
                        closeAddModal();
                        setTimeout(() => location.reload(), 1500);
                    } else {
                        console.error('Add failed:', data.error);
                        showToast('✗ Failed to add book: ' + (data.error || 'Unknown error'), 'error');
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    showToast('✗ Failed to add book. Please try again.', 'error');
                });
        });

        // Handle Edit Book Form
        const editForm = document.getElementById('editBookForm');
        editForm.addEventListener('submit', function(e) {
            e.preventDefault();

            const formData = new FormData(editForm);
            const params = new URLSearchParams(formData);

            fetch('<%= request.getContextPath() %>/admin/updateBook', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: params.toString()
            })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        showToast('✓ Book updated successfully!', 'success');
                        closeEditModal();
                        setTimeout(() => location.reload(), 1500);
                    } else {
                        console.error('Update failed:', data.error);
                        showToast('✗ Failed to update book: ' + (data.error || 'Unknown error'), 'error');
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    showToast('✗ Failed to update book. Please try again.', 'error');
                });
        });
    });

    function openAddModal() {
        document.getElementById('addModal').style.display = 'flex';
    }

    function closeAddModal() {
        document.getElementById('addModal').style.display = 'none';
        document.getElementById('addBookForm').reset();
    }

    function openEditModal(book) {
        document.getElementById('edit_bookId').value = book._id.$oid;
        document.getElementById('edit_title').value = book.title || '';
        document.getElementById('edit_author').value = book.author || '';
        document.getElementById('edit_isbn').value = book.isbn || '';
        document.getElementById('edit_category').value = book.category || '';
        document.getElementById('edit_publisher').value = book.publisher || '';
        document.getElementById('edit_publicationYear').value = book.publicationYear || '';
        document.getElementById('edit_quantity').value = book.quantity || 1;
        document.getElementById('edit_available').value = book.available || 0;
        document.getElementById('edit_description').value = book.description || '';
        document.getElementById('editModal').style.display = 'flex';
    }

    function closeEditModal() {
        document.getElementById('editModal').style.display = 'none';
        document.getElementById('editBookForm').reset();
    }

    function deleteBook(bookId, title) {
        deleteBookId = bookId;
        deleteBookTitle = title;
        document.querySelector('.delete-book-title').textContent = '"' + title + '"';
        document.getElementById('deleteModal').style.display = 'flex';
    }

    function closeDeleteModal() {
        document.getElementById('deleteModal').style.display = 'none';
        deleteBookId = null;
        deleteBookTitle = null;
    }

    function confirmDelete() {
        if (!deleteBookId) return;

        fetch('<%= request.getContextPath() %>/admin/deleteBook?bookId=' + deleteBookId)
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    showToast('✓ Book deleted successfully!', 'success');
                    closeDeleteModal();
                    setTimeout(() => location.reload(), 1500);
                } else {
                    console.error('Delete failed:', data.error);
                    showToast('✗ Failed to delete book: ' + (data.error || 'Unknown error'), 'error');
                    closeDeleteModal();
                }
            })
            .catch(error => {
                console.error('Error:', error);
                showToast('✗ Failed to delete book. Please try again.', 'error');
                closeDeleteModal();
            });
    }

    window.onclick = function(event) {
        const addModal = document.getElementById('addModal');
        const editModal = document.getElementById('editModal');
        const deleteModal = document.getElementById('deleteModal');

        if (event.target == addModal) {
            closeAddModal();
        }
        if (event.target == editModal) {
            closeEditModal();
        }
        if (event.target == deleteModal) {
            closeDeleteModal();
        }
    }


</script>
<script>window.APP_CTX='${pageContext.request.contextPath}';</script>
<script src="<%= request.getContextPath() %>/scripts/scripts.js"></script>





</body>
</html>