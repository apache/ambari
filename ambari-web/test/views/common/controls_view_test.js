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
require('views/common/controls_view');
var validator = require('utils/validator');

describe('App.ServiceConfigRadioButtons', function () {

  describe('#setConnectionUrl', function () {
    var view = App.ServiceConfigRadioButtons.create();
    beforeEach(function () {
      sinon.stub(view, 'getPropertyByType', function (name) {
        return App.ServiceConfigProperty.create({'name': name});
      });
      sinon.stub(view, 'getDefaultPropertyValue', function () {
        return 'host:{0},db:{1}';
      });
    });

    afterEach(function () {
      view.getPropertyByType.restore();
      view.getDefaultPropertyValue.restore();
    });

    it('updates value for connection url', function () {
      expect(view.setConnectionUrl('hostName', 'dbName').get('value')).to.equal('host:hostName,db:dbName');
    });
  });

  describe('#setRequiredProperties', function () {
    var view = App.ServiceConfigRadioButtons.create({
      serviceConfig: Em.Object.create(),
      categoryConfigsAll: [
        App.ServiceConfigProperty.create({
          name: 'p1',
          value: 'v1'
        }),
        App.ServiceConfigProperty.create({
          name: 'p2',
          value: 'v2'
        })
      ]
    });

    beforeEach(function () {
      sinon.stub(view, 'getPropertyByType', function (name) {
        return view.get('categoryConfigsAll').findProperty('name', name);
      });
      sinon.stub(view, 'getDefaultPropertyValue', function (name) {
        return name + '_v';
      });
    });

    afterEach(function () {
      view.getPropertyByType.restore();
      view.getDefaultPropertyValue.restore();
    });

    it('updates value for connection url', function () {
      view.setRequiredProperties(['p2', 'p1']);
      expect(view.get('categoryConfigsAll').findProperty('name', 'p1').get('value')).to.equal('p1_v');
      expect(view.get('categoryConfigsAll').findProperty('name', 'p2').get('value')).to.equal('p2_v');
    });
  });

  describe('#handleDBConnectionProperty', function () {

    var view,
      cases = [
        {
          dbType: 'mysql',
          driver: 'mysql-connector-java.jar',
          serviceConfig: {
            name: 'hive_database',
            value: 'New MySQL Database',
            serviceName: 'HIVE'
          },
          parentView: Em.Object.create({
            serviceConfigs: [
              Em.Object.create({
                name: 'javax.jdo.option.ConnectionURL',
                displayName: 'Database URL'
              }),
              Em.Object.create({
                name: 'hive_database',
                displayName: 'Hive Database'
              })
            ]
          }),
          currentStackVersion: 'HDP-2.2',
          propertyAppendTo1: 'javax.jdo.option.ConnectionURL',
          propertyAppendTo2: 'hive_database',
          isAdditionalView1Null: true,
          isAdditionalView2Null: true,
          title: 'Hive, embedded database'
        },
        {
          dbType: 'postgres',
          driver: 'postgresql.jar',
          serviceConfig: {
            name: 'hive_database',
            value: 'Existing PostgreSQL Database',
            serviceName: 'HIVE'
          },
          parentView: Em.Object.create({
            serviceConfigs: [
              Em.Object.create({
                name: 'javax.jdo.option.ConnectionURL',
                displayName: 'Database URL'
              }),
              Em.Object.create({
                name: 'hive_database',
                displayName: 'Hive Database'
              })
            ]
          }),
          currentStackVersion: 'HDP-2.2',
          propertyAppendTo1: 'javax.jdo.option.ConnectionURL',
          propertyAppendTo2: 'hive_database',
          isAdditionalView1Null: false,
          isAdditionalView2Null: false,
          title: 'Hive, external database'
        },
        {
          dbType: 'derby',
          driver: 'driver.jar',
          serviceConfig: {
            name: 'oozie_database',
            value: 'New Derby Database',
            serviceName: 'OOZIE'
          },
          parentView: Em.Object.create({
            serviceConfigs: [
              Em.Object.create({
                name: 'oozie.service.JPAService.jdbc.url',
                displayName: 'Database URL'
              }),
              Em.Object.create({
                name: 'oozie_database',
                displayName: 'Oozie Database'
              })
            ]
          }),
          currentStackVersion: 'HDP-2.2',
          propertyAppendTo1: 'oozie.service.JPAService.jdbc.url',
          propertyAppendTo2: 'oozie_database',
          isAdditionalView1Null: true,
          isAdditionalView2Null: true,
          title: 'Oozie, embedded database'
        },
        {
          dbType: 'oracle',
          driver: 'ojdbc6.jar',
          serviceConfig: {
            name: 'oozie_database',
            value: 'Existing Oracle Database',
            serviceName: 'OOZIE'
          },
          parentView: Em.Object.create({
            serviceConfigs: [
              Em.Object.create({
                name: 'oozie.service.JPAService.jdbc.url',
                displayName: 'Database URL'
              }),
              Em.Object.create({
                name: 'oozie_database',
                displayName: 'Oozie Database'
              })
            ]
          }),
          currentStackVersion: 'HDP-2.2',
          propertyAppendTo1: 'oozie.service.JPAService.jdbc.url',
          propertyAppendTo2: 'oozie_database',
          isAdditionalView1Null: false,
          isAdditionalView2Null: false,
          title: 'Oozie, external database'
        },
        {
          dbType: 'mysql',
          driver: 'mysql-connector-java.jar',
          serviceConfig: {
            name: 'DB_FLAVOR',
            value: 'MYSQL',
            serviceName: 'RANGER'
          },
          parentView: Em.Object.create({
            serviceConfigs: [
              Em.Object.create({
                name: 'ranger.jpa.jdbc.url'
              }),
              Em.Object.create({
                name: 'DB_FLAVOR'
              })
            ]
          }),
          currentStackVersion: 'HDP-2.2',
          propertyAppendTo1: 'ranger.jpa.jdbc.url',
          propertyAppendTo2: 'DB_FLAVOR',
          isAdditionalView1Null: true,
          isAdditionalView2Null: true,
          title: 'Ranger, HDP 2.2, external database'
        },
        {
          dbType: 'mssql',
          driver: 'sqljdbc4.jar',
          serviceConfig: {
            name: 'DB_FLAVOR',
            value: 'MSSQL',
            serviceName: 'RANGER'
          },
          parentView: Em.Object.create({
            serviceConfigs: [
              Em.Object.create({
                name: 'ranger.jpa.jdbc.url'
              }),
              Em.Object.create({
                name: 'DB_FLAVOR'
              })
            ]
          }),
          currentStackVersion: 'HDP-2.3',
          propertyAppendTo1: 'ranger.jpa.jdbc.url',
          propertyAppendTo2: 'DB_FLAVOR',
          isAdditionalView1Null: false,
          isAdditionalView2Null: false,
          title: 'Ranger, HDP 2.3, external database'
        }
      ];

    before(function () {
      sinon.stub(Em.run, 'next', function (arg) {
        arg();
      });
    });

    afterEach(function () {
      App.get.restore();
      view.sendRequestRorDependentConfigs.restore();
    });

    after(function () {
      Em.run.next.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        sinon.stub(App, 'get').withArgs('currentStackName').returns('HDP').withArgs('currentStackVersion').returns(item.currentStackVersion);
        view = App.ServiceConfigRadioButtons.create({parentView: item.parentView});
        sinon.stub(view, 'sendRequestRorDependentConfigs', Em.K);
        view.setProperties({
          categoryConfigsAll: item.parentView.get('serviceConfigs'),
          serviceConfig: item.serviceConfig
        });
        var additionalView1 = view.get('categoryConfigsAll').findProperty('name', item.propertyAppendTo1).get('additionalView'),
          additionalView2 = view.get('categoryConfigsAll').findProperty('name', item.propertyAppendTo2).get('additionalView');
        expect(Em.isNone(additionalView1)).to.equal(item.isAdditionalView1Null);
        expect(Em.isNone(additionalView2)).to.equal(item.isAdditionalView2Null);
        if (!item.isAdditionalView2Null) {
          expect(additionalView2.create().get('message')).to.equal(Em.I18n.t('services.service.config.database.msg.jdbcSetup').format(item.dbType, item.driver));
        }
      });
    });

  });
});

