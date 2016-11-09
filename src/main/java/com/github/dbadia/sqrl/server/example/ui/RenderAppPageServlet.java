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
import com.github.dbadia.sqrl.server.example.Constants;
import com.github.dbadia.sqrl.server.example.ErrorId;
import com.github.dbadia.sqrl.server.example.Util;
import com.github.dbadia.sqrl.server.example.data.AppUser;
import com.github.dbadia.sqrl.server.util.SqrlConfigHelper;

/**
 * This servlet renders the main page of the application which displays the users surname and their welcome phrase. It
 * can be accessed via username/password auth or SQRL auth
 *
 * @author Dave Badia
 *
 */
@WebServlet(urlPatterns = { "/app" })
public class RenderAppPageServlet extends HttpServlet {
	private static final long	serialVersionUID	= 6252981832657794489L;
	private static final Logger	logger				= LoggerFactory.getLogger(RenderAppPageServlet.class);

	private final SqrlServerOperations sqrlServerOperations = new SqrlServerOperations(
			SqrlConfigHelper.loadFromClasspath());

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		AppUser user = null;
		if (request.getSession(false) != null) {
			user = (AppUser) request.getSession(false).getAttribute(Constants.SESSION_NATIVE_APP_USER);
		}
		if (user == null || Util.isBlank(user.getGivenName()) || Util.isBlank(user.getWelcomePhrase())) {
			logger.error("user is not in session, redirecting to login page");
			RenderLoginPageServlet.redirectToLoginPageWithError(response, ErrorId.ATTRIBUTES_NOT_FOUND);
			return;
		}

		String accountType = "Username/password only";
		if (user.getUsername() == null) {
			accountType = "SQRL only";
		} else if (sqrlServerOperations.fetchSqrlIdentityByUserXref(Long.toString(user.getId())) != null) {
			accountType = "Both SQRL and username/password";
		}
		final HttpSession session = request.getSession(false);
		session.setAttribute("givenname", user.getGivenName());
		session.setAttribute("phrase", user.getWelcomePhrase());
		session.setAttribute("accounttype", accountType);
		request.getRequestDispatcher("WEB-INF/app.jsp").forward(request, response);
	}

}