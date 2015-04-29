package ims.hotcoref.markables;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.regex.Pattern;

import cc.mallet.classify.MaxEnt;
import cc.mallet.classify.MaxEntL1Trainer;
import cc.mallet.classify.MaxEntOptimizableByLabelLikelihood;
import cc.mallet.types.Alphabet;
import cc.mallet.types.AugmentableFeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Label;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.Labeling;
import cc.mallet.util.MalletLogger;
import cc.mallet.util.MalletProgressMessageLogger;

public class NonReferentialClassifier implements ITokenNonReferentialClassifier {

  private static final long serialVersionUID = -4359763882060712033L;

  static{((MalletLogger)MalletProgressMessageLogger.getLogger(MaxEntOptimizableByLabelLikelihood.class.getName())).getRootLogger().setLevel(Level.WARNING);}
  
  protected Map<String,MaxEnt> model = null;	
  transient protected Map<String,InstanceList> trainingdata = null;

  protected final double threshold;
  protected final String language;
  protected final Set<String> TARGETS;
  protected final Map<String,Double> customTokenTh;
  
  	public static final Pattern corefed = Pattern.compile("([0-9]+)");
	public static final List<String> BE = Arrays.asList(new String[]{"is","'s","was","be","'m","'re","are","were","been"});
	public static final List<String> COMMA = Arrays.asList(new String[]{",",":",";","?","!",".","-"});
	public static final List<String> WH1 = Arrays.asList(new String[]{"what","which","where","whether","why","how"});
	public static final List<String> WH2 = Arrays.asList(new String[]{"who","which","that"});
	public static final List<String> TIME = Arrays.asList(new String[]{"time","year","years","days","winter","summer","automn","spring","monday","tuesday","wednesday","thursday","friday","saturday","sunday","january","february","march","april","may","june","july","august","september","october","december"});
	public static final List<String> PVERBS = Arrays.asList(new String[]{"said","predicted","having","understood","agreed","decided","argued","learned","reported","announced","hoped","thought","believed","estimated"});
	public static final List<String> PVERBSTO = Arrays.asList(new String[]{"say","see","know","eat","drink","tell","do","get","talk"});
	public static final List<String> PNOUN = Arrays.asList(new String[]{"anything","people","mistake","matter","fun"});
	public static final List<String> AVERBS = Arrays.asList(new String[]{"made","done","built","based","called"});
	public static final List<String> PADJ = Arrays.asList(new String[]{"interesting","safe","built","impossible","shameful","ok","okay","clear","unclear","easier","better","late","awful","terrible","difficult","hard","unlikely","early","surprising"});
	public static final List<String> PADJ2 = Arrays.asList(new String[]{"same","hard","important","necessary","difficult"});

	public static Set<String> DEFAULT_ENG_TARGETS = new TreeSet<String>(Arrays.asList(new String[]{"it","you","we"})); 
	// they
	public static Set<String> DEFAULT_CHI_TARGETS = new TreeSet<String>(Arrays.asList(new String[]{"你","中国"}));
	//我们 TP=97 FP=79 FN=113
	//你      TP=28 FP=7 FN=96
	//现在 TP=42 FP=45 FN=46
	//中国 TP=57 FP=17 FN=41
	public static Set<String> DEFAULT_ARA_TARGETS = new TreeSet<String>(Arrays.asList(new String[]{"بَيْرُوتَ#bayoruwt#byrwt#bayoruwt+a","هُوَ#huwa#hw#huwa","-نا#clitics#na#-na"}));
	//بَيْرُوتَ#bayoruwt#byrwt#bayoruwt+a TP=8 FP=2 FN=8
	//هُوَ#huwa#hw#huwa TP=23 FP=10 FN=2
	//-نا#clitics#na#-na TP=67 FP=13 FN=0
	
	public NonReferentialClassifier(String _language, double _threshold, Set<String> targets,Map<String,Double> customTokenTh){
		language = _language;
		TARGETS = targets;
		model = null;
		trainingdata = new HashMap<String,InstanceList>();
		for(String target : TARGETS){
			Alphabet featureAlphabet = new Alphabet();
			LabelAlphabet labelAlphabet = new LabelAlphabet();
			featureAlphabet.startGrowth();
			trainingdata.put(target, new InstanceList(featureAlphabet, labelAlphabet));
		}
		threshold  = _threshold;
		this.customTokenTh=customTokenTh;
	}
	
