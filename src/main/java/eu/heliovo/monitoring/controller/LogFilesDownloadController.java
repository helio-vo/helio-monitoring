package eu.heliovo.monitoring.controller;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UrlPathHelper;

@Controller
public final class LogFilesDownloadController {

	private static final int HTTP_404 = 404;
	private final UrlPathHelper urlPathHelper = new UrlPathHelper();
	private final String logFilePath;

	@Autowired
	public LogFilesDownloadController(@Value("${logging.filePath}") final String logFilePath) {
		this.logFilePath = logFilePath;
	}

	@RequestMapping("*.txt")
	public void downloadLogFile(final HttpServletRequest request, final HttpServletResponse response)
			throws IOException {

		String pathWithinServlet = urlPathHelper.getPathWithinServletMapping(request);
		String logFile = pathWithinServlet.substring(1);

		System.out.println("Controller: " + request.getRequestURI());
		System.out.println("Controller path: " + pathWithinServlet);
		System.out.println("Controller logFile: " + logFile);

		try {
			FileCopyUtils.copy(new FileReader(logFilePath + "/" + logFile), response.getWriter());
		} catch (FileNotFoundException e) {
			response.sendError(HTTP_404);
		}
	}
}
