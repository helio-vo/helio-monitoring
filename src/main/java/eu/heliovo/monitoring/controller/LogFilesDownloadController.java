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
public class LogFilesDownloadController {

	private final UrlPathHelper urlPathHelper = new UrlPathHelper();
	private final String logFilePath;

	@Autowired
	public LogFilesDownloadController(@Value("${methodCall.log.filePath}") final String logFilePath) {
		this.logFilePath = logFilePath;
	}

	@RequestMapping("*.txt")
	public void downloadLogFile(final HttpServletRequest request, final HttpServletResponse response)
			throws IOException {

		final String pathWithinServlet = urlPathHelper.getPathWithinServletMapping(request);
		final String logFile = pathWithinServlet.substring(1);

		try {
			FileCopyUtils.copy(new FileReader(logFilePath + "/" + logFile), response.getWriter());
		} catch (final FileNotFoundException e) {
			response.sendError(404);
		}
	}
}
