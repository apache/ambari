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
package org.apache.ambari.infra.model;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import org.junit.Test;

public class ISO8601DateFormatterTest {

  @Test
  public void testFormat() {
    OffsetDateTime offsetDateTime = OffsetDateTime.of(
            2018, 11, 30,
            2, 30, 11, 0,
            ZoneOffset.ofHoursMinutes(1, 30));
    String text = new ISO8601DateFormatter().format(Date.from(offsetDateTime.toInstant()));
    assertThat(text, is("2018-11-30T01:00:11Z"));
  }

  @Test
  public void testParse() throws ParseException {
    Date now = new Date();
    ISO8601DateFormatter iso8601DateFormatter = new ISO8601DateFormatter();
    Date parsed = iso8601DateFormatter.parse(iso8601DateFormatter.format(now));
    assertThat(parsed, is(now));
  }
}