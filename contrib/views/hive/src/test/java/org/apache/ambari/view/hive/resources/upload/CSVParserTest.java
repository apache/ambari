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

package org.apache.ambari.view.hive.resources.upload;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.ambari.view.hive.client.Row;
import org.apache.ambari.view.hive.resources.uploads.parsers.csv.CSVParser;
import org.apache.ambari.view.hive.resources.uploads.parsers.json.JSONParser;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;

public class CSVParserTest {

  /**
   * no exception in creating csvParser with emtpy stream
   * @throws IOException
   */
  @Test
  public void testEmptyStream() throws IOException {
    String csv = "";

    StringReader sr = new StringReader(csv);

    CSVParser jp = null;

    try {

      jp = new CSVParser(sr, null);

      Assert.assertEquals("There should not be any rows.",false, jp.iterator().hasNext());

    }finally{
      if( null != jp )
        jp.close();

      sr.close();
    }
  }

  /**
   * in case of csv an empty line is still considered as row
   * @throws IOException
   */
  @Test
  public void testEmptyRow() throws IOException {
    String csv = "       ";
    StringReader sr = new StringReader(csv);

    CSVParser jp = null;

    try {
      jp = new CSVParser(sr, null);

      Iterator<Row> iterator = jp.iterator();

      Assert.assertEquals("Iterator should be Empty", true, iterator.hasNext());
      Assert.assertArrayEquals("Row should not be empty",new Object[]{"       "},iterator.next().getRow());
    }finally{
      if( null != jp )
        jp.close();

      sr.close();
    }
  }

  @Test
  public void testParse1Row() throws IOException {
    String csv = "value1,c,10,10.1";

    StringReader sr = new StringReader(csv);

    CSVParser jp = null;

    try {
      jp = new CSVParser(sr, null);

      Iterator<Row> iterator = jp.iterator();

      Assert.assertEquals("Iterator Empty!", true, iterator.hasNext());
      Row row = iterator.next();
      Row expected = new Row(new Object[]{"value1", "c", "10", "10.1"});
      Assert.assertEquals("Row not equal!", expected, row);

      Assert.assertEquals("Should report no more rows!", false, iterator.hasNext());
    }finally{
      if( null != jp )
        jp.close();

      sr.close();
    }
  }

  @Test
  public void testParseMultipleRow() throws IOException {

    String csv = "value1,c,10,10.1\n" +
            "value2,c2,102,true";

    StringReader sr = new StringReader(csv);

    CSVParser jp = null;

    try {
      jp = new CSVParser(sr, null);

      Iterator<Row> iterator = jp.iterator();

      Assert.assertEquals("Failed to detect 1st row!", true, iterator.hasNext());
      Assert.assertEquals("Failed to match 1st row!", new Row(new Object[]{"value1", "c", "10", "10.1"}), iterator.next());

      Assert.assertEquals("Failed to detect 2nd row!", true, iterator.hasNext());
      Assert.assertEquals("Failed to match 2nd row!", new Row(new Object[]{"value2", "c2", "102", Boolean.TRUE.toString()}), iterator.next());

      Assert.assertEquals("Failed to detect end of rows!", false, iterator.hasNext());
      Assert.assertEquals("Failed to detect end of rows 2nd time!", false, iterator.hasNext());
    }finally{
      if( null != jp )
        jp.close();

      sr.close();
    }
  }
}