  public void extractTrainingInstances(String[] forms, String[] pos, String[] corefCol,String genre) {
		if(trainingdata==null){
			System.err.println("You shouldn't call extractTrainingInstances after you trained a model!!!");
			System.exit(1);
		}
  	for(int i=1;i<forms.length;++i){
			String token=forms[i].toLowerCase();
			if(TARGETS.contains(token)){
				AugmentableFeatureVector v = featureExtraction(forms, pos, i,genre);
				Boolean pleonastic = !corefed.matcher(corefCol[i]).find(); 
				String context = "";
				for(int k=-3;k<=5;++k)
					if(i+k>0 && i+k<forms.length)
						context += " " + forms[i+k];
				Label lab = ((LabelAlphabet)trainingdata.get(token).getTargetAlphabet()).lookupLabel(pleonastic);
				trainingdata.get(token).add(new Instance(v,lab,null,context));
			}
		}
  }

  public void train() {
		if(trainingdata==null){
			System.err.println("You cannot train the model twice!!!");
			System.exit(2);
		}
		model = new HashMap<String, MaxEnt>();
//		double L1reg = language.equals("ara") ? 5.0 : 0.1;
		double L1reg=0.1;
		for(String target : trainingdata.keySet())
			model.put(target, new MaxEntL1Trainer(L1reg).train(trainingdata.get(target)));
		trainingdata = null;
  }

  public boolean[] classifiy(String[] forms, String[] pos,String genre) {
	  if(model==null){
		  System.err.println("You should first train the model!!!");
		  System.exit(3);
	  }
	  boolean[] ret = new boolean[forms.length];
	  Arrays.fill(ret, false);
	  for(int i=1;i<forms.length;++i){
		  String token=forms[i].toLowerCase();
		  if(TARGETS.contains(token)){
			  Labeling lab = model.get(token).classify(new Instance(featureExtraction(forms, pos, i, genre),null,null,null)).getLabeling();
			  double posterior = lab.value(model.get(token).getLabelAlphabet().lookupIndex(true));
			  double th=(customTokenTh==null?threshold:customTokenTh.get(token));
			  ret[i] = posterior >= th;
		  }
	  }
	  return ret;
  }

  public double[] classifyGetProbs(String[] forms,String[] pos,String genre){
	  if(model==null){
		  System.err.println("You should first train the model!!!");
		  System.exit(3);
	  }
	  double[] ret=new double[forms.length];
	  Arrays.fill(ret, -1);
	  for(int i=1;i<forms.length;++i){
		  String token=forms[i].toLowerCase();
		  if(TARGETS.contains(token)){
			  Labeling lab = model.get(token).classify(new Instance(featureExtraction(forms, pos, i, genre),null,null,null)).getLabeling();
			  double posterior = lab.value(model.get(token).getLabelAlphabet().lookupIndex(true));
			  ret[i]=posterior;
//			  double th=(customTokenTh==null?threshold:customTokenTh.get(token));
//			  ret[i] = posterior >= th;
		  }
	  }
	  return ret;
  }
  
