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
require('controllers/main/admin/security/security_progress_controller');
require('controllers/main/admin/security/add/step4');
require('utils/polling');
require('models/cluster_states');

describe('App.MainAdminSecurityAddStep4Controller', function () {

  /**
   * Test object
   */
  var controller = App.MainAdminSecurityAddStep4Controller.create();

  describe('#moveToNextCommand()', function () {
    controller.reopen({
      saveCommands: function(){},
      enableSubmit: function(){},
      loadClusterConfigs: function(){}
    });
    App.clusterStatus.reopen({
      setClusterStatus: function(){}
    });

    controller.set('commands', [
      App.Poll.create({name: 'STOP_SERVICES', isStarted: false, isPolling: true, isCompleted: false, start: function(){}}),
      App.Poll.create({name: 'APPLY_CONFIGURATIONS', isStarted: false, isPolling: false, isCompleted: false, start: function(){}}),
      App.Poll.create({name: 'START_SERVICES', isStarted: false, isPolling: true, isCompleted: false, start: function(){}})
    ]);

    it('STOP_SERVICES is started', function(){
      controller.moveToNextCommand(controller.get('commands').findProperty('name', 'STOP_SERVICES'));
      expect(controller.get('commands').findProperty('name', 'STOP_SERVICES').get('isStarted')).to.equal(true);
    });

    it('APPLY_CONFIGURATIONS is started', function(){
      controller.moveToNextCommand(controller.get('commands').findProperty('name', 'APPLY_CONFIGURATIONS'));
      expect(controller.get('commands').findProperty('name', 'APPLY_CONFIGURATIONS').get('isStarted')).to.equal(true);
    });

    it('START_SERVICES is started', function(){
      controller.moveToNextCommand(controller.get('commands').findProperty('name', 'START_SERVICES'));
      expect(controller.get('commands').findProperty('name', 'START_SERVICES').get('isStarted')).to.equal(true);
    });
  });

  describe('#loadCommands()', function() {
    describe('YARN installed with ATS', function() {
      beforeEach(function(){
        controller.reopen({
          secureServices: function() {
            return [
              Em.Object.create({
                serviceName: 'YARN'
              })
            ];
          }.property()
        });
        controller.set('commands', []);
        controller.set('totalSteps', 3);
        var service = {
          id: 'YARN',
          service_name: 'YARN',
          host_components: ['APP_TIMLINE_SERVER_c6401.ambari.apache.org']
        };
        var hostComponent = {
          component_name: 'APP_TIMELINE_SERVER',
          id: 'APP_TIMLINE_SERVER_c6401.ambari.apache.org',
          service_id: 'YARN'
        };
        App.store.load(App.HostComponent, hostComponent);
        App.store.load(App.Service, service);
        controller.loadCommands();
      });

      it('delete ATS component stage should be after APPLY_CONFIGURATIONS', function() {
        expect(controller.get('commands').indexOf(controller.get('commands').findProperty('name','DELETE_ATS'))).to.eql(2);
      });

      it('commands length should be equal to 4', function() {
        expect(controller.get('commands').length).to.eql(4);
      });

      it('total steps should be equal to 4', function() {
        expect(controller.get('totalSteps')).to.eql(4);
      });
    });

    describe('YARN installed without ATS', function() {
      beforeEach(function(){
        controller.reopen({
          secureServices: function() {
            return [
              Em.Object.create({
                serviceName: 'YARN'
              })
            ];
          }.property()
        });
        controller.set('commands', []);
        controller.set('totalSteps', 3);
        var service = {
          id: 'YARN',
          service_name: 'YARN',
          host_components: []
        };
        App.store.load(App.Service, service);
        controller.loadCommands();
      });

      it('commands length should be equal to 3', function() {
        expect(controller.get('commands').length).to.eql(3);
      });

      it('total steps should be equal to 3', function() {
        expect(controller.get('totalSteps')).to.eql(3);
      });
    });

  });
});
