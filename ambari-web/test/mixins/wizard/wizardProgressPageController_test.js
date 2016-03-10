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

describe('App.wizardProgressPageControllerMixin', function() {
  var mixedObject = Em.Object.extend(App.wizardProgressPageControllerMixin, {});

  describe('#createComponent', function() {
    var mixedObjectInstance;
    beforeEach(function() {
      mixedObjectInstance = mixedObject.create({});
      sinon.stub(App.ajax, 'send', function(params) {
        return $.extend(params,{complete: function(callback){
          callback();
        }});
      });
      sinon.stub(mixedObjectInstance, "updateAndCreateServiceComponent").returns({
        done: function(callback) {
          return callback();
        }
      });
      sinon.spy(mixedObjectInstance, 'onCreateComponent');
      sinon.spy(mixedObjectInstance, 'updateComponent');
      sinon.stub(mixedObjectInstance, 'checkInstalledComponents', function(componentName, hostNames) {
        var def = $.Deferred();
        var data = {
          'ZOOKEEPER_SERVER': {
            items: []
          },
          'ZOOKEEPER_CLIENT': {
            items: [
              { HostRoles: { host_name: 'host1' } }
            ]
          }
        };
        def.resolve(data[componentName]);
        return def.promise();
      });
      sinon.stub(App.StackServiceComponent, 'find', function(){
        return [
          Em.Object.create({
          componentName: 'ZOOKEEPER_CLIENT',
          serviceName: 'ZOOKEEPER'
        }),
          Em.Object.create({
          componentName: 'ZOOKEEPER_SERVER',
          serviceName: 'ZOOKEEPER'
        })
        ];
      });
      App.serviceComponents = ['ZOOKEEPER_SERVER', 'ZOOKEEPER_CLIENT'];
    });

    afterEach(function() {
      App.ajax.send.restore();
      App.StackServiceComponent.find.restore();
      mixedObjectInstance.updateAndCreateServiceComponent.restore();
      mixedObjectInstance.onCreateComponent.restore();
      mixedObjectInstance.updateComponent.restore();
      mixedObjectInstance.checkInstalledComponents.restore();
    });

    it('should call `checkInstalledComponents` method', function() {
      mixedObjectInstance.createComponent('ZOOKEEPER_SERVER', 'host1', 'ZOOKEEPER');
      expect(mixedObjectInstance.checkInstalledComponents.called).to.be.true;
    });

    it('should call `checkInstalledComponents` method with host name converted to Array', function() {
      mixedObjectInstance.createComponent('ZOOKEEPER_SERVER', 'host1', 'ZOOKEEPER');
      expect(mixedObjectInstance.checkInstalledComponents.calledWith('ZOOKEEPER_SERVER', ['host1'])).to.be.true;
    });

    it('no ZooKeeper Servers installed. install on host1, host2. ajax request should be called with appropriate params', function() {
      mixedObjectInstance.createComponent('ZOOKEEPER_SERVER', ['host1', 'host2'], 'ZOOKEEPER');
      var args = App.ajax.send.args[0][0];

      var queryObject = JSON.parse(args.data.data);
      expect(args.data.hostName).to.be.eql(['host1', 'host2']);
      expect(queryObject.RequestInfo.query).to.be.eql('Hosts/host_name=host1|Hosts/host_name=host2');
      expect(queryObject.Body.host_components[0].HostRoles.component_name).to.be.eql('ZOOKEEPER_SERVER');
      expect(args.data.taskNum).to.be.eql(1);
      // invoke callback
      args.sender[args.success].apply(args.sender, [null, null, args.data]);
      expect(mixedObjectInstance.updateComponent.called).to.be.true;
    });

    it('ZooKeeper Client installed on host1. install on host1, host2. ajax request should be called with appropriate params', function() {
      mixedObjectInstance.createComponent('ZOOKEEPER_CLIENT', ['host1', 'host2'], 'ZOOKEEPER');
      var args = App.ajax.send.args[0][0];
      var queryObject = JSON.parse(args.data.data);
      expect(args.data.hostName).to.be.eql(['host1', 'host2']);
      expect(queryObject.RequestInfo.query).to.be.eql('Hosts/host_name=host2');
      expect(queryObject.Body.host_components[0].HostRoles.component_name).to.be.eql('ZOOKEEPER_CLIENT');
      expect(mixedObjectInstance.onCreateComponent.called).to.be.false;
      // invoke callback
      args.sender[args.success].apply(args.sender, [null, null, args.data]);
      expect(mixedObjectInstance.updateComponent.called).to.be.true;
    });
  });

  describe('#updateComponent', function() {
    var testsAjax = [
      {
        callParams: ['ZOOKEEPER_SERVER', 'host1', 'ZOOKEEPER', 'Install', 1],
        e: [
          { key: 'data.HostRoles.state', value: 'INSTALLED'},
          { key: 'data.hostName[0]', value: 'host1'},
          { key: 'data.query', value: 'HostRoles/component_name=ZOOKEEPER_SERVER&HostRoles/host_name.in(host1)&HostRoles/maintenance_state=OFF'}
        ]
      },
      {
        callParams: ['ZOOKEEPER_SERVER', ['host1', 'host2'], 'ZOOKEEPER', 'start', 1],
        e: [
          { key: 'data.HostRoles.state', value: 'STARTED'},
          { key: 'data.hostName[0]', value: 'host1'},
          { key: 'data.hostName[1]', value: 'host2'},
          { key: 'data.query', value: 'HostRoles/component_name=ZOOKEEPER_SERVER&HostRoles/host_name.in(host1,host2)&HostRoles/maintenance_state=OFF'}
        ]
      }
    ];

    testsAjax.forEach(function(test) {
      describe('called with params: ' + JSON.stringify(test.callParams), function() {
        before(function() {
          sinon.stub(App.ajax, 'send', Em.K);
          var mixedObjectInstance = mixedObject.create({});
          mixedObjectInstance.updateComponent.apply(mixedObjectInstance, test.callParams);
        });

        after(function() {
          App.ajax.send.restore();
        });

        test.e.forEach(function(eKey) {
          it('key: {0} should have value: {1}'.format(eKey.key, eKey.value), function() {
            var args = App.ajax.send.args[0][0];
            expect(args).to.have.deep.property(eKey.key, eKey.value);
          });
        });
      });
    });
  });

  describe('#createInstallComponentTask', function() {
    var mixedObjectInstance;
    beforeEach(function() {
      mixedObjectInstance = mixedObject.create({});
      sinon.stub(mixedObjectInstance, 'createComponent', Em.K);
      sinon.stub(mixedObjectInstance, 'onTaskError', Em.K);
      this.KDCStub = sinon.stub(App, 'get').withArgs('router.mainAdminKerberosController');
    });

    afterEach(function() {
      mixedObjectInstance.createComponent.restore();
      mixedObjectInstance.onTaskError.restore();
      mixedObjectInstance.destroy();
      mixedObjectInstance = null;
      App.get.restore();
      this.KDCStub = null;
    });

    it('when credentials are ok, createComponent method called', function() {
      this.KDCStub.returns({
        getKDCSessionState: function(sCallback, eCallback) {
          sCallback();
        }
      });
      mixedObjectInstance.createInstallComponentTask('componentName', 'hostName', 'serviceName');
      assert.isTrue(mixedObjectInstance.createComponent.calledOnce, 'createComponent should be called');
      assert.equal(JSON.stringify(mixedObjectInstance.createComponent.args[0]), JSON.stringify(['componentName', 'hostName', 'serviceName']), 'passed argument order should be the same');
    });

    it('when credentials are expired and KDC dialog cancelled task status should be changed to failed', function() {
      this.KDCStub.returns({
        getKDCSessionState: function(sCallback, eCallback) {
          eCallback();
        }
      });
      mixedObjectInstance.createInstallComponentTask('componentName', 'hostName', 'serviceName');
      assert.isFalse(mixedObjectInstance.createComponent.calledOnce, 'createComponent should not be called');
      assert.isTrue(mixedObjectInstance.onTaskError.called, 'onTaskError handler called');
    });
  });
});
