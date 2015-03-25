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

import static org.apache.ambari.server.serveraction.kerberos.KerberosConfigDataFile.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * KerberosConfigDataFileBuilder is an implementation of a KerberosConfigDataFile that is used to
 * create a new KerberosConfigDataFile.
 * <p/>
 * This class encapsulates a {@link org.apache.commons.csv.CSVPrinter} to create a CSV-formatted file.
 */
public class KerberosConfigDataFileBuilder extends AbstractKerberosDataFileBuilder {

  /**
   * Creates a new KerberosConfigDataFileBuilder
   * <p/>
   * The file is opened upon creation, so there is no need to manually open it unless manually
   * closed before using.
   *
   * @param file a File declaring where to write the data
   * @throws java.io.IOException
   */
  public KerberosConfigDataFileBuilder(File file) throws IOException {
    super(file);
  }


  /**
   * Appends a new record to the data file
   *
   * @param config    a String declaring the relevant configuration type for the key and value
   * @param key       a String declaring the key (or property name) with in the relevant configuration type
   * @param value     a String containing the value of the configuration property
   * @param operation a String containing the operation to perform, expected "SET" or "REMOVE"
   * @throws java.io.IOException
   */
  public void addRecord(String config, String key, String value, String operation) throws IOException {
    super.appendRecord(config, key, value, operation);
  }

  @Override
  protected Iterable<String> getHeaderRecord() {
    return Arrays.asList(CONFIGURATION_TYPE, KEY, VALUE, OPERATION);
  }
}
