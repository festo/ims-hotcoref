package ims.hotcoref.perceptron;

import java.util.concurrent.Future;

import ims.util.ThreadPoolSingleton;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;

public class FV3 implements IFV<FV3>{

	final TIntArrayList l;
	public FV3(){
		l=new TIntArrayList(200);
	}
	
	@Override
	public double getScore(float[] parameters) {
		double sum=0.d;
		for(int i=0,ma=l.size();i<ma;++i)
			sum+=parameters[l.getQuick(i)];
		return sum;
	}
	
	//cf. http://en.wikipedia.org/wiki/Kahan_summation_algorithm
	public double getScoreKahanSummation(float[] parameters){
		double sum=0.d;
		double c=0.d;
		for(int i=0,ma=l.size();i<ma;++i){
			double y = parameters[l.getQuick(i)] - c;
			double t = sum + y;
			c = t - sum;
			c = c - y;
			sum = t ;
		}
		return sum;
	}

	@Override
	public void add(int i) {
		l.add(i);
	}

	@Override
	public void add(int[] i) {
		l.add(i);
	}

	@Override
	public TIntIntHashMap getMap() {
		TIntIntHashMap m=new TIntIntHashMap();
		for(int i=0,ma=l.size();i<ma;++i)
			m.adjustOrPutValue(l.getQuick(i), 1, 1);
		return m;
	}

	public TIntIntHashMap getDistVector(IFV<FV3> fl2) {
		FV3 o=(FV3) fl2;
		TIntIntHashMap m=new TIntIntHashMap();
		for(int i=0,ma=l.size();i<ma;++i)
			m.adjustOrPutValue(l.getQuick(i), 1, 1);
		for(int i=0,ma=o.l.size();i<ma;++i)
			m.adjustOrPutValue(o.l.getQuick(i), -1, -1);
		return m;
	}
	
	//this one could've been faster by sorting the lists and then doing merge-sort style merging (means fewer hash lookups)
	//but i guesss sorting the lists take too long anyway...
	public TIntIntHashMap getDistVectorF(IFV<FV3> fl2) {
		FV3 o=(FV3) fl2;
		Future<?> f=ThreadPoolSingleton.getInstance().submit(new Runnable(){ public void run(){ l.sort(); }});
		o.l.sort();
		try { f.get(); } catch (Exception e) {throw new RuntimeException(e);}

		TIntArrayList aL=l;
		TIntArrayList bL=o.l;
		int aM=aL.size();
		int bM=bL.size();
		TIntIntHashMap m=new TIntIntHashMap();
		int aN=0,aK=0,aV=0;
		int bN=0,bK=0,bV=0;
		if(aN==aM) {aN=-1;} else {aK=aL.getQuick(aN);aV=1; while(aN+1<aM && aL.getQuick(aN+1)==aK){++aV;++aN;} ++aN;}
		if(bN==bM) {bN=-1;} else {bK=bL.getQuick(bN);bV=1; while(bN+1<bM && bL.getQuick(bN+1)==bK){++bV;++bN;} ++bN;}

		while(aN!=-1 && bN!=-1){
			if(aK==bK){
				m.put(aK,aV-bV);
				if(aN==aM) {aN=-1;} else {aK=aL.getQuick(aN);aV=1; while(aN+1<aM && aL.getQuick(aN+1)==aK){++aV;++aN;} ++aN;}
				if(bN==bM) {bN=-1;} else {bK=bL.getQuick(bN);bV=1; while(bN+1<bM && bL.getQuick(bN+1)==bK){++bV;++bN;} ++bN;}
			} else if(aK<bK){
				m.put(aK, aV);
				if(aN==aM) {aN=-1;} else {aK=aL.getQuick(aN);aV=1; while(aN+1<aM && aL.getQuick(aN+1)==aK){++aV;++aN;} ++aN;}
			} else {
				m.put(bK, -bV);
				if(bN==bM) {bN=-1;} else {bK=bL.getQuick(bN);bV=1; while(bN+1<bM && bL.getQuick(bN+1)==bK){++bV;++bN;} ++bN;}
			}
		}
		while(aN!=-1){
			m.put(aK, aV);
			if(aN==aM) {aN=-1;} else {aK=aL.getQuick(aN);aV=1; while(aN+1<aM && aL.getQuick(aN+1)==aK){++aV;++aN;} ++aN;}
		}
		while(bN!=-1){
			m.put(bK, -bV);
			if(bN==bM) {bN=-1;} else {bK=bL.getQuick(bN);bV=1; while(bN+1<bM && bL.getQuick(bN+1)==bK){++bV;++bN;} ++bN;}
		}
		return m;
	}
//	public TIntIntHashMap getDistVector(IFV<FV3> fl2) {
//		FV3 o=(FV3) fl2;
//		Future<?> f=ThreadPoolSingleton.getInstance().submit(new Runnable(){ public void run(){ l.sort(); }});
//		o.l.sort();
//		try { f.get(); } catch (Exception e) {throw new RuntimeException(e);}
//
//		TIntArrayList aL=l;
//		TIntArrayList bL=o.l;
//		
//		TIntIntHashMap m=new TIntIntHashMap();
//		
//		R a=getR(aL,0);
//		R b=getR(bL,0);
//		while(a!=null && b!=null){
//			if(a.k==b.k){
//				int v=a.v-b.v;
//				m.put(a.k, v);
//				a=getR(aL,a.n);
//				b=getR(bL,b.n);
//			} else if(a.k<b.k){
//				m.put(a.k, a.v);
//				a=getR(aL,a.n);
//			} else {
//				m.put(b.k, -b.v);
//				b=getR(bL,b.n);
//			}
//		}
//		while(a!=null){
//			m.put(a.k, a.v);
//			a=getR(aL,a.n);
//		}
//		while(b!=null){
//			m.put(b.k, -b.v);
//			b=getR(bL,b.n);
//		}
//		return m;
//	}
//	static R getR(TIntArrayList l,int ix){
//		if(ix==l.size())
//			return null;
//		int k=l.get(ix);
//		int v=1;
//		for(;ix+1<l.size() && l.getQuick(ix+1)==k;){
//			++v;
//			++ix;
//		}
//		R r=new R();
//		r.n=++ix;
//		r.k=k;
//		r.v=v;
//		return r;
//	}
//	
//	static class R {
//		int n;
//		int k;
//		int v;
//	}
	

	@Override
	public void clear() {
		l.resetQuick();
	}

}
