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
require('views/common/modal_popups/upgrade_configs_merge_popup');

describe('App.showUpgradeConfigsMergePopup', function () {

  var conflicts = [
      {
        type: 't0',
        property: 'p0',
        current: 'c0',
        new_stack_value: 'n0',
        result_value: 'n0'
      },
      {
        type: 't1',
        property: 'p1',
        current: 'c1',
        new_stack_value: null,
        result_value: 'c1'
      },
      {
        type: 't2',
        property: 'p2',
        current: 'c2',
        new_stack_value: null,
        result_value: null
      }
    ],
    version = 'HDP-2.3.0.0-1111',
    result = [
      {
        type: 't0',
        name: 'p0',
        currentValue: 'c0',
        recommendedValue: 'n0',
        resultingValue: 'n0',
        isDeprecated: false,
        willBeRemoved: false
      },
      {
        type: 't1',
        name: 'p1',
        currentValue: 'c1',
        recommendedValue: Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.deprecated'),
        resultingValue: 'c1',
        isDeprecated: true,
        willBeRemoved: false
      },
      {
        type: 't2',
        name: 'p2',
        currentValue: 'c2',
        recommendedValue: Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.deprecated'),
        resultingValue: Em.I18n.t('popup.clusterCheck.Upgrade.configsMerge.willBeRemoved'),
        isDeprecated: true,
        willBeRemoved: true
      }
    ],
    isPrimaryCalled = false,
    primary = function () {
      isPrimaryCalled = true;
    };

  it('popup with configs merge warnings', function () {
    var popup = App.showUpgradeConfigsMergePopup(conflicts, version , primary);
    expect(popup.get('header')).to.equal(Em.I18n.t('popup.clusterCheck.Upgrade.header').format(version));
    expect(popup.get('bodyClass').create().get('configs')).to.eql(result);
    popup.onPrimary();
    expect(isPrimaryCalled).to.be.true;
  });

});
