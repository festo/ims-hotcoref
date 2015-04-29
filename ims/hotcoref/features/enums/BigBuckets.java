package ims.hotcoref.features.enums;

public enum BigBuckets implements IBuckets<BigBuckets>{
	None,
	VNODE,
	Zero, One, Two, Three, Four, LTEight, LTTwelve, LTTwenty, Rest,
	LTSixteen, LTThirty, LTFourty, LTFifty;
	
	public BigBuckets getBucket(int dist){
		if(dist<0)
			return None;
		switch(dist){
		case 0: return BigBuckets.Zero;
		case 1: return BigBuckets.One;
		case 2: return BigBuckets.Two;
		case 3: return BigBuckets.Three;
		case 4: return BigBuckets.Four;
		}
		
		if(dist<8)
			return BigBuckets.LTEight;
		if(dist<12)
			return BigBuckets.LTTwelve;
		if(dist<16)
			return BigBuckets.LTSixteen;
		if(dist<20)
			return BigBuckets.LTTwenty;
		if(dist<30)
			return BigBuckets.LTThirty;
		if(dist<40)
			return BigBuckets.LTFourty;
		if(dist<50)
			return BigBuckets.LTFifty;
		
		return BigBuckets.Rest;
	}

	@Override
	public BigBuckets getVNodeBucket() {
		return VNODE;
	}

}
