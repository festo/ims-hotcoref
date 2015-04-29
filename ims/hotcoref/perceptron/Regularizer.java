package ims.hotcoref.perceptron;

public abstract class Regularizer {

	public abstract double getTau(double numerator,double distTwoNormSquared);
	public abstract String toString();
	
	public static enum RegularizerTypes { PA0, PA1, PA2, Perceptron }
	
	public static Regularizer getRegularizer(RegularizerTypes t,double C){
		switch(t){
		case PA0: return new PA0Regularizer();
		case PA1: return new PA1Regularizer(C);
		case PA2: return new PA2Regularizer(C);
		case Perceptron: return new RegularPerceptron();
		}
		throw new Error("not implemented");
	}
	
	/*
	 * Loss functions as per Crammer et al 2006.
	 */
	public static class PA0Regularizer extends Regularizer {
		@Override
		public double getTau(double numerator, double distTwoNormSquared) {
			return numerator/distTwoNormSquared;
		}
		@Override
		public String toString() {
			return "PA0";
		}
	}
	public static class PA1Regularizer extends Regularizer {
		private final double C;
		public PA1Regularizer(double C){
			this.C=C;
		}
		@Override
		public double getTau(double numerator, double distTwoNormSquared) {
			return Math.min(C, numerator/distTwoNormSquared);
		}
		@Override
		public String toString() {
			return "PA1, C="+C;
		}
	}
	public static class PA2Regularizer extends Regularizer {
		private final double Cdouble;
		public PA2Regularizer(double C){
			this.Cdouble=2*C;
		}
		@Override
		public double getTau(double numerator, double distTwoNormSquared) {
			return numerator/(distTwoNormSquared+Cdouble);
		}
		@Override
		public String toString() {
			return "PA2, C="+(Cdouble/2.0d);
		}
	}
	public static class RegularPerceptron extends Regularizer {
		@Override
		public double getTau(double numerator, double distTwoNormSquared) {
			return 1.0f;
		}
		@Override
		public String toString() {
			return "Perceptron";
		}
	}
}
