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
                                        <button class="extend-btn" onclick="extendBorrow('${book._id?.$oid || book._id}')">Extend</button>
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
                showToast('Borrow period extended by 10 days!', 'success');
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
            const actualReturnRaw = record.actualReturnDate?.$date || record.actualReturnDate;
            return `
            <tr style="border-bottom:1px solid #f1f5f9;">
              <td style="padding:1rem;">${escapeHtml(record.title || 'Unknown')}</td>
              <td style="padding:1rem;">${escapeHtml(record.isbn || 'N/A')}</td>
              <td style="padding:1rem;">${formatDateTime(record.borrowDate?.$date || record.borrowDate)}</td>
              <td style="padding:1rem;">${formatDateTime(record.expectedReturnDate?.$date || record.expectedReturnDate)}</td>
              <td style="padding:1rem;">${actualReturnRaw ? formatDateTime(actualReturnRaw) : 'Not returned'}</td>
              <td style="padding:1rem;">
                <span class="book-status ${record.status === 'borrowed' ? 'borrowed' : 'available'}">
                  ${record.status === 'borrowed' ? 'Borrowed' : 'Returned'}
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

// admin-loans.js
document.addEventListener('DOMContentLoaded', () => {
    const ctx = window.APP_CTX || '';
    loadLoans();

    async function loadLoans(status = '') {
        const body = document.getElementById('loansTableBody');
        if (!body) return;
        body.innerHTML = `<tr><td colspan="7" style="padding:1rem;color:#64748b;">Loading...</td></tr>`;
        try {
            const url = `${ctx}/admin/loansActivity${status ? `?status=${encodeURIComponent(status)}` : ''}`;
            const res = await fetch(url);
            if (!res.ok) throw new Error('Failed to fetch loans');
            const data = await res.json();
            renderLoans(data);
        } catch (e) {
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
            const id = getId(r._id);
            const borrower = [r.firstName, r.lastName].filter(Boolean).join(' ') || 'N/A';
            const sNum = r.SNumber ? ` <small style="color:#6b7280;">(${escapeHtml(r.SNumber)})</small>` : '';
            const borrowed = formatDateTime(r.borrowDate?.$date || r.borrowDate);
            const expected = formatDateTime(r.expectedReturnDate?.$date || r.expectedReturnDate);
            const returned = r.actualReturnDate ? formatDateTime(r.actualReturnDate?.$date || r.actualReturnDate) : null;
            const isBorrowed = String(r.status || '').toLowerCase() === 'borrowed';

            const statusBadge = isBorrowed
                ? `<span class="book-status borrowed">Borrowed</span>`
                : `<span class="book-status available">Returned</span>`;

            const actions = isBorrowed
                ? `<button class="btn-delete" onclick="adminReturnBook('${id}')">Mark Returned</button>`
                : `<span style="color:#9ca3af;">—</span>`;

            return `
        <tr>
          <td style="padding:1rem;">${escapeHtml(r.title || 'Unknown')}</td>
          <td style="padding:1rem;">${escapeHtml(r.isbn || 'N/A')}</td>
          <td style="padding:1rem;">${escapeHtml(borrower)}${sNum}</td>
          <td style="padding:1rem;">${borrowed}</td>
          <td style="padding:1rem;">${expected}</td>
          <td style="padding:1rem;">${statusBadge}${!isBorrowed && returned ? `<br><small>${returned}</small>` : ''}</td>
          <td style="padding:1rem;">${actions}</td>
        </tr>
      `;
        }).join('');
    }

    window.adminReturnBook = async function(borrowId) {
        try {
            const res = await fetch(`${ctx}/returnBook`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: new URLSearchParams({ borrowId })
            });
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || 'Failed to mark returned');
            if (window.showToast) showToast('Book marked as returned', 'success');
            loadLoans();
        } catch (e) {
            if (window.showToast) showToast('Error marking as returned', 'error');
        }
    };

    function getId(maybeObjId) { return maybeObjId?.$oid || maybeObjId || ''; }
    function escapeHtml(text) { const d = document.createElement('div'); d.textContent = text ?? ''; return d.innerHTML; }
    function formatDateTime(date) {
        if (!date) return 'N/A';
        const d = new Date(date);
        return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
});



// Analytics Dashboard View - Admin Dashboard Page

(function () {
    // Only run on the admin dashboard page
    if (!document.body.classList.contains('admin-dashboard-page')) return;

    const ctxPath = (window.APP_CTX || '<%= request.getContextPath() %>');

    const els = {
        total: document.getElementById('kpiTotalBooks'),
        available: document.getElementById('kpiAvailable'),
        active: document.getElementById('kpiActiveLoans'),
        overdue: document.getElementById('kpiOverdue'),
        rateText: document.getElementById('kpiAvailabilityRate'),
        rateBar: document.getElementById('kpiAvailabilityBar'),
        chart: document.getElementById('activityChart')
    };

    Promise.all([
        fetch(ctxPath + '/browseBooks').then(r => r.ok ? r.json() : [] ).catch(() => []),
        fetch(ctxPath + '/admin/loansActivity').then(r => r.ok ? r.json() : [] ).catch(() => [])
    ]).then(([books, loans]) => {
        // Totals from books
        const totalCopies = books.reduce((sum, b) => sum + (Number(b.quantity || 0)), 0);
        const availableCopies = books.reduce((sum, b) => sum + (Number(b.available || 0)), 0);

        // Active and overdue from loans
        const activeLoans = loans.filter(l => String(l.status || '').toLowerCase() === 'borrowed');
        const now = new Date();
        const overdueLoans = activeLoans.filter(l => {
            const exp = new Date(l.expectedReturnDate?.$date || l.expectedReturnDate);
            return exp instanceof Date && !isNaN(exp) && exp < now;
        });

        setNum(els.total, totalCopies);
        setNum(els.available, availableCopies);
        setNum(els.active, activeLoans.length);
        setNum(els.overdue, overdueLoans.length);

        const rate = totalCopies > 0 ? Math.round((availableCopies / totalCopies) * 100) : 0;
        els.rateText.textContent = rate + '% available';
        els.rateBar.style.width = rate + '%';

        drawActivityChart(els.chart, loans);
    });

    function setNum(el, n) { if (el) el.textContent = Number(n || 0).toLocaleString(); }

    function drawActivityChart(canvas, loans) {
        if (!canvas) return;

        // Build 7 day buckets ending today
        const today = new Date(); today.setHours(0,0,0,0);
        const days = Array.from({ length: 7 }, (_, i) => {
            const d = new Date(today); d.setDate(today.getDate() - (6 - i));
            return d;
        });

        const counts = days.map((d, i) => {
            const start = new Date(d);
            const end = new Date(d); end.setDate(start.getDate() + 1);
            return loans.filter(l => {
                const bd = new Date(l.borrowDate?.$date || l.borrowDate);
                return bd >= start && bd < end;
            }).length;
        });

        const widthCSS = canvas.clientWidth || canvas.parentElement.clientWidth || 600;
        const heightCSS = 120;
        const dpr = Math.max(1, window.devicePixelRatio || 1);
        canvas.width = widthCSS * dpr;
        canvas.height = heightCSS * dpr;
        const ctx = canvas.getContext('2d');
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

        const W = widthCSS, H = heightCSS;
        ctx.clearRect(0, 0, W, H);

        // Grid
        ctx.strokeStyle = '#e5e7eb';
        ctx.lineWidth = 1;
        for (let i = 1; i <= 3; i++) {
            const y = (H / 4) * i;
            ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(W, y); ctx.stroke();
        }

        // Bars
        const max = Math.max(1, ...counts);
        const gap = 8;
        const barW = (W - gap * (counts.length + 1)) / counts.length;
        counts.forEach((v, i) => {
            const x = gap + i * (barW + gap);
            const h = (v / max) * (H - 22);
            ctx.fillStyle = '#6366f1';
            ctx.fillRect(x, H - h - 12, barW, h);

            // Labels
            ctx.fillStyle = '#6b7280';
            ctx.font = '12px system-ui, -apple-system, Segoe UI, Roboto';
            const d = days[i];
            const label = (d.getMonth() + 1) + '/' + d.getDate();
            ctx.fillText(label, x, H - 2);
        });
    }
})();

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
