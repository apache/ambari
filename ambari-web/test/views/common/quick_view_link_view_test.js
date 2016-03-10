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

describe('App.QuickViewLinks', function () {

  var view;

  beforeEach(function () {
    view = App.QuickViewLinks.create();
  });

  describe('#setQuickLinksErrorCallback', function () {

    beforeEach(function () {
      view.setProperties({
        quickLinks: [],
        isLoaded: false,
        areQuickLinksUndefined: false
      });
      view.setQuickLinksErrorCallback();
    });

    it('should fill array for dropdown', function () {
      expect(view.getProperties(['quickLinks', 'isLoaded', 'areQuickLinksUndefined'])).to.eql({
        quickLinks: [{
          label: Em.I18n.t('quick.links.error.label'),
          url: 'javascript:alert("' + Em.I18n.t('contact.administrator') + '");'
        }],
        isLoaded: true,
        areQuickLinksUndefined: true
      });
    });

  });

  describe('#linkTarget', function () {

    var serviceNameCases = [
      {
        serviceName: 'HDFS',
        target: '_blank'
      },
      {
        serviceName: 'YARN',
        target: '_blank'
      },
      {
        serviceName: 'MAPREDUCE2',
        target: '_blank'
      },
      {
        serviceName: 'HBASE',
        target: '_blank'
      },
      {
        serviceName: 'OOZIE',
        target: '_blank'
      },
      {
        serviceName: 'GANGLIA',
        target: '_blank'
      },
      {
        serviceName: 'STORM',
        target: '_blank'
      },
      {
        serviceName: 'SPARK',
        target: '_blank'
      },
      {
        serviceName: 'FALCON',
        target: '_blank'
      },
      {
        serviceName: 'ACCUMULO',
        target: '_blank'
      },
      {
        serviceName: 'ATLAS',
        target: '_blank'
      },
      {
        serviceName: 'RANGER',
        target: '_blank'
      },
      {
        serviceName: 'AMBARI_METRICS',
        target: '_blank'
      },
      {
        serviceName: 'ZOOKEEPER',
        target: ''
      }
    ];

    beforeEach(function () {
      view.set('content', {});
    });

    serviceNameCases.forEach(function (item) {

      it(item.serviceName, function () {
        view.setProperties({
          'isLoaded': false,
          'areQuickLinksUndefined': false,
          'content.serviceName': item.serviceName
        });
        view.propertyDidChange('service');
        expect(view.get('linkTarget')).to.equal(item.target);
      });

    });

    it('quick links not defined', function () {
      view.setProperties({
        isLoaded: true,
        areQuickLinksUndefined: true
      });
      expect(view.get('linkTarget')).to.equal('');
    });

  });

});