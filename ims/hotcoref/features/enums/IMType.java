package ims.hotcoref.features.enums;

import java.util.List;

import ims.hotcoref.mentiongraph.INode;
import ims.util.Util;

public interface IMType<T> {

	public T getType(INode node);
	public int getNone();

	public static final List<Class<?>> mtIdxMap=Util.listOfClasses(MTypeFine.class,MTypeCoarse.class);
	
}
