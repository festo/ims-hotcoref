package ims.hotcoref.util;

import ims.hotcoref.Options;
import ims.hotcoref.features.enums.SemanticClass;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import edu.smu.tspell.wordnet.NounSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;

public class WordNetInterface {
	
	private static WordNetInterface _instance;
	private static boolean triedYet=false;
	
	public static synchronized WordNetInterface theInstance(){
		if(triedYet)
			return _instance;
		triedYet=true;
		if(Options.wordNetDictDir==null){
			_instance=null;
			System.out.println("didnt initialize wordnet -- no path given");
		} else
			_instance=new WordNetInterface(Options.wordNetDictDir);
		return _instance;
	}
	
	private WordNetInterface(File wordNetDatabaseDir){
		System.out.println("Initalizing wordnet from dir "+wordNetDatabaseDir);
		System.setProperty("wordnet.database.dir", wordNetDatabaseDir.toString());
		wnd=WordNetDatabase.getFileInstance();
		
		NounSynset maleSynset=(NounSynset) wnd.getSynsets("male")[1];
		NounSynset femaleSynset=(NounSynset) wnd.getSynsets("female")[1];
		NounSynset personSynset=(NounSynset) wnd.getSynsets("person")[0];
		NounSynset locationSynset=(NounSynset) wnd.getSynsets("location")[0];
		NounSynset organizationSynset=(NounSynset) wnd.getSynsets("organization")[0];
		NounSynset timeSynset=(NounSynset) wnd.getSynsets("time")[0];
		NounSynset dateSynset=(NounSynset) wnd.getSynsets("date")[0];
		NounSynset moneySynset=(NounSynset) wnd.getSynsets("money")[0];
		// NounSynset percentSynset=(NounSynset) Skipped!
		NounSynset objectSynset=(NounSynset) wnd.getSynsets("object")[0];
		
		semanticClassMapping.put(maleSynset, SemanticClass.Male);
		semanticClassMapping.put(femaleSynset, SemanticClass.Female);
		semanticClassMapping.put(personSynset, SemanticClass.Person);
		semanticClassMapping.put(locationSynset, SemanticClass.Location);
		semanticClassMapping.put(organizationSynset, SemanticClass.Organization);
		semanticClassMapping.put(timeSynset, SemanticClass.Time);
		semanticClassMapping.put(dateSynset, SemanticClass.Date);
		semanticClassMapping.put(moneySynset, SemanticClass.Money);
		semanticClassMapping.put(objectSynset, SemanticClass.Object);
	}
	
	private Map<NounSynset,SemanticClass> semanticClassMapping=new HashMap<NounSynset,SemanticClass>();
	private WordNetDatabase wnd;
	
	
	public SemanticClass lookupSemanticClass(String word){
		Synset[] s=wnd.getSynsets(word, SynsetType.NOUN);
		if(s.length>0)
			return traverseUpwards((NounSynset) s[0]);
		else
			return SemanticClass.Unknown;
	}
	
	private SemanticClass traverseUpwards(NounSynset ns){
		if(semanticClassMapping.containsKey(ns))
			return semanticClassMapping.get(ns);

		NounSynset[] nss=ns.getHypernyms();//ns.getInstanceHypernyms();
		if(nss.length>0)
			return traverseUpwards(nss[0]);
		else {
			NounSynset[] nssi=ns.getInstanceHypernyms();
			if(nssi.length>0)
				return traverseUpwards(nssi[0]);
			else
				return SemanticClass.Unknown;
		}
			
	}
	
	public boolean isHypernym(String hyponym,String hypernym){
		Synset[] s1=wnd.getSynsets(hyponym,SynsetType.NOUN);
		Synset[] s2=wnd.getSynsets(hypernym,SynsetType.NOUN);
		if(s1.length==0 || s2.length==0)
			return false;
		NounSynset hyponymSet=(NounSynset) s1[0];
		NounSynset hypernymSet=(NounSynset) s2[0];
		
		while(true){
			if(hyponymSet.equals(hypernymSet))
				return true;
			NounSynset[] nss=hyponymSet.getHypernyms();
			if(nss.length>0)
				hyponymSet=nss[0];
			else
				break;
		}
		return false;
	}
	
	private String[][] foo(String form){
		Synset[] synsets=_instance.wnd.getSynsets(form);

		int count=0;
		for(Synset ss:synsets){
			if(ss instanceof NounSynset)
				count++;
//			q[i++]=ss.getWordForms();
		}
		String[][] q=new String[count][];
		count=0;
		for(Synset ss:synsets)
			if(ss instanceof NounSynset)
				q[count++]=ss.getWordForms();

		return q;
	}
	private static String join(String[][] q){
		StringBuilder sb=new StringBuilder();
		for(String[] r:q)
			sb.append(Arrays.toString(r));
		return sb.toString();
	}
	
	public boolean areSynonyms(String s1,String s2){
		String[][] foo1=foo(s1);
		String[][] foo2=foo(s2);
		return eitherContainsOneAnother(foo1,foo2);
	}
	
	public boolean eitherContainsOneAnother(String[][] q1,String[][] q2){
		for(String[] q:q1){
			for(String r:q){
				for(String[] pp:q2){
					for(String p:pp){
						if(p.equals(r))
							return true;
					}
				}
			}
		}
		return false;
	}
	public int wordnetMinDistance(String a1,String a2){
		
		
		return 0;
	}

	public static void main(String[] args){
		new Options(args);
		String[] singleTest=new String[]{"cook","chef","president","presidents","chair","chairman","chairmen","negotiation","negotiations","treaty","banana","bananas","his","he","me","I","boy","girl","man","woman"};
		String[][] pairTest=new String[][]{{"chef","person"},{"chef","president"},{"person","person"},{"chef","entity"},{"entity","chef"},{"apple","banana"},{"banana","tree"},{"drop","fall"}};
		WordNetInterface wni=WordNetInterface.theInstance();
		for(String s:singleTest){
			SemanticClass klass=wni.lookupSemanticClass(s);
			System.out.println(s+"\t"+klass);
		}
		for(String[] p:pairTest)
			System.out.println(p[0]+"\t"+p[1]+"\t"+wni.isHypernym(p[0], p[1])+"\t"+wni.wordnetMinDistance(p[0],p[1]));
		System.out.println("-----");
		for(String s:singleTest)
			System.out.println(s+"\t"+join(wni.foo(s)));
	}

}
