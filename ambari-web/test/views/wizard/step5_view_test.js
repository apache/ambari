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
var lazyloading = require('utils/lazy_loading');
require('views/wizard/step5_view');
var view;

describe('App.WizardStep5View', function() {
  beforeEach(function() {
    view = App.WizardStep5View.create({
      controller: App.WizardStep5Controller.create({})
    });
  });
  describe('#didInsertElement', function() {
    it('should call controller.loadStep', function() {
      sinon.stub(view.get('controller'), 'loadStep', Em.K);
      view.didInsertElement();
      expect(view.get('controller').loadStep.calledOnce).to.equal(true);
      view.get('controller').loadStep.restore();
    });
  });
});

describe('App.SelectHostView', function() {
  beforeEach(function() {
    view = App.SelectHostView.create({
      controller: App.WizardStep5Controller.create({})
    });
  });

  describe('#didInsertElement', function() {
    beforeEach(function() {
      sinon.stub(view, 'initContent', Em.K);
    });
    afterEach(function() {
      view.initContent.restore();
    });
    it('should call initContent', function() {
      view.didInsertElement();
      expect(view.initContent.calledOnce).to.equal(true);
    });
    it('should set selectedHost to value', function() {
      view.set('selectedHost', 'h1');
      view.set('value', '');
      view.didInsertElement();
      expect(view.get('value')).to.equal('h1');
    });
  });

  describe('#change', function() {
    beforeEach(function() {
      view.set('componentName', 'ZOOKEEPER_SERVER');
      view.set('value', 'h1');
      view.set('zId', 1);
      view.set('controller.rebalanceComponentHostsCounter', 0);
      view.set('controller.componentToRebalance', '');
      sinon.stub(view.get('controller'), 'assignHostToMaster', Em.K);
    });
    afterEach(function() {
      view.get('controller').assignHostToMaster.restore();
    });
    it('should call assignHostToMaster', function() {
      view.change();
      expect(view.get('controller').assignHostToMaster.calledWith('ZOOKEEPER_SERVER', 'h1', 1));
    });
    it('should increment rebalanceComponentHostsCounter', function() {
      view.change();
      expect(view.get('controller.rebalanceComponentHostsCounter')).to.equal(1);
    });
    it('should set componentToRebalance', function() {
      view.change();
      expect(view.get('controller.componentToRebalance')).to.equal('ZOOKEEPER_SERVER');
    });
  });

  describe('#getAvailableHosts', function() {
    var tests = Em.A([
      {
        hosts: Em.A([]),
        selectedHost: 'h2',
        componentName: 'ZOOKEEPER_SERVER',
        selectedServicesMasters: Em.A([
          Em.Object.create({component_name: 'ZOOKEEPER_SERVER', selectedHost: 'h1'})
        ]),
        m: 'Empty hosts',
        e: []
      },
      {
        hosts: Em.A([
          Em.Object.create({host_name: 'h1'}),
          Em.Object.create({host_name: 'h2'})
        ]),
        selectedHost: 'h2',
        componentName: 'c1',
        selectedServicesMasters: Em.A([
          Em.Object.create({component_name: 'c2', selectedHost: 'h1'})
        ]),
        m: 'Two hosts',
        e: ['h1', 'h2']
      },
      {
        hosts: Em.A([
          Em.Object.create({host_name: 'h1'}),
          Em.Object.create({host_name: 'h2'})
        ]),
        selectedHost: 'h2',
        componentName: 'ZOOKEEPER_SERVER',
        selectedServicesMasters: Em.A([
          Em.Object.create({component_name: 'ZOOKEEPER_SERVER', selectedHost: 'h1'})
        ]),
        m: 'Two hosts, ZOOKEEPER_SERVER',
        e: ['h2']
      },
      {
        hosts: Em.A([
          Em.Object.create({host_name: 'h1'}),
          Em.Object.create({host_name: 'h2'})
        ]),
        selectedHost: 'h2',
        componentName: 'HBASE_MASTER',
        selectedServicesMasters: Em.A([
          Em.Object.create({component_name: 'HBASE_MASTER', selectedHost: 'h1'})
        ]),
        m: 'Two hosts, HBASE_MASTER',
        e: ['h2']
      }
    ]);
    tests.forEach(function(test) {
      it(test.m, function() {
        view.set('controller.hosts', test.hosts);
        view.set('componentName', test.componentName);
        view.set('controller.selectedServicesMasters', test.selectedServicesMasters);
        var r = view.getAvailableHosts();
        expect(r.mapProperty('host_name')).to.eql(test.e);
      });
    });
  });

  describe('#rebalanceComponentHosts', function() {
    var tests = Em.A([
      {
        componentName: 'c1',
        componentToRebalance: 'c2',
        isLoaded: true,
        content: [{}],
        m: 'componentName not equal to componentToRebalance',
        e: {
          initContent: false,
          isLoaded: true,
          content: 1
        }
      },
      {
        componentName: 'c2',
        componentToRebalance: 'c2',
        isLoaded: true,
        content: [{}],
        m: 'componentName equal to componentToRebalance',
        e: {
          initContent: true,
          isLoaded: false,
          content: 0
        }
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        view.set('isLoaded', test.isLoaded);
        view.set('content', test.content);
        view.set('componentName', test.componentName);
        view.set('controller.componentToRebalance', test.componentToRebalance);
        sinon.stub(view, 'initContent', Em.K);
        view.rebalanceComponentHosts();
        expect(view.initContent.calledOnce).to.equal(test.e.initContent);
        expect(view.get('isLoaded')).to.equal(test.e.isLoaded);
        expect(view.get('content.length')).to.equal(test.e.content);
        view.initContent.restore();
      });
    });
  });

  describe('#initContent', function() {
    var tests = Em.A([
      {
        isLazyLoading: false,
        hosts: 25,
        m: 'not lazy loading, 25 hosts, no selected host',
        e: 25
      },
      {
        isLazyLoading: false,
        hosts: 25,
        h: 4,
        m: 'not lazy loading, 25 hosts, one selected host',
        e: 25
      },
      {
        isLazyLoading: true,
        hosts: 25,
        h: 4,
        m: 'lazy loading, 25 hosts, one selected host',
        e: 25
      },
      {
        isLazyLoading: true,
        hosts: 25,
        m: 'lazy loading, 25 hosts, no selected host',
        e: 26
      },
      {
        isLazyLoading: true,
        hosts: 100,
        h: 4,
        m: 'lazy loading, 100 hosts, one selected host',
        e: 30
      },
      {
        isLazyLoading: true,
        hosts: 100,
        m: 'lazy loading, 100 hosts, no selected host',
        e: 31
      }
    ]);
    tests.forEach(function(test) {
      it(test.m, function() {
        view.reopen({getAvailableHosts: function() {return d3.range(0, test.hosts).map(function(indx){return Em.Object.create({host_name: indx})});}});
        if (test.h) {
          view.set('selectedHost', test.h);
        }
        view.set('isLazyLoading', test.isLazyLoading);
        view.initContent();
        expect(view.get('content.length')).to.equal(test.e);
      });
    });
  });

});

describe('App.RemoveControlView', function() {
  beforeEach(function() {
    view = App.RemoveControlView.create({
      controller: App.WizardStep5Controller.create({})
    });
  });

  describe('#click', function() {
    beforeEach(function() {
      sinon.stub(view.get('controller'), 'removeComponent', Em.K);
    });
    afterEach(function() {
      view.get('controller').removeComponent.restore();
    });
    it('should call removeComponent', function() {
      view.set('zId', 1);
      view.set('componentName', 'c1');
      view.click();
      expect(view.get('controller').removeComponent.calledWith('c1', 1)).to.equal(true);
    });
  });
});

describe('App.AddControlView', function() {
  beforeEach(function() {
    view = App.AddControlView.create({
      controller: App.WizardStep5Controller.create({})
    });
  });

  describe('#click', function() {
    beforeEach(function() {
      sinon.stub(view.get('controller'), 'addComponent', Em.K);
    });
    afterEach(function() {
      view.get('controller').addComponent.restore();
    });
    it('should call addComponent', function() {
      view.set('componentName', 'c1');
      view.click();
      expect(view.get('controller').addComponent.calledWith('c1')).to.equal(true);
    });
  });
});