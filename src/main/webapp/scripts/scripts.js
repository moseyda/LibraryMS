(function() {
    if (typeof window.ctxPath === 'undefined') {
        window.ctxPath = (typeof window.APP_CTX !== 'undefined')
            ? window.APP_CTX
            : (document.currentScript && document.currentScript.dataset && document.currentScript.dataset.appctx) || '';
    }
})();

// Example safe fetch for books
fetch(`${ctxPath}/admin/getBooks`)
    .then(response => {
        if (!response.ok) throw new Error(response.status + ' ' + response.statusText);
        return response.json();
    })
    .then(data => {
        // handle books (data.books expected)
        console.log('Books loaded:', data);
    })
    .catch(err => {
        console.error('Failed to load books:', err);
    });

// Common utility to escape HTML
function escapeHtml(text) {
    if (text === null || text === undefined) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}


function normalizeRecord(rec) {
    // Handle id: MongoDB uses _id.$oid or _id, SQL uses record_id or id
    const id = rec.record_id || rec.recordId || rec.id || rec._id?.$oid || rec._id || '';

    // Handle bookId
    const bookId = rec.book_id || rec.bookId || '';

    // Handle dates: MongoDB wraps in {$date:...}, SQL returns plain strings
    const parseDate = (d) => {
        if (!d) return null;
        if (typeof d === 'object' && d.$date) return d.$date;
        return d;
    };

    return {
        id: String(id),
        recordId: String(id),
        bookId: String(bookId),
        title: rec.title || rec.bookTitle || '',
        isbn: rec.isbn || rec.ISBN || '',
        author: rec.author || '',
        category: rec.category || '',
        status: (rec.status || 'borrowed').toLowerCase(),
        borrowDate: parseDate(rec.borrowDate),
        expectedReturnDate: parseDate(rec.expectedReturnDate),
        actualReturnDate: parseDate(rec.actualReturnDate),
        studentNumber: rec.SNumber || rec.studentNumber || rec.student_number || '',
        firstName: rec.firstName || rec.first_name || '',
        lastName: rec.lastName || rec.last_name || '',
        available: rec.available ?? rec.available_copies ?? 0,
        quantity: rec.quantity ?? rec.total_copies ?? 0
    };
}


// Book Browsing Modal Script
document.addEventListener('DOMContentLoaded', function() {
    const modal = document.getElementById('booksModal');
    const browseLinks = document.querySelectorAll('a[href="#"]');
    const closeBtn = modal?.querySelector('.close-modal');
    const searchInput = document.getElementById('searchInput');

    let allBooks = [];

    browseLinks.forEach(link => {
        if (link.textContent.includes('Browse')) {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                modal.style.display = 'flex';
                loadBooks();
            });
        }
    });

    closeBtn?.addEventListener('click', () => {
        modal.style.display = 'none';
    });

    window.addEventListener('click', (e) => {
        if (e.target === modal) {
            modal.style.display = 'none';
        }
    });

    searchInput?.addEventListener('input', function() {
        const query = this.value.toLowerCase();
        const filtered = allBooks.filter(book => {
            return (book.title?.toLowerCase().includes(query) ||
                book.author?.toLowerCase().includes(query) ||
                book.isbn?.toLowerCase().includes(query));
        });
        renderBooks(filtered);
    });

    async function loadBooks() {
        try {
            const response = await fetch(window.APP_CTX + '/browseBooks');
            if (!response.ok) throw new Error('Failed to load books');
            allBooks = await response.json();
            renderBooks(allBooks);
        } catch (error) {
            document.getElementById('booksContainer').innerHTML =
                '<p style="text-align:center;color:#ef4444;">Error loading books</p>';
        }
    }

    function renderBooks(books) {
        const container = document.getElementById('booksContainer');
        if (books.length === 0) {
            container.innerHTML = '<p style="text-align:center;color:#64748b;">No books found</p>';
            return;
        }
        container.innerHTML = books.map(book => `
            <div class="book-card">
                <h3>${escapeHtml(book.title || 'Unknown')}</h3>
                <p><strong>Author:</strong> ${escapeHtml(book.author || 'N/A')}</p>
                <p><strong>ISBN:</strong> ${escapeHtml(book.isbn || 'N/A')}</p>
                <p><strong>Category:</strong> ${escapeHtml(book.category || 'N/A')}</p>
                <p><strong>Publisher:</strong> ${escapeHtml(book.publisher || 'N/A')}</p>
                <p><strong>Year:</strong> ${escapeHtml(String(book.publicationYear || 'N/A'))}</p>
                <p><strong>Available:</strong> ${book.available || 0} / ${book.quantity || 0}</p>
                <span class="book-status ${book.status === 'Available' ? 'available' : 'unavailable'}">
                    ${book.status || 'Unknown'}
                </span>
                <button class="borrow-btn" 
                        onclick="borrowBook('${book.isbn}', '${escapeHtml(book.title)}')"
                        ${book.status !== 'Available' ? 'disabled' : ''}>
                    ${book.status === 'Available' ? 'Borrow Book' : 'Unavailable'}
                </button>
            </div>
        `).join('');
    }

    async function borrowBook(isbn, title) {
        try {
            const response = await fetch(`${window.APP_CTX}/borrowBook`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: `isbn=${encodeURIComponent(isbn)}&title=${encodeURIComponent(title)}`
            });

            const data = await response.json();

            if (response.ok) {
                showToast('Book borrowed successfully!', 'success');

                // Re-fetch and display updated books
                const booksResponse = await fetch(`${window.APP_CTX}/browseBooks`);
                if (booksResponse.ok) {
                    allBooks = await booksResponse.json();
                    renderBooks(allBooks);
                }
            } else {
                showToast(data.error || 'Failed to borrow book', 'error');
            }
        } catch (error) {
            console.error('Error:', error);
            showToast('An error occurred while borrowing the book', 'error');
        }
    }


    window.borrowBook = borrowBook;

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
});

