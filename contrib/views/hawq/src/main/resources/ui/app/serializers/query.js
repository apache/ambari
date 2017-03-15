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

import Ember from 'ember';
import DS from 'ember-data';

export default DS.JSONAPISerializer.extend({
  normalizeArrayResponse(store, primaryModelClass, payload, id, requestType) {
    this.transformPayload(payload);
    return this._super(store, primaryModelClass, payload, id, requestType);
  },

  keyForAttribute() {
    // attribute keys are in underscore format
    return Ember.String.underscore(...arguments);
  },

  transformPayload(payload) {
    // map attribute key name to the name expected by Query model
    const attributesMap = {
      "datname": "database_name",
      "procpid": "pid",
      "usename": "user_name",
      "current_query": "query_text",
      "query_duration": "duration",
      "query_start": "query_start_time",
      "client_addr": "client_host"
    };

    const attributesMapKeys = Object.keys(attributesMap);

    // generate data as expected by Ember DS
    payload.data = [];

    payload.items.forEach(function (dataItem) {
      let tempItem = {"attributes": {}};

      Object.keys(dataItem).forEach(function (dataItemKey) {
        // if key is 'type' update value of 'type' as 'queries' for array response
        tempItem[dataItemKey] = (dataItemKey === 'type') ? 'queries' : dataItem[dataItemKey];
      }, this);

      Object.keys(dataItem.attributes).forEach(function (attributeKey) {
        tempItem.attributes[attributesMapKeys.contains(attributeKey) ? attributesMap[attributeKey] : attributeKey] = dataItem.attributes[attributeKey];
      }, this);

      payload.data.push(tempItem);
    }, this);

    // delete additional key-value pairs from payload
    delete payload.items;
    delete payload.href;
    // the payload will contain only one key (data) at this stage
  }
});