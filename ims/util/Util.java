package ims.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Util {

	public static BufferedReader getReader(File file) throws IOException{
		return getReader(file,file.toString().endsWith(".gz"));
	}
	
	public static BufferedReader getReader(File file,boolean gzipped) throws IOException{
		return getReader(file,gzipped,Charset.forName("UTF-8"));
	}

	public static BufferedReader getReader(File file,boolean gzipped,Charset charset) throws IOException{
		InputStream is=new FileInputStream(file);
		if(gzipped){
			is=new GZIPInputStream(is);
		}
		BufferedReader br=new BufferedReader(new InputStreamReader(is,charset));
		return  br;
	}

	public static BufferedWriter getWriter(File file) throws IOException{
		return getWriter(file,file.toString().endsWith(".gz"));
	}
	
	public static BufferedWriter getWriter(File file,boolean gzipped) throws IOException{
		return getWriter(file,gzipped,Charset.forName("UTF-8"));
	}

	public static BufferedWriter getWriter(File file,boolean gzipped,Charset charset) throws IOException{
		OutputStream os=new FileOutputStream(file);
		if(gzipped){
			os=new GZIPOutputStream(os);
		}
		BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(os,charset));
		return writer;
	}
	
	public static String insertCommas(long l){
		return insertCommas(l,true);
	}
	
	public static String insertCommas(long l,boolean withMS){
		StringBuilder ret=new StringBuilder(Long.toString(l));
		ret.reverse();
		for(int i=3;i<ret.length();i+=4){
			if(i+1<=ret.length())
				ret.insert(i,",");
		}
		if(withMS)
			return ret.reverse().append("ms").toString();
		else
			return ret.reverse().toString();
	}
	
	public static String padZerosFront(int len,int i){
		int pad=len-Integer.toString(i).length();
		if(pad<1)
			return Integer.toString(i);
		StringBuilder sb=new StringBuilder();
		for(int r=0;r<pad;++r)
			sb.append('0');
		return sb.append(i).toString();
	}
	
	public static <T> void incrementMapValue(Map<T,MutableInt> tm, T key){
		MutableInt mi=tm.get(key);
		if(mi==null){
			tm.put(key,new MutableInt(1));
		} else {
			mi.increment();
		}
	}

	public static int getBits(int i){
		int b=0;
		while(i>0){
			b++;
			i>>=1;
		}
		return b;
	}

	public static List<Class<?>> listOfClasses(Class<?>... clazzes){
		List<Class<?>> l=new ArrayList<Class<?>>();
		for(Class<?> c:clazzes)
			l.add(c);
		return l;
	}

	public static <T> Map<T, Integer> getInvertedIndex(List<T> list) {
		Map<T,Integer> m=new HashMap<T,Integer>();
		for(int i=0,max=list.size();i<max;++i)
			m.put(list.get(i), i);
		return m;
	}
	
}
