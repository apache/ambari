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

package org.apache.ambari.view.hive.resources.uploads;

import org.apache.ambari.view.hive.client.Row;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;

/**
 * Takes row iterator as input.
 * iterate over rows and creates a CSV formated stream separating rows by endline "\n"
 * Note : column values should not contain "\n".
 */
public class TableDataReader extends Reader {

  private static final int CAPACITY = 1024;
  private StringReader stringReader = new StringReader("");

  private Iterator<Row> iterator;
  private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.withRecordSeparator("\n");

  public TableDataReader(Iterator<Row> rowIterator) {
    this.iterator = rowIterator;
  }

  @Override
  public int read(char[] cbuf, int off, int len) throws IOException {

    int totalLen = len;
    int count = 0;
    do {
      int n = stringReader.read(cbuf, off, len);

      if (n != -1) {
        // n  were read
        len = len - n; // len more to be read
        off = off + n; // off now shifted to n more
        count += n;
      }

      if (count == totalLen) return count; // all totalLen characters were read

      if (iterator.hasNext()) { // keep reading as long as we keep getting rows
        StringWriter stringWriter = new StringWriter(CAPACITY);
        CSVPrinter csvPrinter = new CSVPrinter(stringWriter, CSV_FORMAT);
        Row row = iterator.next();
        csvPrinter.printRecord(row.getRow());
        stringReader.close(); // close the old string reader
        stringReader = new StringReader(stringWriter.getBuffer().toString());
        csvPrinter.close();
        stringWriter.close();
      } else {
        return count == 0 ? -1 : count;
      }
    } while (count < totalLen);

    return count;
  }

  @Override
  public void close() throws IOException {

  }
}
