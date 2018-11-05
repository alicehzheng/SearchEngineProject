import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *  Added on 11/04/18 by @alichehzheng
 */

/**
 *  This class is used to implement learnng to rank operations
 */
public class LeToR {
	// LeToR parameter
	double c = 0.001;
	
	String trainingQueryFile, trainingQrelsFile, trainingFeatureVectorsFile;
	String svmRankLearnPath, svmRankClassifyPath;
	String svmRankModelFile, testingFeatureVectorsFile, testingDocumentScores;
	
	ArrayList<Integer> disabledFeatures;
	
	// BM25 parameters
    double k1, k3, b; 
	// Indri parameters
	int mu;
    double lambda;
    
    
    LeToR(double c, double k1, double k3, double b, int mu, double lambda, 
    		String trainingQueryFile,String trainingQrelsFile, String trainingFeatureVectorsFile,
    		String svmRankLearnPath, String svmRankClassifyPath,
    		String svmRankModelFile, String testingFeatureVectorsFile, String testingDocumentScores
    		){
    	this.c = c;
    	this.k1 = k1;
    	this.k3 = k3;
    	this.b = b;
    	this.mu = mu;
    	this.lambda = lambda;
    	this.trainingQueryFile = trainingQueryFile;
    	this.trainingQrelsFile = trainingQrelsFile;
    	this.trainingFeatureVectorsFile = trainingFeatureVectorsFile;
    	this.svmRankLearnPath = svmRankLearnPath;
    	this.svmRankClassifyPath = svmRankClassifyPath;
    	this.svmRankModelFile = svmRankModelFile;
    	this.testingFeatureVectorsFile = testingFeatureVectorsFile;
    	this.testingDocumentScores = testingDocumentScores;
    	this.disabledFeatures = new ArrayList<Integer>();
    	this.disabledFeatures.add(-1);
    }
    
    LeToR(double c, double k1, double k3, double b, int mu, double lambda, 
    		String trainingQueryFile,String trainingQrelsFile, String trainingFeatureVectorsFile,
    		String svmRankLearnPath, String svmRankClassifyPath,
    		String svmRankModelFile, String testingFeatureVectorsFile, String testingDocumentScores,
    		String disabledFeaturesStr
    		){
    	this.c = c;
    	this.k1 = k1;
    	this.k3 = k3;
    	this.b = b;
    	this.mu = mu;
    	this.lambda = lambda;
    	this.trainingQueryFile = trainingQueryFile;
    	this.trainingQrelsFile = trainingQrelsFile;
    	this.trainingFeatureVectorsFile = trainingFeatureVectorsFile;
    	this.svmRankLearnPath = svmRankLearnPath;
    	this.svmRankClassifyPath = svmRankClassifyPath;
    	this.svmRankModelFile = svmRankModelFile;
    	this.testingFeatureVectorsFile = testingFeatureVectorsFile;
    	this.testingDocumentScores = testingDocumentScores;
    	this.disabledFeatures = new ArrayList<Integer>();
    	String[] parsed = disabledFeaturesStr.split(",");
    	for (String fstr: parsed){
    		this.disabledFeatures.add(Integer.parseInt(fstr));
    	}
    
    }
	
    public void train() throws IOException{
    	this.generateFeatureVectors(trainingQueryFile, trainingFeatureVectorsFile, trainingQrelsFile);
    }
    
