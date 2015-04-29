package ims.hotcoref.symbols;

import ims.hotcoref.data.Document;
import ims.hotcoref.data.Instance;
import ims.hotcoref.data.NE;
import ims.hotcoref.data.Sentence;
import ims.hotcoref.data.CFGTree.CFGNode;
import ims.hotcoref.data.CFGTree.NonTerminal;
import ims.hotcoref.features.FeatureSet;
import ims.hotcoref.features.extractors.TokenTrait;
import ims.hotcoref.io.DocumentReader;
import ims.hotcoref.lang.Language;
import ims.hotcoref.mentiongraph.gengraph.InstanceCreator;
import ims.util.DBO;
import ims.util.Util;

import java.io.Serializable;
import java.util.Set;

public class SymbolTable implements Serializable {
	private static final long serialVersionUID = 6053605795146989988L;

	public static final String ZIP_ENTRY = "_symtab";
	
	public enum Types { 
		Form, Pos, Lemma, BWUV, Deprel, 
		Category,  NE, CfgSubCat, 
		SSCFGPath, DSCFGPath,
		SSFormPath, SSPosPath, SSLemmaPath, SSBWUVPath, 
		DSFormPath, DSPosPath, DSLemmaPath, DSBWUVPath,
		ESForm, ESPos, ESLemma, ESBWUV,
		ESTForm, ESTLemma, ESTBWUV,
		WSForm, WSPos, WSLemma, WSBWUV,	
		Char, CharBigram,
		Genre,
		CoordTokens,
		PathToRoot,PathToRootWDir
		
		;
	}
	
//	public ISymbolMapping<String> tokenTraits[];
	//Could do this differently (using above array)
	public StringSymbolMapping
		forms,			//Simple (string)
		lemmas,			//Simple (string)
		bwuv,			//Simple (string)
		tags,			//Simple (string)
		deprels,		//Simple (string)
		cats,			//Simple (string)
		nes,			//Simple (string)
		chars,			//Simple (string)
		charBigrams,	//Simple (string)
		genre;
	
	public IntSymbolMapping coordTokens;
		
	public CharArraySymbolMapping
		cfgSubCats,		//Complex (char array)
		ssCFGPaths,		//c
		dsCFGPaths;		//c
		
	public CharArraySymbolMapping
		pathToRoot,
		pathToRootWDir;
	
	public CharArraySymbolMapping[]
			ssTraitPaths,
			dsTraitPaths,
			wsTrait;
	
	public StringSymbolMapping[]
			esTrait;
	
	public StringSymbolMapping[]
			esTTrait;
	
	private boolean initialized=false;
	
	public SymbolTable(){}
	
	public void initTable(Set<Types> types){
		if(initialized)
			throw new UnsupportedOperationException("cant init twice");
		initialized=true;
		dsTraitPaths=new CharArraySymbolMapping[TokenTrait.values().length];
		ssTraitPaths=new CharArraySymbolMapping[TokenTrait.values().length];
		wsTrait=new CharArraySymbolMapping[TokenTrait.values().length];
		esTrait=new StringSymbolMapping[TokenTrait.values().length];
		esTTrait=new StringSymbolMapping[TokenTrait.values().length];
		for(Types type:types){
			switch(type){
			case Form: 		forms=new StringSymbolMapping(type.toString()); break;
			case Pos:  		tags=new StringSymbolMapping(type.toString()); break;
			case Lemma: 	lemmas=new StringSymbolMapping(type.toString()); break;
			case BWUV:		bwuv=new StringSymbolMapping(type.toString()); break;
			case Deprel:	deprels=new StringSymbolMapping(type.toString()); break;
			case Category:	cats=new StringSymbolMapping(type.toString()); break;
			case NE:		nes=new StringSymbolMapping(type.toString()); break;
			case Char:		chars=new StringSymbolMapping(type.toString()); break;
			case CharBigram:charBigrams=new StringSymbolMapping(type.toString()); break;
			case Genre:		genre=new StringSymbolMapping(type.toString()); break;
			case CoordTokens: coordTokens=new IntSymbolMapping(type.toString()); break;
			
			//Complex ones below (lots missing)
			case CfgSubCat:		cfgSubCats=new CharArraySymbolMapping(type.toString()); break;
			case SSCFGPath:		ssCFGPaths=new CharArraySymbolMapping(type.toString()); break;
			case SSFormPath:	ssTraitPaths[TokenTrait.Form.ordinal()]=new CharArraySymbolMapping(type.toString()); break;
			case SSPosPath:		ssTraitPaths[TokenTrait.Pos.ordinal()]=new CharArraySymbolMapping(type.toString()); break;
			case SSLemmaPath:
			case SSBWUVPath:	throw new Error("not implemented");
			case DSCFGPath:		dsCFGPaths=new CharArraySymbolMapping(type.toString()); break;
			case DSFormPath:	dsTraitPaths[TokenTrait.Form.ordinal()]=new CharArraySymbolMapping(type.toString()); break;
			case DSPosPath:
			case DSLemmaPath:
			case DSBWUVPath:	throw new Error("not implemented");
			case WSForm:		wsTrait[TokenTrait.Form.ordinal()]=new CharArraySymbolMapping(type.toString()); break;
			case WSPos:			wsTrait[TokenTrait.Pos.ordinal()]=new CharArraySymbolMapping(type.toString()); break;
			case WSLemma:		wsTrait[TokenTrait.Lemma.ordinal()]=new CharArraySymbolMapping(type.toString()); break;
			case WSBWUV:		wsTrait[TokenTrait.BWUV.ordinal()]=new CharArraySymbolMapping(type.toString()); break;
			case ESForm:		esTrait[TokenTrait.Form.ordinal()]=new StringSymbolMapping(type.toString()); break;
			case ESBWUV:		esTrait[TokenTrait.BWUV.ordinal()]=new StringSymbolMapping(type.toString()); break;
			case ESTForm:		esTTrait[TokenTrait.Form.ordinal()]=new StringSymbolMapping(type.toString()); break;
			case ESTBWUV:		esTTrait[TokenTrait.BWUV.ordinal()]=new StringSymbolMapping(type.toString()); break;
			case ESTLemma:		esTTrait[TokenTrait.Lemma.ordinal()]=new StringSymbolMapping(type.toString()); break;
			
			case PathToRoot:      pathToRoot=new CharArraySymbolMapping(type.toString()); break;
			case PathToRootWDir:  pathToRootWDir=new CharArraySymbolMapping(type.toString()); break;
			default: throw new RuntimeException("not implemented: "+type.toString());
			}
		}
	}
	
