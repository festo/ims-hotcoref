package ims.hotcoref.decoder;

public class BeamExpansionMinHeap {

	private final BeamExpansion[] arr; 
	private int cur=0;
	
	public BeamExpansionMinHeap(int size){
		arr=new BeamExpansion[size];
	}
	
	public void insert(BeamExpansion t){
		if(cur==arr.length){
			popAndInsert(t);
		} else {
			arr[cur++]=t;
			if(cur==arr.length)
				heapify();
		}
	}

	private void heapify(){
		for(int i=parent(cur-1);i>=0;--i)
			heapify(i);
	}
	
	private void heapify(int i) {
		int l=left(i);
		int r=right(i);
		int smallest=i;
		if(l<cur && ((arr[l].score<arr[i].score) || (arr[l].score==arr[i].score && arr[l].edgeLen>arr[i].edgeLen)))
			smallest=l;
		if(r<cur && (arr[r].score<arr[smallest].score ||(arr[r].score==arr[smallest].score && arr[r].edgeLen>arr[smallest].edgeLen)))
			smallest=r;
		if(smallest!=i){
			swap(i,smallest);
			heapify(smallest);
		}
	}

	private void popAndInsert(BeamExpansion t) {
		arr[0]=t;
		heapify(0);
	}
	
	public boolean offer(double d,int edgeLen){
		if(cur==arr.length){
			if(d>arr[0].score || (d==arr[0].score && edgeLen<arr[0].edgeLen))
				return true;
			else
				return false;
		} else {
			return true;
		}
	}
	
	public BeamExpansion[] emptyReverse(){
		BeamExpansion[] r=new BeamExpansion[cur];
		if(cur!=arr.length)
			heapify();
		for(int i=cur-1;i>=0;--i)
			r[i]=pop();
		return r;
	}
	
	public BeamExpansion pop(){
		BeamExpansion ret=arr[0];
		arr[0]=arr[--cur];
		heapify(0);
		return ret;
	}
	
	private static int left(int i)		{	return 2*i+1;}
	private static int right(int i)		{	return 2*i+2;}
	private static int parent(int i)	{	return (i-1)/2;}
	private void swap(int a,int b){
		BeamExpansion tmp=arr[a];
		arr[a]=arr[b];
		arr[b]=tmp;
	}

	public int size() {
		return cur;
	}
	
}
