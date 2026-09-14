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

describe('App.db', function () {

  describe('scoped workflow storage', function () {
    afterEach(function () {
      App.db.deactivateWorkflowScope();
      App.db.cleanUp();
      Object.keys(localStorage).filter(function (key) {
        return key.indexOf('ambari-workflow:') === 0;
      }).forEach(function (key) {
        localStorage.removeItem(key);
      });
    });

    it('isolates wizard namespaces by principal, target, and browser tab identity', function () {
      var firstScope = JSON.stringify(['alice', 'clusters', '1', 'tab-a']);
      var secondScope = JSON.stringify(['alice', 'clusters', '1', 'tab-b']);

      App.db.activateWorkflowScope(firstScope);
      App.db.set('AddService', 'currentStep', 3);
      App.db.activateWorkflowScope(secondScope);
      expect(App.db.get('AddService', 'currentStep')).to.be.undefined;

      App.db.set('AddService', 'currentStep', 5);
      App.db.activateWorkflowScope(firstScope);
      expect(App.db.get('AddService', 'currentStep')).to.equal(3);
      expect(localStorage.getObject('ambari').AddService).to.eql({});
    });

    it('removes credentials and authenticated repository URLs before writing a draft', function () {
      var scope = JSON.stringify(['alice', 'drafts', 'draft-a', 'tab-a']);
      App.db.activateWorkflowScope(scope);
      App.db.setProperties('Installer', {
        installOptions: {sshKey: 'private material'},
        serviceConfigProperties: [{
          name: 'database.password',
          value: 'secret-value',
          displayName: 'Database password'
        }],
        repositories: [{baseUrl: 'https://alice:secret@example.test/repo'}]
      });

      var persisted = localStorage.getObject('ambari-workflow:' + encodeURIComponent(scope)).Installer;
      expect(JSON.stringify(persisted)).to.not.contain('private material');
      expect(JSON.stringify(persisted)).to.not.contain('secret-value');
      expect(JSON.stringify(persisted)).to.not.contain('alice:secret');
      expect(persisted.requires_reentry).to.be.true;
    });

    it('keeps entered SSH and KDC credentials only in memory for the active scope', function () {
      var scope = JSON.stringify(['alice', 'clusters', '1', 'tab-a']);
      App.db.activateWorkflowScope(scope);
      App.db.set('Installer', 'installOptions', {sshKey: 'private material'});
      App.db.set('KerberosWizard', 'serviceConfigProperties', [{
        name: 'admin_password',
        value: 'kdc-password'
      }]);

      expect(App.db.get('Installer', 'installOptions').sshKey).to.equal('private material');
      expect(App.db.get('KerberosWizard', 'serviceConfigProperties')[0].value).to.equal('kdc-password');
      var persisted = localStorage.getObject('ambari-workflow:' + encodeURIComponent(scope));
      expect(JSON.stringify(persisted)).to.not.contain('private material');
      expect(JSON.stringify(persisted)).to.not.contain('kdc-password');

      App.db.deactivateWorkflowScope();
      App.db.activateWorkflowScope(scope);
      expect(App.db.get('Installer', 'installOptions').sshKey).to.be.undefined;
      expect(App.db.get('KerberosWizard', 'serviceConfigProperties')[0].value).to.be.undefined;
      expect(App.db.get('Installer', 'installOptions').requires_reentry).to.be.true;
    });

    it('retains non-secret absolute keytab path metadata', function () {
      var sanitized = App.db.sanitizeWorkflowData({
        name: 'dfs.namenode.keytab.file',
        value: '/etc/security/keytabs/nn.service.keytab'
      });

      expect(sanitized.value).to.equal('/etc/security/keytabs/nn.service.keytab');
      expect(sanitized.requires_reentry).to.be.undefined;
    });

    it('retains a direct keytab path while removing a non-path keytab secret', function () {
      var sanitized = App.db.sanitizeWorkflowData({
        keytab: '/etc/security/keytabs/nn.service.keytab',
        keytabContent: 'private material'
      });

      expect(sanitized.keytab).to.equal('/etc/security/keytabs/nn.service.keytab');
      expect(sanitized.keytabContent).to.be.undefined;
      expect(sanitized.requires_reentry).to.be.true;
    });

    it('projects only the active controller namespaces into a server checkpoint', function () {
      var scope = JSON.stringify(['alice', 'clusters', '1', 'tab-a']);
      App.db.activateWorkflowScope(scope);
      App.db.set('AddHost', 'installOptions', {requires_reentry: true});
      App.db.set('AddService', 'currentStep', 3);

      var snapshot = App.db.getWorkflowSnapshot(['AddService']);

      expect(snapshot.AddService.currentStep).to.equal(3);
      expect(snapshot.AddHost).to.be.undefined;
      expect(App.db.hasUnresolvedWorkflowReentry(['AddService'])).to.be.false;
      expect(App.db.hasUnresolvedWorkflowReentry(['AddHost'])).to.be.true;
    });
  });

  describe('#App.db.set', function () {

    afterEach(function () {
      App.db.cleanUp();
    });

    it('should create one object', function () {
      App.db.set('a', 'b', 1);
      expect(App.db.data.a.b).to.equal(1);
    });

    it('should create nested objects', function () {
      App.db.set('b.c', 'd', 1);
      expect(App.db.data.b.c.d).to.equal(1);
    });

  });

  describe('#App.db.get', function () {

    after(function () {
      App.db.cleanUp();
    });

    it('should return undefined', function () {
      var ret = App.db.get('a', 'b');
      expect(ret).to.be.undefined;
    });

    it('should return set value', function () {
      App.db.set('a', 'b', 10);
      var ret = App.db.get('a', 'b');
      expect(ret).to.equal(10);
    });

  });

  describe('#App.db.setProperties', function () {

    afterEach(function () {
      App.db.cleanUp();
    });

    it('should create one object', function () {
      App.db.setProperties('a', {b: 1, c: 2});
      expect(App.db.data.a).to.eql({b: 1, c: 2});
    });

    it('should create nested objects', function () {
      App.db.setProperties('b.c', {b: 1, c: 2});
      expect(App.db.data.b.c).to.eql({b: 1, c: 2});
    });

  });

  describe('#App.db.getProperties', function () {

    after(function () {
      App.db.cleanUp();
    });

    it('should return undefined', function () {
      var ret = App.db.getProperties('a', ['b', 'c']);
      expect(ret).to.eql({b: undefined, c: undefined});
    });

    it('should return set value', function () {
      App.db.setProperties('a', {b: 1, c: 2});
      var ret = App.db.getProperties('a', ['b', 'c']);
      expect(ret).to.eql({b: 1, c: 2});
    });

  });

});
