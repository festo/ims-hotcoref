package ims.hotcoref.features.enums;

public enum ProbabilityBuckets {
	VNODE,
	B005,
	B010,
//	B015,
	B020,
//	B025,
	B030,
//	B035,
	B040,
//	B045,
	B050,
//	B055,
	B060,
//	B065,
	B070,
//	B075,
	B080,
	B085,
	B090,
	B092,
	B094,
//	B095,
	B096,
//	B097,
	B098,
	B099,
	REST;
	
	public static ProbabilityBuckets getBucket(double pr){
		if(pr<0.05)
			return B005;
		else if(pr<0.10)
			return B010;
		else if(pr<0.20)
			return B020;
		else if(pr<0.30)
			return B030;
		else if(pr<0.40)
			return B040;
		else if(pr<0.50)
			return B050;
		else if(pr<0.60)
			return B060;
		else if(pr<0.70)
			return B070;
		else if(pr<0.80)
			return B080;
		else if(pr<0.85)
			return B085;
		else if(pr<0.90)
			return B090;
		else if(pr<0.92)
			return B092;
		else if(pr<0.94)
			return B094;
//		else if(pr<0.95)
//			return B095;
		else if(pr<0.96)
			return B096;
		else if(pr<0.98)
			return B098;
		else if(pr<0.99)
			return B099;
		else
			return REST;
	}
	
	public static ProbabilityBuckets getVNodeBucket(){
		return VNODE;
	}
}
