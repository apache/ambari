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

import org.apache.ambari.view.hive20.resources.uploads.parsers.EndOfDocumentException;
import org.apache.ambari.view.hive20.resources.uploads.parsers.RowMapIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.*;
import java.io.IOException;
import java.util.LinkedHashMap;

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
class XMLIterator implements RowMapIterator {

  protected final static Logger LOG =
          LoggerFactory.getLogger(XMLIterator.class);

  private LinkedHashMap<String, String> nextObject = null;
  private static final String TAG_TABLE = "table";
  private static final String TAG_ROW = "row";
  private static final String TAG_COL = "col";
  private boolean documentStarted = false;
  private XMLEventReader reader;

  public XMLIterator(XMLEventReader reader) throws IOException {
    this.reader = reader;
    try {
      nextObject = readNextObject(this.reader);
    } catch (EndOfDocumentException e) {
      LOG.debug("error : {}", e);
    } catch (XMLStreamException e) {
      throw new IOException(e);
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
    } catch (IOException e) {
      LOG.error("Exception occured while reading the next row from XML : {} ", e);
      nextObject = null;
    } catch (EndOfDocumentException e) {
      LOG.debug("End of XML document reached with next character ending the XML.");
      nextObject = null;
    } catch (XMLStreamException e) {
      LOG.error("Exception occured while reading the next row from XML : {} ", e);
      nextObject = null;
    }
    return currObject;
  }

  @Override
  public void remove() {
    // no operation.
    LOG.info("No operation when remove called.");
  }

  private LinkedHashMap<String, String> readNextObject(XMLEventReader reader) throws IOException, EndOfDocumentException, XMLStreamException {
    LinkedHashMap<String, String> row = new LinkedHashMap<>();
    boolean objectStarted = false;
    String currentName = null;

    while (true) {
      XMLEvent event = reader.nextEvent();
      switch (event.getEventType()) {
        case XMLStreamConstants.START_ELEMENT:
          StartElement startElement = event.asStartElement();
          String qName = startElement.getName().getLocalPart();
          LOG.debug("startName : {}" , qName);
          switch (qName) {
            case TAG_TABLE:
              if (documentStarted) {
                throw new IllegalArgumentException("Cannot have a <table> tag nested inside another <table> tag");
              } else {
                documentStarted = true;
              }
              break;
            case TAG_ROW:
              if (objectStarted) {
                throw new IllegalArgumentException("Cannot have a <row> tag nested inside another <row> tag");
              } else {
                objectStarted = true;
              }
              break;
            case TAG_COL:
              if (!objectStarted) {
                throw new IllegalArgumentException("Stray tag " + qName);
              }
              Attribute nameAttr = startElement.getAttributeByName( new QName("name"));
              if( null == nameAttr ){
                throw new IllegalArgumentException("Missing name attribute in col tag.");
              }
              currentName = nameAttr.getValue();
              break;
            default:
              throw new IllegalArgumentException("Illegal start tag " + qName + " encountered.");
          }
          break;
        case XMLStreamConstants.END_ELEMENT:
          EndElement endElement = event.asEndElement();
          String name = endElement.getName().getLocalPart();
          LOG.debug("endName : {}", name);
          switch (name) {
            case TAG_TABLE:
              if (!documentStarted) {
                throw new IllegalArgumentException("Stray </table> tag.");
              }
              throw new EndOfDocumentException("End of XML document.");

            case TAG_ROW:
              if (!objectStarted) {
                throw new IllegalArgumentException("Stray </row> tag.");
              }
              return row;

            case TAG_COL:
              if (!objectStarted) {
                throw new IllegalArgumentException("Stray tag " + name);
              }
              currentName = null;
              break;

            default:
              throw new IllegalArgumentException("Illegal start ending " + name + " encountered.");
          }
          break;
        case XMLStreamConstants.CHARACTERS:
          Characters characters = event.asCharacters();
          if (characters.isWhiteSpace() && currentName == null)
            break;
          String data = characters.getData();
          LOG.debug("character data : {}", data);
          if (currentName == null) {
            throw new IllegalArgumentException("Illegal characters outside any tag : " + data);
          } else {
            String oldData = row.get(currentName);
            if (null != oldData) {
              data = oldData + data;
            }
            row.put(currentName, data);
          }
          break;
      }
    }
  }
}