    public void generateFeatureVectors(String queryFilePath, String outputFilePath, String relevanceFilePath)
    throws IOException{
    	BufferedReader queryInput = null;
    	BufferedReader relevanceInput = null;
    	BufferedWriter output = null;
    	try{
    		queryInput = new BufferedReader(new FileReader(queryFilePath));
    		relevanceInput = new BufferedReader(new FileReader(relevanceFilePath));
    		output = new BufferedWriter(new FileWriter(outputFilePath));
    		
    		String qLine = null;
    		Map<String, String[]> query2terms = new HashMap<String,String[] > ();
    		while ((qLine = queryInput.readLine()) != null) {
    	        int d = qLine.indexOf(':');

    	        if (d < 0) {
    	          throw new IllegalArgumentException
    	            ("Syntax error:  Missing ':' in query line.");
    	        }

    	        String qid = qLine.substring(0, d);
    	        String query = qLine.substring(d + 1);
    	        String[] queryterms = (query.trim()).split(" ");
    	        query2terms.put(qid, queryterms);
    	        System.out.println("Original Query " + qLine);
    	        //System.out.println(qid);
    	        /**
    	        for(int i = 0; i < queryterms.length; ++i)
    	        	System.out.println(queryterms[i]); 
    	        	**/
    		}
    		
    		String relLine = null;
    		
    		String prevQid = null;
    		String curQid = null;
    		
    		// key: external doc id 
    		// values: relScore, feature1, feature2, ..., feature18
    		Map<String, String[]> doc2features = new HashMap<String,String[] > ();
    		while((relLine = relevanceInput.readLine()) != null){
    			String[] parsed = (relLine.trim()).split(" ");
    			
    			curQid = parsed[0].trim();
    			// all features for the previous query has been generated
    		    // now we need to normalize them and write them to output file
    			if(query2terms.containsKey(prevQid)){
    				for(int fId = 1; fId <= 18; fId++){
    					if(disabledFeatures.indexOf(fId) >= 0)
    						continue;
    					double minVal = 0.0, maxVal = 0.0;
    					boolean first = true;
    					
    					for(String doc: doc2features.keySet()){
    						String fVal = doc2features.get(doc)[fId];
    						//System.out.println("doc" + doc + "feature" + String.valueOf(fId) + " value:" + fVal);
    						if(fVal.equals("NA")){
    							//System.out.println("NA");
    							continue;
    						}
    						double dfVal = Double.parseDouble(fVal);
    						if(first == true){
    							minVal = dfVal;
    							maxVal = dfVal;
    							first = false;
    							//System.out.println("flag : " + minVal);
    							continue;
    						}
    						
    						if(dfVal < minVal)
    							minVal = dfVal;
    						else if(dfVal > maxVal)
    							maxVal = dfVal;
    					}
    					
    					//System.out.println("min val: " + minVal);
    					//System.out.println("max val: " + maxVal);
    					if(first == true || minVal == maxVal){
    						for(String doc: doc2features.keySet()){
        						doc2features.get(doc)[fId] = "0";
    						}
    					}
    					else{
    						
    						for(String doc: doc2features.keySet()){
    							String fVal = doc2features.get(doc)[fId];
    							if(fVal.equals("NA"))
    						        doc2features.get(doc)[fId] = "0";
    							else{
    								double dfVal = Double.parseDouble(fVal);
    								doc2features.get(doc)[fId] = String.valueOf((dfVal - minVal) / (maxVal - minVal));
    							}
    						}	
    					}
    				}
    				
    				for(String doc: doc2features.keySet()){
    					String[] relandfeatures = doc2features.get(doc);
    					String outputMsg = relandfeatures[0] + " qid:" + prevQid;
    					for(int fId = 1; fId <= 18; fId++){
        					if(disabledFeatures.indexOf(fId) >= 0)
        						continue;
        					outputMsg += " " + String.valueOf(fId) + ":" + relandfeatures[fId];
    					}
    					outputMsg += " # " + doc + "\n";
    					//System.out.print(outputMsg);
    					output.write(outputMsg);
    				}
    				doc2features = new HashMap<String,String[] > ();
    			}
    			if(!query2terms.containsKey(curQid)){
    				prevQid = curQid;
    				continue;
    			}
    			String[] terms = query2terms.get(curQid);
    			String externalDocId = parsed[2].trim();
    			int docId;
    			try{
    				docId = Idx.getInternalDocid(externalDocId);
    			}
    			catch (Exception ex){
    				System.out.println(externalDocId + "is not in the index");
    				continue;
    			}
    			
    			String[] relandFeatures = new String[19];
    			relandFeatures[0] = parsed[3].trim(); // relevance score
    			
    			if(disabledFeatures.indexOf(1) < 0)
    				relandFeatures[1] = f1(docId);
    			if(disabledFeatures.indexOf(2) < 0)
    				relandFeatures[2] = f2(docId);
    			if(disabledFeatures.indexOf(3) < 0)
    				relandFeatures[3] = f3(docId);
    			if(disabledFeatures.indexOf(4) < 0)
    				relandFeatures[4] = f4(docId);
    			
    			if(disabledFeatures.indexOf(5) < 0)
    				relandFeatures[5] = f5(terms,docId);
    			if(disabledFeatures.indexOf(6) < 0)
    				relandFeatures[6] = f6(terms,docId);
    			if(disabledFeatures.indexOf(7) < 0)
    				relandFeatures[7] = f7(terms,docId);
    			
    			if(disabledFeatures.indexOf(8) < 0)
    				relandFeatures[8] = f8(terms,docId);
    			if(disabledFeatures.indexOf(9) < 0)
    				relandFeatures[9] = f9(terms,docId);
    			if(disabledFeatures.indexOf(10) < 0)
    				relandFeatures[10] = f10(terms,docId);
    			
    			if(disabledFeatures.indexOf(11) < 0)
    				relandFeatures[11] = f11(terms,docId);
    			if(disabledFeatures.indexOf(12) < 0)
    				relandFeatures[12] = f12(terms,docId);
    			if(disabledFeatures.indexOf(13) < 0)
    				relandFeatures[13] = f13(terms,docId);
    			
    			if(disabledFeatures.indexOf(14) < 0)
    				relandFeatures[14] = f14(terms,docId);
    			if(disabledFeatures.indexOf(15) < 0)
    				relandFeatures[15] = f15(terms,docId);
    			if(disabledFeatures.indexOf(16) < 0)
    				relandFeatures[16] = f16(terms,docId);
    			
    			if(disabledFeatures.indexOf(17) < 0)
    				relandFeatures[17] = f17(docId);
    			if(disabledFeatures.indexOf(18) < 0)
    				relandFeatures[18] = f18(terms,docId);
    			
    			doc2features.put(externalDocId, relandFeatures);
    			
    			
    			prevQid = curQid;
    		}
    		// all features for the previous query has been generated
		    // now we need to normalize them and write them to output file
			if(query2terms.containsKey(prevQid)){
				for(int fId = 1; fId <= 18; fId++){
					if(disabledFeatures.indexOf(fId) >= 0)
						continue;
					double minVal = 0.0, maxVal = 0.0;
					boolean first = true;
					
					for(String doc: doc2features.keySet()){
						String fVal = doc2features.get(doc)[fId];
						//System.out.println("doc" + doc + "feature" + String.valueOf(fId) + " value:" + fVal);
						if(fVal.equals("NA")){
							//System.out.println("NA");
							continue;
						}
						double dfVal = Double.parseDouble(fVal);
						if(first == true){
							minVal = dfVal;
							maxVal = dfVal;
							first = false;
							//System.out.println("flag : " + minVal);
							continue;
						}
						
						if(dfVal < minVal)
							minVal = dfVal;
						else if(dfVal > maxVal)
							maxVal = dfVal;
					}
					
					//System.out.println("min val: " + minVal);
					//System.out.println("max val: " + maxVal);
					if(first == true || minVal == maxVal){
						for(String doc: doc2features.keySet()){
    						doc2features.get(doc)[fId] = "0";
						}
					}
					else{
						
						for(String doc: doc2features.keySet()){
							String fVal = doc2features.get(doc)[fId];
							if(fVal.equals("NA"))
						        doc2features.get(doc)[fId] = "0";
							else{
								double dfVal = Double.parseDouble(fVal);
								doc2features.get(doc)[fId] = String.valueOf((dfVal - minVal) / (maxVal - minVal));
							}
						}	
					}
				}
				
				for(String doc: doc2features.keySet()){
					String[] relandfeatures = doc2features.get(doc);
					String outputMsg = relandfeatures[0] + " qid:" + prevQid;
					for(int fId = 1; fId <= 18; fId++){
    					if(disabledFeatures.indexOf(fId) >= 0)
    						continue;
    					outputMsg += " " + String.valueOf(fId) + ":" + relandfeatures[fId];
					}
					outputMsg += " # " + doc + "\n";
					//System.out.print(outputMsg);
					output.write(outputMsg);
				}
				doc2features = new HashMap<String,String[] > ();
			}
    	}
    	catch (IOException ex) {
    	      ex.printStackTrace();
    	} 
    	finally {
    		if(queryInput != null)
    			queryInput.close();
    		if(relevanceInput != null)
    			relevanceInput.close();
    		if(output != null)
                output.close();
    	}
    }
    
    
    // spam score
    private String f1(int docid){
    	int spamScore;
    	try{
    	spamScore = Integer.parseInt (Idx.getAttribute ("spamScore", docid));
    	}
    	catch (IOException ex){
    		return "NA";
    	}
    	return String.valueOf(spamScore);
    }
    
