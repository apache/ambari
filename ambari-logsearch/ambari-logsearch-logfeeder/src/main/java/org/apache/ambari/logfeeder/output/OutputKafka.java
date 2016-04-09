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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedTransferQueue;

import org.apache.ambari.logfeeder.LogFeederUtil;
import org.apache.ambari.logfeeder.input.Input;
import org.apache.ambari.logfeeder.input.InputMarker;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class OutputKafka extends Output {
  static private Logger logger = Logger.getLogger(OutputKafka.class);

  String brokerList = null;
  String topic = null;
  boolean isAsync = true;
  long messageCount = 0;
  int batchSize = 5000;
  int lingerMS = 1000;

  private KafkaProducer<String, String> producer = null;
  BlockingQueue<KafkaCallBack> failedMessages = new LinkedTransferQueue<KafkaCallBack>();

  // Let's start with the assumption Kafka is down
  boolean isKafkaBrokerUp = false;

  static final int FAILED_RETRY_INTERVAL = 30;
  static final int CATCHUP_RETRY_INTERVAL = 5;

  @Override
  public void init() throws Exception {
    super.init();
    statMetric.metricsName = "output.kafka.write_logs";
    writeBytesMetric.metricsName = "output.kafka.write_bytes";

    brokerList = getStringValue("broker_list");
    topic = getStringValue("topic");
    isAsync = getBooleanValue("is_async", true);
    batchSize = getIntValue("batch_size", batchSize);
    lingerMS = getIntValue("linger_ms", lingerMS);

    Map<String, Object> kafkaCustomProperties = new HashMap<String, Object>();
    // Get all kafka custom properties
    for (String key : configs.keySet()) {
      if (key.startsWith("kafka.")) {
        Object value = configs.get(key);
        if (value == null || value.toString().length() == 0) {
          continue;
        }
        String kafkaKey = key.substring("kafka.".length());
        kafkaCustomProperties.put(kafkaKey, value);
      }
    }

    if (StringUtils.isEmpty(brokerList)) {
      throw new Exception(
        "For kafka output, bootstrap broker_list is needed");
    }

    if (StringUtils.isEmpty(topic)) {
      throw new Exception("For kafka output, topic is needed");
    }

    Properties props = new Properties();
    // 0.9.0
    props.put("bootstrap.servers", brokerList);
    props.put("client.id", "logfeeder_producer");
    props.put("key.serializer", StringSerializer.class.getName());
    props.put("value.serializer", StringSerializer.class.getName());
    props.put("compression.type", "snappy");
    // props.put("retries", "3");
    props.put("batch.size", batchSize);
    props.put("linger.ms", lingerMS);

    for (String kafkaKey : kafkaCustomProperties.keySet()) {
      logger.info("Adding custom Kafka property. " + kafkaKey + "="
        + kafkaCustomProperties.get(kafkaKey));
      props.put(kafkaKey, kafkaCustomProperties.get(kafkaKey));
    }

    // props.put("metadata.broker.list", brokerList);

    producer = new KafkaProducer<String, String>(props);
    Thread retryThread = new Thread("kafka-writer-retry,topic=" + topic) {
      @Override
      public void run() {
        KafkaCallBack kafkaCallBack = null;
        logger.info("Started thread to monitor failed messsages. "
          + getShortDescription());
        while (true) {
          try {
            if (kafkaCallBack == null) {
              kafkaCallBack = failedMessages.take();
            }
            if (publishMessage(kafkaCallBack.message,
              kafkaCallBack.inputMarker)) {
              // logger.info("Sent message. count="
              // + kafkaCallBack.thisMessageNumber);
              kafkaCallBack = null;
            } else {
              // Should wait for sometime
              logger.error("Kafka is down. messageNumber="
                + kafkaCallBack.thisMessageNumber
                + ". Going to sleep for "
                + FAILED_RETRY_INTERVAL + " seconds");
              Thread.sleep(FAILED_RETRY_INTERVAL * 1000);
            }

          } catch (Throwable t) {
            final String LOG_MESSAGE_KEY = this.getClass()
              .getSimpleName() + "_KAFKA_RETRY_WRITE_ERROR";
            LogFeederUtil.logErrorMessageByInterval(
              LOG_MESSAGE_KEY,
              "Error sending message to Kafka during retry. message="
                + (kafkaCallBack == null ? null
                : kafkaCallBack.message), t,
              logger, Level.ERROR);
          }
        }

      }
    };
    retryThread.setDaemon(true);
    retryThread.start();
  }

  @Override
  public void setDrain(boolean drain) {
    super.setDrain(drain);
  }

  /**
   * Flush document buffer
   */
  public void flush() {
    logger.info("Flush called...");
    setDrain(true);
  }

  @Override
  public void close() {
    logger.info("Closing Kafka client...");
    flush();
    if (producer != null) {
      try {
        producer.close();
      } catch (Throwable t) {
        logger.error("Error closing Kafka topic. topic=" + topic);
      }
    }
    logger.info("Closed Kafka client");
    super.close();
  }

  @Override
  synchronized public void write(String block, InputMarker inputMarker) throws Exception {
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
          logger.error("Kafka is down. Going to sleep for "
            + FAILED_RETRY_INTERVAL + " seconds");
          Thread.sleep(FAILED_RETRY_INTERVAL * 1000);

        } else {
          logger.warn("Kafka is still catching up from previous failed messages. outstanding messages="
            + failedMessages.size()
            + " Going to sleep for "
            + CATCHUP_RETRY_INTERVAL + " seconds");
          Thread.sleep(CATCHUP_RETRY_INTERVAL * 1000);
        }
      } catch (Throwable t) {
        // ignore
        break;
      }
    }
  }

  private boolean publishMessage(String block, InputMarker inputMarker) {
    if (isAsync && isKafkaBrokerUp) { // Send asynchronously
      producer.send(new ProducerRecord<String, String>(topic, block),
        new KafkaCallBack(this, block, inputMarker, ++messageCount));
      return true;
    } else { // Send synchronously
      try {
        // Not using key. Let it round robin
        RecordMetadata metadata = producer.send(
          new ProducerRecord<String, String>(topic, block)).get();
        if (metadata != null) {
          statMetric.count++;
          writeBytesMetric.count += block.length();
        }
        if (!isKafkaBrokerUp) {
          logger.info("Started writing to kafka. "
            + getShortDescription());
          isKafkaBrokerUp = true;
        }
        return true;
      } catch (InterruptedException e) {
        isKafkaBrokerUp = false;
        final String LOG_MESSAGE_KEY = this.getClass().getSimpleName()
          + "_KAFKA_INTERRUPT";
        LogFeederUtil.logErrorMessageByInterval(LOG_MESSAGE_KEY,
          "InterruptedException-Error sending message to Kafka",
          e, logger, Level.ERROR);
      } catch (ExecutionException e) {
        isKafkaBrokerUp = false;
        final String LOG_MESSAGE_KEY = this.getClass().getSimpleName()
          + "_KAFKA_EXECUTION";
        LogFeederUtil.logErrorMessageByInterval(LOG_MESSAGE_KEY,
          "ExecutionException-Error sending message to Kafka", e,
          logger, Level.ERROR);
      } catch (Throwable t) {
        isKafkaBrokerUp = false;
        final String LOG_MESSAGE_KEY = this.getClass().getSimpleName()
          + "_KAFKA_WRITE_ERROR";
        LogFeederUtil.logErrorMessageByInterval(LOG_MESSAGE_KEY,
          "GenericException-Error sending message to Kafka", t,
          logger, Level.ERROR);
      }
    }
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.ambari.logfeeder.ConfigBlock#getShortDescription()
   */
  @Override
  public String getShortDescription() {
    return "output:destination=kafka,topic=" + topic;
  }

}

