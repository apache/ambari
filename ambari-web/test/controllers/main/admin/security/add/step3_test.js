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

require('controllers/main/admin/security/add/step3');
require('models/host_component');
require('models/host');
require('models/service');

describe('App.MainAdminSecurityAddStep3Controller', function () {

  var mainAdminSecurityAddStep3Controller = App.MainAdminSecurityAddStep3Controller.create();

  describe('#getSecurityUsers', function() {
    it('no hosts, just check users (testMode = true)', function() {
      App.testMode = true;
      expect(mainAdminSecurityAddStep3Controller.getSecurityUsers().length).to.equal(11);
    });
  });

  describe('#changeDisplayName', function() {
    it('HiveServer2', function() {
      expect(mainAdminSecurityAddStep3Controller.changeDisplayName('HiveServer2')).to.equal('Hive Metastore and HiveServer2');
    });
    it('Not HiveServer2', function() {
      expect(mainAdminSecurityAddStep3Controller.changeDisplayName('something')).to.equal('something');
    });
  });

});