    // URL depth
    private String f2(int docid){
    	String rawUrl;
    	int depths = 0;
    	try{
    		rawUrl = Idx.getAttribute ("rawUrl", docid);
    		//System.out.println(rawUrl);
    		for(int i = 0; i < rawUrl.length(); ++i)
    			if(rawUrl.charAt(i) == '/')
    				depths++;
    		//depths = (rawUrl.split("/")).length-3;
    		//depths -= 2;
    		//System.out.println(depths);
    	}
    	catch (IOException ex){
    		return "NA";
    	}
    	return String.valueOf(depths);
    }
    
    // wiki score
    private String f3(int docid){
    	String rawUrl;
    	int wiki = 0;
    	try{
    		rawUrl = Idx.getAttribute ("rawUrl", docid);
    		if(rawUrl.indexOf("wikipedia.org") >= 0)
    			wiki = 1;
    	}
    	catch (IOException ex){
    		return "NA";
    	}
    	return String.valueOf(wiki);
    }
    
    // Page Rank
    private String f4(int docid){
    	float prScore;
    	try{
    		prScore = Float.parseFloat (Idx.getAttribute ("PageRank", docid));
    	}
    	catch (IOException ex){
    		return "NA";
    	}
    	return String.valueOf(prScore);
    }
    
    // BM25 for body
    private String f5(String[] bow, int docid){
    	String bm25_body;
    	try{
    		bm25_body = bm25Score(bow, docid, "body");
    	}
    	catch (IOException ex){
    		return "NA";
    	}
    	return bm25_body;
    }
    
