package com.github.dbadia.sqrl.server.example.ui;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dbadia.sqrl.server.backchannel.SqrlServerOperations;
import com.github.dbadia.sqrl.server.example.sqrl.SqrlSettings;

/**
 * Servlet which handles logout requests
 * 
 * @author Dave Badia
 *
 */
@WebServlet(name = "SqrlExampleLogout", urlPatterns = { "/logout" })
public class LogoutServlet extends HttpServlet {
	private static final Logger logger = LoggerFactory.getLogger(LogoutServlet.class);
	private final SqrlServerOperations sqrlServerOperations = new SqrlServerOperations(SqrlSettings.getSqrlConfig());

	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		final HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		sqrlServerOperations.deleteSqrlAuthCookies(request, response);
		response.setStatus(302);
		response.setHeader("Location", "login");
	}
}