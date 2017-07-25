package worker;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Worker {
	
	public Map<Integer, Topic> topics;
	public List<Topic> topicList;
	public int id;
	public String name;
	
	public Worker(Map<Integer, Topic> topics, int id) {
		this.topics = topics;
		this.id = id;
		this.topicList = new ArrayList<Topic>();
		for (int i = 0; i < topics.size(); i++) {
			this.topicList.add(topics.get(i));
		}
		
		
	}
}