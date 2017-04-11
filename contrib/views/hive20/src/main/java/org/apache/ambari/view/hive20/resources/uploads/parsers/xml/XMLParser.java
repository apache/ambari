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

package org.apache.ambari.view.hive20.resources.uploads.parsers.xml;

import org.apache.ambari.view.hive20.client.Row;
import org.apache.ambari.view.hive20.resources.uploads.parsers.ParseOptions;
import org.apache.ambari.view.hive20.resources.uploads.parsers.Parser;
import org.apache.ambari.view.hive20.resources.uploads.parsers.RowIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.Iterator;

/**
 * assumes XML of following format
 * <table>
 * <row>
 * <col name="col1Name">row1-col1-Data</col>
 * <col name="col2Name">row1-col2-Data</col>
 * <col name="col3Name">row1-col3-Data</col>
 * <col name="col4Name">row1-col4-Data</col>
 * </row>
 * <row>
 * <col name="col1Name">row2-col1-Data</col>
 * <col name="col2Name">row2-col2-Data</col>
 * <col name="col3Name">row2-col3-Data</col>
 * <col name="col4Name">row2-col4-Data</col>
 * </row>
 * </table>
 */
public class XMLParser extends Parser {

  protected final static Logger LOG =
          LoggerFactory.getLogger(XMLParser.class);

  private RowIterator iterator;
  private XMLEventReader xmlReader;
  private XMLIterator xmlIterator;

  public XMLParser(Reader reader, ParseOptions parseOptions) throws IOException {
    super(reader, parseOptions);
    XMLInputFactory factory = XMLInputFactory.newInstance();
    try {
      factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
      factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
      this.xmlReader = factory.createXMLEventReader(reader);
    } catch (XMLStreamException e) {
      LOG.error("error occurred while creating xml reader : ", e);
      throw new IOException("error occurred while creating xml reader : ", e);
    }
    xmlIterator = new XMLIterator(this.xmlReader);
    iterator = new RowIterator(xmlIterator);
  }

  @Override
  public Row extractHeader() {
    Collection<String> headers = this.iterator.extractHeaders();
    Object[] objs = new Object[headers.size()];
    Iterator<String> iterator = headers.iterator();
    for (int i = 0; i < headers.size(); i++) {
      objs[i] = iterator.next();
    }

    return new Row(objs);
  }

  @Override
  public void close() throws Exception {
    try {
      this.xmlReader.close();
    } catch (XMLStreamException e) {
      throw new IOException(e);
    }
  }

  @Override
  public Iterator<Row> iterator() {
    return iterator;
  }
}
