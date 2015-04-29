package ims.util;

public class EditDistance {

	public static int[][] levenshteinDistanceTableIntArr(int[] a,int ai,int am,int[] b,int bi,int bm){
		int[][] d=new int[2+am-ai][2+bm-bi];
		for(int i=1;i<d.length;++i)
			d[i][0]=i;
		for(int j=1;j<d[0].length;++j)
			d[0][j]=j;
		for(int y=0;bi<=bm;++bi,++y)
			for(int x=0,q=ai;q<=am;++q,++x)
				d[x+1][y+1]=(a[q]==b[bi]?d[x][y]:min(d[x][y+1],d[x+1][y],d[x][y])+1);
		return d;
	}
	
	public static int[][] levenshteinDistanceTableStringArrays(String[] a,int ai,int am,String[] b,int bi,int bm){
		int[][] d=new int[2+am-ai][2+bm-bi];
		for(int i=1;i<d.length;++i)
			d[i][0]=i;
		for(int j=1;j<d[0].length;++j)
			d[0][j]=j;
		for(int y=0;bi<=bm;++bi,++y)
			for(int x=0,q=ai;q<=am;++q,++x)
				d[x+1][y+1]=a[q].equals(b[bi])?d[x][y]:min(d[x][y+1],d[x+1][y],d[x][y])+1;
		return d;
	}
	
	
	public static int[][] levenshteinDistanceTable(String s1,String s2){
		s1=s1.toLowerCase();
		s2=s2.toLowerCase();
		int[][] d=new int[s1.length()+1][s2.length()+1];
		for(int i=1;i<d.length;++i)
			d[i][0]=i;
		for(int j=1;j<d[0].length;++j)
			d[0][j]=j;
		for(int j=0;j<s2.length();++j){
			for(int i=0;i<s1.length();++i){
				if(s1.charAt(i)==s2.charAt(j))
					d[i+1][j+1]=d[i][j];
				else
					d[i+1][j+1]=min(d[i][j+1],d[i+1][j],d[i][j])+1;
			}
		}
		return d;		
	}
	
	public static int levenshteinDistanceIntArr(int[] a,int ai,int am,int[] b,int bi,int bm){
		int[][] d=levenshteinDistanceTableIntArr(a,ai,am,b,bi,bm);
		return d[am-ai+1][bm-bi+1];
	}
	
	public static int levenshteinDistanceStringArr(String[] a,int ai,int am,String[] b,int bi,int bm){
		int[][] d=levenshteinDistanceTableStringArrays(a,ai,am,b,bi,bm);
		return d[am-ai+1][bm-bi+1];
	}
	
	public static int levenshteinDistance(String s1,String s2){
		int d[][]=levenshteinDistanceTable(s1,s2);
		return d[s1.length()][s2.length()];
	}
	
	public static String editScriptStringArr(String[] a,int ai,int am,String[] b,int bi,int bm){
		StringBuilder sb=new StringBuilder();
		int[][] d=levenshteinDistanceTableStringArrays(a,ai,am,b,bi,bm);
		int n=d.length;
		int m=d[0].length;
		int x=n-1;
		int y=m-1;
		while(true){
			if(d[x][y]==0)
				break;
			if(y>0 && x>0 && d[x-1][y-1]<d[x][y]){
				sb.append('R').append(Integer.toString(x-1)).append(a[am]).append('-').append(b[bm]);
				--x;--y;
				--am;--bm;
				continue;
			}
			if(y>0 && d[x][y-1]<d[x][y]){
				sb.append('I').append(Integer.toString(x)).append(b[bm]);
				--y;
				--bm;
				continue;
			}
			if(x>0 && d[x-1][y]<d[x][y]){
				sb.append('D').append(Integer.toString(x-1)).append(a[am]);
				--x;
				--am;
				continue;
			}
			if (x>0&& y>0 && d[x-1][y-1]==d[x][y]) {
				x--; y--; --am; --bm;
				continue ;
			}
			if (x>0&&  d[x-1][y]==d[x][y]) {
				x--; --am;
				continue;
			}
			if (y>0 && d[x][y-1]==d[x][y]) {
				y--; --bm;
				continue;
			}
		}
		return sb.toString();
	}
	
	public static String editScript(String s1,String s2){
		StringBuilder sb=new StringBuilder();
		int[][] d=levenshteinDistanceTable(s1,s2);
		int n=d.length;
		int m=d[0].length;
		
		int x=n-1;
		int y=m-1;
		while(true){
			if(d[x][y]==0)
				break;
			if(y>0 && x>0 && d[x-1][y-1]<d[x][y]){
				sb.append('R').append(Integer.toString((x-1))).append(s1.charAt(x-1)).append(s2.charAt(y-1));
				--x;
				--y;
				continue;
			}
			if(y>0 && d[x][y-1]<d[x][y]){
				sb.append('I').append(Integer.toString(x)).append(s2.charAt(y-1));
				--y;
				continue;
			}
			if(x>0 && d[x-1][y]<d[x][y]){
				sb.append('D').append(Integer.toString((x-1))).append(s1.charAt(x-1));
				--x;
			}
			if (x>0&& y>0 && d[x-1][y-1]==d[x][y]) {
				x--; y--;
				continue ;
			}
			if (x>0&&  d[x-1][y]==d[x][y]) {
				x--; 
				continue;
			}
			if (y>0 && d[x][y-1]==d[x][y]) {
				y--;
				continue;
			}
		}
		if(sb.length()==0)
			return "0";
		else
			return sb.toString();
	}
	
	private static int min(int a, int b, int c){
		return Math.min(a, Math.min(b, c));
	}
	
	public static void main(String[] args){
		String[][] test={{"sitting","kitten"}, //should be 3
						{"fluff","floof"},     //should be 2
						{"eat","eats"},        //should be 1
						{"sunday","saturday"}, //should be 3
						{"Monday","TuesDay"},
						{"monday","sunday"},
						{"Monday","Sunday"}};
		for(String[] pair:test){
			int d=levenshteinDistance(pair[0],pair[1]);
			System.out.println(pair[0]+"\t"+pair[1]+"\t"+d);
		}
		System.out.println();
		String[][] esTest={{"Mondo","Monda"},
						   {"Mondo","Nondo"},
						   {"Fluff","Floef"},
						   {"Fluff","Floaaaef"},
						   {"AABB","BBBB"},
						   {"BBAA","BBBB"},
						   {"AAA","A  AAA"},
						   {"AAAAAA","A"},
						   {"SAME","SAME"}};
		for(String[][] q:new String[][][]{esTest,test}){
			for(String[] e:q){
				String s=editScript(e[0],e[1]);
				System.out.println(e[0]+'\t'+e[1]+'\t'+s);
			}
			System.out.println();
		}
		
		System.out.println();
		System.out.println("Test on string arrays");
		String[][][] strArrTest={{{"I","like","chocolate"},{"Yes",",","I","hate","chocolate","too"}}};
		for(String[][] q:strArrTest){
			String s1=join(q[0]);
			String s2=join(q[1]);
			int dist=levenshteinDistanceStringArr(q[0],0,q[0].length-1,q[1],0,q[1].length-1);
			String es=editScriptStringArr(q[0],0,q[0].length-1,q[1],0,q[1].length-1);
			System.out.println(s1 + " -- " + s2 + " -- "+dist + " -- " + es);
		}
	}
	
	private static String join(String[] a){
		if(a.length==0)
			return "";
		StringBuilder sb=new StringBuilder(a[0]);
		for(int i=1;i<a.length;++i)
			sb.append(' ').append(a[i]);
		return sb.toString();
	}
}
