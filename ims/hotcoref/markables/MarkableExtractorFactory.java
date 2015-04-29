package ims.hotcoref.markables;

import ims.hotcoref.Options;
import ims.hotcoref.lang.Language;
import ims.util.ArrayFunctions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkableExtractorFactory {

	
	public static ChainedExtractor getChainedExtractor(String[] n){
		if(ArrayFunctions.sameTwice(n))
			throw new RuntimeException("Same markable extractor twice!");
		IMarkableExtractor[] ex=new IMarkableExtractor[n.length];
		for(int i=0;i<n.length;++i)
			ex[i]=getSingleExtractor(n[i]);
		return new ChainedExtractor(ex);
	}
	
	public static IMarkableExtractor getExtractorS(String csvNames){
		if(csvNames.contains(",")){
			String[] namess=csvNames.split(",");
			return getChainedExtractor(namess);
		} else {
			return getSingleExtractor(csvNames);
		}
	}

	
	//NT-NP, NT-NML, -- non terminals with that label
	//T-PRP, T-PRP$, -- terminals with that tag
	//ST-NN, ST-NNP, -- subtrees headed by that tag
	//NER-LABEL, -- NER's with that label
	//NER-ALL, -- All NER's
	
	static final Pattern NT_PATTERN=Pattern.compile("^NT-([a-zA-Z]+)$");
	static final Pattern T_PATTERN=Pattern.compile("^T-([a-zA-Z\\$]+)$");
	static final Pattern ST_PATTERN=Pattern.compile("^ST-([a-zA-Z\\$]+)$");
	static final Pattern NER_PATTERN=Pattern.compile("^NER-([a-zA-Z]+)$");
	private static IMarkableExtractor getSingleExtractor(String name) {
		Matcher m=NT_PATTERN.matcher(name);
		if(m.matches())
			return new NonTerminalExtractor(m.group(1));
		Matcher m2=T_PATTERN.matcher(name);
		if(m2.matches())
			return new TerminalExtractor(m2.group(1));
		Matcher m3=ST_PATTERN.matcher(name);
		if(m3.matches())
			return new SubTreeExtractor(m3.group(1));
		Matcher m4=NER_PATTERN.matcher(name);
		if(m4.matches()){
			if(m4.group(1).equalsIgnoreCase("all"))
				return new AbstractNERExtractor.AllNERExtractor();
			else
				return new AbstractNERExtractor.OneNERExtractor(m.group(1));
		}
		if(name.equalsIgnoreCase("LeftConjuncts"))
			return new LeftDepConjunctExtractor();
		if(name.equals("NonReferential"))
			return new NonReferentialPruner();
		if(name.equalsIgnoreCase("StandAloneDemonstrative"))
			return new StandAloneDemonstrativeDepExtractor();
		if(name.equalsIgnoreCase("SameHeadPruner"))
			return new SameHeadPruner();
		throw new RuntimeException("Unknown markable extractor: "+name);
	}

	public static IMarkableExtractor getGoldExtractor(IMarkableExtractor me,boolean keepNonRefPruner) {
		if(!keepNonRefPruner)
			return new GoldStandardMarkableExtractor();
		if(me instanceof ChainedExtractor){
			ChainedExtractor ce=(ChainedExtractor) me;
			NonReferentialPruner nrp=null;
			for(IMarkableExtractor ime:ce.extractors)
				if(ime instanceof NonReferentialPruner){
					nrp=(NonReferentialPruner) ime;
					break;
				}
			if(nrp==null)
				return new GoldStandardMarkableExtractor();
			else {
				IMarkableExtractor[] q=new IMarkableExtractor[2];
				q[0]=new GoldStandardMarkableExtractor();
				q[1]=nrp;
				return new ChainedExtractor(q);
			}
		} else {
			return new GoldStandardMarkableExtractor();
		}
	}

	public static IMarkableExtractor getExtractorS(Options options) {
		if(options.markableExtractors==null)
			return getExtractorS(Language.getLanguage().getDefaultMarkableExtractors());
		else
			return getExtractorS(options.markableExtractors);
	}
	
}
