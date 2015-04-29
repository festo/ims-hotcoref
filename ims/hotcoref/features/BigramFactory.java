package ims.hotcoref.features;

public class BigramFactory {

	public static IFeature getBigram(IFeature af1, IFeature af2, int cutOff) {
		if(af1 instanceof AbstractSinglePairFeature && af2 instanceof AbstractSinglePairFeature)
			return new AbstractSingleSinglePairFeatureBigram(af1.getName()+"+"+af2.getName(),(AbstractSinglePairFeature)af1,(AbstractSinglePairFeature)af2);
		else if(af1 instanceof AbstractSinglePairFeature && af2 instanceof AbstractMultiPairFeature ||
		   af1 instanceof AbstractMultiPairFeature && af2 instanceof AbstractSinglePairFeature){
			final AbstractSinglePairFeature single;
			final AbstractMultiPairFeature multi;
			if(af1 instanceof AbstractMultiPairFeature){
				single=(AbstractSinglePairFeature) af2;
				multi=(AbstractMultiPairFeature) af1;
			} else {
				single=(AbstractSinglePairFeature) af1;
				multi=(AbstractMultiPairFeature) af2;
			}
			return new AbstractMultiSinglePairFeature(multi, single);
		} else if(af1 instanceof AbstractMultiPairFeature && af2 instanceof AbstractMultiPairFeature){
			return new AbstractMultiMultiPairFeature((AbstractMultiPairFeature)af1,(AbstractMultiPairFeature)af2);
		}
		throw new Error("not implemented: "+af1.toString()+" + "+af2.toString());
	}

}
