/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.view.hive20.resources.upload;

import org.apache.ambari.view.hive20.resources.uploads.parsers.ParseUtils;
import org.junit.Assert;
import org.junit.Test;

public class ParseUtilsTest {
  @Test
  public void testDateFormats() {
    Assert.assertTrue(ParseUtils.isDate("1970-01-01"));
    Assert.assertTrue(ParseUtils.isDate("1970-01-01 "));
    Assert.assertTrue(ParseUtils.isDate("0001-1-3"));
    Assert.assertTrue(ParseUtils.isDate("1996-1-03"));
    Assert.assertTrue(ParseUtils.isDate("1996-01-3"));
    Assert.assertTrue(ParseUtils.isDate("1996-10-3"));
    Assert.assertFalse(ParseUtils.isDate("1970-01-01 01:01:01"));
    Assert.assertFalse(ParseUtils.isDate("1970-01-01 23:59:59.999999"));
    Assert.assertFalse(ParseUtils.isDate("1970/01/01"));
    Assert.assertFalse(ParseUtils.isDate("01-01-1970"));
    Assert.assertFalse(ParseUtils.isDate("1970-13-01"));
    Assert.assertFalse(ParseUtils.isDate("1970-01-32"));
    Assert.assertFalse(ParseUtils.isDate("01/01/1970"));
    Assert.assertFalse(ParseUtils.isDate("001-1-3"));
  }

  @Test
  public void testTimestampFormats() {
    Assert.assertFalse(ParseUtils.isTimeStamp("1999-11-30"));
    Assert.assertFalse(ParseUtils.isTimeStamp("1999-12-31 23:59"));
    Assert.assertTrue(ParseUtils.isTimeStamp("1999-12-31 23:59:59"));
    Assert.assertTrue(ParseUtils.isTimeStamp("1999-12-31 23:59:59.100"));
    Assert.assertTrue(ParseUtils.isTimeStamp("1999-12-31 23:59:59.999999"));
    Assert.assertTrue(ParseUtils.isTimeStamp("1999-12-31 23:59:59.99999999"));
    Assert.assertTrue(ParseUtils.isTimeStamp("1999-12-31 23:59:59.999999999"));
    Assert.assertTrue(ParseUtils.isTimeStamp("1999-10-31 23:59:59.999999999"));
    Assert.assertFalse(ParseUtils.isTimeStamp("1999-12-31 23:59:59.9999999999"));
    Assert.assertFalse(ParseUtils.isTimeStamp("1999/12/31 23:59:59.9999999999"));
  }
}
