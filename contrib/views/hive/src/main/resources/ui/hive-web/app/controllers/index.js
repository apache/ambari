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
import utils from 'hive/utils/functions';

export default Ember.Controller.extend({
  needs: [ constants.namingConventions.openQueries,
           constants.namingConventions.databases,
           constants.namingConventions.udfs,
           constants.namingConventions.jobLogs,
           constants.namingConventions.jobResults,
           constants.namingConventions.jobExplain,
           constants.namingConventions.settings,
           constants.namingConventions.visualExplain,
           constants.namingConventions.tezUI,
           constants.namingConventions.jobProgress,
  ],

  openQueries: Ember.computed.alias('controllers.' + constants.namingConventions.openQueries),
  databases: Ember.computed.alias('controllers.' + constants.namingConventions.databases),
  udfs: Ember.computed.alias('controllers.' + constants.namingConventions.udfs + '.udfs'),
  logs: Ember.computed.alias('controllers.' + constants.namingConventions.jobLogs),
  results: Ember.computed.alias('controllers.' + constants.namingConventions.jobResults),
  explain: Ember.computed.alias('controllers.' + constants.namingConventions.jobExplain),
  settings: Ember.computed.alias('controllers.' + constants.namingConventions.settings),
  visualExplain: Ember.computed.alias('controllers.' + constants.namingConventions.visualExplain),
  tezUI: Ember.computed.alias('controllers.' + constants.namingConventions.tezUI),
  jobProgress: Ember.computed.alias('controllers.' + constants.namingConventions.jobProgress),

  isDatabaseExplorerVisible: true,

  init: function () {
    this._super();

    // initialize queryParams with an empty array
    this.set('queryParams', Ember.ArrayProxy.create({ content: Ember.A([]) }));

    this.set('queryProcessTabs', Ember.ArrayProxy.create({ content: Ember.A([
      Ember.Object.create({
        name: Ember.I18n.t('menus.logs'),
        path: constants.namingConventions.subroutes.jobLogs
      }),
      Ember.Object.create({
        name: Ember.I18n.t('menus.results'),
        path: constants.namingConventions.subroutes.jobResults
      }),
      Ember.Object.create({
        name: Ember.I18n.t('menus.explain'),
        path: constants.namingConventions.subroutes.jobExplain
      })
    ])}));

    this.set('queryPanelActions', Ember.ArrayProxy.create({ content: Ember.A([
      Ember.Object.create({
        icon: 'fa-expand',
        action: 'toggleDatabaseExplorerVisibility',
        tooltip: Ember.I18n.t('tooltips.expand')
      })
    ])}));
  },

  canExecute: function () {
    var isModelRunning = this.get('model.isRunning');
    var hasParams = this.get('queryParams.length');

    if (isModelRunning) {
      return false;
    }

    if (hasParams) {
      // all param have values?
      return this.get('queryParams').every(function (param) { return param.value; });
    }

    return true;
  }.property('model.isRunning', 'queryParams.@each.value'),

  parseQueryParams: function () {
    var query = this.get('openQueries.currentQuery.fileContent'),
        param,
        updatedParams = [],
        currentParams = this.get('queryParams'),
        paramRegExp = /\$\w+/ig,
        paramNames = query.match(paramRegExp) || [];

    paramNames = paramNames.uniq();

    paramNames.forEach(function (name) {
      param = currentParams.findBy('name', name);
      if (param) {
        updatedParams.push(param);
      } else {
        updatedParams.push({ name: name, value: "" });
      }
    });

    currentParams.setObjects(updatedParams);
  }.observes('openQueries.currentQuery.fileContent'),

  _executeQuery: function (referrer, shouldExplain, shouldGetVisualExplain) {
    var queryId,
        query,
        finalQuery,
        job,
        defer = Ember.RSVP.defer(),
        originalModel = this.get('model');

    job = this.store.createRecord(constants.namingConventions.job, {
      title: originalModel.get('title'),
      sessionTag: originalModel.get('sessionTag'),
      dataBase: this.get('databases.selectedDatabase.name'),
      referrer: referrer
    });

    originalModel.set('isRunning', true);

     //if it's a saved query / history entry set the queryId
    if (!originalModel.get('isNew')) {
      queryId = originalModel.get('constructor.typeKey') === constants.namingConventions.job ?
                originalModel.get('queryId') :
                originalModel.get('id');

      job.set('queryId', queryId);
    }

    query = this.get('openQueries').getQueryForModel(originalModel);

    query = this.buildQuery(query, shouldExplain, shouldGetVisualExplain);

    // for now we won't support multiple queries
    // buildQuery will return false it multiple queries
    // are selected
    if (!query) {
      originalModel.set('isRunning', false);
      defer.reject({
        responseJSON: {
          message: 'Running multiple queries is not supported.'
        }
      });

      return defer.promise;
    }

    finalQuery = query;
    finalQuery = this.bindQueryParams(finalQuery);
    finalQuery = this.prependQuerySettings(finalQuery);

    job.set('forcedContent', finalQuery);

    if (shouldGetVisualExplain) {
      return this.getVisualExplainJson(job, originalModel);
    }

    return this.saveQuery(job, originalModel);
  },

  getVisualExplainJson: function (job, originalModel) {
    var self = this;
    var defer = Ember.RSVP.defer();

    job.save().then(function () {
      self.get('results').getResultsJson(job).then(function (json) {
        defer.resolve(json);
        originalModel.set('isRunning', undefined);
      }, function (err) {
        defer.reject(err);
        originalModel.set('isRunning', undefined);
      });
    }, function (err) {
      defer.reject(err);
        originalModel.set('isRunning', undefined);
    });

    return defer.promise;
  },

  saveQuery: function (job, originalModel) {
    var defer = Ember.RSVP.defer(),
        self = this,
        openQueries = this.get('openQueries');

    var handleError = function (err) {
      originalModel.set('isRunning', undefined);
      defer.reject(err);
    };

    job.save().then(function () {
      //convert tab for current model since the execution will create a new job, and navigate to the new job route.
      openQueries.convertTabToJob(originalModel, job).then(function () {
        //reset flag on the original model
        originalModel.set('isRunning', undefined);

        defer.resolve(job);
      }, function (err) {
        handleError(err);
      });

      self.get('settings').updateSettingsId(originalModel.get('id'), job.get('id'));
    }, function (err) {
      handleError(err);
    });

    return defer.promise;
  },

  prependQuerySettings: function (query) {
    var validSettings = this.get('settings').getCurrentValidSettings();
    var regex = new RegExp(utils.regexes.setSetting);
    var existingSettings = query.match(regex);

    //clear previously added settings
    if (existingSettings) {
      existingSettings.forEach(function (setting) {
        query = query.replace(setting, '');
      });
    }

    query = query.trim();

    //update with the current settings
    if (validSettings.get('length')) {
      query = '\n' + query;

      validSettings.forEach(function (setting) {
        query = setting + '\n' + query;
      });
    }

    return query;
  },

  buildQuery: function (query, shouldExplain, shouldGetVisualExplain) {
    var selections = this.get('openQueries.highlightedText'),
        isQuerySelected = selections && selections[0] !== "",
        queryContent = query ? query.get('fileContent') : '',
        queryComponents = this.extractComponents(queryContent),
        finalQuery = '',
        queries = null;

    if (isQuerySelected) {
      queryComponents.queryString = selections.join('');
    }

    queries = queryComponents.queryString.split(';');
    queries = queries.map(function (s) {
      return s.trim();
    });
    queries = queries.filter(Boolean);

    // return false if multiple queries are selected
    // @FIXME: Remove this to support multiple queries
    // if (queries.length > 1) {
    //   return false;
    // }

    queries = queries.map(function (query) {
      if (shouldExplain) {
        query = query.replace(/explain|formatted/gi, '').trim();

        if (shouldGetVisualExplain) {
          return constants.namingConventions.explainFormattedPrefix + query;
        } else {
          return constants.namingConventions.explainPrefix + query;
        }
      } else {
        return query.replace(/explain|formatted/gi, '').trim();
      }
    });

    if (queryComponents.files.length) {
      finalQuery += queryComponents.files.join("\n") + "\n\n";
    }

    if (queryComponents.udfs.length) {
      finalQuery += queryComponents.udfs.join("\n") + "\n\n";
    }

    finalQuery += queries.join(";");
    finalQuery += ";";
    return finalQuery;
  },

  bindQueryParams: function (query) {
    var params = this.get('queryParams');

    if (!params.get('length')) {
      return query;
    }

    params.forEach(function (param) {
      query = query.split(param.name).join(param.value);
    });

    return query;
  },

  displayJobTabs: function () {
    return this.get('content.constructor.typeKey') === constants.namingConventions.job &&
           utils.isInteger(this.get('content.id'));
  }.property('content'),

  modelChanged: function () {
    var self = this;
    var content = this.get('content');
    var openQueries = this.get('openQueries');
    var database = this.get('databases').findBy('name', this.get('content.dataBase'));

    if (database) {
      this.set('databases.selectedDatabase', database);
    }

    //update open queries list when current query model changes
    openQueries.update(content).then(function (isExplainedQuery) {
      var newId = content.get('id');
      var tab = openQueries.getTabForModel(content);

      //if not an ATS job
      if (content.get('constructor.typeKey') === constants.namingConventions.job && utils.isInteger(newId)) {
        self.get('queryProcessTabs').forEach(function (queryTab) {
          queryTab.set('id', newId);
        });

        if (isExplainedQuery) {
          self.set('explain.content', content);
        } else {
          self.set('logs.content', content);
          self.set('results.content', content);
        }

        self.setExplainVisibility(isExplainedQuery);

        self.transitionToRoute(tab.get('subroute'));
      }
    });
  }.observes('content'),

  selectedDatabaseChanged: function () {
    this.set('content.dataBase', this.get('databases.selectedDatabase.name'));
  }.observes('databases.selectedDatabase'),

  csvUrl: function () {
    if (this.get('content.constructor.typeKey') !== constants.namingConventions.job) {
      return;
    }

    if (!utils.insensitiveCompare(this.get('content.status'), constants.statuses.succeeded)) {
      return;
    }

    var url = this.container.lookup('adapter:application').buildURL();
    url += '/' + constants.namingConventions.jobs + '/' + this.get('content.id');
    url += '/results/csv';

    return url;
  }.property('content'),

  downloadMenu: function () {
    var items = [];
    var tabs = this.get('queryProcessTabs');
    var isResultsTabVisible = tabs.findBy('path', constants.namingConventions.subroutes.jobResults).get('visible');

    if (utils.insensitiveCompare(this.get('content.status'), constants.statuses.succeeded) && isResultsTabVisible) {
      items.push({
        title: Ember.I18n.t('buttons.saveHdfs'),
        action: 'saveToHDFS'
      });

      if (this.get('csvUrl')) {
        items.push(
          Ember.Object.create({
            title: Ember.I18n.t('buttons.saveCsv'),
            action: 'downloadAsCSV'
          })
        );
      }
    }

    return items.length ? items : null;
  }.property('content.status', 'queryProcessTabs.@each.visible'),

  extractComponents: function (queryString) {
    var components = {};

    var udfRegEx = new RegExp("(" + constants.namingConventions.udfInsertPrefix + ").+", "ig");
    var fileRegEx = new RegExp("(" + constants.namingConventions.fileInsertPrefix + ").+", "ig");

    components.udfs         = queryString.match(udfRegEx) || [];
    components.files        = queryString.match(fileRegEx) || [];
    components.queryString  = queryString.replace(udfRegEx, "").replace(fileRegEx, "").trim();

    return components;
  },

  saveToHDFS: function (path) {
    var job = this.get('content');

    if (!utils.insensitiveCompare(job.get('status'), constants.statuses.succeeded)) {
      return;
    }

    var self = this;

    var file = "/tmp/" + path + ".csv";
    var url = this.container.lookup('adapter:application').buildURL();
    url +=  "/jobs/" + job.get('id') + "/results/csv/saveToHDFS";

    Ember.$.getJSON(url, {
        commence: true,
        file: file
    }).then(function (response) {
      self.pollSaveToHDFS(response);
    }, function (response) {
      self.notify.error(response.responseJSON.message, response.responseJSON.trace);
    });
  },

  pollSaveToHDFS: function (data) {
    var self = this;
    var url = this.container.lookup('adapter:application').buildURL();
    url += "/jobs/" + data.jobId + "/results/csv/saveToHDFS";

    Ember.run.later(function () {
      Ember.$.getJSON(url).then(function (response) {
        if (!utils.insensitiveCompare(response.status, constants.results.statuses.terminated)) {
          self.pollSaveToHDFS(response);
        } else {
          self.set('content.isRunning', false);
        }
      }, function (response) {
        self.notify.error(response.responseJSON.message, response.responseJSON.trace);
      });
    }, 2000);
  },

  setExplainVisibility: function (show) {
    var tabs = this.get('queryProcessTabs');

    tabs.findBy('path', constants.namingConventions.subroutes.jobExplain).set('visible', show);
    tabs.findBy('path', constants.namingConventions.subroutes.jobLogs).set('visible', !show);
    tabs.findBy('path', constants.namingConventions.subroutes.jobResults).set('visible', !show);
  },

  queryProcessTitle: function () {
    return Ember.I18n.t('titles.query.process') + ' (' + Ember.I18n.t('titles.query.status') + this.get('content.status') + ')';
  }.property('content.status'),

  actions: {
    saveToHDFS: function () {
      var self = this,
          defer = Ember.RSVP.defer();

      this.send('openModal', 'modal-save', {
        heading: "modals.download.hdfs",
        text: this.get('content.title') + '_' + this.get('content.id'),
        defer: defer
      });

      defer.promise.then(function (text) {
        self.set('content.isRunning', true);
        self.saveToHDFS(text);
      });
    },

    downloadAsCSV: function () {
      var self = this,
          defer = Ember.RSVP.defer();

      this.send('openModal', 'modal-save', {
        heading: "modals.download.csv",
        text: this.get('content.title'),
        defer: defer
      });

      defer.promise.then(function (text) {
        // download file ...
        var urlString = "%@/?fileName=%@.csv";
        var url = self.get('csvUrl');
        url = urlString.fmt(url, text);
        window.open(url);
      });
    },

    insertUdf: function (item) {
      var query = this.get('openQueries.currentQuery');

      var queryString = query.get('fileContent');

      var newUdf = constants.namingConventions.udfInsertPrefix + item.get('name') + " as '" + item.get('classname') + "';";
      var newFileResource = item.get('fileResource.path');

      if (item.get('fileResource.path')) {
        newFileResource = constants.namingConventions.fileInsertPrefix + item.get('fileResource.path') + ";";
      }

      var components = this.extractComponents(queryString);

      if (!components.files.contains(newFileResource) && newFileResource) {
        components.files.push(newFileResource);
      }

      if (!components.udfs.contains(newUdf)) {
        components.udfs.push(newUdf);
      }

      var updatedContent = components.files.join("\n") + "\n\n";
      updatedContent += components.udfs.join("\n") + "\n\n";
      updatedContent += components.queryString;

      query.set('fileContent', updatedContent);
    },

    addQuery: (function () {
      var idCounter = 0;

      return function (workSheetName) {
        var model = this.store.createRecord(constants.namingConventions.savedQuery, {
          dataBase: this.get('databases.selectedDatabase.name'),
          title: workSheetName ? workSheetName : Ember.I18n.t('titles.query.tab'),
          queryFile: '',
          id: 'fixture_' + idCounter
        });

        if (idCounter && !workSheetName) {
          model.set('title', model.get('title') + ' (' + idCounter + ')');
        }

        idCounter++;

        this.transitionToRoute(constants.namingConventions.subroutes.savedQuery, model);
      };
    }()),

    saveQuery: function () {
      //case 1. Save a new query from a new query tab -> route changes to new id
      //case 2. Save a new query from an existing query tab -> route changes to new id
      //case 3. Save a new query from a job tab -> route doesn't change
      //case 4. Update an existing query tab. -> route doesn't change

      var self = this,
          defer = Ember.RSVP.defer(),
          currentQuery = this.get('openQueries.currentQuery');

      this.set('model.dataBase', this.get('databases.selectedDatabase.name'));

      this.send('openModal', 'modal-save-query', {
        heading: 'modals.save.heading',
        message: 'modals.save.overwrite',
        text: this.get('content.title'),
        content: this.get('content'),
        defer: defer
      });

      defer.promise.then(function (result) {
        currentQuery.set('fileContent', self.prependQuerySettings(currentQuery.get('fileContent')));
        // we need to update the original model
        // because when this is executed
        // it sets the title from the original model
        self.set('model.title', result.get('text'));

        if (result.get('overwrite')) {
          self.get('openQueries').save(self.get('content'), null, true, result.get('text'));
        } else {
          self.get('openQueries').save(self.get('content'), null, false, result.get('text')).then(function (newId) {
            if (self.get('model.constructor.typeKey') !== constants.namingConventions.job) {
              self.transitionToRoute(constants.namingConventions.subroutes.savedQuery, newId);
            }
          });
        }
      });
    },

    executeQuery: function (referrer) {
      var self = this;
      var subroute;

      referrer = referrer || constants.jobReferrer.job;

      this._executeQuery(referrer).then(function (job) {
        if (job.get('status') !== constants.statuses.succeeded) {
          subroute = constants.namingConventions.subroutes.jobLogs;
        } else {
          subroute = constants.namingConventions.subroutes.jobResults;
        }

        self.get('openQueries').updateTabSubroute(job, subroute);

        self.transitionToRoute(constants.namingConventions.subroutes.historyQuery, job.get('id'));
      }, function (err) {
        var errorBody = err.responseJSON.trace ? err.responseJSON.trace : false;
        self.notify.error(err.responseJSON.message, errorBody);
      });
    },

    explainQuery: function () {
      var self = this;

      this._executeQuery(constants.jobReferrer.explain, true).then(function (job) {
        self.get('openQueries').updateTabSubroute(job, constants.namingConventions.subroutes.jobExplain);

        self.transitionToRoute(constants.namingConventions.subroutes.historyQuery, job.get('id'));
      }, function (err) {
        self.notify.error(err.responseJSON.message, err.responseJSON.trace);
      });
    },

    toggleDatabaseExplorerVisibility: function () {
      this.toggleProperty('isDatabaseExplorerVisible');
    }
  }
});
