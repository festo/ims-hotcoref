package ims.hotcoref.features.enums;

public enum Buckets implements IBuckets<Buckets> {
	None,
	VNODE,
	Zero, One, Two, Three, Four, LTEight, LTTwelve, LTTwenty, Rest;
	
	public Buckets getBucket(int dist){
		if(dist<0)
			return None;
		switch(dist){
		case 0: return Buckets.Zero;
		case 1: return Buckets.One;
		case 2: return Buckets.Two;
		case 3: return Buckets.Three;
		case 4: return Buckets.Four;
		}
		
		if(dist<8)
			return Buckets.LTEight;
		if(dist<12)
			return Buckets.LTTwelve;
		if(dist<20)
			return Buckets.LTTwenty;
		
		return Buckets.Rest;
	}

	@Override
	public Buckets getVNodeBucket() {
		return VNODE;
	}

}
