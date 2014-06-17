/**
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
package org.apache.ambari.shell.customization;

import org.springframework.shell.plugin.HistoryFileNameProvider;
import org.springframework.stereotype.Component;

/**
 * Specifies the name of the Ambari command log. Later this log can be used
 * to re-execute the commands with either the --cmdfile option at startup
 * or with the script --file command.
 */
@Component
public class AmbariHistory implements HistoryFileNameProvider {

  @Override
  public String getHistoryFileName() {
    return "ambari-log.ash";
  }

  @Override
  public String getProviderName() {
    return "AmbariShell";
  }
}
