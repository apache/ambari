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
package org.apache.ambari.logfeeder.output;

import java.io.File;

import org.apache.ambari.logfeeder.input.InputMarker;
import org.apache.log4j.Logger;

/**
 * Output that just ignore the logs
 */
public class OutputDevNull extends Output {

  private static final Logger LOG = Logger.getLogger(OutputDevNull.class);

  @Override
  public void write(String block, InputMarker inputMarker){
    LOG.trace("Ignore log block: " + block);
  }

  @Override
  public void copyFile(File inputFile, InputMarker inputMarker) {
    throw new UnsupportedOperationException("copyFile method is not yet supported for output=dev_null");
  }
}
