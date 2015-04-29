package ims.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RunJobs {
	
	//....RunJobs 5           -- use 5 threads, read STDIN
	//....RunJobs 5 -         -- use 5 threads, read STDIN
	//....RunJobs 5 <file>    -- use 5 threads, read <file>
	//....RunJobs 5 -|<file> [-initSleep <time>] --- 5 threads, read STDIN, and the first 5 jobs are delayed 0, 1*time, 2*time, etc seconds
	
	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException{
		int maxJobs=Integer.parseInt(args[0]);
		
		BufferedReader reader;
		if(args.length==1 || args[1].equals("-")){ //Read jobs from STDIN
			System.out.println("Reading jobs from STDIN");
			reader=new BufferedReader(new InputStreamReader(System.in));
		} else {
			reader=Util.getReader(new File(args[1]));
		}
		double initSleep=0;
		if(args.length>3){
			if(args[2].equals("-initSleep"))
				initSleep=Integer.parseInt(args[3]);
			else
				throw new Error("Unknown argument "+args[2]);
		}
		String[] cmds=read(reader);
		int[] init=getInitSleep(cmds.length,initSleep,maxJobs);
		reader.close();
		run(maxJobs,cmds,init);
	}
	

	private static int[] getInitSleep(int length, double initSleep, int threads) {
		int sleep[]=new int[length];
		if(initSleep==0)
			return sleep;
		for(int i=0;i<threads;++i){
			int time=(int) Math.floor(i*1000*initSleep);
			sleep[i]=time;
		}
		return sleep;
	}

	private static String[] read(BufferedReader reader) throws IOException {
		List<String> a=new ArrayList<String>();
		String line;
		while((line=reader.readLine())!=null)
			a.add(line.trim());
		return a.toArray(new String[a.size()]);

	}

	static final Pattern PARSE_CMD_LINE_PATTERN=Pattern.compile("([^\\&]*)\\&?\\>\\s*([^\\& ]*)(\\s*\\&\\s*)?");
	private static void run(int maxJobs, String[] cmds, int[] initSleep) throws InterruptedException, ExecutionException, IOException {
		List<Job> jobs=new ArrayList<Job>();
		for(int i=0;i<cmds.length;++i){
			String cmd=cmds[i];
			int sleep=initSleep[i];
			Matcher m=PARSE_CMD_LINE_PATTERN.matcher(cmd);
			if(m.matches()){
				jobs.add(new Job(m.group(1),new File(m.group(2)),sleep));
			} else {
				System.out.println("Falied to parse: "+cmd);
			}
		}
		ExecutorService threadPool=Executors.newFixedThreadPool(maxJobs);
		int failed=0;
		for(Future<Integer> f:threadPool.invokeAll(jobs)){
			if(f.get()!=0)
				failed++;
		}
		System.out.println("Completed "+jobs.size()+" jobs with "+failed+" failures");
		threadPool.shutdown();
	}
	
	static class Job implements Callable<Integer> {

		private final String cmd;
		private final File logFile;
		private final int initSleepTimeMillis;
		private BufferedWriter logWriter;
		public Job(String cmd,File logFile,int initSleepTimeMillis) throws IOException { 
			this.cmd=cmd; 
			this.logFile=logFile;
			this.initSleepTimeMillis=initSleepTimeMillis;
		}
		
		public synchronized void writeLog(char[] c,int len) throws IOException{
			logWriter.write(c, 0, len);
			logWriter.flush();
		}
		
		@Override
		public Integer call() throws Exception {
			if(initSleepTimeMillis>0)
				Thread.sleep(initSleepTimeMillis);
			this.logWriter=Util.getWriter(logFile);
			System.out.println(new Date()+": Executing: "+cmd);
			Process p=Runtime.getRuntime().exec(cmd);
			StreamSinkThread stdoutThread=new StreamSinkThread(this,p.getInputStream());
			StreamSinkThread stderrThread=new StreamSinkThread(this,p.getErrorStream());
			stdoutThread.start();
			stderrThread.start();
			int exit=p.waitFor();
			if(exit!=0)
				System.out.println("Non-zero exit status for '"+cmd+"' -- exited with "+exit);
			stdoutThread.join();
			stderrThread.join();
			logWriter.close();
			return exit;
		}
	}

	static class StreamSinkThread extends Thread {
		
		final InputStreamReader reader;
		final Job job;
		public StreamSinkThread(Job job,InputStream is){
			this.job=job;
			reader=new InputStreamReader(is);
		}
		
		public void run(){
			char[] buf=new char[1024];
			try {
				int c;
				while((c=reader.read(buf, 0, buf.length))!=-1){
					job.writeLog(buf,c);
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new Error("!");
			}
		}
	}
}
