/** 
 *  Created on 10/20/18 by @alicehzheng
 */

/**
 *  This class is used to create objects that provide fast access to 
 *  query expansion model parameters.
 */

public class QryExpansionModel {
	
	int fbDocs, fbTerms, fbMu;
	double fbOrigWeight;
	String fbInitialRankingFile, fbExpansionQueryFile;
	
	QryExpansionModel(int fbdocs, int fbterms, int fbmu, double fborigweight){
		this.fbDocs = fbdocs;
		this.fbTerms = fbterms;
		this.fbMu = fbmu;
		this.fbOrigWeight = fborigweight;
		this.fbInitialRankingFile = null;
		this.fbExpansionQueryFile = null;
	}
	
	void addRankingFile(String rankingFile ){
		this.fbInitialRankingFile = rankingFile;
	}
	
	void addQueryFile(String queryFile){
		this.fbExpansionQueryFile = queryFile;
	}

}


