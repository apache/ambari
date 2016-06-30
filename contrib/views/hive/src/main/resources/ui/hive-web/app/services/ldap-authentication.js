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
import constants from 'hive/utils/constants';
import ENV from '../config/environment';

export default Ember.Service.extend({

  authenticateLdapPassword: function(password){
      var password = password;
      var pathName = window.location.pathname;
      var pathNameArray = pathName.split("/");
      var hiveViewVersion = pathNameArray[3];
      var hiveViewName = pathNameArray[4];
      var ldapAuthURL = "/api/v1/views/HIVE/versions/"+ hiveViewVersion + "/instances/" + hiveViewName + "/jobs/auth";

      return Ember.$.ajax({
        url: ldapAuthURL,
        type: 'post',
        headers: {'X-Requested-With': 'XMLHttpRequest', 'X-Requested-By': 'ambari'},
        contentType: 'application/json',
        data: JSON.stringify({ "password" : password}),
      })
  }
});
