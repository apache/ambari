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

import com.google.common.collect.ImmutableList;
import org.apache.ambari.view.ViewContext;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public abstract class HiveJob {

  private final String username;
  private final Type type;
  private final ViewContext viewContext;

  public HiveJob(Type type, String username,ViewContext viewContext) {
    this.type = type;
    this.username = username;
    this.viewContext = viewContext;
  }

  public String getUsername() {
    return username;
  }




  public Type getType() {
    return type;
  }



  public ViewContext getViewContext() {
    return viewContext;
  }


  public enum Type {
    SYNC,
    ASYNC
  }

}