// My Books Modal Script
document.addEventListener('DOMContentLoaded', function() {
    const myBooksLinks = document.querySelectorAll('a[href="#"]');

    myBooksLinks.forEach(link => {
        if (link.textContent.includes('My Books')) {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                showMyBooksModal();
            });
        }
    });

    async function showMyBooksModal() {
        try {
            const response = await fetch(window.APP_CTX + '/myBooks');
            if (!response.ok) throw new Error('Failed to load borrowed books');

            const books = await response.json();
            displayMyBooksModal(books);
        } catch (error) {
            showToast('Error loading your books', 'error');
        }
    }

    function displayMyBooksModal(books) {
        const modalHTML = `
        <div id="myBooksModal" class="modal" style="display: flex;">
            <div class="modal-content">
                <div class="modal-header">
                    <h2>📖 My Borrowed Books</h2>
                    <span class="close-modal" onclick="document.getElementById('myBooksModal').remove()">&times;</span>
                </div>
                <div class="modal-body">
                    <div class="books-grid">
                        ${books.length === 0 ?
            '<p style="text-align:center;color:#64748b;">You have no borrowed books</p>' :
            books.map(book => `
                                <div class="book-card">
                                    <h3>${escapeHtml(book.title || 'Unknown')}</h3>
                                    <p><strong>ISBN:</strong> ${escapeHtml(book.isbn || 'N/A')}</p>
                                    <p><strong>Borrowed:</strong> ${new Date(book.borrowDate?.$date || book.borrowDate).toLocaleDateString()}</p>
                                    <p><strong>Expected Return:</strong> ${new Date(book.expectedReturnDate?.$date || book.expectedReturnDate).toLocaleDateString()}</p>
                                    <span class="book-status borrowed">Borrowed</span>
                                    <div style="display: flex; gap: 0.5rem; margin-top: 1rem;">
                                        <button class="return-btn" onclick="returnBook('${book._id?.$oid || book._id}')">Return</button>
                                        <button type="button" class="extend-btn" onclick="extendBorrow('${book._id?.$oid || book._id}')">Extend</button>
                                    </div>
                                </div>
                            `).join('')
        }
                    </div>
                </div>
            </div>
        </div>
    `;
        document.body.insertAdjacentHTML('beforeend', modalHTML);
    }

    window.returnBook = async function(borrowId) {
        try {
            const formData = new URLSearchParams();
            formData.append('borrowId', borrowId);

            const response = await fetch(window.APP_CTX + '/returnBook', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: formData
            });

            const result = await response.json();

            if (response.ok) {
                showToast('Book returned successfully!', 'success');
                document.getElementById('myBooksModal').remove();
            } else {
                showToast(result.error || 'Failed to return book', 'error');
            }
        } catch (error) {
            showToast('Error returning book', 'error');
        }
    };

    window.extendBorrow = async function(borrowId) {
        try {
            const formData = new URLSearchParams();
            formData.append('borrowId', borrowId);

            const response = await fetch(window.APP_CTX + '/extendBorrow', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: formData
            });

            const result = await response.json();

            if (response.ok) {
                showToast('Borrow period extended by 1 day!', 'success');
                document.getElementById('myBooksModal').remove();
            } else {
                showToast(result.error || 'Failed to extend borrow period', 'error');
            }
        } catch (error) {
            showToast('Error extending borrow period', 'error');
        }
    };

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
});

// Account History Modal Script (REPLACEMENT)
document.addEventListener('DOMContentLoaded', function () {
    const historyLinks = document.querySelectorAll('a[href="#"]');
    let accountHistoryAll = [];
    let currentSortField = 'borrowDate';
    let currentSortDir = 'desc';

    historyLinks.forEach(link => {
        if (link.textContent.includes('Account History')) {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                openHistoryModal();
            });
        }
    });

    async function fetchHistory(sortField = 'borrowDate', dir = 'desc') {
        try {
            const url = `${window.APP_CTX}/accountHistory?sort=${encodeURIComponent(sortField)}&dir=${encodeURIComponent(dir)}`;
            const response = await fetch(url);
            if (!response.ok) throw new Error('Failed to load account history');
            accountHistoryAll = await response.json();
            renderHistoryRows(accountHistoryAll);
        } catch (error) {
            showToast('Error loading account history', 'error');
            const body = document.getElementById('historyTableBody');
            if (body) body.innerHTML = '<tr><td colspan="6" style="padding:1rem;color:#ef4444;">Load error</td></tr>';
        }
    }

    function openHistoryModal() {
        // Remove existing modal if open
        document.getElementById('historyModal')?.remove();

        const modalHTML = `
        <div id="historyModal" class="modal" style="display:flex;">
          <div class="modal-content" style="max-width:1200px;">
            <div class="modal-header">
              <h2>📋 Account History</h2>
              <span class="close-modal" onclick="document.getElementById('historyModal').remove()">&times;</span>
            </div>
            <div class="modal-body">
              <div class="history-controls" style="display:flex;flex-wrap:wrap;gap:0.75rem;padding:0 1rem 1rem;">
                  <input id="historySearch" type="text" placeholder="Search title / ISBN"
                         style="flex:1;min-width:220px;padding:0.5rem 0.75rem;border:2px solid #e5e7eb;border-radius:8px;">
                  <select id="historyStatusFilter"
                          style="padding:0.5rem 0.75rem;border:2px solid #e5e7eb;border-radius:8px;">
                      <option value="">All Statuses</option>
                      <option value="borrowed">Borrowed</option>
                      <option value="returned">Returned</option>
                      <option value="overdue">Overdue</option>
                  </select>
                  <select id="historySortField"
                          style="padding:0.5rem 0.75rem;border:2px solid #e5e7eb;border-radius:8px;">
                      <option value="borrowDate">Borrow Date</option>
                      <option value="expectedReturnDate">Expected Return</option>
                      <option value="actualReturnDate">Actual Return</option>
                      <option value="title">Title</option>
                      <option value="status">Status</option>
                  </select>
                  <button id="historySortDir" data-dir="desc"
                          style="padding:0.5rem 0.75rem;border:2px solid #e5e7eb;border-radius:8px;background:#fff;cursor:pointer;">
                      Desc
                  </button>
              </div>
              <div style="overflow-x:auto;">
                <table style="width:100%;border-collapse:collapse;">
                  <thead style="background:#f8fafc;border-bottom:2px solid #e5e7eb;">
                    <tr>
                      <th style="padding:1rem;text-align:left;font-weight:600;">Book Title</th>
                      <th style="padding:1rem;text-align:left;font-weight:600;">ISBN</th>
                      <th style="padding:1rem;text-align:left;font-weight:600;">Borrowed</th>
                      <th style="padding:1rem;text-align:left;font-weight:600;">Expected Return</th>
                      <th style="padding:1rem;text-align:left;font-weight:600;">Actual Return</th>
                      <th style="padding:1rem;text-align:left;font-weight:600;">Status</th>
                    </tr>
                  </thead>
                  <tbody id="historyTableBody">
                    <tr><td colspan="6" style="padding:1rem;color:#64748b;">Loading...</td></tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>`;
        document.body.insertAdjacentHTML('beforeend', modalHTML);

        // Attach events
        document.getElementById('historySearch').addEventListener('input', filterAndRender);
        document.getElementById('historyStatusFilter').addEventListener('change', filterAndRender);
        document.getElementById('historySortField').addEventListener('change', (e) => {
            currentSortField = e.target.value;
            fetchHistory(currentSortField, currentSortDir);
        });
        document.getElementById('historySortDir').addEventListener('click', (e) => {
            currentSortDir = e.target.getAttribute('data-dir') === 'desc' ? 'asc' : 'desc';
            e.target.setAttribute('data-dir', currentSortDir);
            e.target.textContent = currentSortDir === 'desc' ? 'Desc' : 'Asc';
            fetchHistory(currentSortField, currentSortDir);
        });

        fetchHistory(currentSortField, currentSortDir);
    }

    function filterAndRender() {
        const q = document.getElementById('historySearch').value.trim().toLowerCase();
        const statusVal = document.getElementById('historyStatusFilter').value;
        const filtered = accountHistoryAll.filter(r => {
            const title = (r.title || '').toLowerCase();
            const isbn = (r.isbn || '').toLowerCase();
            const status = (r.status || '').toLowerCase();
            const matchesQuery = !q || title.includes(q) || isbn.includes(q);
            const matchesStatus = !statusVal || status === statusVal;
            return matchesQuery && matchesStatus;
        });
        renderHistoryRows(filtered);
    }

    // admin-loansActivity fetch section
    function renderHistoryRows(records) {
        const body = document.getElementById('historyTableBody');
        if (!body) return;

        if (records.length === 0) {
            body.innerHTML = '<tr><td colspan="6" style="padding:1rem;color:#64748b;">No history found</td></tr>';
            return;
        }

        body.innerHTML = records.map(record => {
            const rawStatus = (record.status || '').toLowerCase();
            const uiStatus = ['borrowed', 'returned', 'overdue'].includes(rawStatus)
                ? rawStatus
                : 'borrowed';

            const label =
                uiStatus === 'overdue' ? 'Overdue' :
                    uiStatus === 'returned' ? 'Returned' :
                        'Borrowed';

            const cssClass =
                uiStatus === 'overdue' ? 'overdue' :
                    uiStatus === 'returned' ? 'returned' :
                        'borrowed';

            const actualReturnRaw = record.actualReturnDate?.$date || record.actualReturnDate;

            return `
            <tr style="border-bottom:1px solid #f1f5f9;">
              <td style="padding:1rem;">${escapeHtml(record.title || 'Unknown')}</td>
              <td style="padding:1rem;">${escapeHtml(record.isbn || 'N/A')}</td>
              <td style="padding:1rem;">${formatDateTime(record.borrowDate?.$date || record.borrowDate)}</td>
              <td style="padding:1rem;">${formatDateTime(record.expectedReturnDate?.$date || record.expectedReturnDate)}</td>
              <td style="padding:1rem;">${actualReturnRaw ? formatDateTime(actualReturnRaw) : 'Not returned'}</td>
              <td style="padding:1rem;">
                <span class="book-status ${cssClass}">
                  ${label}
                </span>
              </td>
            </tr>`;
        }).join('');
    }


    function formatDateTime(date) {
        if (!date) return 'N/A';
        const d = new Date(date);
        return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
});

