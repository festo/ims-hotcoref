package ims.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUtil {

	public static ZipOutputStream openZOS(File file) throws FileNotFoundException{
		ZipOutputStream zos=new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
		return zos;
	}
	
	public static void writeObjectAsZipEntry(ZipOutputStream zos,String entryName,Object obj) throws IOException{
		zos.putNextEntry(new ZipEntry(entryName));
		ObjectOutputStream oos=new ObjectOutputStream(zos);
		oos.writeObject(obj);
		oos.flush();
	}
	
	public static <T> T loadObjectFromEntry(String entryName,File zf,Class<T> clazz) throws IOException, ClassNotFoundException{
		return loadObjectFromEntry(entryName,new ZipFile(zf),clazz);
	}
	
	public static <T> T loadObjectFromEntry(String entryName,ZipFile zf,Class<T> clazz) throws IOException, ClassNotFoundException{
		ObjectInputStream ois=new ObjectInputStream(new BufferedInputStream(zf.getInputStream(zf.getEntry(entryName))));
		Object o=ois.readObject();
		return clazz.cast(o);
	}
	
	public static void writeStringTXT(ZipOutputStream zos,String entry,String... strings) throws IOException{
		zos.putNextEntry(new ZipEntry(entry));
		BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(zos));
		for(String s:strings){
			writer.write(s);
			writer.newLine();
		}
		writer.flush();
	}
	
	public static String[] readStringTXT(File zf,String entry) throws IOException{
		return readStringTXT(new ZipFile(zf),entry);
	}
	public static String[] readStringTXT(ZipFile zf,String entry) throws IOException{
		BufferedReader reader=new BufferedReader(new InputStreamReader(zf.getInputStream(zf.getEntry(entry))));
		List<String> a=new ArrayList<String>();
		String line;
		while((line=reader.readLine())!=null)
			a.add(line);
		return a.toArray(new String[a.size()]);
	}
}
