package worker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class MatchingInstance {
	
	
    public static Topic computeTopicAverage(List<Worker> workerList, Integer topicId) {
    	int successfulCalls = 0;
    	int totalCalls = 0;
    	int totalTime = 0;
    	for (int i = 0; i < workerList.size(); i++) {
    		Topic topic = workerList.get(i).topics.get(topicId);
    		successfulCalls += topic.numTaken;
    		totalCalls += topic.successRate != 0.0 ? topic.numTaken / topic.successRate : 0;
    		totalTime += topic.numTaken * topic.avgTime * 1.0;
    	}
    	double averageTime = (totalTime * 1.0) / successfulCalls;
    	double successRate = (1.0 * successfulCalls) / totalCalls;
    	return new Topic(topicId, averageTime, successfulCalls, successRate, 0, 0);
    }
    
    public static double computeTopicScore(Worker worker, int topicId, Map<Integer, Topic> topics) {
    	Topic t = worker.topics.get(topicId);
    	Topic tTotal = topics.get(topicId);
    	double numCallsMultiplier = Math.sqrt(1.0 * t.numTaken);
    	double numCallsScore = Math.cbrt(1.0 * t.numTaken);
    	double timeScore = Math.pow((tTotal.avgTime - t.avgTime) / tTotal.avgTime, 3);
    	double successScore = Math.pow((t.successRate - tTotal.successRate) / tTotal.successRate, 3);
    	double score = numCallsScore + numCallsMultiplier * (timeScore + successScore);
    	return score;
    }
    
    public static Map<Integer, Topic> computeAverageForAllTopics(List<Worker> workers, Integer numTopics) {
    	Map<Integer, Topic> t = new TreeMap<Integer, Topic>();
    	for (int i = 0; i < numTopics; i++) {
    		t.put(i, computeTopicAverage(workers, i));
    	}
    	return t;
    }
    
    public static void computeScores(List<Worker> workers, Integer numTopics, Map<Integer, Topic> topicAvg) {
    	for (int i = 0; i < workers.size(); i++) {
    		for (int j = 0; j < numTopics; j++) {
    			Worker w = workers.get(i);
    			w.topics.get(j).score = computeTopicScore(w, j, topicAvg);
    			if (w.topics.get(j).score > 2 * topicAvg.get(j).score) {
    				topicAvg.get(j).score = w.topics.get(j).score / 2.0;
    			}
    		}
    	}
    }
    
    public static void computePreferences(List<Worker> workers, Integer numTopics, Map<Integer, Topic> topicAvg) {
    	for (int j = 0; j < numTopics; j++) {
    		int numAbove = 0;
    		for (int i = 0; i < workers.size(); i++) {
    			if (workers.get(i).topics.get(j).score > topicAvg.get(j).score) {
    				numAbove++;
    			}
    		}
    		topicAvg.get(j).preference = workers.size() / (numAbove * 1.0);
    	}
    	for (int j = 0; j < numTopics; j++) {
    		for (int i = 0; i < workers.size(); i++) {
    			workers.get(i).topics.get(j).preference = workers.get(i).topics.get(j).score *
    					topicAvg.get(j).preference;
    		}
    	}
    }
    
    public static void createWorkers(List<Worker> workers, Integer numTopics, Integer numWorkers) {
    	Random rng = new Random();
    	List<Integer> topicTime = new ArrayList<Integer>();
    	for (int i = 0; i < numTopics; i++) {
    		int avgTimeMin = rng.nextInt(500) + 300;
    		topicTime.add(avgTimeMin);
    	}
    	for (int i = 0; i < numWorkers; i++) {
    		Map<Integer, Topic> t = new TreeMap<Integer, Topic>();
    		for (int j = 0; j < numTopics; j++) {
    			double successRate = rng.nextDouble() / 2;
    			int avgTime = topicTime.get(j) + rng.nextInt(300);
    			int numTaken = rng.nextInt(85);
    			if (numTaken > 15) {
    				successRate += .5;
    			}
    			t.put(j, new Topic(j, avgTime, numTaken, successRate, 0, 0));
    		}
    		workers.add(new Worker(t, i));
		}
    }
    
    public static List<Worker> rankForCallTopic(List<Worker> workers, final Integer topicId, Map<Integer, Topic> topicAvg) {
    	List<Worker> candidatePool = new ArrayList<Worker>();
    	for (int i = 0; i < workers.size(); i++) {
    		if (workers.get(i).topics.get(topicId).score > topicAvg.get(topicId).score) {
    			candidatePool.add(workers.get(i));
    		}
    	}
    	candidatePool.sort(new Comparator<Worker>() {
    		public int compare(Worker worker1, Worker worker2) {
    			return Double.valueOf(worker1.topics.get(topicId).preference).compareTo(
    					Double.valueOf(worker2.topics.get(topicId).preference));
    		}
    	});
    	return candidatePool;
    }
}