package org.apache.ambari.metrics.alertservice.spark;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ambari.metrics.alertservice.common.MetricAnomaly;
import org.apache.ambari.metrics.alertservice.common.TimelineMetric;
import org.apache.ambari.metrics.alertservice.common.TimelineMetrics;
import org.apache.ambari.metrics.alertservice.methods.ema.EmaModel;
import org.apache.ambari.metrics.alertservice.methods.MetricAnomalyModel;
import org.apache.ambari.metrics.alertservice.methods.ema.EmaModelLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaPairReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka.KafkaUtils;
import scala.Tuple2;

import java.util.*;

public class MetricAnomalyDetector {

    private static final Log LOG = LogFactory.getLog(MetricAnomalyDetector.class);
    private static String groupId = "ambari-metrics-group";
    private static String topicName = "ambari-metrics-topic";
    private static int numThreads = 1;

    //private static String zkQuorum = "avijayan-ams-1.openstacklocal:2181,avijayan-ams-2.openstacklocal:2181,avijayan-ams-3.openstacklocal:2181";
    //private static Map<String, String> kafkaParams = new HashMap<>();
    //static {
    //    kafkaParams.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "avijayan-ams-2.openstacklocal:6667");
    //    kafkaParams.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
    //    kafkaParams.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.connect.json.JsonSerializer");
    //    kafkaParams.put("metadata.broker.list", "avijayan-ams-2.openstacklocal:6667");
    //}

    public MetricAnomalyDetector() {
    }

    public static void main(String[] args) throws InterruptedException {


        if (args.length < 6) {
            System.err.println("Usage: MetricAnomalyDetector <method1,method2> <appid1,appid2> <collector_host> <port> <protocol> <zkQuorum>");
            System.exit(1);
        }

        List<String> appIds = Arrays.asList(args[1].split(","));
        String collectorHost = args[2];
        String collectorPort = args[3];
        String collectorProtocol = args[4];
        String zkQuorum = args[5];

        List<MetricAnomalyModel> anomalyDetectionModels = new ArrayList<>();
        AnomalyMetricPublisher anomalyMetricPublisher = new AnomalyMetricPublisher(collectorHost, collectorProtocol, collectorPort);

        SparkConf sparkConf = new SparkConf().setAppName("AmbariMetricsAnomalyDetector");

        JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, new Duration(10000));

        for (String method : args[0].split(",")) {
            if (method.equals("ema")) {
                LOG.info("Model EMA requested.");
                EmaModel emaModel = new EmaModelLoader().load(jssc.sparkContext().sc(), "/tmp/model/ema");
                anomalyDetectionModels.add(emaModel);
            }
        }

        JavaPairReceiverInputDStream<String, String> messages =
                KafkaUtils.createStream(jssc, zkQuorum, groupId, Collections.singletonMap(topicName, numThreads));

        //Convert JSON string to TimelineMetrics.
        JavaDStream<TimelineMetrics> timelineMetricsStream = messages.map(new Function<Tuple2<String, String>, TimelineMetrics>() {
            @Override
            public TimelineMetrics call(Tuple2<String, String> message) throws Exception {
                LOG.info(message._2());
                ObjectMapper mapper = new ObjectMapper();
                TimelineMetrics metrics = mapper.readValue(message._2, TimelineMetrics.class);
                return metrics;
            }
        });

        //Group TimelineMetric by AppId.
        JavaPairDStream<String, TimelineMetrics> appMetricStream = timelineMetricsStream.mapToPair(
                timelineMetrics -> new Tuple2<String, TimelineMetrics>(timelineMetrics.getMetrics().get(0).getAppId(),timelineMetrics)
        );

        appMetricStream.print();

        //Filter AppIds that are not needed.
        JavaPairDStream<String, TimelineMetrics> filteredAppMetricStream = appMetricStream.filter(new Function<Tuple2<String, TimelineMetrics>, Boolean>() {
            @Override
            public Boolean call(Tuple2<String, TimelineMetrics> appMetricTuple) throws Exception {
                return appIds.contains(appMetricTuple._1);
            }
        });

        filteredAppMetricStream.print();

        filteredAppMetricStream.foreachRDD(rdd -> {
            rdd.foreach(
                    tuple2 -> {
                        TimelineMetrics metrics = tuple2._2();
                        LOG.info("Received Metric : " + metrics.getMetrics().get(0).getMetricName());
                        for (TimelineMetric metric : metrics.getMetrics()) {

                            TimelineMetric timelineMetric =
                                    new TimelineMetric(metric.getMetricName(), metric.getAppId(), metric.getHostName(), metric.getMetricValues());
                            LOG.info("Models size : " + anomalyDetectionModels.size());

                            for (MetricAnomalyModel model : anomalyDetectionModels) {
                                LOG.info("Testing against Model : " + model.getClass().getCanonicalName());
                                List<MetricAnomaly> anomalies = model.test(timelineMetric);
                                anomalyMetricPublisher.publish(anomalies);
                                for (MetricAnomaly anomaly : anomalies) {
                                    LOG.info(anomaly.getAnomalyAsString());
                                }

                            }
                        }
                    });
        });

        jssc.start();
        jssc.awaitTermination();
    }
}




