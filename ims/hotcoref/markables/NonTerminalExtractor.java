package ims.hotcoref.markables;

import ims.hotcoref.data.Sentence;
import ims.hotcoref.data.Span;
import ims.hotcoref.data.CFGTree.CFGNode;
import ims.hotcoref.data.CFGTree.NonTerminal;

import java.util.Set;

public class NonTerminalExtractor extends AbstractMarkableExtractor{
	private static final long serialVersionUID = 1L;
	
	final String label;
	
	public NonTerminalExtractor(String label){
		this.label=label;
	}

	@Override
	public void extractMarkables(Sentence s, Set<Span> sink,String docName) {
		recurseAndAdd(s.ct.root,sink);
	}

	private void recurseAndAdd(CFGNode cfgNode,Set<Span> sink){
		if(cfgNode instanceof NonTerminal){
			if(cfgNode.getLabel().equals(label)){
				sink.add(cfgNode.getSpan());
			}
			for(CFGNode n:cfgNode.getChildren())
				recurseAndAdd(n,sink);
		}
	}
	
	public String toString(){
		return "NT-"+label;
	}
}
