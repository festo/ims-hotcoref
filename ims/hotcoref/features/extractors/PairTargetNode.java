package ims.hotcoref.features.extractors;

public enum PairTargetNode {
	MFrom,   //DONT MOVE THESE TWO
	MTo,     //CAUSE WE ASSUME THEY ARE 0 AND 1 BY ORDINAL IN PairTargetNodeExtractor.getFOIdx
	MGP,
	MSib,
//	MTPre1,
	MLinFirst,
//	MTPre2,
	MLinPre,
//	MTPre3,
//	MTPreSS1,
	MLinPre2,
	MLinPre3,
//	MTPreSS3
	
	;
	
	public PairTargetNodeExtractor getExtractor(){
		return PairTargetNodeExtractor.getExtractor(this);
	}
	
}