	protected AugmentableFeatureVector featureExtraction(String[] forms, String[] pos, int i,String genre) {
		String target=forms[i].toLowerCase();
		
		Alphabet alphabet = null;
		if(trainingdata != null)
			alphabet = trainingdata.get(target).getDataAlphabet();
		else
			alphabet = model.get(target).getAlphabet();
		AugmentableFeatureVector afv = new AugmentableFeatureVector(alphabet, 100, true);
		if(!language.equals("eng")){
			genericFeatureExtraction(forms, pos, i, afv);
			return afv;
		}

		if(!target.equals("it")){
			addFeature(afv, "G"+genre);
			addFeature(afv, "WF"+forms[i]);
			for(int k=i+1;k<forms.length && k<i+4;++k){
				if(pos[k].startsWith("NNP") && forms[k-1].equals(",")){
					addFeature(afv, "NE");
					break;
				}
			}
			for(int k=i+1;k<forms.length;++k){
				if(forms[k].equals("''")){
					addFeature(afv,"QUOTE");
					break;
				}
				if(forms[k].equals("``")){
					break;
				}
			}
		}

		if(target.equals("you")){
			int n=0;
			for(String l : forms)
				if(l.equalsIgnoreCase(forms[i]))
					n++;
			addFeature(afv,"MentionNum",n-1);
		}

		if(i==1)
			addFeature(afv, "FirstToken");
		else if(pos[i-1].startsWith("V"))
			addFeature(afv, "F10"+forms[i-1]);
		else if(pos[i-1].equals("IN"))
			addFeature(afv,"F19");

		for(int k=-4;k<=0;++k){
			if(i+k<1)
				continue;
			for(int l=3;l<=5;++l){
				if(k+l<=0 || i+k+l>=forms.length)
					continue;
				String phrase = forms[i+k];
				int len=1;
				for(int j=1;len<l;++j){
//					if(i+k+j>=forms.length)
//						break;
					String t = forms[i+k+j];
//					if(!(t.equals("n't") || t.equals("not") || t.equals("a") || t.equals("an") || t.equals("the"))){
						len++;
						phrase += "_" + t;
//					}
				}
				addFeature(afv, "F17"+phrase);
			}
		}
		
		for(int k=-1;k<=5;++k){
			if(k==0 || i+k<1 || i+k>=forms.length)
				continue;
			addFeature(afv, "F"+(21+k)+pos[i+k].charAt(0));
		}

		String VERB = null, ADJ = null;
		boolean SEEM = false;
		for(int k=i+1;k<forms.length;++k){
			String token = forms[k];
			if(token.equalsIgnoreCase("it")){
				break;
			}
			if(target.equals("it")){
				if(token.equalsIgnoreCase("that")){
					if(VERB!=null && ADJ==null){
						addFeature(afv, "F4-", k-i);
						addFeature(afv, "F4-"+VERB, k-i);
					}
					else if(VERB!=null && ADJ!=null){
						addFeature(afv, "F1-", k-i);
						addFeature(afv, "F1-"+VERB, k-i);
						addFeature(afv, "F1-"+VERB+"/"+ADJ, k-i);
					}
					else if(VERB==null && ADJ!=null){
						addFeature(afv, "F7-", k-i);
						addFeature(afv, "F7-"+ADJ, k-i);
					}
					break;
				}
				if(token.equalsIgnoreCase("to")){
					if(VERB!=null && ADJ==null){
						addFeature(afv, "F6-", k-i);
						addFeature(afv, "F6-"+VERB, k-i);
					}
					else if(VERB!=null && ADJ!=null){
						addFeature(afv, "F3-", k-i);
						addFeature(afv, "F3-"+VERB+"/"+ADJ, k-i);
					}
					else if(VERB==null && ADJ!=null){
						addFeature(afv, "F7-", k-i);
						addFeature(afv, "F7-"+VERB, k-i);
						addFeature(afv, "F7-"+VERB+"/"+ADJ, k-i);
					}
					break;
				}
			}
			if(WH1.contains(token)){
				if(VERB!=null && ADJ==null){
					addFeature(afv, "F5-", k-i);
					addFeature(afv, "F5-"+VERB, k-i);
				}
				else if(VERB!=null && ADJ!=null){
					addFeature(afv, "F2-", k-i);
					addFeature(afv, "F2-"+VERB, k-i);
					addFeature(afv, "F2-"+VERB+"/"+ADJ, k-i);
				}
				break;
			}
			if(COMMA.contains(token) && VERB!=null){
				addFeature(afv, "F9-", k-i);
				addFeature(afv, "F9-"+VERB, k-i);
				break;
			}
			if(token.equalsIgnoreCase("if") && forms[k-1].equalsIgnoreCase("as") && SEEM){
				addFeature(afv, "F8-", k-i);
				break;
			}
			if(token.startsWith("seem") || token.startsWith("appear") || token.startsWith("mean"))
				SEEM=true;
			String POS = pos[k];
			if(POS.startsWith("V"))
				VERB=token;
			if(POS.startsWith("JJ"))
				ADJ=token;
		}
		
		for(int k=i+1;k<forms.length;++k){
			String POS = pos[k];
			if(POS.startsWith("V")){
				addFeature(afv, "F11"+forms[k].toLowerCase());
				if(BE.contains(forms[k].toLowerCase()))
					addFeature(afv, "F16");
				break;
			}
		}

		for(int k=i-1;k>i-3;--k){
			if(k<1) break;
			String POS = pos[k];
			if(POS.startsWith("V")){
				addFeature(afv, "PREVERB"+forms[k].toLowerCase());
				break;
			}
		}

		if(target.equals("it")){
			for(int k=i+1;k<forms.length;++k){
				String POS = pos[k];
				if(POS.startsWith("JJ")){
					addFeature(afv, "F12"+forms[k].toLowerCase());
					break;
				}
			}
		}


		if(i+1<forms.length && BE.contains(forms[i+1].toLowerCase())){
			for(int k=i+1;k<forms.length && k<i+6;++k){
				String token = forms[k].toLowerCase();
				if(TIME.contains(token)){
					addFeature(afv, "TIME");
					break;
				}
				if(PVERBS.contains(token)){
					addFeature(afv, "PVERBS");
					break;
				}
				if(AVERBS.contains(token)){
					addFeature(afv, "AVERBS");
					break;
				}
				if(PVERBSTO.contains(token) && forms[k-1].equalsIgnoreCase("to")){
					addFeature(afv, "PVERBSTO");
					break;
				}
				if(PNOUN.contains(token)){
					addFeature(afv, "PNOUN");
					break;
				}
				if(PADJ.contains(token)){
					addFeature(afv, "PADJ");
					break;
				}
//				if(PADJ2.contains(token)){
//					addFeature(afv, "PADJ2");
//					break;
//				}
			}
		}
		
		return afv;
	}

