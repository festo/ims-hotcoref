package ims.hotcoref.io;

import java.io.File;
import java.io.IOException;

import ims.hotcoref.data.Sentence;

public class C12Writer extends AbstractWriter {

	public C12Writer(File out) throws IOException {
		super(out);
	}

	@Override
	String sentenceToString(Sentence s) {
		StringBuilder sb=new StringBuilder();
		for(int i=1;i<s.forms.length;++i){
			//doc name, no, token index
			sb.append(s.d.docName).append('\t').append(s.d.docNo).append('\t').append(Integer.toString(i)).append('\t');
			//form, tag, cfg
			sb.append(s.wholeForm[i]).append('\t').append(s.tags[i]).append('\t').append(s.cfgCol[i]).append('\t');
			//sense/srl stuff -- we blank them
			sb.append('-').append('\t').append('-').append('\t').append('-').append('\t');
			//Speaker, NE
			sb.append(s.speaker[i]).append('\t').append(s.neCol[i]).append('\t');
			//We have no SRL stuff for now...
			//So only coref remaining
			sb.append(s.corefCol[i]).append('\n');
		}
		return sb.toString();
	}

}
