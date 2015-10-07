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

require('mixins/common/widgets/time_range_mixin');

describe('App.TimeRangeMixin', function () {

  var obj;

  beforeEach(function () {
    obj = Em.Object.create(App.TimeRangeMixin);
  });

  describe('#currentTimeRange', function () {

    var cases = Em.Object.create(App.TimeRangeMixin).get('timeRangeOptions'),
      title = 'should set "{0}" time range';

    cases.forEach(function (item) {
      it(title.format(item.name), function () {
        obj.set('currentTimeRangeIndex', item.index);
        expect(obj.get('currentTimeRange')).to.eql(item);
      });
    });

  });

  describe('#setTimeRange', function () {

    it('should set time range', function () {
      obj.setTimeRange({
        context: {
          index: 1
        }
      });
      expect(obj.get('currentTimeRangeIndex')).to.equal(1);
    });

  });

});
