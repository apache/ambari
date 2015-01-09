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
require('controllers/main/admin/highAvailability_controller');
require('models/host_component');
require('models/host');
require('utils/ajax/ajax');

describe('App.MainAdminHighAvailabilityController', function () {

  var controller = App.MainAdminHighAvailabilityController.create();

  describe('#enableHighAvailability()', function () {

    var hostComponents = [];

    beforeEach(function () {
      sinon.stub(App.router, 'transitionTo', Em.K);
      sinon.stub(App.HostComponent, 'find', function(){
        return hostComponents;
      });
      sinon.spy(controller, "showErrorPopup");
    });
    afterEach(function () {
      App.router.transitionTo.restore();
      controller.showErrorPopup.restore();
      App.HostComponent.find.restore();
    });

    it('NAMENODE in INSTALLED state', function () {
      hostComponents = [
        Em.Object.create({
          componentName: 'NAMENODE',
          workStatus: 'INSTALLED'
        }),
        Em.Object.create({
          componentName: 'ZOOKEEPER_SERVER',
          workStatus: 'INSTALLED'
        }),
        Em.Object.create({
          componentName: 'ZOOKEEPER_SERVER',
          workStatus: 'INSTALLED'
        }),
        Em.Object.create({
          componentName: 'ZOOKEEPER_SERVER',
          workStatus: 'INSTALLED'
        })
      ];

      sinon.stub(App.router, 'get', function(){
        return 3;
      });
      expect(controller.enableHighAvailability()).to.be.false;
      expect(controller.showErrorPopup.calledOnce).to.be.true;
      App.router.get.restore();
    });
    it('Cluster has less than 3 ZOOKEPER_SERVER components', function () {
      hostComponents = [
        Em.Object.create({
          componentName: 'NAMENODE',
          workStatus: 'STARTED'
        })
      ];

      sinon.stub(App.router, 'get', function(){
        return 3;
      });
      expect(controller.enableHighAvailability()).to.be.false;
      expect(controller.showErrorPopup.called).to.be.true;
      App.router.get.restore();
    });
    it('total hosts number less than 3', function () {
      hostComponents = [
        Em.Object.create({
          componentName: 'NAMENODE',
          workStatus: 'STARTED'
        }),
        Em.Object.create({
          componentName: 'ZOOKEEPER_SERVER',
          workStatus: 'INSTALLED'
        }),
        Em.Object.create({
          componentName: 'ZOOKEEPER_SERVER',
          workStatus: 'INSTALLED'
        }),
        Em.Object.create({
          componentName: 'ZOOKEEPER_SERVER',
          workStatus: 'INSTALLED'
        })
      ];
      sinon.stub(App.router, 'get', function () {
        return 1;
      });
      expect(controller.enableHighAvailability()).to.be.false;
      expect(controller.showErrorPopup.calledOnce).to.be.true;
      App.router.get.restore();
    });
    it('All checks passed', function () {
      hostComponents = [
        Em.Object.create({
          componentName: 'NAMENODE',
          workStatus: 'STARTED'
        }),
        Em.Object.create({
          componentName: 'ZOOKEEPER_SERVER',
          workStatus: 'INSTALLED'
        }),
        Em.Object.create({
          componentName: 'ZOOKEEPER_SERVER',
          workStatus: 'INSTALLED'
        }),
        Em.Object.create({
          componentName: 'ZOOKEEPER_SERVER',
          workStatus: 'INSTALLED'
        })
      ];
      sinon.stub(App.router, 'get', function(){
        return 3;
      });
      expect(controller.enableHighAvailability()).to.be.true;
      expect(App.router.transitionTo.calledWith('main.services.enableHighAvailability')).to.be.true;
      expect(controller.showErrorPopup.calledOnce).to.be.false;
      App.router.get.restore();
    });
  });

  describe('#joinMessage()', function () {
    it('message is empty', function () {
      var message = [];
      expect(controller.joinMessage(message)).to.be.empty;
    });
    it('message is array from two strings', function () {
      var message = ['yes', 'no'];
      expect(controller.joinMessage(message)).to.equal('yes<br/>no');
    });
    it('message is string', function () {
      var message = 'hello';
      expect(controller.joinMessage(message)).to.equal('<p>hello</p>');
    });
  });

});
