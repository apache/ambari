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

var modelSetup = require('test/init_model_test');
require('models/config_group');
require('models/host');

var configGroup,
  hostRecord,
  hosts = [
    Em.Object.create({
      id: 'host0',
      hostName: 'host0',
      hostComponents: []
    }),
    Em.Object.create({
      id: 'host1',
      hostName: 'host1',
      hostComponents: []
    })
  ],
  host = Em.Object.create({
    id: 'host0',
    hostName: 'host0',
    hostComponents: []
  }),
  properties = [
    {
      name: 'n0',
      value: 'v0'
    },
    {
      name: 'n1',
      value: 'v1'
    }
  ],
  setParentConfigGroup = function (configGroup, hosts) {
    configGroup.set('parentConfigGroup', App.ConfigGroup.create());
    configGroup.set('parentConfigGroup.hosts', hosts.mapProperty('hostName'));
  };

describe('App.ConfigGroup', function () {

  beforeEach(function () {
    configGroup = App.ConfigGroup.create();
  });

  describe('#displayName', function () {
    it('should equal name if maximum length is not exceeded', function () {
      configGroup.set('name', 'n');
      expect(configGroup.get('displayName')).to.equal(configGroup.get('name'));
    });
    it('should be shortened if maximum length is exceeded', function () {
      var maxLength = App.config.CONFIG_GROUP_NAME_MAX_LENGTH;
      for (var i = maxLength + 1, name = ''; i--; ) {
        name += 'n';
      }
      configGroup.set('name', name);
      expect(configGroup.get('displayName')).to.contain('...');
      expect(configGroup.get('displayName')).to.have.length(2 * Math.floor(maxLength / 2) + 3);
    });
  });

  describe('#displayNameHosts', function () {
    it('should indicate the number of hosts', function () {
      var displayName = configGroup.get('displayName');
      configGroup.set('hosts', []);
      expect(configGroup.get('displayNameHosts')).to.equal(displayName + ' (0)');
      configGroup.set('hosts', hosts);
      expect(configGroup.get('displayNameHosts')).to.equal(displayName + ' (2)');
    });
  });

  describe('#availableHosts', function () {

    beforeEach(function () {
      App.clusterStatus.set('clusterState', 'DEFAULT');
      sinon.stub(App.Host, 'find', function() {
        return [host];
      });
      setParentConfigGroup(configGroup, hosts);
    });

    afterEach(function () {
      App.Host.find.restore();
    });

    it('should return an empty array as default', function () {
      configGroup.set('isDefault', true);
      expect(configGroup.get('availableHosts')).to.eql([]);
    });

    it('should return an empty array if there are no unused hosts', function () {
      configGroup.set('parentConfigGroup', App.ConfigGroup.create());
      expect(configGroup.get('availableHosts')).to.eql([]);
    });

    it('should take hosts from parentConfigGroup', function () {
      setParentConfigGroup(configGroup, hosts);
      configGroup.set('clusterHosts', hosts);
      expect(configGroup.get('availableHosts')).to.have.length(2);
    });
  });

  describe('#isAddHostsDisabled', function () {

    beforeEach(function () {
      hostRecord = App.Host.createRecord(host);
      setParentConfigGroup(configGroup, hosts);
      configGroup.set('isDefault', false);
      configGroup.reopen({availableHosts: [{}]});
    });

    afterEach(function () {
      modelSetup.deleteRecord(hostRecord);
    });

    it('should be false', function () {
      expect(configGroup.get('isAddHostsDisabled')).to.be.false;
    });
    it('should be true', function () {
      App.clusterStatus.set('clusterState', 'DEFAULT');
      configGroup.set('isDefault', true);
      expect(configGroup.get('isAddHostsDisabled')).to.be.true;
      configGroup.set('availableHosts', hosts);
      expect(configGroup.get('isAddHostsDisabled')).to.be.true;
    });
  });

  describe('#propertiesList', function () {
    it('should be formed from properties', function () {
      configGroup.set('properties', properties);
      properties.forEach(function (item) {
        Em.keys(item).forEach(function (prop) {
          expect(configGroup.get('propertiesList')).to.contain(item[prop]);
        });
      });
      expect(configGroup.get('propertiesList')).to.have.length(24);
    });
  });

});
