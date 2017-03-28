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
package org.apache.ambari.logsearch.common;

import java.io.Serializable;

import org.apache.ambari.logsearch.web.model.User;

public class LogSearchContext implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private User user;

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  //------------------------------------------------------------------------------------------------------
  
  private static final ThreadLocal<LogSearchContext> contextThreadLocal = new ThreadLocal<LogSearchContext>();

  public static LogSearchContext getContext() {
    return contextThreadLocal.get();
  }

  public static void setContext(LogSearchContext context) {
    contextThreadLocal.set(context);
  }

  public static void resetContext() {
    contextThreadLocal.remove();
  }

  public static String getCurrentUsername() {
    LogSearchContext context = LogSearchContext.getContext();
    if (context != null && context.getUser() != null) {
        return context.getUser().getUsername();
    }
    return null;
  }
}
