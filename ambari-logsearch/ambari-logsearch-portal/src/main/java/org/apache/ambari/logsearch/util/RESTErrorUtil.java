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
package org.apache.ambari.logsearch.util;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.ambari.logsearch.view.VMessage;
import org.apache.ambari.logsearch.view.VResponse;
import org.apache.log4j.Logger;

public class RESTErrorUtil {
  private static final Logger logger = Logger.getLogger(RESTErrorUtil.class);

  private RESTErrorUtil() {
    throw new UnsupportedOperationException();
  }
  
  public static WebApplicationException createRESTException(VResponse response) {
    return createRESTException(response, HttpServletResponse.SC_BAD_REQUEST);
  }

  public static WebApplicationException createRESTException(String errorMessage, MessageEnums messageEnum) {
    List<VMessage> messageList = new ArrayList<VMessage>();
    messageList.add(messageEnum.getMessage());

    VResponse response = new VResponse();
    response.setStatusCode(VResponse.STATUS_ERROR);
    response.setMsgDesc(errorMessage);
    response.setMessageList(messageList);
    WebApplicationException webAppEx = createRESTException(response);
    logger.error("Operation error. response=" + response, webAppEx);
    return webAppEx;
  }

  private static WebApplicationException createRESTException(VResponse response, int sc) {
    Response errorResponse = Response.status(sc).entity(response).build();
    WebApplicationException restException = new WebApplicationException(errorResponse);
    restException.fillInStackTrace();
    return restException;
  }

}