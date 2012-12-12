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


var Ember = require('ember');
var App = require('app');
require('models/hosts');
require('controllers/wizard/step9_controller');

/*describe('App.InstallerStep9Controller', function () {
  //var controller = App.InstallerStep3Controller.create();

  describe('#isStepFailed', function () {
    var controller = App.InstallerStep9Controller.create();
    it('should return true if even a single action of a role with 100% success factor fails', function () {
      var polledData = new Ember.Set([
        {
          actionId: '1',
          name: '192.168.1.1',
          status: 'completed',
          sf: '100',
          role: 'DataNode',
          message: 'completed 30%'
        },
        {
          actionId: '2',
          name: '192.168.1.2',
          status: 'completed',
          sf: '100',
          role: 'DataNode',
          message: 'completed 20%'
        },
        {
          actionId: '3',
          name: '192.168.1.3',
          status: 'completed',
          sf: '100',
          role: 'DataNode',
          message: 'completed 30%'
        },
        {
          actionId: '4',
          name: '192.168.1.4',
          status: 'failed',
          sf: '100',
          role: 'DataNode',
          message: 'completed 40%'
        }
      ]);


      expect(controller.isStepFailed(polledData)).to.equal(true);

    })

    it('should return false if action of a role fails but with less percentage than success factor of the role', function () {
      var polledData = new Ember.Set([
        {
          actionId: '1',
          name: '192.168.1.1',
          status: 'failed',
          sf: '30',
          role: 'DataNode',
          message: 'completed 30%'
        },
        {
          actionId: '2',
          name: '192.168.1.2',
          status: 'failed',
          sf: '30',
          role: 'DataNode',
          message: 'completed 20%'
        },
        {
          actionId: '3',
          name: '192.168.1.3',
          status: 'completed',
          sf: '30',
          role: 'DataNode',
          message: 'completed 30%'
        },
        {
          actionId: '4',
          name: '192.168.1.4',
          status: 'completed',
          sf: '30',
          role: 'DataNode',
          message: 'completed 40%'
        }
      ]);

      expect(controller.isStepFailed(polledData)).to.equal(false);

    })

  })

  describe('#setHostsStatus', function () {
    var controller = App.InstallerStep9Controller.create();
    it('sets the status of all hosts in the content to the passed status value', function () {
      var mockData = new Ember.Set(
        {
          actionId: '1',
          name: '192.168.1.1',
          status: 'completed',
          sf: '100',
          role: 'DataNode',
          message: 'completed 30%'
        },
        {
          actionId: '2',
          name: '192.168.1.2',
          status: 'completed',
          sf: '100',
          role: 'DataNode',
          message: 'completed 20%'
        },
        {
          actionId: '3',
          name: '192.168.1.3',
          status: 'completed',
          sf: '100',
          role: 'DataNode',
          message: 'completed 30%'
        },
        {
          actionId: '4',
          name: '192.168.1.4',
          status: 'completed',
          sf: '100',
          role: 'DataNode',
          message: 'completed 40%'
        }
      );
      mockData.forEach(function(_polledData){
        controller.content.pushObject(_polledData);
      });

      controller.setHostsStatus(mockData,'finish');
      var result = controller.content.everyProperty('status','finish');
      //console.log('value of pop is: '+ result.pop.actionId);
      expect(result).to.equal(true);

    })
  })


})*/


