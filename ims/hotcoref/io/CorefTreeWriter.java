package ims.hotcoref.io;

import ims.hotcoref.data.Document;
import ims.hotcoref.data.Instance;
import ims.hotcoref.decoder.HOTState;
import ims.util.Util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;

public class CorefTreeWriter {
	final BufferedWriter out;
	private final String treeNamePrefix;
	
	public CorefTreeWriter(File out,String namePrefix) throws IOException{
		this.out=Util.getWriter(out);
		this.treeNamePrefix=namePrefix;
	}
	
	public void writeTree(HOTState outState,Document d,Instance inst,String treeNameSuffix) throws IOException{
		String[] nodeSignatures=new String[inst.nodes.length];
		String[] nodeAttributes=new String[inst.nodes.length];
		for(int i=0;i<inst.nodes.length;++i){
			nodeSignatures[i]=inst.nodes[i].getDotNodeIdentifier();
			StringBuilder sb=new StringBuilder();
			for(Entry<String,String> e:inst.nodes[i].getNodeAttributeList().entrySet()){
				sb.append(e.getKey()).append(":").append(e.getValue()).append(";");
			}
			nodeAttributes[i]=sb.toString();
		}
		//Header
//		out.write("DocTree\n"+d.header);
		out.write(d.header+"\n");
		out.write("#id "+treeNamePrefix+treeNameSuffix+"\n");
		//Nodes
		out.write("#begin nodes\n");
		for(int i=0;i<inst.nodes.length;++i){
			StringBuilder sb=new StringBuilder(nodeSignatures[i]).append('\t').append(nodeAttributes[i]);
			out.write(sb.toString());
			out.write('\n');
		}
		out.write("#end nodes\n");
		//Edges
		out.write("#begin edges\n");
		int[] heads=outState.heads;
		for(int i=1;i<heads.length;++i){
			String edgeAttributes="Type:IDENT";
			StringBuilder ed=new StringBuilder(nodeSignatures[heads[i]]).append(">>").append(nodeSignatures[i]).append('\t').append(edgeAttributes);
			out.write(ed.toString());
			out.write('\n');
		}
		//Footer
		out.write("#end edges\n");
		out.write("#end document\n");
	}
	
	
	public void close() throws IOException{
		out.close();
	}
}
