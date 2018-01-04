package org.apache.ambari.infra.job.archive;

import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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

public class FileNameSuffixFormatterTest {

  private FileNameSuffixFormatter formatter = new FileNameSuffixFormatter("logtime", "yyyy-MM-dd'T'hh-mm-ss-SSSX");

  @Test(expected = NullPointerException.class)
  public void testFormatWhenDocumentIsNullThrowingException() throws Exception {
    formatter.format(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFormatWhenSpecifiedColumnDoesNotExistsInTheDocumentThrowingException() throws Exception {
    formatter.format(new Document(new HashMap<>()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFormatWhenSpecifiedColumnContainsBlankValueThrowingException() throws Exception {
    formatter.format(new Document(new HashMap<String, String>() {{ put("logtime", "  "); }}));
  }

  @Test
  public void testFormatWhenNoDateFormatSpecifiedRawColumnValueReturned() throws Exception {
    FileNameSuffixFormatter formatter = new FileNameSuffixFormatter("logtime", null);
    assertThat(formatter.format(new Document(new HashMap<String, String>() {{ put("logtime", "Monday"); }})), is("Monday"));
  }

  @Test
  public void testFormatWhenDateFormatIsSpecifiedAFormattedValueReturned() throws Exception {
    assertThat(formatter.format(new Document(new HashMap<String, String>() {{ put("logtime", "2017-12-15T10:12:33.453Z"); }})), is("2017-12-15T10-12-33-453Z"));
  }
}