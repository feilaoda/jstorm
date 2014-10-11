package com.alibaba.jstorm.kafka;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import scala.actors.threadpool.Arrays;

import com.alibaba.jstorm.utils.JStormUtils;

import backtype.storm.spout.MultiScheme;
import backtype.storm.spout.RawMultiScheme;

/**
 * by fld
 */
public class KafkaSpoutConfig implements Serializable {

	
	private static final long serialVersionUID = 1L;

	public MultiScheme scheme = new RawMultiScheme();
	public List<KafkaBroker> hosts;
	public int partitionsPerBroker;
	public String topic;
	
	public int fetchMaxBytes = 1024*1024;
	public int fetchWaitMaxMs = 10000;
    public int socketTimeoutMs = 30 * 1000;
    public int socketReceiveBufferBytes = 64*1024;
    public long startOffsetTime = -1;
    public boolean fromBeginning = false;
    public String clientId;
    public boolean resetOffsetIfOutOfRange = false;
    
    public KafkaSpoutConfig(Properties properties) {
        topic = properties.getProperty("kafka.topic", "jstorm");
        String hoststrs = properties.getProperty("kafka.broker.hosts", "127.0.0.1:9092");
        String[] hostarr = hoststrs.split(",");
        hosts = convertHosts(Arrays.asList(hostarr));
        partitionsPerBroker = JStormUtils.parseInt(properties.getProperty("kafka.broker.partitions"), 1);
        fetchMaxBytes = JStormUtils.parseInt(properties.getProperty("kafka.fetch.max.bytes"), 1024*1024);
        fetchWaitMaxMs = JStormUtils.parseInt(properties.getProperty("kafka.fetch.wait.max.ms"), 10000);
        socketTimeoutMs = JStormUtils.parseInt(properties.getProperty("kafka.socket.timeout.ms"), 30 * 1000);
        socketReceiveBufferBytes = JStormUtils.parseInt(properties.getProperty("kafka.socket.receive.buffer.bytes"), 64*1024);
        fromBeginning = JStormUtils.parseBoolean(properties.getProperty("kafka.fetch.from.beginning"), false);
        startOffsetTime = JStormUtils.parseInt(properties.getProperty("kafka.start.offset.time"), -1);
        clientId = properties.getProperty("kafka.client.id", "jstorm");
    }
    
//    public KafkaSpoutConfig(Map conf) {
//        topic = Utils.parseString(conf.get("kafka.topic"), "jstorm");
//        String hoststrs = Utils.parseString(conf.get("kafka.broker.hosts"), "127.0.0.1:9092");
//        String[] hostarr = hoststrs.split(",");
//        hosts = convertHosts(Arrays.asList(hostarr));
//        partitionsPerBroker = JStormUtils.parseInt(conf.get("kafka.broker.partitions"), 1);
//        fetchMaxBytes = JStormUtils.parseInt(conf.get("kafka.fetch.max.bytes"), 1024*1024);
//        fetchWaitMaxMs = JStormUtils.parseInt(conf.get("kafka.fetch.wait.max.ms"), 10000);
//        socketTimeoutMs = JStormUtils.parseInt(conf.get("kafka.socket.timeout.ms"), 30 * 1000);
//        socketReceiveBufferBytes = JStormUtils.parseInt(conf.get("kafka.socket.receive.buffer.bytes"), 64*1024);
//        fromBeginning = JStormUtils.parseBoolean(conf.get("kafka.fetch.from.beginning"), false);
//        startOffsetTime = JStormUtils.parseInt(conf.get("kafka.start.offset.time"), -1);
//        clientId = Utils.parseString(conf.get("kafka.client.id"), "jstorm");
//        
//    }

	public KafkaSpoutConfig( String topic, List<String> hostList) {
		hosts = convertHosts(hostList);
		this.topic = topic;
	}

	public static List<KafkaBroker> convertHosts(List<String> hostList) {
		List<KafkaBroker> brokerList = new ArrayList<KafkaBroker>();
		for (String s : hostList) {
			KafkaBroker broker;
			String[] spec = s.split(":");
			if (spec.length == 1) {
				broker = new KafkaBroker(spec[0]);
			} else if (spec.length == 2) {
				broker = new KafkaBroker(spec[0], Integer.parseInt(spec[1]));
			} else {
				throw new IllegalArgumentException("Invalid host specification: " + s);
			}
			brokerList.add(broker);
		}
		return brokerList;
	}

	public MultiScheme getScheme() {
		return scheme;
	}

	public void setScheme(MultiScheme scheme) {
		this.scheme = scheme;
	}

	public List<KafkaBroker> getHosts() {
		return hosts;
	}

	public void setHosts(List<KafkaBroker> hosts) {
		this.hosts = hosts;
	}

	public int getPartitionsPerBroker() {
		return partitionsPerBroker;
	}

	public void setPartitionsPerBroker(int partitionsPerBroker) {
		this.partitionsPerBroker = partitionsPerBroker;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	
}
