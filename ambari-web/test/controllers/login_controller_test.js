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


var App = require('app');
require('models/cluster');
require('controllers/wizard');
require('controllers/installer');

describe('App.LoginController', function () {

  var loginController = App.LoginController.create();

  describe('#postLogin', function() {
    it ('Should set error connect', function() {
      loginController.postLogin(false, false, false);
      expect(loginController.get('errorMessage')).to.be.equal('Unable to connect to Ambari Server. Confirm Ambari Server is running and you can reach Ambari Server from this machine.');
    });
    it ('Should set error login', function() {
      loginController.postLogin(true, false, 'User is disabled');
      expect(loginController.get('errorMessage')).to.be.equal('Unable to sign in. Invalid username/password combination.');
    });
    it ('Should set error', function() {
      loginController.postLogin(true, false, '');
      expect(loginController.get('errorMessage')).to.be.equal('Unable to sign in. Invalid username/password combination.');
    });
  });

});
