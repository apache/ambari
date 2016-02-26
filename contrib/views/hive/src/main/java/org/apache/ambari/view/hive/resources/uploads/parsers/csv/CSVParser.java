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
package org.apache.ambari.view.hive.resources.uploads.parsers.csv;

import org.apache.ambari.view.hive.client.Row;
import org.apache.ambari.view.hive.resources.uploads.parsers.ParseOptions;
import org.apache.ambari.view.hive.resources.uploads.parsers.Parser;
import org.apache.commons.csv.CSVFormat;

import java.io.*;
import java.util.*;

/**
 * Parses the given Reader which contains CSV stream and extracts headers and rows, and detect datatypes of columns
 */
public class CSVParser extends Parser {

  private CSVIterator iterator;
  private org.apache.commons.csv.CSVParser parser;

  public CSVParser(Reader reader, ParseOptions parseOptions) throws IOException {
    super(reader, parseOptions);
    parser = new org.apache.commons.csv.CSVParser(this.reader, CSVFormat.EXCEL);
    iterator = new CSVIterator(parser.iterator());
  }

  @Override
  public Row extractHeader() {
    return this.iterator().next();
  }

  @Override
  public void close() throws IOException {
    this.parser.close();
  }

  public Iterator<Row> iterator() {
    return iterator; // only one iterator per parser.
  }
}
