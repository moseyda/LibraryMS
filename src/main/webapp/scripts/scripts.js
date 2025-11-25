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

    fetchDataAndRender();

    async function fetchDataAndRender() {
        try {
            const [books, loans] = await Promise.all([
                fetch(ctxPath + '/browseBooks').then(r => r.ok ? r.json() : []).catch(() => []),
                fetch(ctxPath + '/admin/loansActivity').then(r => r.ok ? r.json() : []).catch(() => [])
            ]);

            // Store for re-rendering on button click
            window.__lastLoansData = loans;

            // --- KPIs ---
            const totalCopies = books.reduce((sum, b) => sum + (Number(b.quantity || 0)), 0);
            const availableCopies = books.reduce((sum, b) => sum + (Number(b.available || 0)), 0);

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
            els.rateText.textContent = `${rate}% available`;
            els.rateBar.style.width = `${rate}%`;

            // --- Chart ---
            drawActivityChart(loans, selectedDays);

        } catch (err) {
            console.error('Failed to fetch dashboard data:', err);
        }
    }

    function setNum(el, n) {
        if (el) el.textContent = Number(n || 0).toLocaleString();
    }

    // NEW: Filter button listeners
    document.querySelectorAll('.chart-filter-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const days = Number(btn.dataset.days);

            // Highlight active button
            document.querySelectorAll('.chart-filter-btn')
                .forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            selectedDays = days;

            // Re-render chart using last fetched data
            if (window.__lastLoansData) {
                drawActivityChart(window.__lastLoansData, selectedDays);
            }
        });
    });

    function drawActivityChart(loans, daysRange = 7) {
        if (!els.chart) return;

        // Prepare buckets ending today
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        const days = Array.from({ length: daysRange }, (_, i) => {
            const d = new Date(today);
            d.setDate(today.getDate() - (daysRange - 1 - i));
            return d;
        });

        const borrowsCounts = days.map(d => {
            const start = new Date(d);
            const end = new Date(d);
            end.setDate(start.getDate() + 1);
            return loans.filter(l => {
                const bd = new Date(l.borrowDate?.$date || l.borrowDate);
                return bd >= start && bd < end;
            }).length;
        });

        const returnsCounts = days.map(d => {
            const start = new Date(d);
            const end = new Date(d);
            end.setDate(start.getDate() + 1);
            return loans.filter(l => {
                const rd = l.actualReturnDate ? new Date(l.actualReturnDate?.$date || l.actualReturnDate) : null;
                return rd && rd >= start && rd < end;
            }).length;
        });

        const labels = days.map(d => `${d.getMonth() + 1}/${d.getDate()}`);

        if (!activityChart) {
            activityChart = echarts.init(els.chart);
        }

        const option = {
            color: ['#6366F1', '#10B981'],
            tooltip: { trigger: 'axis', backgroundColor: '#111827', textStyle: { color: '#fff' } },
            legend: { data: ['Borrows', 'Returns'], textStyle: { color: '#374151' } },
            grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
            xAxis: { type: 'category', boundaryGap: false, data: labels, axisLine: { lineStyle: { color: '#475569' } }, axisTick: { show: false } },
            yAxis: { type: 'value', axisLine: { lineStyle: { color: '#475569' } }, splitLine: { lineStyle: { color: 'rgba(203,213,225,0.35)' } } },
            series: [
                {
                    name: 'Borrows',
                    type: 'line',
                    smooth: true,
                    data: borrowsCounts,
                    areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(99,102,241,0.45)' }, { offset: 1, color: 'rgba(99,102,241,0)' }]) },
                    lineStyle: { width: 3 }
                },
                {
                    name: 'Returns',
                    type: 'line',
                    smooth: true,
                    data: returnsCounts,
                    areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(16,185,129,0.45)' }, { offset: 1, color: 'rgba(16,185,129,0)' }]) },
                    lineStyle: { width: 3 }
                }
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
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                if (data.payments.length === 0) {
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
                    container.innerHTML = data.payments.map(payment => `
                        <div class="payment-history-item">
                            <div class="payment-receipt-header">
                                <div class="receipt-id">Receipt #${payment._id.$oid.substring(0, 8).toUpperCase()}</div>
                                <span class="receipt-status ${payment.status}">${payment.status.toUpperCase()}</span>
                            </div>
                            <div class="receipt-details">
                                <div class="receipt-detail-row">
                                    <span class="receipt-detail-label">Payment Date:</span>
                                    <span class="receipt-detail-value">${formatDateTime(payment.actualPaymentDate)}</span>
                                </div>
                                <div class="receipt-detail-row">
                                    <span class="receipt-detail-label">Student Number:</span>
                                    <span class="receipt-detail-value">${payment.studentNumber}</span>
                                </div>
                                <div class="receipt-detail-row">
                                    <span class="receipt-detail-label">Full Name:</span>
                                    <span class="receipt-detail-value">${payment.fullName}</span>
                                </div>
                            </div>
                            <div class="receipt-books-list">
                                <div class="receipt-books-title">Books Paid For:</div>
                                ${payment.books.map(book => `
                                    <div class="receipt-book-item">
                                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                            <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"></path>
                                        </svg>
                                        <span>${book.title} (${book.isbn})</span>
                                    </div>
                                `).join('')}
                            </div>
                            <div class="receipt-total">
                                <span class="receipt-total-label">Total Paid:</span>
                                <span class="receipt-total-amount">£${payment.totalAmount.toFixed(2)}</span>
                            </div>
                        </div>
                    `).join('');
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
        .then(response => response.json())
        .then(data => {
            const tbody = document.getElementById('finesTableBody');
            if (data.success && data.fines.length > 0) {
                tbody.innerHTML = data.fines.map(fine => `
                    <tr>
                        <td>#${fine._id.$oid.substring(0, 8).toUpperCase()}</td>
                        <td>${fine.studentNumber}</td>
                        <td><strong>${fine.fullName}</strong></td>
                        <td>${fine.books.length} book(s)</td>
                        <td><strong>£${fine.totalAmount.toFixed(2)}</strong></td>
                        <td><span class="category-badge">${fine.status}</span></td>
                        <td>${formatDateTime(fine.actualPaymentDate)}</td>
                        <td>
                            <button class="btn-edit" onclick='openAdjustFineModal(${JSON.stringify(fine)})'>
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <path d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                                </svg>
                                Adjust
                            </button>
                        </td>
                    </tr>
                `).join('');
            } else {
                tbody.innerHTML = '<tr><td colspan="8" class="empty-state">No fines recorded yet</td></tr>';
            }
        })
        .catch(error => {
            console.error('Error loading fines:', error);
            document.getElementById('finesTableBody').innerHTML =
                '<tr><td colspan="8" style="padding:1rem;color:#dc2626;">Failed to load fines</td></tr>';
        });
}

function openAdjustFineModal(fine) {
    document.getElementById('adjust_fineId').value = fine._id.$oid;
    document.getElementById('adjust_currentAmount').value = `£${fine.totalAmount.toFixed(2)}`;
    document.getElementById('adjust_newAmount').value = fine.totalAmount;
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