	public int registerSimple(DocumentReader reader, int count){
		long start=System.currentTimeMillis();
		DBO.println("Registering simple types in symbol table:");
		DBO.printWithPrefix("Doc:  ");
		int erase=0;
		int docCount=0;
		for(Document d:reader){
			erase=DBO.eraseAndPrint(erase,Integer.toString(docCount));
			docCount++;
			if(genre!=null) genre.addSymbol(d.genre);
			for(Sentence s:d.sen){
				if(this.forms!=null) for(int i=0;i<s.forms.length;++i) forms.addSymbol(s.forms[i]);
				if(this.lemmas!=null) for(int i=0;i<s.forms.length;++i) lemmas.addSymbol(s.lemmas[i]);
				if(this.bwuv!=null) for(int i=0;i<s.forms.length;++i) bwuv.addSymbol(s.bwuv[i]);
				if(this.tags!=null) for(int i=0;i<s.forms.length;++i) tags.addSymbol(s.tags[i]);
				if(this.deprels!=null) for(int i=0;i<s.forms.length;++i) deprels.addSymbol(s.dt.lbls[i]);
				if(this.cats!=null) addCatsRecursive(s.ct.root);
				if(this.nes!=null) for(NE ne:s.nes) nes.addSymbol(ne.getLabel());
				if(this.chars!=null) for(int i=0;i<s.forms.length;++i) { chars.addSymbol(s.forms[i].substring(0,1)); chars.addSymbol(s.forms[i].substring(s.forms[i].length()-1)); }
				if(this.charBigrams!=null) for(int i=0;i<s.forms.length;++i) if(s.forms[i].length()>1) { charBigrams.addSymbol(s.forms[i].substring(0,2)); charBigrams.addSymbol(s.forms[i].substring(s.forms[i].length()-2)); }
				if(this.coordTokens!=null) for(int i=0;i<s.forms.length;++i) if(Language.getLanguage().isCoordToken(s.forms[i])) { coordTokens.addSymbol(forms.lookup(s.forms[i])); } 
			}
			if(docCount>count)
				break;
		}

		long time=System.currentTimeMillis()-start;
		DBO.printlnNoPrefix(",  time: "+Util.insertCommas(time));
		DBO.println();
		DBO.printlnNoPrefix("Types");
		if(this.forms!=null) 		{DBO.printlnNoPrefix(String.format("Forms:         %5d : %2d", forms.getItems(),forms.getBits())); 				forms.freeze();             }
		if(this.lemmas!=null) 		{DBO.printlnNoPrefix(String.format("Lemmas:        %5d : %2d", lemmas.getItems(),lemmas.getBits()));			lemmas.freeze();			}
		if(this.bwuv!=null) 		{DBO.printlnNoPrefix(String.format("BWUV:          %5d : %2d", bwuv.getItems(),bwuv.getBits()));				bwuv.freeze();				}
		if(this.tags!=null) 		{DBO.printlnNoPrefix(String.format("Tags:          %5d : %2d", tags.getItems(),tags.getBits()));				tags.freeze();				}
		if(this.deprels!=null) 		{DBO.printlnNoPrefix(String.format("Deprels:       %5d : %2d", deprels.getItems(),deprels.getBits()));			deprels.freeze();			}
		if(this.cats!=null)			{DBO.printlnNoPrefix(String.format("Cats:          %5d : %2d", cats.getItems(),cats.getBits()));				cats.freeze();				}
		if(this.nes!=null) 			{DBO.printlnNoPrefix(String.format("NEs:           %5d : %2d", nes.getItems(),nes.getBits()));					nes.freeze();				}
		if(this.chars!=null) 		{DBO.printlnNoPrefix(String.format("Chars:         %5d : %2d", chars.getItems(),chars.getBits()));				chars.freeze();				}
		if(this.charBigrams!=null) 	{DBO.printlnNoPrefix(String.format("CharBigrams:   %5d : %2d", charBigrams.getItems(),charBigrams.getBits()));	charBigrams.freeze();		}
		if(this.genre!=null) 		{DBO.printlnNoPrefix(String.format("Genre:         %5d : %2d", genre.getItems(),genre.getBits()));				genre.freeze();				}
		if(this.coordTokens!=null)  {DBO.printlnNoPrefix(String.format("Coordinations: %5d : %2d", coordTokens.getItems(),coordTokens.getBits()));  coordTokens.freeze();       }
		DBO.println();
		return docCount;
	}

