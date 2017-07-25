package worker;

public class Topic implements Comparable<Topic> {
	
	public int id;
	public double avgTime;
	public int numTaken;
	public double successRate;
	public double score;
	public double preference;
	
	public Topic(int id, double avgTime, int numTaken, double successRate, double score, double preference) {
		this.id = id;
		this.avgTime = avgTime;
		this.numTaken = numTaken;
		this.successRate = successRate;
		this.score = score;
		this.preference = preference;
	}
	
	@Override
	public int compareTo(Topic other) {
		return Double.valueOf(other.preference).compareTo(Double.valueOf(this.preference));
	}
}