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
require('controllers/main/service/reassign_controller');

describe('App.ReassignMasterController', function () {

  var reassignMasterController = App.ReassignMasterController.create({});

  describe('#totalSteps', function () {

    var cases = [
      {
        componentName: 'ZOOKEEPER_SERVER',
        result: 4
      },
      {
        componentName: 'RESOURCE_MANAGER',
        result: 4
      },
      {
        componentName: 'OOZIE_SERVER',
        result: 6
      },
      {
        componentName: 'APP_TIMELINE_SERVER',
        result: 6
      },
      {
        componentName: 'NAMENODE',
        result: 6
      }
    ];

    cases.forEach(function (c) {
      it('check ' + c.componentName, function () {
        reassignMasterController.set('content.reassign', {'component_name': c.componentName});
        expect(reassignMasterController.get('totalSteps')).to.equal(c.result);
        reassignMasterController.set('content.reassign', {service_id:null});
      });
    });
  });

  reassignMasterController.set('content.reassign', {service_id:null});

});
