package ims.hotcoref.markables;

import ims.hotcoref.data.Sentence;
import ims.hotcoref.data.Span;

import java.util.Set;

public class LeftDepConjunctExtractor extends AbstractMarkableExtractor {
	private static final long serialVersionUID = -6101411888557148886L;

	@Override
	public void extractMarkables(Sentence s, Set<Span> sink, String docName) {
		for(int i=1;i<s.forms.length;++i){
			String tag=s.tags[i];
			if(!tag.startsWith("N"))
				continue;
			//The three cases (that I can think of :)
			// 1. (a) and (b) -- standard two conjunct coordination
			// 2. (a), (b) [,/and ...] -- enumeration
			// 3. (a), and (b) -- serial comma (could be at the end, or not)
			
			//Now what's interesting is the rightmost dep. Is this a coordination (of sorts) ?
			int rmc = s.dt.getRightMostChild(i);
			if(rmc==-1)
				continue;
			String rmcTag = s.tags[rmc];
			String rmcRel = s.dt.lbls[rmc];
			int lmd=s.dt.getLeftMostDep(i);
			if(rmcTag.equals("CC") && rmcRel.equals("COORD")){ //Case 1
				//Collect this conjunct and continue
				int beg = lmd == -1 ? i : lmd;
				int end = rmc-1;
				Span span=s.getSpan(beg, end);
				sink.add(span);
				continue;
			}
			int commaIndex=-1;
			for(int k=i+1;k<rmc;++k){
				if(s.forms[k].equals(",")){
					commaIndex = k;
					break;
				}
			}
			if(rmcRel.equals("CONJ") && commaIndex!=-1){ //Case 2 and 3
				int beg = lmd == -1 ? i : lmd;
				int end = commaIndex -1;
				Span span=s.getSpan(beg,end);
				sink.add(span);
				continue;
			}
		}
	}

	
	
	
// Training set	
	
//			bn/voa/01/voa_0152      0       0       Portals NNS     (TOP(S(NP*)     portal  -       1       -       *       2       SBJ     (ARG1*) -
//			bn/voa/01/voa_0152      0       1       will    MD      (VP*    -       -       -       -       *       0       ROOT    (ARGM-MOD*)     -
//			bn/voa/01/voa_0152      0       2       be      VB      (VP*    be      -       -       -       *       2       VC      *       -
//			bn/voa/01/voa_0152      0       3       added   VBN     (VP*    add     02      1       -       *       3       VC      (V*)    -
//			bn/voa/01/voa_0152      0       4       in      IN      (PP*    -       -       -       -       *       4       TMP     (ARGM-TMP*      -
//			bn/voa/01/voa_0152      0       5       2001    CD      (NP*))  -       -       -       -       (DATE)  5       PMOD    *)      -
//			bn/voa/01/voa_0152      0       6       for     IN      (PP*    -       -       -       -       *       4       ADV     (ARGM-PNC*      -
//			bn/voa/01/voa_0152      0       7       Germany NNP     (NP(NP*)        -       -       -       -       (GPE)   7       PMOD    *       -
//			bn/voa/01/voa_0152      0       8       ,       ,       *       -       -       -       -       *       8       P       *       -
//			bn/voa/01/voa_0152      0       9       France  NNP     (NP*)   -       -       -       -       (GPE)   8       CONJ    *       -
//			bn/voa/01/voa_0152      0       10      ,       ,       *       -       -       -       -       *       10      P       *       -
//			bn/voa/01/voa_0152      0       11      Italy   NNP     (NP*)   -       -       -       -       (GPE)   10      CONJ    *       -
//			bn/voa/01/voa_0152      0       12      ,       ,       *       -       -       -       -       *       12      P       *       -
//			bn/voa/01/voa_0152      0       13      and     CC      *       -       -       -       -       *       12      COORD   *       -
//			bn/voa/01/voa_0152      0       14      Spain   NNP     (NP*))))))      -       -       -       -       (GPE)   14      CONJ    *)      -
//			bn/voa/01/voa_0152      0       15      .       .       *))     -       -       -       -       *       2       P       *       -

	
	
}
