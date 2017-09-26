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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetricSparkConsumer {

  private static final Log LOG = LogFactory.getLog(MetricSparkConsumer.class);
  private static String groupId = "ambari-metrics-group";
  private static String topicName = "ambari-metrics-topic";
  private static int numThreads = 1;
  private static long pitStartTime = System.currentTimeMillis();
  private static long ksStartTime = pitStartTime;
  private static long hdevStartTime = ksStartTime;
  private static Set<Pattern> includeMetricPatterns = new HashSet<>();
  private static Set<String> includedHosts = new HashSet<>();
  private static Set<TrendMetric> trendMetrics = new HashSet<>();

  public MetricSparkConsumer() {
  }

  public static Properties readProperties(String propertiesFile) {
    try {
      Properties properties = new Properties();
      InputStream inputStream = ClassLoader.getSystemResourceAsStream(propertiesFile);
      if (inputStream == null) {
        inputStream = new FileInputStream(propertiesFile);
      }
      properties.load(inputStream);
      return properties;
    } catch (IOException ioEx) {
      LOG.error("Error reading properties file for jmeter");
      return null;
    }
  }

  public static void main(String[] args) throws InterruptedException {

    if (args.length < 1) {
      System.err.println("Usage: MetricSparkConsumer <input-config-file>");
      System.exit(1);
    }

    Properties properties = readProperties(args[0]);

    List<String> appIds = Arrays.asList(properties.getProperty("appIds").split(","));

    String collectorHost = properties.getProperty("collectorHost");
    String collectorPort = properties.getProperty("collectorPort");
    String collectorProtocol = properties.getProperty("collectorProtocol");

    String zkQuorum = properties.getProperty("zkQuorum");

    double emaW = Double.parseDouble(properties.getProperty("emaW"));
    double emaN = Double.parseDouble(properties.getProperty("emaN"));
    int emaThreshold = Integer.parseInt(properties.getProperty("emaThreshold"));
    double tukeysN = Double.parseDouble(properties.getProperty("tukeysN"));

    long pitTestInterval = Long.parseLong(properties.getProperty("pointInTimeTestInterval"));
    long pitTrainInterval = Long.parseLong(properties.getProperty("pointInTimeTrainInterval"));

    long ksTestInterval = Long.parseLong(properties.getProperty("ksTestInterval"));
    long ksTrainInterval = Long.parseLong(properties.getProperty("ksTrainInterval"));
    int hsdevNhp = Integer.parseInt(properties.getProperty("hsdevNhp"));
    long hsdevInterval = Long.parseLong(properties.getProperty("hsdevInterval"));

    String ambariServerHost = properties.getProperty("ambariServerHost");
    String clusterName = properties.getProperty("clusterName");

    String includeMetricPatternStrings = properties.getProperty("includeMetricPatterns");
    if (includeMetricPatternStrings != null && !includeMetricPatternStrings.isEmpty()) {
      String[] patterns = includeMetricPatternStrings.split(",");
      for (String p : patterns) {
        LOG.info("Included Pattern : " + p);
        includeMetricPatterns.add(Pattern.compile(p));
      }
    }

    String includedHostList = properties.getProperty("hosts");
    if (includedHostList != null && !includedHostList.isEmpty()) {
      String[] hosts = includedHostList.split(",");
      includedHosts.addAll(Arrays.asList(hosts));
    }

    MetricsCollectorInterface metricsCollectorInterface = new MetricsCollectorInterface(collectorHost, collectorProtocol, collectorPort);

    SparkConf sparkConf = new SparkConf().setAppName("AmbariMetricsAnomalyDetector");

    JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, new Duration(10000));

    EmaTechnique emaTechnique = new EmaTechnique(emaW, emaN, emaThreshold);
    PointInTimeADSystem pointInTimeADSystem = new PointInTimeADSystem(metricsCollectorInterface,
      tukeysN,
      pitTestInterval,
      pitTrainInterval,
      ambariServerHost,
      clusterName);

    TrendADSystem trendADSystem = new TrendADSystem(metricsCollectorInterface,
      ksTestInterval,
      ksTrainInterval,
      hsdevNhp);

    Broadcast<EmaTechnique> emaTechniqueBroadcast = jssc.sparkContext().broadcast(emaTechnique);
    Broadcast<PointInTimeADSystem> pointInTimeADSystemBroadcast = jssc.sparkContext().broadcast(pointInTimeADSystem);
    Broadcast<TrendADSystem> trendADSystemBroadcast = jssc.sparkContext().broadcast(trendADSystem);
    Broadcast<MetricsCollectorInterface> metricsCollectorInterfaceBroadcast = jssc.sparkContext().broadcast(metricsCollectorInterface);
    Broadcast<Set<Pattern>> includePatternBroadcast = jssc.sparkContext().broadcast(includeMetricPatterns);
    Broadcast<Set<String>> includedHostBroadcast = jssc.sparkContext().broadcast(includedHosts);

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
            trendADSystemBroadcast.getValue().runKSTest(currentTime, trendMetrics);
            ksStartTime = ksStartTime + ksTestInterval;
          }

          if (currentTime > hdevStartTime + hsdevInterval) {
            LOG.info("Running HSdev Test....");
            trendADSystemBroadcast.getValue().runHsdevMethod();
            hdevStartTime = hdevStartTime + hsdevInterval;
          }

          TimelineMetrics metrics = tuple2._2();
          for (TimelineMetric timelineMetric : metrics.getMetrics()) {

            boolean includeHost = includedHostBroadcast.getValue().contains(timelineMetric.getHostName());
            boolean includeMetric = false;
            if (includeHost) {
              if (includePatternBroadcast.getValue().isEmpty()) {
                includeMetric = true;
              }
              for (Pattern p : includePatternBroadcast.getValue()) {
                Matcher m = p.matcher(timelineMetric.getMetricName());
                if (m.find()) {
                  includeMetric = true;
                }
              }
            }

            if (includeMetric) {
              trendMetrics.add(new TrendMetric(timelineMetric.getMetricName(), timelineMetric.getAppId(),
                timelineMetric.getHostName()));
              List<MetricAnomaly> anomalies = ema.test(timelineMetric);
              metricsCollectorInterfaceBroadcast.getValue().publish(anomalies);
            }
          }
        });
    });

    jssc.start();
    jssc.awaitTermination();
  }
}




