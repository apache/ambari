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
var testHelpers = require('test/helpers');

describe('App.ManageJournalNodeWizardStep3Controller', function () {
  var controller;

  beforeEach(function () {
    controller = App.ManageJournalNodeWizardStep3Controller.create({
      isHDFSNameSpacesLoaded: false
    });
  });

  describe('#pullCheckPointsStatuses()', function () {

    beforeEach(function () {
      this.mockHDFS = sinon.stub(App.HDFSService, 'find');
      sinon.stub(App.HostComponent, 'find').returns(Em.Object.create({workStatus: 'STOPPED'}));
      sinon.stub(controller, 'pullCheckPointStatus');
      sinon.stub(controller, 'removeObserver');
      sinon.stub(controller, 'addObserver');
    })

    afterEach(function () {
      this.mockHDFS.restore();
      App.HostComponent.find.restore();
      controller.pullCheckPointStatus.restore();
      controller.removeObserver.restore();
      controller.addObserver.restore();
    })

    it('should execute addObserver function', function () {
      controller.pullCheckPointsStatuses();
      expect(controller.addObserver.calledOnce).to.be.true;
    });

    it('should send check point statuses{1}', function () {
      controller.set('isHDFSNameSpacesLoaded', true);
      this.mockHDFS.returns(Em.Object.create({
        masterComponentGroups: [
          {
            hosts: ['host1']
          },
          {
            hosts: ['host2']
          }
        ],
        activeNameNodes: [{hostName: 'host1'}]
      }));
      controller.pullCheckPointsStatuses();
      var args = testHelpers.findAjaxRequest('name', 'admin.high_availability.getNnCheckPointsStatuses');
      expect(args[0].data.hostNames.join(', ')).to.eql('host1, host2');
    });

    it('should send check point statuses{2}', function () {
      controller.set('isHDFSNameSpacesLoaded', true);
      this.mockHDFS.returns(Em.Object.create({
        masterComponentGroups: [
          {
            hosts: ['host1']
          },
          {
            hosts: ['host2']
          }
        ],
        activeNameNodes: [{hostName: 'host1'}, {hostName: 'host2'}]
      }));
      controller.pullCheckPointsStatuses();
      var args = testHelpers.findAjaxRequest('name', 'admin.high_availability.getNnCheckPointsStatuses');
      expect(args[0].data.hostNames.join(', ')).to.eql('host1, host2');
    })

    it('should execute pullCheckPointStatus function', function () {
      controller.set('isHDFSNameSpacesLoaded', true);
      this.mockHDFS.returns(Em.Object.create({
        masterComponentGroups: [
          {
            hosts: ['host1']
          }
        ],
        activeNameNodes: [{hostName: 'host1'}]
      }));
      controller.pullCheckPointsStatuses();
      expect(controller.pullCheckPointStatus.calledOnce).to.be.true;
    });
  });

  describe('#checkNnCheckPointsStatuses()', function () {
    var data = {};
    beforeEach(function () {
      sinon.stub(controller, 'pullCheckPointsStatuses');
      sinon.stub(controller, 'setProperties');
      this.clock = sinon.useFakeTimers();
      controller.checkNnCheckPointsStatuses(data);
    })

    afterEach(function () {
      controller.pullCheckPointsStatuses.restore();
      controller.setProperties.restore();
      this.clock.restore();
    })

    it('should execute setProperties function', function () {
      expect(controller.setProperties.calledOnce).to.be.true;
    });

    it('should execute pullCheckPointsStatuses function', function () {
      controller.set('POLL_INTERVAL', 1000);
      this.clock.tick(controller.POLL_INTERVAL);
      expect(controller.pullCheckPointsStatuses.calledOnce).to.be.true;
    });
  });

});
