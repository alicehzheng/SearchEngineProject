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
			if(prevQid != null && dotIdx < 0 && curQid != prevQid){
				for(String doc: docScores.keySet()){
					while(docScores.get(doc).size() < curIntent + 1){
						docScores.get(doc).add(0.0);
					}
				}
				this.query2docScores.put(prevQid, docScores);
				docScores = new HashMap<String, ArrayList>();
				scoreCnt = 0;
			}
			if(dotIdx > 0){
				curQid = curQid.substring(0, dotIdx);
				curIntent = Integer.parseInt(curQid.substring(dotIdx+1));
			}
			
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
		    	if(curIntent > 0)
		    		continue;
		    	docScores.put(externalDocId, new ArrayList<Double>());
		    }
		    int size = docScores.get(externalDocId).size();
		    while(size < curIntent){
		    	docScores.get(externalDocId).add(0.0);
		    	size++;
		    }
		    docScores.get(externalDocId).add(score); 
		}
		for(String doc: docScores.keySet()){
			while(docScores.get(doc).size() < curIntent + 1){
				docScores.get(doc).add(0.0);
			}
		}
		this.query2docScores.put(prevQid, docScores);
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
