package com.example.uploadingfiles;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.example.uploadingfiles.storage.StorageFileNotFoundException;
import com.example.uploadingfiles.storage.StorageService;
import org.apache.tomcat.util.http.fileupload.FileItemIterator;
import org.apache.tomcat.util.http.fileupload.FileItemStream;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FileUploadController {

	private final StorageService storageService;

	@Autowired
	public FileUploadController(StorageService storageService) {
		this.storageService = storageService;
	}

	@GetMapping("/")
	public String listUploadedFiles(Model model) throws IOException {

		model.addAttribute("files", storageService.loadAll().map(
				path -> MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
						"serveFile", path.getFileName().toString()).build().toUri().toString())
				.collect(Collectors.toList()));

		return "uploadForm";
	}

	@GetMapping("/files/{filename:.+}")
	@ResponseBody
	public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

		Resource file = storageService.loadAsResource(filename);
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=\"" + file.getFilename() + "\"").body(file);
	}



	@ExceptionHandler(StorageFileNotFoundException.class)
	public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
		return ResponseEntity.notFound().build();
	}

	@PostMapping("/")
	public String handleFileUpload(final HttpServletRequest request, RedirectAttributes redirectAttributes) {

		boolean isMultipart = ServletFileUpload.isMultipartContent(request);

		if (!isMultipart) {
			// consider raising an error here if desired
		}

		// Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload();

		FileItemIterator iter;
		InputStream fileStream = null;
		String name = null;
		try {
			// retrieve the multi-part constituent items parsed from the request
			iter = upload.getItemIterator(request);

			// loop through each item
			while (iter.hasNext()) {
				FileItemStream item = iter.next();
				name = item.getName();
				fileStream = item.openStream();

				// check if the item is a file
				if (!item.isFormField()) {
					System.out.println("File field " + name + " with file name " + item.getName() + " detected.");
					break; // break here so that the input stream can be processed
				}
			}
		} catch (FileUploadException | IOException e) {
			// log / handle the error here as necessary
			e.printStackTrace();
		}

		if (fileStream != null) {
			// a file has been sent in the http request
			// pass the fileStream to a method on the storageService so it can be persisted
			// note the storageService will need to be modified to receive and process the fileStream
			storageService.store(fileStream, name);
		}

		redirectAttributes.addFlashAttribute("message", "You successfully uploaded " + name + "!");

		return "redirect:/";
	}

}
