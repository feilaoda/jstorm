package com.alipay.dw.jstorm.example.sequence;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.thrift7.server.THsHaServer.Args;
import org.yaml.snakeyaml.Yaml;

import backtype.storm.Config;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.generated.TopologyAssignException;
import backtype.storm.topology.BoltDeclarer;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;

import com.alibaba.jstorm.cluster.StormConfig;
import com.alibaba.jstorm.kafka.KafkaSpoutConfig;
import com.alibaba.jstorm.local.LocalCluster;
import com.alibaba.jstorm.utils.JStormUtils;
import com.alipay.dw.jstorm.example.sequence.bean.Pair;
import com.alipay.dw.jstorm.example.sequence.bean.TradeCustomer;
import com.alipay.dw.jstorm.example.sequence.bolt.MergeRecord;
import com.alipay.dw.jstorm.example.sequence.bolt.PairCount;
import com.alipay.dw.jstorm.example.sequence.bolt.PrintBolt;
import com.alipay.dw.jstorm.example.sequence.bolt.SplitRecord;
import com.alipay.dw.jstorm.example.sequence.bolt.TotalCount;
import com.alipay.dw.jstorm.example.sequence.spout.MyKafkaSpout;
import com.alipay.dw.jstorm.example.sequence.spout.SequenceSpout;

public class SequenceTopology {

	private final static String TOPOLOGY_SPOUT_PARALLELISM_HINT = "spout.parallel";
	private final static String TOPOLOGY_BOLT_PARALLELISM_HINT = "bolt.parallel";

	public static void SetBuilder(TopologyBuilder builder, Map conf) {

		int spout_Parallelism_hint = JStormUtils.parseInt(
				conf.get(TOPOLOGY_SPOUT_PARALLELISM_HINT), 1);
		int bolt_Parallelism_hint = JStormUtils.parseInt(
				conf.get(TOPOLOGY_BOLT_PARALLELISM_HINT), 2);

//		Properties p = new Properties();
//        p.setProperty("zookeeper.connect", "127.0.0.1:2181");
//        p.setProperty("kafka.broker.hosts", "127.0.0.1:9092");
//        p.setProperty("kafka.topic", "jstorm3");
//        p.setProperty("kafka.fetch.from.beginning", "false");
//        p.setProperty("kafka.broker.partitions", "3");
//        p.setProperty("kafka.fetch.max.bytes", "200");
//        
        KafkaSpoutConfig config = new KafkaSpoutConfig();
        
		builder.setSpout(SequenceTopologyDef.SEQUENCE_SPOUT_NAME,
				new MyKafkaSpout(config), spout_Parallelism_hint);

		boolean isEnableSplit = JStormUtils.parseBoolean(
				conf.get("enable.split"), false);

		builder.setBolt("kafka-bolt",
                new PrintBolt(), bolt_Parallelism_hint)
                .localOrShuffleGrouping(
                        SequenceTopologyDef.SEQUENCE_SPOUT_NAME);
		
//		
//		if (isEnableSplit == false) {
//			BoltDeclarer boltDeclarer = builder.setBolt(
//					SequenceTopologyDef.TOTAL_BOLT_NAME, new TotalCount(),
//					bolt_Parallelism_hint);
//
//			// localFirstGrouping is only for jstorm
//			// boltDeclarer.localFirstGrouping(SequenceTopologyDef.SEQUENCE_SPOUT_NAME);
//			boltDeclarer
//					.localOrShuffleGrouping(SequenceTopologyDef.SEQUENCE_SPOUT_NAME);
//		} else {
//
//			builder.setBolt(SequenceTopologyDef.SPLIT_BOLT_NAME,
//					new SplitRecord(), bolt_Parallelism_hint)
//					.localOrShuffleGrouping(
//							SequenceTopologyDef.SEQUENCE_SPOUT_NAME);
//
//			builder.setBolt(SequenceTopologyDef.TRADE_BOLT_NAME,
//					new PairCount(), bolt_Parallelism_hint).shuffleGrouping(
//					SequenceTopologyDef.SPLIT_BOLT_NAME,
//					SequenceTopologyDef.TRADE_STREAM_ID);
//			builder.setBolt(SequenceTopologyDef.CUSTOMER_BOLT_NAME,
//					new PairCount(), bolt_Parallelism_hint).shuffleGrouping(
//					SequenceTopologyDef.SPLIT_BOLT_NAME,
//					SequenceTopologyDef.CUSTOMER_STREAM_ID);
//
//			builder.setBolt(SequenceTopologyDef.MERGE_BOLT_NAME,
//					new MergeRecord(), bolt_Parallelism_hint)
//					.fieldsGrouping(SequenceTopologyDef.TRADE_BOLT_NAME,
//							new Fields("ID"))
//					.fieldsGrouping(SequenceTopologyDef.CUSTOMER_BOLT_NAME,
//							new Fields("ID"));
//
//			builder.setBolt(SequenceTopologyDef.TOTAL_BOLT_NAME,
//					new TotalCount(), bolt_Parallelism_hint).noneGrouping(
//					SequenceTopologyDef.MERGE_BOLT_NAME);
//		}

		boolean kryoEnable = JStormUtils.parseBoolean(conf.get("kryo.enable"),
				false);
		if (kryoEnable == true) {
			System.out.println("Use Kryo ");
			boolean useJavaSer = JStormUtils.parseBoolean(
					conf.get("fall.back.on.java.serialization"), true);

			Config.setFallBackOnJavaSerialization(conf, useJavaSer);

			Config.registerSerialization(conf, TradeCustomer.class);
			Config.registerSerialization(conf, Pair.class);
		}

		// conf.put(Config.TOPOLOGY_DEBUG, false);
		// conf.put(ConfigExtension.TOPOLOGY_DEBUG_RECV_TUPLE, false);
		// conf.put(Config.STORM_LOCAL_MODE_ZMQ, false);

		int ackerNum = JStormUtils.parseInt(
				conf.get(Config.TOPOLOGY_ACKER_EXECUTORS), 1);
		Config.setNumAckers(conf, ackerNum);
		// conf.put(Config.TOPOLOGY_MAX_TASK_PARALLELISM, 6);
		// conf.put(Config.TOPOLOGY_MESSAGE_TIMEOUT_SECS, 20);
		// conf.put(Config.TOPOLOGY_MAX_SPOUT_PENDING, 1);

		int workerNum = JStormUtils.parseInt(conf.get(Config.TOPOLOGY_WORKERS),
				20);
		conf.put(Config.TOPOLOGY_WORKERS, workerNum);

	}

