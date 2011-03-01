package eu.heliovo.monitoring.controller;

import java.io.*;

import javax.servlet.http.*;

import org.springframework.beans.factory.annotation.*;
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
	public LogFilesDownloadController(@Value("${logging.filePath}") String logFilePath) {
		this.logFilePath = logFilePath;
	}

	@RequestMapping("*.txt")
	public void downloadLogFile(HttpServletRequest request, HttpServletResponse response) throws IOException {

		String pathWithinServlet = urlPathHelper.getPathWithinServletMapping(request);
		String logFile = pathWithinServlet.substring(1);

		try {
			FileCopyUtils.copy(new FileReader(logFilePath + "/" + logFile), response.getWriter());
		} catch (FileNotFoundException e) {
			response.sendError(HTTP_404);
		}
	}
}