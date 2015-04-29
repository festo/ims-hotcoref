package ims.hotcoref.features.extractors;

import ims.hotcoref.symbols.SymbolTable.Types;

public enum TokenTrait {
	Form,Pos, Lemma, BWUV,Fun,
	FFChar,FLChar,FF2Char,FL2Char;
	
	public Types getType(){
		switch(this){
		case Form:		return Types.Form;
		case Pos:		return Types.Pos;
		case Fun:		return Types.Deprel;
		case Lemma:		return Types.Lemma;
		case BWUV:		return Types.BWUV;
		case FFChar:	
		case FLChar:	return Types.Char;
		case FF2Char:
		case FL2Char:	return Types.CharBigram;
		}
		throw new RuntimeException("not implemented");
	}

	public Types getWSType() {
		switch(this){
		case Form:		return Types.WSForm;
		case Pos:		return Types.WSPos;
		case Lemma:		return Types.WSLemma;
		default: 	throw new RuntimeException("not implenmented: "+this.toString());
		}
	}

	public Types getESType() {
		switch(this){
		case Form:		return Types.ESForm;
		case Pos:		return Types.ESPos;
		case Lemma:		return Types.ESLemma;
		case BWUV:		return Types.ESBWUV;
		default: throw new RuntimeException("not imple,ented "+this.toString());
		}
	}

	public Types getESTType(){
		switch(this){
		case Form:		return Types.ESTForm;
		case BWUV:		return Types.ESTBWUV;
		case Lemma:		return Types.ESTLemma;
		default: throw new RuntimeException("not implemented"+this.toString());
		}
	}
}
