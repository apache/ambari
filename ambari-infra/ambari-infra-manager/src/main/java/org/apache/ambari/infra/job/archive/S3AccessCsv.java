/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.infra.job.archive;

import org.apache.ambari.infra.conf.security.PasswordStore;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.csv.CSVFormat.DEFAULT;

public class S3AccessCsv implements PasswordStore {
  private static final Logger LOG = LoggerFactory.getLogger(S3AccessCsv.class);

  public static S3AccessCsv file(String path) {
    try {
      return new S3AccessCsv(new FileReader(path));
    } catch (FileNotFoundException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Map<String, String> passwordMap = new HashMap<>();

  public S3AccessCsv(Reader reader) {
    try (CSVParser csvParser = CSVParser.parse(reader, DEFAULT.withHeader(
            S3AccessKeyNames.AccessKeyId.getCsvName(), S3AccessKeyNames.SecretAccessKey.getCsvName()))) {
      Iterator<CSVRecord> iterator = csvParser.iterator();
      if (!iterator.hasNext()) {
        throw new S3AccessCsvFormatException("Csv file is empty!");
      }

      CSVRecord record = iterator.next();
      if (record.size() < 2) {
        throw new S3AccessCsvFormatException("Csv file contains less than 2 columns!");
      }

      checkColumnExists(record, S3AccessKeyNames.AccessKeyId);
      checkColumnExists(record, S3AccessKeyNames.SecretAccessKey);

      if (!iterator.hasNext()) {
        throw new S3AccessCsvFormatException("Csv file contains header only!");
      }

      record = iterator.next();

      Map<String, Integer> header = csvParser.getHeaderMap();
      for (S3AccessKeyNames keyNames : S3AccessKeyNames.values())
        passwordMap.put(keyNames.getEnvVariableName(), record.get(header.get(keyNames.getCsvName())));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (S3AccessCsvFormatException e) {
      LOG.warn("Unable to parse csv file: {}", e.getMessage());
    }
  }

  private void checkColumnExists(CSVRecord record, S3AccessKeyNames s3AccessKeyName) {
    if (!s3AccessKeyName.getCsvName().equals(record.get(s3AccessKeyName.getCsvName()))) {
      throw new S3AccessCsvFormatException(String.format("Csv file does not contain the required column: '%s'", s3AccessKeyName.getCsvName()));
    }
  }

  @Override
  public Optional<String> getPassword(String propertyName) {
    return Optional.ofNullable(passwordMap.get(propertyName));
  }
}
