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

describe('App.ServiceConfigRadioButtons', function () {

  describe('#didInsertElement', function () {

    var view = App.ServiceConfigRadioButtons.create({
        categoryConfigsAll: [],
        controller: Em.Object.create({
          wizardController: Em.Object.create({})
        }),
        serviceConfig: Em.Object.create({
          value: null
        })
      }),
      cases = [
        {
          wizardControllerName: 'addServiceController',
          serviceConfigValue: 'New MySQL Database',
          onOptionsChangeCalledTwice: true,
          handleDBConnectionPropertyCalledTwice: false,
          title: 'Add Service Wizard, New MySQL Database'
        },
        {
          wizardControllerName: 'addServiceController',
          serviceConfigValue: 'Existing MySQL Database',
          onOptionsChangeCalledTwice: false,
          handleDBConnectionPropertyCalledTwice: true,
          title: 'Add Service Wizard, Existing MySQL Database'
        },
        {
          wizardControllerName: 'installerController',
          serviceConfigValue: 'New MySQL Database',
          onOptionsChangeCalledTwice: true,
          handleDBConnectionPropertyCalledTwice: false,
          title: 'Install Wizard, New MySQL Database'
        },
        {
          wizardControllerName: 'installerController',
          serviceConfigValue: 'Existing MySQL Database',
          onOptionsChangeCalledTwice: false,
          handleDBConnectionPropertyCalledTwice: true,
          title: 'Install Wizard, Existing MySQL Database'
        },
        {
          wizardControllerName: null,
          serviceConfigValue: null,
          onOptionsChangeCalledTwice: false,
          handleDBConnectionPropertyCalledTwice: false,
          title: 'Service Configs Page'
        }
      ];

    beforeEach(function () {
      sinon.stub(view, 'onOptionsChange', Em.K);
      sinon.stub(view, 'handleDBConnectionProperty', Em.K);
    });

    afterEach(function () {
      view.onOptionsChange.restore();
      view.handleDBConnectionProperty.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        view.set('controller.wizardController.name', item.wizardControllerName);
        view.set('serviceConfig.value', item.serviceConfigValue);
        view.didInsertElement();
        expect(view.onOptionsChange.calledTwice).to.equal(item.onOptionsChangeCalledTwice);
        expect(view.handleDBConnectionProperty.calledTwice).to.equal(item.handleDBConnectionPropertyCalledTwice);
      });
    });

  });

  describe('#databaseNameProperty', function () {

    var view = App.ServiceConfigRadioButtons.create({
        serviceConfig: Em.Object.create(),
        categoryConfigsAll: [
          {
            name: 'ambari.hive.db.schema.name',
            value: 'db0'
          },
          {
            name: 'oozie.db.schema.name',
            value: 'db1'
          },
          {
            name: 'db_name',
            value: 'db2'
          }
        ]
      }),
      cases = [
        {
          serviceName: 'HIVE',
          value: 'db0'
        },
        {
          serviceName: 'OOZIE',
          value: 'db1'
        },
        {
          serviceName: 'RANGER',
          value: 'db2'
        }
      ];

    cases.forEach(function (item) {
      it(item.serviceName, function () {
        view.set('serviceConfig.serviceName', item.serviceName);
        expect(view.get('databaseNameProperty.value')).to.equal(item.value);
        expect(view.get('databaseName')).to.equal(item.value);
      });
    });

    it('default case', function () {
      view.set('serviceConfig.serviceName', 'YARN');
      expect(view.get('databaseNameProperty')).to.be.null;
      expect(view.get('databaseName')).to.be.null;
    });

  });

  describe('#hostNameProperty', function () {

    var view = App.ServiceConfigRadioButtons.create({
        serviceConfig: Em.Object.create(),
        categoryConfigsAll: [
          {
            name: 'hive_ambari_host',
            value: 'h0'
          },
          {
            name: 'hive_existing_mysql_host',
            value: 'h1'
          },
          {
            name: 'hive_existing_postgresql_host',
            value: 'h2'
          },
          {
            name: 'hive_existing_oracle_host',
            value: 'h3'
          },
          {
            name: 'hive_existing_mssql_server_host',
            value: 'h4'
          },
          {
            name: 'hive_existing_mssql_server_2_host',
            value: 'h5'
          },
          {
            name: 'hive_hostname',
            value: 'h6'
          },
          {
            name: 'oozie_ambari_host',
            value: 'h10'
          },
          {
            name: 'oozie_existing_mysql_host',
            value: 'h11'
          },
          {
            name: 'oozie_existing_postgresql_host',
            value: 'h12'
          },
          {
            name: 'oozie_existing_oracle_host',
            value: 'h13'
          },
          {
            name: 'oozie_existing_mssql_server_host',
            value: 'h14'
          },
          {
            name: 'oozie_existing_mssql_server_2_host',
            value: 'h15'
          },
          {
            name: 'oozie_hostname',
            value: 'h16'
          },
          {
            name: 'ranger_mysql_host',
            value: 'h17'
          },
          {
            name: 'ranger_oracle_host',
            value: 'h18'
          },
          {
            name: 'ranger_postgres_host',
            value: 'h19'
          },
          {
            name: 'ranger_mssql_host',
            value: 'h20'
          },


        ]
      }),
      cases = [
        {
          serviceName: 'HIVE',
          value: 'New MySQL Database',
          expected: 'h0'
        },
        {
          serviceName: 'HIVE',
          value: 'Existing MySQL Database',
          expected: 'h1'
        },
        {
          serviceName: 'HIVE',
          value: Em.I18n.t('services.service.config.hive.oozie.postgresql'),
          expected: 'h2'
        },
        {
          serviceName: 'HIVE',
          value: 'Existing Oracle Database',
          expected: 'h3'
        },
        {
          serviceName: 'HIVE',
          value: 'Existing MSSQL Server database with SQL authentication',
          expected: 'h4'
        },
        {
          serviceName: 'HIVE',
          value: 'Existing MSSQL Server database with integrated authentication',
          expected: 'h5'
        },
        {
          serviceName: 'HIVE',
          value: 'default case',
          expected: 'h6'
        },
        {
          serviceName: 'OOZIE',
          value: 'New Derby Database',
          expected: 'h10'
        },
        {
          serviceName: 'OOZIE',
          value: 'Existing MySQL Database',
          expected: 'h11'
        },
        {
          serviceName: 'OOZIE',
          value: Em.I18n.t('services.service.config.hive.oozie.postgresql'),
          expected: 'h12'
        },
        {
          serviceName: 'OOZIE',
          value: 'Existing Oracle Database',
          expected: 'h13'
        },
        {
          serviceName: 'OOZIE',
          value: 'Existing MSSQL Server database with SQL authentication',
          expected: 'h14'
        },
        {
          serviceName: 'OOZIE',
          value: 'Existing MSSQL Server database with integrated authentication',
          expected: 'h15'
        },
        {
          serviceName: 'OOZIE',
          value: 'default case',
          expected: 'h16'
        },
        {
          serviceName: 'RANGER',
          value: 'MYSQL',
          expected: 'h17'
        },
        {
          serviceName: 'RANGER',
          value: 'ORACLE',
          expected: 'h18'
        },
        {
          serviceName: 'RANGER',
          value: 'POSTGRES',
          expected: 'h19'
        },
        {
          serviceName: 'RANGER',
          value: 'MSSQL',
          expected: 'h20'
        }
      ];

    before(function () {
      sinon.stub(view, 'handleDBConnectionProperty', Em.K);
    });

    after(function () {
      view.handleDBConnectionProperty.restore();
    });

    cases.forEach(function (item) {
      it(item.serviceName + ', ' + item.value, function () {
        view.get('serviceConfig').setProperties({
          serviceName: item.serviceName,
          value: item.value
        });
        expect(view.get('hostNameProperty.value')).to.equal(item.expected);
        expect(view.get('hostName')).to.equal(item.expected);
      });
    });

  });

  describe('#onOptionsChange', function () {

    var view = App.ServiceConfigRadioButtons.create({
        hostName: null,
        databaseName: null,
        hostNameProperty: null,
        databaseNameProperty: null,
        connectionUrl: Em.Object.create(),
        dbClass: Em.Object.create(),
        serviceConfig: Em.Object.create(),
        categoryConfigsAll: [
          Em.Object.create({
            name: 'javax.jdo.option.ConnectionUserName'
          }),
          Em.Object.create({
            name: 'javax.jdo.option.ConnectionPassword'
          }),
          Em.Object.create({
            name: 'oozie.service.JPAService.jdbc.username'
          }),
          Em.Object.create({
            name: 'oozie.service.JPAService.jdbc.password'
          }),
          Em.Object.create({
            name: 'db_host'
          }),
          Em.Object.create({
            name: 'SQL_COMMAND_INVOKER'
          })
        ],
        parentView: Em.Object.create({
          serviceConfigs: [
            {
              name: 'hive_database_type',
              value: null
            },
            Em.Object.create({
              name: 'SQL_CONNECTOR_JAR',
              value: null
            })
          ]
        }),
        configs: [{}]
      }),
      cases = [
        {
          serviceName: 'HIVE',
          serviceConfigValue: 'New MySQL Database',
          databaseName: 'db0',
          hostName: 'h0',
          databaseNameDefault: 'db0d',
          hostNameDefault: 'h0d',
          connectionUrlValue: 'jdbc:mysql://h0/db0?createDatabaseIfNotExist=true',
          connectionUrlDefaultValue: 'jdbc:mysql://h0d/db0d?createDatabaseIfNotExist=true',
          dbClassValue: 'com.mysql.jdbc.Driver',
          isAuthVisibleAndRequired: true,
          hiveDbTypeValue: 'mysql'
        },
        {
          serviceName: 'HIVE',
          serviceConfigValue: Em.I18n.t('services.service.config.hive.oozie.postgresql'),
          databaseName: 'db1',
          hostName: 'h1',
          databaseNameDefault: 'db1d',
          hostNameDefault: 'h1d',
          connectionUrlValue: 'jdbc:postgresql://h1:5432/db1',
          connectionUrlDefaultValue: 'jdbc:postgresql://h1d:5432/db1d',
          dbClassValue: 'org.postgresql.Driver',
          isAuthVisibleAndRequired: true,
          hiveDbTypeValue: 'postgres'
        },
        {
          serviceName: 'HIVE',
          serviceConfigValue: 'Existing MySQL Database',
          databaseName: 'db2',
          hostName: 'h2',
          databaseNameDefault: 'db2d',
          hostNameDefault: 'h2d',
          connectionUrlValue: 'jdbc:mysql://h2/db2?createDatabaseIfNotExist=true',
          connectionUrlDefaultValue: 'jdbc:mysql://h2d/db2d?createDatabaseIfNotExist=true',
          dbClassValue: 'com.mysql.jdbc.Driver',
          isAuthVisibleAndRequired: true,
          hiveDbTypeValue: 'mysql'
        },
        {
          serviceName: 'HIVE',
          serviceConfigValue: 'Existing MSSQL Server database with SQL authentication',
          databaseName: 'db3',
          hostName: 'h3',
          databaseNameDefault: 'db3d',
          hostNameDefault: 'h3d',
          connectionUrlValue: 'jdbc:sqlserver://h3;databaseName=db3',
          connectionUrlDefaultValue: 'jdbc:sqlserver://h3d;databaseName=db3d',
          dbClassValue: 'com.microsoft.sqlserver.jdbc.SQLServerDriver',
          isAuthVisibleAndRequired: true,
          hiveDbTypeValue: 'mssql'
        },
        {
          serviceName: 'HIVE',
          serviceConfigValue: 'Existing Oracle Database',
          databaseName: 'db4',
          hostName: 'h4',
          databaseNameDefault: 'db4d',
          hostNameDefault: 'h4d',
          connectionUrlValue: 'jdbc:oracle:thin:@//h4:1521/db4',
          connectionUrlDefaultValue: 'jdbc:oracle:thin:@//h4d:1521/db4d',
          dbClassValue: 'oracle.jdbc.driver.OracleDriver',
          isAuthVisibleAndRequired: true,
          hiveDbTypeValue: 'oracle'
        },
        {
          serviceName: 'HIVE',
          serviceConfigValue: 'Existing MSSQL Server database with integrated authentication',
          databaseName: 'db5',
          hostName: 'h5',
          databaseNameDefault: 'db5d',
          hostNameDefault: 'h5d',
          connectionUrlValue: 'jdbc:sqlserver://h5;databaseName=db5;integratedSecurity=true',
          connectionUrlDefaultValue: 'jdbc:sqlserver://h5d;databaseName=db5d;integratedSecurity=true',
          dbClassValue: 'com.microsoft.sqlserver.jdbc.SQLServerDriver',
          isAuthVisibleAndRequired: false,
          hiveDbTypeValue: 'mssql'
        },
        {
          serviceName: 'OOZIE',
          serviceConfigValue: 'New Derby Database',
          databaseName: 'db6',
          hostName: 'h6',
          databaseNameDefault: 'db6d',
          hostNameDefault: 'h6d',
          connectionUrlValue: 'jdbc:derby:${oozie.data.dir}/${oozie.db.schema.name}-db;create=true',
          connectionUrlDefaultValue: 'jdbc:derby:${oozie.data.dir}/${oozie.db.schema.name}-db;create=true',
          dbClassValue: 'org.apache.derby.jdbc.EmbeddedDriver',
          isAuthVisibleAndRequired: true
        },
        {
          serviceName: 'OOZIE',
          serviceConfigValue: 'Existing MySQL Database',
          databaseName: 'db7',
          hostName: 'h7',
          databaseNameDefault: 'db7d',
          hostNameDefault: 'h7d',
          connectionUrlValue: 'jdbc:mysql://h7/db7',
          connectionUrlDefaultValue: 'jdbc:mysql://h7/db7',
          dbClassValue: 'com.mysql.jdbc.Driver',
          isAuthVisibleAndRequired: true
        },
        {
          serviceName: 'OOZIE',
          serviceConfigValue: Em.I18n.t('services.service.config.hive.oozie.postgresql'),
          databaseName: 'db8',
          hostName: 'h8',
          databaseNameDefault: 'db8d',
          hostNameDefault: 'h8d',
          connectionUrlValue: 'jdbc:postgresql://h8:5432/db8',
          connectionUrlDefaultValue: 'jdbc:postgresql://h8:5432/db8',
          dbClassValue: 'org.postgresql.Driver',
          isAuthVisibleAndRequired: true
        },
        {
          serviceName: 'OOZIE',
          serviceConfigValue: 'Existing MSSQL Server database with SQL authentication',
          databaseName: 'db9',
          hostName: 'h9',
          databaseNameDefault: 'db9d',
          hostNameDefault: 'h9d',
          connectionUrlValue: 'jdbc:sqlserver://h9;databaseName=db9',
          connectionUrlDefaultValue: 'jdbc:sqlserver://h9;databaseName=db9',
          dbClassValue: 'com.microsoft.sqlserver.jdbc.SQLServerDriver',
          isAuthVisibleAndRequired: true
        },
        {
          serviceName: 'OOZIE',
          serviceConfigValue: 'Existing Oracle Database',
          databaseName: 'db10',
          hostName: 'h10',
          databaseNameDefault: 'db10d',
          hostNameDefault: 'h10d',
          connectionUrlValue: 'jdbc:oracle:thin:@//h10:1521/db10',
          connectionUrlDefaultValue: 'jdbc:oracle:thin:@//h10:1521/db10',
          dbClassValue: 'oracle.jdbc.driver.OracleDriver',
          isAuthVisibleAndRequired: true
        },
        {
          serviceName: 'OOZIE',
          serviceConfigValue: 'Existing MSSQL Server database with integrated authentication',
          databaseName: 'db11',
          hostName: 'h11',
          databaseNameDefault: 'db11d',
          hostNameDefault: 'h11d',
          connectionUrlValue: 'jdbc:sqlserver://h11;databaseName=db11;integratedSecurity=true',
          connectionUrlDefaultValue: 'jdbc:sqlserver://h11;databaseName=db11;integratedSecurity=true',
          dbClassValue: 'com.microsoft.sqlserver.jdbc.SQLServerDriver',
          isAuthVisibleAndRequired: false
        },


        {
          serviceName: 'RANGER',
          serviceConfigValue: 'MYSQL',
          databaseName: 'db12',
          hostName: 'h12',
          databaseNameDefault: 'db12d',
          hostNameDefault: 'h12d',
          connectionUrlValue: 'jdbc:mysql://h12/db12',
          connectionUrlDefaultValue: 'jdbc:mysql://h12/db12',
          dbClassValue: 'com.mysql.jdbc.Driver',
          isAuthVisibleAndRequired: true
        },
        {
          serviceName: 'RANGER',
          serviceConfigValue: 'ORACLE',
          databaseName: 'db13',
          hostName: 'h13',
          databaseNameDefault: 'db13d',
          hostNameDefault: 'h13d',
          connectionUrlValue: 'jdbc:oracle:thin:@//h13:1521/db13',
          connectionUrlDefaultValue: 'jdbc:oracle:thin:@//h13:1521/db13',
          dbClassValue: 'oracle.jdbc.driver.OracleDriver',
          isAuthVisibleAndRequired: true
        },
        {
          serviceName: 'RANGER',
          serviceConfigValue: 'POSTGRES',
          databaseName: 'db14',
          hostName: 'h14',
          databaseNameDefault: 'db14d',
          hostNameDefault: 'h14d',
          connectionUrlValue: 'jdbc:postgresql://h14:5432/db14',
          connectionUrlDefaultValue: 'jdbc:postgresql://h14:5432/db14',
          dbClassValue: 'org.postgresql.Driver',
          isAuthVisibleAndRequired: true
        },
        {
          serviceName: 'RANGER',
          serviceConfigValue: 'MSSQL',
          databaseName: 'db15',
          hostName: 'h15',
          databaseNameDefault: 'db15d',
          hostNameDefault: 'h15d',
          connectionUrlValue: 'jdbc:sqlserver://h15;databaseName=db15',
          connectionUrlDefaultValue: 'jdbc:sqlserver://h15;databaseName=db15',
          dbClassValue: 'com.microsoft.sqlserver.jdbc.SQLServerDriver',
          isAuthVisibleAndRequired: true
        }
      ],
      serviceAuthPropsMap = {
        HIVE: ['javax.jdo.option.ConnectionUserName', 'javax.jdo.option.ConnectionPassword'],
        OOZIE: ['oozie.service.JPAService.jdbc.username', 'oozie.service.JPAService.jdbc.password'],
        RANGER: []
      };

    beforeEach(function () {
      sinon.stub(view, 'handleDBConnectionProperty', Em.K);
      sinon.stub(App.Service, 'find').returns([
        {
          serviceName: 'HIVE'
        }
      ]);
    });

    afterEach(function () {
      view.handleDBConnectionProperty.restore();
      App.Service.find.restore();
    });

    cases.forEach(function (item) {
      it(item.serviceName + ', ' + item.serviceConfigValue, function () {
        view.get('serviceConfig').setProperties({
          serviceName: item.serviceName,
          value: item.serviceConfigValue
        });
        view.setProperties({
          databaseName: item.databaseName,
          hostName: item.hostName,
          databaseNameProperty: Em.Object.create({
            recommendedValue: item.databaseNameDefault
          }),
          hostNameProperty: Em.Object.create({
            recommendedValue: item.hostNameDefault
          })
        });
        expect(view.get('connectionUrl.value')).to.equal(item.connectionUrlValue);
        expect(view.get('connectionUrl.recommendedValue')).to.equal(item.connectionUrlDefaultValue);
        expect(view.get('dbClass.value')).to.equal(item.dbClassValue);
        serviceAuthPropsMap[item.serviceName].forEach(function (propName) {
          expect(view.get('categoryConfigsAll').findProperty('name', propName).get('isVisible')).to.equal(item.isAuthVisibleAndRequired);
          expect(view.get('categoryConfigsAll').findProperty('name', propName).get('isRequired')).to.equal(item.isAuthVisibleAndRequired);
        });
        if (item.serviceName == 'HIVE') {
          expect(view.get('parentView.serviceConfigs').findProperty('name', 'hive_database_type').value).to.equal(item.hiveDbTypeValue);
        }
      });
    });

  });

  describe('#handleDBConnectionProperty', function () {

    var view,
      cases = [
        {
          dbType: 'mysql',
          serviceConfig: {
            name: 'hive_database',
            value: 'New MySQL Database',
            serviceName: 'HIVE'
          },
          categoryConfigsAll: [
            Em.Object.create({
              name: 'javax.jdo.option.ConnectionURL',
              displayName: 'Database URL'
            }),
            Em.Object.create({
              name: 'hive_database',
              displayName: 'Hive Database'
            })
          ],
          currentStackVersion: 'HDP-2.2',
          propertyAppendTo1: 'javax.jdo.option.ConnectionURL',
          propertyAppendTo2: 'hive_database',
          isAdditionalView1Null: true,
          isAdditionalView2Null: true,
          title: 'Hive, embedded database'
        },
        {
          dbType: 'postgres',
          serviceConfig: {
            name: 'hive_database',
            value: 'Existing PostgreSQL Database',
            serviceName: 'HIVE'
          },
          categoryConfigsAll: [
            Em.Object.create({
              name: 'javax.jdo.option.ConnectionURL',
              displayName: 'Database URL'
            }),
            Em.Object.create({
              name: 'hive_database',
              displayName: 'Hive Database'
            })
          ],
          currentStackVersion: 'HDP-2.2',
          propertyAppendTo1: 'javax.jdo.option.ConnectionURL',
          propertyAppendTo2: 'hive_database',
          isAdditionalView1Null: false,
          isAdditionalView2Null: false,
          title: 'Hive, external database'
        },
        {
          dbType: 'derby',
          serviceConfig: {
            name: 'oozie_database',
            value: 'New Derby Database',
            serviceName: 'OOZIE'
          },
          categoryConfigsAll: [
            Em.Object.create({
              name: 'oozie.service.JPAService.jdbc.url',
              displayName: 'Database URL'
            }),
            Em.Object.create({
              name: 'oozie_database',
              displayName: 'Oozie Database'
            })
          ],
          currentStackVersion: 'HDP-2.2',
          propertyAppendTo1: 'oozie.service.JPAService.jdbc.url',
          propertyAppendTo2: 'oozie_database',
          isAdditionalView1Null: true,
          isAdditionalView2Null: true,
          title: 'Oozie, embedded database'
        },
        {
          dbType: 'oracle',
          serviceConfig: {
            name: 'oozie_database',
            value: 'Existing Oracle Database',
            serviceName: 'OOZIE'
          },
          categoryConfigsAll: [
            Em.Object.create({
              name: 'oozie.service.JPAService.jdbc.url',
              displayName: 'Database URL'
            }),
            Em.Object.create({
              name: 'oozie_database',
              displayName: 'Oozie Database'
            })
          ],
          currentStackVersion: 'HDP-2.2',
          propertyAppendTo1: 'oozie.service.JPAService.jdbc.url',
          propertyAppendTo2: 'oozie_database',
          isAdditionalView1Null: false,
          isAdditionalView2Null: false,
          title: 'Oozie, external database'
        },
        {
          dbType: 'mysql',
          serviceConfig: {
            name: 'DB_FLAVOR',
            value: 'MYSQL',
            serviceName: 'RANGER'
          },
          categoryConfigsAll: [
            Em.Object.create({
              name: 'ranger.jpa.jdbc.url'
            }),
            Em.Object.create({
              name: 'DB_FLAVOR'
            })
          ],
          currentStackVersion: 'HDP-2.2',
          propertyAppendTo1: 'ranger.jpa.jdbc.url',
          propertyAppendTo2: 'DB_FLAVOR',
          isAdditionalView1Null: true,
          isAdditionalView2Null: true,
          title: 'Ranger, HDP 2.2, external database'
        },
        {
          dbType: 'mssql',
          serviceConfig: {
            name: 'DB_FLAVOR',
            value: 'MSSQL',
            serviceName: 'RANGER'
          },
          categoryConfigsAll: [
            Em.Object.create({
              name: 'ranger.jpa.jdbc.url'
            }),
            Em.Object.create({
              name: 'DB_FLAVOR'
            })
          ],
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

    beforeEach(function () {
      view = App.ServiceConfigRadioButtons.create();
    });

    afterEach(function () {
      App.get.restore();
    });

    after(function () {
      Em.run.next.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        sinon.stub(App, 'get').withArgs('currentStackName').returns('HDP').withArgs('currentStackVersion').returns(item.currentStackVersion);
        view.setProperties({
          categoryConfigsAll: item.categoryConfigsAll,
          serviceConfig: item.serviceConfig
        });
        var additionalView1 = view.get('categoryConfigsAll').findProperty('name', item.propertyAppendTo1).get('additionalView'),
          additionalView2 = view.get('categoryConfigsAll').findProperty('name', item.propertyAppendTo2).get('additionalView');
        expect(Em.isNone(additionalView1)).to.equal(item.isAdditionalView1Null);
        expect(Em.isNone(additionalView2)).to.equal(item.isAdditionalView2Null);
        if (!item.isAdditionalView2Null) {
          expect(additionalView2.create().get('message')).to.equal(Em.I18n.t('services.service.config.database.msg.jdbcSetup').format(item.dbType, item.dbType));
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
          name: 'oozie_ambari_host',
          value: 'h0'
        }),
        Em.Object.create({
          name: 'kdc_host',
          value: 'h1'
        }),
        Em.Object.create({
          name: 'hive_ambari_host',
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

  var view;

  beforeEach(function () {
    view = App.BaseUrlTextField.create({
      repository: Em.Object.create({
        baseUrl: 'val'
      })
    });
    view.didInsertElement();
  });

  describe('#valueWasChanged', function () {

    it('should be recalculated after value is changed', function () {
      view.setProperties({
        value: 'val',
        recommendedValue: 'val'
      });
      expect(view.get('valueWasChanged')).to.be.false;
      view.set('value', 'newVal');
      expect(view.get('valueWasChanged')).to.be.true;
    });

  });

  describe('#restoreValue', function () {

    it('should unset value', function () {
      view.setProperties({
        value: 'valNew',
        savedValue: 'val'
      });
      view.restoreValue();
      expect(view.get('value')).to.equal('val');
    });

  });

});
