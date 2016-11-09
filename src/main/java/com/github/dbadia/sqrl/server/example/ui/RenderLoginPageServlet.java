package com.github.dbadia.sqrl.server.example.ui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Base64;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dbadia.sqrl.server.SqrlAuthPageData;
import com.github.dbadia.sqrl.server.SqrlAuthenticationStatus;
import com.github.dbadia.sqrl.server.SqrlConfig;
import com.github.dbadia.sqrl.server.backchannel.SqrlServerOperations;
import com.github.dbadia.sqrl.server.example.Constants;
import com.github.dbadia.sqrl.server.example.ErrorId;
import com.github.dbadia.sqrl.server.example.Util;
import com.github.dbadia.sqrl.server.util.SqrlConfigHelper;
import com.github.dbadia.sqrl.server.util.SqrlException;

/**
 * Servlet renders the login page by perparing data then forwarding to login.jsp
 *
 * @author Dave Badia
 *
 */
@WebServlet(urlPatterns = { "/login" })
public class RenderLoginPageServlet extends HttpServlet {
	private static final long serialVersionUID = -849809318695746441L;

	private static final Logger			logger					= LoggerFactory.getLogger(RenderLoginPageServlet.class);
	private final SqrlConfig			sqrlConfig				= SqrlConfigHelper.loadFromClasspath();
	private final SqrlServerOperations	sqrlServerOperations	= new SqrlServerOperations(sqrlConfig);

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		if (logger.isInfoEnabled()) {
			final StringBuilder buf = new StringBuilder();
			if (request.getCookies() != null) { // TODO: move to util cookies to string
				for (final Cookie cookie : request.getCookies()) {
					buf.append(cookie.getName()).append("=").append(cookie.getValue()).append("  ");
				}
			}
			logger.info("In do post for /login with params: {}.  cookies: {}", request.getParameterMap(),
					buf.toString());
		}
		try {
			displayLoginPage(request, response);
		} catch (final Exception e) {
			throw new ServletException("Error rendering login page", e);
		}
	}

	public static void redirectToLoginPageWithError(final HttpServletResponse response, ErrorId errorId) {
		if (errorId == null) {
			errorId = ErrorId.GENERIC;
		}
		response.setHeader("Location", "login?error=" + errorId.getId());
		response.setStatus(302);
	}

	private void displayLoginPage(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		request.setAttribute(Constants.JSP_SUBTITLE, "Login Page");
		// Default action, show the login page with a new SQRL QR code
		try {
			final SqrlAuthPageData pageData = sqrlServerOperations.buildQrCodeForAuthPage(request, response,
					InetAddress.getByName(request.getRemoteAddr()), 250);
			final ByteArrayOutputStream baos = pageData.getQrCodeOutputStream();
			baos.flush();
			final byte[] imageInByteArray = baos.toByteArray();
			baos.close();
			// Since this is being passed to the browser, we use regular Base64 encoding, NOT SQRL specific
			// Base64URL encoding
			final String b64 = new StringBuilder("data:image/").append(pageData.getHtmlFileType(sqrlConfig))
					.append(";base64, ").append(Base64.getEncoder().encodeToString(imageInByteArray)).toString();
			request.setAttribute("sqrlqr64", b64);
			request.setAttribute("sqrlurl", pageData.getUrl().toString());
			request.setAttribute("sqrlqrdesc", "Click or scan to login with SQRL");
			request.setAttribute("sqrlstate", SqrlAuthenticationStatus.CORRELATOR_ISSUED.toString());
			request.setAttribute("correlator", pageData.getCorrelator());
			logger.debug("Showing login page with correlator={}, sqrlurl={}", pageData.getCorrelator(),
					pageData.getUrl().toString());

			checkForErrorState(request, response);
			request.getRequestDispatcher("WEB-INF/login.jsp").forward(request, response);
		} catch (final SqrlException e) {
			redirectToLoginPageWithError(response, ErrorId.ERROR_SQRL_INTERNAL);
		}
	}

	private void checkForErrorState(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException, SqrlException {
		final String errorParam = request.getParameter("error");
		if (Util.isBlank(errorParam)) {
			return;
		}
		String errorMessage = null;
		if (errorParam.startsWith("ERROR_")) {
			try {
				errorMessage = ErrorId.valueOf(errorParam).getErrorMessage();
			} catch (final IllegalArgumentException e) {
				logger.error("Error translating errorParam '{}' to ErrorId", errorParam, e);
				errorMessage = ErrorId.GENERIC.getErrorMessage();
			}
		} else {
			errorMessage = ErrorId.byId(Integer.parseInt(errorParam)).getErrorMessage();
		}
		final StringBuilder buf = new StringBuilder("An error occurred").append(errorMessage).append(".");
		request.setAttribute(Constants.JSP_SUBTITLE, Util.wrapErrorInRed(buf.toString()));
		// If we have access to the correlator, append the first 5 chars to the message in case it gets reported
		final String correlatorString = sqrlServerOperations.extractSqrlCorrelatorStringFromRequestCookie(request);
		if (Util.isNotBlank(correlatorString)) {
			buf.append("    code=" + correlatorString.substring(0, 5));
		}

		// Since we are in an error state, kill the session
		if (request.getSession(false) != null) {
			request.getSession(false).invalidate();
		}
	}

}