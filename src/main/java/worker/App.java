package worker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;

import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange.*;
/**
 * Hello world!
 *
 */
public class App 
{	
	
	public static List<Worker> workers = new ArrayList<Worker>();
	public static Map<Integer, Topic> topicAvg;
	public static String[] names;
	public static List<Worker> occupied = new ArrayList<Worker>();
	public static int numWorkers = 100;
	
    public static void main( String[] args ) throws IOException
    {
    	int i = 0;
    	names = new String[numWorkers];
    	File file = ResourceUtils.getFile("classpath:names.txt");
		Scanner scanner = new Scanner(file);
		while (scanner.hasNextLine() && i < numWorkers) {
			String line = scanner.nextLine();
			String name = line.split(" ")[0];
			String output = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase(); 
			names[i] = output;
			i++;
		}
    	createWorkers(workers, 10, numWorkers);
    	topicAvg = computeAverageForAllTopics(workers, 10);
    	computeScores(workers, 10, topicAvg);
    	computePreferences(workers, 10, topicAvg);
    	HttpServer server = HttpServer.create(new InetSocketAddress(8005), 0);
    	server.createContext("/topic", new MyHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    	
    	
    }
    
    static class MyHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
        	t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        	if (occupied.size() > 3 * workers.size()) {
        		workers.add(occupied.get(0));
        		occupied.remove(0);
        	}
        	String number = t.getRequestURI().getRawPath().split("/topic/")[1];
        	List<Worker> candidatePool = rankForCallTopic(2, workers, Integer.valueOf(number), topicAvg);
        	Worker w = candidatePool.get(0);
        	System.out.println(candidatePool.size());
        	Topic topic = w.topics.get(Integer.valueOf(number));
        	String a = "{\"agent\": {\"avgTime\": \"" + String.valueOf(Math.round(topic.avgTime)) + "\","; 
        	String b = "\"numTaken\": \"" + String.valueOf(topic.numTaken) + "\","; 
        	String x = "\"name\": \"" + names[w.id] + "\","; 
        	String c = "\"successRate\": \"" + String.valueOf(Math.round(topic.successRate * 100)) + "%\"},"; 
        	topic = topicAvg.get(Integer.valueOf(number));
        	String d = "\"average\": {\"avgTime\": \"" + String.valueOf(Math.round(topic.avgTime)) + "\","; 
        	String e = "\"numTaken\": \"" + String.valueOf(topic.numTaken) + "\","; 
        	String y = "\"name\": \"Average\","; 
        	String f = "\"successRate\": \"" + String.valueOf(Math.round(topic.successRate * 100)) + "%\"}}"; 
            String response = a + b + x + c + d + e + y + f;
            workers.remove(w);
            occupied.add(w);
            t.getResponseHeaders().set("Content-Type", "application/json");
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
    
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
    	double numCallsMultiplier = Math.pow(1.0 * t.numTaken, 1 / 3.5);
    	double numCallsScore = Math.pow(1.0 * t.numTaken, 1/2.5);
    	double timeScore = Math.pow((tTotal.avgTime - t.avgTime) / tTotal.avgTime, 3);
    	double successScore = Math.pow((t.successRate - tTotal.successRate) / tTotal.successRate, 3);
    	double score = numCallsMultiplier * (timeScore + successScore);
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
    
    public static Topic createTopicForWorker(Integer topicId, Integer avgTime, Double successRate, 
    											Boolean doesHandle, Boolean good) {
    	Random rng = new Random();
    	int aTime;
    	int numTaken;
    	double sRate;
    	if (good) {
    		aTime = avgTime + 100 - rng.nextInt(300);
    		sRate = successRate - .2 + rng.nextDouble() / 3;
    	} else {
    		aTime = avgTime - 100 + rng.nextInt(300);
    		sRate = successRate + .2 - rng.nextDouble() / 3;
    	}
    	if (doesHandle) {
    		numTaken = rng.nextInt(50) + 35;
    	} else {
    		numTaken = rng.nextInt(15);
    	}
    	if (sRate > 1) {
    		sRate = 1;
    	} 
    	if (sRate < 0) {
    		sRate = 0;
    	}
    	return new Topic(topicId, aTime, numTaken, sRate, 0, 0);
    }
    
    public static void createWorkers(List<Worker> workers, Integer numTopics, Integer numWorkers) {
    	Random rng = new Random();
    	List<Integer> topicTime = new ArrayList<Integer>();
    	List<Double> topicSRate = new ArrayList<Double>();
    	List<Double> topicHandle = new ArrayList<Double>();
    	List<Double> topicGood = new ArrayList<Double>();
    	for (int i = 0; i < numTopics; i++) {
    		int avgTimeMin = rng.nextInt(300) + rng.nextInt(300) + rng.nextInt(300) + 200;
    		topicTime.add(avgTimeMin);
    		double avgSRate = (rng.nextDouble() + rng.nextDouble()) / 2;
    		topicSRate.add(avgSRate);
    		double avgHandle = (rng.nextDouble() + rng.nextDouble()) / 2;
    		topicHandle.add(avgHandle);
    		double avgGood = (rng.nextDouble() + rng.nextDouble()) / 2;
    		topicGood.add(avgGood);
    	}
    	for (int i = 0; i < numWorkers; i++) {
    		Map<Integer, Topic> t = new TreeMap<Integer, Topic>();
    		for (int j = 0; j < numTopics; j++) {
    			int avgTime = 0;
    			double successRate = 0;
    			boolean doesHandle = true;
    			boolean good = true;
    			switch(j) {
    			case 0:
    				avgTime = 400;
    				successRate = .95;
    				doesHandle = rng.nextDouble() > .4;
    				good = rng.nextDouble() > .2;
    				break;
    			case 1:
    				avgTime = 600;
    				successRate = .7;
    				doesHandle = rng.nextDouble() > .6;
    				good = rng.nextDouble() > .5;
    				break;
    			case 2:
    				avgTime = 800;
    				successRate = .4;
    				doesHandle = rng.nextDouble() > .8;
    				good = rng.nextDouble() > .8;
    				break;
    			case 3:
    				avgTime = 750;
    				successRate = .95;
    				doesHandle = rng.nextDouble() > .3;
    				good = rng.nextDouble() > .6;
    				break;
    			default:
    				avgTime = topicTime.get(j);
    				successRate = topicSRate.get(j);
    				doesHandle = rng.nextDouble() > topicHandle.get(j);
    				good = rng.nextDouble() > topicGood.get(j);
    				break;
    				
    			}
    			if (doesHandle) {
    				good = rng.nextDouble() > .2;
    			}
    			t.put(j, createTopicForWorker(j, avgTime, successRate, doesHandle, good));
    		}
    		workers.add(new Worker(t, i));
		}
    }
    
    public static List<Worker> rankForCallTopic(int x, List<Worker> workers, final Integer topicId, 
    												Map<Integer, Topic> topicAvg) {
    	List<Worker> candidatePool = new ArrayList<Worker>();
    	for (int i = 0; i < workers.size(); i++) {
    		Worker w = workers.get(i);
    		Collections.sort(w.topicList);
    		if (w.topicList.indexOf(workers.get(i).topics.get(topicId)) < x * w.topicList.size() / 8) {
    			candidatePool.add(w);
    		}
//    		if (workers.get(i).topics.get(topicId).score > topicAvg.get(topicId).score) {
//    			candidatePool.add(workers.get(i));
//    		}
    	}
    	if (candidatePool.isEmpty()) {
    		return rankForCallTopic(x + 1, workers, topicId, topicAvg);
    	}

    	candidatePool.sort((worker1, worker2) -> {
    		int w1 = 0;
    		int w2 = 0;
    		Collections.sort(worker1.topicList);
    		Collections.sort(worker2.topicList);
    		for (int i = 0; i < worker1.topicList.size(); i++) {
    			if (worker1.topicList.get(i).id == topicId) {
    				w1 = i;
    			}
    			if (worker2.topicList.get(i).id == topicId) {
    				w2 = i;
    			} 
    		}
    		return Double.compare(worker2.topics.get(topicId).score, worker1.topics.get(topicId).score);
//    		return Double.compare(w1 / Math.pow(worker1.topics.get(topicId).score, 3), 
//    				w2 / Math.pow(worker1.topics.get(topicId).score, 3));
    	});
    	return candidatePool;
    }
}
