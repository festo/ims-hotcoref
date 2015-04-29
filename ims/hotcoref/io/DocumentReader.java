package ims.hotcoref.io;

import ims.hotcoref.data.Document;

public interface DocumentReader extends Iterable<Document>{

	static final String ROOT_FORM="<root-form>";
	static final String ROOT_LEMMA="<root-lemma>";
	static final String ROOT_TAG="<root-tag>";
	static final String ROOT_CFG="<root-cfg-col>";
	static final String ROOT_NE="<root-ne>";
	static final String ROOT_SPEAKER="<root-speaker>";
	static final String ROOT_COREF="<coref-null>";
	static final int ROOT_HEAD=-1;
	static final String ROOT_LBL="<root-lbl>";
	static final String[] ROOT_FEATS={"<root-feat>"};
}
