package ims.hotcoref.markables;

import java.io.Serializable;

public interface ITokenNonReferentialClassifier extends Serializable {

	public void extractTrainingInstances(String[] forms,String[] pos,String[] corefCol,String genre);
	public void train();
	public boolean[] classifiy(String[] forms,String[] pos,String genre);
	
}
