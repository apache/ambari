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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.ambari.view.hive20.client.Row;
import org.apache.ambari.view.hive20.resources.uploads.parsers.json.JSONParser;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;

public class JsonParserTest {

  @Test(expected = IOException.class)
  public void testEmptyStream() throws Exception {
    String json = "";

    try(
      StringReader sr = new StringReader(json);
      JSONParser jp =  new JSONParser(sr, null);
    ) {
      // PARSING WILL THROW ERROR
    }
  }

  @Test
  public void testEmptyRow() throws Exception {
    JsonObject jo = new JsonObject();
    JsonArray ja = new JsonArray();
    ja.add(jo);
    String json = ja.toString();

    try(
      StringReader sr = new StringReader(json);
      JSONParser jp = new JSONParser(sr, null)
      ) {

      Iterator<Row> iterator = jp.iterator();

      Assert.assertEquals("Iterator should not be Empty", true, iterator.hasNext());
      Assert.assertArrayEquals("Row should be empty",new Object[]{},iterator.next().getRow());
    }
  }


  @Test
  public void testEmptyTable() throws Exception {
    JsonArray ja = new JsonArray();
    String json = ja.toString();

    try(
      StringReader sr = new StringReader(json);
      JSONParser jp =  new JSONParser(sr, null);
    ) {
      Iterator<Row> iterator = jp.iterator();
      Assert.assertEquals("Iterator Empty!", false, iterator.hasNext());
    }
  }

  @Test
  public void testParse1Row() throws Exception {
    JsonObject jo = new JsonObject();
    jo.addProperty("key1","value1");
    jo.addProperty("key2",'c');
    jo.addProperty("key3",10);
    jo.addProperty("key4",10.1);

    JsonArray ja = new JsonArray();
    ja.add(jo);
    String json = ja.toString();

    try(StringReader sr = new StringReader(json);

        JSONParser jp  = new JSONParser(sr, null)
    ) {
      Iterator<Row> iterator = jp.iterator();

      Assert.assertEquals("Iterator Empty!", true, iterator.hasNext());
      Row row = iterator.next();
      Row expected = new Row(new Object[]{"value1", "c", "10", "10.1"});
      Assert.assertEquals("Row not equal!", expected, row);

      Assert.assertEquals("Should report no more rows!", false, iterator.hasNext());
    }
  }

  @Test
  public void testParseMultipleRow() throws Exception {
    JsonObject jo1 = new JsonObject();
    jo1.addProperty("key1","value1");
    jo1.addProperty("key2","c");
    jo1.addProperty("key3","10");
    jo1.addProperty("key4","10.1");

    JsonObject jo2 = new JsonObject();
    jo2.addProperty("key1","value2");
    jo2.addProperty("key2","c2");
    jo2.addProperty("key3","102");
    jo2.addProperty("key4",true);


    JsonArray ja = new JsonArray();
    ja.add(jo1);
    ja.add(jo2);

    String json = ja.toString();



    try(
      StringReader sr = new StringReader(json);
      JSONParser jp = new JSONParser(sr, null)
    ) {
      Iterator<Row> iterator = jp.iterator();

      Assert.assertEquals("Failed to detect 1st row!", true, iterator.hasNext());
      Assert.assertEquals("Failed to match 1st row!", new Row(new Object[]{"value1", "c", "10", "10.1"}), iterator.next());

      Assert.assertEquals("Failed to detect 2nd row!", true, iterator.hasNext());
      Assert.assertEquals("Failed to match 2nd row!", new Row(new Object[]{"value2", "c2", "102", Boolean.TRUE.toString()}), iterator.next());

      Assert.assertEquals("Failed to detect end of rows!", false, iterator.hasNext());
      Assert.assertEquals("Failed to detect end of rows 2nd time!", false, iterator.hasNext());
    }
  }
}
