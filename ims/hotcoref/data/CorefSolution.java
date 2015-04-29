package ims.hotcoref.data;

import ims.util.IntPair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class CorefSolution {

	private final Map<Integer,Chain> chainMap;
	private final Map<Span,Integer> span2int;
	
	private int idCounter=0;
	
	public CorefSolution(){
		this.chainMap=new HashMap<Integer,Chain>();
		this.span2int=new HashMap<Span,Integer>();
	}
	
	public Set<Span> getSpanSet(){
		return span2int.keySet();
	}
	
	public CorefSolution(Map<Integer, Chain> m) {
		this.chainMap=m;
		this.span2int=new HashMap<Span,Integer>();
		for(Entry<Integer,Chain> e:m.entrySet()){
			Integer key=e.getKey();
			for(Span s:e.getValue().spans){
				span2int.put(s, key);
			}
		}
	}
	
	public CorefSolution(Chain[] chains){
		this(chainArrToMap(chains));
	}

	private static Map<Integer, Chain> chainArrToMap(Chain[] chains) {
		Map<Integer,Chain> m=new HashMap<Integer,Chain>();
		for(Chain c:chains)
			m.put(c.chainId, c);
		return m;
	}
	
	public void addLink(Span a,Span b){
		//We have a problem when a begins before b begins, and a ends before b ends. then they will get mashed...
		
		Integer aId=span2int.get(a);
		Integer bId=span2int.get(b);
		if(aId==null && bId==null){ //create new chain
			Integer chainId=idCounter++;
			Chain ch=new Chain(chainId,a,b);
			chainMap.put(chainId, ch);
			span2int.put(a, chainId);
			span2int.put(b, chainId);
		} else if(aId!=null && bId!=null){
			//Here we need to merge two chains
			Chain aChain=chainMap.get(aId);
			Chain bChain=chainMap.remove(bId);
			for(Span s:bChain.spans){
				aChain.addSpan(s);
				span2int.put(s, aId);
			}
		} else if(aId==null){ //merge a with b
			Chain ch=chainMap.get(bId);
			ch.addSpan(a);
			span2int.put(a, bId);
		} else { //merge b with a
			Chain ch=chainMap.get(aId);
			ch.addSpan(b);
			span2int.put(b, aId);
		}
	}

	public List<Span> getSpanList(Integer chainID) {
		return chainMap.get(chainID).spans;
	}

	public int getChainCount() {
		return chainMap.size();
	}

	public int getMentionCount() {
		int count=0;
		for(Chain c:chainMap.values())
			count+=c.spans.size();
		return count;
	}

	public List<Chain> getKey() {
		List<Chain> key=new ArrayList<Chain>(chainMap.values());
		Collections.sort(key); //Make sure we have an ordered list.
		return key;
	}

	public void assignStackMap(Document doc) {
		doc.stackMap=span2int;
	}

	public Integer getSpanChainID(Span s) {
		return span2int.get(s);
	}
	
	public Set<Span> getClonedSpanSet(){
		return new HashSet<Span>(span2int.keySet());
	}

	public int getChainSize(Integer predChainId) {
		Chain c=chainMap.get(predChainId);
		return c.spans.size();
	}
	
	public Map<Integer,Chain> getChainMap(){
		return chainMap;
	}
	
	public Map<Span,Integer> getSpan2IntMap(){
		return span2int;
	}
	
	public int pruneSingletons(){
		int count=0;
		for(Iterator<Entry<Integer,Chain>> it=chainMap.entrySet().iterator();it.hasNext();){
			Entry<Integer,Chain> e=it.next();
			Chain c=e.getValue();
			if(c.spans.size()==1){
				Span sp=c.spans.get(0);
				span2int.remove(sp);
				it.remove();
				count++;
			}
		}
		return count;
	}
	
	public IntPair pruneVerbs(){
		int prunedChains=0;
		int prunedMentions=0;
		for(Iterator<Entry<Integer,Chain>> it=chainMap.entrySet().iterator();it.hasNext();){
			Entry<Integer,Chain> e=it.next();
			Chain c=e.getValue();
			for(Iterator<Span> its=c.spans.iterator();its.hasNext();){
				Span s=its.next();
				if(s.s.tags[s.hd].startsWith("V")){
					its.remove();
					span2int.remove(s);
					prunedMentions++;
				}
			}
			if(c.spans.size()<2){
				prunedChains++;
				it.remove();
				for(Span s:c.spans){
					span2int.remove(s);
					prunedMentions++;
				}
			}
		}
		return new IntPair(prunedChains,prunedMentions);
	}
	
}
