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
package org.apache.ambari.logsearch.query.converter;

import org.apache.ambari.logsearch.model.request.impl.AnyGraphRequest;
import org.apache.ambari.logsearch.query.model.AnyGraphSearchCriteria;
import org.springframework.stereotype.Component;

@Component
public class AnyGraphRequestConverter extends AbstractCommonSearchRequestConverter<AnyGraphRequest, AnyGraphSearchCriteria> {

  @Override
  public AnyGraphSearchCriteria convertToSearchCriteria(AnyGraphRequest anyGraphRequest) {
    AnyGraphSearchCriteria criteria = new AnyGraphSearchCriteria();
    criteria.addParam("xAxis", anyGraphRequest.getxAxis());
    criteria.addParam("yAxis", anyGraphRequest.getyAxis());
    criteria.addParam("stackBy", anyGraphRequest.getStackBy());
    criteria.addParam("unit", anyGraphRequest.getUnit());
    criteria.addParam("from", anyGraphRequest.getFrom());
    criteria.addParam("to", anyGraphRequest.getTo());
    return criteria;
  }
}
