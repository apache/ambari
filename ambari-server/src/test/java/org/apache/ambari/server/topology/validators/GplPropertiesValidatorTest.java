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
package org.apache.ambari.server.topology.validators;

import static org.apache.ambari.server.topology.validators.GplPropertiesValidator.CODEC_CLASSES_PROPERTY_NAME;
import static org.apache.ambari.server.topology.validators.GplPropertiesValidator.LZO_CODEC_CLASS;
import static org.apache.ambari.server.topology.validators.GplPropertiesValidator.LZO_CODEC_CLASS_PROPERTY_NAME;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.GPLLicenseNotAcceptedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class GplPropertiesValidatorTest {

  private static final boolean ACCEPTED = true;

  private final org.apache.ambari.server.configuration.Configuration serverConfig = createNiceMock(org.apache.ambari.server.configuration.Configuration.class);
  private final TopologyValidator validator = new GplPropertiesValidator(serverConfig);

  @Before
  public void setup() {
    reset(serverConfig);
  }

  @After
  public void tearDown() {
    verify(serverConfig);
  }

  @Test(expected = GPLLicenseNotAcceptedException.class) // THEN
  public void rejectsCodecsPropertyWithLzoClassIfGplIsNotAccepted() throws Exception {
    // GIVEN
    gpl(!ACCEPTED);
    ClusterTopology topology = TopologyValidatorTests.topologyWithProperties(ImmutableMap.of(
      "core-site", ImmutableMap.of(
        CODEC_CLASSES_PROPERTY_NAME, "OtherCodec, " + LZO_CODEC_CLASS
      )
    ));

    // WHEN
    validator.validate(topology);
  }

  @Test(expected = GPLLicenseNotAcceptedException.class) // THEN
  public void rejectsLzoCodecPropertyIfGplIsNotAccepted() throws Exception {
    // GIVEN
    gpl(!ACCEPTED);
    ClusterTopology topology = TopologyValidatorTests.topologyWithProperties(ImmutableMap.of(
      "core-site", ImmutableMap.of(
        LZO_CODEC_CLASS_PROPERTY_NAME, LZO_CODEC_CLASS
      )
    ));

    // WHEN
    validator.validate(topology);
  }

  @Test
  public void allowsLzoCodecIfGplIsAccepted() throws Exception {
    // GIVEN
    gpl(ACCEPTED);
    ClusterTopology topology = TopologyValidatorTests.topologyWithProperties(ImmutableMap.of(
      "core-site", ImmutableMap.of(
        LZO_CODEC_CLASS_PROPERTY_NAME, LZO_CODEC_CLASS,
        CODEC_CLASSES_PROPERTY_NAME, "OtherCodec," + LZO_CODEC_CLASS
      )
    ));

    // WHEN
    validator.validate(topology);

    // THEN
    // no exception expected
  }

  @Test
  public void allowsConfigWithoutReferenceToGplEvenIfGplIsNotAccepted() throws Exception {
    // GIVEN
    gpl(!ACCEPTED);
    ClusterTopology topology = TopologyValidatorTests.topologyWithProperties(ImmutableMap.of(
    "core-site", ImmutableMap.of(
      "fs.defaultFS", "hdfs://localhost:8020",
      "io.compression.codecs", "org.apache.hadoop.io.compress.DefaultCodec"
      )
    ));

    // WHEN
    validator.validate(topology);

    // THEN
    // no exception expected
  }

  private void gpl(boolean accepted) {
    expect(serverConfig.getGplLicenseAccepted()).andReturn(accepted).atLeastOnce();
    replay(serverConfig);
  }
}
