package com.alibaba.jstorm.kafka;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.alibaba.jstorm.kafka.PartitionConsumer.EmitState;

import backtype.storm.spout.MultiScheme;
import backtype.storm.spout.RawMultiScheme;
import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichSpout;
import backtype.storm.topology.OutputFieldsDeclarer;

public class KafkaSpout implements IRichSpout {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(KafkaSpout.class);

	protected SpoutOutputCollector collector;
	
	
	PartitionCoordinator coordinator;
	
	private KafkaSpoutConfig config;
	
	public KafkaSpout(KafkaSpoutConfig config) {
		this.config = config;
	}
	
	@Override
	public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
		// TODO Auto-generated method stub
		this.collector = collector;
		coordinator = new PartitionCoordinator(config, context);
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public void activate() {
		// TODO Auto-generated method stub

	}

	@Override
	public void deactivate() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void nextTuple() {
		Collection<PartitionConsumer> partitionConsumers = coordinator.getPartitionConsumers();
		for(PartitionConsumer consumer: partitionConsumers) {
			EmitState state = consumer.emit(collector);
			LOG.debug("====== partition "+ consumer.getPartition().getPartition() + " emit message state is "+state);
//			if(state != EmitState.EMIT_MORE) {
//				currentPartitionIndex  = (currentPartitionIndex+1) % consumerSize;
//			}
//			if(state != EmitState.EMIT_NONE) {
//				break;
//			}
		}
		commitState();
	}
	
	public void commitState() {
		for(PartitionConsumer consumer: coordinator.getPartitionConsumers()) {
			consumer.commitState();
        }
	}

	@Override
	public void ack(Object msgId) {
		KafkaMessageId messageId = (KafkaMessageId)msgId;
		PartitionConsumer consumer = coordinator.getConsumer(messageId.getPartition());
		consumer.ack(messageId.getOffset());
	}

	@Override
	public void fail(Object msgId) {
		KafkaMessageId messageId = (KafkaMessageId)msgId;
		PartitionConsumer consumer = coordinator.getConsumer(messageId.getPartition());
		consumer.fail(messageId.getOffset());
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
	    MultiScheme scheme = new RawMultiScheme();
		declarer.declare(scheme.getOutputFields());
	}

	@Override
	public Map<String, Object> getComponentConfiguration() {
		return null;
	}
	
	

}