	private void addCatsRecursive(NonTerminal root) {
		cats.addSymbol(root.getLabel());
		for(CFGNode n:root.getChildren()) 
			if(n instanceof NonTerminal) 
				addCatsRecursive((NonTerminal) n); 
			else cats.addSymbol(n.getLabel());
	}

	public Instance[] registerComplexAndCreateInstances(DocumentReader reader, InstanceCreator ic,int totDocCount,FeatureSet fs,boolean doClearSpans) {
		long start=System.currentTimeMillis();
		DBO.println("Registering complex types in symbol table:");
		DBO.printWithPrefix("Doc:  ");
		int erase=0;
		int docCount=0;
		Instance[] inst=new Instance[totDocCount];
		long timeSInst=0;
		long timeGenerate=0;
		long timeFill=0;
		long timeClear=0;
		int totalEdges=0;
		int totalNodes=0;
		int totalSubGraphs=0;
		for(Document d:reader){
			erase=DBO.eraseAndPrint(erase,Integer.toString(docCount));
			long tn1=System.currentTimeMillis();
			d.initSInst(this);
			long t0=System.currentTimeMillis();
			timeSInst+=t0-tn1;
			inst[docCount]=ic.createTrainingInstance(d);
			long t1=System.currentTimeMillis();
			timeGenerate+=t1-t0;
			fs.fillInstance(inst[docCount],d,this);
			long t2=System.currentTimeMillis();
			timeFill+=t2-t1;
			if(doClearSpans)
				inst[docCount].clearSpans();
			long t3=System.currentTimeMillis();
			timeClear+=t3-t2;
			totalNodes+=inst[docCount].nodes.length;
			totalEdges+=(inst[docCount].nodes.length*inst[docCount].nodes.length-1)/2;
			totalSubGraphs+=inst[docCount].chainNodes.length;
			
//			{ //DEBUG
//				CorefSolution gold=GoldStandardChainExtractor.getGoldCorefSolution(d);
//				Map<Span,Integer> span2int=gold.getSpan2IntMap();
//				Map<Integer,Chain> int2chain=gold.getChainMap();
//				for(int i=1;i<inst[docCount].nodes.length;++i){
//					MNode right=(MNode) inst[docCount].nodes[i];
//					Integer r=span2int.get(right.span);
//					Chain c=(r==null?null:int2chain.get(r));
//					for(int j=0;j<i;++j){
//						boolean ss1=Decoder.sameChain(i, j, inst[docCount]);
//						if(j!=0){
//							MNode left=(MNode) inst[docCount].nodes[j];
//							Integer l=span2int.get(left.span);
//							if((l!=null && r!=null && l.equals(r)) ^ ss1)
//								System.out.println("priblim here");
//						} else {
//							boolean ss2=c==null || c.getNodes().get(0).equals(right);
//							if(ss2!=ss1){
//								System.out.println("probullm "+ss1+" "+ss2);
//								Decoder.sameChain(i, j, inst[docCount]);
//							}
//						}
//					}
//				}
//			}
			docCount++;
			if(docCount==inst.length)
				break;
		}
		long time=System.currentTimeMillis()-start;
		DBO.printlnNoPrefix(",  time: "+Util.insertCommas(time));
		DBO.printlnNoPrefix("Time SInst: "+Util.insertCommas(timeSInst));
		DBO.printlnNoPrefix("Time gen graph: "+Util.insertCommas(timeGenerate)+", out of which");
		DBO.printlnNoPrefix(" time create edges:  "+Util.insertCommas(InstanceCreator.TIME_CREATE_EDGES));
		DBO.printlnNoPrefix(" time create sgs:    "+Util.insertCommas(InstanceCreator.TIME_SUBGRAPHS));
		DBO.printlnNoPrefix(" time create inst:   "+Util.insertCommas(InstanceCreator.TIME_INSTANCE_CONSTRUCTOR));
		DBO.printlnNoPrefix(" time rest:          "+Util.insertCommas(timeGenerate-InstanceCreator.TIME_INSTANCE_CONSTRUCTOR-InstanceCreator.TIME_SUBGRAPHS-InstanceCreator.TIME_CREATE_EDGES));
		DBO.printlnNoPrefix("Time fill instances: "+Util.insertCommas(timeFill));
		DBO.printlnNoPrefix("Time clear:          "+Util.insertCommas(timeClear));
		DBO.println();
		DBO.printlnNoPrefix("Complex Types");
		if(this.cfgSubCats!=null)            { DBO.printlnNoPrefix(String.format("CFGSubCats     %7d : %2d",cfgSubCats.getItems(),cfgSubCats.getBits()));		cfgSubCats.freeze(); 		}
		if(this.ssCFGPaths!=null)            { DBO.printlnNoPrefix(String.format("SSCFGPaths     %7d : %2d",ssCFGPaths.getItems(),ssCFGPaths.getBits()));		ssCFGPaths.freeze(); 		}
		if(this.dsCFGPaths!=null)            { DBO.printlnNoPrefix(String.format("DSCFGPaths     %7d : %2d",dsCFGPaths.getItems(),dsCFGPaths.getBits()));		dsCFGPaths.freeze();		}
		for(int k=0;k<TokenTrait.values().length;++k){
			if(wsTrait[k]!=null){
				wsTrait[k].freeze();
				DBO.printlnNoPrefix(String.format("%-12s    %7d : %2d", "WS["+TokenTrait.values()[k]+"]",wsTrait[k].getItems(),wsTrait[k].getBits()));
			}
		}
		for(int k=0;k<TokenTrait.values().length;++k){
			if(ssTraitPaths[k]!=null){
				DBO.printlnNoPrefix(String.format("%-12s    %7d : %2d","SSTP["+TokenTrait.values()[k].toString()+"]",ssTraitPaths[k].getItems(),ssTraitPaths[k].getBits()));		
				ssTraitPaths[k].freeze();
			}
		}
		for(int k=0;k<TokenTrait.values().length;++k){
			if(dsTraitPaths[k]!=null){
				DBO.printlnNoPrefix(String.format("%-12s    %7d : %2d","DSTP["+TokenTrait.values()[k].toString()+"]",dsTraitPaths[k].getItems(),dsTraitPaths[k].getBits()));		
				dsTraitPaths[k].freeze();
			}
		}
		for(int k=0;k<TokenTrait.values().length;++k){
			if(esTrait[k]!=null){
				DBO.printlnNoPrefix(String.format("%-12s    %7d : %2d", "EST["+TokenTrait.values()[k]+"]",esTrait[k].getItems(),esTrait[k].getBits()));
				esTrait[k].freeze();
			}
		}
		for(int k=0;k<TokenTrait.values().length;++k){
			if(esTTrait[k]!=null){
				esTTrait[k].freeze();
				DBO.printlnNoPrefix(String.format("%-12s    %7d : %2d", "ESTT["+TokenTrait.values()[k]+"]",esTTrait[k].getItems(),esTTrait[k].getBits()));
			}
		}
		if(this.pathToRoot!=null)		{DBO.printlnNoPrefix(String.format("PathToRoot     %7d : %2d", pathToRoot.getItems(),pathToRoot.getBits()));			pathToRoot.freeze();}
		if(this.pathToRootWDir!=null)	{DBO.printlnNoPrefix(String.format("PathToRootWD   %7d : %2d", pathToRootWDir.getItems(),pathToRootWDir.getBits()));	pathToRootWDir.freeze();}
		DBO.println();
		DBO.println("Edges: "+Util.insertCommas(totalEdges,false)+", nodes: "+Util.insertCommas(totalNodes,false));
		DBO.println("Subgraphs: "+Util.insertCommas(totalSubGraphs,false));
		return inst;
	}

}
