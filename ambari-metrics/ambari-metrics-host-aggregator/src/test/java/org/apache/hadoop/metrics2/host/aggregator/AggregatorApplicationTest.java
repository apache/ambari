/**
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
package org.apache.hadoop.metrics2.host.aggregator;

import junit.framework.Assert;
import org.junit.Test;

import java.net.URI;

import static org.easymock.EasyMock.createMockBuilder;


public class AggregatorApplicationTest {
    @Test
    public void testMainNotEnoughArguments() {
        try {
            AggregatorApplication.main(new String[0]);
            throw new Exception("Should not be thrown");
        } catch (Exception e) {
            //expected
        }
        try {
            AggregatorApplication.main(new String[1]);
            throw new Exception("Should not be thrown");
        } catch (Exception e) {
            //expected
        }
    }

    @Test
    public void testGetURI() {
        AggregatorApplication aggregatorApplicationMock = createMockBuilder(AggregatorApplication.class)
                .withConstructor("", "")
                .addMockedMethod("createHttpServer")
                .addMockedMethod("initConfiguration").createMock();

        URI uri = aggregatorApplicationMock.getURI();
        Assert.assertEquals("http://" + aggregatorApplicationMock.getHostName() + ":61888/", uri.toString());
    }
}
