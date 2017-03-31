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
package org.apache.ambari.view.hive20.resources.uploads.parsers.csv.commonscsv;

import org.apache.ambari.view.hive20.client.Row;
import org.apache.ambari.view.hive20.resources.uploads.parsers.ParseOptions;
import org.apache.ambari.view.hive20.resources.uploads.parsers.Parser;
import org.apache.commons.csv.CSVFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

/**
 * Parses the given Reader which contains CSV stream and extracts headers and rows, and detect datatypes of columns
 */
public class CSVParser extends Parser {
  private CSVIterator iterator;
  private org.apache.commons.csv.CSVParser parser;
  private final static Logger LOG =
    LoggerFactory.getLogger(CSVParser.class);

  public CSVParser(Reader reader, ParseOptions parseOptions) throws IOException {
    super(reader, parseOptions);
    CSVFormat format = CSVFormat.DEFAULT;
    String optHeader =  (String)parseOptions.getOption(ParseOptions.OPTIONS_HEADER);
    if(optHeader != null){
      if(optHeader.equals(ParseOptions.HEADER.FIRST_RECORD.toString())) {
        format = format.withHeader();
      }else if( optHeader.equals(ParseOptions.HEADER.PROVIDED_BY_USER.toString())){
        String [] headers = (String[]) parseOptions.getOption(ParseOptions.OPTIONS_HEADERS);
        format = format.withHeader(headers);
      }
    }

    Character delimiter = (Character) parseOptions.getOption(ParseOptions.OPTIONS_CSV_DELIMITER);
    if(delimiter != null){
      LOG.info("setting delimiter as {}", delimiter);
      format = format.withDelimiter(delimiter);
    }

    Character quote = (Character) parseOptions.getOption(ParseOptions.OPTIONS_CSV_QUOTE);
    if( null != quote ){
      LOG.info("setting Quote char : {}", quote);
      format = format.withQuote(quote);
    }

    Character escape = (Character) parseOptions.getOption(ParseOptions.OPTIONS_CSV_ESCAPE_CHAR);
    if(escape != null){
      LOG.info("setting escape as {}", escape);
      format = format.withEscape(escape);
    }

    parser = new org.apache.commons.csv.CSVParser(this.reader,format );
    iterator = new CSVIterator(parser.iterator());
  }

  @Override
  public Row extractHeader() {
    return new Row(parser.getHeaderMap().keySet().toArray());
  }

  @Override
  public void close() throws Exception {
    this.parser.close();
  }

  public Iterator<Row> iterator() {
    return iterator; // only one iterator per parser.
  }
}
