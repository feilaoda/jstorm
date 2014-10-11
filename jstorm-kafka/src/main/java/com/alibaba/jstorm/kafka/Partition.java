package com.alibaba.jstorm.kafka;

import java.io.Serializable;

public class Partition  implements Serializable{
	private KafkaBroker broker;
	private int partition;

	public Partition(KafkaBroker broker, int partition) {
		this.broker = broker;
		this.partition = partition;
	}

	public KafkaBroker getBroker() {
		return broker;
	}

	public void setBroker(KafkaBroker broker) {
		this.broker = broker;
	}

	public int getPartition() {
		return partition;
	}

	public void setPartition(int partition) {
		this.partition = partition;
	}

}