describe('App.ServiceConfigRadioButton', function () {

  describe('#disabled', function () {

    var cases = [
      {
        wizardControllerName: 'addServiceController',
        value: 'New MySQL Database',
        title: 'Add Service Wizard, new database',
        disabled: false
      },
      {
        wizardControllerName: 'installerController',
        value: 'New MySQL Database',
        title: 'Install Wizard, new database',
        disabled: false
      },
      {
        wizardControllerName: 'addServiceController',
        value: 'Existing MySQL Database',
        title: 'Add Service Wizard, existing database',
        disabled: false
      },
      {
        wizardControllerName: 'installerController',
        value: 'Existing MySQL Database',
        title: 'Install Wizard, existing database',
        disabled: false
      },
      {
        wizardControllerName: null,
        value: 'New MySQL Database',
        title: 'No installer, new database',
        disabled: true
      },
      {
        wizardControllerName: null,
        value: 'Existing MySQL Database',
        title: 'No installer, existing database',
        disabled: false
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        var view = App.ServiceConfigRadioButton.create({
          parentView: Em.Object.create({
            serviceConfig: Em.Object.create()
          }),
          controller: Em.Object.create({
            wizardController: Em.Object.create({
              name: null
            })
          })
        });
        view.set('value', item.value);
        view.set('controller.wizardController.name', item.wizardControllerName);
        view.set('parentView.serviceConfig.isEditable', true);
        expect(view.get('disabled')).to.equal(item.disabled);
      });
    });

    it('parent view is disabled', function () {
      var view = App.ServiceConfigRadioButton.create({
        parentView: Em.Object.create({
          serviceConfig: Em.Object.create()
        })
      });
      view.set('parentView.serviceConfig.isEditable', false);
      expect(view.get('disabled')).to.be.true;
    });

  });

});