// Helper: safe parse JSON even if response body is empty or invalid
async function safeParseJson(response) {
    try {
        const text = await response.text();
        if (!text) return null;
        return JSON.parse(text);
    } catch (e) {
        console.warn('safeParseJson: invalid JSON', e);
        return null;
    }
}

// Helper: get an ID string from various shapes (Mongo {$oid}, plain string, SQL id fields)
function getIdString(obj) {
    if (!obj && obj !== 0) return '';
    // if object with $oid
    if (typeof obj === 'object') {
        if (obj.$oid) return String(obj.$oid);
        // if object already a string-like (rare)
        if (obj.toString && obj.toString() !== '[object Object]') return String(obj);
        return '';
    }
    return String(obj);
}

// Helper: short display id prefix (safe substring)
function getShortId(obj, len = 8) {
    const id = getIdString(obj);
    return id ? id.substring(0, Math.min(len, id.length)).toUpperCase() : '';
}

// admin-loans.js
document.addEventListener('DOMContentLoaded', () => {
    const ctx = window.APP_CTX || '';
    if (!document.getElementById('loansTableBody')) return;

    loadLoans();

    async function loadLoans(status = '') {
        const body = document.getElementById('loansTableBody');
        if (!body) return;
        body.innerHTML = `<tr><td colspan="7" style="padding:1rem;color:#64748b;">Loading...</td></tr>`;

        try {
            const url = `${ctx}/admin/loansActivity${status ? `?status=${encodeURIComponent(status)}` : ''}`;
            const res = await fetch(url);
            if (!res.ok) throw new Error('Failed to fetch loans: ' + res.status);

            const data = await safeParseJson(res) || [];
            const rawRecords = Array.isArray(data) ? data : (data.loans || data.records || []);
            const records = (rawRecords || []).map(normalizeRecord);

            renderLoans(records);
        } catch (e) {
            console.error('loadLoans error:', e);
            body.innerHTML = `<tr><td colspan="7" style="padding:1rem;color:#ef4444;">Error loading loans</td></tr>`;
        }
    }

    function renderLoans(records) {
        const body = document.getElementById('loansTableBody');
        if (!body) return;

        if (!records || records.length === 0) {
            body.innerHTML = `<tr><td colspan="7" style="padding:1rem;color:#64748b;">No loan records</td></tr>`;
            return;
        }

        body.innerHTML = records.map(r => {
            const borrower = [r.firstName, r.lastName].filter(Boolean).join(' ') || 'N/A';
            const sNum = r.studentNumber ? ` <small style="color:#6b7280;">(${escapeHtml(r.studentNumber)})</small>` : '';
            const borrowed = formatDateTime(r.borrowDate);
            const expected = formatDateTime(r.expectedReturnDate);
            const returned = r.actualReturnDate ? formatDateTime(r.actualReturnDate) : null;

            const uiStatus = ['borrowed', 'returned', 'overdue'].includes(r.status) ? r.status : 'borrowed';
            const isBorrowedLike = uiStatus === 'borrowed' || uiStatus === 'overdue';

            const cssClass = uiStatus === 'overdue' ? 'overdue' : uiStatus === 'returned' ? 'returned' : 'borrowed';
            const label = uiStatus === 'overdue' ? 'Overdue' : uiStatus === 'returned' ? 'Returned' : 'Borrowed';

            const statusBadge = `<span class="book-status ${cssClass}">${label}</span>` +
                (returned ? `<div style="font-size:0.75rem;color:#6b7280;margin-top:0.25rem;">Returned: ${returned}</div>` : '');

            // Use recordId for Mongo, bookId for SQL — send both for compatibility
            const actions = isBorrowedLike
                ? `<button class="btn-delete" 
                           data-record-id="${escapeHtml(r.recordId)}"
                           data-book-id="${escapeHtml(r.bookId)}"
                           onclick="adminReturnBook('${escapeHtml(r.recordId || r.bookId)}', '${escapeHtml(r.bookId)}')">
                       Mark Returned
                   </button>`
                : `<span style="color:#9ca3af;">—</span>`;

            return `
            <tr>
              <td style="padding:1rem;">${escapeHtml(r.title || 'Unknown')}</td>
              <td style="padding:1rem;">${escapeHtml(r.isbn || 'N/A')}</td>
              <td style="padding:1rem;">${escapeHtml(borrower)}${sNum}</td>
              <td style="padding:1rem;">${borrowed}</td>
              <td style="padding:1rem;">${expected}</td>
              <td style="padding:1rem;">${statusBadge}</td>
              <td style="padding:1rem;">${actions}</td>
            </tr>`;
        }).join('');
    }

    window.adminReturnBook = async function(recordId, bookId) {
        try {
            const params = new URLSearchParams();
            // Send both params for SQL/Mongo compatibility
            if (recordId) params.append('borrowId', recordId);
            if (bookId) params.append('bookId', bookId);

            const res = await fetch(`${ctx}/returnBook`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: params.toString()
            });
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || 'Failed to mark returned');
            if (window.showToast) showToast('Book marked as returned', 'success');
            loadLoans();
        } catch (e) {
            console.error('adminReturnBook error:', e);
            if (window.showToast) showToast('Error marking as returned', 'error');
        }
    };

    window.loadLoans = loadLoans;

    function escapeHtml(text) {
        const d = document.createElement('div');
        d.textContent = text ?? '';
        return d.innerHTML;
    }

    function formatDateTime(date) {
        if (!date) return 'N/A';
        const d = new Date(date);
        if (isNaN(d.getTime())) return 'N/A';
        return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
});




