package edu.cs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

@WebServlet("/FileUploadServlet")
@MultipartConfig(fileSizeThreshold=1024*1024*10, 	// 10 MB 
               maxFileSize=1024*1024*300,      	// 300 MB
               maxRequestSize=1024*1024*400)   	// 400 MB
public class FileUploadServlet extends HttpServlet {

  private static final long serialVersionUID = 205242440643911308L;
	
  /**
   * Directory where uploaded files will be saved, its relative to
   * the web application directory.
   */
  private static final String UPLOAD_DIR = "uploads";
  private static final long MAX_UPLOAD_SIZE = 1024L * 1024 * 300; // 300 MB limit (2b)
  
  protected void doPost(HttpServletRequest request,
          HttpServletResponse response) throws ServletException, IOException {
      // gets absolute path of the web application
      String applicationPath = request.getServletContext().getRealPath("");

      // constructs path of the directory to save uploaded file
      String uploadFilePath = applicationPath + File.separator + UPLOAD_DIR;

      // creates the save directory if it does not exists
      File fileSaveDir = new File(uploadFilePath);
      if (!fileSaveDir.exists()) {
          fileSaveDir.mkdirs();
      }
      System.out.println("Upload File Directory="+fileSaveDir.getAbsolutePath());
      
      String fileName = "";
      //Get all the parts from request and write it to the file on server
      for (Part part : request.getParts()) {
          fileName = getFileName(part);
          fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
          
          // level 2B: Reject file if too large
          if(part.getSize() > MAX_UPLOAD_SIZE){
              response.setContentType("text/html");
              response.getWriter().write("File upload rejected. File size exceeds limit of 5MB.");
              return;
          }
          
          part.write(uploadFilePath + File.separator + fileName);
      }

      // ---------------- Professor's original Level 1 code ----------------
      String message = "Result";
      File uploadedFile = new File(uploadFilePath + File.separator + fileName);

      response.setContentType("text/html");

      if (uploadedFile.exists() && uploadedFile.length() > 0) {
          // exception handling is used prevent server crashes when non-text files are uploaded.
          try {
              // code to show content in browser
              String content = new Scanner(uploadedFile).useDelimiter("\\Z").next();
              response.getWriter().write(message + "<br>File uploaded successfully.<br><pre>" + content + "</pre>");
          } catch (Exception e) {
              response.getWriter().write(message + "<br>File uploaded successfully.<br>(Binary file — content not displayed)");
          }
      } else {
          response.getWriter().write("File upload failed or file is empty.");
      }

      /****** Integrate remote DB connection with this servlet, uncomment and modify the code below *******
         //ADD YOUR CODE HERE!
      *********************************************************/
      // ------------- Level 1 -----------------------
      
      String jdbcURL = "jdbc:mysql://127.0.0.1:3306/cs370S26?useSSL=false&serverTimezone=UTC";
      String dbUser = "root";
      String dbPassword = "root_user";
      
      // ---------------- Level 2a: database insertion ----------------
      
      //String jdbcURL = "jdbc:mysql://192.168.1.43:3306/cs370S26?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
      //String jdbcURL = "jdbc:mysql://66.108.66.246:5000/cs370S26"; // for level 3 & 4
      //String dbUser = "remoteuser";
      //String dbPassword = "remote123";
		
      
      try {
          // Load MySQL driver
          Class.forName("com.mysql.cj.jdbc.Driver");

          try (Connection conn = DriverManager.getConnection(jdbcURL, dbUser, dbPassword)) {
              String sql = "INSERT INTO uploaded_files (filename, content) VALUES (?, ?)";
              
              // Use professor's loop again exactly if multiple parts exist
              for (Part part : request.getParts()) {
                  String fileNameDB = getFileName(part);
                  fileNameDB = fileNameDB.substring(fileNameDB.lastIndexOf("\\") + 1);

                  // Insert into DB
                  try (InputStream inputStream = part.getInputStream();
                       PreparedStatement stmt = conn.prepareStatement(sql)) {
                      stmt.setString(1, fileNameDB);
                      stmt.setBlob(2, inputStream);
                      stmt.executeUpdate();
                  }
              }

              System.out.println("All files stored in database successfully!");
          }

      } catch (ClassNotFoundException e) {
          e.printStackTrace();
          response.getWriter().write("<br>Database error: MySQL driver not found.");
      } catch (SQLException e) {
          e.printStackTrace();
          response.getWriter().write("<br>Database error: " + e.getMessage());
      }

      // ---------------- Professor's commented sections (keep intact) ----------------
      //request.setAttribute("message", "File uploaded successfully!");
      //getServletContext().getRequestDispatcher("/response.jsp").forward(
      //        request, response);
      
      //Below is added for parsing EHR
      //DecodeCCDA parsed = new DecodeCCDA(uploadFilePath + File.separator + fileName);
      //writeToResponse(response, parsed.getjson());
  }

  private String getFileName(Part part) {
      String contentDisp = part.getHeader("content-disposition");
      System.out.println("content-disposition header= "+contentDisp);
      String[] tokens = contentDisp.split(";");
      for (String token : tokens) {
          if (token.trim().startsWith("filename")) {
              return token.substring(token.indexOf("=") + 2, token.length()-1);
          }
      }
      return "";
  }

  private void writeToResponse(HttpServletResponse resp, String results) throws IOException {
      PrintWriter writer = new PrintWriter(resp.getOutputStream());
      resp.setContentType("text/plain");

      if (results.isEmpty()) {
          writer.write("No results found.");
      } else {
          writer.write(results);
      }
      writer.close();
  }	
}