describe('App.CheckDBConnectionView', function () {

  describe('#masterHostName', function () {

    var cases = [
        {
          serviceName: 'OOZIE',
          value: 'h0'
        },
        {
          serviceName: 'KERBEROS',
          value: 'h1'
        },
        {
          serviceName: 'HIVE',
          value: 'h2'
        },
        {
          serviceName: 'RANGER',
          value: 'h3'
        }
      ],
      categoryConfigsAll = [
        Em.Object.create({
          name: 'oozieserver_host',
          value: 'h0'
        }),
        Em.Object.create({
          name: 'kdc_host',
          value: 'h1'
        }),
        Em.Object.create({
          name: 'hivemetastore_host',
          value: 'h2'
        }),
        Em.Object.create({
          name: 'rangerserver_host',
          value: 'h3'
        })
      ];

    cases.forEach(function (item) {
      it(item.serviceName, function () {
        var view = App.CheckDBConnectionView.create({
          parentView: {
            service: {
              serviceName: item.serviceName
            },
            categoryConfigsAll: categoryConfigsAll
          }
        });
        expect(view.get('masterHostName')).to.equal(item.value);
      });
    });

  });

  describe('#setResponseStatus', function () {

    var view,
      cases = [
        {
          isSuccess: 'success',
          logsPopupBefore: null,
          logsPopup: null,
          responseCaption: Em.I18n.t('services.service.config.database.connection.success'),
          isConnectionSuccess: true,
          title: 'success, no popup displayed'
        },
        {
          isSuccess: 'success',
          logsPopupBefore: {},
          logsPopup: {
            header: Em.I18n.t('services.service.config.connection.logsPopup.header').format('MySQL', Em.I18n.t('common.success'))
          },
          responseCaption: Em.I18n.t('services.service.config.database.connection.success'),
          isConnectionSuccess: true,
          title: 'success, popup is displayed'
        },
        {
          isSuccess: 'error',
          logsPopupBefore: {},
          logsPopup: {
            header: Em.I18n.t('services.service.config.connection.logsPopup.header').format('MySQL', Em.I18n.t('common.error'))
          },
          responseCaption: Em.I18n.t('services.service.config.database.connection.failed'),
          isConnectionSuccess: false,
          title: 'error, popup is displayed'
        }
      ];

    beforeEach(function () {
      view = App.CheckDBConnectionView.create({
        databaseName: 'MySQL'
      });
      sinon.stub(view, 'setConnectingStatus', Em.K);
    });

    afterEach(function () {
      view.setConnectingStatus.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        view.set('logsPopup', item.logsPopupBefore);
        view.setResponseStatus(item.isSuccess);
        expect(view.get('isRequestResolved')).to.be.true;
        expect(view.setConnectingStatus.calledOnce).to.be.true;
        expect(view.setConnectingStatus.calledWith(false)).to.be.true;
        expect(view.get('responseCaption')).to.equal(item.responseCaption);
        expect(view.get('isConnectionSuccess')).to.equal(item.isConnectionSuccess);
        expect(view.get('logsPopup')).to.eql(item.logsPopup);
      });
    });

  });

  describe('#showLogsPopup', function () {

    var view,
      cases = [
        {
          isConnectionSuccess: true,
          showAlertPopupCallCount: 0,
          title: 'successful connection'
        },
        {
          isConnectionSuccess: false,
          isRequestResolved: true,
          showAlertPopupCallCount: 1,
          responseFromServer: 'fail',
          header: Em.I18n.t('services.service.config.connection.logsPopup.header').format('MySQL', Em.I18n.t('common.error')),
          popupMethodExecuted: 'onClose',
          title: 'failed connection without output data, popup dismissed with Close button'
        },
        {
          isConnectionSuccess: false,
          isRequestResolved: false,
          showAlertPopupCallCount: 1,
          responseFromServer: {
            stderr: 'stderr',
            stdout: 'stdout',
            structuredOut: 'structuredOut'
          },
          header: Em.I18n.t('services.service.config.connection.logsPopup.header').format('MySQL', Em.I18n.t('common.testing')),
          popupMethodExecuted: 'onPrimary',
          title: 'check in progress with output data, popup dismissed with OK button'
        }
      ];

    beforeEach(function () {
      view = App.CheckDBConnectionView.create({
        databaseName: 'MySQL'
      });
      sinon.spy(App, 'showAlertPopup');
    });

    afterEach(function () {
      App.showAlertPopup.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        view.setProperties({
          isConnectionSuccess: item.isConnectionSuccess,
          isRequestResolved: item.isRequestResolved,
          responseFromServer: item.responseFromServer
        });
        view.showLogsPopup();
        expect(App.showAlertPopup.callCount).to.equal(item.showAlertPopupCallCount);
        if (!item.isConnectionSuccess) {
          expect(view.get('logsPopup.header')).to.equal(item.header);
          if (typeof item.responseFromServer == 'object') {
            expect(view.get('logsPopup.bodyClass').create().get('openedTask')).to.eql(item.responseFromServer);
          } else {
            expect(view.get('logsPopup.body')).to.equal(item.responseFromServer);
          }
          view.get('logsPopup')[item.popupMethodExecuted]();
          expect(view.get('logsPopup')).to.be.null;
        }
      });
    });

  });

});

