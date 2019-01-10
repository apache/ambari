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

describe('App.ReassignMasterWizardStep3View', function () {
  var view;

  beforeEach(function () {
    view = App.ReassignMasterWizardStep3View.create({});
  });

  describe('#jdbcSetupMessage', function () {
    it('should return jdbcSetupMessage false if component_name is not in HIVE_SERVER, HIVE_METASTORE, OOZIE_SERVER', function () {
      view.set('controller', Em.Object.create({
        content: {
          reassign: {
            component_name: 'test'
          }
        }
      }));
      expect(view.get('jdbcSetupMessage')).to.be.equal(false);
    });

    it('should return jdbcSetupMessage false if component_name is OOZIE_SERVER and database type is derby', function () {
      view.set('controller', Em.Object.create({
        content: {
          reassign: {
            component_name: 'OOZIE_SERVER'
          },
          databaseType: 'derby'
        }
      }));
      expect(view.get('jdbcSetupMessage')).to.be.equal(false);
    });

    it('should return formated jdbcSetupMessage if component_name is OOZIE_SERVER and database type is not derby', function () {
      view.set('controller', Em.Object.create({
        content: {
          reassign: {
            component_name: 'OOZIE_SERVER'
          },
          databaseType: 'test'
        }
      }));
      expect(view.get('jdbcSetupMessage')).to.be.equal(
        Em.I18n.t('services.service.config.database.msg.jdbcSetup').format('test', 'test'));
    });

    it('should return formated jdbcSetupMessage if component_name is HIVE_METASTORE', function () {
      view.set('controller', Em.Object.create({
        content: {
          reassign: {
            component_name: 'HIVE_METASTORE'
          },
          databaseType: 'derby'
        }
      }));
      expect(view.get('jdbcSetupMessage')).to.be.equal(
        Em.I18n.t('services.service.config.database.msg.jdbcSetup').format('derby', 'derby'));
    });
  });
});