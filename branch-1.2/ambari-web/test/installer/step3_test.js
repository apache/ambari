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
require('controllers/wizard/step3_controller');

/*
describe('App.InstallerStep3Controller', function () {
  //var controller = App.InstallerStep3Controller.create();

  describe('#parseHostInfo', function () {
    var controller = App.InstallerStep3Controller.create();
    it('should return true if there is no host with pending status in the data provided by REST bootstrap call.  It should also update the status on the client side', function () {
      var hostFromServer = [
        {
          name: '192.168.1.1',
          status: 'error'
        },
        {
          name: '192.168.1.2',
          status: 'success'
        },
        {
          name: '192.168.1.3',
          status: 'error'
        },
        {
          name: '192.168.1.4',
          status: 'success'
        }
      ];
      controller.content.pushObject(App.HostInfo.create({
        name: '192.168.1.1',
        status: 'error'
      }));
      controller.content.pushObject(App.HostInfo.create({
        name: '192.168.1.2',
        status: 'success'
      }));
      controller.content.pushObject(App.HostInfo.create({
        name: '192.168.1.3',
        status: 'pending'        //status should be overriden to 'error' after the parseHostInfo call
      }));
      controller.content.pushObject(App.HostInfo.create({
        name: '192.168.1.4',
        status: 'success'
      }));

      var result = controller.parseHostInfo(hostFromServer, controller.content);
      var host = controller.content.findProperty('name', '192.168.1.3');
      expect(result).to.equal(true);
      expect(host.bootStatus).to.equal('error');
    })
  })


  describe('#onAllChecked', function () {
    var controller = App.InstallerStep3Controller.create();
    it('should set all visible hosts\'s isChecked to true upon checking the "all" checkbox', function () {
      controller.set('category', 'All Hosts');
      controller.set('allChecked', true);
      controller.content.pushObject(App.HostInfo.create({
        name: '192.168.1.1',
        status: 'error',
        isChecked: false
      }));
      controller.content.pushObject(App.HostInfo.create({
        name: '192.168.1.2',
        status: 'success',
        isChecked: false
      }));
      controller.content.pushObject(App.HostInfo.create({
        name: '192.168.1.3',
        status: 'pending', //status should be overriden to 'error' after the parseHostInfo call
        isChecked: true
      }));
      controller.content.pushObject(App.HostInfo.create({
        name: '192.168.1.4',
        status: 'success',
        isChecked: false
      }));
      controller.onAllChecked();
      controller.content.forEach(function (host) {
        var result = host.get('isChecked');
        expect(result).to.equal(true);
      });

    })
  })
})

*/
