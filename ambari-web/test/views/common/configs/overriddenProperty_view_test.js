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
require('views/common/configs/overriddenProperty_view');

describe('App.ServiceConfigView.SCPOverriddenRowsView', function () {

  var view = App.ServiceConfigView.SCPOverriddenRowsView.create();

  describe('#didInsertElement', function () {

    beforeEach(function () {
      sinon.spy(view, 'setSwitchText');
      sinon.stub(App, 'tooltip', Em.K);
    });

    afterEach(function () {
      view.setSwitchText.restore();
      App.tooltip.restore();
    });

    it('setSwitchLinks method should be executed', function () {
      view.didInsertElement();
      expect(view.setSwitchText.calledOnce).to.be.true;
    });

  });

  describe('#setSwitchText', function () {

    var view = App.ServiceConfigView.SCPOverriddenRowsView.create({
      serviceConfigProperty: {
        overrides: [
          Em.Object.create({
            group: Em.Object.create({
              displayName: 'hcg',
              switchGroupTextShort: 'short',
              switchGroupTextFull: 'full'
            })
          })
        ]
      }
    });

    beforeEach(function () {
      sinon.stub(App, 'tooltip', Em.K);
    });

    afterEach(function () {
      App.tooltip.restore();
    });

    it('should not modify overrides', function () {
      view.set('isDefaultGroupSelected', false);
      expect(view.get('serviceConfigProperty.overrides.firstObject.group.switchGroupTextShort')).to.equal('short');
      expect(view.get('serviceConfigProperty.overrides.firstObject.group.switchGroupTextFull')).to.equal('full');
    });

    it('should set switchGroupTextShort and switchGroupTextFull', function () {
      view.set('isDefaultGroupSelected', true);
      expect(view.get('serviceConfigProperty.overrides.firstObject.group.switchGroupTextShort')).to.equal(Em.I18n.t('services.service.config_groups.switchGroupTextShort').format('hcg'));
      expect(view.get('serviceConfigProperty.overrides.firstObject.group.switchGroupTextFull')).to.equal(Em.I18n.t('services.service.config_groups.switchGroupTextFull').format('hcg'));
    });

  });

});
