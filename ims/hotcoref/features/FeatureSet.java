package ims.hotcoref.features;

import ims.hotcoref.Options;
import ims.hotcoref.data.Document;
import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTEdge;
import ims.hotcoref.decoder.HOTState;
import ims.hotcoref.features.enums.BigBuckets;
import ims.hotcoref.features.enums.Buckets;
import ims.hotcoref.features.enums.CFGTarget;
import ims.hotcoref.features.enums.MTypeCoarse;
import ims.hotcoref.features.enums.MTypeFine;
import ims.hotcoref.features.extractors.PairTargetNode;
import ims.hotcoref.features.extractors.PairTargetNodeExtractor;
import ims.hotcoref.features.extractors.SpanToken;
import ims.hotcoref.features.extractors.SpanTokenExtractor;
import ims.hotcoref.features.extractors.TokenTrait;
import ims.hotcoref.features.extractors.TokenTraitExtractor;
import ims.hotcoref.lang.Language;
import ims.hotcoref.mentiongraph.Edge;
import ims.hotcoref.perceptron.Long2IntInterface;
import ims.hotcoref.symbols.SymbolTable;
import ims.hotcoref.symbols.SymbolTable.Types;
import ims.util.SerializableObject;
import ims.util.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeatureSet implements Serializable {
	private static final long serialVersionUID = -8279475802157143606L;

	public static final String ZIP_ENTRY = "_fs";
	public static final String _FEATURES_ZIP_ENTRY = "FEATURES.txt";
	
	private final List<IFeature> features;
	private final List<IFeature> hoFeatures;
	public final int _bitsFeatureTypes;
	private final int hoOffset;
	public final boolean higherOrder;
	
	
	private FeatureSet(List<IFeature> f){
		this._bitsFeatureTypes=Util.getBits(f.size());
		this.features=new ArrayList<IFeature>();
		this.hoFeatures=new ArrayList<IFeature>();
		for(IFeature q:f)
			if(q.firstOrderFeature())
				features.add(q);
			else
				hoFeatures.add(q);
		hoOffset=features.size();
		higherOrder=hoFeatures.size()>0;
	}
	
	public SymbolTable createSymbolTable(){
		Set<Types> types=new HashSet<Types>();
		for(IFeature f:features)
			f.addSymbolTypes(types);
		for(IFeature f:hoFeatures)
			f.addSymbolTypes(types);
		SymbolTable st=new SymbolTable();
		st.initTable(types);
		return st;
	}
	
	public static FeatureSet getFeatureSet(Options options) throws IOException{
		if(options.featureFile==null)
			return Language.getLanguage().getDefaultFeatureSet();
		else
			return getFromFile(options.featureFile);
	}
	
	
	public static FeatureSet getFromFile(File featureSetFile) throws IOException {
		BufferedReader in=Util.getReader(featureSetFile);
		String line;
		List<String> names=new ArrayList<String>();
		while((line=in.readLine())!=null){
			if(line.startsWith("#") || line.length()==0)
				continue;
			if(line.contains(" ")){
				String[] a=line.split(" ");
				if(a[0].length()>0)
					names.add(a[0]);
			} else {
				names.add(line);
			}
		}
		return getFromNameList(names);
	}
	
	public static FeatureSet getFromNameList(List<String> names){
		return getFromNameArray(names.toArray(new String[names.size()]));
	}
	public static FeatureSet getFromNameArray(String... names){
		String duplicate=getFirstDuplicate(names);
		if(duplicate!=null)
			throw new RuntimeException("Duplicate feature: "+duplicate);
		List<IFeature> features=new ArrayList<IFeature>();
		for(String name:names){
			IFeature f=getFromName(name);
			features.add(f);
		}
		return new FeatureSet(features);
	}
	
	private static String getFirstDuplicate(String[] names){
		Set<String> canonicalNames=new HashSet<String>();
		for(String n:names){
			String cn;
			if(n.contains("+")){
				Matcher m=CUTOFF_PATTERN.matcher(n);
				if(m.matches())
					n=n.replace("-"+m.group(1), "");
				String[] a=n.split("\\+");
				Arrays.sort(a);
				StringBuilder sb=new StringBuilder(a[0]);
				for(int i=1;i<a.length;++i)
					sb.append("+").append(a[i]);
				cn=sb.toString();
			} else {
				cn=n;
			}
			if(canonicalNames.contains(cn))
				return cn;
			canonicalNames.add(cn);
		}
		return null;
	}

	
	private static final String SPAN="(MFrom|MTo|MGP|MSib|MLinPre|MLinFirst|MLinPre2|MLinPre3)";
	private static final String TOKEN="(Hd|HdGov|HdLmc|HdRmc|HdRs|HdLs|HdP|HdPP|HdN|HdNN|HdIP|HdIPP|HdIN|HdINN|SF|SL|SFo|SPr)";
	private static final String TRAIT="(Form|Pos|Fun|Lemma|Brown|BrownMid|BrownShort|FFChar|FLChar|FF2Char|FL2Char|BWUV)";
//	private static final String LEFTRIGHT="(Left|Right)";
	private static final String BUCKETS="(Buckets|BigBuckets)";
	private static final String CFG_TARGET="(Node|ParentNode|GrandParentNode)";
    private static final Pattern SPAN_TOKEN_TRAIT_PATTERN=Pattern.compile(SPAN+TOKEN+TRAIT);
	private static final Pattern CUTOFF_PATTERN=Pattern.compile("^.*-(\\d+)$");	
	private static final Pattern M_TYPE_PATTERN=Pattern.compile(SPAN+"(MTypeFine|MTypeCoarse)");
	private static final Pattern SENT_DIST_PATTERN=Pattern.compile(SPAN+SPAN+"Distance"+BUCKETS);
	private static final Pattern CFG_SUBCAT_PATTERN=Pattern.compile(SPAN+CFG_TARGET+"CFGSubCat");
	private static final Pattern SPAN_PRONOUN_FORM=Pattern.compile(SPAN+"PronounForm");
	private static final Pattern WHOLESPANTRAIT_PATTERN=Pattern.compile(SPAN+"WholeSpan"+TRAIT);
	private static final Pattern CFG_PATH_PATTERN=Pattern.compile(SPAN+SPAN+"CFG(SS|DS)Path");
	private static final Pattern CFG_TRAIT_PATH_PATTERN=Pattern.compile(SPAN+SPAN+"CFG(SS|DS)"+TRAIT+"Path");
	private static final Pattern CFG_NODE_CATEGORY_PATTERN=Pattern.compile(SPAN+"CFG"+CFG_TARGET+"Category");
	private static final Pattern GENDER_NE_QUOTED_DOMVERB_HDSUBSTRMATCH_ANAPHORICITY_NUMBER_PATTERN=Pattern.compile(SPAN+"(Gender|NamedEntity|Quoted|DominatingVerb|Anaphoricity|Number|CoordinationBOW)");
	private static final Pattern SPAN_HD_SUBSTRING_MATCH_PATTERN=Pattern.compile(SPAN+"HdForm"+SPAN+"SubStringMatch");
	private static final Pattern MENTION_DIST_PATTERN=Pattern.compile(SPAN+SPAN+"MentionDist"+BUCKETS);
	private static final Pattern EDIT_DISTANCE_PATTERN=Pattern.compile(SPAN+SPAN+"(WholeSpan(?:Token)?)"+TRAIT+"EditDistance"+BUCKETS);
	private static final Pattern EDIT_SCRIPT_PATTERN=Pattern.compile(SPAN+SPAN+"(WholeSpan(?:Token)?)"+TRAIT+"EditScript");
	private static final Pattern SPAN_BAG_OF_TRAIT_PATTERN=Pattern.compile(SPAN+"BagOf"+TRAIT);
	
	private static final Pattern CHAINCOUNT_DIFF=Pattern.compile(SPAN+SPAN+"ChainCountDiff"+BUCKETS);
	private static final Pattern CHAINCOUNT_BOD=Pattern.compile(SPAN+"ChainCountBOD"+BUCKETS);
	private static final Pattern DECAY_DENSITY=Pattern.compile("DecayDensity(\\d+)");
	private static final Pattern BOD_DISTANCE=Pattern.compile(SPAN+"(Mention|Sentence)DistBOD"+BUCKETS);

	private static final Pattern CFG_PATHTOROOT=Pattern.compile(SPAN+"CFGPathToRoot(:?Chopped)?(:?WD)?");
	
	private static final Pattern ALIAS_STRINGMATCH_SPEAKER_NESTED_SAMECHAIN_PATTERN=Pattern.compile(SPAN+SPAN+"(CleverStringMatch|ExactStringMatch|Alias|SameSpeaker|Nested|SameChain|SameMention)");
	
	private static final Pattern SPAN_NE_PATTERN=Pattern.compile(SPAN+"NE"+SPAN);
	private static final Pattern SPAN_SIZE_PATTERN=Pattern.compile(SPAN+"MentionSize"+BUCKETS);
	private static final Pattern CLUSTER_PATH_PATTERN=Pattern.compile("(?:Linear)?ClusterPath(\\d+)");
	
	private static IFeature getFromName(String fullName) {
		Matcher m0=CUTOFF_PATTERN.matcher(fullName);
		String name=fullName;
		int cutOff=0;
		if(m0.matches()){
			cutOff=Integer.parseInt(m0.group(1));
			name=fullName.replace("-"+m0.group(1), "");
		}

		//First check if this is some kind of N-gram feature (contains +)
		if(name.contains("+")){
			String[] nn=name.split("\\+",2);
			IFeature af1=getFromName(nn[0]);
			IFeature af2=getFromName(nn[1]);
			return BigramFactory.getBigram(af1, af2, cutOff);
		}

		if(name.equals("Genre"))
			return new F_Genre();
		
		if(name.startsWith("CBOF_")){
			String f=name.substring("CBOF_".length());
			IFeature iF=getFromName(f.replaceAll("_", "\\+"));
			if(iF instanceof AbstractMultiPairFeature)
				return new C_BOFM((ISingleMentionMultiFeature) iF);
			else if(iF instanceof AbstractSinglePairFeature)
				return new C_BOF((ISingleMentionFeature) iF);
			else
				throw new RuntimeException("!");
		}
		
		Matcher m=ALIAS_STRINGMATCH_SPEAKER_NESTED_SAMECHAIN_PATTERN.matcher(name);
		if(m.matches()){
			PairTargetNodeExtractor tse1=PairTargetNode.valueOf(m.group(1)).getExtractor();
			PairTargetNodeExtractor tse2=PairTargetNode.valueOf(m.group(2)).getExtractor();
			String t=m.group(3);
			if(t.equals("CleverStringMatch")){
				return new F_CleverStringMatch(tse1,tse2);
			} else if(t.equals("ExactStringMatch")){
				return new F_ExactStringMatch(tse1,tse2);
			} else if(t.equals("Alias")){
				return new F_Alias(tse1,tse2);
			} else if(t.equals("SameSpeaker")){
				return new F_SameSpeaker(tse1,tse2);
			} else if(t.equals("Nested")){
				return new F_Nested(tse1,tse2);
			} else if(t.equals("SameChain")){
				return new F_SameChain(tse1,tse2);
			} else if(t.equals("SameMention")){
				return new F_SameMention(tse1,tse2);
			}
		}
		
		Matcher m1=M_TYPE_PATTERN.matcher(name);
		if(m1.matches()){
			final PairTargetNodeExtractor tse=PairTargetNode.valueOf(m1.group(1)).getExtractor();
			String mType=m1.group(2);
			if(mType.equals("MTypeFine")){
				return new F_MType<MTypeFine>(name,MTypeFine.values(),tse);
			} else if(mType.equals("MTypeCoarse")){
				return new F_MType<MTypeCoarse>(name,MTypeCoarse.values(),tse);
			} else {
				throw new Error("!");
			}
		}
		
		Matcher m2=SENT_DIST_PATTERN.matcher(name);
		if(m2.matches()){
			PairTargetNodeExtractor tse1=PairTargetNode.valueOf(m2.group(1)).getExtractor();
			PairTargetNodeExtractor tse2=PairTargetNode.valueOf(m2.group(2)).getExtractor();
			String b=m2.group(3);
			if(b.equals("Buckets"))
				return new F_SentenceDistanceBucketed<Buckets>(Buckets.values(),tse1,tse2);
			else
				return new F_SentenceDistanceBucketed<BigBuckets>(BigBuckets.values(),tse1,tse2);
		}
		
		Matcher m3=CFG_SUBCAT_PATTERN.matcher(name);
		if(m3.matches()){
			final PairTargetNodeExtractor tse=PairTargetNode.valueOf(m3.group(1)).getExtractor();
			CFGTarget cfgTarget=CFGTarget.valueOf(m3.group(2));
			return new F_CFGSubCat(tse,cfgTarget);
		}
		
		Matcher m4=SPAN_PRONOUN_FORM.matcher(name);
		if(m4.matches()){
			final PairTargetNodeExtractor tse=PairTargetNode.valueOf(m4.group(1)).getExtractor();
			return new F_PronounForm(tse);
		}
		
		Matcher m5=WHOLESPANTRAIT_PATTERN.matcher(name);
		if(m5.matches()){
			final PairTargetNodeExtractor tse=PairTargetNode.valueOf(m5.group(1)).getExtractor();
			final TokenTraitExtractor tte=TokenTraitExtractor.getTTE(TokenTrait.valueOf(m5.group(2)));
			return new F_WholeSpanTrait(tse, tte);
		}
		
		Matcher m6=CFG_PATH_PATTERN.matcher(name);
		if(m6.matches()){
			PairTargetNodeExtractor tse1=PairTargetNode.valueOf(m6.group(1)).getExtractor();
			PairTargetNodeExtractor tse2=PairTargetNode.valueOf(m6.group(2)).getExtractor();
			if(m6.group(3).equals("SS"))
				return new F_SSCFGPath(tse1,tse2);
			else
				return new F_DSCFGPath(tse1,tse2);
		}
		
		Matcher m7=CFG_NODE_CATEGORY_PATTERN.matcher(name);
		if(m7.matches()){
			final PairTargetNodeExtractor tse=PairTargetNode.valueOf(m7.group(1)).getExtractor();
			CFGTarget cfgTarget=CFGTarget.valueOf(m7.group(2));
			return new F_CFGCat(tse,cfgTarget);
		}
		
		Matcher m8=CFG_TRAIT_PATH_PATTERN.matcher(name);
		if(m8.matches()){
			PairTargetNodeExtractor tse1=PairTargetNode.valueOf(m8.group(1)).getExtractor();
			PairTargetNodeExtractor tse2=PairTargetNode.valueOf(m8.group(2)).getExtractor();
			final TokenTraitExtractor tte=TokenTraitExtractor.getTTE(TokenTrait.valueOf(m8.group(4)));
			if(m8.group(3).equals("SS"))
				return new F_SSCFGTraitPath(tte,tse1,tse2);
			else 
				return new F_DSCFGTraitPath(tte,tse1,tse2);
		}
		
		Matcher m9=GENDER_NE_QUOTED_DOMVERB_HDSUBSTRMATCH_ANAPHORICITY_NUMBER_PATTERN.matcher(name);
		if(m9.matches()){
			final PairTargetNodeExtractor tse=PairTargetNode.valueOf(m9.group(1)).getExtractor();
			String t=m9.group(2);
			if(t.equals("Gender")){
				return new F_Gender(tse);
			} else if(t.equals("NamedEntity")){
				return new F_NamedEntity(tse);
			} else if(t.equals("Quoted")){
				return new F_Quoted(tse);
			} else if(t.equals("DominatingVerb")){
				return new F_DominatingVerb(tse);
			} else if(t.equals("Anaphoricity")){
				return new F_AnaphoricityTH(tse);
			} else if(t.equals("Number")){
				return new F_Number(tse);
			} else if(t.equals("CoordinationBOW")){
				return new F_CoordinationBOW(tse);
			}
			throw new Error("not implemented");
		}
		
		Matcher m10=SPAN_TOKEN_TRAIT_PATTERN.matcher(name);
		if(m10.matches()){
			final PairTargetNodeExtractor tse=PairTargetNode.valueOf(m10.group(1)).getExtractor();
			final SpanTokenExtractor ste=new SpanTokenExtractor(SpanToken.valueOf(m10.group(2)));
			final TokenTraitExtractor tte=TokenTraitExtractor.getTTE(TokenTrait.valueOf(m10.group(3)));
			return new F_PairNodeTokenTraitFeature(tse, ste, tte);
		}
		
		Matcher m11=MENTION_DIST_PATTERN.matcher(name);
		if(m11.matches()){
			PairTargetNodeExtractor tse1=PairTargetNode.valueOf(m11.group(1)).getExtractor();
			PairTargetNodeExtractor tse2=PairTargetNode.valueOf(m11.group(2)).getExtractor();
			String b=m11.group(3);
			if(b.equals("Buckets"))
				return new F_MentionDist<Buckets>(Buckets.values(),tse1,tse2);
			else
				return new F_MentionDist<BigBuckets>(BigBuckets.values(),tse1,tse2);
		}
		
		Matcher m12=EDIT_DISTANCE_PATTERN.matcher(name);
		if(m12.matches()){
			//WholeSpan(?:Token))
			PairTargetNodeExtractor tse1=PairTargetNode.valueOf(m12.group(1)).getExtractor();
			PairTargetNodeExtractor tse2=PairTargetNode.valueOf(m12.group(2)).getExtractor();
			String t=m12.group(3);
			String tr=m12.group(4);
			String bu=m12.group(5);
			final TokenTraitExtractor tte=TokenTraitExtractor.getTTE(TokenTrait.valueOf(tr));
			if(t.equals("WholeSpan")){
				if(bu.equals("Buckets"))
					return new F_WholeSpanTraitEditDistance<Buckets>(tte,Buckets.values(),tse1,tse2);
				else
					return new F_WholeSpanTraitEditDistance<BigBuckets>(tte,BigBuckets.values(),tse1,tse2);
			} else if(t.equals("WholeSpanToken")){
				if(bu.equals("Buckets"))
					return new F_WholeSpanTokenEditDistance<Buckets>(tte,Buckets.values(),tse1,tse2);
				else
					return new F_WholeSpanTokenEditDistance<BigBuckets>(tte,BigBuckets.values(),tse1,tse2);
			}
		}
		
		Matcher m13=EDIT_SCRIPT_PATTERN.matcher(name);
		if(m13.matches()){
			//WholeSpan(?:Token))
			PairTargetNodeExtractor tse1=PairTargetNode.valueOf(m13.group(1)).getExtractor();
			PairTargetNodeExtractor tse2=PairTargetNode.valueOf(m13.group(2)).getExtractor();
			String t=m13.group(3);
			String tr=m13.group(4);
			final TokenTraitExtractor tte=TokenTraitExtractor.getTTE(TokenTrait.valueOf(tr));
			if(t.equals("WholeSpan"))
				return new F_WholeSpanTraitEditScript(tte,tse1,tse2);
			else if(t.equals("WholeSpanToken"))
				return new F_WholeSpanTokenEditScript(tte,tse1,tse2);
		}
		
		Matcher m14=SPAN_BAG_OF_TRAIT_PATTERN.matcher(name);
		if(m14.matches()){
			String span=m14.group(1);
			String trait=m14.group(2);
			final PairTargetNodeExtractor tse=PairTargetNode.valueOf(span).getExtractor();
			final TokenTraitExtractor tte=TokenTraitExtractor.getTTE(TokenTrait.valueOf(trait));
			return new F_SpanBagOfTrait(tse,tte);
		}
		
		Matcher m15=SPAN_HD_SUBSTRING_MATCH_PATTERN.matcher(name);
		if(m15.matches()){
			PairTargetNodeExtractor tse1=PairTargetNode.valueOf(m15.group(1)).getExtractor();
			PairTargetNodeExtractor tse2=PairTargetNode.valueOf(m15.group(2)).getExtractor();
			return new F_HeadSubStringMatch(tse1,tse2);
		}
		
//		CHAINCOUNT_DIFF=Pattern.compile(SPAN+SPAN+"ChainCountDiff"+BUCKETS);
		Matcher m16=CHAINCOUNT_DIFF.matcher(name);
		if(m16.matches()){
			PairTargetNodeExtractor tse1=PairTargetNode.valueOf(m16.group(1)).getExtractor();
			PairTargetNodeExtractor tse2=PairTargetNode.valueOf(m16.group(2)).getExtractor();
			String buckets=m16.group(3);
			return new F_ChainCountDiff(buckets.equals("Buckets")?Buckets.values():BigBuckets.values(),tse1,tse2);
		}
		
		Matcher m17=CHAINCOUNT_BOD.matcher(name);
		if(m17.matches()){
			PairTargetNodeExtractor tse1=PairTargetNode.valueOf(m17.group(1)).getExtractor();
			String buckets=m17.group(2);
			return new F_ChainCountBOD(buckets.equals("Buckets")?Buckets.values():BigBuckets.values(),tse1);
		}
		
		Matcher m18=DECAY_DENSITY.matcher(name);
		if(m18.matches()){
			int buckets=Integer.parseInt(m18.group(1));
			return new F_DecayedDensity(buckets);
		}
		
		Matcher m19=BOD_DISTANCE.matcher(name);
		if(m19.matches()){
			PairTargetNodeExtractor tse=PairTargetNode.valueOf(m19.group(1)).getExtractor();
			String type=m19.group(2);
			String buckets=m19.group(3);
			if(buckets.equals("Buckets"))
				return F_DistFromBOD.getFeature(Buckets.values(), type, tse);
			else if(buckets.equals("BigBuckets"))
				return F_DistFromBOD.getFeature(BigBuckets.values(), type, tse);
		}
		
		Matcher m20=CFG_PATHTOROOT.matcher(name);
		if(m20.matches()){
			PairTargetNodeExtractor tse=PairTargetNode.valueOf(m20.group(1)).getExtractor();
			boolean wd=name.endsWith("WD");
			boolean chop=name.contains("Chopped");
			return new F_CFGPathToRoot(tse, wd,chop);
		}
		
		Matcher m21=SPAN_NE_PATTERN.matcher(name);
		if(m21.matches()){
			PairTargetNodeExtractor tse1=PairTargetNode.valueOf(m21.group(1)).getExtractor();
			PairTargetNodeExtractor tse2=PairTargetNode.valueOf(m21.group(2)).getExtractor();
			return new T_TargetNETarget(tse1,tse2);
		}
		
		Matcher m22=SPAN_SIZE_PATTERN.matcher(name);
		if(m22.matches()){
			PairTargetNodeExtractor tse1=PairTargetNode.valueOf(m22.group(1)).getExtractor();
			String b=m22.group(2);
			if(b.equals("Buckets"))
				return new F_MentionSize<Buckets>(tse1, Buckets.values());
			else if(b.equals("BigBuckets"))
				return new F_MentionSize<BigBuckets>(tse1,BigBuckets.values());
		}
		
		Matcher m23=CLUSTER_PATH_PATTERN.matcher(name);
		if(m23.matches()){
			boolean linear=name.startsWith("Linear");
			int length=Integer.parseInt(m23.group(1));
			return new F_ClusterPathToRoot(linear, length);
		}
		
		if(name.startsWith("ChainSize")){
			String b=name.substring("ChainSize".length());
			if(b.equals("Buckets"))
				return new F_ChainSize<Buckets>(Buckets.values());
			else if(b.equals("BigBuckets"))
				return new F_ChainSize<BigBuckets>(BigBuckets.values());
		}
		
		throw new RuntimeException("not implemented: "+fullName);
	}

//	@SuppressWarnings("unchecked")
//	private static <T extends Enum<T> & IBuckets<T>> T[] getBuckets(String group) {
//		if(group.equals("Buckets"))
//			return (T[]) Buckets.values();
//		else if(group.equals("BigBuckets"))
//			return (T[]) BigBuckets.values();
//
//		throw new Error("Unknown buckets: "+group);
//	}


	private final Object l2i_LOCK=new SerializableObject();


	public Edge synchronizedCreateScoredEdge(int headIdx,int depIdx,Long2IntInterface l2i,float[] parameters,SymbolTable symTab,Instance inst,long[] sink) {
		Edge e=new Edge(headIdx,depIdx);
		int next = fillSinkFO(headIdx,depIdx, symTab, inst, sink);
		double sum;
		int[] f=new int[next];
		synchronized(l2i_LOCK){
			sum = l2i(l2i, parameters, sink, f);
		}
		e.setF(f);
		e.setScore(sum);
		return e;
	}

	public Edge createScoredEdge(int headIdx,int depIdx,Long2IntInterface l2i,float[] parameters,SymbolTable symTab,Instance inst,long[] sink) {
		Edge e=new Edge(headIdx,depIdx);
		int next = fillSinkFO(headIdx,depIdx, symTab, inst, sink);
		int[] f=new int[next];
		double sum = l2i(l2i, parameters, sink, f);
		e.setF(f);
		e.setScore(sum);
		return e;
	}
	
	public HOTEdge synchronizedCreateHOTEdge(Long2IntInterface l2i,float[] parameters,SymbolTable symTab,Instance inst,long[] sink,HOTState hotState,Edge e) {
		int next=fillSinkHO(e.headIdx, e.depIdx, symTab, inst, sink, hotState);
		int[] f=new int[next];
		double sum;
		synchronized(l2i_LOCK){
			sum=l2i(l2i,parameters,sink,f);
		}
		HOTEdge h=new HOTEdge(e, f, sum); 
		return h;
	}
	
	public HOTEdge createHOTEdge(Long2IntInterface l2i,float[] parameters,SymbolTable symTab,Instance inst,long[] sink,HOTState hotState,Edge e) {
		int next=fillSinkHO(e.headIdx, e.depIdx, symTab, inst, sink, hotState);
		int[] f=new int[next];
		double sum=l2i(l2i,parameters,sink,f);
		HOTEdge h=new HOTEdge(e, f, sum); 
		return h;
	}
	
	private int fillSinkHO(int headIdx,int depIdx,SymbolTable symTab,Instance inst,long[] sink,HOTState hotState){
		int next=0;
		int ft=hoOffset;
		int[] tnes=PairTargetNodeExtractor.getAllIdx(headIdx, depIdx, hotState);
		for(IFeature f:hoFeatures)
			next=f.fillLongs(sink, next, ft++, symTab, _bitsFeatureTypes, inst, tnes, hotState);
		return next;
	}
	
	private int fillSinkFO(int headIdx,int depIdx,SymbolTable symTab,Instance inst,long[] sink) {
		int next=0;
		int i=0;
		int[] tnes=PairTargetNodeExtractor.getFOIdx(headIdx, depIdx);
		for(IFeature f:features)
			next=f.fillLongs(sink,next,i++,symTab,_bitsFeatureTypes,inst,tnes,null);
		if(i!=hoOffset)
			throw new Error("!"); //XXX this can go.
		return next;
	}
	private double l2i(Long2IntInterface l2i, float[] parameters, long[] sink,int[] f) {
		double sum=0.f;
		for(int i=0,m=f.length,q;i<m;++i){
			if((q=l2i.l2i(sink[i]))<0)
				continue;
			f[i]=q;		
			sum+=parameters[f[i]];
		}
		return sum;
	}

	public void fillInstance(Instance inst, Document d,	SymbolTable symTab) {
		if(symTab.genre!=null)
			inst.genre=symTab.genre.lookup(d.genre);
		List<Callable<Void>> l=new ArrayList<Callable<Void>>();
		List<Future<Void>> fjs=new ArrayList<Future<Void>>();
		for(IFeature f:features)
			f.XfillFillInstanceJobs(inst, symTab, l, fjs);
		for(IFeature f:hoFeatures)
			f.XfillFillInstanceJobs(inst, symTab, l, fjs);
		try {
			for(Callable<Void> c:l)
				c.call();
			for(Future<Void> f:fjs)
				f.get();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		inst.filled=true;
	}
	
	public String toString(SymbolTable symTab){
		StringBuilder sb=new StringBuilder("Feature set: "+features.size()+" + "+hoFeatures.size()+"  (fo + ho) templates. ("+_bitsFeatureTypes+" bits)\n");
		sb.append("First order features:\n");
		for(IFeature f:features){
			int bitsF=f.getBits(symTab);
			int bitsTotal=bitsF+_bitsFeatureTypes;
			sb.append(String.format("%-50s | bits: %2d (+%2d = %2d)\n",f.getName(),bitsF,_bitsFeatureTypes,bitsTotal));
			if(bitsTotal>62)
				throw new Error("to many bits needed to encode feature");
		}
		sb.append("\nHigher order features:\n");
		for(IFeature f:hoFeatures){
			int bitsF=f.getBits(symTab);
			int bitsTotal=bitsF+_bitsFeatureTypes;
			sb.append(String.format("%-50s | bits: %2d (+%2d = %2d)\n",f.getName(),bitsF,_bitsFeatureTypes,bitsTotal));
			if(bitsTotal>62)
				throw new Error("to many bits needed to encode feature");
		}
		return sb.toString();
	}

	public String[] getFeatureNamesForTXT(SymbolTable symTab) {
		String[] s=new String[features.size()+hoFeatures.size()];
		int a=0;
		int maxLen=0;
		for(IFeature f:features){
			s[a]=f.getName();
			maxLen=Math.max(maxLen, s[a].length());
			++a;
		}
		for(IFeature f:hoFeatures){
			s[a]=f.getName();
			maxLen=Math.max(maxLen, s[a].length());
			++a;
		}
		a=0;
		String fr="%-"+(maxLen+5)+"s %d";
		for(IFeature f:features){
			s[a]=String.format(fr, s[a],f.getBits(symTab));
			++a;
		}
		for(IFeature f:hoFeatures){
			s[a]=String.format(fr, s[a],f.getBits(symTab));
			++a;
		}
		return s;
	}
	
	static String getCanonicalName(String... names){
		if(names.length==0)
			return null;
		if(names.length==1)
			return names[0];
		Arrays.sort(names);
		StringBuilder sb=new StringBuilder(names[0]);
		for(int i=1;i<names.length;++i)
			sb.append('+').append(names[i]);
		return sb.toString();
	}
	
	public int getFeatureIdxByName(String name){
		int idx=0;
		for(IFeature f:features){
			if(f.getName().equals(name))
				return idx;
			++idx;
		}
		return -1;
	}
	public IFeature getFeatureByIdx(int i){
		return features.get(i);
	}
	public int getHOFeatureIdxByName(String name){
		int idx=0;
		for(IFeature f:hoFeatures){
			if(f.getName().equals(name))
				return idx;
			++idx;
		}
		return -1;
	}
	public IFeature getHOFeatureByIdx(int i){
		return hoFeatures.get(i);
	}
	public int getNrFeatures() {
		return features.size();
	}
	public int getNrHOFeatures() {
		return hoFeatures.size();
	}
}
