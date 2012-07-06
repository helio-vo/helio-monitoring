package eu.heliovo.monitoring.controller;

import java.io.*;

import javax.servlet.http.*;

import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UrlPathHelper;

/**
 * This Controller sends a requested log file for a URL displayed in the Nagios service status information.
 * 
 * @author Kevin Seidler
 * 
 */
@Controller
public final class LogFilesDownloadController {

	private static final int HTTP_404 = 404;
	private final UrlPathHelper urlPathHelper = new UrlPathHelper();
	private final String logFilePath;

	@Autowired
	public LogFilesDownloadController(@Value("${logging.filePath}") String logFilePath) {
		this.logFilePath = logFilePath;
	}

	@RequestMapping("/**/*.txt")
	public void downloadLogFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String pathWithinServlet = urlPathHelper.getPathWithinServletMapping(request);
		String logFile = pathWithinServlet.substring(1);
		File logFileFile = new File(logFilePath, logFile);
		try {
			FileCopyUtils.copy(new FileReader(logFileFile), response.getWriter());
		} catch (FileNotFoundException e) {
			response.sendError(HTTP_404, "Unable to find log file at " + logFileFile.getCanonicalPath());
		}
	}
}