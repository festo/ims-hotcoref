package ims.hotcoref.lang;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import ims.hotcoref.data.Sentence;
import ims.hotcoref.data.Span;
import ims.hotcoref.data.CFGTree.CFGNode;
import ims.hotcoref.data.CFGTree.NonTerminal;
import ims.hotcoref.data.CFGTree.Terminal;
import ims.hotcoref.features.FeatureSet;
import ims.hotcoref.headrules.HeadFinder;
import ims.hotcoref.headrules.HeadRules;
import ims.hotcoref.headrules.HeadRules.*;

public class Chinese extends Language {
	private static final long serialVersionUID = 1L;

	
	@Override
	public FeatureSet getDefaultFeatureSet() {
		String[] names={
				"MFromHdForm+MToHdForm",							//1
				"MFromHdPos",										//2
				"ExactStringMatch+MToMTypeCoarse",					//3
				"DistanceBuckets",									//4
				"DistanceBuckets+MToMTypeCoarse",					//5
				"SameSpeaker+MFromPronounForm+MToPronounForm",		//6
				"MFromWholeSpanForm",								//7
				"MFromSPrPos",										//8
				"MToNodeCFGSubCat+Nested",							//9
				"Genre+Nested",										//10
				"CFGSSPath",										//11
				"MToSFForm+MFromHdForm",							//12
				"MFromSPrForm",										//13
				"CFGDSPath",										//14
				"MToSPrPos",										//15
				"Genre+MFromSFForm",								//16
				"MFromNodeCFGSubCat",								//17
				"CFGSSPath+MToPronounForm",							//18
				"MFromWholeSpanForm+MToWholeSpanForm",				//19
				"DistanceBuckets+MToHdForm",						//20
				"ExactStringMatch+DistanceBuckets",					//21
				"MFromParentNodeCFGSubCat",							//22
				"Genre+MFromHdForm",								//23
				"Nested",											//24
				"CFGSSPath+Genre",									//25
				"MFromNodeCFGSubCat+Nested",						//26
				"Genre+Nested+MToHdFormSubStringMatch+MToMTypeCoarse+MFromMTypeCoarse", //27
				"MFromParentNodeCFGSubCat+MToSPrPos",				//28
				"MToHdFormSubStringMatch+MToMTypeCoarse+MFromMTypeCoarse+MentionDistBigBuckets", //29
				"MFromParentNodeCFGSubCat+MToHdForm",				//30
				
//			      1 AntecedentHdForm+AnaphorHdForm
//			      2 AntecedentHdPos
//			      3 ExactStringMatch+AnaphorPronoun
//			      4 DistanceBucketed
//			      5 DistanceBucketed+AnaphorPronoun
//			      6 SameSpeaker+AntecedentPronounForm+AnaphorPronounForm
//			      7 AntecedentWholeSpanForm
//			      8 AntecedentSPrPos
//			      9 AnaphorCFGSubCat+Nested
//			     10 Genre+Nested
//			     11 CFGSSPath
//			     12 AnaphorSFForm+AntecedentHdForm
//			     13 AntecedentSPrForm
//			     14 CFGDSPath
//			     15 AnaphorSPrPos
//			     16 Genre+AntecedentSFForm
//			     17 AntecedentCFGSubCat
//			     18 CFGSSPath+AnaphorPronounForm
//			     19 AnaphorWholeSpanForm+AntecedentWholeSpanForm
//			     20 DistanceBucketed+AnaphorHdForm
//			     21 ExactStringMatch+DistanceBucketed
//			     22 AntecedentCFGParentSubCat
//			     23 Genre+AntecedentHdForm
//			     24 Nested
//			     25 CFGSSPath+Genre
//			     26 AntecedentCFGSubCat+Nested
//			     27 Genre+Nested+AnaphorHdFormSubStringMatch+AnaphorProperName+AntecedentProperName
//			     28 AntecedentCFGParentSubCat+AnaphorSPrPos
//			     29 AnaphorHdFormSubStringMatch+AnaphorProperName+AntecedentProperName+MentionDistBigBuckets+AntecedentPronoun
//			     30 AntecedentCFGParentSubCat+AnaphorHdForm
		};
		return FeatureSet.getFromNameArray(names);
	}
	
//	@Override
//	public FeatureSet getDefaultFeatureSet() {
//		String[] names={
//				"AntecedentHdForm+AnaphorHdForm",
//				"AntecedentHdPos",
//				"ExactStringMatch",
//				"DistanceBucketed",
//				"DistanceBucketed+AnaphorPronoun",
//				"SameSpeaker+AntecedentPronounForm+AnaphorPronounForm",
//				"AntecedentWholeSpanForm",
//				"AntecedentSPrPos",
//				"AnaphorCFGSubCat+Nested",
//				"Genre+Nested",
//				"CFGSSPath",
//				"AnaphorSFForm+AntecedentHdForm",
//				"AntecedentSPrForm",
//				"CFGDSPath",
//				"AnaphorSPrPos",
//				"Genre+AntecedentSFForm",
//				"AntecedentCFGSubCat",
//				"CFGSSPath+AnaphorPronounForm",
//				"AnaphorWholeSpanForm+AntecedentWholeSpanForm",
//				"DistanceBucketed+AnaphorHdForm",
//				"ExactStringMatch+DistanceBucketed",
//				"AntecedentCFGParentSubCat",
//				"Genre+AntecedentHdForm",
//				"Nested",
//				"CFGSSPath+Genre",
//				"AntecedentCFGSubCat+Nested",
//				"Genre+Nested+AnaphorHdFormSubStringMatch+AnaphorProperName+AntecedentProperName",
//				"AntecedentCFGParentSubCat+AnaphorSPrPos",
//				"AnaphorHdFormSubStringMatch+AnaphorProperName+AntecedentProperName+MentionDistBigBuckets+AntecedentPronoun",
//				"AntecedentCFGParentSubCat+AnaphorHdForm"
//		};
////		String[] names={
////				"AntecedentHdForm+AnaphorHdForm",
////				"AntecedentHdPos",
////				"ExactStringMatch",
////				"DistanceBucketed",
////				"DistanceBucketed+AnaphorPronoun",
////				"SameSpeaker+AntecedentPronounForm+AnaphorPronounForm",
////				"AntecedentWholeSpanForm",
////				"AntecedentSPrPos",
////				"AnaphorCFGSubCat+Nested",
////				"Genre+Nested",
////				"CFGSSPath",
////				"AnaphorSFForm+AntecedentHdForm",
////				"AntecedentSPrForm",
////				"CFGDSPath",
////				"AnaphorSPrPos",
////				"Genre+AntecedentSFForm",
////				"AntecedentCFGSubCat",
////				"CFGSSPath+AnaphorPronounForm",
////				"AnaphorWholeSpanForm+AntecedentWholeSpanForm",
////				"AntecedentNamedEntity",
////				"DistanceBucketed+AnaphorHdForm",
////				"ExactStringMatch+DistanceBucketed"};
//
//
//		return FeatureSet.getFromNameArray(names);
//	}