document.addEventListener('DOMContentLoaded', function () {
    if (!document.body.classList.contains('admin-dashboard-page')) return;

    const ctxPath = window.APP_CTX || '';

    const els = {
        total: document.getElementById('kpiTotalBooks'),
        available: document.getElementById('kpiAvailable'),
        active: document.getElementById('kpiActiveLoans'),
        overdue: document.getElementById('kpiOverdue'),
        rateText: document.getElementById('kpiAvailabilityRate'),
        rateBar: document.getElementById('kpiAvailabilityBar'),
        chart: document.getElementById('activityChart')
    };

    let activityChart = null;
    let selectedDays = 7;


    function normalizeBook(b) {
        return {
            id: b.book_id || b.bookId || b._id || '',
            title: b.title || '',
            author: b.author || '',
            isbn: b.isbn || b.ISBN || '',
            category: b.category || '',
            available: parseInt(b.available ?? 0, 10),
            quantity: parseInt(b.quantity ?? 0, 10)
        };
    }

    function normalizeLoan(rec) {
        const parseDate = (d) => {
            if (!d) return null;
            if (typeof d === 'object' && d.$date) return new Date(d.$date);
            return new Date(d);
        };

        return {
            id: rec.record_id || rec.recordId || rec.id || rec._id?.$oid || rec._id || '',
            bookId: rec.book_id || rec.bookId || '',
            title: rec.title || rec.bookTitle || '',
            status: (rec.status || 'borrowed').toLowerCase(),
            borrowDate: parseDate(rec.borrowDate || rec.borrow_date),
            expectedReturnDate: parseDate(rec.expectedReturnDate || rec.expected_return_date),
            actualReturnDate: parseDate(rec.actualReturnDate || rec.actual_return_date)
        };
    }


    fetchDataAndRender();

    async function fetchDataAndRender() {
        try {
            const [booksRes, loansRes] = await Promise.all([
                fetch(`${ctxPath}/admin/getBooks`),
                fetch(`${ctxPath}/admin/loansActivity`)
            ]);

            if (!booksRes.ok) {
                console.error('Books fetch failed:', booksRes.status);
                return;
            }
            if (!loansRes.ok) {
                console.error('Loans fetch failed:', loansRes.status);
                return;
            }

            const booksData = await safeParseJson(booksRes);
            const loansData = await safeParseJson(loansRes);

            const rawBooks = Array.isArray(booksData) ? booksData : (booksData && (booksData.books || booksData.data)) || [];
            const rawLoans = Array.isArray(loansData) ? loansData : (loansData && (loansData.loans || loansData.records || loansData.data)) || [];

            const books = rawBooks.map(normalizeBook);
            const loans = rawLoans.map(normalizeLoan);

            window.__lastLoansData = loans;

            const totalCopies = books.reduce((sum, b) => sum + (b.quantity || 0), 0);
            const availableCopies = books.reduce((sum, b) => sum + (b.available || 0), 0);
            const activeLoans = loans.filter(l => l.status === 'borrowed' || l.status === 'overdue');
            const overdueLoans = loans.filter(l => l.status === 'overdue');

            setNum(els.total, books.length);
            setNum(els.available, availableCopies);
            setNum(els.active, activeLoans.length);
            setNum(els.overdue, overdueLoans.length);

            const rate = totalCopies > 0 ? Math.round((availableCopies / totalCopies) * 100) : 0;
            if (els.rateText) els.rateText.textContent = `${rate}% available`;
            if (els.rateBar) els.rateBar.style.width = `${rate}%`;

            drawActivityChart(loans, selectedDays);
        } catch (err) {
            console.error('Failed to fetch dashboard data:', err);
        }
    }

    function setNum(el, n) {
        if (el) el.textContent = Number(n || 0).toLocaleString();
    }

    // Chart filter buttons
    document.querySelectorAll('.chart-filter-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.chart-filter-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            selectedDays = parseInt(btn.dataset.days || '7', 10);
            if (window.__lastLoansData) {
                drawActivityChart(window.__lastLoansData, selectedDays);
            }
        });
    });

    function drawActivityChart(loans, daysRange = 7) {
        if (!els.chart || typeof echarts === 'undefined') return;

        const today = new Date();
        today.setHours(0, 0, 0, 0);

        const days = Array.from({ length: daysRange }, (_, i) => {
            const d = new Date(today);
            d.setDate(today.getDate() - (daysRange - 1 - i));
            return d;
        });

        const borrowsCounts = days.map(d => {
            return loans.filter(l => {
                if (!l.borrowDate) return false;
                const bd = new Date(l.borrowDate);
                return bd.toDateString() === d.toDateString();
            }).length;
        });

        const returnsCounts = days.map(d => {
            return loans.filter(l => {
                if (!l.actualReturnDate) return false;
                const rd = new Date(l.actualReturnDate);
                return rd.toDateString() === d.toDateString();
            }).length;
        });

        const labels = days.map(d => `${d.getMonth() + 1}/${d.getDate()}`);

        if (!activityChart) {
            activityChart = echarts.init(els.chart);
        }

        const option = {
            tooltip: { trigger: 'axis' },
            legend: { data: ['Borrows', 'Returns'], bottom: 0 },
            xAxis: { type: 'category', data: labels },
            yAxis: { type: 'value', minInterval: 1 },
            series: [
                { name: 'Borrows', type: 'line', smooth: true, data: borrowsCounts, itemStyle: { color: '#6366f1' } },
                { name: 'Returns', type: 'line', smooth: true, data: returnsCounts, itemStyle: { color: '#22c55e' } }
            ]
        };

        activityChart.clear();
        activityChart.resize();
        activityChart.setOption(option);
    }
});


// Toast Notification Function
function showToast(message, type = 'success') {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    document.body.appendChild(toast);

    setTimeout(() => {
        toast.classList.add('toast-show');
    }, 100);

    setTimeout(() => {
        toast.classList.remove('toast-show');
        setTimeout(() => {
            toast.remove();
        }, 300);
    }, 3000);
}

