// java
package student;

import admin.NotificationServlet;
import common.FinesServlet;
import configs.DatabaseConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class LibraryServletsTest {

    private HttpServletRequest req;
    private HttpServletResponse resp;
    private HttpSession session;
    private StringWriter body;
    private PrintWriter writer;

    @BeforeEach
    void setup() throws IOException {
        req = mock(HttpServletRequest.class);
        resp = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        body = new StringWriter();
        writer = new PrintWriter(body, true);
        when(resp.getWriter()).thenReturn(writer);
    }

    @Test
    void borrowBook_requiresIsbn() throws IOException {
        BorrowBook servlet = new BorrowBook();
        when(req.getParameter("isbn")).thenReturn(null);
        servlet.doPost(req, resp);
        assertTrue(body.toString().contains("ISBN is required"));
        verify(resp).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    void borrowBook_requiresLogin() throws IOException {
        BorrowBook servlet = new BorrowBook();
        when(req.getParameter("isbn")).thenReturn("123");
        when(req.getSession(false)).thenReturn(null);
        servlet.doPost(req, resp);
        assertTrue(body.toString().contains("Not logged in"));
        verify(resp).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void myBooks_requiresLogin() throws IOException {
        MyBooks servlet = new MyBooks();
        when(req.getSession(false)).thenReturn(null);
        servlet.doGet(req, resp);
        assertTrue(body.toString().contains("Not logged in"));
        verify(resp).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void extendBorrow_propagatesMissingIdAsNotFound() throws IOException {
        ExtendBorrow servlet = new ExtendBorrow();
        when(req.getParameter("borrowId")).thenReturn(null);
        servlet.doPost(req, resp);
        verify(resp).setStatus(HttpServletResponse.SC_NOT_FOUND);
        assertTrue(body.toString().contains("Borrow record"));
    }

    @Test
    void finesServlet_getOverdue_requiresAuth() throws IOException, ServletException {
        FinesServlet servlet = new FinesServlet();
        when(req.getServletPath()).thenReturn("/student/getOverdueLoans");
        when(req.getSession(false)).thenReturn(null);
        servlet.doGet(req, resp);
        assertTrue(body.toString().contains("Not authenticated"));
    }

    @Test
    void finesServlet_processPayment_requiresSelection() throws IOException, ServletException {
        FinesServlet servlet = new FinesServlet();
        when(req.getServletPath()).thenReturn("/student/processFinePayment");
        when(req.getSession(false)).thenReturn(session);
        when(session.getAttribute("userSNumber")).thenReturn("S1");
        when(session.getAttribute("userFName")).thenReturn("A");
        when(session.getAttribute("userLName")).thenReturn("B");
        when(req.getParameterValues("loanIds[]")).thenReturn(new String[0]);
        servlet.doPost(req, resp);
        assertTrue(body.toString().contains("No loans selected"));
    }

    // Example of verifying redirect on logout
    @Test
    void logout_redirectsToLogin() throws IOException, ServletException {
        Logout servlet = new Logout();
        when(req.getSession(false)).thenReturn(session);
        servlet.doGet(req, resp);
        verify(session).invalidate();
        ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
        verify(resp).sendRedirect(cap.capture());
        assertTrue(cap.getValue().endsWith("/login"));
    }
}
