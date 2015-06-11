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

import DS from 'ember-data';
import constants from 'hive/utils/constants';

export default DS.RESTAdapter.extend({
  headers: {
    'X-Requested-By': 'ambari',
    'Content-Type': 'application/json'
    //,'Authorization': 'Basic YWRtaW46YWRtaW4='
  },

  buildURL: function () {
    var version = constants.adapter.version,
        instanceName = constants.adapter.instance;

    var params = window.location.pathname.split('/').filter(function (param) {
      return !!param;
    });

    if (params[params.length - 3] === 'HIVE') {
      version = params[params.length - 2];
      instanceName = params[params.length - 1];
    }

    var prefix = constants.adapter.apiPrefix + version + constants.adapter.instancePrefix + instanceName;
    var url = this._super.apply(this, arguments);
    return prefix + url;
  }
});
