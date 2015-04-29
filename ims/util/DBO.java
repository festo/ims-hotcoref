package ims.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class DBO {

	static PrintStream out=System.out;
	private static boolean printCaller=true;
	private static boolean printTime=true;
	
	public static int printNoPrefix(String s){
		out.print(s);
		return s.length();
	}
	
	public static int printWithPrefix(String s){
		StringBuilder sb=getPrefix();
		if(sb!=null)
			out.print(sb.toString());
		out.print(s);
		return s.length();
	}
	
	public static void printlnNoPrefix(String s){
		out.println(s);
	}
	
	public static int println(String s){
		StringBuilder sb=getPrefix();
		if(printCaller || printTime)
			out.print(sb.toString());
		out.println(s);
		return 0;
	}
	
	public static int eraseAndPrint(int erase,int i){
		erase(erase);
		return printNoPrefix(Integer.toString(i));
	}
	
	public static int eraseAndPrint(int erase,String s){
		erase(erase);
		return printNoPrefix(s);
	}
	
	public static void erase(int erase){
		for(int i=0;i<erase;++i)
			out.print('\b');
	}

	public static void println() {
		out.println();
	}
	
	public static void setOutFile(File f) throws FileNotFoundException{
		out=new PrintStream(new BufferedOutputStream(new FileOutputStream(f)));
	}
	
	public static void close(){
		out.close();
	}
	
	public static void setPrintCaller(boolean printCaller){
		DBO.printCaller=printCaller;
	}
	public static void setPrintTime(boolean printTime){
		DBO.printTime=printTime;
	}
	
	
	private static StringBuilder getPrefix(){
		if(printCaller)
			return getCaller();
		else if(printTime)
			return getTime();
		else
			return null;
	}
	
	private static final String BIG = "                                                                                    " ;
	private static StringBuilder getCaller(){
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		StringBuilder msg;
		if(printTime)
			msg=getTime();
		else
			msg=new StringBuilder();
		msg.append(ste[4].getClassName()+" "+ste[4].getLineNumber());
		msg.append(':');
		msg.append(ste[4].getMethodName());
		msg.append('>');
		int l = 65-msg.length();
		if (l < 0) l =0;
		msg.append(BIG.substring(0, l));
		return msg;
	}
	
	private static StringBuilder getTime() {
		GregorianCalendar s_cal =  new GregorianCalendar();   
		StringBuilder sb = new StringBuilder();
		sb.append(s_cal.get(Calendar.HOUR_OF_DAY));
		sb.append('.');
		sb.append(s_cal.get(Calendar.MINUTE));
		sb.append('.');
		sb.append(s_cal.get(Calendar.SECOND));
//		sb.append('.');
//		sb.append(s_cal.get(Calendar.MILLISECOND));
		for(int i=sb.length();i<7;++i)
			sb.append(' ');
		sb.append("   ");
		return sb;
	}
}
