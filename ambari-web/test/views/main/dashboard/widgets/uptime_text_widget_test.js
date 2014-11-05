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

describe('App.UptimeTextDashboardWidgetView', function() {

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
      var uptimeTextDashboardWidgetView = App.UptimeTextDashboardWidgetView.create({thresh1:40, thresh2:70});
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
      var uptimeTextDashboardWidgetView = App.UptimeTextDashboardWidgetView.create({thresh1:40, thresh2:70});
      it('timestamp ' + timestamp.t + '. timeUnit should be ' + '"' + timestamp.e.timeUnit + '"', function() {
        var result = uptimeTextDashboardWidgetView.uptimeProcessing(((new Date()).getTime() - timestamp.diff));
        expect(uptimeTextDashboardWidgetView.get('timeUnit')).to.equal(timestamp.e.timeUnit);
      });
    });
  });

});
