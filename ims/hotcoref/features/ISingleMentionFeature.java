package ims.hotcoref.features;

import ims.hotcoref.data.Instance;

public interface ISingleMentionFeature extends IFeature {

	public int getIntValue(Instance inst,int idx);
	
}