document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('messageForm');
    if (!form) return;
    const submitBtn = form.querySelector('button[type="submit"]');
    form.addEventListener('submit', function(e) {
        e.preventDefault();
        if (submitBtn) submitBtn.disabled = true; // Disable button
        const data = new URLSearchParams(new FormData(form));
        fetch(window.APP_CTX + '/common/sendMessage', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: data
        })
            .then(res => res.json())
            .then(res => {
                if (res.success) {
                    showToast('Message sent successfully!', 'success');
                    form.reset();
                } else {
                    showToast(res.error, 'error');
                }
            })
            .catch(() => showToast('Message sending failed', 'error'))
            .finally(() => {
                if (submitBtn) submitBtn.disabled = false;
            });
    });
});


let currentMessages = [];
let currentUser = null;

// ------------------------
// Modal Open / Close
// ------------------------

async function openMessagesModal() {
    document.getElementById('messagesModal').style.display = 'flex';
    document.body.classList.add("no-scroll");
    await loadUsers();
}

function closeMessagesModal() {
    document.getElementById('messagesModal').style.display = 'none';
    document.body.classList.remove("no-scroll");
    currentUser = null;
}

// ------------------------
// Load & Display Users
// ------------------------

async function loadUsers() {
    try {
        const response = await fetch(`${window.APP_CTX}/common/getMessages`);
        const data = await response.json();

        if (data.success) {
            currentMessages = data.messages;
            displayUsers();
        }
    } catch (error) {
        console.error('Error loading messages:', error);
    }
}

function displayUsers() {
    const usersList = document.getElementById('usersList');
    const grouped = {};

    currentMessages.forEach(msg => {
        if (!grouped[msg.studentNumber]) {
            grouped[msg.studentNumber] = {
                name: msg.name,
                studentNumber: msg.studentNumber,
                count: 0,
                messages: []
            };
        }
        grouped[msg.studentNumber].count++;
        grouped[msg.studentNumber].messages.push(msg);
    });

    usersList.innerHTML = Object.values(grouped).map(user => `
        <div class="user-item" data-id="${user.studentNumber}">
            <div class="user-avatar">${user.name.charAt(0).toUpperCase()}</div>
            <div class="user-info">
                <div class="user-name">${user.name}</div>
                <div class="user-student-number">${user.studentNumber}</div>
            </div>
            <span class="user-badge">${user.count}</span>
        </div>
    `).join('');
}

// ------------------------
// Selecting a User
// ------------------------

function selectUser(studentNumber, element) {
    currentUser = studentNumber;

    document.querySelectorAll('.user-item')
        .forEach(item => item.classList.remove('active'));

    element.classList.add('active');

    const userMessages = currentMessages.filter(m => m.studentNumber === studentNumber);
    const user = userMessages[0];

    document.getElementById('chatHeader').innerHTML = `
        <div class="chat-header-content">
            <button id="chatBackBtn" class="chat-back-btn" onclick="resetChatState()">
                <svg width="24" height="24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M15 18l-6-6 6-6"/>
                </svg>
            </button>
            <div class="chat-user-avatar">${user.name.charAt(0).toUpperCase()}</div>
            <div class="chat-user-info">
                <h3>${user.name}</h3>
                <p>Student: ${user.studentNumber}</p>
            </div>
        </div>
    `;

    document.getElementById('chatBody').innerHTML = userMessages.map(msg => `
        <div class="message-item">
            <div class="message-subject">${msg.subject}</div>
            <div class="message-text">${msg.message}</div>
            <div class="message-meta">
                <span class="message-time">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="10"></circle>
                        <polyline points="12 6 12 12 16 14"></polyline>
                    </svg>
                    ${new Date(msg.timestamp).toLocaleString()}
                </span>
            </div>
        </div>
    `).join('');
}

const backBtn = document.getElementById('chatBackBtn');
if (backBtn) {
    backBtn.style.display = 'flex';
}


function resetChatState() {
    const chatBody = document.querySelector('.messages-chat-body');
    const chatHeader = document.querySelector('.messages-chat-header');
    const backBtn = document.getElementById('chatBackBtn');

    // Hide back button
    if (backBtn) {
        backBtn.style.display = 'none';
    }

    // Reset to empty state
    chatHeader.innerHTML = '<div class="empty-chat-state"><svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg><p>Select a student to view messages</p></div>';
    chatBody.innerHTML = '';

    // Remove active class from all users
    document.querySelectorAll('.user-item').forEach(item => {
        item.classList.remove('active');
    });
}

// ------------------------
// Search users
// ------------------------

function filterUsers() {
    const search = document.getElementById('userSearch').value.toLowerCase();
    const items = document.querySelectorAll('.user-item');

    items.forEach(item => {
        const name = item.querySelector('.user-name').textContent.toLowerCase();
        const number = item.querySelector('.user-student-number').textContent.toLowerCase();
        item.style.display = (name.includes(search) || number.includes(search)) ? 'flex' : 'none';
    });
}

// ------------------------
// Event Listeners
// ------------------------

document.addEventListener('DOMContentLoaded', () => {

    // Open modal button
    const openBtn = document.getElementById('openMessagesBtn');
    if (openBtn) {
        openBtn.addEventListener('click', openMessagesModal);
    }

    // Search input
    document.getElementById('userSearch')
        .addEventListener('input', filterUsers);

    // Delegate clicks on user list
    document.getElementById('usersList')
        .addEventListener('click', e => {
            const item = e.target.closest('.user-item');
            if (!item) return;
            const id = item.getAttribute('data-id');
            selectUser(id, item);
        });
});


// ==========================================
// FINES MANAGEMENT
// ==========================================

let selectedOverdueLoans = [];

function checkOverdueBooks() {
    fetch(`${window.APP_CTX}/student/getOverdueLoans`)
        .then(response => response.json())
        .then(data => {
            if (data.success && data.overdueLoans && data.overdueLoans.length > 0) {
                showFinesNotification(data.overdueLoans.length, data.overdueLoans.length * 10);
            }
        })
        .catch(error => console.error('Error checking overdue books:', error));
}

function showFinesNotification(count, total) {
    const notification = document.getElementById('finesNotification');
    if (notification) {
        document.getElementById('overdueCount').textContent = count;
        document.getElementById('totalFine').textContent = total;
        notification.style.display = 'block';
    }
}

function dismissFinesNotification() {
    const notification = document.getElementById('finesNotification');
    if (notification) {
        notification.style.display = 'none';
    }
}

function openFinesModal() {
    document.getElementById('finesModal').style.display = 'flex';
    document.body.classList.add('no-scroll');
    loadOverdueBooks();
    switchFinesTab('overdue');
}

function closeFinesModal() {
    document.getElementById('finesModal').style.display = 'none';
    document.body.classList.remove('no-scroll');
    selectedOverdueLoans = [];
}

function switchFinesTab(tabName) {
    // Update tab buttons
    document.querySelectorAll('.fines-tab').forEach(tab => {
        tab.classList.remove('active');
        if (tab.dataset.tab === tabName) {
            tab.classList.add('active');
        }
    });

    // Update tab content
    document.querySelectorAll('.fines-tab-content').forEach(content => {
        content.classList.remove('active');
    });

    if (tabName === 'overdue') {
        document.getElementById('overdueTab').classList.add('active');
        loadOverdueBooks();
    } else if (tabName === 'history') {
        document.getElementById('historyTab').classList.add('active');
        loadPaymentHistory();
    }
}

