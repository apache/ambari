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

package org.apache.ambari.view.hive2.resources.uploads.parsers.json;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.apache.ambari.view.hive2.resources.uploads.parsers.EndOfDocumentException;
import org.apache.ambari.view.hive2.resources.uploads.parsers.RowMapIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;

/**
 * iterates over the JsonReader and reads creates row data
 * assumes the array of json objects.
 * eg : [ { "col1Name" : "value-1-1", "col2Name" : "value-1-2"}, { "col1Name" : "value-2-1", "col2Name" : "value-2-2"}]
 */
class JSONIterator implements RowMapIterator {

  protected final static Logger LOG =
          LoggerFactory.getLogger(JSONIterator.class);

  private LinkedHashMap<String, String> nextObject = null;

  private LinkedHashMap<String, String> readNextObject(JsonReader reader) throws IOException, EndOfDocumentException {
    LinkedHashMap<String, String> row = new LinkedHashMap<>();
    boolean objectStarted = false;
    boolean shouldBeName = false;
    String currentName = null;

    while (true) {
      JsonToken token = reader.peek();
      switch (token) {
        case BEGIN_ARRAY:
          throw new IllegalArgumentException("Row data cannot have an array.");
        case END_ARRAY:
          throw new EndOfDocumentException("End of Json Array document.");
        case BEGIN_OBJECT:
          if (objectStarted == true) {
            throw new IllegalArgumentException("Nested objects not supported.");
          }
          if (shouldBeName == true) {
            throw new IllegalArgumentException("name expected, got begin_object");
          }
          objectStarted = true;
          shouldBeName = true;
          reader.beginObject();
          break;
        case END_OBJECT:
          if (shouldBeName == false) {
            throw new IllegalArgumentException("value expected, got end_object");
          }
          reader.endObject();
          return row;
        case NAME:
          if (shouldBeName == false) {
            throw new IllegalArgumentException("name not expected at this point.");
          }
          shouldBeName = false;
          currentName = reader.nextName();
          break;
        case NUMBER:
        case STRING:
          if (shouldBeName == true) {
            throw new IllegalArgumentException("value not expected at this point.");
          }
          String n = reader.nextString();
          row.put(currentName, n);
          shouldBeName = true;
          break;
        case BOOLEAN:
          if (shouldBeName == true) {
            throw new IllegalArgumentException("value not expected at this point.");
          }
          String b = String.valueOf(reader.nextBoolean());
          row.put(currentName, b);
          shouldBeName = true;
          break;
        case NULL:
          if (shouldBeName == true) {
            throw new IllegalArgumentException("value not expected at this point.");
          }
          reader.nextNull();
          row.put(currentName, "");
          shouldBeName = true;
          break;
        case END_DOCUMENT:
          return row;

        default:
          throw new IllegalArgumentException("Illegal token detected inside json: token : " + token.toString());
      }
    }
  }

  private JsonReader reader;

  public JSONIterator(JsonReader reader) throws IOException {
    this.reader = reader;
    // test the start of array
    JsonToken jt = reader.peek();
    if (jt != JsonToken.BEGIN_ARRAY) {
      throw new IllegalArgumentException("Expected the whole document to contain a single JsonArray.");
    }

    reader.beginArray(); // read the start of array
    try {
      nextObject = readNextObject(this.reader);
    } catch (EndOfDocumentException e) {
    }
  }

  @Override
  public boolean hasNext() {
    return null != nextObject;
  }

  public LinkedHashMap<String, String> peek() {
    return nextObject;
  }

  @Override
  public LinkedHashMap<String, String> next() {
    LinkedHashMap<String, String> currObject = nextObject;
    try {
      nextObject = readNextObject(this.reader);
    } catch (EndOfDocumentException e) {
      LOG.debug("End of Json document reached with next character ending the JSON Array.");
      nextObject = null;
    } catch (Exception e){
      // for any other exception throw error right away
      throw new IllegalArgumentException(e);
    }
    return currObject;
  }

  @Override
  public void remove() {
    // no operation.
    LOG.info("No operation when remove called on JSONIterator.");
  }
}