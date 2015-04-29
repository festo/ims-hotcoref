package ims.hotcoref.features;

import ims.hotcoref.data.Instance;

public interface ISingleMentionMultiFeature extends IFeature {

	public long[] getLongValues(Instance inst,int idx);
	
}
