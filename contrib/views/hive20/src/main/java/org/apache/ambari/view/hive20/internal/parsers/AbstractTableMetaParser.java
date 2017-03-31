/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive20.internal.parsers;


import org.apache.ambari.view.hive20.client.Row;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public abstract class AbstractTableMetaParser<T> implements TableMetaSectionParser<T> {
  private final String sectionMarker;
  private final String secondarySectionMarker;
  private final String sectionStartMarker;
  private final String sectionEndMarker;


  public AbstractTableMetaParser(String sectionMarker, String sectionStartMarker, String sectionEndMarker) {
    this(sectionMarker, null, sectionStartMarker, sectionEndMarker);
  }

  public AbstractTableMetaParser(String sectionMarker, String secondarySectionMarker, String sectionStartMarker, String sectionEndMarker) {
    this.sectionMarker = sectionMarker;
    this.secondarySectionMarker = secondarySectionMarker;
    this.sectionStartMarker = sectionStartMarker;
    this.sectionEndMarker = sectionEndMarker;
  }

  protected Map<String, Object> parseSection(List<Row> rows) {
    boolean sectionStarted = false;
    boolean startMarkerAndEndMarkerIsSame = !(sectionStartMarker == null || sectionEndMarker == null) && sectionStartMarker.equalsIgnoreCase(sectionEndMarker);
    boolean sectionDataReached = false;

    Map<String, Object> result = new LinkedHashMap<>();

    Iterator<Row> iterator = rows.iterator();

    String currentNestedEntryParent = null;
    List<Entry> currentNestedEntries = null;
    boolean processingNestedEntry = false;

    while (iterator.hasNext()) {
      Row row = iterator.next();
      String colName = ((String) row.getRow()[0]).trim();
      String colValue = row.getRow()[1] != null ? ((String) row.getRow()[1]).trim() : null;
      String colComment = row.getRow()[2] != null ? ((String) row.getRow()[2]).trim() : null;

      if (sectionMarker.equalsIgnoreCase(colName)) {
        sectionStarted = true;
      } else {
        if (sectionStarted) {
          if (secondarySectionMarker != null && secondarySectionMarker.equalsIgnoreCase(colName) && colValue != null) {
            continue;
          }

          if (sectionStartMarker != null && sectionStartMarker.equalsIgnoreCase(colName) && colValue == null) {
            if (startMarkerAndEndMarkerIsSame) {
              if (sectionDataReached) {
                break;
              }
            }
            sectionDataReached = true;
            continue;
          } else if (sectionEndMarker != null && sectionEndMarker.equalsIgnoreCase(colName) && colValue == null) {
            break;
          } else if (sectionStartMarker == null) {
            sectionDataReached = true;
            //continue;
          }

          if (colValue == null && !processingNestedEntry) {
            currentNestedEntryParent = colName;
            currentNestedEntries = new ArrayList<>();
            processingNestedEntry = true;
            continue;
          } else if (colName.equalsIgnoreCase("") && processingNestedEntry) {
            Entry entry = new Entry(colValue, colComment);
            currentNestedEntries.add(entry);
            continue;
          } else if (processingNestedEntry) {
            result.put(currentNestedEntryParent, currentNestedEntries);
            processingNestedEntry = false;
          }

          Entry entry = new Entry(colName, colValue, colComment);
          result.put(colName, entry);

        }

      }
    }

    if (processingNestedEntry) {
      result.put(currentNestedEntryParent, currentNestedEntries);
    }

    return result;
  }

  protected Map<String, String> getMap(Map<String, Object> parsedSection, String key) {
    Map<String, String> result = new HashMap<>();
    Object value = parsedSection.get(key);
    if(value == null) {
      return null;
    }
    if (value instanceof List) {
      List<Entry> entries = (List<Entry>)value;
      for(Entry entry: entries) {
        result.put(entry.getName(), entry.getValue());
      }
    }
    return result;
  }

  protected String getString(Map<String, Object> parsedSection, String key) {
    Object value = parsedSection.get(key);
    if(value == null) {
      return null;
    }
    if (value instanceof Entry) {
      return ((Entry) parsedSection.get(key)).getValue();
    }
    return null;
  }


  public static class Entry {
    private final String name;
    private final String value;
    private final String comment;

    public Entry(String name, String type, String comment) {
      this.name = name;
      this.value = type;
      this.comment = comment;
    }

    public Entry(String name, String type) {
      this(name, type, null);
    }

    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }

    public String getComment() {
      return comment;
    }
  }
}
