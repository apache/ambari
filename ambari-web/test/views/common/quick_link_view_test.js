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
require('views/common/quick_view_link_view');

describe('App.QuickViewLinks', function () {

  var quickViewLinks = App.QuickViewLinks.create({
    content: Em.Object.create()
  });

  describe("#linkTarget", function () {
    it("blank link", function () {
      quickViewLinks.set('content.serviceName', 'HDFS');
      quickViewLinks.propertyDidChange('linkTarget');
      expect(quickViewLinks.get('linkTarget')).to.equal('_blank');
    });
    it("non-blank link", function () {
      quickViewLinks.set('content.serviceName', 'S1');
      quickViewLinks.propertyDidChange('linkTarget');
      expect(quickViewLinks.get('linkTarget')).to.be.empty;
    });
  });

  describe("#ambariProperties", function () {
    beforeEach(function () {
      sinon.stub(App.router, 'get').returns({p: 1});
    });
    afterEach(function () {
      App.router.get.restore();
    });
    it("", function () {
      expect(quickViewLinks.get('ambariProperties')).to.eql({p: 1});
    });
  });

  describe("#didInsertElement()", function () {
    beforeEach(function () {
      sinon.stub(App.router, 'get').returns({p: 1});
      sinon.stub(quickViewLinks, 'setQuickLinks');
    });
    afterEach(function () {
      App.router.get.restore();
      quickViewLinks.setQuickLinks.restore();
    });
    it("", function () {
      quickViewLinks.didInsertElement();
      expect(quickViewLinks.setQuickLinks.calledOnce).to.be.true;
    });
  });

  describe("#willDestroyElement()", function () {
    it("", function () {
      quickViewLinks.setProperties({
        configProperties: [{}],
        actualTags: [""],
        quickLinks: [{}]
      });
      quickViewLinks.willDestroyElement();
      expect(quickViewLinks.get('configProperties')).to.be.empty;
      expect(quickViewLinks.get('actualTags')).to.be.empty;
      expect(quickViewLinks.get('quickLinks')).to.be.empty;
    });
  });

  describe("#setQuickLinks()", function () {
    beforeEach(function () {
      this.mock = sinon.stub(App, 'get');
      sinon.stub(quickViewLinks, 'loadTags', Em.K);
    });
    afterEach(function () {
      this.mock.restore();
      quickViewLinks.loadTags.restore();
    });
    it("data loaded", function () {
      this.mock.returns(true);
      quickViewLinks.setQuickLinks();
      expect(quickViewLinks.loadTags.calledOnce).to.be.true;
    });
    it("data not loaded", function () {
      this.mock.returns(false);
      quickViewLinks.setQuickLinks();
      expect(quickViewLinks.loadTags.called).to.be.false;
    });
  });

  describe("#loadTags()", function () {
    beforeEach(function () {
      sinon.stub(App.ajax, 'send');
    });
    afterEach(function () {
      App.ajax.send.restore();
    });
    it("call $.ajax", function () {
      quickViewLinks.loadTags();
      expect(App.ajax.send.calledWith({
        name: 'config.tags',
        sender: quickViewLinks,
        success: 'loadTagsSuccess',
        error: 'loadTagsError'
      })).to.be.true;
    });
  });

  describe("#loadTagsSuccess()", function () {
    beforeEach(function () {
      sinon.stub(quickViewLinks, 'setConfigProperties', function () {
        return {
          done: function (callback) {
            callback();
          }
        }
      });
      sinon.stub(quickViewLinks, 'getQuickLinksHosts');
    });
    afterEach(function () {
      quickViewLinks.setConfigProperties.restore();
      quickViewLinks.getQuickLinksHosts.restore();
    });
    it("", function () {
      var data = {
        Clusters: {
          desired_configs: {
            site1: {
              tag: 'tag1'
            }
          }
        }
      };
      quickViewLinks.loadTagsSuccess(data);
      expect(quickViewLinks.get('actualTags')[0]).to.eql(Em.Object.create({
        siteName: 'site1',
        tagName: 'tag1'
      }));
      expect(quickViewLinks.setConfigProperties.calledOnce).to.be.true;
      expect(quickViewLinks.getQuickLinksHosts.calledOnce).to.be.true;
    });
  });

  describe("#loadTagsError()", function () {
    beforeEach(function () {
      sinon.stub(quickViewLinks, 'getQuickLinksHosts');
    });
    afterEach(function () {
      quickViewLinks.getQuickLinksHosts.restore();
    });
    it("call getQuickLinksHosts", function () {
      quickViewLinks.loadTagsError();
      expect(quickViewLinks.getQuickLinksHosts.calledOnce).to.be.true;
    });
  });

  describe("#getQuickLinksHosts()", function () {
    beforeEach(function () {
      sinon.stub(App.ajax, 'send');
      sinon.stub(App.HostComponent, 'find').returns([
        Em.Object.create({
          isMaster: true,
          hostName: 'host1'
        })
      ]);
    });
    afterEach(function () {
      App.ajax.send.restore();
      App.HostComponent.find.restore();
    });
    it("call $.ajax", function () {
      quickViewLinks.getQuickLinksHosts();
      expect(App.ajax.send.calledWith({
        name: 'hosts.for_quick_links',
        sender: quickViewLinks,
        data: {
          clusterName: App.get('clusterName'),
          masterHosts: 'host1',
          urlParams: ''
        },
        success: 'setQuickLinksSuccessCallback'
      })).to.be.true;
    });
    it("call $.ajax, HBASE service", function () {
      quickViewLinks.set('content.serviceName', 'HBASE');
      quickViewLinks.getQuickLinksHosts();
      expect(App.ajax.send.calledWith({
        name: 'hosts.for_quick_links',
        sender: quickViewLinks,
        data: {
          clusterName: App.get('clusterName'),
          masterHosts: 'host1',
          urlParams: ',host_components/metrics/hbase/master/IsActiveMaster'
        },
        success: 'setQuickLinksSuccessCallback'
      })).to.be.true;
    });
  });

  describe("#setQuickLinksSuccessCallback()", function () {
    beforeEach(function () {
      this.mock = sinon.stub(quickViewLinks, 'getHosts');
      sinon.stub(quickViewLinks, 'setEmptyLinks');
      sinon.stub(quickViewLinks, 'setSingleHostLinks');
      sinon.stub(quickViewLinks, 'setMultipleHostLinks');
      quickViewLinks.set('content.quickLinks', []);
    });
    afterEach(function () {
      this.mock.restore();
      quickViewLinks.setEmptyLinks.restore();
      quickViewLinks.setSingleHostLinks.restore();
      quickViewLinks.setMultipleHostLinks.restore();
    });
    it("no hosts", function () {
      this.mock.returns([]);
      quickViewLinks.setQuickLinksSuccessCallback();
      expect(quickViewLinks.setEmptyLinks.calledOnce).to.be.true;
    });
    it("quickLinks is null", function () {
      this.mock.returns([{}]);
      quickViewLinks.set('content.quickLinks', null);
      quickViewLinks.setQuickLinksSuccessCallback();
      expect(quickViewLinks.setEmptyLinks.calledOnce).to.be.true;
    });
    it("single host", function () {
      this.mock.returns([{hostName: 'host1'}]);
      quickViewLinks.setQuickLinksSuccessCallback();
      expect(quickViewLinks.setSingleHostLinks.calledWith([{hostName: 'host1'}])).to.be.true;
    });
    it("multiple hosts", function () {
      this.mock.returns([{hostName: 'host1'}, {hostName: 'host2'}]);
      quickViewLinks.setQuickLinksSuccessCallback();
      expect(quickViewLinks.setMultipleHostLinks.calledWith(
        [{hostName: 'host1'}, {hostName: 'host2'}]
      )).to.be.true;
    });
  });

  describe("#getPublicHostName()", function () {
    it("host present", function () {
      var hosts = [{
        Hosts: {
          host_name: 'host1',
          public_host_name: 'public_name'
        }
      }];
      expect(quickViewLinks.getPublicHostName(hosts, 'host1')).to.equal('public_name');
    });
    it("host absent", function () {
      expect(quickViewLinks.getPublicHostName([], 'host1')).to.be.null;
    });
  });

  describe("#setConfigProperties()", function () {
    var mock = {getConfigsByTags: Em.K};
    beforeEach(function () {
      sinon.stub(App.router, 'get').returns(mock);
      sinon.spy(mock, 'getConfigsByTags');
    });
    afterEach(function () {
      mock.getConfigsByTags.restore();
      App.router.get.restore();
    });
    it("", function () {
      quickViewLinks.set('actualTags', [{siteName: 'hdfs-site'}]);
      quickViewLinks.setConfigProperties();
      expect(mock.getConfigsByTags.calledWith([{siteName: 'hdfs-site'}])).to.be.true;
    });
  });

  describe("#setEmptyLinks()", function () {
    it("", function () {
      quickViewLinks.setEmptyLinks();
      expect(quickViewLinks.get('quickLinks')).to.eql([{
        label: quickViewLinks.t('quick.links.error.label'),
        url: 'javascript:alert("' + quickViewLinks.t('contact.administrator') + '");return false;'
      }]);
      expect(quickViewLinks.get('isLoaded')).to.be.true;
    });
  });

  describe("#processOozieHosts()", function () {
    it("", function () {
      quickViewLinks.set('content.hostComponents', [Em.Object.create({
        componentName: 'OOZIE_SERVER',
        workStatus: 'STARTED',
        hostName: 'host1'
      })]);
      var host = {hostName: 'host1'};
      quickViewLinks.processOozieHosts([host]);
      expect(host.status).to.equal(Em.I18n.t('quick.links.label.active'));
    });
  });

  describe("#processHdfsHosts()", function () {
    beforeEach(function () {
      quickViewLinks.set('content.activeNameNode', null);
      quickViewLinks.set('content.standbyNameNode', null);
      quickViewLinks.set('content.standbyNameNode2', null);
    });
    it("active namenode host", function () {
      quickViewLinks.set('content.activeNameNode', Em.Object.create({hostName: 'host1'}));
      var host = {hostName: 'host1'};
      quickViewLinks.processHdfsHosts([host]);
      expect(host.status).to.equal(Em.I18n.t('quick.links.label.active'));
    });
    it("standby namenode host", function () {
      quickViewLinks.set('content.standbyNameNode', Em.Object.create({hostName: 'host1'}));
      var host = {hostName: 'host1'};
      quickViewLinks.processHdfsHosts([host]);
      expect(host.status).to.equal(Em.I18n.t('quick.links.label.standby'));
    });
    it("second standby namenode host", function () {
      quickViewLinks.set('content.standbyNameNode2', Em.Object.create({hostName: 'host1'}));
      var host = {hostName: 'host1'};
      quickViewLinks.processHdfsHosts([host]);
      expect(host.status).to.equal(Em.I18n.t('quick.links.label.standby'));
    });
  });

  describe("#processHbaseHosts()", function () {
    it("isActiveMaster is true", function () {
      var response = {
        items: [
          {
            Hosts: {
              host_name: 'host1'
            },
            host_components: [
              {
                HostRoles: {
                  component_name: 'HBASE_MASTER'
                },
                metrics: {
                  hbase: {
                    master: {
                      IsActiveMaster: 'true'
                    }
                  }
                }
              }
            ]
          }
        ]
      };
      var host = {hostName: 'host1'};
      quickViewLinks.processHbaseHosts([host], response);
      expect(host.status).to.equal(Em.I18n.t('quick.links.label.active'));
    });
    it("isActiveMaster is false", function () {
      var response = {
        items: [
          {
            Hosts: {
              host_name: 'host1'
            },
            host_components: [
              {
                HostRoles: {
                  component_name: 'HBASE_MASTER'
                },
                metrics: {
                  hbase: {
                    master: {
                      IsActiveMaster: 'false'
                    }
                  }
                }
              }
            ]
          }
        ]
      };
      var host = {hostName: 'host1'};
      quickViewLinks.processHbaseHosts([host], response);
      expect(host.status).to.equal(Em.I18n.t('quick.links.label.standby'));
    });
    it("isActiveMaster is undefined", function () {
      var response = {
        items: [
          {
            Hosts: {
              host_name: 'host1'
            },
            host_components: [
              {
                HostRoles: {
                  component_name: 'HBASE_MASTER'
                }
              }
            ]
          }
        ]
      };
      var host = {hostName: 'host1'};
      quickViewLinks.processHbaseHosts([host], response);
      expect(host.status).to.be.undefined;
    });
  });

  describe("#processYarnHosts()", function () {
    it("haStatus is ACTIVE", function () {
      quickViewLinks.set('content.hostComponents', [Em.Object.create({
        componentName: 'RESOURCEMANAGER',
        hostName: 'host1',
        haStatus: 'ACTIVE'
      })]);
      var host = {hostName: 'host1'};
      quickViewLinks.processYarnHosts([host]);
      expect(host.status).to.equal(Em.I18n.t('quick.links.label.active'));
    });
    it("haStatus is STANDBY", function () {
      quickViewLinks.set('content.hostComponents', [Em.Object.create({
        componentName: 'RESOURCEMANAGER',
        hostName: 'host1',
        haStatus: 'STANDBY'
      })]);
      var host = {hostName: 'host1'};
      quickViewLinks.processYarnHosts([host]);
      expect(host.status).to.equal(Em.I18n.t('quick.links.label.standby'));
    });
    it("haStatus is undefined", function () {
      quickViewLinks.set('content.hostComponents', [Em.Object.create({
        componentName: 'RESOURCEMANAGER',
        hostName: 'host1'
      })]);
      var host = {hostName: 'host1'};
      quickViewLinks.processYarnHosts([host]);
      expect(host.status).to.be.undefined;
    });
  });

  describe("#findHosts()", function () {
    beforeEach(function () {
      sinon.stub(quickViewLinks, 'getPublicHostName').returns('public_name');
    });
    afterEach(function () {
      quickViewLinks.getPublicHostName.restore();
    });
    it("", function () {
      quickViewLinks.set('content.hostComponents', [Em.Object.create({
        componentName: 'C1',
        hostName: 'host1'
      })]);
      expect(quickViewLinks.findHosts('C1', {})).to.eql([{
        hostName: 'host1',
        publicHostName: 'public_name'
      }]);
    });
  });

  describe('#setProtocol', function () {
    var tests = [
      {
        serviceName: "YARN", configProperties: [
        {type: 'yarn-site', properties: {'yarn.http.policy': 'HTTPS_ONLY'}}
      ], m: "https for yarn", result: "https"
      },
      {
        serviceName: "YARN", configProperties: [
        {type: 'yarn-site', properties: {'yarn.http.policy': 'HTTP_ONLY'}}
      ], m: "http for yarn", result: "http"
      },
      {
        serviceName: "YARN", configProperties: [
        {type: 'yarn-site', properties: {'yarn.http.policy': 'HTTP_ONLY'}}
      ], m: "http for yarn (overrides hadoop.ssl.enabled)", result: "http"
      },
      {
        serviceName: "YARN", configProperties: [
        {type: 'yarn-site', properties: {'yarn.http.policy': 'HTTPS_ONLY'}}
      ], m: "https for yarn (overrides hadoop.ssl.enabled)", result: "https"
      },
      {
        serviceName: "MAPREDUCE2", configProperties: [
        {type: 'mapred-site', properties: {'mapreduce.jobhistory.http.policy': 'HTTPS_ONLY'}}
      ], m: "https for mapreduce2", result: "https"
      },
      {
        serviceName: "MAPREDUCE2", configProperties: [
        {type: 'mapred-site', properties: {'mapreduce.jobhistory.http.policy': 'HTTP_ONLY'}}
      ], m: "http for mapreduce2", result: "http"
      },
      {
        serviceName: "ANYSERVICE", configProperties: [
        {type: 'hdfs-site', properties: {'dfs.http.policy': 'HTTPS_ONLY'}}
      ], m: "https for anyservice", servicesSupportsHttps: ["ANYSERVICE"], result: "https"
      },
      {
        serviceName: "RANGER", configProperties: [
        {type: 'ranger-site', properties: {'http.enabled': 'true'}}
      ], m: "http for ranger (HDP2.2)", result: "http"
      },
      {
        serviceName: "RANGER", configProperties: [
        {type: 'ranger-site', properties: {'http.enabled': 'false'}}
      ], m: "https for ranger (HDP2.2)", result: "https"
      },
      {
        serviceName: "RANGER", configProperties: [
        {
          type: 'ranger-admin-site',
          properties: {'ranger.service.http.enabled': 'true', 'ranger.service.https.attrib.ssl.enabled': 'false'}
        }
      ], m: "http for ranger (HDP2.3)", result: "http"
      },
      {
        serviceName: "RANGER", configProperties: [
        {
          type: 'ranger-admin-site',
          properties: {'ranger.service.http.enabled': 'false', 'ranger.service.https.attrib.ssl.enabled': 'true'}
        }
      ], m: "https for ranger (HDP2.3)", result: "https"
      }
    ];

    tests.forEach(function (t) {
      it(t.m, function () {
        quickViewLinks.set('servicesSupportsHttps', t.servicesSupportsHttps);
        expect(quickViewLinks.setProtocol(t.serviceName, t.configProperties, t.ambariProperties)).to.equal(t.result);
      });
    });
  });

  describe('#setPort', function () {
    var testData = [
      Em.Object.create({
        'service_id': 'YARN',
        'protocol': 'http',
        'result': '8088',
        'default_http_port': '8088',
        'default_https_port': '8090',
        'regex': '\\w*:(\\d+)'
      }),
      Em.Object.create({
        'service_id': 'YARN',
        'protocol': 'https',
        'https_config': 'https_config',
        'result': '8090',
        'default_http_port': '8088',
        'default_https_port': '8090',
        'regex': '\\w*:(\\d+)'
      }),
      Em.Object.create({
        'service_id': 'YARN',
        'protocol': 'https',
        'https_config': 'https_config',
        'result': '8090',
        'default_http_port': '8088',
        'default_https_port': '8090',
        'regex': '\\w*:(\\d+)'
      }),
      Em.Object.create({
        'service_id': 'YARN',
        'protocol': 'https',
        'https_config': 'https_config',
        'config': 'https_config_custom',
        'site': 'yarn-site',
        'result': '9091',
        'default_http_port': '8088',
        'default_https_port': '8090',
        'regex': '\\w*:(\\d+)',
        'configProperties': [{
          'type': 'yarn-site',
          'properties': {
            'https_config': 'h:9090',
            'https_config_custom': 'h:9091'
          }
        }]
      }),
      Em.Object.create({
        'service_id': 'YARN',
        'protocol': 'https',
        'http_config': 'http_config',
        'https_config': 'https_config',
        'site': 'yarn-site',
        'result': '9090',
        'default_http_port': '8088',
        'default_https_port': '8090',
        'regex': '\\w*:(\\d+)',
        'configProperties': [{
          'type': 'yarn-site',
          'properties': {
            'http_config': 'h:9088',
            'https_config': 'h:9090'
          }
        }]
      }),
      Em.Object.create({
        'service_id': 'RANGER',
        'protocol': 'http',
        'http_config': 'http_config',
        'https_config': 'https_config',
        'result': '6080',
        'default_http_port': '6080',
        'default_https_port': '6182',
        'regex': '(\\d*)+'
      }),
      Em.Object.create({
        'service_id': 'RANGER',
        'protocol': 'https',
        'http_config': 'http_config',
        'https_config': 'https_config',
        'result': '6182',
        'default_http_port': '6080',
        'default_https_port': '6182',
        'regex': '(\\d*)+'
      })
    ];

    after(function () {
      quickViewLinks.set('configProperties', []);
    });

    testData.forEach(function (item) {
      it(item.service_id + ' ' + item.protocol, function () {
        quickViewLinks.set('configProperties', item.configProperties || []);
        expect(quickViewLinks.setPort(item, item.protocol, item.config)).to.equal(item.result);
      })
    }, this);
  });

});
