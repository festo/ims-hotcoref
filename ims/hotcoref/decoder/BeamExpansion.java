package ims.hotcoref.decoder;

import java.util.Collections;
import java.util.Comparator;

import ims.hotcoref.data.Instance;

public class BeamExpansion {
	final HOTState seed;
	final HOTEdge  ed;
	final double score;
	final int edgeLen;
	BeamExpansion(HOTState seed,HOTEdge ed){
		this.seed=seed;
		this.ed=ed;
		this.score=seed.score+ed.hotScore+ed.e.getScore();
		this.edgeLen=seed.edgeLenSum+ed.getLen();
	}

	HOTState toStateCertainlyGold(){
		HOTState r=seed.copy();
		r.appendEdge(ed);
		return r;
	}
	HOTState toState(Instance inst,boolean certainlyGold){
		HOTState r=seed.copy();
		r.appendEdge(ed);
		if(!certainlyGold && seed.getCorrect() && !Decoder.sameChain(ed.e.depIdx,ed.e.headIdx,inst))
			r.setCorrect(false);
		return r;
	}
	
	
	public static final Comparator<BeamExpansion> BEAM_EXPANSION_SCORE_AND_EDGELEN_COMPARATOR=new Comparator<BeamExpansion>(){
		@Override
		public int compare(BeamExpansion arg0, BeamExpansion arg1) {
			double a0s=arg0.score;
			double a1s=arg1.score;
			if(a0s<a1s) //lower score means smaller (worse)
				return -1;
			else if (a0s>a1s)
				return 1;
			
			int a0el=arg0.edgeLen;
			int a1el=arg1.edgeLen;
			if (a0el>a1el) //greater edge len sum means smaller (worse)
				return -1;
			else if (a0el<a1el)
				return 1;
			else
				return 0;
		}
	};
	public static final Comparator<BeamExpansion> REV_BEAM_EXPANSION_SCORE_AND_EDGELEN_COMPARATOR=Collections.reverseOrder(BEAM_EXPANSION_SCORE_AND_EDGELEN_COMPARATOR);
}