function loadOverdueBooks() {
    const container = document.getElementById('overdueBooksList');
    container.innerHTML = '<div class="loading" style="padding:2rem;text-align:center;color:#64748b;">Loading overdue books...</div>';

    fetch(`${window.APP_CTX}/student/getOverdueLoans`)
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                if (data.overdueLoans.length === 0) {
                    container.innerHTML = `
                        <div class="empty-state-fines">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                            </svg>
                            <h3>No Overdue Books</h3>
                            <p>You don't have any overdue books at the moment.</p>
                        </div>
                    `;
                } else {
                    container.innerHTML = data.overdueLoans.map(loan => `
                        <div class="overdue-book-item" data-loan-id="${loan.loanId}">
                            <input type="checkbox" class="overdue-book-checkbox" 
                                   onchange="toggleOverdueBookSelection('${loan.loanId}', ${loan.fine})">
                            <div class="overdue-book-info">
                                <div class="overdue-book-title">${loan.title}</div>
                                <div class="overdue-book-details">ISBN: ${loan.isbn}</div>
                                <div class="overdue-book-dates">
                                    <div class="overdue-date-item">
                                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                                            <line x1="16" y1="2" x2="16" y2="6"></line>
                                            <line x1="8" y1="2" x2="8" y2="6"></line>
                                            <line x1="3" y1="10" x2="21" y2="10"></line>
                                        </svg>
                                        <span class="overdue-date-label">Due:</span>
                                        <span class="overdue-date-value overdue">
                                            ${formatDateTime(loan.expectedReturnDate)}
                                        </span>
                                    </div>
                                </div>
                            </div>
                            <div class="overdue-book-fine">£${loan.fine.toFixed(2)}</div>
                        </div>
                    `).join('');
                }
                updateFinesSummary();
            } else {
                container.innerHTML = '<div style="padding:2rem;text-align:center;color:#dc2626;">Error loading overdue books</div>';
            }
        })
        .catch(error => {
            console.error('Error:', error);
            container.innerHTML = '<div style="padding:2rem;text-align:center;color:#dc2626;">Failed to load overdue books</div>';
        });
}

function toggleOverdueBookSelection(loanId, fine) {
    const checkbox = document.querySelector(`[data-loan-id="${loanId}"] .overdue-book-checkbox`);
    const item = document.querySelector(`[data-loan-id="${loanId}"]`);

    if (checkbox.checked) {
        selectedOverdueLoans.push({ loanId, fine });
        item.classList.add('selected');
    } else {
        selectedOverdueLoans = selectedOverdueLoans.filter(loan => loan.loanId !== loanId);
        item.classList.remove('selected');
    }

    updateFinesSummary();
}

function updateFinesSummary() {
    const count = selectedOverdueLoans.length;
    const total = selectedOverdueLoans.reduce((sum, loan) => sum + loan.fine, 0);

    document.getElementById('selectedCount').textContent = count;
    document.getElementById('selectedTotal').textContent = total.toFixed(2);

    const payBtn = document.getElementById('btnPayFines');
    payBtn.disabled = count === 0;
}

function processFinePayment() {
    if (selectedOverdueLoans.length === 0) {
        showToast('Please select at least one book to pay fines for', 'error');
        return;
    }

    const formData = new FormData();
    selectedOverdueLoans.forEach(loan => {
        formData.append('loanIds[]', loan.loanId);
    });

    fetch(`${window.APP_CTX}/student/processFinePayment`, {
        method: 'POST',
        body: formData
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showToast('✓ Payment processed successfully!', 'success');
                selectedOverdueLoans = [];
                checkOverdueBooks();
                switchFinesTab('history');
            } else {
                showToast('✗ Payment failed: ' + (data.error || 'Unknown error'), 'error');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('✗ Payment processing failed', 'error');
        });
}

function loadPaymentHistory() {
    const container = document.getElementById('paymentHistoryList');
    container.innerHTML = '<div class="loading" style="padding:2rem;text-align:center;color:#64748b;">Loading payment history...</div>';

    fetch(`${window.APP_CTX}/student/getPaymentHistory`)
        .then(response => response.json().catch(() => null))
        .then(data => {
            if (data && data.success) {
                if (!data.payments || data.payments.length === 0) {
                    container.innerHTML = `
                        <div class="empty-state-fines">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"></path>
                            </svg>
                            <h3>No Payment History</h3>
                            <p>You haven't made any fine payments yet.</p>
                        </div>
                    `;
                } else {
                    container.innerHTML = data.payments.map(payment => {
                        const rid = getShortId(payment._id || payment.id || payment._oid);
                        const status = payment.status || 'unknown';
                        const total = typeof payment.totalAmount === 'number' ? payment.totalAmount : (payment.totalAmount ? Number(payment.totalAmount) : 0);
                        const booksHtml = (Array.isArray(payment.books) ? payment.books : []).map(b => `<div class="receipt-book-item"><svg ...></svg><span>${escapeHtml(b.title || '')} (${escapeHtml(b.isbn || '')})</span></div>`).join('');
                        return `
                            <div class="payment-history-item">
                                <div class="payment-receipt-header">
                                    <div class="receipt-id">Receipt #${rid}</div>
                                    <span class="receipt-status ${escapeHtml(status)}">${escapeHtml(String(status).toUpperCase())}</span>
                                </div>
                                <div class="receipt-details">
                                    <div class="receipt-detail-row">
                                        <span class="receipt-detail-label">Payment Date:</span>
                                        <span class="receipt-detail-value">${formatDateTime(payment.actualPaymentDate)}</span>
                                    </div>
                                    <div class="receipt-detail-row">
                                        <span class="receipt-detail-label">Student Number:</span>
                                        <span class="receipt-detail-value">${escapeHtml(payment.studentNumber || '')}</span>
                                    </div>
                                    <div class="receipt-detail-row">
                                        <span class="receipt-detail-label">Full Name:</span>
                                        <span class="receipt-detail-value">${escapeHtml(payment.fullName || '')}</span>
                                    </div>
                                </div>
                                <div class="receipt-books-list">
                                    <div class="receipt-books-title">Books Paid For:</div>
                                    ${booksHtml}
                                </div>
                                <div class="receipt-total">
                                    <span class="receipt-total-label">Total Paid:</span>
                                    <span class="receipt-total-amount">£${(total).toFixed(2)}</span>
                                </div>
                            </div>
                        `;
                    }).join('');
                }
            } else {
                container.innerHTML = '<div style="padding:2rem;text-align:center;color:#dc2626;">Error loading payment history</div>';
            }
        })
        .catch(error => {
            console.error('Error:', error);
            container.innerHTML = '<div style="padding:2rem;text-align:center;color:#dc2626;">Failed to load payment history</div>';
        });
}

