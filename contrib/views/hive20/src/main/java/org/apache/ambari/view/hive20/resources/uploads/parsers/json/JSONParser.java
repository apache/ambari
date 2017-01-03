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

package org.apache.ambari.view.hive20.resources.uploads.parsers.json;

import com.google.gson.stream.JsonReader;
import org.apache.ambari.view.hive20.client.Row;
import org.apache.ambari.view.hive20.resources.uploads.parsers.ParseOptions;
import org.apache.ambari.view.hive20.resources.uploads.parsers.Parser;
import org.apache.ambari.view.hive20.resources.uploads.parsers.RowIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.Iterator;


/**
 * Parses the input data from reader as JSON and provides iterator for rows.
 *
 * Expects the input reader to contains a JsonArray in which each element is a JsonObject
 * corresponding to the row.
 * eg. :
 *
 * [
 *  {row1-col1, row1-col2, row1-col3},
 *  {row2-col1, row2-col2, row2-col3}
 * ]
 *
 */
public class JSONParser extends Parser {

  protected final static Logger LOG =
          LoggerFactory.getLogger(JSONParser.class);

  private RowIterator iterator;
  private JsonReader jsonReader;
  private JSONIterator JSONIterator;

  public JSONParser(Reader reader, ParseOptions parseOptions) throws IOException {
    super(reader, parseOptions);
    this.jsonReader = new JsonReader(this.reader);
    JSONIterator = new JSONIterator(this.jsonReader);
    iterator = new RowIterator(JSONIterator);
  }

  @Override
  public Row extractHeader() {
    Collection<String> headers = this.iterator.extractHeaders();
    Object[] objs = new Object[headers.size()];
    Iterator<String> iterator = headers.iterator();
    for(int i = 0 ; i < headers.size() ; i++){
      objs[i] = iterator.next();
    }

    return new Row(objs);
  }

  @Override
  public void close() throws Exception {
    this.jsonReader.close();
  }

  @Override
  public Iterator<Row> iterator() {
    return iterator;
  }
}