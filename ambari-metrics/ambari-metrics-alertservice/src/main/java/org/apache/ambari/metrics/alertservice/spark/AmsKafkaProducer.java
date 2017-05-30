package org.apache.ambari.metrics.alertservice.spark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ambari.metrics.alertservice.common.TimelineMetric;
import org.apache.ambari.metrics.alertservice.common.TimelineMetrics;
import org.apache.kafka.clients.producer.*;

import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class AmsKafkaProducer {

    Producer producer;
    private static String topicName = "ambari-metrics-topic";

    public AmsKafkaProducer(String kafkaServers) {
        Properties configProperties = new Properties();
        configProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers); //"avijayan-ams-2.openstacklocal:6667"
        configProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.ByteArraySerializer");
        configProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.connect.json.JsonSerializer");
        producer = new KafkaProducer(configProperties);
    }

    public void sendMetrics(TimelineMetrics timelineMetrics) throws InterruptedException, ExecutionException {

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.valueToTree(timelineMetrics);
        ProducerRecord<String, JsonNode> rec = new ProducerRecord<String, JsonNode>(topicName,jsonNode);
        Future<RecordMetadata> kafkaFuture =  producer.send(rec);

        System.out.println(kafkaFuture.isDone());
        System.out.println(kafkaFuture.get().topic());
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        final long now = System.currentTimeMillis();

        TimelineMetrics timelineMetrics = new TimelineMetrics();
        TimelineMetric metric1 = new TimelineMetric();
        metric1.setMetricName("mem_free");
        metric1.setHostName("avijayan-ams-3.openstacklocal");
        metric1.setTimestamp(now);
        metric1.setStartTime(now - 1000);
        metric1.setAppId("HOST");
        metric1.setType("Integer");

        TreeMap<Long, Double> metricValues = new TreeMap<Long, Double>();

        for (int i = 0; i<20;i++) {
            double metric = 20000 + Math.random();
            metricValues.put(now - i*100, metric);
        }

        metric1.setMetricValues(metricValues);

//        metric1.setMetricValues(new TreeMap<Long, Double>() {{
//            put(now - 100, 1.20);
//            put(now - 200, 11.25);
//            put(now - 300, 1.30);
//            put(now - 400, 4.50);
//            put(now - 500, 16.35);
//            put(now - 400, 5.50);
//        }});

        timelineMetrics.getMetrics().add(metric1);

        for (int i = 0; i<1; i++) {
            new AmsKafkaProducer("avijayan-ams-2.openstacklocal:6667").sendMetrics(timelineMetrics);
            Thread.sleep(1000);
        }
    }
}
