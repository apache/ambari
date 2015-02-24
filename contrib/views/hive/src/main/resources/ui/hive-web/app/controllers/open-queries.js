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

import Ember from 'ember';
import constants from 'hive/utils/constants';

export default Ember.ArrayController.extend({
  needs: [ constants.namingConventions.databases,
           constants.namingConventions.loadedFiles,
           constants.namingConventions.jobResults,
           constants.namingConventions.jobExplain,
           constants.namingConventions.columns,
           constants.namingConventions.index,
           constants.namingConventions.settings
         ],

  databases: Ember.computed.alias('controllers.' + constants.namingConventions.databases),

  files: Ember.computed.alias('controllers.' + constants.namingConventions.loadedFiles),

  jobResults: Ember.computed.alias('controllers.' + constants.namingConventions.jobResults),

  jobExplain: Ember.computed.alias('controllers.' + constants.namingConventions.jobExplain),

  index: Ember.computed.alias('controllers.' + constants.namingConventions.index),

  settings: Ember.computed.alias('controllers.' + constants.namingConventions.settings),

  init: function () {
    this._super();

    this.set('queryTabs', Ember.ArrayProxy.create({ content: Ember.A([])}));
  },

  pushObject: function (queryFile, model) {
    return this._super(queryFile || Ember.Object.create({
      id: model.get('id'),
      fileContent: ""
    }));
  },

  getTabForModel: function (model) {
    return this.get('queryTabs').find(function (tab) {
      return tab.id === model.get('id') && tab.type === model.get('constructor.typeKey');
    });
  },

  updateTabSubroute: function (model, path) {
    var tab = this.get('queryTabs').find(function (tab) {
      return tab.id === model.get('id') && tab.type === model.get('constructor.typeKey');
    });

    if (tab) {
      tab.set('subroute', path);
    }
  },

  getQueryForModel: function (model) {
    return this.find(function (openQuery) {
      if (model.get('isNew')) {
        return openQuery.get('id') === model.get('id');
      }

      return openQuery.get('id') === model.get('queryFile');
    });
  },

  update: function (model) {
    var path,
        type,
        currentQuery,
        defer = Ember.RSVP.defer(),
        existentTab,
        self = this,
        updateSubroute = function () {
          var isExplainedQuery,
              subroute;

          if (model.get('constructor.typeKey') === constants.namingConventions.job) {
            isExplainedQuery = self.get('currentQuery.fileContent').indexOf(constants.namingConventions.explainPrefix) > -1;

            if (isExplainedQuery) {
              subroute = constants.namingConventions.subroutes.jobExplain;
            } else {
              subroute = constants.namingConventions.subroutes.jobLogs;
            }

            if (!existentTab.get('subroute')) {
              self.updateTabSubroute(model, subroute);
            }
          }

          defer.resolve(isExplainedQuery);
        };

    existentTab = this.getTabForModel(model);

    if (!existentTab) {
      type = model.get('constructor.typeKey');
      path = type === constants.namingConventions.job ?
             constants.namingConventions.subroutes.historyQuery :
             constants.namingConventions.subroutes.savedQuery;

      existentTab = this.get('queryTabs').pushObject(Ember.Object.create({
        name: model.get('title'),
        id: model.get('id'),
        visible: true,
        path: path,
        type: type
      }));

      if (model.get('isNew')) {
        this.set('currentQuery', this.pushObject(null, model));

        defer.resolve();
      } else {
        this.get('files').loadFile(model.get('queryFile')).then(function (file) {
          self.set('currentQuery', self.pushObject(file));

          updateSubroute();
        });

        if (model.get('logFile') && !model.get('log')) {
          this.get('files').loadFile(model.get('logFile')).then(function (file) {
            model.set('log', file.get('fileContent'));
          });
        }
      }
    } else {
      currentQuery = this.getQueryForModel(model);
      this.set('currentQuery', currentQuery);

      updateSubroute();
    }

    return defer.promise;
  },

  save: function (model, query) {
    var tab = this.getTabForModel(model),
        self = this,
        wasNew,
        defer = Ember.RSVP.defer(),
        jobModel = model,
        originalId = model.get('id');

    if (!query) {
      query = this.getQueryForModel(model);
    }

    if (model.get('isNew')) {
      wasNew = true;
      model.set('id', null);
    }

    //if current query it's a job, convert it to a savedQuery before saving
    if (model.get('constructor.typeKey') === constants.namingConventions.job) {
      model = this.store.createRecord(constants.namingConventions.savedQuery, {
        dataBase: this.get('databases.selectedDatabase.name'),
        title: model.get('title'),
        queryFile: model.get('queryFile'),
        owner: model.get('owner')
      });
    }

    model.save().then(function (updatedModel) {
      tab.set('name', updatedModel.get('title'));
      jobModel.set('queryId', updatedModel.get('id'));

      var content = self.get('index').prependQuerySettings(query.get('fileContent'));
      //update query tab path with saved model id if its a new record
      if (wasNew) {
        self.get('settings').updateSettingsId(originalId, updatedModel.get('id'));
        tab.set('id', updatedModel.get('id'));

        self.get('files').loadFile(updatedModel.get('queryFile')).then(function (file) {
          file.set('fileContent', content);
          file.save().then(function (updatedFile) {
            self.removeObject(query);
            self.pushObject(updatedFile);
            self.set('currentQuery', updatedFile);

            defer.resolve();
          }, function (err) {
            defer.reject(err);
          });
        }, function (err) {
          defer.reject(err);
        });
      } else {
        query.set('fileContent', content);
        query.save().then(function () {
          self.toggleProperty('tabUpdated');
          defer.resolve();
        }, function (err) {
          defer.reject(err);
        });
      }
    }, function (err) {
      defer.reject(err);
    });

    return defer.promise;
  },

  convertTabToJob: function (model, job) {
    var defer = Ember.RSVP.defer(),
        oldQuery = this.getQueryForModel(model),
        tab = this.getTabForModel(model),
        jobId = job.get('id'),
        self = this;

    tab.set('id', job.get('id'));
    tab.set('type', constants.namingConventions.job);
    tab.set('path', constants.namingConventions.subroutes.historyQuery);

    this.get('files').loadFile(job.get('queryFile')).then(function (file) {
      //replace old model representing file to reflect model update to job
      if (self.keepOriginalQuery(jobId)) {
        file.set('fileContent', oldQuery.get('fileContent'));
      }

      self.removeObject(oldQuery);
      self.pushObject(file);

      defer.resolve();
    }, function (err) {
      defer.reject(err);
    });

    return defer.promise;
  },

  keepOriginalQuery: function (jobId) {
    var selected = this.get('highlightedText');
    var hasQueryParams = this.get('index.queryParams.length');
    var hasSettings = this.get('settings').hasSettings(jobId);

    if ( selected && selected[0] !== "" ||
         hasQueryParams ||
         hasSettings ) {
      return true;
    }

    return false;
  },

  actions: {
    removeQueryTab: function (tab) {
      var self = this,
          defer,
          remainingTabs = this.get('queryTabs').without(tab),
          lastTab = remainingTabs.get('lastObject'),
          closeTab = function () {
            self.set('queryTabs', remainingTabs);

            //remove cached results set
            if (tab.type === constants.namingConventions.job) {
              self.get('jobResults').clearCachedResultsSet(tab.id);
              self.get('jobExplain').clearCachedExplainSet(tab.id);
            }

            if (lastTab.type === constants.namingConventions.job) {
              self.transitionToRoute(constants.namingConventions.subroutes.historyQuery, lastTab.id);
            } else {
              self.transitionToRoute(constants.namingConventions.subroutes.savedQuery, lastTab.id);
            }
          };

      this.store.find(tab.type, tab.id).then(function (model) {
        var query = self.getQueryForModel(model);

        if ((model.get('isNew') && !query.get('fileContent')) ||
            (!model.get('isNew') && !query.get('isDirty'))) {
          closeTab();
        } else {
          defer = Ember.RSVP.defer();
          self.send('openModal',
                    'modal-save',
                     {
                        heading: "modals.save.saveBeforeCloseHeading",
                        text: model.get('title'),
                        defer: defer
                     });

          defer.promise.then(function (text) {
            model.set('title', text);
            self.save(model, query).then(function () {
              closeTab();
            });
          }, function () {
            model.rollback();
            closeTab();
          });
        }
      });
    },

    getColumnsForAutocomplete: function (tableName, callback) {
      this.get('databases').getAllColumns(tableName).then(function () {
        callback();
      });
    }
  }
});