function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return 'N/A';
    const date = new Date(dateTimeStr);
    return date.toLocaleDateString('en-GB', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Initialise fines check on student dashboard load
if (document.querySelector('.dashboard-container')) {
    document.addEventListener('DOMContentLoaded', function() {
        checkOverdueBooks();
    });
}


// Load Fines Activity
function loadFinesActivity() {
    fetch(`${window.APP_CTX}/admin/getFinesActivity`)
        .then(res => res.ok ? res.json().catch(() => null) : Promise.reject('bad response'))
        .then(data => {
            const tbody = document.getElementById('finesTableBody');
            if (!data || !data.success || !Array.isArray(data.fines) || data.fines.length === 0) {
                tbody.innerHTML = '<tr><td colspan="8" class="empty-state">No fines recorded yet</td></tr>';
                return;
            }

            tbody.innerHTML = data.fines.map(fine => {
                const shortId = getShortId(fine._id || fine.id || fine.fine_id);
                const receiptId = shortId ? `#${shortId}` : '#UNKNOWN';
                const totalAmount = typeof fine.totalAmount === 'number' ? fine.totalAmount : (fine.totalAmount ? Number(fine.totalAmount) : 0);
                const status = fine.status || 'unknown';

                // prepare safe JSON-stringified object for adjust button without throwing
                let finePayload = {};
                try { finePayload = fine; } catch (e) { finePayload = { id: fine.id || fine._id }; }

                return `
                    <tr>
                        <td>${receiptId}</td>
                        <td>${escapeHtml(fine.studentNumber || '')}</td>
                        <td><strong>${escapeHtml(fine.fullName || '')}</strong></td>
                        <td>${Array.isArray(fine.books) ? fine.books.length : (fine.books ? String(fine.books).length : 0)} book(s)</td>
                        <td><strong>£${(totalAmount).toFixed(2)}</strong></td>
                        <td><span class="category-badge">${escapeHtml(status)}</span></td>
                        <td>${formatDateTime(fine.actualPaymentDate)}</td>
                        <td>
                            <button class="btn-edit" onclick='openAdjustFineModal(${escapeHtml(JSON.stringify(finePayload))})'>
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <path d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                                </svg>
                                Adjust
                            </button>
                        </td>
                    </tr>
                `;
            }).join('');
        })
        .catch(error => {
            console.error('Error loading fines:', error);
            document.getElementById('finesTableBody').innerHTML =
                '<tr><td colspan="8" style="padding:1rem;color:#dc2626;">Failed to load fines</td></tr>';
        });
}

function openAdjustFineModal(fine) {
    // fine may be an object or a JSON string from onclick; ensure object
    let f = fine;
    if (typeof fine === 'string') {
        try { f = JSON.parse(fine); } catch (e) { f = { _id: fine }; }
    }

    const fullId = getIdString(f._id || f.id || f.fine_id);
    document.getElementById('adjust_fineId').value = fullId || '';
    document.getElementById('adjust_currentAmount').value = `£${(f.totalAmount ? Number(f.totalAmount) : 0).toFixed(2)}`;
    document.getElementById('adjust_newAmount').value = (f.totalAmount ? Number(f.totalAmount) : 0);
    document.getElementById('adjustFineModal').style.display = 'flex';
}

function closeAdjustFineModal() {
    document.getElementById('adjustFineModal').style.display = 'none';
}

document.getElementById('adjustFineForm').addEventListener('submit', function(e) {
    e.preventDefault();

    const fineId = document.getElementById('adjust_fineId').value;
    const newAmount = document.getElementById('adjust_newAmount').value;

    fetch(`${window.APP_CTX}/admin/updateFineAmount`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: `fineId=${fineId}&newAmount=${newAmount}`
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showToast('✓ Fine amount updated successfully!', 'success');
                closeAdjustFineModal();
                loadFinesActivity();
            } else {
                showToast('✗ Failed to update fine: ' + data.error, 'error');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('✗ Failed to update fine amount', 'error');
        });
});

// Load fines on page load
document.addEventListener('DOMContentLoaded', function() {
    if (document.querySelector('.admin-dashboard-page')) {
        loadFinesActivity();
    }
});


// admin dashboard student-specific history modal and table
function openStudentHistoryModal() {
    const modalHTML = `
    <div id="studentHistoryModal" class="modal" style="display:flex;">
        <div class="modal-content" style="max-width:900px;">
            <div class="modal-header">
                <h2>🔍 Student Loan History</h2>
                <span class="close-modal" onclick="document.getElementById('studentHistoryModal').remove()">&times;</span>
            </div>
            <div class="modal-body">
                <div style="display:flex;gap:1rem;margin-bottom:1rem;flex-wrap:wrap;">
                    <input id="histStudentNum" type="text" placeholder="Student Number" 
                           style="flex:1;min-width:180px;padding:0.5rem;border:2px solid #e5e7eb;border-radius:8px;">
                    <input id="histMonth" type="month" 
                           style="padding:0.5rem;border:2px solid #e5e7eb;border-radius:8px;">
                    <button onclick="searchStudentHistory()" 
                            style="padding:0.5rem 1rem;background:#6366f1;color:#fff;border:none;border-radius:8px;cursor:pointer;">
                        Search
                    </button>
                </div>
                <div id="studentInfoBar" style="display:none;padding:1rem;background:#f8fafc;border-radius:8px;margin-bottom:1rem;"></div>
                <div style="overflow-x:auto;">
                    <table style="width:100%;border-collapse:collapse;">
                        <thead style="background:#f8fafc;border-bottom:2px solid #e5e7eb;">
                            <tr>
                                <th style="padding:0.75rem;text-align:left;">Title</th>
                                <th style="padding:0.75rem;text-align:left;">ISBN</th>
                                <th style="padding:0.75rem;text-align:left;">Borrowed</th>
                                <th style="padding:0.75rem;text-align:left;">Expected Return</th>
                                <th style="padding:0.75rem;text-align:left;">Returned</th>
                                <th style="padding:0.75rem;text-align:left;">Status</th>
                            </tr>
                        </thead>
                        <tbody id="studentHistoryBody">
                            <tr><td colspan="6" style="padding:1rem;color:#64748b;text-align:center;">Enter student number to search</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>`;
    document.body.insertAdjacentHTML('beforeend', modalHTML);
}