describe('App.BaseUrlTextField', function () {

  var view = App.BaseUrlTextField.create({
    repository: Em.Object.create({
      baseUrl: 'val'
    }),
    parentView: Em.Object.create({
      uiValidation: Em.K
    })
  });

  describe('#valueWasChanged', function () {
    it('should be recalculated after value is changed', function () {
      view.setProperties({
        value: 'val',
        defaultValue: 'val'
      });
      expect(view.get('valueWasChanged')).to.be.false;
      view.set('value', 'newVal');
      expect(view.get('valueWasChanged')).to.be.true;
    });
  });

  describe('#restoreValue()', function () {
    it('should unset value', function () {
      view.setProperties({
        value: 'valNew',
        defaultValue: 'val'
      });
      view.restoreValue();
      expect(view.get('value')).to.equal('val');
    });
  });

  describe('#didInsertElement()', function () {
    it('should set defaultValue', function () {
      view.setProperties({
        value: 'valNew',
        defaultValue: 'val'
      });
      view.didInsertElement();
      expect(view.get('defaultValue')).to.equal('valNew');
    });
  });

  describe('#validate()', function () {
    beforeEach(function(){
      sinon.stub(view.get('parentView'), 'uiValidation', Em.K);
      sinon.stub(validator, 'isValidBaseUrl').returns(true);
    });
    afterEach(function(){
      view.get('parentView').uiValidation.restore();
      validator.isValidBaseUrl.restore();
    });
    it('skip validation', function () {
      view.set('repository', Em.Object.create({
        skipValidation: true
      }));
      expect(view.get('repository.hasError')).to.be.false;
    });
    it('apply validation', function () {
      view.set('repository', Em.Object.create({
          skipValidation: false
      }));
      expect(view.get('repository.hasError')).to.be.false;
      expect(validator.isValidBaseUrl.calledOnce).to.be.true;
    });
  });
});
