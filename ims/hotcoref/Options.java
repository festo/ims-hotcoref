package ims.hotcoref;

import ims.hotcoref.perceptron.Regularizer.RegularizerTypes;
import ims.util.DBO;
import ims.util.ThreadPoolSingleton;
import ims.util.Util;

import java.io.File;
import java.io.OutputStream;
import java.util.Date;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class Options {

	public final Date optionsCreate=new Date();
	
	public enum Format { C12, C12AUG, HTML }
	public enum HeadFinding { Rules, DepTree }
	
	@Option(name="-lang",aliases="--language",usage="Set language (default eng)")
	public String lang="eng";
	
	/*
	 *  begin IO stuff
	 */
	@Option(name="-in",aliases="--input",usage="Input file",required=true)
	public File in;
	@Option(name="-in2",aliases="--input2",usage="Second input file (rarely used)")
	public File in2;
	@Option(name="-out",aliases="--output",usage="Output file")
	public File out=new File("out");
	@Option(name="-gold",usage="Gold file")
	public File gold;
	@Option(name="-inFormat",aliases="--inputFormat",usage="Input format (default C12AUG)")
	public Format inputFormat=Format.C12;
	@Option(name="-outFormat",aliases="--outputFormat",usage="Output format (default C12AUG)")
	public Format outputFormat=Format.C12;
	@Option(name="-inEnc",aliases="--inputEncoding",usage="Input file encoding")
	public String inputEnc="UTF-8";
	@Option(name="-outEnc",aliases="--outputEncoding",usage="Output file encoding")
	public String outputEnc="UTF-8";
	@Option(name="-inGz",aliases="--inputGzipped",usage="Input file is gzipped")
	public boolean inGz=false;
	@Option(name="-outGz",aliases="--outputGzipped",usage="Output file is gzipped")
	public boolean outGz=false;
	@Option(name="-graphOutDir",usage="out dir for graphs/files")
	public File graphOutDir;
	@Option(name="-ignoreSingletons",usage="ignore singletons when drawing graphs")
	public boolean ignoreSingletons=false;
	@Option(name="-drawLatentHeads",usage="draw latent heads in output graphs")
	public boolean drawLatentHeads=false;
	@Option(name="-ignoreRoot",usage="ignore root node when drawing")
	public boolean ignoreRoot=false;
	@Option(name="-foif",aliases="--fileOutInfix",usage="file infix")
	public String fileOutInfix="";
	@Option(name="-errorAnalysis",usage="do error analysis while testing (requires gold input)")
	public boolean errorAnalysis=false;
	
	@Option(name="-icarusOut",usage="output icarus trees")
	public boolean outIcarusTrees=false;
	
	@Option(name="-model",usage="Model file")
	public void setModel(String modelstr){
		if(modelstr.equalsIgnoreCase("null"))
			this.model=null;
		else
			this.model=new File(modelstr);
	}
	public File model=null;
	@Option(name="-treeModel",usage="model to precompute trees for instances")
	public File treeModel=null;
	/*
	 * end IO stuff
	 */

	
	/*
	 * begin other runtime things
	 */
	@Option(name="-dontClearSpans",usage="Don't clear spans (needs more memory)")
	public static boolean dontClearSpans=false;
	/*
	 * end other runtime things
	 */
	
	/*
	 * begin model/algorithmic parameters
	 */
	@Option(name="-mes",aliases="--markableExtractor,--markableExtractors",usage="Comma-separated list of markable extractors")
	public String markableExtractors;
	@Option(name="-ecs",aliases="--edgeCreators",usage="what edge creators to use")
	public String edgeCreators;
//	@Option(name="-gGenType",aliases="--graphGeneratorType",usage="Type of graph generator (default CompleteLeftGraph)")
//	public GraphGeneratorTypes gGenType=GraphGeneratorTypes.CompleteLeftGraph;
//	@Option(name="-windowSize",usage="window size (default 100)")
//	public int leftWindowSize=100;
//	@Option(name="-bidir",aliases="--bidirectional",usage="generate edges in both directions (between mentions)")
//	public boolean bidirectionalGraphGen=false;
//	@Option(name="-dirbit",aliases="--useDirBit",usage="add one bit to indicate direction of the edge")
//	public boolean dirBit=false;
	@Option(name="-features",usage="file with feature set")
	public File featureFile;
	@Option(name="-rootLoss",usage="loss for edges that are erroneously attached to the root")
	public float rootLoss=1.5f;
//	@Option(name="-lossAugmented",usage="use loss augmentation during prediction in training")
//	public boolean lossAugmented=false;
	@Option(name="-dontPruneVerbs",usage="Don't prune verbs during trainign etc")
	public static boolean dontPruneVerbs=false;
	@Option(name="-dontInject",usage="Don't inject gold mentions that were not extracted")
	public static boolean dontInjectGold=false;
	@Option(name="-beam",usage="beam size")
	public int beam=1;
//	@Option(name="-beamUpdate",usage="update strategy for beam search")
//	public Update beamUpdate=Update.Standard;
//	@Option(name="-mvType",usage="mv type (1, 2, or 3)")
//	public int mvType=3;
//	@Option(name="-maxVioRightWindow",usage="right window for max vio training")
//	public int maxVioRightWindow=Integer.MAX_VALUE-1;
	@Option(name="-beamEarlyIter",usage="Iterative early update with beam search")
	public boolean beamEarlyIter=false;
	@Option(name="-delayUpdates",usage="delay beam search updates until end of doc")
	public boolean delayUpdates=false;
	@Option(name="-guided",usage="Use guided perceptron learning")
	public boolean guided=false;
	@Option(name="-guidedCount",usage="max number of repeated updates for guided")
	public int guidedCount=10;
	@Option(name="-count",usage="maximum number of training instances")
	public int count=Integer.MAX_VALUE-1;
	@Option(name="-testEveryIter",usage="Test at every iteration during training")
	public boolean testAtEveryIter=false;
	/*
	 * end model/algorithmic parameters
	 */
	
	
	/*
	 * begin perceptron parameters
	 */
	@Option(name="-reg",aliases="--regularizer",usage="which PA regularizer to use")
	public RegularizerTypes regularizerType=RegularizerTypes.PA0;
	@Option(name="-C",usage="the C parameter for the regularizer (default 0.1)")
	public float C=0.1f;
	@Option(name="-hsize",aliases="--hashSize",usage="size of parameter vector for hash kernel")
	public int hashSize=0x07ffffff; //134200007 this is a prime closeby
//	public int hashSize=90002351;
	@Option(name="-iter",aliases="--iterations",usage="number of iterations for perceptron")
	public int iterations=25;
	@Option(name="-dontShuffle",usage="don't shuffle training instances")
	public boolean dontShuffle=false;
	@Option(name="-hotDelay",usage="the iteration at which higher-order features will kick in")
	public int hotDelay=0;
	@Option(name="-cw",usage="confidence weighted")
	public boolean cw=false;
	@Option(name="-arow",usage="adaptive regularization")
	public boolean arow;
	
	
	public boolean useHashKernel=true;
	@Option(name="-nohk",aliases="--noHashKernel",usage="Don't use the hash kernel -- use exact mapping")
	public void setNoHK(Boolean b){
		useHashKernel=false;
	}
	/*
	 * 
	 */

	
	
	/*
	 * begin Static ones (too lazy to make this nicer)
	 */
	@Option(name="-wn",aliases="--wordnet,--wordNetDictDir",usage="The path to the wordnet dict dir")
	public static File wordNetDictDir;
	@Option(name="-hf",aliases="--headFinding",usage="Head finding rules (default DepTree)")
	public static HeadFinding headFinding=HeadFinding.Rules;
	@Option(name="-gender",aliases="--genderData",usage="The bergsma/lin gender data file")
	public static File genderData;
	@Option(name="-anaphTh",aliases="--anaphoricityTh", usage="Probability threshold for the anaphoricity classifier (high means filter less; default 0.95)")
	public static double anaphoricityThreshold = 0.95;
	@Option(name="-cores",aliases="-threads",usage="Number of threads available for threadpools (default half of available)")
	public static int cores = (int) Math.max(1, Math.floor(Runtime.getRuntime().availableProcessors()/2.0));
	@Option(name="-customAnaphoricityTh",usage="comma separated string of token specific anaphoricity ths")
	public static String customTokenAnaphoricityTh;
	@Option(name="-debug",usage="more debug output")
	public static boolean DEBUG=false;
	@Option(name="-unnoisy",usage="other debug output")
	public static boolean NO_NOISE=false;
	/*
	 * end Static ones
	 */

	
	
	/*
	 * scorer service
	 */
	public String scorerHost;
	public int scorerPort;
	@Option(name="-scorer",usage="host for scorer server")
	public void setScorer(String s){
		String[] b=s.split(":");
		if(b.length!=2)
			throw new Error("!");
		scorerHost=b[0];
		scorerPort=Integer.parseInt(b[1]);
	}
	/*
	 * end scorer service
	 */
	
	public Options(String[] args){
		CmdLineParser clp=new CmdLineParser(this);
		try {
			clp.parseArgument(args);
		} catch (CmdLineException e) {
			e.printStackTrace();
			System.err.println("Failed to parse cmd line, aborting.");
			System.err.println(e.getMessage());
			System.err.println();
			System.err.println("Valid options are:");
			printUsage(System.err);
			System.exit(1);
		}
	}
	private Options(){}
	public static void printUsage(OutputStream out){
		CmdLineParser clp=new CmdLineParser(new Options());
		clp.printUsage(out);
	}	
	public static void main(String[] args){
		printUsage(System.out);
	}
	
	public void done(){
		DBO.println("done() at "+new Date());
		DBO.println("Total time "+Util.insertCommas(System.currentTimeMillis()-optionsCreate.getTime()));
		ThreadPoolSingleton.shutdown();
		DBO.close();
	}
}