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

export default Ember.Service.extend({

  requestLdapPassword: function (context, callback) {

    var url = context.container.lookup('adapter:application').buildURL() + '/resources/connection/';
    var defer = Ember.RSVP.defer();
    var self = context;

    self.send('openModal', 'modal-save', {
      heading: "modals.authenticationLDAP.heading",
      text: "",
      type: "password",
      defer: defer
    });

    return defer.promise.then(function (text) {
      // make a post call with the given ldap password.
      var password = text;
      var ldapAuthURL = url + "auth";

      $.ajax({
        url: ldapAuthURL,
        type: 'post',
        headers: {'X-Requested-With': 'XMLHttpRequest', 'X-Requested-By': 'ambari'},
        contentType: 'application/json',
        data: JSON.stringify({"password": password}),
        success: function (data, textStatus, jQxhr) {
          console.log("LDAP done: " + data);
          callback();
        },
        error: function (jqXhr, textStatus, errorThrown) {
          console.log("LDAP fail: " + errorThrown);
          self.get('notifyService').error("Wrong Credentials.");
        }
      });
    });

  }
});