   // indri for body
    private String f6(String[] bow, int docid){
    	String indri_body;
    	try{
    		indri_body = indriScore(bow, docid, "body");
    	}
    	catch (IOException ex){
    		return "NA";
    	}
    	return indri_body;
    }
    
    // term overlap for body
    private String f7(String[] bow, int docid){
    	String overlap_body;
    	try{
    		overlap_body = termOverlap(bow, docid, "body");
    	}
    	catch (IOException ex){
    		return "NA";
    	}
    	return overlap_body;
    }
    
   // BM25 for title
    private String f8(String[] bow, int docid){
    	String bm25_title;
    	try{
    		bm25_title = bm25Score(bow, docid, "title");
    	}
    	catch (IOException ex){
    		return "NA";
    	}
    	return bm25_title;
    }
    
    // indri for title
    private String f9(String[] bow, int docid){
    	String indri_title;
    	try{
    		indri_title = indriScore(bow, docid, "title");
    	}
    	catch (IOException ex){
    		return "NA";
    	}
    	return indri_title;
    }
    
   // term overlap for title
    private String f10(String[] bow, int docid){
    	String overlap_title;
    	try{
    		overlap_title = termOverlap(bow, docid, "title");
    	}
    	catch (IOException ex){
    		return "NA";
    	}
    	return overlap_title;
    }
    
    // BM25 for url
    private String f11(String[] bow, int docid){
    	String bm25_url;
    	try{
    		bm25_url = bm25Score(bow, docid, "url");
    	}
    	catch (IOException ex){
    		return "NA";
    	}
    	return bm25_url;
    }
    
    // indri for url
    private String f12(String[] bow, int docid){
    	String indri_url;
    	try{
    		indri_url = indriScore(bow, docid, "url");
    	}
    	catch (IOException ex){
    		return "NA";
    	}
    	return indri_url;
    }
    
    // term overlap for url
    private String f13(String[] bow, int docid){
    	String overlap_url;
    	try{
    		overlap_url = termOverlap(bow, docid, "url");
    	}
    	catch (IOException ex){
    		return "NA";
    	}
    	return overlap_url;
    }
    // BM25 for inlink
    private String f14(String[] bow, int docid){
    	String bm25_inlink;
    	try{
    		bm25_inlink = bm25Score(bow, docid, "inlink");
    	}
    	catch (IOException ex){
    		return "NA";
    	}
    	return bm25_inlink;
    }
    
    // indri for inlink
    private String f15(String[] bow, int docid){
    	String indri_inlink;
    	try{
    		indri_inlink = indriScore(bow, docid, "inlink");
    	}
    	catch (IOException ex){
    		return "NA";
    	}
    	return indri_inlink;
    }
    
    // term overlap for inlink
    private String f16(String[] bow, int docid){
    	String overlap_inlink;
    	try{
    		overlap_inlink = termOverlap(bow, docid, "inlink");
    	}
    	catch (IOException ex){
    		return "NA";
    	}
    	return overlap_inlink;
    }
    
