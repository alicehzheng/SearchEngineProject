/*
 *  Copyright (c) 2018 Carnegie Mellon University.  All Rights Reserved.
 *  Version 3.3.3.
 */
import java.io.*;
import java.util.*;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import javafx.util.Pair;

/**
 *  This software illustrates the architecture for the portion of a
 *  search engine that evaluates queries.  It is a guide for class
 *  homework assignments, so it emphasizes simplicity over efficiency.
 *  It implements an unranked Boolean retrieval model, however it is
 *  easily extended to other retrieval models.  For more information,
 *  see the ReadMe.txt file.
 */
public class QryEval {

  //  --------------- Constants and variables ---------------------

  private static final String USAGE =
    "Usage:  java QryEval paramFile\n\n";

  private static final String[] TEXT_FIELDS =
    { "body", "title", "url", "inlink" };


  //  --------------- Methods ---------------------------------------

  /**
   *  @param args The only argument is the parameter file name.
   *  @throws Exception Error accessing the Lucene index.
   */
  public static void main(String[] args) throws Exception {

    //  This is a timer that you may find useful.  It is used here to
    //  time how long the entire program takes, but you can move it
    //  around to time specific parts of your code.
    
    Timer timer = new Timer();
    timer.start ();

    //  Check that a parameter file is included, and that the required
    //  parameters are present.  Just store the parameters.  They get
    //  processed later during initialization of different system
    //  components.

    if (args.length < 1) {
      throw new IllegalArgumentException (USAGE);
    }

    Map<String, String> parameters = readParameterFile (args[0]);

    //  Open the index and initialize the retrieval model.

    Idx.open (parameters.get ("indexPath"));
    
    // Added on 11/04/18 by @alicehzheng: taking LeToR into consideration (training)
    boolean needLeToR = (parameters.get ("retrievalAlgorithm").toLowerCase()).equals("letor");
    LeToR letor = null;
    if(needLeToR){
    	System.out.println("Learning To Rank");
    	letor = initializeLeToR(parameters);
    	letor.train();
    }
    	
    	
  
    RetrievalModel model = initializeRetrievalModel (parameters);
    
    QryExpansionModel queryExpansion = initializeQryExpansionModel(parameters);
    
    DiversityModel diversitymodel = initializeDiversityModel(parameters);
    
    // Modified on 09/16/18 by @alicehzheng: added two more parameters
    // Modified on 11/05/18 by @alicehzheng: taking LeToR into consideration (reranking)
    // Modified on 11/19/18 by @alicehzheng: taking diversity into consideration
    //  Perform experiments.
    
    processQueryFile(parameters.get("queryFilePath"), parameters.get("trecEvalOutputPath"), Integer.parseInt(parameters.get("trecEvalOutputLength")),model,queryExpansion, letor);

    
   
    
    //  Clean up.
    
    timer.stop ();
    System.out.println ("Time:  " + timer);
  }
  