	protected static void genericFeatureExtraction(String[] forms, String[] pos, int i, AugmentableFeatureVector afv) {
		if(i==1)
			addFeature(afv, "FirstToken");

		for(int k=-4;k<=4;++k){
			if(i+k>=forms.length || i+k<=0 || k==0)
				continue;
			addFeature(afv, "POS"+k+"-"+pos[i+k]);
			addFeature(afv, "POS"+(k<0 ? "B" : "A")+"-"+pos[i+k]);
		}

		for(int k=-4;k<=0;++k){
			if(i+k<0)
				continue;
			for(int l=2;l<=5;++l){
				if(k+l<=0 || i+k+l>=forms.length)
					continue;
				String phrase = forms[i+k];
				for(int j=1;j<l;++j){
					phrase += "_" + forms[i+k+j];
				}
				addFeature(afv, "PHRASE-"+phrase);
			}
		}

		for(int k=i+1;k<forms.length;++k){
			if(pos[k].startsWith("V")){
				addFeature(afv, "VERBA"+forms[k]);
				break;
			}
		}
		for(int k=i-1;k>i-4;--k){
			if(k<0) break;
			if(pos[k].startsWith("V")){
				addFeature(afv, "VERBB"+forms[k]);
				break;
			}
		}
		for(int k=i+1;k<forms.length;++k){
			if(pos[k].startsWith("JJ")){
				addFeature(afv, "ADJ"+forms[k]);
				break;
			}
		}
	}

	private static int disc(int i) {
		if(i<4)
			return 1;
		if(i<7)
			return 5;
		if(i<12)
			return 8;
		return 12;
	}

	public static void addFeature(AugmentableFeatureVector afv, String feature){
		addFeature(afv, feature, 1);
	}
	
	public static void addFeature(AugmentableFeatureVector afv, String feature, int distance){
		feature += disc(distance); 
		int idx = afv.getAlphabet().lookupIndex(feature);
		if (!afv.getAlphabet().growthStopped() || idx != -1){
			afv.add(idx);
		}
	}

}
