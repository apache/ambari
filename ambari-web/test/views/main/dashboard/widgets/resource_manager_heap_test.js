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

require('views/main/dashboard/widgets/resource_manager_heap');

describe('App.ResourceManagerHeapPieChartView', function () {

  var view;

  beforeEach(function () {
    view = App.ResourceManagerHeapPieChartView.create({
      model: Em.Object.create()
    });
  });

  describe('#didInsertElement()', function () {

    beforeEach(function () {
      sinon.stub(view, 'calc', Em.K);
    });

    it('should execute calc function', function () {
      view.didInsertElement();
      expect(view.calc.calledOnce).to.be.true;
    });
  });

  describe('#getUsed()', function () {

    it('should return 0 used memory value', function () {
      expect(view.getUsed()).to.equal(0);
    });

    it('should return used memory value', function () {
      view.set('model.jvmMemoryHeapUsed', 100000);
      expect(view.getUsed()).to.equal(0.095367431640625);
    });
  });

  describe('#getMax()', function () {

    it('should return 0 max memory value', function () {
      expect(view.getMax()).to.equal(0);
    });

    it('should return max memory value', function () {
      view.set('model.jvmMemoryHeapMax', 200000);
      expect(view.getMax()).to.equal(0.19073486328125);
    });
  });

});
