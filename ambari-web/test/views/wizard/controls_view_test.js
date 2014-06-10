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
require('views/wizard/controls_view');
require('utils/ajax/ajax');
require('router');

describe('views/wizard/control_views', function() {
  describe('App.CheckDBConnectionView', function() {
    var createView = function(serviceName) {
      return App.CheckDBConnectionView.extend({
        parentView: Em.View.create({
          service: Em.Object.create({
            serviceName: serviceName
          }),
          categoryConfigsAll: function() {
            return Em.A(
              require('data/HDP2/global_properties').configProperties.concat(require('data/HDP2/site_properties').configProperties)
                .filterProperty('serviceName', serviceName).map(function(property) { return App.ServiceConfigProperty.create(property)})
            );
          }.property()
        })
      });
    };
    var generateTypeValueProp = function(type, value) {
      return {
        type: type,
        value: value
      }
    };
    var prepareConfigsTesting = function() {
      var view = createView('HIVE').create({ databaseName: 'MySQL'});
      var setConfigProperty = function(name, value) {
        view.get('parentView.categoryConfigsAll').findProperty('name', name).set('value', value);
      };

      setConfigProperty('javax.jdo.option.ConnectionUserName', 'hive_user');
      setConfigProperty('javax.jdo.option.ConnectionPassword', 'hive_pass');
      setConfigProperty('ambari.hive.db.schema.name', 'hive_scheme');
      setConfigProperty('javax.jdo.option.ConnectionURL', 'hive_c_url');
      return view;
    }
    describe('`Oozie` properties checking', function() {
      var view = createView('OOZIE').create();
      describe('#requiredProperties', function() {
        var expectedProperties = ['oozie.db.schema.name','oozie.service.JPAService.jdbc.username','oozie.service.JPAService.jdbc.password','oozie.service.JPAService.jdbc.driver','oozie.service.JPAService.jdbc.url'];
        it('required properties present {0}'.format(expectedProperties.join(',')), function() {
          expect(view.get('requiredProperties')).to.have.members(expectedProperties);
        });
      });
      describe('#hostNameProperty', function() {
        var testMessage = 'property name should be `{0}`';
        var tests = [
          {
            databaseName: 'MySQL',
            e: 'oozie_existing_mysql_host'
          },
          {
            databaseName: 'PostgreSQL',
            e: 'oozie_existing_postgresql_host'
          },
          {
            databaseName: 'Oracle',
            e: 'oozie_existing_oracle_host'
          }
        ];
        tests.forEach(function(test) {
          it(testMessage.format(test.e), function() {
            view.set('databaseName', test.databaseName);
            expect(view.get('hostNameProperty')).to.eql(test.e);
          });
        });
      });
    });

    describe('`Hive` properties checking', function() {
      var view = createView('HIVE').create();
      describe('#requiredProperties', function() {
        var expectedProperties = ['ambari.hive.db.schema.name','javax.jdo.option.ConnectionUserName','javax.jdo.option.ConnectionPassword','javax.jdo.option.ConnectionDriverName','javax.jdo.option.ConnectionURL'];
        it('required properties present {0}'.format(expectedProperties.join(',')), function() {
          expect(view.get('requiredProperties')).to.have.members(expectedProperties);
        });
      });
      describe('#hostNameProperty', function() {
        var testMessage = 'property name should be `{0}`';
        var tests = [
          {
            databaseName: 'MySQL',
            e: 'hive_existing_mysql_host'
          },
          {
            databaseName: 'PostgreSQL',
            e: 'hive_existing_postgresql_host'
          },
          {
            databaseName: 'Oracle',
            e: 'hive_existing_oracle_host'
          }
        ];
        tests.forEach(function(test) {
          it(testMessage.format(test.e), function() {
            view.set('databaseName', test.databaseName);
            expect(view.get('hostNameProperty')).to.eql(test.e);
          });
        }, this);
      });
      describe('#connectionProperties', function() {
        var view = prepareConfigsTesting();
        var tests = [
          generateTypeValueProp('user_name', 'hive_user'),
          generateTypeValueProp('user_passwd', 'hive_pass'),
          generateTypeValueProp('db_connection_url', 'hive_c_url')
        ];
        var testMessage = 'property `{0}` should have `{1}`';
        tests.forEach(function(test) {
          it(testMessage.format(test.value, test.type), function() {
            expect(view.get('connectionProperties')[test.type]).to.eql(test.value);
          });
        });
      });

      describe('#preparedDBProperties', function() {
        var view = prepareConfigsTesting();
        var tests = [
          generateTypeValueProp('javax.jdo.option.ConnectionUserName', 'hive_user'),
          generateTypeValueProp('javax.jdo.option.ConnectionPassword', 'hive_pass'),
          generateTypeValueProp('javax.jdo.option.ConnectionURL', 'hive_c_url')
        ];
        var testMessage = 'property `{1}` should have `{0}`';
        tests.forEach(function(test) {
          it(testMessage.format(test.value, test.type), function() {
            expect(view.get('preparedDBProperties')[test.type]).to.eql(test.value);
          });
        });
      });


    });

    describe('#isBtnDisabled', function() {
      var view = createView('HIVE').create({ databaseName: 'MySQL' });
      var testMessage = 'button should be {0} if `isValidationPassed`/`isConnecting`: {1}/{2}';
      var tests = [
        {
          isValidationPassed: true,
          isConnecting: true,
          e: true
        },
        {
          isValidationPassed: true,
          isConnecting: false,
          e: false
        }
      ];
      tests.forEach(function(test) {
        it(testMessage.format(!!test.e ? 'disabled' : 'enabled', test.isValidationPassed, test.isConnecting), function() {
          view.set('isValidationPassed', test.isValidationPassed);
          view.set('isConnecting', test.isConnecting);
          expect(view.get('isBtnDisabled')).to.be.eql(test.e);
        });
      })
    });

    describe('#connectToDatabase()', function() {
      before(function() {
        sinon.spy(App.ajax, 'send');
      });
      describe('connection request validation', function() {
        var view = createView('HIVE').create({ databaseName: 'MySQL'});
        var setConfigProperty = function(name, value) {
          view.get('parentView.categoryConfigsAll').findProperty('name', name).set('value', value);
        };

        setConfigProperty('javax.jdo.option.ConnectionUserName', 'hive_user');
        setConfigProperty('javax.jdo.option.ConnectionPassword', 'hive_pass');
        setConfigProperty('ambari.hive.db.schema.name', 'hive_scheme');
        setConfigProperty('javax.jdo.option.ConnectionURL', 'hive_c_url');

        it('request should be passed with correct params', function() {
          view.connectToDatabase();
          expect(App.ajax.send.calledOnce).to.be.ok;
        })
      });
      after(function() {
        App.ajax.send.restore();
      })
    });

  });

  describe('App.ServiceConfigRadioButtons', function() {
    var createView = function(serviceName) {
      return App.ServiceConfigRadioButtons.extend({
        categoryConfigsAll: function() {
          return Em.A(
            require('data/HDP2/global_properties').configProperties.concat(require('data/HDP2/site_properties').configProperties)
              .filterProperty('serviceName', serviceName).map(function(property) { return App.ServiceConfigProperty.create(property)})
          );
        }.property()
      });
    };

    var setProperties = function(properties, propertyMap) {
      for (var propertyName in propertyMap) {
        properties.findProperty('name', propertyName).set('value', propertyMap[propertyName]);
      }
    };

    before(function() {
      App.clusterStatus.set('wizardControllerName','installerController');
    });
    describe('#onOptionsChange()', function() {
      var oozieDerby =  {
        serviceConfig: { value: 'New Derby Database' },
        setupProperties: {
          'oozie.db.schema.name': 'derby.oozie.schema',
          'oozie.service.JPAService.jdbc.driver': 'oozie.driver',
          'oozie_ambari_host': 'derby.host.com'
        },
        expectedProperties: [
          {
            path: 'databaseName',
            value: 'derby.oozie.schema'
          },
          {
            path: 'dbClass.name',
            value: 'oozie.service.JPAService.jdbc.driver'
          },
          {
            path: 'dbClass.value',
            value: 'org.apache.derby.jdbc.EmbeddedDriver'
          },
          {
            path: 'hostName',
            value: 'derby.host.com'
          },
          {
            path: 'connectionUrl.name',
            value: 'oozie.service.JPAService.jdbc.url'
          },
          {
            path: 'connectionUrl.value',
            value: 'jdbc:derby:${oozie.data.dir}/${oozie.db.schema.name}-db;create=true'
          }
        ]
      };
      var oozieExistingMysql = {
        serviceConfig: { value: 'Existing MySQL Database' },
        setupProperties: {
          'oozie.db.schema.name': 'mysql.oozie.schema',
          'oozie.service.JPAService.jdbc.driver': 'oozie.driver',
          'oozie_existing_mysql_host': 'mysql.host.com'
        },
        expectedProperties: [
          {
            path: 'databaseName',
            value: 'mysql.oozie.schema'
          },
          {
            path: 'dbClass.name',
            value: 'oozie.service.JPAService.jdbc.driver'
          },
          {
            path: 'dbClass.value',
            value: 'com.mysql.jdbc.Driver'
          },
          {
            path: 'hostName',
            value: 'mysql.host.com'
          },
          {
            path: 'connectionUrl.name',
            value: 'oozie.service.JPAService.jdbc.url'
          },
          {
            path: 'connectionUrl.value',
            value: 'jdbc:mysql://mysql.host.com/mysql.oozie.schema'
          }
        ]
      };
      var oozieExistingPostgresql = {
        serviceConfig: { value: 'Existing PostgreSQL Database' },
        setupProperties: {
          'oozie.db.schema.name': 'postgresql.oozie.schema',
          'oozie.service.JPAService.jdbc.driver': 'oozie.driver',
          'oozie_existing_postgresql_host': 'postgresql.host.com'
        },
        expectedProperties: [
          {
            path: 'databaseName',
            value: 'postgresql.oozie.schema'
          },
          {
            path: 'dbClass.name',
            value: 'oozie.service.JPAService.jdbc.driver'
          },
          {
            path: 'dbClass.value',
            value: 'org.postgresql.Driver'
          },
          {
            path: 'hostName',
            value: 'postgresql.host.com'
          },
          {
            path: 'connectionUrl.name',
            value: 'oozie.service.JPAService.jdbc.url'
          },
          {
            path: 'connectionUrl.value',
            value: 'jdbc:postgresql://postgresql.host.com:5432/postgresql.oozie.schema'
          }
        ]
      };
      var oozieExistingOracle = {
        serviceConfig: { value: 'Existing Oracle Database' },
        setupProperties: {
          'oozie.db.schema.name': 'oracle.oozie.schema',
          'oozie.service.JPAService.jdbc.driver': 'oozie.driver',
          'oozie_existing_oracle_host': 'oracle.host.com'
        },
        expectedProperties: [
          {
            path: 'databaseName',
            value: 'oracle.oozie.schema'
          },
          {
            path: 'dbClass.name',
            value: 'oozie.service.JPAService.jdbc.driver'
          },
          {
            path: 'dbClass.value',
            value: 'oracle.jdbc.driver.OracleDriver'
          },
          {
            path: 'hostName',
            value: 'oracle.host.com'
          },
          {
            path: 'connectionUrl.name',
            value: 'oozie.service.JPAService.jdbc.url'
          },
          {
            path: 'connectionUrl.value',
            value: 'jdbc:oracle:thin:@//oracle.host.com:1521/oracle.oozie.schema'
          }
        ]
      };
      var tests = [
        {
          serviceName: 'OOZIE',
          mockData: [
            oozieDerby,
            oozieExistingMysql,
            oozieExistingPostgresql,
            oozieExistingOracle
          ]
        }
      ];
      tests.forEach(function(test) {
        describe('`{0}` service processing'.format(test.serviceName), function() {
          test.mockData.forEach(function(test) {
            describe('`oozie_database` value "{0}"'.format(test.serviceConfig.value), function() {
              var view = createView('OOZIE').create();
              before(function() {
                var categoryConfigs = view.get('categoryConfigsAll');
                view.reopen({
                  serviceConfig: function() {
                    var property = categoryConfigs.findProperty('name', 'oozie_database');
                    property.set('value', test.serviceConfig.value);
                    return property;
                  }.property()
                });
                setProperties(categoryConfigs, test.setupProperties);
                view.didInsertElement();
              })
              test.expectedProperties.forEach(function(property) {
                it('#{0} should be "{1}"'.format(property.path, property.value), function() {
                  expect(view.get(property.path)).to.eql(property.value);
                });
              });
            });
          });
        })
      });
    });
  });
});
