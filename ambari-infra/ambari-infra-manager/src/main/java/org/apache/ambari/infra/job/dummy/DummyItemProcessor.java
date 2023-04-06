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
package org.apache.ambari.infra.job.dummy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.item.ItemProcessor;

public class DummyItemProcessor implements ItemProcessor<DummyObject, String> {

  private static final Logger logger = LogManager.getLogger(DummyItemProcessor.class);

  @Override
  public String process(DummyObject input) throws Exception {
    logger.info("Dummy processing, f1: {}, f2: {}. wait 10 seconds", input.getF1(), input.getF2());
    Thread.sleep(10000);
    return String.format("%s, %s", input.getF1(), input.getF2());
  }

}