	@Override
	public boolean cleverStringMatch(Span ant,Span ana) {
		throw new Error("not implemented");
	}

	@Override
	public boolean isAlias(Span ant,Span ana) {
		throw new Error("not implemented");
	}

	@Override
	public void computeAtomicSpanFeatures(Span s) {
		s.isPronoun=(s.size()==1 && s.s.tags[s.start].equals("PN"));
		s.isProperName=isProperName(s);
		s.isDemonstrative=isDemonstrative(s);
	}

	
	private static final Set<String> DEMONSTRATIVES=new HashSet<String>();
	static {
		Collections.addAll(DEMONSTRATIVES,
			"这","这些",
			"该", //Not sure if this is the 'that' from the annotation guidelines
			"本", //I'm not entirely sure -- is this the same character that's used for 'our' in the pos annotation guidelines?
			"那","那些");
	}
	private boolean isDemonstrative(Span s) {
		return DEMONSTRATIVES.contains(s.s.forms[s.start]);
	}

	private boolean isProperName(Span s) {
		for(int i=s.start;i<=s.end;++i){
			if(!s.s.tags[i].equals("NR"))
				return false;
		}
		return true;
	}

	@Override
	public int findNonTerminalHead(Sentence s, CFGNode n) {
		if(n==null)
			return -1;
		if(n instanceof Terminal)
			return n.beg;
		NonTerminal nt=(NonTerminal) n;
		int h=headFinder.findHead(s, nt);
		if(h<1)
			return nt.end;
		else
			return h;
	}
	
	static final HeadFinder headFinder;
	
	static {
		Map<String,HeadRules> m=new HashMap<String,HeadRules>();
		String[] zParRules={
		"ADJP:r ADJP|JJ|AD;r .*",
		"ADVP:l CS; r ADVP|AD|JJ|NP|PP|P|VA|VV;r .*",
		"CLP:r CLP|M|NN|NP;r .*",
		"CP:r DEC|CP|ADVP|IP|VP;r .*",
		"DNP:r DEG|DNP|DEC|QP;r .*",
		"DP:r QP|M|CLP;l DP|DT|OD;l .*",
		"DVP:r DEV|AD|VP;r .*",
		"IP:r VP|IP|NP;r .*",
		"LCP:r LCP|LC;r .*",
		"LST:r CD|NP|QP;r .*",
		"NP:r NP|NN|IP|NR|NT;r .*",
		"NN:r NP|NN|IP|NR|NT;r .*",
		"PP:l P|PP;l .*",
		"PRN:l PU;l .*",
		"QP:r QP|CLP|CD|OD;r .*",
		"UCP:r IP|NP|VP;r .*",
		"VCD:r VV|VA|VE;r .*",
		"VP:l VE|VC|VV|VNV|VPT|VRD|VSB|VCD|VP|IP;l .*",
		"VPT:l VA|VV;l .*",
		"VRD:l VV|VA;l .*",
		"VSB:r VV|VE;r .*",
		"FRAG:r VP|VV|NP|NR|NN|NT;r .*",};
		for(String line:zParRules){
			String[] a=line.split(":");
			String[] b=a[1].split(";");
			Rule[] rules=new Rule[b.length];
			int i=0;
			for(String s:b){
				String[] c=s.split(" ");
				Direction d=(c[0].equals("r")?Direction.RightToleft:Direction.LeftToRight);
				rules[i++]=new Rule(d,Pattern.compile(c[1]));
			}
			m.put(a[0], new HeadRules(a[0],rules));
		}
		headFinder=new HeadFinder(m);
	}

	@Override
	public String getDefaultMarkableExtractors() {
		return "NT-NP,T-PN,T-NR";//,NonReferential";
	}

	@Override
	public String computeCleverString(Span span) {
		throw new Error("not implemented");
	}

	@Override
	public String getDefaultEdgeCreators() {
		return "LeftGraph";
	}

}
