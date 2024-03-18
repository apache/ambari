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

public enum MessageEnums {

  // Common Errors
  DATA_NOT_FOUND("logsearch.error.data_not_found", "Data not found"),
  OPER_NOT_ALLOWED_FOR_STATE("logsearch.error.oper_not_allowed_for_state", "Operation not allowed in current state"),
  OPER_NOT_ALLOWED_FOR_ENTITY("logsearch.error.oper_not_allowed_for_state", "Operation not allowed for entity"),
  OPER_NO_PERMISSION("logsearch.error.oper_no_permission", "User doesn't have permission to perform this operation"),
  DATA_NOT_UPDATABLE("logsearch.error.data_not_updatable", "Data not updatable"),
  ERROR_CREATING_OBJECT("logsearch.error.create_object", "Error creating object"),
  ERROR_DUPLICATE_OBJECT("logsearch.error.duplicate_object", "Error creating duplicate object"),
  ERROR_SYSTEM("logsearch.error.system", "System Error. Please try later."),
  SOLR_ERROR("logsearch.solr.error","Something went wrong, For more details check the logs or configuration."),
  ZNODE_NOT_READY("logsearch.zk.znode.error", "ZNode is not available."),
  ZK_CONFIG_NOT_READY("logsearch.zk.config.error", "Collection configuration has not uploaded yet"),
  SOLR_COLLECTION_NOT_READY("logsearch.solr.collection.error", "Solr has not accessible yet for collection."),
  CONFIGURATION_NOT_AVAILABLE("logsearch.config.not_available", "Log Search configuration is not available"),
  CONFIGURATION_API_DISABLED("logsearch.config.api.disabled", "Log Search configuration is not available"),
  SOLR_CONFIGURATION_API_SOLR_NOT_AVAILEBLE("logsearch.config.api.solr.not.available", "Solr as log level filter manager source is not available"),
  // Common Validations
  INVALID_PASSWORD("logsearch.validation.invalid_password", "Invalid password"),
  INVALID_INPUT_DATA("logsearch.validation.invalid_input_data", "Invalid input data"),
  NO_INPUT_DATA("logsearch.validation.no_input_data", "Input data is not provided"),
  INPUT_DATA_OUT_OF_BOUND("logsearch.validation.data_out_of_bound", "Input data if out of bound"),
  NO_NAME("logsearch.validation.no_name", "Name is not provided"),
  NO_OR_INVALID_COUNTRY_ID("logsearch.validation.no_country_id", "Valid Country Id was not provided"),
  NO_OR_INVALID_CITY_ID("logsearch.validation.no_city_id", "Valid City Id was not provided"),
  NO_OR_INVALID_STATE_ID("logsearch.validation.no_state_id", "Valid State Id was not provided");

  private String rbKey;
  private String messageDesc;

  private MessageEnums(String rbKey, String messageDesc) {
    this.rbKey = rbKey;
    this.messageDesc = messageDesc;
  }

  public MessageData getMessage() {
    MessageData msg = new MessageData();
    msg.setName(this.toString());
    msg.setRbKey(rbKey);
    msg.setMessage(messageDesc);
    return msg;
  }

  public MessageData getMessage(Long objectId, String fieldName) {
    MessageData msg = new MessageData();
    msg.setName(this.toString());
    msg.setRbKey(rbKey);
    msg.setMessage(messageDesc);
    msg.setObjectId(objectId);
    msg.setFieldName(fieldName);
    return msg;
  }
}
