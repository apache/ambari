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
    var timestamps = [
      {
        t: 1358245370553,
        e: {
          l: 2,
          f: 'Tue Jan 15 2013'
        }
      },
      {
        t: 0,
        e: {
          l: 2,
          f: 'Thu Jan 01 1970'
        }
      }
    ];
    timestamps.forEach(function(timestamp) {
      var uptimeTextDashboardWidgetView = App.UptimeTextDashboardWidgetView.create({thresh1:40, thresh2:70});
      it('timestamp ' + timestamp.t, function() {
        var result = uptimeTextDashboardWidgetView.timeConverter(timestamp.t);
        expect(result.length).to.equal(timestamp.e.l);
        expect(result[0]).to.equal(timestamp.e.f);
      });
    });
  });

  describe('#uptimeProcessing', function() {
    var timestamps = [
      {
        t: (new Date()).getTime() - 10*1000,
        e: {
          timeUnit: 's'
        }
      },
      {
        t: (new Date()).getTime() - 3600*1000,
        e: {
          timeUnit: 'hr'
        }
      },
      {
        t: (new Date()).getTime() - 24*3600*1000,
        e: {
          timeUnit: 'd'
        }
      },
      {
        t: (new Date()).getTime() - 1800*1000,
        e: {
          timeUnit: 'min'
        }
      }
    ];
    timestamps.forEach(function(timestamp) {
      var uptimeTextDashboardWidgetView = App.UptimeTextDashboardWidgetView.create({thresh1:40, thresh2:70});
      it('timestamp ' + timestamp.t + '. timeUnit should be ' + '"' + timestamp.e.timeUnit + '"', function() {
        var result = uptimeTextDashboardWidgetView.uptimeProcessing(timestamp.t);
        expect(uptimeTextDashboardWidgetView.get('timeUnit')).to.equal(timestamp.e.timeUnit);
      });
    });
  });

});
