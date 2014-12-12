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

require('controllers/main/service/reassign/step1_controller');
require('models/host_component');

describe('App.ReassignMasterWizardStep1Controller', function () {


  var controller = App.ReassignMasterWizardStep1Controller.create({
    content: Em.Object.create({
      reassign: Em.Object.create({}),
      services: []
    })
  });
  controller.set('_super', Em.K);

  describe('#loadConfigTags', function() {
    beforeEach(function() {
      sinon.stub(App.ajax, 'send', Em.K);
    });

    afterEach(function() {
      App.ajax.send.restore();
    });

    it('tests loadConfigTags', function() {
      controller.loadConfigsTags();

      expect(App.ajax.send.calledOnce).to.be.true;
    });

    it('tests saveDatabaseType with type', function() {
      sinon.stub(App.router, 'get', function() {
        return { saveDatabaseType: Em.K};
      });

      controller.saveDatabaseType(true);
      expect(App.router.get.calledOnce).to.be.true;

      App.router.get.restore();
    });

    it('tests saveDatabaseType without type', function() {
      sinon.stub(App.router, 'get', function() {
        return { saveDatabaseType: Em.K};
      });

      controller.saveDatabaseType(false);
      expect(App.router.get.called).to.be.false;

      App.router.get.restore();
    });

    it('tests saveServiceProperties with propertie', function() {
      sinon.stub(App.router, 'get', function() {
        return { saveServiceProperties: Em.K};
      });

      controller.saveServiceProperties(true);
      expect(App.router.get.calledOnce).to.be.true;

      App.router.get.restore();
    });

    it('tests saveServiceProperties without properties', function() {
      sinon.stub(App.router, 'get', function() {
        return { saveServiceProperties: Em.K};
      });

      controller.saveServiceProperties(false);
      expect(App.router.get.called).to.be.false;

      App.router.get.restore();
    });

    it('tests getDatabaseHost', function() {
      controller.set('content.serviceProperties', {
        'javax.jdo.option.ConnectionURL': "jdbc:mysql://c6401/hive?createDatabaseIfNotExist=true"
      });

      controller.set('content.reassign.service_id', 'HIVE');
      controller.set('databaseType', 'mysql');

      expect(controller.getDatabaseHost()).to.equal('c6401')
    });

  });
});
