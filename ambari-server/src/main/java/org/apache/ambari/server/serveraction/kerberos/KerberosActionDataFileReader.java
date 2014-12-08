/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.serveraction.kerberos;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;

/**
 * KerberosActionDataFileReader is an implementation of a KerberosActionDataFile that is used to
 * read existing KerberosActionDataFiles.
 * <p/>
 * This class encapsulates a {@link org.apache.commons.csv.CSVParser} to read a CSV-formatted file.
 */
public class KerberosActionDataFileReader implements KerberosActionDataFile, Iterable<Map<String, String>> {

  private File file;
  private CSVParser csvParser = null;


  /**
   * Creates a new KerberosActionDataFileReader
   * <p/>
   * The file is opened upon creation, so there is no need to manually open it unless manually
   * closed before using.
   *
   * @param file a File declaring where to write the data
   * @throws IOException
   */
  public KerberosActionDataFileReader(File file) throws IOException {
    this.file = file;
    open();
  }


  /**
   * Opens the data file for reading.
   * <p/>
   * This may be called multiple times and the appropriate action will occur depending on if the
   * file has been previously opened or closed.
   *
   * @throws IOException
   */
  public void open() throws IOException {
    if (isClosed()) {
      csvParser = CSVParser.parse(file, Charset.defaultCharset(), CSVFormat.DEFAULT.withHeader());
    }
  }


  /**
   * Tests this KerberosActionDataFileReader  to see if the data file is closed.
   *
   * @return true if closed; otherwise false
   */
  public boolean isClosed() {
    return csvParser == null;
  }

  public void close() throws IOException {
    csvParser.close();
    csvParser = null;
  }

  /**
   * Gets an iterator to use to access the records in the data file.
   * <p/>
   * Each item is a Map of column names to values.
   *
   * @return an Iterator
   */
  @Override
  public Iterator<Map<String, String>> iterator() {
    return new Iterator<Map<String, String>>() {
      Iterator<CSVRecord> iterator = csvParser.iterator();

      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public Map<String, String> next() {
        return iterator.next().toMap();
      }

      @Override
      public void remove() {
        iterator.remove();
      }
    };
  }
}
