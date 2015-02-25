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

describe('App.MainAdminKerberosController', function() {

  var controller = App.MainAdminKerberosController.create({});

  describe('#prepareConfigProperties', function() {
    beforeEach(function() {
      sinon.stub(App.Service, 'find').returns([
        Em.Object.create({ serviceName: 'KERBEROS'}),
        Em.Object.create({ serviceName: 'HDFS' })
      ]);
      this.result = controller.prepareConfigProperties([
        Em.Object.create({ name: 'prop1', isEditable: true, serviceName: 'SERVICE1'}),
        Em.Object.create({ name: 'prop2', isEditable: true, serviceName: 'KERBEROS'}),
        Em.Object.create({ name: 'prop3', isEditable: true, serviceName: 'HDFS'}),
        Em.Object.create({ name: 'prop4', isEditable: true, serviceName: 'Cluster'}),
        Em.Object.create({ name: 'prop5', isEditable: true, serviceName: 'SERVICE1'}),
      ]);
    });

    afterEach(function() {
      App.Service.find.restore();
    });

    ['prop1', 'prop5'].forEach(function(item) {
      it('property `{0}` should be absent'.format(item), function() {
        expect(this.result.findProperty('name', item)).to.be.undefined;
      });
    });

    ['prop2', 'prop3', 'prop4'].forEach(function(item) {
      it('property `{0}` should be present and not editable'.format(item), function() {
        var prop = this.result.findProperty('name', item);
        expect(prop).to.be.ok;
        expect(prop.get('isEditable')).to.be.false;
      });
    });

    it('should take displayType from predefinedSiteProperties', function () {
      sinon.stub(App.config, 'get').withArgs('preDefinedSiteProperties').returns([
        {
          name: 'hadoop.security.auth_to_local',
          displayType: 'multiLine'
        }
      ]);
      expect(controller.prepareConfigProperties([
        Em.Object.create({
          name: 'hadoop.security.auth_to_local',
          serviceName: 'HDFS'
        })
      ])[0].get('displayType')).to.equal('multiLine');
      App.config.get.restore();
    });
  });

  describe("#runSecurityCheckSuccess()", function () {
    beforeEach(function () {
      sinon.stub(App, 'showClusterCheckPopup', Em.K);
      sinon.stub(controller, 'startKerberosWizard', Em.K);
    });
    afterEach(function () {
      App.showClusterCheckPopup.restore();
      controller.startKerberosWizard.restore();
    });
    it("shows popup", function () {
      var check =  { items: [{
        UpgradeChecks: {
          "check": "Work-preserving RM/NM restart is enabled in YARN configs",
          "status": "FAIL",
          "reason": "FAIL",
          "failed_on": [],
          "check_type": "SERVICE"
        }
      }]};
      controller.runSecurityCheckSuccess(check,null,{label: "name"});
      expect(controller.startKerberosWizard.called).to.be.false;
      expect(App.showClusterCheckPopup.called).to.be.true;
    });
    it("runs startKerberosWizard", function () {
      var check = { items: [{
        UpgradeChecks: {
          "check": "Work-preserving RM/NM restart is enabled in YARN configs",
          "status": "PASS",
          "reason": "OK",
          "failed_on": [],
          "check_type": "SERVICE"
        }
      }]};
      controller.runSecurityCheckSuccess(check,null,{label: "name"});
      expect(controller.startKerberosWizard.called).to.be.true;
      expect(App.showClusterCheckPopup.called).to.be.false;
    });
  });

  describe('#regenerateKeytabs()', function () {

    beforeEach(function () {
      sinon.spy(App.ModalPopup, "show");
      sinon.stub(App.ajax, 'send', Em.K);
    });
    afterEach(function () {
      App.ModalPopup.show.restore();
      App.ajax.send.restore();
    });

    it('confirm popup should be displayed', function () {
      var popup = controller.regenerateKeytabs();
      expect(App.ModalPopup.show.calledOnce).to.be.true;
      popup.onPrimary();
      expect(App.ajax.send.calledOnce).to.be.true;
    });

    it('user checked regeneration only for missing host/components', function () {
      var popup = controller.regenerateKeytabs();
      popup.set('regenerateKeytabsOnlyForMissing', true);
      popup.onPrimary();
      expect(App.ajax.send.args[0][0].data.type).to.equal('missing');
    });

    it('user didn\'t check regeneration only for missing host/components', function () {
      var popup = controller.regenerateKeytabs();
      popup.onPrimary();
      expect(App.ajax.send.args[0][0].data.type).to.equal('all');
    });

  });

  describe('#loadStep', function() {

    it('security enabled but kerberos descriptor resource doesn\'t exist for cluster, security can be enabled', function() {
      var controller = App.MainAdminKerberosController.create({ securityEnabled: true });
      sinon.stub(App.router, 'get').withArgs('mainAdminKerberosController.securityEnabled').returns(true);
      sinon.spy(controller, 'setStepConfigs');
      sinon.stub(controller, 'loadClusterDescriptorConfigs').returns($.Deferred().reject());
      controller.loadStep();
      App.router.get.restore();
      controller.loadClusterDescriptorConfigs.restore();
      // configurations will not set
      expect(controller.setStepConfigs.called).to.be.false;
      controller.setStepConfigs.restore();
      // security status should change to `false`
      expect(controller.get('securityEnabled')).to.be.false;
    });

    it('security enabled and kerberos descriptor artifacts resource saved, security was enabled successfully', function() {
      var controller = App.MainAdminKerberosController.create({ securityEnabled: true });
      sinon.stub(App.router, 'get').withArgs('mainAdminKerberosController.securityEnabled').returns(true);
      sinon.stub(controller, 'setStepConfigs').returns(true);
      sinon.stub(controller, 'createServicesStackDescriptorConfigs').returns($.Deferred().resolve());
      sinon.stub(controller, 'loadClusterDescriptorConfigs').returns($.Deferred().resolve());
      controller.loadStep();
      App.router.get.restore();
      controller.createServicesStackDescriptorConfigs.restore();
      controller.loadClusterDescriptorConfigs.restore();
      // configurations will set with current descriptor object
      expect(controller.setStepConfigs.called).to.be.true;
      controller.setStepConfigs.restore();
    });

  });
});
