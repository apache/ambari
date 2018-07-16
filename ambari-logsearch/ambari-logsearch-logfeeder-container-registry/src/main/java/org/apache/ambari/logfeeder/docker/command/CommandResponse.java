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
package org.apache.ambari.logfeeder.docker.command;

import java.util.List;

/**
 * Represent a bash command response (stdout as string list, stderr in string and an exit code)
 */
public class CommandResponse {
  private final int exitCode;
  private final List<String> stdOut;
  private final String stdErr;

  CommandResponse(int exitCode, List<String> stdOut, String stdErr) {
    this.exitCode = exitCode;
    this.stdOut = stdOut;
    this.stdErr = stdErr;
  }

  public int getExitCode() {
    return exitCode;
  }

  public List<String> getStdOut() {
    return stdOut;
  }

  public String getStdErr() {
    return stdErr;
  }
}
