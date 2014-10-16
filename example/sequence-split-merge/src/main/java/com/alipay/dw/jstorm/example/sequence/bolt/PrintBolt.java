package com.alipay.dw.jstorm.example.sequence.bolt;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.IBasicBolt;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;

public class PrintBolt implements IRichBolt{
    private OutputCollector  collector;

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        // TODO Auto-generated method stub
        this.collector = collector;
    }

    @Override
    public void execute(Tuple input) {
        // TODO Auto-generated method stub
        byte[] bytes = input.getBinary(0);
        try {
            String message = new String(bytes, "UTF-8");
            System.out.println("======= bolt out =======" + message);
            System.out.println(input);
            collector.ack(input);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

    @Override
    public void cleanup() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Map<String, Object> getComponentConfiguration() {
        // TODO Auto-generated method stub
        return null;
    }


}
