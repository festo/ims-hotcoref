package ims.hotcoref.perceptron;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.procedure.TIntIntProcedure;
import gnu.trove.procedure.TIntProcedure;

import java.util.Arrays;

public class ParametersFloat {

	public final float[] parameters;
	public final float[] total;

	final Regularizer reg;
	
	public ParametersFloat(int size,Regularizer reg){
		parameters=new float[size];
		total=new float[size];
		this.reg=reg;
	}
	
	public void average(float avVal) {
		for(int j=0;j<total.length;++j)
			parameters[j]-=total[j]/avVal;
		Arrays.fill(total,Float.NEGATIVE_INFINITY);
	}
	public float[] averagedCopy(float avVal){
		float[] r=new float[parameters.length];
		for(int i=0;i<r.length;++i)
			r[i]=parameters[i]-total[i]/avVal;
		return r;
	}
	
	public int countNZ(){
		int notZero=0;
		for(int i=0;i<parameters.length;i++)
			if(parameters[i]!=0.f)
				notZero++; 
		return notZero;
	}
	
	public static final float F_TH=0.00001f;
	public int countNZ2(){
		int notZero=0;
		for(int i=0;i<parameters.length;i++)
				if(parameters[i]!=0.f && Math.abs(parameters[i])-F_TH>0.f)
					notZero++; 
		return notZero;
	}
	
	public String getNonZeroWeights2(boolean indicesOnly){
		StringBuilder sb=new StringBuilder();
		for(int i=0;i<parameters.length;i++)
			if(parameters[i]!=0.f && Math.abs(parameters[i])-F_TH>0.f){
				sb.append(i);
				if(!indicesOnly) sb.append('=').append(parameters[i]);
				sb.append(", ");
			}
		return sb.toString();
	}
	
	public String getNonZeroWeights(boolean indicesOnly){
		StringBuilder sb=new StringBuilder();
		for(int i=0;i<parameters.length;i++)
			if (parameters[i]!=0.0F){
				sb.append(i);
				if(!indicesOnly) sb.append('=').append(parameters[i]);
				sb.append(", ");
			}
		return sb.toString();
	}
	
	public boolean update(UpdStruct us){
		if(us.dv==null || us.dv.isEmpty())
			return false;
		float distTwoNormSquared=selfDotProduct(us.dv);
		if(distTwoNormSquared<0.000001){
			System.out.println("\nRETURNING DUE TO TOO SMALL DIST NORM: "+distTwoNormSquared);
			return false;
		}
		double tau=reg.getTau(us.num, distTwoNormSquared);
		if(tau<0.000001){
			System.out.println("\nReturning due to too small tau: "+tau);
			return false;
		}
		update(tau,us.upd,us.dv);
		return true;
	}
	
	public static class UpdStruct {
		final TIntIntHashMap dv;
		final double num;
		final double upd;
		public final double loss;
		public UpdStruct(double predScore,double actScore,double loss,double upd,TIntIntHashMap dv){
			if(predScore<actScore)
				System.out.println("\nPred score less than act score!!: "+predScore +" < "+actScore);
			this.dv=dv;
			this.upd=upd;
			this.num=predScore-actScore+loss;
			this.loss=loss;
		}
	}
	
//	public int batchUpdate(List<UpdStruct> uss,double upd) {
//		DistVectorMergeProcedure dvmp=new DistVectorMergeProcedure(new TIntIntHashMap());
//		double accLoss=0.d;
//		for(UpdStruct us:uss){
//			us.dv.forEachEntry(dvmp);
//			accLoss+=us.loss;
//		}
//		double distTwoNormSquared=selfDotProduct(dvmp.sink);
//		if(distTwoNormSquared<1){
//			System.out.println("RETURNING DUE TO TOO SMALL DISTNORM WITH BATCH UPD");
//			return uss.size();
//		}
//		//compute numerator for merged dist.
//		TIntIntIterator it=dvmp.sink.iterator();
//		double num=0.d;
//		while(it.hasNext()){
//			it.advance();
//			num-=parameters[it.key()]*it.value();
//		}
//		num+=accLoss;
//		
//		double tau=reg.getTau(num, distTwoNormSquared);
//		update(tau,upd,dvmp.sink);
//
//		return 0;
//	}
	
	private void update(double tau,double upd,TIntIntHashMap m){
		UpdateProcedure up=new UpdateProcedure(tau,upd);
		m.forEachEntry(up);
	}
	
	class UpdateProcedure implements TIntIntProcedure{
		final double tau;
		final double upd;
		
		UpdateProcedure(double tau,double upd){
			this.tau=tau;
			this.upd=upd;
		}
		@Override
		public boolean execute(int arg0, int arg1) {
			parameters[arg0]+=arg1*tau;
			total[arg0]+=upd*arg1*tau;
			return true;
		}
	}

	
	static int selfDotProduct(TIntIntHashMap m){
		SelfDotProductProcedure sdpp=new SelfDotProductProcedure();
		m.forEachValue(sdpp);
		return sdpp.sum;
	}
	static class SelfDotProductProcedure implements TIntProcedure {
		int sum=0;
		@Override
		public boolean execute(int arg0) {
			sum+=arg0*arg0;
			return true;
		}
	}

	public boolean update(FV3 predFV, FV3 goldFV, float accLoss,double upd) {
		TIntIntHashMap dv=goldFV.getDistVector(predFV);

		
//		double num=predFV.getScoreKahanSummation(parameters) - goldFV.getScoreKahanSummation(parameters);
		
		double num=predFV.getScore(parameters) - goldFV.getScore(parameters);
		
		//worse precision
//		double num=0.d;
//		for(TIntIterator pIt=predFV.l.iterator();pIt.hasNext();)
//			num+=parameters[pIt.next()];
//		for(TIntIterator gIt=goldFV.l.iterator();gIt.hasNext();)
//			num-=parameters[gIt.next()];
//		
		
		
		//This way of computing num can be indeterministic if the l2i is a proper hash map 
		//(because the internal ordering of the dv hashmap can change, thus leading to different 
		// results in floating point arithmetics in the loop below)
//		TIntIntIterator it=dv.iterator();
//		double num=0.d;
//		while(it.hasNext()){
//			it.advance();
//			num-=parameters[it.key()]*it.value();
//		}
		
		
		
		num+=accLoss;
		
		
		double distTwoNormSquared=selfDotProduct(dv);
		if(distTwoNormSquared<1){
			System.out.println("RETURNING DUE TO TOO SMALL DISTNORM WITH BATCH UPD");
			return false;
		}
		double tau=reg.getTau(num, distTwoNormSquared);
		if(tau<0.000001){
			System.out.println("\nReturning due to too small tau: "+tau);
			return false;
		}
		update(tau,upd,dv);
		return true;
	}
	
//	public static TIntIntHashMap mergeDistVectors(List<TIntIntHashMap> dvs){
//	DistVectorMergeProcedure dvmp=new DistVectorMergeProcedure(new TIntIntHashMap());
//	for(TIntIntHashMap dv:dvs)
//		dv.forEachEntry(dvmp);
//	return dvmp.sink;
//}

//static class DistVectorMergeProcedure implements TIntIntProcedure {
//	final TIntIntHashMap sink;
//	DistVectorMergeProcedure(TIntIntHashMap sink){
//		this.sink=sink;
//	}
//	@Override
//	public boolean execute(int arg0, int arg1) {
//		sink.adjustOrPutValue(arg0, arg1, arg1);
//		return true;
//	}
//}
}
