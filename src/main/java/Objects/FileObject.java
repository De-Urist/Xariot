package Objects;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

public class FileObject {
	
	private File file;
	private String hash;
	private long lastModified;
	ChecksumSHA1 s = new ChecksumSHA1();
	
	public FileObject(File f) throws Exception {
		this.file = f;
		this.lastModified = System.currentTimeMillis();
		this.hash = ChecksumSHA1.getSHA1Checksum(f.getAbsolutePath());
	}
	
	public synchronized File getFile(){
		return file;
	}
	
	public synchronized String getHash(){
		return hash;
	}
	
	public synchronized long getLastModified() {
		return lastModified;
	}
	
	public synchronized void updateLastModified() {
		this.lastModified = System.currentTimeMillis();
	}
	 
	public synchronized void setNewHash(String x) throws Exception {
		this.hash = s.getSHA1Checksum(x);
	}
}
