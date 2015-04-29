package ims.util;

public class PrecisionRecall {

	public int tp=0;
	public int fp=0;
	public int fn=0;
	
	public PrecisionRecall(){}
	public PrecisionRecall(int tp,int fp,int fn){
		this.tp=tp;
		this.fp=fp;
		this.fn=fn;
	}
	
	public double getF1(){
		double p=getPrecision();
		double r=getRecall();
		double f=2*p*r/(p+r);
		return f;
	}
	
	public double getPrecision(){
		return 100.0d*tp/(tp+fp);
	}
	
	public double getRecall(){
		return 100.0d*tp/(tp+fn);
	}
	
	public String getPrecisionStr(){
		return String.format("Precision:   100 * %6d/(%6d + %6d) = %6.3f", tp,tp,fp,getPrecision()); 
	}

	public String getRecallStr(){
		return String.format("Recall:      100 * %6d/(%6d + %6d) = %6.3f", tp,tp,fn,getRecall());
	}
	
	public String getF1Str(){
		return String.format("F1:          %6.3f", getF1());
	}
	
	public String getPRFString(){
		return getPrecisionStr()+"\n"+getRecallStr()+"\n"+getF1Str();
	}
	
	
}