    // content length for body
    private String f17(int docid){
    	int content_len = 0;
    	try{
    		content_len = Idx.getFieldLength("body",docid);
    	}
    	catch (IOException ex){
    		return "NA";
    	}
    	return String.valueOf(content_len);
    }
    
    // query term density for body
    private String f18(String[] bow, int docid){
    	TermVector termvec;
    	try{
    		termvec = new TermVector(docid, "body");
    		if(termvec.positionsLength() == 0)
    			return "NA";
    	}
    	catch (IOException ex){
    		return "NA";
    	}
          	
        int totalTermFreq = 0;
        int len = bow.length;
        for(int i = 0; i < len; ++i){
        	String term = bow[i];        	
        	int termIdx = termvec.indexOfStem(term);
        	if(termIdx >= 0)
        		totalTermFreq += termvec.stemFreq(termIdx);	
        }
        double density = ((double) totalTermFreq) / termvec.positionsLength();
        return String.valueOf(density);
    	
    }
    private String bm25Score(String[] bow, int docid, String field) throws IOException{
    	
    	TermVector termvec = new TermVector(docid, field);
        if(termvec.positionsLength() == 0)
        	return "NA";
        
    	double k1 = this.k1;
    	double b = this.b;
    	
        double score = 0.0;
        int len = bow.length;
        for(int i = 0; i < len; ++i){
        	String term = bow[i];
        	
        	double idfWeight = 0.0;
        	double tfWeight = 0.0;
        	int termIdx = termvec.indexOfStem(term);
        	if(termIdx < 0)
        		continue;
        	
        	long N = Idx.getNumDocs(); // total number of documents
        	
        	int df = termvec.stemDf(termIdx); // document frequency of this term
        	double avg_len = Idx.getSumOfFieldLengths(field) / (float) Idx.getDocCount (field); // average document length of specified field
        	int doc_len = termvec.positionsLength(); // document length of matched document in specified field
        	int tf = termvec.stemFreq(termIdx); // term frequency
        	idfWeight = Math.max(0.0, Math.log((N - df + 0.5) / (double) (df + 0.5)));
        	tfWeight = ((double)tf) / (tf + k1 * ((1-b) + b * ((double)doc_len) / avg_len));
        	score += idfWeight * tfWeight;
        	
        }
        
        return String.valueOf(score);
    	
    }
    
    private String indriScore(String[] bow, int docid, String field) throws IOException{
    	
    	TermVector termvec = new TermVector(docid, field);
        if(termvec.positionsLength() == 0)
        	return "NA";
        
    	int mu = this.mu;
        double lambda = this.lambda;
        
    	double score = 1.0;
    	 
        int len = bow.length;
        double exp = 1 / (double)len;
        boolean hasMatch = false;
        
        for(int i = 0; i < len; ++i){
        	String term = bow[i];
        	int termIdx = termvec.indexOfStem(term);
        	int tf = 0;
        	if(termIdx >= 0)
        		tf = termvec.stemFreq(termIdx); // term frequency
        	if(tf > 0)
        		hasMatch = true;
        	InvList invertedlist = new InvList(term, field);
            double ctf = invertedlist.ctf; // collection term frequency associated with term
            long c_len = Idx.getSumOfFieldLengths(field); // total length of collection in a specified field
            int doc_len = termvec.positionsLength(); // document length of doc docid in specified field
            double MLE = ctf / c_len;
            
            double cur_score = (1 - lambda) * ((double)(tf + mu * MLE) / (double)(doc_len + mu)) + lambda * MLE;
            score = score * Math.pow(cur_score, exp);
        }
        
        if(!hasMatch)
        	score = 0.0;
    	return String.valueOf(score);
    }
    
    private String termOverlap(String[] bow, int docid, String field) throws IOException{
    	
    	TermVector termvec = new TermVector(docid, field);
        if(termvec.positionsLength() == 0)
        	return "NA";
          	
        int matchingCnt = 0;
        int len = bow.length;
        for(int i = 0; i < len; ++i){
        	String term = bow[i];        	
        	int termIdx = termvec.indexOfStem(term);
        	if(termIdx >= 0)
        		matchingCnt++;  	
        }
        double percentage = ((double) matchingCnt) / len;
        return String.valueOf(percentage); 	
    }
    
	
}
