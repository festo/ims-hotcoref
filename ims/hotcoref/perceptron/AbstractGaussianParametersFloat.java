package ims.hotcoref.perceptron;

import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.procedure.TIntIntProcedure;

import ims.util.DBO;
import ims.util.DoublePair;

import java.util.Arrays;

public abstract class AbstractGaussianParametersFloat extends ParametersFloat {

	protected final float[] sigmaDiag;
	protected final float[] sigmaDiagInv;
	
	public AbstractGaussianParametersFloat(int size,float initVar,Regularizer reg) {
		super(size,reg);
		sigmaDiag=new float[size];
		sigmaDiagInv=new float[size];
		Arrays.fill(sigmaDiag, initVar);
		Arrays.fill(sigmaDiagInv, 1.0f/initVar);
	}

	public <T extends IFV<T>> boolean update(IFV<T> act, IFV<T> pred, double upd, float loss,Regularizer regularizer) {
		
		TIntIntHashMap distMap = act.getDistVector(pred);
		
		double v=0.d;
		{
			TIntIntIterator it=distMap.iterator();
			while(it.hasNext()){
				it.advance();
				int k=it.key();   //Index
				int w=it.value(); //Value
				v+=sigmaDiag[k]*w*w;
			}
		}

		double m=act.getScore(parameters)-pred.getScore(parameters);
		DoublePair dp=getAlphaBeta(m,loss,v);

		double alpha=dp.d1;
		double beta=dp.d2;
		CWParametersUpdateProcedure cwpup=new CWParametersUpdateProcedure(parameters,total,sigmaDiag,alpha,upd);
		distMap.forEachEntry(cwpup);
		
		CWSigmaUpdateProcedure cwsup=new CWSigmaUpdateProcedure(sigmaDiag,sigmaDiagInv,beta);
		distMap.forEachEntry(cwsup);
		return true;
	}

	static class CWParametersUpdateProcedure implements TIntIntProcedure {
		final float[] parameters;
		final float[] total;
		final float[] sigmaDiag;
		final double alpha;
		final double upd;
		CWParametersUpdateProcedure(float[] parameters, float[] total,float[] sigmaDiag, double alpha, double upd) {
			this.parameters = parameters;
			this.total = total;
			this.sigmaDiag = sigmaDiag;
			this.alpha = alpha;
			this.upd = upd;
		}
		
		@Override
		public boolean execute(int arg0, int arg1) {
			double p=sigmaDiag[arg0]*arg1*alpha;
			parameters[arg0]+=p;
			total[arg0]+=upd*p;
			return true;
		}
	}
	
	static class CWSigmaUpdateProcedure implements TIntIntProcedure {
		final float[] sigmaDiag;
		final float[] sigmaDiagInv;
		final double beta;
		public CWSigmaUpdateProcedure(float[] sigmaDiag,float[] sigmaDiagInv,double beta) {
			this.sigmaDiag = sigmaDiag;
			this.sigmaDiagInv = sigmaDiagInv;
			this.beta = beta;
		}
		@Override
		public boolean execute(int arg0, int arg1) {
			sigmaDiagInv[arg0]+=beta*arg1*arg1;
			sigmaDiag[arg0]=1.0f/sigmaDiagInv[arg0];
			return true;
		}
	}
	
	abstract DoublePair getAlphaBeta(double m, float loss,double v);

	public static class CWParametersFloat extends AbstractGaussianParametersFloat {
		private final float fi;
		public CWParametersFloat(int size, float initVar, float fi,Regularizer reg) {
			super(size, initVar,reg);
			DBO.println("Initalizing CWParams: fi = "+fi+", initVar: "+initVar);
			this.fi=fi;
		}

		@Override
		DoublePair getAlphaBeta(double m, float loss,double v) {
			double fi_l = fi*loss;
			double fiP  = 1.0d+fi_l*fi_l/2.0d;
			double fiPP = 1.0d+fi_l*fi_l;
			
			
			double alpha = Math.max(0,1.0d/(v*fiPP) * (-m*fiP + Math.sqrt(m*m*Math.pow(fi_l, 4.0d) + v*fi_l*fi_l*fiPP)));		
			double vPlus = 0.25d * Math.pow((-alpha*v*fi_l + Math.sqrt(alpha*alpha*v*v*fi_l*fi_l + 4.0d*v)),2.0d);
			double beta  = alpha*fi_l/Math.sqrt(vPlus);
			
			return new DoublePair(alpha,beta);
		}
	}
	
	public static class AROWParametersFloat extends AbstractGaussianParametersFloat {
		private final float r;
		private final double beta;
		public AROWParametersFloat(int size, float initVar, float r,Regularizer reg) {
			super(size, initVar,reg);
			DBO.println("Initalizing AROWParams: r = "+r+", initVar: "+initVar);
			this.r=r;
			this.beta=1.0d/r;
		}

		@Override
		DoublePair getAlphaBeta(double m, float loss,double v) {
			double alpha=Math.max(0, loss-m)/(r+v);
			return new DoublePair(alpha,beta);
		}
	}
}