  private static LeToR initializeLeToR (Map<String, String> parameters) throws IOException{
	  if(!(parameters.containsKey ("BM25:k_1") && parameters.containsKey ("BM25:k_3") && parameters.containsKey ("BM25:b")))
	      throw new IllegalArgumentException("Missing Parameters for BM25 in LeToR");
	  double k1 = Double.valueOf(parameters.get("BM25:k_1"));
	  double k3 = Double.valueOf(parameters.get("BM25:k_3"));
	  double b =  Double.valueOf(parameters.get("BM25:b"));
	  if(k1 < 0 || k3 < 0 || b < 0 || b > 1 )
	      throw new IllegalArgumentException("Illegal Parameters for BM25 Retrieval Model ");
	  
	  if(!(parameters.containsKey("Indri:mu") && parameters.containsKey("Indri:lambda")))
	      throw new IllegalArgumentException("Missing Parameters for Indri in LeToR "); 
	  int mu = Integer.valueOf(parameters.get("Indri:mu"));
	  double lambda = Double.valueOf(parameters.get("Indri:lambda"));
	  if(mu < 0 || lambda < 0 || lambda > 1)
	      throw new IllegalArgumentException("Illegal Parameters for Indri Retrieval Model ");
	   
	  if(!(parameters.containsKey("letor:trainingQueryFile") && 
			  parameters.containsKey("letor:trainingQrelsFile") &&
			  parameters.containsKey("letor:trainingFeatureVectorsFile") &&
			  parameters.containsKey("letor:svmRankLearnPath") && 
			  parameters.containsKey("letor:svmRankClassifyPath") &&
			  parameters.containsKey("letor:svmRankParamC") &&
			  parameters.containsKey("letor:svmRankModelFile") && 
			  parameters.containsKey("letor:testingFeatureVectorsFile") && 
			  parameters.containsKey("letor:testingDocumentScores")))
		  throw new IllegalArgumentException("Missing Parameters for LeToR "); 
	  
	  double c = Double.valueOf(parameters.get("letor:svmRankParamC"));
	  if(parameters.containsKey("letor:featureDisable"))
		  return new LeToR(c, k1, k3, b, mu, lambda,
				  parameters.get("letor:trainingQueryFile"),parameters.get("letor:trainingQrelsFile"), parameters.get("letor:trainingFeatureVectorsFile"),
				  parameters.get("letor:svmRankLearnPath"),parameters.get("letor:svmRankClassifyPath"),
				  parameters.get("letor:svmRankModelFile"),parameters.get("letor:testingFeatureVectorsFile"), parameters.get("letor:testingDocumentScores"),
				  parameters.get("letor:featureDisable"));
	  else
		  return new LeToR(c, k1, k3, b, mu, lambda,
				  parameters.get("letor:trainingQueryFile"),parameters.get("letor:trainingQrelsFile"), parameters.get("letor:trainingFeatureVectorsFile"),
				  parameters.get("letor:svmRankLearnPath"),parameters.get("letor:svmRankClassifyPath"),
				  parameters.get("letor:svmRankModelFile"),parameters.get("letor:testingFeatureVectorsFile"), parameters.get("letor:testingDocumentScores"));
	  
  }

  /**
   *  Modified on 09/29/18 by @alicehzheng: adding BM25 and Indri
   *  Modified on 11/04/2018 by @alicehzheng: taking LeToR into consideration
   *  Allocate the retrieval model and initialize it using parameters
   *  from the parameter file.
   *  @return The initialized retrieval model
   *  @throws IOException Error accessing the Lucene index.
   */
  private static RetrievalModel initializeRetrievalModel (Map<String, String> parameters)
    throws IOException {

    RetrievalModel model = null;
    String modelString = parameters.get ("retrievalAlgorithm").toLowerCase();


    if (modelString.equals("unrankedboolean")) {
      model = new RetrievalModelUnrankedBoolean();
    }
    else if(modelString.equals("rankedboolean")){
    	model = new RetrievalModelRankedBoolean();
    }
    else if(modelString.equals("bm25") || modelString.equals("letor")){
        if(!(parameters.containsKey ("BM25:k_1") && parameters.containsKey ("BM25:k_3") && parameters.containsKey ("BM25:b")))
            throw new IllegalArgumentException("Missing Parameters for BM25 Retrieval Model ");
        double k1 = Double.valueOf(parameters.get("BM25:k_1"));
        double k3 = Double.valueOf(parameters.get("BM25:k_3"));
        double b =  Double.valueOf(parameters.get("BM25:b"));
        if(k1 < 0 || k3 < 0 || b < 0 || b > 1 )
            throw new IllegalArgumentException("Illegal Parameters for BM25 Retrieval Model ");
        model = new RetrievalModelBM25(k1,k3,b);
        // debug info
        System.out.println("Parameters of BM25 are k1:"+ k1 + " k3:" + k3 + " b:"+ b);
    }
    else if(modelString.equals("indri")){
        if(!(parameters.containsKey("Indri:mu") && parameters.containsKey("Indri:lambda")))
            throw new IllegalArgumentException("Missing Parameters for Indri Retrieval Model "); 
        int mu = Integer.valueOf(parameters.get("Indri:mu"));
        double lambda = Double.valueOf(parameters.get("Indri:lambda"));
        if(mu < 0 || lambda < 0 || lambda > 1)
            throw new IllegalArgumentException("Illegal Parameters for Indri Retrieval Model ");    
        model = new RetrievalModelIndri(mu,lambda);
    }
    else {
      throw new IllegalArgumentException
        ("Unknown retrieval model " + parameters.get("retrievalAlgorithm"));
      
    }
      
    return model;
  }

