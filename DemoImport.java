package demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import neo.velocity.common.Utility;

import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.object.UpdatableSqlQuery;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class DemoImport {
	final String host = "jdbc:oracle:thin:@//localhost:1521/orcl";
	final String user = "tax";
	final String pass = "abc1234";

	public static void main(String[] args) {
		new DemoImport();
	}

	int sumRow = 0;
	long mySEQ = 0;
	int batchSize = 10000;
	Utility u = new Utility();
	HashMap<String, String> mParams = new HashMap<String, String>();
//	List<String> list = new ArrayList<>();
	SimpleDateFormat dateParse = new SimpleDateFormat("mm/dd/yy");
	SimpleDateFormat dateFormat = new SimpleDateFormat("dd/mm/yyyy");
	DataFormatter df = new DataFormatter();
	PreparedStatement ps = null;
	Connection conn = null;

	public DemoImport() {
		int MAX_MEMORY_SIZE = 1024 * 1024 * 60; // 60MB
		boolean isMultipart = true;

		String result = "";
		String rootFolder = "D:/NEOCompany/BK_NEW/Server/webapps/ROOT"
				+ "/tong_hop_du_lieu_thue";
		String duLieuThueFolder = "/du_lieu_thue/";
		String duLieuThanhToanFolder = "/du_lieu_thanh_toan/";
		int UPLOAD_FILE_TYPE = 0;
		// UPLOAD_FILE_TYPE = 0 : Upload dữ liệu thuế
		int SHEET_NUMB = 1;
		if (isMultipart) {
			// Create a factory for disk-based file items
			DiskFileItemFactory factory = new DiskFileItemFactory();
			factory.setSizeThreshold(MAX_MEMORY_SIZE);
			// Create a new file upload handler
			ServletFileUpload upload = new ServletFileUpload(factory);
			upload.setHeaderEncoding("UTF-8");
			try {
				String extFileName = ".xls";
				// String resultWriteFile =
				// "BANGKE_01012015_30062016_Phuong09_14_20161030093634.xlsx";
				String resultWriteFile = "Book1_20161101222111.xls";
				if (resultWriteFile.length() > 0) {
					// Upload dữ liệu thuế
					int excelType = 0;
					if (extFileName.equals(".xls") && UPLOAD_FILE_TYPE == 0) {
						result = insertXlsData(rootFolder + duLieuThueFolder
								+ resultWriteFile, SHEET_NUMB , excelType,null);
					} else if (extFileName.equals(".xlsx")
							&& UPLOAD_FILE_TYPE == 0) {

						excelType = 1;
						result = insertXlsData(rootFolder + duLieuThueFolder
								+ resultWriteFile, SHEET_NUMB, excelType,null);
					}
					// Upload dữ liệu thanh toán
					if (extFileName.equals(".xls") && UPLOAD_FILE_TYPE == 1) {
						result = getNumbXlsColumn(rootFolder
								+ duLieuThanhToanFolder + resultWriteFile,
								SHEET_NUMB);
					}
					if (!extFileName.equals(".xls")
							&& !extFileName.equals(".xlsx")) {
						File file = new File(rootFolder + duLieuThanhToanFolder
								+ resultWriteFile);
						file.delete();
						result = "{RESULT:'FAIL',VALUE:'Định dạng file ko đúng.'}";
					}
				} else {
					result = "{RESULT:'FAIL',VALUE:'Ghi file không thành công'}";
				}
			} catch (NumberFormatException e) {
				result = "{RESULT:'FAIL',VALUE:'Sheet nhập ở dạng số'}";
			} catch (Exception e) {
				result = "{RESULT:'FAIL',VALUE:'" + e.getMessage() + "'}";
			}
		}
		System.out.println(result);
	}

	public Connection connectDB() {
		Connection cn = null;
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver").newInstance();
			System.out.println("open connect");
			cn = DriverManager.getConnection(host, user, pass);
			System.out.println("connect to " + host);
		} catch (InstantiationException | IllegalAccessException
				| ClassNotFoundException | SQLException e1) {
			e1.printStackTrace();
		}
		return cn;
	}

	@SuppressWarnings("resource")
	public String insertXlsData(String fileName, int sheetNum, int excelType,HashMap<String, String> mParams) {
		String result = "";
		String sql = "insert into tax.nnts_uploads_temp(mcq,ma_unt,ten_unt,cbk_unt,ngay_unt,ma_nnt,ten_nnt,sac_thue,chuong,tieu_muc,dia_ban,kbnn,kythue,loaitk_nsnn,han_nop,magiao_unt,loai_tien,tien_giao,tien_con,tien_thuduoc,tien_quyettoan,so_bl,ngay_bl,sobl_unt,ngaybl_unt,ngay_banke,sonha_ct,matinh_ct,tentinh_ct,maquan_ct,tenquan_ct,maxa_ct,tenxa_ct,mobile,email,sonha_tt,matinh_tt,tentinh_tt,maquan_tt,tenquan_tt,maxa_tt,tenxa_tt,id,ma_tinh,log_user,log_ip,log_date) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'','','',sysdate)";
		// String sqlDelTable =
		// "delete from tax.nnts_uploads_temp where log_ip='"+mParams.get("user_ip")+"'";
		String sqlSEQ = "select NNTS_UPLOADS_TEMP_SEQ.NEXTVAL from dual";

		try {
			conn = connectDB();
			// stm.executeUpdate(sqlDelTable);
			conn.setAutoCommit(false);
			ps = conn.prepareStatement(sqlSEQ);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				mySEQ = rs.getLong(1);
			ps = conn.prepareStatement(sql);
			if (excelType == 1) {
				result = readFileExcelXLSX(fileName, sheetNum);
			} else {
				result = readXLS(fileName, sheetNum);
			}
		} catch (Exception e) {
			result = "{Err sqlConnection: " + e.getMessage()+"}";
		} finally {
			try {
				if (ps != null) {
					ps.close();
				}
				if (conn != null) {
					conn.close();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return result;
	}

	public String readFileExcelXLSX(String fileName, int numSheet)
			throws IOException {
		String result = "";
		File file = new File(fileName);
		if (!file.exists()) {
			System.out.println("");
			return "File not found";
		}
		OPCPackage container = null;
		try {
			container = OPCPackage.open(file);
			ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(
					container);
			XSSFReader xssfReader = new XSSFReader(container);
			StylesTable styles = xssfReader.getStylesTable();
			InputStream stream = xssfReader.getSheet("rId" + numSheet);
			if (stream != null) {
				result = processSheet(styles, strings, stream);

				ps.executeBatch();
				conn.commit();
				stream.close();
			} else {
				result = "{RESULT:'FAIL',VALUE:'Lỗi ko có dữ liệu hoặc sheet ko tồn tại'}";
			}
		} catch (SAXException e) {
			return e.getMessage();
		} catch (SQLException e) {
			return e.getMessage();
		} catch (InvalidFormatException e) {
			e.printStackTrace();
		} catch (OpenXML4JException e) {
			return e.getMessage();
		} finally {
			if (container != null) {
				container.close();
			}
		}
		return result;
	}

	private String processSheet(StylesTable styles,
			ReadOnlySharedStringsTable strings, InputStream sheetInputStream)
			throws IOException, SAXException {

		InputSource sheetSource = new InputSource(sheetInputStream);
		SAXParserFactory saxFactory = SAXParserFactory.newInstance();
		try {
			SAXParser saxParser = saxFactory.newSAXParser();
			XMLReader sheetParser = saxParser.getXMLReader();
			ContentHandler handler = new XSSFSheetXMLHandler(styles, strings,
					new SheetContentsHandler() {
				List<String> list = new ArrayList<>();
						@Override
						public void startRow(int rowNum) {
							sumRow = rowNum;
						}

						@Override
						public void endRow() {
							if (sumRow > 1) {
								try {
									updateDB(list);
									if (sumRow % batchSize == 0) {
										try {
											ps.executeBatch();
										} catch (Exception e) {
											System.out.println(e.toString());
										}
										ps.clearBatch();
										conn.commit();
									}

								} catch (SQLException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							list.clear();
						}

						@Override
						public void cell(String cellReference, String value) {
							try {
								list.add(dateFormat.format(dateParse
										.parse(value)));
							} catch (Exception e) {
								list.add(value);
							}
						}

						@Override
						public void headerFooter(String text, boolean isHeader,
								String tagName) {
						}

					}, false);
			sheetParser.setContentHandler(handler);
			sheetParser.parse(sheetSource);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("SAX parser appears to be broken - "
					+ e.getMessage());
		}
		return "Upload success!";
	}

	public String readXLS(String path, int numbOfSheet) {
		List<String> list = null;
		try {
			FileInputStream input = new FileInputStream(path);
			POIFSFileSystem fs = new POIFSFileSystem(input);
			HSSFWorkbook wb = new HSSFWorkbook(fs);
			int totalSheet = wb.getNumberOfSheets();
			if (numbOfSheet > totalSheet -1) {
				return "Sheet không tồn tại";
			}
			HSSFSheet sheet = wb.getSheetAt(numbOfSheet);
			Iterator<Row> rows = sheet.rowIterator();
			for (int i = 0; i < 2; i++)
				rows.next();

			int count = 0;
			while (rows.hasNext()) {
				HSSFRow row = (HSSFRow) rows.next();
				Iterator<Cell> cells = row.cellIterator();
				int i = 1;
				list = new ArrayList();
				while (cells.hasNext() && i < 43) {
					HSSFCell cell = (HSSFCell) cells.next();
					list.add(getCellValue(cell));
					i++;
				}
				updateDB(list);
				if (++count % batchSize == 0) {
					try {
						ps.executeBatch();
						ps.clearBatch();
						conn.commit();
					} catch (Exception e) {
						System.out.println(e.toString());
					}
					count = 0;
				}
			}
			input.close();
			ps.executeBatch();
			ps.clearBatch();
			conn.commit();
		} catch (IOException e) {
			return  "{RESULT:'FAIL',VALUE:'Lỗi ko có dữ liệu hoặc file ko tồn tại'}";
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

	private void updateDB(List<String> list) {
		if (conn == null) {
			System.out.println("Connect fail");
			return;
		}
		try {
			String value = "";
			for (int i = 0; i < 42; i++) {
				value = i < list.size() ? list.get(i) : null;
				ps.setString(i + 1, value);
			}
			// ID
			ps.setLong(43, mySEQ);
			ps.addBatch();

			ps.clearParameters();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static String getNumbXlsColumn(String path, int numbOfSheet) {
		String result = "";
		String s = "";
		try {
			FileInputStream input = new FileInputStream(path);
			POIFSFileSystem fs = new POIFSFileSystem(input);
			HSSFWorkbook wb = new HSSFWorkbook(fs);
			int totalSheet = wb.getNumberOfSheets();
			if (numbOfSheet > totalSheet - 1) {
				return result = "{RESULT:'FAIL',VALUE:'Lỗi ko có dữ liệu hoặc sheet ko tồn tại'}";
			}
			HSSFSheet sheet = wb.getSheetAt(numbOfSheet);
			Iterator<Row> rows = sheet.rowIterator();
			// rows.next();
			HSSFRow row = (HSSFRow) rows.next();
			Iterator<Cell> cells = row.cellIterator();
			int count = 0;
			while (cells.hasNext()) {
				HSSFCell cell = (HSSFCell) cells.next();
				String cellVal = getCellValue(cell);
				if (cellVal != "") {
					s = s + ",{'COLUMN_NAME':'" + cellVal + "'}";
					count++;
				}
			}

			input.close();
			result = "{'RESULT':'OK','VALUE':[" + s.substring(1)
					+ "],'FILE_PATH':'" + path.substring(path.lastIndexOf("/"))
					+ "'}";
		} catch (Exception e) {
			result = "{RESULT:'FAIL',VALUE:'Sheet ko có dữ liệu hoặc: "
					+ e.getMessage() + "'}";
		}
		return result;
	}

	public static String getCellValue(HSSFCell cell) {
		if (cell == null) {
			return null;
		}
		if (cell.getCellType() == HSSFCell.CELL_TYPE_STRING) {
			return cell.getStringCellValue().trim().replaceAll("[\n\r]", "");
		} else if (cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
			return cell.getNumericCellValue() + "";
		} else if (cell.getCellType() == HSSFCell.CELL_TYPE_BOOLEAN) {
			return cell.getBooleanCellValue() + "";
		} else if (cell.getCellType() == HSSFCell.CELL_TYPE_BLANK) {
			return cell.getStringCellValue().trim().replaceAll("[\n\r]", "");
		} else if (cell.getCellType() == HSSFCell.CELL_TYPE_ERROR) {
			return cell.getErrorCellValue() + "";
		} else {
			return null;
		}
	}

	public static String getCellXSSFValue(XSSFCell cell) {
		if (cell == null) {
			return null;
		}
		if (cell.getCellType() == XSSFCell.CELL_TYPE_STRING) {
			return cell.getStringCellValue();
		} else if (cell.getCellType() == XSSFCell.CELL_TYPE_NUMERIC) {
			return cell.getNumericCellValue() + "";
		} else if (cell.getCellType() == XSSFCell.CELL_TYPE_BOOLEAN) {
			return cell.getBooleanCellValue() + "";
		} else if (cell.getCellType() == XSSFCell.CELL_TYPE_BLANK) {
			return cell.getStringCellValue();
		} else if (cell.getCellType() == XSSFCell.CELL_TYPE_ERROR) {
			return cell.getErrorCellValue() + "";
		} else {
			return null;
		}
	}

	public String getCurrentTime() {
		Calendar cal = Calendar.getInstance();
		String format = "_yyyyMMddHHmmss";
		DateFormat dateFormat = new SimpleDateFormat(format);
		return dateFormat.format(cal.getTime());
	}

	public String doiTenFile(String fileName) {
		fileName = fileName.replaceAll(" ", "");
		String orgFileName = fileName.substring(0, fileName.lastIndexOf("."));
		String extFileName = fileName.substring(orgFileName.length())
				.toLowerCase();
		fileName = orgFileName;
		File f = null;
		int j = 0;
		while (true) {
			f = new File(fileName + "/" + fileName + extFileName);
			if (f.exists())
				fileName = orgFileName + "" + j;
			else
				break;
			j++;
		}
		return fileName += extFileName;
	}

	@SuppressWarnings("unused")
	private String ghiFile(InputStream in, String fileName, int type) {
		String result = "";
		File fileUploaded = null;
		String rootFolder = "D:/NEOCompany/BK_NEW/Server/webapps/ROOT/"
				+ "tong_hop_du_lieu_thue";
		String duLieuThueFolder = "/du_lieu_thue/";
		String duLieuThanhToanFolder = "/du_lieu_thanh_toan/";
		String path = "";
		try {
			String orgFileName = fileName.substring(0,
					fileName.lastIndexOf("."));
			String extFileName = fileName.substring(orgFileName.length())
					.toLowerCase();
			String finalFileName = orgFileName + getCurrentTime() + extFileName;
			if (type == 0) {
				path = rootFolder + duLieuThueFolder + finalFileName;
			} else if (type == 1) {
				path = rootFolder + duLieuThanhToanFolder + finalFileName;
			}
			int j = 0;
			while (true) {
				fileUploaded = new File(path);
				if (fileUploaded.exists())
					finalFileName = orgFileName + getCurrentTime() + "_" + j
							+ extFileName;
				else
					break;
				j++;
			}
			fileUploaded = new File(path);
			OutputStream out = new FileOutputStream(fileUploaded);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			if (fileUploaded.exists())
				// ghi file thanh cong, tra ve ten file
				result = finalFileName;
			out.close();
			in.close();
		} catch (Exception e) {
			result = "{RESULT:'FAIL',VALUE:'" + e.getMessage() + "'}";
		}
		return result;
	}

	public String vn_lower = "à,á,ả,ã,ạ,â,ầ,ấ,ẩ,ẫ,ậ,ă,ằ,ắ,ẳ,ẵ,f,è,é,ẻ,ẽ,ẹ,ê,ề,ế,ể,ễ,ệ,ì,í,ỉ,ĩ,ị,ò,ó,ỏ,õ,ọ,ô,ồ,ố,ổ,ỗ,ộ,ơ,ờ,ớ,ở,ỡ,ợ,ù,ú,ủ,ũ,ụ,ư,ừ,ứ,ử,ữ,ự,ỳ,ý,ỷ,ỹ,ỵ,đ";
	String vn_upper = "À,Á,Ả,Ã,Ạ,Â,Ầ,Ấ,Ẩ,Ẫ,Ậ,Ă,Ằ,Ắ,Ẳ,Ẵ,Ặ,È,É,Ẻ,Ẽ,Ẹ,Ê,Ề,Ế,Ể,Ễ,Ệ,Ì,Í,Ỉ,Ĩ,Ị,Ò,Ó,Ỏ,Õ,Ọ,Ô,Ồ,Ố,Ổ,Ỗ,Ộ,Ơ,Ờ,Ớ,Ở,Ỡ,Ợ,Ù,Ú,Ủ,Ũ,Ụ,Ư,Ừ,Ứ,Ử,Ữ,Ự,Ỳ,Ý,Ỷ,Ỹ,Ỵ,Đ";
	String en_lower = "a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,e,e,e,e,e,e,e,e,e,e,e,i,i,i,i,i,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,o,u,u,u,u,u,u,u,u,u,u,u,y,y,y,y,y,d";
	String en_upper = "A,A,A,A,A,A,A,A,A,A,A,A,A,A,A,A,A,E,E,E,E,E,E,E,E,E,E,E,I,I,I,I,I,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,O,U,U,U,U,U,U,U,U,U,U,U,Y,Y,Y,Y,Y,D";

	String vn_char_lower = vn_lower.replaceAll(",", "");
	String vn_char_upper = vn_upper.replaceAll(",", "");
	String en_char_lower = en_lower.replaceAll(",", "");
	String en_char_upper = en_upper.replaceAll(",", "");
	String character_iso_8859_1_map = "&quot;,&apos;,&amp;,&lt;,&gt;,&nbsp;,&iexcl;,&cent;,&pound;,&curren;,&yen;,&brvbar;,&sect;,&uml;,&copy;,&ordf;,&laquo;,&not;,&shy;,&reg;,&macr;,&deg;,&plusmn;,&sup2;,&sup3;,&acute;,&micro;,&para;,&middot;,&cedil;,&sup1;,&ordm;,&raquo;,&frac14;,&frac12;,&frac34;,&iquest;,&times;,&divide;,&Agrave;,&Aacute;,&Acirc;,&Atilde;,&Auml;,&Aring;,&AElig;,&Ccedil;,&Egrave;,&Eacute;,&Ecirc;,&Euml;,&Igrave;,&Iacute;,&Icirc;,&Iuml;,&ETH;,&Ntilde;,&Ograve;,&Oacute;,&Ocirc;,&Otilde;,&Ouml;,&Oslash;,&Ugrave;,&Uacute;,&Ucirc;,&Uuml;,&Yacute;,&THORN;,&szlig;,&agrave;,&aacute;,&acirc;,&atilde;,&auml;,&aring;,&aelig;,&ccedil;,&egrave;,&eacute;,&ecirc;,&euml;,&igrave;,&iacute;,&icirc;,&iuml;,&eth;,&ntilde;,&ograve;,&oacute;,&ocirc;,&otilde;,&ouml;,&oslash;,&ugrave;,&uacute;,&ucirc;,&uuml;,&yacute;,&thorn;,&yuml;";
	String[] character_iso_8859_1_map_arr = character_iso_8859_1_map.split(",");
	String character_unicode = "\",',&,<,>, ,¡,¢,£,¤,¥,¦,§,¨,©,ª,«,¬,�­,®,¯,°,±,²,³,´,µ,¶,·,¸,¹,º,»,¼,½,¾,¿,×,÷,À,Á,Â,Ã,Ä,Å,Æ,Ç,È,É,Ê,Ë,Ì,Í,Î,Ï,Ð,Ñ,Ò,Ó,Ô,Õ,Ö,Ø,Ù,Ú,Û,Ü,Ý,Þ,ß,à,á,â,ã,ä,å,æ,ç,è,é,ê,ë,ì,í,î,ï,ð,ñ,ò,ó,ô,õ,ö,ø,ù,ú,û,ü,ý,þ,ÿ";
	String[] character_unicode_arr = character_unicode.split(",");

	public String getEnStringFromVnString(String vnUtf8String) {
		if (vnUtf8String == null)
			return "";
		if (vnUtf8String.length() == 0)
			return "";

		String a = "";
		String s = ClearISO_8859_1_From_UTF8String(vnUtf8String);

		char b;
		for (int i = 0; i < s.length(); i++) {
			b = s.charAt(i);
			for (int j = 0; j < vn_char_lower.length(); j++) {
				if (b == vn_char_lower.charAt(j)) {
					b = en_char_lower.charAt(j);
					break;
				} else if (b == vn_char_upper.charAt(j)) {
					b = en_char_upper.charAt(j);
					break;
				}
			}

			a = a + Character.toString(b);
		}
		// String a = new String(vnChar);
		return a;
	}

	public String ClearISO_8859_1_From_UTF8String(String s) {
		try {
			if (s == null)
				return "";
			if (s.length() == 0)
				return "";

			String result = s;

			for (int i = 0; i < character_iso_8859_1_map_arr.length; i++) {
				String s1 = character_iso_8859_1_map_arr[i];
				// replace iso 8859_1 string by unicode string
				result = result.replaceAll(s1, character_unicode_arr[i]);
			}

			return result;
		} catch (Exception ex) {
			ex.printStackTrace();
			return "";
		}
	}
}
