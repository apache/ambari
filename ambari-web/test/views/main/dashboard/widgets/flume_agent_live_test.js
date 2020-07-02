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

require('views/main/dashboard/widgets/flume_agent_live');

describe('App.FlumeAgentUpView', function () {

  var view;

  beforeEach(function () {
    view = App.FlumeAgentUpView.create({
      model: Em.Object.create()
    });
  });

  describe('#hiddenInfo()', function () {

    it('should return 0 nodes status', function () {
      expect(view.get('hiddenInfo')).to.eql(
        [
          '0' + ' ' + Em.I18n.t('dashboard.services.hdfs.nodes.live'),
          '0' + ' ' + Em.I18n.t('dashboard.services.hdfs.nodes.dead')
        ]
      );
    });

    it('should return nodes status', function () {
      view.set('flumeAgentsLive', [1,2,3]);
      view.set('flumeAgentsDead', [1,2,3,4]);
      expect(view.get('hiddenInfo')).to.eql(
        [
          '3' + ' ' + Em.I18n.t('dashboard.services.hdfs.nodes.live'),
          '4' + ' ' + Em.I18n.t('dashboard.services.hdfs.nodes.dead')
        ]
      );
    });
  });

  describe('#data()', function () {

    it('should return positive number', function () {
      view.set('flumeAgentsLive', [1,2,3]);
      view.set('model.hostComponents', Em.A([
        {
          componentName: 'FLUME_HANDLER',
          workStatus: 'STARTED'
        },
        {
          componentName: 'FLUME_HANDLER',
          workStatus: 'INSTALLED'
        },
        {
          componentName: 'HANDLER',
          workStatus: 'INSTALLED'
        }
      ]));
      expect(view.get('data')).to.equal(150);
    });

    it('should return -1', function () {
      expect(view.get('data')).to.equal(-1);
    });
  });

  describe('#statusObserver()', function () {

    beforeEach(function () {
      sinon.stub(Em.run, 'once', Em.K);
    });

    afterEach(function () {
      Em.run.once.restore();
    });

    it('should run status filter', function () {
      view.statusObserver();
      expect(Em.run.once.calledWith(view, 'filterStatusOnce')).to.be.true;
    });
  });

  describe('#filterStatusOnce()', function () {

    it('should set nodes statuses', function () {
      view.set('model.hostComponents', Em.A([
        {
          componentName: 'FLUME_HANDLER',
          workStatus: 'STARTED'
        },
        {
          componentName: 'FLUME_HANDLER',
          workStatus: 'INSTALLED'
        },
        {
          componentName: 'HANDLER',
          workStatus: 'INSTALLED'
        }
      ]));
      view.filterStatusOnce();
      expect(view.get('flumeAgentsLive')).to.eql([
        {
          componentName: 'FLUME_HANDLER',
          workStatus: 'STARTED'
        }
      ]);
      expect(view.get('flumeAgentsDead')).to.eql([
        {
          componentName: 'FLUME_HANDLER',
          workStatus: 'INSTALLED'
        }
      ]);
    });
  });

  describe('#hintInfo()', function () {

    it('should return formatted value', function () {
      view.set('maxValue', 150);
      expect(view.get('hintInfo')).to.equal(Em.I18n.t('dashboard.widgets.hintInfo.hint1').format('150'));
    });
  });

});