  /**
   *  Created on 10/20/18 by @alicehzheng
   *  Allocate the query expansion model and initialize it using parameters
   *  from the parameter file.
   *  @return The initialized query expansion model
   *  @throws IOException Error accessing the Lucene index.
   */
  private static QryExpansionModel initializeQryExpansionModel (Map<String, String> parameters)
    throws IOException {

    QryExpansionModel model = null;
    
    if(!parameters.containsKey("fb") || parameters.get("fb") == "false")
    	return model;
    
    if(!parameters.containsKey("fbDocs") || !parameters.containsKey("fbTerms") || !parameters.containsKey("fbMu") || !parameters.containsKey("fbOrigWeight"))
    	throw new IllegalArgumentException("Not Enought Parameters for Query Expansion ");  
    int fbdocs = Integer.parseInt(parameters.get("fbDocs"));
    int fbterms = Integer.parseInt(parameters.get("fbTerms"));
    int fbmu = Integer.parseInt(parameters.get("fbMu"));
    double fborigweight = Double.parseDouble(parameters.get("fbOrigWeight"));
    
    if(fbdocs <= 0 || fbterms <= 0 || fbmu < 0 || fborigweight < 0 || fborigweight > 1)
    	throw new IllegalArgumentException("Invalid Parameter(s) for Query Expansion ");  
    
    model = new QryExpansionModel(fbdocs, fbterms, fbmu, fborigweight);
    
    if(parameters.containsKey("fbInitialRankingFile"))
    	model.addRankingFile(parameters.get("fbInitialRankingFile"));
    
    if(parameters.containsKey("fbExpansionQueryFile"))
    	model.addQueryFile(parameters.get("fbExpansionQueryFile"));
      
    return model;
  }
  
  /**
   *  Created on 11/19/18 by @alicehzheng
   *  Allocate the diversity model and initialize it using parameters
   */
  private static DiversityModel initializeDiversityModel (Map<String, String> parameters)
    throws IOException {

    DiversityModel model = null;
    
    if(!parameters.containsKey("diversity") || parameters.get("diversity") == "false")
    	return model;
    
    if(!parameters.containsKey("diversity:maxInputRankingsLength") || !parameters.containsKey("diversity:maxResultRankingLength") || !parameters.containsKey("diversity:algorithm") || !parameters.containsKey("diversity:intentsFile") || !parameters.containsKey("diversity:lambda") )
    	throw new IllegalArgumentException("Not Enought Parameters for Diversification ");  
    int maxinputrankingslength = Integer.parseInt(parameters.get("diversity:maxInputRankingsLength"));
    int maxresultrankinglength = Integer.parseInt(parameters.get("diversity:maxResultRankingLength"));
    double lambda = Double.parseDouble(parameters.get("diversity:lambda"));
    String initialRankingFile = null;
    if(parameters.containsKey("diversity:initialRankingFile"))
    	initialRankingFile = parameters.get("diversity:initialRankingFile");
    String intentsFile = parameters.get("diversity:intentsFile");
    String algorithm = (parameters.get("diversity:algorithm")).toLowerCase();
    if(algorithm == "xquad")
    	model = new DiversityModelXquad(initialRankingFile,maxinputrankingslength,maxresultrankinglength,intentsFile,lambda);
    else
    	model = new DiversityModelPm2(initialRankingFile,maxinputrankingslength,maxresultrankinglength,intentsFile,lambda);

      
    return model;
  }
  /**
   * Print a message indicating the amount of memory used. The caller can
   * indicate whether garbage collection should be performed, which slows the
   * program but reduces memory usage.
   * 
   * @param gc
   *          If true, run the garbage collector before reporting.
   */
  public static void printMemoryUsage(boolean gc) {

    Runtime runtime = Runtime.getRuntime();

    if (gc)
      runtime.gc();

    System.out.println("Memory used:  "
        + ((runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)) + " MB");
  }

