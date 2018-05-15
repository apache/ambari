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

package org.apache.ambari.view.hive2.actor.message;

import com.google.common.base.Optional;
import org.apache.ambari.view.ViewContext;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.Collection;


public class SQLStatementJob extends HiveJob {

  public static final String SEMICOLON = ";";
  private String[] statements;

  private final String jobId;
  private final String logFile;

  public SQLStatementJob(Type type, String[] statements, String username, String jobId, String logFile) {
    super(type, username);
    this.statements = new String[statements.length];
    this.jobId = jobId;
    this.logFile = logFile;
    for (int i = 0; i < statements.length; i++) {
      this.statements[i] = clean(statements[i]);
    }
  }
  public SQLStatementJob(Type type, String[] statements, String username) {
    this(type, statements, username, null, null);
  }

  private String clean(String statement) {
    return StringUtils.trim(statement);
  }

  public Collection<String> getStatements() {
    return Arrays.asList(statements);
  }

  public Optional<String> getJobId() {
    return Optional.fromNullable(jobId);
  }

  public Optional<String> getLogFile() {
    return Optional.fromNullable(logFile);
  }
}
