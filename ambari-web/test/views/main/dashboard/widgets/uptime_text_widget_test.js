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
require('views/main/dashboard/widget');
require('views/main/dashboard/widgets/text_widget');
require('views/main/dashboard/widgets/uptime_text_widget');

var uptimeTextDashboardWidgetView;
describe('App.UptimeTextDashboardWidgetView', function() {

  beforeEach(function () {
    uptimeTextDashboardWidgetView = App.UptimeTextDashboardWidgetView.create({thresholdMin:40, thresholdMax:70});
  });

  describe('#timeConverter', function() {
    var ts1 = 1358245370553, ts2 = 0;
    var timestamps = [
      {
        t: ts1,
        e: {
          l: 2,
          f: new Date(ts1)
        }
      },
      {
        t: ts2,
        e: {
          l: 2,
          f: new Date(ts2)
        }
      }
    ];
    timestamps.forEach(function(timestamp) {
      it('timestamp ' + timestamp.t, function() {
        var result = uptimeTextDashboardWidgetView.timeConverter(timestamp.t);
        expect(result.length).to.equal(timestamp.e.l);
        assert.include(timestamp.e.f.toString(), result[0].toString(), timestamp.e.f + ' contains string ' + result[0]);
      });
    });
  });

  describe('#uptimeProcessing', function() {
    var timestamps = [
      {
        diff: 10*1000,
        e: {
          timeUnit: 's'
        }
      },
      {
        diff: 3600*1000,
        e: {
          timeUnit: 'hr'
        }
      },
      {
        diff: 24*3600*1000,
        e: {
          timeUnit: 'd'
        }
      },
      {
        diff: 1800*1000,
        e: {
          timeUnit: 'min'
        }
      }
    ];
    timestamps.forEach(function(timestamp) {
      it('timestamp {0}. timeUnit should be "{1}"'.format(timestamp.t, timestamp.e.timeUnit), function() {
        uptimeTextDashboardWidgetView.uptimeProcessing(new Date().getTime() - timestamp.diff);
        expect(uptimeTextDashboardWidgetView.get('timeUnit')).to.equal(timestamp.e.timeUnit);
      });
    });
  });

});
