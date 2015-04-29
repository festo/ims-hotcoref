package ims.hotcoref.perceptron;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.procedure.TIntIntProcedure;

public interface IFV<T extends IFV<T>> {

	public double getScore(float[] parameters);
	public void add(int i);
	public void add(int[] i);
//	public void update(ParametersFloat pf, double tau, double upd);
//	public void updateCW(ParametersFloat pf,double alpha,double upd,float[] sigmaDiag);
//	public int selfDotProduct();
	public TIntIntHashMap getMap();
	public TIntIntHashMap getDistVector(IFV<T> o);
	public void clear();
	


	static class ScoreProcedure implements TIntIntProcedure{
		final float[] parameters;
		double sum=0.d;
		ScoreProcedure(float[] parameters){
			this.parameters=parameters;
		}
		@Override
		public boolean execute(int arg0, int arg1) {
			sum+=parameters[arg0]*arg1;
			return true;
		}
	}

}
