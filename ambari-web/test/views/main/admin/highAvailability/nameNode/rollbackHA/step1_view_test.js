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
require('views/main/admin/highAvailability/nameNode/rollbackHA/step1_view');
var testHelpers = require('test/helpers');

describe('App.RollbackHighAvailabilityWizardStep1View', function () {
  var view = App.RollbackHighAvailabilityWizardStep1View.create({
    controller: Em.Object.create({
      content: Em.Object.create({}),
    })
  });

  describe("#didInsertElement()", function () {
    before(function () {
      sinon.spy(view, 'loadHostsName');
    });
    after(function () {
      view.loadHostsName.restore();
    });
    it("call loadHostsName", function () {
      view.didInsertElement();
      expect(view.loadHostsName.calledOnce).to.be.true;
    });
  });

  describe("#done()", function() {
    before(function () {
      sinon.stub(App.router, 'send');
    });
    after(function () {
      App.router.send.restore();
    });
    it("call App.router.send", function() {
      view.set('selectedSNNHost', 'host1');
      view.set('selectedAddNNHost', 'host2');
      view.done();
      expect(view.get('controller.content.selectedSNNHost')).to.be.equal('host1');
      expect(view.get('controller.content.selectedAddNNHost')).to.be.equal('host2');
      expect(App.router.send.called).to.be.true;
    });
  });

  describe("#loadHostsName()", function () {
    it("call loadHostsName", function () {
      view.loadHostsName();
      var args = testHelpers.findAjaxRequest('name', 'hosts.all');
      expect(args).to.exist;
    });
  });

  describe("#loadHostsNameSuccessCallback()", function() {
    var data = {
      items: [
        {
          Hosts: {
            host_name: 'host1'
          }
        }
      ]
    };

    beforeEach(function () {
      this.stub = sinon.stub(App.HostComponent, 'find');
    });

    afterEach(function () {
      this.stub.restore();
    });

    it("data should be parsed properly", function() {
      this.stub.returns([
        Em.Object.create({
          componentName: 'NAMENODE',
          displayNameAdvanced: 'Active NameNode',
          hostName: 'host1'
        }),
        Em.Object.create({
          componentName: 'NAMENODE',
          displayNameAdvanced: 'Standby NameNode',
          hostName: 'host2'
        })
      ]);
      view.set('controller.content.sNNHost', 'host1');
      view.set('controller.content.addNNHost', 'host2');
      view.loadHostsNameSuccessCallback(data);
      expect(view.get('selectedSNNHost')).to.be.equal('host1');
      expect(view.get('selectedAddNNHost')).to.be.equal('host2');
      expect(view.get('addNNHosts')).to.eql(['host1','host2']);
      expect(view.get('sNNHosts')).to.eql(['host1']);
      expect(view.get('isLoaded')).to.be.true;
    });
  });

  describe("#loadHostsNameErrorCallback()", function() {
    it("isLoaded should be true", function() {
      view.loadHostsNameErrorCallback();
      expect(view.get('isLoaded')).to.be.true;
    });
  });

});
