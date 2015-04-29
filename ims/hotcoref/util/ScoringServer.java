package ims.hotcoref.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ScoringServer extends Thread {

	private final int port;
	private final File scorerExec;
	private final ExecutorService es;
	
	public ScoringServer(int port,File scorerExec,int threads) throws IOException{
		this.port=port;
		this.scorerExec=scorerExec;
		if(threads>0)
			es=Executors.newFixedThreadPool(threads);
		else
			es=Executors.newCachedThreadPool();
	}
	
	@SuppressWarnings("resource")
	public void run(){
		Deque<Future<ScoringJob>> d=new ArrayDeque<Future<ScoringJob>>();
		ServerSocket ss;
		try {
			ss = new ServerSocket(port);
			ss.setSoTimeout(15000);
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		while(true){
			try {
				Socket s=ss.accept();
				ArrayList<ScoringJob> sj=getJob(s);
				for(ScoringJob j:sj){
					Future<ScoringJob> f=es.submit(j);
					d.addLast(f);
				}
			} catch(SocketTimeoutException e){
				//do nothing (we need this so that we empty the queue of the finished jobs every once in a while)
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("ABORTING!");
				return;
			}
			while(!d.isEmpty() && d.peekFirst().isDone()){
				ScoringJob j;
				try {
					j=d.removeFirst().get();
					System.err.println(new Date()+"  Job '"+j.toString()+"' finished with exit status "+j.exitStatus);
				} catch (Exception e){
					System.err.println("Failed to score job:");
					e.printStackTrace();
					System.err.println();
				}
			}
		}
	}
	
	private ArrayList<ScoringJob> getJob(Socket s) throws IOException{
		ArrayList<ScoringJob> r=new ArrayList<ScoringJob>();
		BufferedReader reader=new BufferedReader(new InputStreamReader(s.getInputStream()));
		String line;
		while((line=reader.readLine())!=null && line.length()>0){
			try {
				ScoringJob job=new ScoringJob(line);
				r.add(job);
			} catch(IllegalArgumentException e){
				System.err.println("Failed to parse job:");
				System.err.println(line);
				System.err.println("IGNORING!");
				continue;
			}
		}
		reader.close();
		s.close();
		return r;
	}
	
	public ScoringJob makeJob(File gold,File pred,File out){
		return new ScoringJob(gold,pred,out);
	}
	
	
	class ScoringJob implements Callable<ScoringJob> {
		final File gold;
		final File pred;
		final File output;
		int exitStatus=-1;
		public ScoringJob(File gold, File pred, File output) {
			this.gold = gold;
			this.pred = pred;
			this.output = output;
		}
		public ScoringJob(String s){
			String[] a=s.split("\\s+");
			if(a.length!=3)
				throw new IllegalArgumentException("Can't parse job: '"+s+"'");
			this.gold=new File(a[0]);
			this.pred=new File(a[1]);
			this.output=new File(a[2]);

		}
		public ScoringJob call() throws Exception{
			Thread.sleep(25000); //Add some sleeping so the file system can sync
			//Check that files exist
			if(!this.gold.exists())
				throw new FileNotFoundException(this.gold.toString()+" doesn't exist.");
			if(!this.pred.exists())
				throw new FileNotFoundException(this.pred.toString()+" doesn't exist.");
			try {
				this.output.createNewFile();
			} catch (IOException e){
				throw new IllegalArgumentException("Can't write output to "+this.output,e);
			}
			ProcessBuilder pb=new ProcessBuilder(scorerExec.toString(),"all",gold.toString(),pred.toString(),"none");
			Map<String,String> env=pb.environment();
			env.put("PERL5LIB", scorerExec.getParent()+"/lib");
			pb.redirectErrorStream(true);
			Process p=pb.start();
			BufferedWriter out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output.toString())));
			InputStreamReader reader=new InputStreamReader(p.getInputStream());
			char[] buf=new char[1024];
			int c;
			while((c=reader.read(buf, 0, buf.length))!=-1){
				synchronized(out){
					out.write(buf, 0, c);
				}
			}
			reader.close();
			int exit=p.waitFor();
			out.close();
			this.exitStatus=exit;
			return this;
		}
		public String toString(){
			return job2String(gold,pred,output);
		}
	}
	
	public static String job2String(File gold,File pred,File out){
		return gold.getAbsolutePath()+"\t"+pred.getAbsolutePath()+"\t"+out.getAbsolutePath();
	}
	
	public static void sendJob(File gold,File pred,File out,String host,int port) throws UnknownHostException, IOException{
		Socket s=new Socket(host,port);
		OutputStreamWriter writer=new OutputStreamWriter(s.getOutputStream());
		writer.write(job2String(gold,pred,out));
		writer.write("\n");
		writer.close();
		s.close();
	}
	
	public static void main(String[] args) throws Exception{
		if(!(args.length==2 || args.length==3)){
			usage();
			System.exit(1);
		}
		int port=Integer.parseInt(args[0]);
		File exec=new File(args[1]);
		int threads=-1;
		if(args.length==3)
			threads=Integer.parseInt(args[2]);
		
		if(!exec.exists() || !exec.canExecute()){
			System.err.println("Scorer "+exec.toString()+" does not exist, or cannot be executed, aborting");
			System.exit(2);
		}
		
		ScoringServer ss=new ScoringServer(port,exec,threads);

		InetAddress localhost=InetAddress.getLocalHost();
		System.out.println("Launching scorer server on "+localhost.getHostName()+":"+port+" at "+new Date());
		System.out.println("Threads: "+threads);
		System.out.println();
		ss.run();
	}

	private static void usage() {
		System.err.println("Usage:");
		System.err.println("java -cp ... "+ScoringServer.class.getCanonicalName()+" <port> <scorer-binary> [threads]");
		System.err.println();
		System.err.println("if threads is omitted or <=0, then there is no limit to number of parallel jobs");
	}
}
