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
package org.apache.ambari.shell.flash;

import static java.lang.Math.round;

import java.math.BigDecimal;

import org.apache.ambari.groovy.client.AmbariClient;
import org.springframework.shell.core.JLineShellComponent;

/**
 * Show the install progress in % value.
 */
public class InstallProgress extends AbstractFlash {

  private static final int SUCCESS = 100;
  private static final int FAILED = -1;
  private final boolean exit;
  private AmbariClient client;
  private volatile boolean done;

  public InstallProgress(JLineShellComponent shell, AmbariClient client, boolean exit) {
    super(shell, FlashType.INSTALL);
    this.client = client;
    this.exit = exit;
  }

  @Override
  public String getText() {
    StringBuilder sb = new StringBuilder();
    if (!done) {
      BigDecimal progress = client.getRequestProgress();
      if (progress != null) {
        BigDecimal decimal = progress.setScale(2, BigDecimal.ROUND_HALF_UP);
        int intValue = decimal.intValue();
        if (intValue != SUCCESS && intValue != FAILED) {
          sb.append("Installation: ").append(decimal).append("% ");
          long rounded = round(progress.setScale(0, BigDecimal.ROUND_UP).floatValue() / 10);
          for (long i = 0; i < 10; i++) {
            if (i < rounded) {
              sb.append("=");
            } else {
              sb.append("-");
            }
          }
        } else if (intValue == FAILED) {
          sb.append("Installation: FAILED");
          done = true;
        } else {
          sb.append("Installation: COMPLETE");
          done = true;
        }
      } else {
        sb.append("Installation: WAITING..");
      }
    } else {
      if (exit) {
        done = true;
      }
    }
    return sb.toString();
  }
}
