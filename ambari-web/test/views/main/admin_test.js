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
require('views/main/admin');

describe('App.MainAdminView', function () {

  var view;

  beforeEach(function () {
    view = App.MainAdminView.create();
  });

  describe('#categories', function () {

    var cases = [
      {
        isHadoopWindowsStack: true,
        categories: [
          {
            name: 'stackAndUpgrade',
            url: 'stackAndUpgrade.index',
            label: Em.I18n.t('admin.stackUpgrade.title')
          },
          {
            name: 'adminServiceAccounts',
            url: 'adminServiceAccounts',
            label: Em.I18n.t('common.serviceAccounts')
          }
        ],
        title: 'HDPWIN'
      },
      {
        isHadoopWindowsStack: false,
        categories: [
          {
            name: 'stackAndUpgrade',
            url: 'stackAndUpgrade.index',
            label: Em.I18n.t('admin.stackUpgrade.title')
          },
          {
            name: 'adminServiceAccounts',
            url: 'adminServiceAccounts',
            label: Em.I18n.t('common.serviceAccounts')
          },
          {
            name: 'kerberos',
            url: 'adminKerberos.index',
            label: Em.I18n.t('common.kerberos')
          }
        ],
        title: 'not HDPWIN'
      }
    ];

    afterEach(function () {
      App.get.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        sinon.stub(App, 'get').withArgs('isHadoopWindowsStack').returns(item.isHadoopWindowsStack);
        view.propertyDidChange('categories');
        expect(view.get('categories')).to.eql(item.categories);
      });
    });

  });

});