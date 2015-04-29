package ims.hotcoref.util;

import ims.hotcoref.data.Span;
import ims.hotcoref.features.enums.Gender;
import ims.hotcoref.features.enums.Num;
import ims.util.Util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class BergsmaLinLookup implements Serializable{
	private static final long serialVersionUID = 1L;
	
	public static final String[] BYTE2STRING={"MALE","FEMALE","NEUTRAL","PLURAL"};
	public static final Gender[] BYTE2GEN=new Gender[]{Gender.Masc,Gender.Fem,Gender.Neut,Gender.Unknown};
	public static final Num[] BYTE2NUM=new Num[]{Num.Sin,Num.Sin,Num.Sin,Num.Plu};
	private static final Pattern TAB=Pattern.compile("\\t");
	private static final Pattern SPACE=Pattern.compile(" ");
	private static final Pattern DIGITS=Pattern.compile("\\d{2,}");
	
	private static final int DEFAULT_THRESHOLD=9;
	
	private Map<String,Byte> wholePhrase;
	private Map<String,Byte> prefix;
	private Map<String,Byte> suffix;
	
	private BergsmaLinLookup(){
		wholePhrase=new HashMap<String,Byte>();
		prefix=new HashMap<String,Byte>();
		suffix=new HashMap<String,Byte>();
	}
	
	public BergsmaLinLookup(File file) throws IOException{
		this(file,DEFAULT_THRESHOLD);
	}
	public BergsmaLinLookup(File file,int threshold) throws IOException{
		this();
		if(file==null){
			System.out.println("No gender data file given. The gender dictionary will be empty.");
			return;
		}
//		this.threshold=threshold;
		BufferedReader reader=Util.getReader(file);
		int lineCount=0;
		int saveCount=0;
		String line;
		System.out.println("Reading gender data from "+file.toString());
		while((line=reader.readLine())!=null){
			lineCount++;
			String[] a=TAB.split(line);
			String[] counts=SPACE.split(a[1]);
			
			byte mostCommon=-1;
			int mostCommonCount=-1;
			for(byte k=0;k<4;++k){
				int i=Integer.parseInt(counts[k]);
				if(i>mostCommonCount){
					mostCommon=k;
					mostCommonCount=i;
				}
			}

			if(mostCommonCount>threshold){
				saveCount++;
				if(a[0].startsWith("!")){
					suffix.put(SPACE.split(a[0])[1],new Byte(mostCommon));
				} else if (a[0].endsWith("!")){
					prefix.put(SPACE.split(a[0])[0],new Byte(mostCommon));
				} else {
					wholePhrase.put(a[0], new Byte(mostCommon));
				}
			}
		}
		System.out.println("Read "+lineCount+" lines, saved "+saveCount);
		reader.close();
	}
	
	public Byte lookupByHeadAndAffixes(Span span){
		String pre=span.s.forms[span.start].toLowerCase();
		String suf=span.s.forms[span.end].toLowerCase();
		if(suf.equals("'s"))
			suf=span.s.forms[span.end-1].toLowerCase();
		String head=span.s.forms[span.hd].toLowerCase();
		Byte b;
		if((b=wholePhrase.get(head))!=null)
			return b;
		if((b=prefix.get(pre))!=null)
			return b;
		if((b=suffix.get(suf))!=null)
			return b;
		return null;
	}
	
	public Num lookupNum(Span span){
		Byte b=span2Byte(span);
		if(b==null)
			return Num.Unknown;
		else
			return BYTE2NUM[b];
	}
	
	public Gender lookupGen(Span span){
		Byte b=span2Byte(span);
		if(b==null)
			return Gender.Unknown;
		else
			return BYTE2GEN[b];
	}
	
	private Byte span2Byte(Span span){
		if(span.start==span.end){ //Single word
			Byte b=wholePhrase.get(span.s.forms[span.start].toLowerCase());
			return b;
		}
		String phrase=span2phraseString(span);
		Byte b=wholePhrase.get(phrase);
		if(b!=null)
			return b;
		Byte c=lookupByHeadAndAffixes(span);
		return c;
	}
	
	private String span2phraseString(Span span){
		StringBuilder sb=new StringBuilder();
		for(int i=span.start;i<span.end;++i)
			sb.append(span.s.forms[i]).append(' ');
		if(!span.s.forms[span.end].equals("'s"))
			sb.append(span.s.forms[span.end]);
		return DIGITS.matcher(sb.toString().trim().toLowerCase()).replaceAll("#");
	}
	
	private Byte lookup(String s){
		String numbersReplaced=DIGITS.matcher(s.toLowerCase()).replaceAll("#");
		if(wholePhrase.containsKey(numbersReplaced)){
			return wholePhrase.get(numbersReplaced);
		} else {
			String[] split=SPACE.split(numbersReplaced);
			if(prefix.containsKey(split[0])){
				return prefix.get(split[0]);
			} else if(suffix.containsKey(split[split.length-1])){
				return suffix.get(split[split.length-1]);
			} else {
				return null;
			}
		}
	}
	
	public static void main(String[] args) throws IOException, ClassNotFoundException{
		File f=new File("/home/anders/corpora/gender.data.gz");
		if(args.length>0)
			f=new File(args[0]);
		String[] examples={"anders"};
		BergsmaLinLookup lookup=new BergsmaLinLookup(f);
		for(String e:examples){
			Byte b=lookup.lookup(e);
			String gender=(b==null?"null":BYTE2STRING[b.byteValue()]);
			System.out.printf("%-25s %10s\n", e,gender);
		}
		System.out.println("saving...");
		ZipOutputStream zos=new ZipOutputStream(new BufferedOutputStream(new FileOutputStream("foo")));
		lookup.save(zos);
		zos.close();
		System.out.println("loading...");
		BergsmaLinLookup lookup2=load(new ZipFile("foo"));
		for(String e:examples){
			Byte b=lookup2.lookup(e);
			String gender=(b==null?"null":BYTE2STRING[b.byteValue()]);
			System.out.printf("%-25s %10s\n", e,gender);
		}
	}
	
	private static final boolean USE_OOS=false; //So this seems to be somewhat slower to load, but takes less space on disk. I would assume the memory requirements during runtime are equal (though havent checked)
	
	public static final String GENDER_ZIP_ENTRY="gender-data";
	public void save(ZipOutputStream zos) throws IOException {
		ZipEntry e=new ZipEntry(GENDER_ZIP_ENTRY);
		zos.putNextEntry(e);
		if(USE_OOS){
			ObjectOutputStream oos=new ObjectOutputStream(zos);
			oos.writeObject(wholePhrase);
			oos.writeObject(prefix);
			oos.writeObject(suffix);
			zos.closeEntry();
			return;
		}
		DataOutputStream dos=new DataOutputStream(zos);
		dos.writeInt(wholePhrase.size());
		for(Entry<String,Byte> en:wholePhrase.entrySet()){
			dos.writeUTF(en.getKey());
			dos.writeByte(en.getValue());
		}
		dos.writeInt(prefix.size());
		for(Entry<String,Byte> en:prefix.entrySet()){
			dos.writeUTF(en.getKey());
			dos.writeByte(en.getValue());
		}
		dos.writeInt(suffix.size());
		for(Entry<String,Byte> en:suffix.entrySet()){
			dos.writeUTF(en.getKey());
			dos.writeByte(en.getValue());
		}
		dos.flush();
		zos.closeEntry();
	}
	
	@SuppressWarnings("unchecked")
	public static BergsmaLinLookup load(ZipFile zipFile) throws IOException, ClassNotFoundException{
		BergsmaLinLookup lookup=new BergsmaLinLookup();
		if(USE_OOS){
			ObjectInputStream ois=new ObjectInputStream(new BufferedInputStream(zipFile.getInputStream(zipFile.getEntry(GENDER_ZIP_ENTRY))));
			lookup.wholePhrase=(Map<String, Byte>) ois.readObject();
			lookup.prefix=(Map<String, Byte>) ois.readObject();
			lookup.suffix=(Map<String, Byte>) ois.readObject();
			ois.close();
			return lookup;
		}
		DataInputStream dis=new DataInputStream(new BufferedInputStream(zipFile.getInputStream(zipFile.getEntry(GENDER_ZIP_ENTRY))));
		int c=dis.readInt();
		for(int i=0;i<c;++i){
			String key=dis.readUTF();
			Byte b=dis.readByte();
			lookup.wholePhrase.put(key, b);
		}
		c=dis.readInt();
		for(int i=0;i<c;++i){
			String key=dis.readUTF();
			Byte b=dis.readByte();
			lookup.prefix.put(key, b);
		}
		c=dis.readInt();
		for(int i=0;i<c;++i){
			String key=dis.readUTF();
			Byte b=dis.readByte();
			lookup.suffix.put(key, b);
		}
		dis.close();
		return lookup;
	}
}
