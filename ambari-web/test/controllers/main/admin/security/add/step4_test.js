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

  describe('#moveToNextStage()', function () {
    controller.reopen({
      saveStages: function(){},
      enableSubmit: function(){},
      loadClusterConfigs: function(){}
    });
    App.clusterStatus.reopen({
      setClusterStatus: function(){}
    });

    controller.set('stages', [
      App.Poll.create({stage: 'stage2', isStarted: false, isPolling: true, isCompleted: false, start: function(){}}),
      App.Poll.create({stage: 'stage3', isStarted: false, isPolling: false, isCompleted: false, start: function(){}}),
      App.Poll.create({stage: 'stage4', isStarted: false, isPolling: true, isCompleted: false, start: function(){}})
    ]);

    it('stage2 is started', function(){
      controller.moveToNextStage(controller.get('stages').findProperty('stage', 'stage2'));
      expect(controller.get('stages').findProperty('stage', 'stage2').get('isStarted')).to.equal(true);
    });

    it('stage3 is started', function(){
      controller.moveToNextStage(controller.get('stages').findProperty('stage', 'stage3'));
      expect(controller.get('stages').findProperty('stage', 'stage3').get('isStarted')).to.equal(true);
    });

    it('stage4 is started', function(){
      controller.moveToNextStage(controller.get('stages').findProperty('stage', 'stage4'));
      expect(controller.get('stages').findProperty('stage', 'stage4').get('isStarted')).to.equal(true);
    });
  });
});
