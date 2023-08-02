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

require('views/main/dashboard/widgets/supervisor_live');

describe('App.SuperVisorUpView', function () {

  var view;

  beforeEach(function () {
    view = App.SuperVisorUpView.create({
      model: Em.Object.create()
    });
  });

  describe('#hiddenInfo()', function () {

    it('should return nodes status', function () {
      view.set('model.superVisorsStarted', 3);
      view.set('model.superVisorsInstalled', 4);
      expect(view.get('hiddenInfo')).to.eql(
        [
          '3' + ' ' + Em.I18n.t('dashboard.services.hdfs.nodes.live'),
          '4' + ' ' + Em.I18n.t('dashboard.services.hdfs.nodes.dead')
        ]
      );
    });
  });

  describe('#data()', function () {

    it('should return -1', function () {
      expect(view.get('data')).to.equal(-1);
    });

    it('should return data', function () {
      view.set('model.superVisorsStarted', 3);
      view.set('model.superVisorsTotal', 6);
      expect(view.get('data')).to.equal(50);
    });
  });

  describe('#content()', function () {

    it('should return not available', function () {
      expect(view.get('content')).to.equal(Em.I18n.t('services.service.summary.notAvailable'));
    });

    it('should return string content', function () {
      view.set('model.superVisorsStarted', 3);
      view.set('model.superVisorsTotal', 6);
      expect(view.get('content')).to.equal('3/6');
    });
  });

  describe('#hintInfo()', function () {

    it('should return formatted value', function () {
      view.set('maxValue', 150);
      expect(view.get('hintInfo')).to.equal(Em.I18n.t('dashboard.widgets.hintInfo.hint1').format('150'));
    });
  });

});
