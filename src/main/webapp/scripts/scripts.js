(function () {
    function setup() {
        const contextPath = window.APP_CTX || (function () {
            const parts = location.pathname.split('/').filter(Boolean);
            return parts.length ? '/' + parts[0] : '';
        })();

        const modal = document.getElementById('booksModal');
        const browseBtn = document.querySelector('.dashboard-actions .action-card');
        const closeBtn = document.querySelector('#booksModal .close-modal');
        const booksContainer = document.getElementById('booksContainer');
        const searchInput = document.getElementById('searchInput');
        let allBooks = [];

        if (!modal || !browseBtn || !closeBtn || !booksContainer) return;

        browseBtn.addEventListener('click', e => {
            e.preventDefault();
            modal.style.display = 'block';
            fetchBooks();
        });

        closeBtn.addEventListener('click', () => modal.style.display = 'none');
        window.addEventListener('click', e => { if (e.target === modal) modal.style.display = 'none'; });

        function fetchBooks() {
            fetch(contextPath + '/browseBooks', { headers: { Accept: 'application/json' } })
                .then(r => {
                    if (!r.ok) throw new Error('HTTP ' + r.status);
                    return r.json();
                })
                .then(data => {
                    allBooks = Array.isArray(data) ? data : [];
                    displayBooks(allBooks);
                })
                .catch(err => {
                    console.error(err);
                    booksContainer.innerHTML = '<div class="error">Failed to load books.</div>';
                });
        }

        function displayBooks(books) {
            if (!books.length) {
                booksContainer.innerHTML = '<div class="no-books">No books found.</div>';
                return;
            }
            booksContainer.innerHTML = books.map(b => {
                const status = b.status || 'Unknown';
                const available = status === 'Available';
                return `
                <div class="book-card">
                    <div class="book-header">
                        <h3>${escapeHtml(b.title) || 'Untitled'}</h3>
                        <span class="book-status ${available ? 'available' : 'unavailable'}">${status}</span>
                    </div>
                    <div class="book-details">
                        <p><strong>Author:</strong> ${escapeHtml(b.author) || 'Unknown'}</p>
                        <p><strong>ISBN:</strong> ${escapeHtml(b.isbn) || 'N/A'}</p>
                        <p><strong>Category:</strong> ${escapeHtml(b.category) || 'N/A'}</p>
                        <p><strong>Publisher:</strong> ${escapeHtml(b.publisher) || 'N/A'}</p>
                        <p><strong>Publication Year:</strong> ${b.publicationYear || 'N/A'}</p>
                        <p><strong>Quantity:</strong> ${b.quantity != null ? b.quantity : 'N/A'}</p>
                        <p><strong>Available:</strong> ${b.available != null ? b.available : 'N/A'}</p>
                        <p><strong>Description:</strong> ${escapeHtml(b.description) || 'N/A'}</p>
                    </div>
                    <button class="borrow-btn" ${!available ? 'disabled' : ''}>
                        ${available ? 'Borrow Book' : 'Unavailable'}
                    </button>
                </div>`;
            }).join('');
        }

        if (searchInput) {
            searchInput.addEventListener('input', e => {
                const term = e.target.value.toLowerCase();
                displayBooks(allBooks.filter(b =>
                    (b.title || '').toLowerCase().includes(term) ||
                    (b.author || '').toLowerCase().includes(term) ||
                    (b.isbn || '').toLowerCase().includes(term) ||
                    (b.category || '').toLowerCase().includes(term)
                ));
            });
        }

        function escapeHtml(str) {
            if (str == null) return '';
            return String(str)
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;');
        }
    }

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', setup);
    else setup();
})();
