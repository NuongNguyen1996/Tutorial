package lib.report;

import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public class Upload extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
	}
	
	//khi upload can dung phuong thuc post
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		boolean isMultipart = ServletFileUpload.isMultipartContent(request);
		if (isMultipart) {
			FileItemFactory factory = new DiskFileItemFactory();
			ServletFileUpload upload = new ServletFileUpload(factory);
			String UPLOAD_DIRECTORY = getServletContext().getRealPath(File.separator + "Upload" + File.separator);
			if (!new File(UPLOAD_DIRECTORY).exists()) {
				new File(UPLOAD_DIRECTORY).mkdir();
			}
			try {
				List<FileItem> multiparts = upload.parseRequest(request);
				for (FileItem item : multiparts) {
					if (!item.isFormField()) {
						String name = new File(item.getName()).getName();
						item.write(new File(UPLOAD_DIRECTORY + File.separator + name));
						System.out.println("Folder file upload on server : " + UPLOAD_DIRECTORY);
						System.out.println("File upload success");
					}
				}
			} catch (Exception e) {
				System.out.println("Exception : " + e.toString());
				System.out.println("File upload failed");
			}
		}
	}
}
