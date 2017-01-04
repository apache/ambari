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
require('controllers/main/admin/highAvailability/journalNode/step1_controller');

describe('App.ManageJournalNodeWizardStep6Controller', function () {
  var controller;

  beforeEach(function () {
    controller = App.ManageJournalNodeWizardStep6Controller.create({
      content: Em.Object.create()
    });
  });

  describe('#startZooKeeperServers', function() {

    beforeEach(function() {
      sinon.stub(controller, 'updateComponent');
    });

    afterEach(function() {
      controller.updateComponent.restore();
    });

    it('updateComponent should be called', function() {
      controller.set('content.masterComponentHosts', [
        {
          component: 'ZOOKEEPER_SERVER',
          hostName: 'host1'
        }
      ]);
      controller.startZooKeeperServers();
      expect(controller.updateComponent.calledWith('ZOOKEEPER_SERVER', ['host1'], "ZOOKEEPER", "Start")).to.be.true;
    });
  });

  describe('#startActiveNameNode', function() {

    beforeEach(function() {
      sinon.stub(controller, 'updateComponent');
    });

    afterEach(function() {
      controller.updateComponent.restore();
    });

    it('updateComponent should be called', function() {
      controller.set('content.activeNN', {
        host_name: 'host1'
      });
      controller.startActiveNameNode();
      expect(controller.updateComponent.calledWith('NAMENODE', 'host1', "HDFS", "Start")).to.be.true;
    });
  });

});

