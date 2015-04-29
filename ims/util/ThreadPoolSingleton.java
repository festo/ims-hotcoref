package ims.util;

import ims.hotcoref.Options;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadPoolSingleton {

	
	private static ThreadPoolSingleton theInstance;
	private static int threads;
	
	private static void initiate(int threads){
		if(theInstance!=null)
			throw new RuntimeException("You are wrong here.");
		theInstance=new ThreadPoolSingleton(threads);
		ThreadPoolSingleton.threads=threads;
	}
	
	public static ThreadPoolSingleton getInstance(){
		if(theInstance==null)
			initiate(Options.cores);
		return theInstance;
	}
	
	public static void shutdown(){
		if(theInstance==null)
			return;
		theInstance.threadPool.shutdown();
		theInstance=null;
	}
	
	public static int threadCount(){
		if(theInstance==null)
			getInstance();
		return threads;
	}
	
	
	
	
	private ExecutorService threadPool;

	private ThreadPoolSingleton(int threads) {
		DBO.println("Initiating threadpool with "+threads+" threads");
		this.threadPool=Executors.newFixedThreadPool(threads);
	}

	
	
	
	
	public <T> Future<T> submit(Callable<T> job){
		return threadPool.submit(job);
	}
	
	public Future<?> submit(Runnable job){
		return threadPool.submit(job);
	}
	
	public <T> List<Future<T>> invokeAll(List<Callable<T>> jobs) {
		try {
			List<Future<T>> res=threadPool.invokeAll(jobs);
			return res;
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	public <T> List<Future<T>> submitAll(List<Callable<T>> jobs) {
		List<Future<T>> res=new ArrayList<Future<T>>();
		for(Callable<T> job:jobs){
			res.add(threadPool.submit(job));
		}
		return res;
	}
	
	public ExecutorService getThreadPool(){
//		if(theInstance==null)
//			getInstance();
		return theInstance.threadPool;
	}

}