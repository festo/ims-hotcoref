package ims.hotcoref.lang;

import ims.hotcoref.Options;
import ims.hotcoref.data.CFGTree.CFGNode;
import ims.hotcoref.data.CFGTree.NonTerminal;
import ims.hotcoref.data.CFGTree.Terminal;
import ims.hotcoref.data.Sentence;
import ims.hotcoref.data.Span;
import ims.hotcoref.features.FeatureSet;
import ims.hotcoref.features.enums.Gender;
import ims.hotcoref.features.enums.Num;
import ims.hotcoref.features.enums.SemanticClass;
import ims.hotcoref.headrules.HeadFinder;
import ims.hotcoref.headrules.HeadRules;
import ims.hotcoref.headrules.HeadRules.Direction;
import ims.hotcoref.headrules.HeadRules.Rule;
import ims.hotcoref.util.BergsmaLinLookup;
import ims.hotcoref.util.WordNetInterface;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class Hungraian extends Language{
	private static final long serialVersionUID = 1L;

	private final BergsmaLinLookup lookup;

	public Hungraian(){
		try {
			lookup=new BergsmaLinLookup(Options.genderData);
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("!");
		}
	}

	@Override
	public FeatureSet getDefaultFeatureSet() {
		String[] names={
				"MToHdForm+MFromHdForm",                          	//1
//				"AnaphorDemonstrative" //2
				"MToSFForm",										//2'
				"CleverStringMatch",								//3
				"DistanceBuckets+MToMTypeCoarse",					//5
				"SameSpeaker+MFromPronounForm+MToPronounForm",		//6
				"MFromWholeSpanForm",								//7
				"CFGSSPath+MToPronounForm",							//8
				"MFromCFGParentNodeCategory",						//9
				"MFromNodeCFGSubCat+Nested",						//10
				"Genre+Nested",										//11
				"MFromSPrForm",										//12
				
				
//				"CFGDSFormPath",									//13
				
				
				"MFromWholeSpanForm+MToWholeSpanForm",				//14
				"MFromSFoPos",										//15
				"MToSFoPos+MFromSFForm",							//16
				"MFromHdPos+MToPronounForm",						//17
				"MToSFForm+MFromHdForm",							//18
				"MFromParentNodeCFGSubCat",							//19
				"MFromSPrForm+MToHdForm",							//20
				"MFromParentNodeCFGSubCat+MToPronounForm",			//21
				"CleverStringMatch+MFromMTypeCoarse",				//22
				"Nested+MToMTypeCoarse",							//23
				"DistanceBuckets+MToPronounForm",					//24
				"CFGSSPosPath",										//25
				
//				"Alias",                                            //4
//				"Alias+MFromMTypeCoarse+MToMTypeCoarse",			//4'

				"Genre+MFromPronounForm+MToPronounForm",			//26
				"MFromHdINForm+MFromHdPos",							//27
				"MFromSFoPos+MToPronounForm",						//28
				"MFromGender+MToPronounForm",						//29
				"MentionDistBuckets+MToMTypeCoarse",				//30
				"MFromParentNodeCFGSubCat+MentionDistBuckets+MToMTypeCoarse", // 31
				"CleverStringMatch+MFromMTypeCoarse+MFromHdForm+MToHdForm",   // 32
//				"MFromNamedEntity",											  // 33 // seems to work negatively for the treebased system
				"MToQuoted+MToPronounForm+MFromDominatingVerb",               // 34

		};
		return FeatureSet.getFromNameArray(names);
	}

	@Override
	public boolean cleverStringMatch(Span ant,Span ana) {
		String s1=ana.getCleverString();
		String s2=ant.getCleverString();
		return s1.length()>0 && s1.equals(s2);
	}

	@Override
	public String computeCleverString(Span sp) {
		StringBuilder sb=new StringBuilder();
		for(int i=sp.start;i<=sp.end;++i){
			if(sp.s.forms[i].equals("\"") ||sp.s.forms[i].equals(":") ||sp.s.forms[i].equals(".")||sp.s.forms[i].equals(","))
				continue;
			sb.append(sp.s.forms[i]).append(" ");
		}
		return sb.toString().trim();
	}

	@Override
	public boolean isAlias(Span ant,Span ana) {
		return AliasStuff.isAlias(ant, ana);
	}

	@Override
	public void computeAtomicSpanFeatures(Span s) {
		s.isProperName=isProperName(s);
		s.isPronoun=isPronoun(s);
		s.isDefinite=isDefinite(s);
		s.isDemonstrative=isDemonstrative(s);
		s.gender=lookupGender(s);
		s.number=lookupNumber(s);
		s.isQuoted=isQuoted(s);
		if(s.isPronoun){
			s.semanticClass=pronounSemanticClassLookup(s.s.forms[s.hd]);
		} else {
			WordNetInterface wni=WordNetInterface.theInstance();
			if(wni!=null)
				s.semanticClass=wni.lookupSemanticClass(s.s.forms[s.hd]);
		}
	}

	private boolean isQuoted(Span s) {
		boolean quoteBegin=false;
		boolean quoteEnd=false;
		for(int i=s.start-1;i>0;--i){
			if(s.s.forms[i].equals("\"")){
				quoteBegin=true;
				break;
			}
		}
		if(!quoteBegin)
			return false;
		for(int i=s.end+1;i<s.s.forms.length;++i){
			if(s.s.forms[i].equals("\"")){
				quoteEnd=true;
				break;
			}
		}
		return quoteBegin && quoteEnd;
	}

	private SemanticClass pronounSemanticClassLookup(String lcSurfaceForm) {
		if(FEMALE_PRONOUNS_SET.contains(lcSurfaceForm))
			return SemanticClass.Female;
		if(MALE_PRONOUNS_SET.contains(lcSurfaceForm))
			return SemanticClass.Male;
		if (SINGULAR_PRONOUNS_SET.contains(lcSurfaceForm) && !lcSurfaceForm.startsWith("it"))
			return SemanticClass.Person;
		else
			return SemanticClass.Unknown;
	}

	private Num lookupNumber(Span s) {
		if(s.isPronoun){
			String formLc=s.s.forms[s.start].toLowerCase();
			if(SINGULAR_PERSONAL_PRONOUNS_SET.contains(formLc))
				return Num.Sin;
			if(PLURAL_PERSONAL_PRONOUNS_SET.contains(formLc))
				return Num.Plu;
		}
		return lookup.lookupNum(s);
	}

	private static final Pattern MASC_TITLE_PATTERN=Pattern.compile("^(?:Mr\\.?|Mister)$");
	private static final Pattern FEM_TITLE_PATTERN=Pattern.compile("^M(?:r?s\\.?|iss)$");
	private Gender lookupGender(Span s) {
		if(s.isPronoun){
			String formLc=s.s.forms[s.start].toLowerCase();
			if(MALE_PRONOUNS_SET.contains(formLc))
				return Gender.Masc;
			if(FEMALE_PRONOUNS_SET.contains(formLc))
				return Gender.Fem;
			if(NEUT_PRONOUNS_SET.contains(formLc))
				return Gender.Neut;
			return Gender.Unknown;
		} else {
			if(s.isProperName){ //Might be a title of a person
				if(MASC_TITLE_PATTERN.matcher(s.s.forms[s.start]).matches())
					return Gender.Masc;
				if(FEM_TITLE_PATTERN.matcher(s.s.forms[s.start]).matches())
					return Gender.Fem;
			}
			//Otherwise we try the gender lookup
			return lookup.lookupGen(s);
		}
	}

	private boolean isDemonstrative(Span s) {
		int len=s.end-s.start+1;
		if(len==1)
			return false;
		String formLc=s.s.forms[s.start].toLowerCase();
		return DEMONSTRATIVE_PRONOUNS_SET.contains(formLc);
	}

	private static final Pattern DEFINITE_PATTERN=Pattern.compile("^a(?:z)$",Pattern.CASE_INSENSITIVE);
	private boolean isDefinite(Span s) {
		int len=s.end-s.start+1;
		if(len==1)
			return false;
		return DEFINITE_PATTERN.matcher(s.s.forms[s.start]).matches();
	}

	private boolean isProperName(Span s) {
		int len=s.end-s.start+1;
		if(len>1) {
			for(int i=s.start;i<s.end;++i){
				if(!s.s.tags[i].startsWith("N##SubPOS=p"))
					return false;
			}
			return true;
		} else {
			return s.s.tags[s.start].startsWith("N##SubPOS=p");
		}
	}

	private boolean isPronoun(Span s) {
		return s.s.tags[s.hd].startsWith("PRP") || ALL_PRONOUNS.contains(s.s.forms[s.hd].toLowerCase());
//		int len=s.end-s.start+1;
//		if(len==1){
//			if(ALL_PRONOUNS.contains(s.s.forms[s.start]))
//				return true;
//			return s.s.forms[s.start].equalsIgnoreCase("one") && s.s.tags[s.start].equals("PRP");
//		} else if(len==2){
//			return s.s.forms[s.start].equalsIgnoreCase("one") && s.s.forms[s.start+1].equals("'s");
//		} else {
//			return false;
//		}
	}

	// Szemelyes nevmasok
	private static final String[] SINGULAR_PERSONAL_PRONOUNS=new String[]{
			"én","te","ő",
			"engem", "téged", "őt",
			"nekem", "neked", "neki",
			"velem", "veled", "vele",
			"értem", "érted", "érte",
			"bennem", "benned", "benne",
			"belém", "beléd", "belé",
			"belőlem", "belőled", "belőle",
			"nálam", "nálad", "nála",
			"hozzám", "hozzád", "hozzá",
			"tőlem", "tőled", "tőle",
			"rajtam", "rajtad", "rajta",
			"rám", "rád", "rá",
			"rólam", "rólad", "róla"
	};

	private static final String[] PLURAL_PERSONAL_PRONOUNS=new String[]{
			"mi","ti","ők",
			"minket", "titeket", "őket",
			"nekünk", "nektek", "nekik",
			"velünk", "veletek", "velük",
			"értünk", "értetek", "értük",
			"bennünk", "bennetek", "bennük",
			"belénk", "belétek", "beléjük",
			"belőlünk", "belőletek", "belőlük",
			"nálunk", "nálatok", "náluk",
			"hozzánk", "hozzátok", "hozzájuk",
			"tőlünk", "tőletek", "tőlük",
			"rajtunk", "rajtatok", "rajtuk",
			"ránk", "rátok", "rájuk",
			"rólunk", "rólatok", "róluk"
	};


	private static final String[] DEMONSTRATIVE_PRONOUNS=new String[]{
			"ez", "emez", "ugyanez", "az", "amaz", "ugyanaz",
			"ilyen", "efféle", "ekkora", "ugyanilyen", "ebbéli", "olyan", "afféle", "akkore", "ugyanolyan", "abbéli", "amekkora",
			"ennyi", "ugyanennyi", "emennyi", "annyi", "ugyanannyi", "amannyi"
	};

	private static final Set<String> SINGULAR_PERSONAL_PRONOUNS_SET=new HashSet<String>();
	private static final Set<String> PLURAL_PERSONAL_PRONOUNS_SET=new HashSet<String>();
	private static final Set<String> DEMONSTRATIVE_PRONOUNS_SET=new HashSet<String>();


	//Is yourself always singular?
	private static final String[] SINGULAR_PRONOUNS=new String[]{"i","he","she","it","me","my", "myself", "mine","him","his","himself","her","hers","herself","its","itself"};
	private static final String[] PLURAL_PRONOUNS=new String[]{"we","our","ours","ourself","ourselves","yourselves","they","them","their","theirs","us", "themselves"};
	private static final String[] MALE_PRONOUNS=new String[]{"he","him","his","himself"};
	private static final String[] FEMALE_PRONOUNS=new String[]{"she","her","hers","herself"};
	private static final String[] NEUT_PRONOUNS=new String[]{"it","its"};
	
	private static final Set<String> SINGULAR_PRONOUNS_SET=new HashSet<String>();
	private static final Set<String> PLURAL_PRONOUNS_SET=new HashSet<String>();
	private static final Set<String> MALE_PRONOUNS_SET=new HashSet<String>();
	private static final Set<String> FEMALE_PRONOUNS_SET=new HashSet<String>();
	private static final Set<String> NEUT_PRONOUNS_SET=new HashSet<String>();
	
	public static final Set<String> ALL_PRONOUNS=new HashSet<String>();
	static {
		Collections.addAll(SINGULAR_PRONOUNS_SET,SINGULAR_PRONOUNS);
		Collections.addAll(PLURAL_PRONOUNS_SET,PLURAL_PRONOUNS);
		Collections.addAll(FEMALE_PRONOUNS_SET,FEMALE_PRONOUNS);
		Collections.addAll(MALE_PRONOUNS_SET,MALE_PRONOUNS);
		Collections.addAll(NEUT_PRONOUNS_SET,NEUT_PRONOUNS);
		
		//All below
		Collections.addAll(ALL_PRONOUNS,SINGULAR_PRONOUNS);
		Collections.addAll(ALL_PRONOUNS,PLURAL_PRONOUNS);
		Collections.addAll(ALL_PRONOUNS,MALE_PRONOUNS);
		Collections.addAll(ALL_PRONOUNS,FEMALE_PRONOUNS);
		String[] additionalPronouns=new String[]{"you", "your", "yourself","yours"};
		Collections.addAll(ALL_PRONOUNS,additionalPronouns);

		// New collections:
		Collections.addAll(SINGULAR_PERSONAL_PRONOUNS_SET,SINGULAR_PERSONAL_PRONOUNS);
		Collections.addAll(PLURAL_PERSONAL_PRONOUNS_SET,PLURAL_PERSONAL_PRONOUNS);
		Collections.addAll(DEMONSTRATIVE_PRONOUNS_SET,DEMONSTRATIVE_PRONOUNS);

	}
	static class AliasStuff{
		
		public static boolean isAlias(Span ant,Span ana){
			String antSFWTP=toSurfaceFormWithoutTrailingPossesives(ant);
			String anaSFWTP=toSurfaceFormWithoutTrailingPossesives(ana);
			if(ant.ne==null || ana.ne==null || !ant.ne.getLabel().equals(ana.ne.getLabel()))
				return false;
			String neLbl=ant.ne.getLabel();	
			if(neLbl.equals("PERSON")){
				return comparePerson(antSFWTP.split(" "),anaSFWTP.split(" "));
			} else if(neLbl.equals("ORG")){
				return compareOrg(ant,ana);
			} else {
				return false;
			}
//				return toSurfaceFormWithoutTrailingPossesives(ant).equalsIgnoreCase(toSurfaceFormWithoutTrailingPossesives(ana));
//			}
		}
		
		private static boolean comparePerson(String[] ant, String[] ana) {
			return ant[ant.length-1].equals(ana[ana.length-1]);
		}

		private static boolean compareOrg(Span ant, Span ana) {
			String antStr=toSurfaceFormWithoutTrailingPossesives(ant);
			String anaStr=toSurfaceFormWithoutTrailingPossesives(ana);
			return compareOrg(antStr,anaStr);
		}
		
		private static boolean compareOrg(String antStr, String anaStr) {
//			String antStr=toSurfaceFormWithoutTrailingPossesives(ant);
//			String anaStr=toSurfaceFormWithoutTrailingPossesives(ana);
			if(antStr.replaceAll("\\.", "").equals(anaStr) ||
				anaStr.replaceAll("\\.", "").equals(antStr)){
				return true;
			} else {
				if(antStr.length()>anaStr.length()){
					String[] acr=getAcronyms(antStr);
					String s=loseInitialThe(anaStr);
					return matchesAny(s,acr);
				} else {
					String[] acr=getAcronyms(anaStr);
					String s=loseInitialThe(antStr);
					return matchesAny(s,acr);
				}
			}
		}
		

		public static String toSurfaceFormWithoutTrailingPossesives(Span s){
			StringBuilder sb=new StringBuilder();
			for(int i=s.start;i<=s.end;++i){
				if(s.s.tags[i].equals("POS"))
					continue;
				sb.append(s.s.forms[i]).append(" ");
			}
			return sb.toString().trim();
		}
		private static boolean matchesAny(String s,	String[] acronyms) {
			for(String acro:acronyms){
				if(s.equals(acro))
					return true;
			}
			return false;
		}
		
		private static String loseInitialThe(String s){
			String ret=s.replaceFirst("^[Tt]he ","");
			return ret;
		}

		private static String[] getAcronyms(String anaphorSurfaceForm) {
			String[] tokens=anaphorSurfaceForm.split(" ");
			StringBuilder a1=new StringBuilder();
			StringBuilder a2=new StringBuilder();
			StringBuilder a3=new StringBuilder();
			for(int i=0;i<tokens.length;++i){
				if(!tokens[i].toLowerCase().matches("(assoc|bros|co|coop|corp|devel|inc|llc|ltd)\\.?")){
					a1.append(tokens[i]);
					if(Character.isUpperCase(tokens[i].charAt(0))){
						a2.append(tokens[i].charAt(0));
						a3.append(tokens[i].charAt(0)).append(".");
					}
				}
			}
			return new String[]{ a1.toString(), a2.toString(), a3.toString()};
		}
	}

	@Override
	public int findNonTerminalHead(Sentence s,CFGNode n){
		return findEnglishCFGHead(s,n);
	}
	
	public static int findEnglishCFGHead(Sentence s,CFGNode n){
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
		String[] clearRules=new String[]{
				"ADJP	r	A|C|I|K|M|N|O|P|R|S|V|X|Y|Z;ADJP;ADVP|INFU|INFZ|NEG|NP|PAU|PAZ|PP|PREVERB|VU|VZ|XP;PUNC|T;.*",
				"ADVP	r	A|C|I|K|M|N|O|P|R|S|V|X|Y|Z;ADVP;ADJP|INFU|INFZ|NEG|NP|PAU|PAZ|PP|PREVERB|VU|VZ|XP;PUNC|T;.*",
				"CZ	r	A|C|I|K|M|N|O|P|R|S|V|X|Y|Z;CZ;ADJP|ADVP|INFU|INFZ|NEG|NP|PAU|PAZ|PP|PREVERB|VU|VZ|XP;PUNC|T;.*",
				"INFU	r	A|C|I|K|M|N|O|P|R|S|V|X|Y|Z;INFU;ADJP|ADVP|INFZ|NEG|NP|PAU|PAZ|PP|PREVERB|VU|VZ|XP;PUNC|T;.*",
				"INFZ	r	A|C|I|K|M|N|O|P|R|S|V|X|Y|Z;INFZ;ADJP|ADVP|INFU|NEG|NP|PAU|PAZ|PP|PREVERB|VU|VZ|XP;PUNC|T;.*",
				"NEG	r	A|C|I|K|M|N|O|P|R|S|V|X|Y|Z;NEG;ADJP|ADVP|INFU|INFZ|NP|PAU|PAZ|PP|PREVERB|VU|VZ|XP;PUNC|T;.*",
				"NP	r	A|C|I|K|M|N|O|P|R|S|V|X|Y|Z;NP;ADJP|ADVP|INFU|INFZ|NEG|PAU|PAZ|PP|PREVERB|VU|VZ|XP;PUNC|T;.*",
				"PAU	r	A|C|I|K|M|N|O|P|R|S|V|X|Y|Z;PAU;ADJP|ADVP|INFU|INFZ|NEG|NP|PAZ|PP|PREVERB|VU|VZ|XP;PUNC|T;.*",
				"PAZ	r	A|C|I|K|M|N|O|P|R|S|V|X|Y|Z;PAZ;ADJP|ADVP|INFU|INFZ|NEG|NP|PAU|PP|PREVERB|VU|VZ|XP;PUNC|T;.*",
				"PP	r	A|C|I|K|M|N|O|P|R|S|V|X|Y|Z;PP;ADJP|ADVP|INFU|INFZ|NEG|NP|PAU|PAZ|PREVERB|VU|VZ|XP;PUNC|T;.*",
				"PREVERB	r	A|C|I|K|M|N|O|P|R|S|V|X|Y|Z;PREVERB;ADJP|ADVP|INFU|INFZ|NEG|NP|PAU|PAZ|PP|ROOT|VU|VZ|XP;PUNC|T;.*",
				"CP	l	VU;INFU;PAU;NP|PP;ADJP|ADVP;CP;A|C|I|K|M|N|O|P|R|S|V|X|Y|Z;.*",
				"XP	l	VU;INFU;PAU;NP|PP;ADJP|ADVP;XP;CP;A|C|I|K|M|N|O|P|R|S|V|X|Y|Z;.*",
				"ROOT	l	CP;.*",
				"VU	l	VZ;.*",
				"VZ	l	V;.*"
		};
		for(String line:clearRules){
			String[] a=line.split("\\t");
			String lbl=a[0];
			Direction d=(a[1].equals("r")?Direction.RightToleft:Direction.LeftToRight);
			String[] r=a[2].split(";");
			Rule[] rules=new Rule[r.length];
			int i=0;
			for(String s:r)
				rules[i++]=new Rule(d,Pattern.compile(s));
			m.put(lbl, new HeadRules(lbl,rules));
		}
		headFinder=new HeadFinder(m);
	}
	@Override
	public String getDefaultMarkableExtractors() {
		return "NT-NP,T-PRP,T-PRP$,NER-ALL";
	}

	public void preprocessSentence(Sentence s){
		if(s.forms[1].equals("Mm"))
			s.tags[1]="UH";
	}
	static final Set<String> nonReferentials=new HashSet<String>(Arrays.asList("you","it","we"));
	public Set<String> getNonReferentialTokenSet() {
		return nonReferentials;
	}

	@Override
	public String getDefaultEdgeCreators() {
		return "LeftGraph";
	}
	
	private static final Pattern COORD_TOKEN_PATTERN=Pattern.compile("(and|but|or|,)",Pattern.CASE_INSENSITIVE);
	public boolean isCoordToken(String string) {
		return COORD_TOKEN_PATTERN.matcher(string).matches();
	}
}
