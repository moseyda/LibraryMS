package admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet(name = "adminLogout", value = "/admin/logout")
public class adminLogout extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);

        if (session != null) {
            // Remove admin-specific attributes
            session.removeAttribute("adminLoggedIn");
            session.removeAttribute("adminUsername");
            session.removeAttribute("adminRole");

            // Invalidate the session
            session.invalidate();
        }

        // Redirect to index page
        response.sendRedirect(request.getContextPath() + "/common/index.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