  /**
   * Modified on 09/16/18 by @alicehzheng: sort the score list first using score (descending), then using external id (ascending)
   * Process one query.
   * @param qString A string that contains a query.
   * @param model The retrieval model determines how matching and scoring is done.
   * @return Search results
   * @throws IOException Error accessing the index
   */
  static ScoreList processQuery(String qString, RetrievalModel model)
    throws IOException {

    String defaultOp = model.defaultQrySopName ();
    qString = defaultOp + "(" + qString + ")";
    Qry q = QryParser.getQuery (qString);

    // Show the query that is evaluated
    
    System.out.println("    --> " + q);
    
    if (q != null) {

      ScoreList r = new ScoreList ();
      
      if (q.args.size () > 0) {		// Ignore empty queries

        q.initialize (model);

        while (q.docIteratorHasMatch (model)) {
          int docid = q.docIteratorGetMatch ();
          double score = ((QrySop) q).getScore (model);
          r.add (docid, score);
          q.docIteratorAdvancePast (docid);
        }
      }
      // Added on 09/16/18 by @alicehzheng
      // sort the score list first using score (descending), then using external id (ascending)
      r.sortExternal(); 
      
      return r;
    } else
      return null;
  }

  /**
   *  Added on 10/20/18 by @alicehzheng: Comparator for the weighted list of terms
   */
  public static class WeightListComparator implements Comparator<Pair<String,Double>> {

	    @Override
	    public int compare(Pair<String, Double> s1, Pair<String,Double> s2) {
	    	double w1 = s1.getValue(), w2 = s2.getValue();
	    	if(w1 > w2)
	    		return -1;
	    	if(w1 == w2)
	    		return 0;
	    	return 1;
	    }
   }
  
