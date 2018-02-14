/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.logfeeder.output;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;

import org.apache.ambari.logfeeder.conf.LogFeederProps;
import org.apache.ambari.logfeeder.input.InputFileMarker;
import org.apache.ambari.logfeeder.output.OutputKafka.KafkaCallBack;
import org.apache.ambari.logfeeder.plugin.input.Input;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.log4j.Logger;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class OutputKafkaTest {
  private static final Logger LOG = Logger.getLogger(OutputKafkaTest.class);

  private static final String TEST_TOPIC = "test topic";

  private OutputKafka outputKafka;

  @SuppressWarnings("unchecked")
  private KafkaProducer<String, String> mockKafkaProducer = EasyMock.strictMock(KafkaProducer.class);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void init() {
    outputKafka = new OutputKafka() {
      @Override
      protected KafkaProducer<String, String> creteKafkaProducer(Properties props) {
        return mockKafkaProducer;
      }
    };
  }

  @Test
  public void testOutputKafka_uploadData() throws Exception {
    LOG.info("testOutputKafka_uploadData()");

    Map<String, Object> config = new HashMap<String, Object>();
    config.put("broker_list", "some broker list");
    config.put("topic", TEST_TOPIC);

    outputKafka.loadConfig(config);
    outputKafka.init(new LogFeederProps());

    @SuppressWarnings("unchecked")
    Future<RecordMetadata> mockFuture = EasyMock.mock(Future.class);

    EasyMock.expect(mockKafkaProducer.send(new ProducerRecord<String, String>(TEST_TOPIC, "value0")))
        .andReturn(mockFuture);
    EasyMock.expect(mockFuture.get()).andReturn(null);

    for (int i = 1; i < 10; i++)
      EasyMock.expect(mockKafkaProducer.send(EasyMock.eq(new ProducerRecord<String, String>(TEST_TOPIC, "value" + i)),
          EasyMock.anyObject(KafkaCallBack.class))).andReturn(null);

    EasyMock.replay(mockKafkaProducer);

    for (int i = 0; i < 10; i++) {
      InputFileMarker inputMarker = new InputFileMarker(EasyMock.mock(Input.class), null, 0);
      outputKafka.write("value" + i, inputMarker);
    }

    EasyMock.verify(mockKafkaProducer);
  }

  @Test
  public void testOutputKafka_noBrokerList() throws Exception {
    LOG.info("testOutputKafka_noBrokerList()");

    expectedException.expect(Exception.class);
    expectedException.expectMessage("For kafka output, bootstrap broker_list is needed");

    Map<String, Object> config = new HashMap<String, Object>();
    config.put("topic", TEST_TOPIC);

    outputKafka.loadConfig(config);
    outputKafka.init(new LogFeederProps());
  }

  @Test
  public void testOutputKafka_noTopic() throws Exception {
    LOG.info("testOutputKafka_noBrokerList()");

    expectedException.expect(Exception.class);
    expectedException.expectMessage("For kafka output, topic is needed");

    Map<String, Object> config = new HashMap<String, Object>();
    config.put("broker_list", "some broker list");

    outputKafka.loadConfig(config);
    outputKafka.init(new LogFeederProps());
  }

  @After
  public void cleanUp() {
    EasyMock.reset(mockKafkaProducer);
  }
}