class KafkaCallBack implements Callback {
  static private Logger logger = Logger.getLogger(KafkaCallBack.class);

  long thisMessageNumber;
  OutputKafka output = null;
  String message;
  InputMarker inputMarker;

  public KafkaCallBack(OutputKafka output, String message, InputMarker inputMarker,
                       long messageCount) {
    this.thisMessageNumber = messageCount;
    this.output = output;
    this.inputMarker = inputMarker;
    this.message = message;
  }

  public void onCompletion(RecordMetadata metadata, Exception exception) {
    if (metadata != null) {
      if (!output.isKafkaBrokerUp) {
        logger.info("Started writing to kafka. "
          + output.getShortDescription());
        output.isKafkaBrokerUp = true;
      }
      output.incrementStat(1);
      output.writeBytesMetric.count += message.length();

      // metadata.partition();
      // metadata.offset();
    } else {
      output.isKafkaBrokerUp = false;
      final String LOG_MESSAGE_KEY = this.getClass().getSimpleName()
        + "_KAFKA_ASYNC_ERROR";
      LogFeederUtil.logErrorMessageByInterval(LOG_MESSAGE_KEY,
        "Error sending message to Kafka. Async Callback",
        exception, logger, Level.ERROR);

      output.failedMessages.add(this);
    }
  }
}