  /**
   *  Modified on 09/16/18 by @alicehzheng: Added outputing result
   *  Modified on 10/20/18 by @alicehzheng: Added query expansion processing
   *  Modified on 11/05/18 by @alicehzheng: taking LeToR into consideration (reranking)
   *  Process the query file.
   *  @param queryFilePath
   *  @param outputFilePath // added on 09/16/18
   *  @param outputLen      // added on 09/16/18
   *  @param model
   *  @param expansionModel // added on 10/20/18
   *  @param letor          // added on 11/05/18 
   *  @throws IOException Error accessing the Lucene index.
   */
  static void processQueryFile(String queryFilePath, String outputFilePath, int outputLen, RetrievalModel model, QryExpansionModel expansionModel, LeToR letor)
      throws IOException {

    BufferedReader input = null;
    BufferedWriter output = null;
    
    BufferedReader rankingFileInput = null;
    BufferedWriter queryExpansionOutput = null;
    
    BufferedWriter testingFeatureVectorOutput = null;
    
    
    Map<String, ArrayList<Integer>> query2docids = new HashMap<String, ArrayList<Integer>> ();
    Map<String, ArrayList<Double>> query2scores = new HashMap<String, ArrayList<Double>> ();
    int fbdocs = 0, fbterms = 0, fbmu = 0;
    double fborigweight = 0.0;
    
    ArrayList<String> qidList = new ArrayList<String>();
   
    try {
      String qLine = null;

      input = new BufferedReader(new FileReader(queryFilePath));
      
      output = new BufferedWriter(new FileWriter(outputFilePath));
      
      if(letor != null)
    	  testingFeatureVectorOutput = new BufferedWriter(new FileWriter(letor.testingFeatureVectorsFile));
      
      // Added on 10/20/18 by @alicehzheng: Preprocesssing for potential query expansion
      
      if(expansionModel != null){
    	  fbdocs = expansionModel.fbDocs;
    	  fbterms = expansionModel.fbTerms;
    	  fbmu = expansionModel.fbMu;
    	  fborigweight = expansionModel.fbOrigWeight;
    	  String dLine = null;
    	  if(expansionModel.fbInitialRankingFile != null){
    		  //System.out.println(expansionModel.fbInitialRankingFile);
    		  rankingFileInput = new BufferedReader(new FileReader(expansionModel.fbInitialRankingFile));
    		  while ((dLine = rankingFileInput.readLine()) != null) {
    			  //System.out.println(dLine);
    			  String[] parsed = dLine.split("\\s+");
    			  //System.out.println(parsed);
    			  
    		      String qid = parsed[0];
    		      String externalDocId = parsed[2];
    		      Double score = Double.valueOf(parsed[4]);
    		      Integer internalDocId = null;
				  try {
					internalDocId = Idx.getInternalDocid(externalDocId);
				  } catch (Exception e) {
					e.printStackTrace();
				  }
    		      if(!query2docids.containsKey(qid)){		
    		    		query2docids.put(qid, new ArrayList<Integer> ());
    		    		query2docids.get(qid).add(internalDocId);
    		
    		    		query2scores.put(qid, new ArrayList<Double> ());
    		    		query2scores.get(qid).add(score);
    		      }
    		      else{
    		    	  if(query2docids.get(qid).size() >= fbdocs)
    		    		  continue;
    		    	  query2docids.get(qid).add(internalDocId);
    		    	  query2scores.get(qid).add(score);
    		      }
    		 }
    	  }
    	  if(expansionModel.fbExpansionQueryFile != null)
    		  queryExpansionOutput = new BufferedWriter(new FileWriter(expansionModel.fbExpansionQueryFile));
    	  
      }

      //  Each pass of the loop processes one query.

      while ((qLine = input.readLine()) != null) {
        int d = qLine.indexOf(':');

        if (d < 0) {
          throw new IllegalArgumentException
            ("Syntax error:  Missing ':' in query line.");
        }

        printMemoryUsage(false);

        String qid = qLine.substring(0, d);
        String query = qLine.substring(d + 1);

        System.out.println("Original Query " + qLine);
        
        qidList.add(qid);
        
        ScoreList r = null;
        ScoreList roriginal = null;

        if(expansionModel == null){
        	r= processQuery(query, model);
        }
        else{
        	ArrayList<Integer> docList = new ArrayList<Integer> ();
        	ArrayList<Double> docScoreList = new ArrayList<Double> ();
        	ArrayList<Integer> lenList = new ArrayList<Integer> ();
        	ArrayList<TermVector> fwdIdxList = new ArrayList<TermVector> ();
        	ArrayList<String> termList = new ArrayList<String> ();
        	TreeSet<Pair<String,Double>> weightSet = new TreeSet<Pair<String,Double>> (new WeightListComparator());
        	
        	if(rankingFileInput == null){
        		roriginal = processQuery(query, model);
        		int upperbound = Math.min(roriginal.size(), fbdocs);
        		for(int i = 0; i < upperbound; ++i){
        			docList.add(roriginal.getDocid(i));
        			docScoreList.add(roriginal.getDocidScore(i));
        		}
        	}
        	else{
        		docList = query2docids.get(qid);
        		docScoreList = query2scores.get(qid);
        	}
        	
        	int docCnt = docList.size();
        	for(int i = 0; i < docCnt; ++i){
        		int docid = docList.get(i);
        		TermVector forwardIndex = new TermVector(docid, "body");
        		fwdIdxList.add(forwardIndex);
        		int doclen = forwardIndex.positionsLength();
        		lenList.add(doclen);
        		int vocabSize = forwardIndex.stemsLength();
        		for(int j = 0; j < vocabSize; ++j){
        			if(forwardIndex.stemFreq(j) > 0){ // filter stop words
        				String term = forwardIndex.stemString(j);
        				// filter terms that have already been added as well as those contain comma/period 
        				if (!termList.contains(term) && !term.contains(",") && !term.contains("."))
        					termList.add(term);
        			}
        		}
        	}
        	
        	long collectionTokenCnt = Idx.getSumOfFieldLengths("body");
        	int termCnt = termList.size();
        	for(int i = 0; i < termCnt; ++i){
        		String term = termList.get(i);
        		double pt_c = (double) Idx.getTotalTermFreq("body", term) / collectionTokenCnt;
        		double weight = 0.0;
        		for(int j = 0; j < docCnt; ++j){
        			TermVector fwdidx = fwdIdxList.get(j);
        			int tf = 0;
        			int termIdx = fwdidx.indexOfStem(term);
        			if( termIdx >= 0)
        				tf = fwdidx.stemFreq(termIdx);
        			weight += ((tf + fbmu * pt_c) / (lenList.get(j) + fbmu)) * docScoreList.get(j);
        		}
        		weight *= Math.log(1 / pt_c);
        		weightSet.add(new Pair<String, Double> (term,weight));
        	}
        	
        	
        	StringBuffer expandedQuery = new StringBuffer();
        	expandedQuery.append("#wand ( ");
        	Iterator it = weightSet.iterator();
        	int expandedCnt = 0;
        	
        	while(it.hasNext() && expandedCnt < fbterms){
        		expandedCnt++;
        	    Pair<String, Double> termpair = (Pair<String,Double>) it.next();
        	    expandedQuery.append(termpair.getValue());
        	    expandedQuery.append(" ");
        	    expandedQuery.append(termpair.getKey());
        	    expandedQuery.append(" ");
        	}
        	expandedQuery.append(" )");
        	
        	//System.out.println(expansionModel.fbExpansionQueryFile);
        	if(queryExpansionOutput != null){
        		queryExpansionOutput.write(qid);
        		queryExpansionOutput.write(": ");
        		queryExpansionOutput.write(expandedQuery.toString());
        		queryExpansionOutput.newLine();
        	}
        	
        	StringBuffer newQuery = new StringBuffer();
        	newQuery.append("#wand ( ");
        	newQuery.append(fborigweight);
        	newQuery.append(" #and(");
        	newQuery.append(query);
        	newQuery.append(" ) ");
        	newQuery.append(1 - fborigweight);
        	newQuery.append(" ");
        	newQuery.append(expandedQuery);
        	newQuery.append(" )");
        	
        	String newqry = newQuery.toString();
        	System.out.println("New Query " + newqry);
        	
        	r= processQuery(newqry, model);
        	
        	
        }
        
        
        // Modified on 11/05/18 by @alicehzheng: adding a letor reranking process
        if (r != null ) {
        	if(letor == null){
        		printResults(qid, r,output, outputLen);
        		System.out.println();
        	}
        	else{	
        		ArrayList<String> externalList = new ArrayList<String> ();
        		int upperbound = Math.min(r.size(), 100);
        		for(int i = 0; i < upperbound; ++i){
        			// get external doc id of the doc in the score list
        			externalList.add(Idx.getExternalDocid(r.getDocid(i)));
        		}
        		try {
        			letor.test(qLine, externalList,testingFeatureVectorOutput);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        }   
          
      }
      
      
      if(letor != null){
    	  if(testingFeatureVectorOutput != null)
        	  testingFeatureVectorOutput.close();
    	  ArrayList<ScoreList> results = null;
      		try {
				results = letor.rerank();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
      	
      		int queryCnt = qidList.size();
      		for(int i = 0; i < queryCnt; ++i){
      			printResults(qidList.get(i), results.get(i), output, outputLen);
      			System.out.println();
      		}
      	}
      
      
      
    } catch (IOException ex) {
      ex.printStackTrace();
    } finally {
      input.close();
      output.close();
      if(rankingFileInput != null)
    	  rankingFileInput.close();
      if(queryExpansionOutput != null)
    	  queryExpansionOutput.close();
      
    }
  }
  
  /**
   *  Added on 11/19/18 by @alicehzheng
   *  Process the query file with Diversification
   */
 
  static void processQueryFileDiversity(String queryFilePath, String outputFilePath, int outputLen, RetrievalModel model, DiversityModel diversityModel)
      throws IOException {

    BufferedReader input = null;
    BufferedWriter output = null;
    
    
    
    BufferedReader intentsInput = null;

    try {
      String qLine = null;

      input = new BufferedReader(new FileReader(queryFilePath));
      
      output = new BufferedWriter(new FileWriter(outputFilePath));
      
      if(diversityModel.initialRankingFile != null)
    	  diversityModel.retrievaInitialRanking();
      else
    	  intentsInput = new BufferedReader(new FileReader(diversityModel.intentsFile));
      
      
      
      //  Each pass of the loop processes one query.

      while ((qLine = input.readLine()) != null) {
        int d = qLine.indexOf(':');

        if (d < 0) {
          throw new IllegalArgumentException
            ("Syntax error:  Missing ':' in query line.");
        }

        printMemoryUsage(false);

        String qid = qLine.substring(0, d);
        String query = qLine.substring(d + 1);

        System.out.println("Query " + qLine);
        
        if(intentsInput == null){
        	Map<String, ArrayList> doc2scores = diversityModel.getScoresForQuery(qid);
        	Map<String, ArrayList> normalized = diversityModel.normalize(doc2scores);
        	int intentCnt = 0;
        	for(String doc: normalized.keySet()){
        		intentCnt = normalized.get(doc).size() - 1;
        		break;
        	}
        	LiteScoreList res = diversityModel.rerank(normalized,intentCnt);
        }
        else{
        	ScoreList r = null;
            r = processQuery(query, model);
        }
        
       
        
        
        
        if (res != null) {
          printResults(qid, r,output, outputLen);
          System.out.println();
        }
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    } finally {
      input.close();
      output.close();
    }
  }

  /**
   * Modified on 09/16/18 by @alicehzheng: modified output format; added functionality to write output to file
   * 
   * Print the query results.
   * 
   * THIS IS NOT THE CORRECT OUTPUT FORMAT. YOU MUST CHANGE THIS METHOD SO
   * THAT IT OUTPUTS IN THE FORMAT SPECIFIED IN THE HOMEWORK PAGE, WHICH IS:
   * 
   * QueryID Q0 DocID Rank Score RunID
   * 
   * @param queryId
   * @param result
   *          A list of document ids and scores
   * @param outputWriter
   * @param outputLen
   * @throws IOException Error accessing the Lucene index.
   */
  static void printResults(String queryId, ScoreList result, BufferedWriter outputWriter, int outputLen) throws IOException {
    String outputMsg = queryId + " Q0 dummy 1 0 run-1";
    if (result.size() < 1) {
        System.out.println(outputMsg);
        outputWriter.write(outputMsg + "\n");
    } 
    else {
        for (int i = 0; i < result.size() && i < outputLen; i++) {
            outputMsg = queryId + " Q0 " + Idx.getExternalDocid(result.getDocid(i)) + " " 
                    + (i+1) + " " + result.getDocidScore(i) + " run-1";
            System.out.println(outputMsg);
            outputWriter.write(outputMsg + "\n");
        }
    }
  }

  /**
   *  Read the specified parameter file, and confirm that the required
   *  parameters are present.  The parameters are returned in a
   *  HashMap.  The caller (or its minions) are responsible for processing
   *  them.
   *  @return The parameters, in <key, value> format.
   */
  private static Map<String, String> readParameterFile (String parameterFileName)
    throws IOException {

    Map<String, String> parameters = new HashMap<String, String>();

    File parameterFile = new File (parameterFileName);

    if (! parameterFile.canRead ()) {
      throw new IllegalArgumentException
        ("Can't read " + parameterFileName);
    }

    Scanner scan = new Scanner(parameterFile);
    String line = null;
    do {
      line = scan.nextLine();
      String[] pair = line.split ("=");
      parameters.put(pair[0].trim(), pair[1].trim());
    } while (scan.hasNext());

    scan.close();

    if (! (parameters.containsKey ("indexPath") &&
           parameters.containsKey ("queryFilePath") &&
           parameters.containsKey ("trecEvalOutputPath") &&
           parameters.containsKey("trecEvalOutputLength") && 
           parameters.containsKey ("retrievalAlgorithm"))) {
      throw new IllegalArgumentException
        ("Required parameters were missing from the parameter file.");
    }

    return parameters;
  }

}
