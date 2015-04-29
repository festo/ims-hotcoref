package ims.hotcoref.features.extractors;

import ims.hotcoref.data.Span;

import java.io.Serializable;

public class SpanTokenExtractor implements Serializable{
	private static final long serialVersionUID = 1L;

	public final SpanToken st;
	
	public SpanTokenExtractor(SpanToken st){
		this.st=st;
	}
	
	public int getToken(Span s){
		switch(st){
		case Hd:	return s.hd;
		case HdGov: return s.hdgov;
		case HdLmc: return s.hdlmc;
		case HdRmc: return s.hdrmc;
		case HdLs:  return s.hdls;
		case HdRs:  return s.hdrs;
		case HdP:   return s.hd-1;
		case HdPP:  return s.hd-2;
		case HdN:   return s.hd+1;
		case HdNN:  return s.hd+2;
		case HdIP:  return s.hd-1>=s.start?s.hd-1:-1;
		case HdIPP: return s.hd-2>=s.start?s.hd-2:-1;
		case HdIN:  return s.hd+1<=s.end?s.hd+1:-1;
		case HdINN: return s.hd+2<=s.end?s.hd+2:-1;
		case SF:    return s.start<s.hd?s.start:-1;
		case SL:    return s.end>s.hd?s.end:-1;
		case SPr:	return s.start-1;
		case SFo:	return s.end<s.s.forms.length?s.end+1:-1;
		default: throw new Error("!");
		}
	}
}