async function searchStudentHistory() {
    const studentNum = document.getElementById('histStudentNum').value.trim();
    const month = document.getElementById('histMonth').value;
    const tbody = document.getElementById('studentHistoryBody');
    const infoBar = document.getElementById('studentInfoBar');

    if (!studentNum) {
        showToast('Please enter a student number', 'error');
        return;
    }

    tbody.innerHTML = '<tr><td colspan="6" style="padding:1rem;text-align:center;">Loading...</td></tr>';

    try {
        const url = `${window.APP_CTX}/admin/studentHistory?studentNumber=${encodeURIComponent(studentNum)}${month ? `&month=${month}` : ''}`;
        const res = await fetch(url);
        const data = await res.json();

        if (!data.success) {
            tbody.innerHTML = `<tr><td colspan="6" style="padding:1rem;color:#ef4444;">${data.error}</td></tr>`;
            infoBar.style.display = 'none';
            return;
        }

        if (data.student && data.student.firstName) {
            infoBar.innerHTML = `<strong>${data.student.firstName} ${data.student.lastName}</strong> (${data.student.studentNumber})`;
            infoBar.style.display = 'block';
        } else {
            infoBar.style.display = 'none';
        }

        if (!data.records || data.records.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="padding:1rem;color:#64748b;text-align:center;">No records found</td></tr>';
            return;
        }

        tbody.innerHTML = data.records.map(r => {
            const status = (r.status || 'borrowed').toLowerCase();
            const cssClass = status === 'overdue' ? 'overdue' : status === 'returned' ? 'returned' : 'borrowed';
            return `
                <tr style="border-bottom:1px solid #f1f5f9;">
                    <td style="padding:0.75rem;">${escapeHtml(r.title)}</td>
                    <td style="padding:0.75rem;">${escapeHtml(r.isbn)}</td>
                    <td style="padding:0.75rem;">${formatDateTime(r.borrowDate)}</td>
                    <td style="padding:0.75rem;">${formatDateTime(r.expectedReturnDate)}</td>
                    <td style="padding:0.75rem;">${r.actualReturnDate ? formatDateTime(r.actualReturnDate) : '-'}</td>
                    <td style="padding:0.75rem;"><span class="book-status ${cssClass}">${status}</span></td>
                </tr>`;
        }).join('');
    } catch (e) {
        tbody.innerHTML = '<tr><td colspan="6" style="padding:1rem;color:#ef4444;">Error loading history</td></tr>';
    }
}

// ==========================================
// ADMIN NOTIFICATIONS SYSTEM
// ==========================================


let notifications = [];
let notificationPollingInterval = null;

// ------------------------------------------
// PANEL TOGGLE
// ------------------------------------------

function toggleNotificationPanel() {
    const panel = document.getElementById('notificationPanel');
    if (!panel) return;

    panel.classList.toggle('active');
    if (panel.classList.contains('active')) {
        loadNotifications(true);
    }
}

// Close panel when clicking outside
document.addEventListener('click', (e) => {
    const wrapper = document.querySelector('.notification-wrapper');
    const panel = document.getElementById('notificationPanel');
    if (wrapper && panel && !wrapper.contains(e.target)) {
        panel.classList.remove('active');
    }
});

// ------------------------------------------
// LOAD NOTIFICATIONS
// ------------------------------------------

async function loadNotifications(force = false) {
    try {
        const response = await fetch(`${window.APP_CTX}/admin/getNotifications`, {
            cache: force ? 'no-store' : 'default'
        });

        if (!response.ok) throw new Error(response.status);

        const data = await response.json();
        if (!data.success) return;

        notifications = data.notifications || [];
        renderNotifications();
        updateBadge();

    } catch (err) {
        console.error('Notification load failed:', err);
    }
}

// ------------------------------------------
// RENDER NOTIFICATIONS
// ------------------------------------------

function renderNotifications() {
    const list = document.getElementById('notificationList');
    if (!list) return;

    if (!notifications.length) {
        list.innerHTML = '<div class="notification-empty">No notifications</div>';
        return;
    }

    list.innerHTML = notifications.map(n => `
        <div class="notification-item ${n.read ? '' : 'unread'}"
             onclick="markAsRead(event, '${String(n.id)}')">
            <div class="notification-icon">${getNotificationIcon(n.type)}</div>
            <div class="notification-content">
                <div class="notification-title">${escapeHtml(n.title)}</div>
                <div class="notification-text">${escapeHtml(n.message)}</div>
                <div class="notification-time">${formatTimeAgo(n.timestamp)}</div>
            </div>
        </div>
    `).join('');
}

// ------------------------------------------
// ICONS
// ------------------------------------------

function getNotificationIcon(type) {
    const icons = {
        borrow: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>',
        return: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>',
        overdue: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>',
        fine: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2v20M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"/></svg>',
        message: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>'
    };
    return icons[type] || icons.message;
}

// ------------------------------------------
// MARK SINGLE NOTIFICATION AS READ
// ------------------------------------------

async function markAsRead(event, notificationId) {
    event.stopPropagation(); // CRITICAL FIX

    const n = notifications.find(x => String(x.id) === String(notificationId));
    if (!n || n.read) return;

    // Optimistic UI update
    n.read = true;
    renderNotifications();
    updateBadge();

    try {
        const res = await fetch(`${window.APP_CTX}/admin/markNotificationRead`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: `id=${encodeURIComponent(notificationId)}`
        });

        if (!res.ok) throw new Error();

        // Force authoritative refresh
        loadNotifications(true);

    } catch (err) {
        console.error('Mark read failed:', err);
    }
}

// ------------------------------------------
// MARK ALL AS READ
// ------------------------------------------

async function markAllAsRead() {
    notifications.forEach(n => n.read = true);
    renderNotifications();
    updateBadge();

    try {
        const res = await fetch(`${window.APP_CTX}/admin/markAllNotificationsRead`, {
            method: 'POST'
        });

        if (!res.ok) throw new Error();

        loadNotifications(true);

    } catch (err) {
        console.error('Mark all failed:', err);
    }
}

// ------------------------------------------
// BADGE UPDATE
// ------------------------------------------
function updateBadge() {
    const badge = document.getElementById('notificationBadge');
    if (!badge) return;

    const unread = notifications.filter(n => !n.read).length;
    badge.hidden = unread === 0;
    badge.textContent = unread > 99 ? '99+' : unread;
}

// ------------------------------------------
// TIME FORMATTING
// ------------------------------------------

function formatTimeAgo(timestamp) {
    if (!timestamp) return '';

    const now = new Date();
    const date = new Date(timestamp);
    const seconds = Math.floor((now - date) / 1000);

    if (isNaN(seconds) || seconds < 0) return '';
    if (seconds < 60) return 'Just now';
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
    if (seconds < 604800) return `${Math.floor(seconds / 86400)}d ago`;

    return date.toLocaleDateString();
}

// ------------------------------------------
// POLLING CONTROL
// ------------------------------------------

function startNotificationPolling() {
    stopNotificationPolling();
    loadNotifications(true);
    notificationPollingInterval = setInterval(() => loadNotifications(), 30000);
}

function stopNotificationPolling() {
    if (notificationPollingInterval) {
        clearInterval(notificationPollingInterval);
        notificationPollingInterval = null;
    }
}


// ------------------------------------------
// MODAL HANDLERS
// ------------------------------------------

function openNotificationModal() {
    const modal = document.getElementById('notificationModal');
    if (!modal) return;

    modal.style.display = 'flex';
    document.body.classList.add('no-scroll');
    loadNotifications();
}

function closeNotificationModal() {
    const modal = document.getElementById('notificationModal');
    if (!modal) return;

    modal.style.display = 'none';
    document.body.classList.remove('no-scroll');
}

// ------------------------------------------
// INIT (ADMIN DASHBOARD ONLY)
// ------------------------------------------

if (document.body.classList.contains('admin-dashboard-page')) {
    document.addEventListener('DOMContentLoaded', startNotificationPolling);
    document.addEventListener('visibilitychange', () => {
        document.hidden ? stopNotificationPolling() : startNotificationPolling();
    });
}