	public static void SetLocalTopology() throws Exception {
		TopologyBuilder builder = new TopologyBuilder();

		conf.put(TOPOLOGY_BOLT_PARALLELISM_HINT, Integer.valueOf(1));

		SetBuilder(builder, conf);

		LocalCluster cluster = new LocalCluster();
		cluster.submitTopology("JStorm-Kafka", conf, builder.createTopology());

		Thread.sleep(600000);

		cluster.shutdown();
	}

	public static void SetRemoteTopology() throws AlreadyAliveException,
			InvalidTopologyException, TopologyAssignException {

		String streamName = (String) conf.get(Config.TOPOLOGY_NAME);
		if (streamName == null) {
			streamName = "SequenceTest";
		}

		TopologyBuilder builder = new TopologyBuilder();

		SetBuilder(builder, conf);

		conf.put(Config.STORM_CLUSTER_MODE, "distributed");

		StormSubmitter.submitTopology(streamName, conf,
				builder.createTopology());

	}

	public static void SetDPRCTopology() throws AlreadyAliveException,
			InvalidTopologyException, TopologyAssignException {
		// LinearDRPCTopologyBuilder builder = new LinearDRPCTopologyBuilder(
		// "exclamation");
		//
		// builder.addBolt(new TotalCount(), 3);
		//
		// Config conf = new Config();
		//
		// conf.setNumWorkers(3);
		// StormSubmitter.submitTopology("rpc", conf,
		// builder.createRemoteTopology());
		System.out
				.println("Please refer to com.alipay.dw.jstorm.example.drpc.ReachTopology");
	}

	private static Map conf = new HashMap<Object, Object>();

	private static void LoadProperty(String prop) {
		Properties properties = new Properties();

		try {
			InputStream stream = new FileInputStream(prop);
			properties.load(stream);
		} catch (FileNotFoundException e) {
			System.out.println("No such file " + prop);
		} catch (Exception e1) {
			e1.printStackTrace();

			return;
		}

		conf.putAll(properties);
	}

	private static void LoadYaml(String confPath) {

		Yaml yaml = new Yaml();

		try {
			InputStream stream = new FileInputStream(confPath);

			conf = (Map) yaml.load(stream);
			if (conf == null || conf.isEmpty() == true) {
				throw new RuntimeException("Failed to read config file");
			}

		} catch (FileNotFoundException e) {
			System.out.println("No such file " + confPath);
			throw new RuntimeException("No config file");
		} catch (Exception e1) {
			e1.printStackTrace();
			throw new RuntimeException("Failed to read config file");
		}

		return;
	}

	private static void LoadConf(String arg) {
		if (arg.endsWith("yaml")) {
			LoadYaml(arg);
		} else {
			LoadProperty(arg);
		}
	}

	public static boolean local_mode(Map conf) {
		String mode = (String) conf.get(Config.STORM_CLUSTER_MODE);
		if (mode != null) {
			if (mode.equals("local")) {
				return true;
			}
		}

		return false;

	}

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.err.println("Please input configuration file");
			System.exit(-1);
		}

		LoadConf(args[0]);

		if (local_mode(conf)) {
			SetLocalTopology();
		} else {
			SetRemoteTopology();
		}

	}

}
