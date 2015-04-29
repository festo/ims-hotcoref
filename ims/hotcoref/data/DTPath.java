package ims.hotcoref.data;


public class DTPath {

	final public Span ana;
	final public Span ant;	
	final public Link[] links;
	public int lcaIndex=-1;
	
	public DTPath(Span antecedent,Span anaphor){
		this.ana=anaphor;
		this.ant=antecedent;
		if(antecedent.s==anaphor.s){
			links=getSSPath(antecedent,anaphor);
		} else {
			links=getDSPath(antecedent,anaphor);
		}
	}
	
	public boolean sameSentence(){
		return ana.s==ant.s;
	}

	private Link[] getDSPath(Span antecedent, Span anaphor) {
		int[] anaSenHeads=anaphor.s.dt.heads;
		int[] antSenHeads=antecedent.s.dt.heads;
		int anaphorLinksToRoot=0;
		for(int i=anaphor.hd;i!=-1;i=anaSenHeads[i])
			anaphorLinksToRoot++;
		int antecedentLinksToRoot=0;
		for(int i=antecedent.hd;i!=-1;i=antSenHeads[i])
			antecedentLinksToRoot++;
		
		Link[] links=new Link[antecedentLinksToRoot+anaphorLinksToRoot];
		int q=0;
		for(int i=anaphor.hd;i!=-1;i=anaSenHeads[i])
			links[q++]=new Link(true,anaphor.s.dt.lbls[i],anaSenHeads[i],anaphor.s);
		int p=links.length-1;
		for(int i=antecedent.hd;i!=-1;i=antSenHeads[i])
			links[p--]=new Link(false,antecedent.s.dt.lbls[i],i,antecedent.s);
		
		//Sanity checks:
		if(p!=q-1)
			throw new Error("error here, look into this");
		
		for(Link l:links)
			if(l==null)
				throw new Error("error here, look into thi2s");
		
		return links;
	}

	private Link[] getSSPath(Span antecedent, Span anaphor) {
		int[] heads=anaphor.s.dt.heads;
		String[] lbls=anaphor.s.dt.lbls;
		boolean isChild[][]=anaphor.s.dt.isChild;
		final Link[] links;
		if(isChild[antecedent.hd][anaphor.hd]){ //Antecedent dominates anaphor
			int cnt=0;
			for(int r=anaphor.hd;r!=antecedent.hd;r=heads[r])
				cnt++;
			links=new Link[cnt];
			cnt=0;
			for(int r=anaphor.hd;r!=antecedent.hd;r=heads[r])
				links[cnt++]=new Link(true,lbls[r],heads[r],anaphor.s);
		} else if(isChild[anaphor.hd][antecedent.hd]){ //Anaphor dominates antecedent
			int cnt=0;
			for(int r=antecedent.hd;r!=anaphor.hd;r=heads[r])
				cnt++;
			links=new Link[cnt];
			cnt--;
			for(int r=antecedent.hd;r!=anaphor.hd;r=heads[r])
				links[cnt--]=new Link(false,lbls[r],r,anaphor.s);
		} else { //Find LCA
			int lca=anaphor.hd;
			int anaphorDistToLCA=0;
			while(!isChild[lca][antecedent.hd]){
				anaphorDistToLCA++;
				lca=heads[lca];
			}
			lcaIndex=lca;
			int antecedentDistToLCA=0;
			for(int i=antecedent.hd;i!=lca;i=heads[i])
				antecedentDistToLCA++;
			links=new Link[anaphorDistToLCA+antecedentDistToLCA];
			int q=0;
			for(int i=anaphor.hd;i!=lca;i=heads[i])
				links[q++]=new Link(true,lbls[i],heads[i],anaphor.s);
			int p=links.length-1;
			for(int i=antecedent.hd;i!=lca;i=heads[i])
				links[p--]=new Link(false,lbls[i],i,anaphor.s);
			//At this point p should point to the last added anaphor (up) link,
			//and q should point to the last added antecedent (down) link.
			//We make a sanity check, but this code can be removed if we have
			//never seen the exception in the future:
			if(p!=q-1)
				throw new Error("Path computation failed, look into this.");
		}
		//We also add an additional sanity check to make sure nothing is null.
		//Again, this should be possible to delete if we never encounter this exception:
		for(Link l:links)
			if(l==null)
				throw new Error("Empty links in path, look into this");
		return links;
	}

	
	public class Link {
		public final boolean up;
		public final String lbl;
		public final int toIndex;
		public final Sentence toSentence;
		public Link(boolean up,String lbl,int toIndex,Sentence toSentence){
			this.up=up;this.lbl=lbl;this.toIndex=toIndex;this.toSentence=toSentence;
		}
	}
}
