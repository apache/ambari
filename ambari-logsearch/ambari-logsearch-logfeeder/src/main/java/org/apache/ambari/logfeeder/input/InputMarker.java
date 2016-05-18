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

package org.apache.ambari.logfeeder.input;

/**
 * This file contains the file inode, line number of the log currently been read
 */
public class InputMarker {
  public int lineNumber = 0;
  public int beginLineNumber = 0;
  public Input input;
  public String filePath;
  public Object fileKey = null;
  public String base64FileKey = null;

  @Override
  public String toString() {
    return "InputMarker [lineNumber=" + lineNumber + ", input="
      + input.getShortDescription() + "]";
  }

}
