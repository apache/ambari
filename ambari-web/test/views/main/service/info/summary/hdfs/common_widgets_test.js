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
require('/views/main/service/info/summary/hdfs/common_widgets');

var view;

function testDiskPart(propertyName, i18nKey, totalKey, usedKey) {
  describe('#' + propertyName, function () {

    beforeEach(function () {
      view.reopen({
        model: Em.Object.create()
      });
    });

    it('n/a / n/a', function () {
      expect(view.get(propertyName)).to.equal('n/a / n/a');
    });

    it('"value / n/a" and "n/a / n/a" for usedKey === "nonDfsUsed"', function () {
      if (usedKey === 'nonDfsUsed') {
        expect(view.get(propertyName)).to.equal('n/a / n/a');
      } else {
        view.set(usedKey, 8);
        expect(view.get(propertyName)).to.equal('8.0 Bytes / n/a');
      }
    });

    it('n/a / value', function () {
      view.set(totalKey, 20);
      expect(view.get(propertyName)).to.equal('n/a / 20.0 Bytes');
    });

    it('value / value', function () {
      view.set(totalKey, 20);
      if (usedKey === 'nonDfsUsed') {
        view.set('model.capacityRemaining', 2);
        view.set('model.capacityUsed', 8);
        expect(view.get(propertyName)).to.equal('10.0 Bytes / 20.0 Bytes');
      } else {
        view.set(usedKey, 8);
        expect(view.get(propertyName)).to.equal('8.0 Bytes / 20.0 Bytes');
      }
    });
  });
}

function testDiskPartPercent(propertyName, i18nKey, totalKey, usedKey) {
  describe('#' + propertyName, function () {

    beforeEach(function () {
      view.reopen({
        model: Em.Object.create()
      });
    });

    it('n/a and 0.00% for usedKey === "nonDfsUsed"', function () {
      view.set(totalKey, 20);
      if (usedKey === 'nonDfsUsed') {
        expect(view.get(propertyName)).to.equal('0.00%');
      } else {
        expect(view.get(propertyName)).to.equal('n/a %');
      }
    });

    it('0 %', function () {
      expect(view.get(propertyName)).to.equal('0%');
    });

    it('should return percent', function () {
      view.set(totalKey, 20);
      if (usedKey === 'nonDfsUsed') {
        view.set('model.capacityRemaining', 2);
        view.set('model.capacityUsed', 8);
        expect(view.get(propertyName)).to.equal('50.00%');
      } else {
        view.set(usedKey, 8);
        expect(view.get(propertyName)).to.equal('40.00%');
      }
    });
  });
}

function getView(options) {
  return App.HDFSSummaryCommonWidgetsView.create(options || {});
}

describe('App.HDFSSummaryCommonWidgetsView', function () {

  beforeEach(function () {
    view = getView({
      model: Em.Object.create()
    });
  });

  testDiskPart('dfsUsedDisk', 'dashboard.services.hdfs.capacityUsed', 'model.capacityTotal', 'model.capacityUsed');
  testDiskPart('nonDfsUsedDisk', 'dashboard.services.hdfs.capacityUsed', 'model.capacityTotal', 'nonDfsUsed');
  testDiskPart('remainingDisk', 'dashboard.services.hdfs.capacityUsed', 'model.capacityTotal', 'model.capacityRemaining');

  testDiskPartPercent(
    'dfsUsedDiskPercent', 'dashboard.services.hdfs.capacityUsedPercent', 'model.capacityTotal', 'model.capacityUsed');
  testDiskPartPercent(
    'nonDfsUsedDiskPercent', 'dashboard.services.hdfs.capacityUsedPercent', 'model.capacityTotal', 'nonDfsUsed');
  testDiskPartPercent(
    'remainingDiskPercent', 'dashboard.services.hdfs.capacityUsedPercent', 'model.capacityTotal', 'model.capacityRemaining');
  
  describe('#nonDfsUsed', function () {

    it('should return null', function () {
      view.set('model.capacityTotal', 20);
      view.set('model.capacityRemaining', 2);
      view.propertyDidChange('nonDfsUsed');
      expect(view.get('nonDfsUsed')).to.equal(null);
    });

    it('should return number of nonDfsUsed capacity', function () {
      view.set('model.capacityTotal', 20);
      view.set('model.capacityRemaining', 2);
      view.set('model.capacityUsed', 8);
      view.propertyDidChange('nonDfsUsed');
      expect(view.get('nonDfsUsed')).to.equal(10);
    });
  });

});
