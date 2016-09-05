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

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.model.request.impl.UserConfigRequest;
import org.apache.ambari.logsearch.query.model.UserConfigSearchCriteria;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class UserConfigRequestConverter implements Converter<UserConfigRequest, UserConfigSearchCriteria> {

  @Override
  public UserConfigSearchCriteria convert(UserConfigRequest request) {
    UserConfigSearchCriteria criteria = new UserConfigSearchCriteria();
    criteria.addParam(LogSearchConstants.USER_NAME, request.getUserId());
    criteria.addParam(LogSearchConstants.FILTER_NAME, request.getFilterName());
    criteria.addParam(LogSearchConstants.ROW_TYPE, request.getRowType());
    return criteria;
  }
}
