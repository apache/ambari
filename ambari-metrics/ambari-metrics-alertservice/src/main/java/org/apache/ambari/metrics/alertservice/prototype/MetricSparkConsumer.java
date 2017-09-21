/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.metrics.alertservice.prototype;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ambari.metrics.alertservice.prototype.methods.MetricAnomaly;
import org.apache.ambari.metrics.alertservice.prototype.methods.ema.EmaTechnique;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaPairReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka.KafkaUtils;
import scala.Tuple2;

import java.util.*;

public class MetricSparkConsumer {

  private static final Log LOG = LogFactory.getLog(MetricSparkConsumer.class);
  private static String groupId = "ambari-metrics-group";
  private static String topicName = "ambari-metrics-topic";
  private static int numThreads = 1;
  private static long pitStartTime = System.currentTimeMillis();
  private static long ksStartTime = pitStartTime;
  private static long hdevStartTime = ksStartTime;

  public MetricSparkConsumer() {
  }

  public static void main(String[] args) throws InterruptedException {

    if (args.length < 5) {
      System.err.println("Usage: MetricSparkConsumer <appid1,appid2> <collector_host> <port> <protocol> <zkQuorum>");
      System.exit(1);
    }

    List<String> appIds = Arrays.asList(args[0].split(","));
    String collectorHost = args[1];
    String collectorPort = args[2];
    String collectorProtocol = args[3];
    String zkQuorum = args[4];

    double emaW = StringUtils.isNotEmpty(args[5]) ? Double.parseDouble(args[5]) : 0.5;
    double emaN = StringUtils.isNotEmpty(args[8]) ? Double.parseDouble(args[6]) : 3;
    double tukeysN = StringUtils.isNotEmpty(args[7]) ? Double.parseDouble(args[7]) : 3;

    long pitTestInterval = StringUtils.isNotEmpty(args[8]) ? Long.parseLong(args[8]) : 5 * 60 * 1000;
    long pitTrainInterval = StringUtils.isNotEmpty(args[9]) ? Long.parseLong(args[9]) : 15 * 60 * 1000;

    String fileName = args[10];
    long ksTestInterval = StringUtils.isNotEmpty(args[11]) ? Long.parseLong(args[11]) : 10 * 60 * 1000;
    long ksTrainInterval = StringUtils.isNotEmpty(args[12]) ? Long.parseLong(args[12]) : 10 * 60 * 1000;
    int hsdevNhp = StringUtils.isNotEmpty(args[13]) ? Integer.parseInt(args[13]) : 3;
    long hsdevInterval = StringUtils.isNotEmpty(args[14]) ? Long.parseLong(args[14]) : 30 * 60 * 1000;

    String ambariServerHost = args[15];
    String clusterName = args[16];

    MetricsCollectorInterface metricsCollectorInterface = new MetricsCollectorInterface(collectorHost, collectorProtocol, collectorPort);

    SparkConf sparkConf = new SparkConf().setAppName("AmbariMetricsAnomalyDetector");

    JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, new Duration(10000));

    EmaTechnique emaTechnique = new EmaTechnique(emaW, emaN);
    PointInTimeADSystem pointInTimeADSystem = new PointInTimeADSystem(metricsCollectorInterface,
      tukeysN,
      pitTestInterval,
      pitTrainInterval,
      ambariServerHost,
      clusterName);

    TrendADSystem trendADSystem = new TrendADSystem(metricsCollectorInterface,
      ksTestInterval,
      ksTrainInterval,
      hsdevNhp,
      fileName);

    Broadcast<EmaTechnique> emaTechniqueBroadcast = jssc.sparkContext().broadcast(emaTechnique);
    Broadcast<PointInTimeADSystem> pointInTimeADSystemBroadcast = jssc.sparkContext().broadcast(pointInTimeADSystem);
    Broadcast<TrendADSystem> trendADSystemBroadcast = jssc.sparkContext().broadcast(trendADSystem);
    Broadcast<MetricsCollectorInterface> metricsCollectorInterfaceBroadcast = jssc.sparkContext().broadcast(metricsCollectorInterface);

    JavaPairReceiverInputDStream<String, String> messages =
      KafkaUtils.createStream(jssc, zkQuorum, groupId, Collections.singletonMap(topicName, numThreads));

    //Convert JSON string to TimelineMetrics.
    JavaDStream<TimelineMetrics> timelineMetricsStream = messages.map(new Function<Tuple2<String, String>, TimelineMetrics>() {
      @Override
      public TimelineMetrics call(Tuple2<String, String> message) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        TimelineMetrics metrics = mapper.readValue(message._2, TimelineMetrics.class);
        return metrics;
      }
    });

    timelineMetricsStream.print();

    //Group TimelineMetric by AppId.
    JavaPairDStream<String, TimelineMetrics> appMetricStream = timelineMetricsStream.mapToPair(
      timelineMetrics -> timelineMetrics.getMetrics().isEmpty()  ?  new Tuple2<>("TEST", new TimelineMetrics()) : new Tuple2<String, TimelineMetrics>(timelineMetrics.getMetrics().get(0).getAppId(), timelineMetrics)
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
          long currentTime = System.currentTimeMillis();
          EmaTechnique ema = emaTechniqueBroadcast.getValue();
          if (currentTime > pitStartTime + pitTestInterval) {
            LOG.info("Running Tukeys....");
            pointInTimeADSystemBroadcast.getValue().runTukeysAndRefineEma(ema, currentTime);
            pitStartTime = pitStartTime + pitTestInterval;
          }

          if (currentTime > ksStartTime + ksTestInterval) {
            LOG.info("Running KS Test....");
            trendADSystemBroadcast.getValue().runKSTest(currentTime);
            ksStartTime = ksStartTime + ksTestInterval;
          }

          if (currentTime > hdevStartTime + hsdevInterval) {
            LOG.info("Running HSdev Test....");
            trendADSystemBroadcast.getValue().runHsdevMethod();
            hdevStartTime = hdevStartTime + hsdevInterval;
          }

          TimelineMetrics metrics = tuple2._2();
          for (TimelineMetric timelineMetric : metrics.getMetrics()) {
            List<MetricAnomaly> anomalies = ema.test(timelineMetric);
            metricsCollectorInterfaceBroadcast.getValue().publish(anomalies);
          }
        });
    });

    jssc.start();
    jssc.awaitTermination();
  }
}




