/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ambari.logfeeder.output;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedTransferQueue;

import org.apache.ambari.logfeeder.conf.LogFeederProps;
import org.apache.ambari.logfeeder.input.InputMarker;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class OutputKafka extends Output {
  private static final Logger LOG = Logger.getLogger(OutputKafka.class);

  private static final int FAILED_RETRY_INTERVAL = 30;
  private static final int CATCHUP_RETRY_INTERVAL = 5;

  private static final int DEFAULT_BATCH_SIZE = 5000;
  private static final int DEFAULT_LINGER_MS = 1000;

  private String topic = null;
  private boolean isAsync = true;
  private long messageCount = 0;

  private KafkaProducer<String, String> producer = null;
  private BlockingQueue<KafkaCallBack> failedMessages = new LinkedTransferQueue<KafkaCallBack>();

  // Let's start with the assumption Kafka is down
  private boolean isKafkaBrokerUp = false;

  @Override
  protected String getStatMetricName() {
    return "output.kafka.write_logs";
  }

  @Override
  protected String getWriteBytesMetricName() {
    return "output.kafka.write_bytes";
  }
  
  @Override
  public void init(LogFeederProps logFeederProps) throws Exception {
    super.init(logFeederProps);
    Properties props = initProperties();

    producer = creteKafkaProducer(props);
    createKafkaRetryThread();
  }

  private Properties initProperties() throws Exception {
    String brokerList = getStringValue("broker_list");
    if (StringUtils.isEmpty(brokerList)) {
      throw new Exception("For kafka output, bootstrap broker_list is needed");
    }

    topic = getStringValue("topic");
    if (StringUtils.isEmpty(topic)) {
      throw new Exception("For kafka output, topic is needed");
    }

    isAsync = getBooleanValue("is_async", true);
    int batchSize = getIntValue("batch_size", DEFAULT_BATCH_SIZE);
    int lingerMS = getIntValue("linger_ms", DEFAULT_LINGER_MS);

    Properties props = new Properties();
    props.put("bootstrap.servers", brokerList);
    props.put("client.id", "logfeeder_producer");
    props.put("key.serializer", StringSerializer.class.getName());
    props.put("value.serializer", StringSerializer.class.getName());
    props.put("compression.type", "snappy");
    props.put("batch.size", batchSize);
    props.put("linger.ms", lingerMS);

    for (String key : configs.keySet()) {
      if (key.startsWith("kafka.")) {
        Object value = configs.get(key);
        if (value == null || value.toString().length() == 0) {
          continue;
        }
        String kafkaKey = key.substring("kafka.".length());
        LOG.info("Adding custom Kafka property. " + kafkaKey + "=" + value);
        props.put(kafkaKey, value);
      }
    }

    return props;
  }

  protected KafkaProducer<String, String> creteKafkaProducer(Properties props) {
    return new KafkaProducer<String, String>(props);
  }

  private void createKafkaRetryThread() {
    Thread retryThread = new Thread("kafka-writer-retry,topic=" + topic) {
      @Override
      public void run() {
        KafkaCallBack kafkaCallBack = null;
        LOG.info("Started thread to monitor failed messsages. " + getShortDescription());
        while (true) {
          try {
            if (kafkaCallBack == null) {
              kafkaCallBack = failedMessages.take();
            }
            if (publishMessage(kafkaCallBack.message, kafkaCallBack.inputMarker)) {
              kafkaCallBack = null;
            } else {
              LOG.error("Kafka is down. messageNumber=" + kafkaCallBack.thisMessageNumber + ". Going to sleep for " +
                  FAILED_RETRY_INTERVAL + " seconds");
              Thread.sleep(FAILED_RETRY_INTERVAL * 1000);
            }

          } catch (Throwable t) {
            String logMessageKey = this.getClass().getSimpleName() + "_KAFKA_RETRY_WRITE_ERROR";
            LogFeederUtil.logErrorMessageByInterval(logMessageKey, "Error sending message to Kafka during retry. message=" +
                (kafkaCallBack == null ? null : kafkaCallBack.message), t, LOG, Level.ERROR);
          }
        }

      }
    };
    retryThread.setDaemon(true);
    retryThread.start();
  }

  @Override
  public synchronized void write(String block, InputMarker inputMarker) throws Exception {
    while (!isDrain() && !inputMarker.input.isDrain()) {
      try {
        if (failedMessages.size() == 0) {
          if (publishMessage(block, inputMarker)) {
            break;
          }
        }
        if (isDrain() || inputMarker.input.isDrain()) {
          break;
        }
        if (!isKafkaBrokerUp) {
          LOG.error("Kafka is down. Going to sleep for " + FAILED_RETRY_INTERVAL + " seconds");
          Thread.sleep(FAILED_RETRY_INTERVAL * 1000);
        } else {
          LOG.warn("Kafka is still catching up from previous failed messages. outstanding messages=" + failedMessages.size() +
              " Going to sleep for " + CATCHUP_RETRY_INTERVAL + " seconds");
          Thread.sleep(CATCHUP_RETRY_INTERVAL * 1000);
        }
      } catch (Throwable t) {
        // ignore
        break;
      }
    }
  }

  @Override
  public void setDrain(boolean drain) {
    super.setDrain(drain);
  }

  public void flush() {
    LOG.info("Flush called...");
    setDrain(true);
  }

  @Override
  public void close() {
    LOG.info("Closing Kafka client...");
    flush();
    if (producer != null) {
      try {
        producer.close();
      } catch (Throwable t) {
        LOG.error("Error closing Kafka topic. topic=" + topic);
      }
    }
    LOG.info("Closed Kafka client");
    super.close();
  }

  private boolean publishMessage(String block, InputMarker inputMarker) {
    if (isAsync && isKafkaBrokerUp) { // Send asynchronously
      producer.send(new ProducerRecord<String, String>(topic, block), new KafkaCallBack(this, block, inputMarker, ++messageCount));
      return true;
    } else { // Send synchronously
      try {
        // Not using key. Let it round robin
        RecordMetadata metadata = producer.send(new ProducerRecord<String, String>(topic, block)).get();
        if (metadata != null) {
          statMetric.value++;
          writeBytesMetric.value += block.length();
        }
        if (!isKafkaBrokerUp) {
          LOG.info("Started writing to kafka. " + getShortDescription());
          isKafkaBrokerUp = true;
        }
        return true;
      } catch (InterruptedException e) {
        isKafkaBrokerUp = false;
        String logKeyMessage = this.getClass().getSimpleName() + "_KAFKA_INTERRUPT";
        LogFeederUtil.logErrorMessageByInterval(logKeyMessage, "InterruptedException-Error sending message to Kafka", e, LOG,
            Level.ERROR);
      } catch (ExecutionException e) {
        isKafkaBrokerUp = false;
        String logKeyMessage = this.getClass().getSimpleName() + "_KAFKA_EXECUTION";
        LogFeederUtil.logErrorMessageByInterval(logKeyMessage, "ExecutionException-Error sending message to Kafka", e, LOG,
            Level.ERROR);
      } catch (Throwable t) {
        isKafkaBrokerUp = false;
        String logKeyMessage = this.getClass().getSimpleName() + "_KAFKA_WRITE_ERROR";
        LogFeederUtil.logErrorMessageByInterval(logKeyMessage, "GenericException-Error sending message to Kafka", t, LOG,
            Level.ERROR);
      }
    }
    return false;
  }

  @Override
  public String getShortDescription() {
    return "output:destination=kafka,topic=" + topic;
  }

  class KafkaCallBack implements Callback {

    private long thisMessageNumber;
    private OutputKafka output = null;
    private String message;
    private InputMarker inputMarker;

    public KafkaCallBack(OutputKafka output, String message, InputMarker inputMarker, long messageCount) {
      this.thisMessageNumber = messageCount;
      this.output = output;
      this.inputMarker = inputMarker;
      this.message = message;
    }

    public void onCompletion(RecordMetadata metadata, Exception exception) {
      if (metadata != null) {
        if (!output.isKafkaBrokerUp) {
          LOG.info("Started writing to kafka. " + output.getShortDescription());
          output.isKafkaBrokerUp = true;
        }
        output.incrementStat(1);
        output.writeBytesMetric.value += message.length();
      } else {
        output.isKafkaBrokerUp = false;
        String logKeyMessage = this.getClass().getSimpleName() + "_KAFKA_ASYNC_ERROR";
        LogFeederUtil.logErrorMessageByInterval(logKeyMessage, "Error sending message to Kafka. Async Callback", exception, LOG,
            Level.ERROR);

        output.failedMessages.add(this);
      }
    }
  }

  @Override
  public void copyFile(File inputFile, InputMarker inputMarker) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("copyFile method is not yet supported for output=kafka");
  }
}