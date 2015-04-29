package ims.hotcoref.features.enums;

import ims.util.Util;

import java.util.List;

public interface IBuckets<T extends Enum<T>> {
	
	public T getBucket(int i);
	public T getVNodeBucket();
	
	public static final List<Class<?>> bIdxMap=Util.listOfClasses(Buckets.class,BigBuckets.class);
	
}
