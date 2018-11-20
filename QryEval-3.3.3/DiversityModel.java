import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created on 11/19/18
 * @author alicehzheng
 *
 */
public abstract class DiversityModel {
	String initialRankingFile;
	int maxInputRankingsLength;
	int maxResultRankingLength;
	String intentsFile;
	double lambda;
	Map<String, Map<String, ArrayList>> query2docScores = new HashMap<String, Map<String, ArrayList>>();
	DiversityModel(){
	}
	DiversityModel(String initial_ranking_file, int max_input_rankings_length, int max_result_ranknig_length,
			String intents_file, double lambda){
		this.initialRankingFile = initial_ranking_file;
		this.maxInputRankingsLength = max_input_rankings_length;
		this.maxResultRankingLength = max_result_ranknig_length;
		this.intentsFile = intents_file;
		this.lambda = lambda;
	}
	
	public boolean retrievaInitialRanking() throws IOException {
		BufferedReader initialInput = null;
		if (initialRankingFile == null)
			return false;
		initialInput = new BufferedReader(new FileReader(initialRankingFile));
		String line;
		String prevQid = null;
		Map<String,ArrayList> docScores = new HashMap<String, ArrayList>();
		int scoreCnt = 0;
		int prevIntent = 0;
		int curIntent = 0;
		while ((line = initialInput.readLine()) != null){
			String[] parsed = line.split("\\s+");
			String curQid = parsed[0];
			int dotIdx = curQid.indexOf('.');
			
			//System.out.println(dotIdx);
			if(prevQid != null && dotIdx < 0  && !curQid.equals(prevQid)){
				//System.out.println(prevQid);
				//System.out.println(curQid);
				for(String doc: docScores.keySet()){
					while(docScores.get(doc).size() < curIntent + 1){
						docScores.get(doc).add(0.0);
					}
				}
				/**
				for(String doc: docScores.keySet()){
					System.out.println(doc + "->");
					ArrayList<Double> sList = docScores.get(doc);
					for(Double s: sList){
						System.out.println(s);
					}
				}
				**/
				this.query2docScores.put(prevQid, docScores);
				docScores = new HashMap<String, ArrayList>();
				scoreCnt = 0;
				curIntent = 0;
			}
			if(dotIdx > 0){
				String tmp = curQid;
				curQid = tmp.substring(0, dotIdx);
				curIntent = Integer.parseInt(tmp.substring(dotIdx+1));
				
			}
			//System.out.println(curQid + " " + curIntent);
			
			if(curIntent != prevIntent){
				scoreCnt = 0;
			}
			prevQid = curQid;
		    prevIntent = curIntent;
			if(scoreCnt >= this.maxInputRankingsLength)
				continue;
			scoreCnt++;	
			String externalDocId = parsed[2];
		    Double score = Double.valueOf(parsed[4]);
		    if (!docScores.containsKey(externalDocId)){
		    	//System.out.println("flag1");
		    	if(curIntent > 0)
		    		continue;
		    	//System.out.println("flag2");
		    	docScores.put(externalDocId, new ArrayList<Double>());
		    }
		    int size = docScores.get(externalDocId).size();
		    while(size < curIntent){
		    	//System.out.println("flag3");
		    	docScores.get(externalDocId).add(0.0);
		    	size++;
		    }
		    docScores.get(externalDocId).add(score); 
		    //System.out.println("flag4");
		}
		for(String doc: docScores.keySet()){
			//System.out.println("flag5");
			while(docScores.get(doc).size() < prevIntent + 1){
				docScores.get(doc).add(0.0);
			}
		}
		
		/**
		for(String doc: docScores.keySet()){
			
			System.out.println(doc + "->");
			ArrayList<Double> sList = docScores.get(doc);
			for(Double s: sList){
				System.out.println(s);
			}
		}
		**/
		this.query2docScores.put(prevQid, docScores);
		for(String qid: query2docScores.keySet())
			System.out.println(qid + ":" + query2docScores.get(qid).size());
		return true;
	}
	
	public Map<String, ArrayList> getScoresForQuery(String query){
		return query2docScores.get(query);
	}
	
	public Map<String,ArrayList> normalize(Map<String,ArrayList> doc2scores){
		boolean needNormalize = false;
		ArrayList<Double> sumList = new ArrayList<Double>();
		boolean firstDoc = true;
		for(String key: doc2scores.keySet()){
			ArrayList<Double> scores = doc2scores.get(key);
			if(firstDoc){
				for(Double score: scores){
					sumList.add(score);
					if (score > 1.0)
						needNormalize = true;
				}
				firstDoc = false;
			}
			else{
				for(int i = 0; i < scores.size(); ++i){
					Double score = scores.get(i);
					if (score > 1.0)
						needNormalize = true;
					sumList.set(i, sumList.get(i) + score);
				}
			}
		}
		if(needNormalize){
			Double maxSum = sumList.get(0);
			for(Double sum: sumList){
				maxSum = Math.max(maxSum, sum);
			}
			for(String key: doc2scores.keySet()){
				ArrayList<Double> scores = doc2scores.get(key);
				for(int i = 0; i < scores.size(); ++i){
					doc2scores.get(key).set(i, scores.get(i) / maxSum);
				}
			}
		}
		return doc2scores;
	}
	
	public abstract LiteScoreList rerank(Map<String,ArrayList> doc2scores, int intentCnt);
	
	
}
