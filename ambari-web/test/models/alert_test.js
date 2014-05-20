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

require('models/alert');

var alert,
  sampleTime = 1399312800,
  statusCases = [
    {
      status: 0,
      property: 'isOk',
      format: 'OK'
    },
    {
      status: 1,
      property: 'isWarning',
      format: 'WARN'
    },
    {
      status: 2,
      property: 'isCritical',
      format: 'CRIT'
    },
    {
      status: 3,
      property: 'isPassive',
      format: 'MAINT'
    },
    {
      status: 4,
      property: '',
      format: 'UNKNOWN'
    }
  ],
  ignoredCases = [
    {
      title: 'title',
      result: false
    },
    {
      title: 'Percent',
      result: true
    }
  ],
  serviceTypeCases = [
    {
      type: 'MAPREDUCE',
      name: 'MapReduce',
      link: '#/main/services/MAPREDUCE/summary'
    },
    {
      type: 'HDFS',
      name: 'HDFS',
      link: '#/main/services/HDFS/summary'
    },
    {
      type: 'HBASE',
      name: 'HBase',
      link: '#/main/services/HBASE/summary'
    },
    {
      type: 'ZOOKEEPER',
      name: 'Zookeeper',
      link: '#/main/services/ZOOKEEPER/summary'
    },
    {
      type: 'OOZIE',
      name: 'Oozie',
      link: '#/main/services/OOZIE/summary'
    },
    {
      type: 'HIVE',
      name: 'Hive',
      link: '#/main/services/HIVE/summary'
    },
    {
      type: 'service',
      name: null,
      link: null
    },
    {
      type: null,
      name: null,
      link: null
    }
  ],
  titles = ['NodeManager health', 'NodeManager process', 'TaskTracker process', 'RegionServer process', 'DataNode process', 'DataNode space', 'ZooKeeper Server process', 'Supervisors process'];

describe('App.Alert', function () {

  beforeEach(function() {
    alert = App.Alert.create();
  });

  describe('#date', function () {
    it('is Mon May 05 2014', function () {
      alert.set('lastTime', sampleTime);
      expect(alert.get('date').toDateString()).to.equal('Mon May 05 2014');
    });
  });

  statusCases.forEach(function (item) {
    var status = item.status,
      property = item.property;
    if (property) {
      describe('#' + property, function () {
        it('status ' + status + ' is for ' + property, function () {
          alert.set('status', status);
          expect(alert.get(property)).to.be.true;
          var falseStates = statusCases.mapProperty('property').without(property).without('');
          var falseStatuses = [];
          falseStates.forEach(function (state) {
            falseStatuses.push(alert.get(state));
          });
          expect(falseStatuses).to.eql([false, false, false]);
        });
      });
    }
  });

  describe('#ignoredForServices', function () {
    titles.forEach(function (item) {
      it('should be true for ' + item, function () {
        alert.set('title', item);
        expect(alert.get('ignoredForServices')).to.be.true;
      });
    });
    it('should be false', function () {
      alert.set('title', 'title');
      expect(alert.get('ignoredForServices')).to.be.false;
    });
  });

  describe('#ignoredForHosts', function () {
    ignoredCases.forEach(function (item) {
      it('should be ' + item.result, function () {
        alert.set('title', item.title);
        expect(alert.get('ignoredForHosts')).to.equal(item.result);
      });
    });
  });

  describe('#timeSinceAlert', function () {
    statusCases.forEach(function (item) {
      var format = item.format;
      it('should indicate ' + format + ' status duration', function () {
        alert.setProperties({
          lastTime: sampleTime,
          status: item.status.toString()
        });
        expect(alert.get('timeSinceAlert')).to.have.string(format);
        expect(alert.get('timeSinceAlert.length')).to.be.above(format.length);
        alert.set('lastTime', 0);
        expect(alert.get('timeSinceAlert')).to.equal(format);
      });
    });
    it('should be empty', function () {
      alert.set('lastTime', undefined);
      expect(alert.get('timeSinceAlert')).to.be.empty;
    });
  });

  describe('#makeTimeAtleastMinuteAgo', function () {
    it('should set the minute-ago time', function () {
      var time = App.dateTime() - 50000,
        date = new Date(time - 10000);
      alert.set('lastTime', time);
      expect(alert.makeTimeAtleastMinuteAgo(alert.get('date'))).to.be.at.least(date);
    });
    it('should return the actual time', function () {
      var time = App.dateTime() - 70000;
      alert.set('lastTime', time);
      expect(alert.makeTimeAtleastMinuteAgo(alert.get('date'))).to.eql(alert.get('date'));
    });
  });

  describe('#timeSinceAlertDetails', function () {
    it ('should return the appropriate string', function () {
      alert.set('lastTime', sampleTime);
      var occurred = Em.I18n.t('services.alerts.occurredOn').format('May 05 2014', alert.get('date').toLocaleTimeString());
      var brChecked = Em.I18n.t('services.alerts.brLastCheck').format($.timeago(sampleTime));
      var checked = Em.I18n.t('services.alerts.lastCheck').format($.timeago(sampleTime));
      expect(alert.get('timeSinceAlertDetails')).to.equal(occurred);
      alert.set('lastCheck', sampleTime / 1000);
      expect(alert.get('timeSinceAlertDetails')).to.equal(occurred + brChecked);
      alert.set('lastTime', undefined);
      expect(alert.get('timeSinceAlertDetails')).to.equal(checked);
    });
    it ('should be empty', function () {
      alert.set('lastCheck', undefined);
      expect(alert.get('timeSinceAlertDetails')).to.be.empty;
    });
  });

  describe('#serviceName', function () {
    serviceTypeCases.forEach(function (item) {
      it('should be ' + item.name, function () {
        alert.set('serviceType', item.type);
        expect(alert.get('serviceName')).to.equal(item.name);
      });
    });
  });

  describe('#serviceLink', function () {
    serviceTypeCases.forEach(function (item) {
      it('should be ' + item.link, function () {
        alert.set('serviceType', item.type);
        expect(alert.get('serviceLink')).to.equal(item.link);
      });
    });
  });

});
