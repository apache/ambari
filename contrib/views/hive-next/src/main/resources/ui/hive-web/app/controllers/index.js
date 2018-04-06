/* * Licensed to the Apache Software Foundation (ASF) under one
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
  jobService: Ember.inject.service(constants.namingConventions.job),
  jobProgressService: Ember.inject.service(constants.namingConventions.jobProgress),
  databaseService: Ember.inject.service(constants.namingConventions.database),
  notifyService: Ember.inject.service(constants.namingConventions.notify),
  session: Ember.inject.service(constants.namingConventions.session),
  settingsService: Ember.inject.service(constants.namingConventions.settings),

  openQueries   : Ember.inject.controller(constants.namingConventions.openQueries),
  udfs          : Ember.inject.controller(constants.namingConventions.udfs),
  logs          : Ember.inject.controller(constants.namingConventions.jobLogs),
  results       : Ember.inject.controller(constants.namingConventions.jobResults),
  explain       : Ember.inject.controller(constants.namingConventions.jobExplain),
  visualExplain : Ember.inject.controller(constants.namingConventions.visualExplain),
  tezUI         : Ember.inject.controller(constants.namingConventions.tezUI),

  selectedDatabase: Ember.computed.alias('databaseService.selectedDatabase'),
  isDatabaseExplorerVisible: true,
  canKillSession: Ember.computed.and('model.sessionTag', 'model.sessionActive'),
  queryProcessTabs: [
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
  ],

  queryPanelActions: [
    Ember.Object.create({
      icon: 'fa-expand',
      action: 'toggleDatabaseExplorerVisibility',
      tooltip: Ember.I18n.t('tooltips.expand')
    })
  ],

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

  currentQueryObserver: function () {
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

    this.set('visualExplain.shouldChangeGraph', true);
  }.observes('openQueries.currentQuery.fileContent'),

  _executeQuery: function (referrer, shouldExplain, shouldGetVisualExplain) {
    var queryId,
        query,
        finalQuery,
        job,
        defer = Ember.RSVP.defer(),
        originalModel = this.get('model');

    var title = "";
    if(shouldGetVisualExplain){
      title += "Visual Explain "
    }else if(shouldExplain){
      title += "Explain "
    }

    title += originalModel.get('title');
    job = this.store.createRecord(constants.namingConventions.job, {
      title: title,
      sessionTag: originalModel.get('sessionTag'),
      dataBase: this.get('selectedDatabase.name'),
      referrer: referrer
    });

    if (!shouldGetVisualExplain) {
      originalModel.set('isRunning', true);
    }

     //if it's a saved query / history entry set the queryId
    if (!originalModel.get('isNew')) {
      queryId = originalModel.get('constructor.typeKey') === constants.namingConventions.job ?
                originalModel.get('queryId') :
                originalModel.get('id');

      job.set('queryId', queryId);
    }

    query = this.get('openQueries').getQueryForModel(originalModel);

    query = this.buildQuery(query, shouldExplain, shouldGetVisualExplain);


    // Condition for no query.
    if(query === ';') {
      originalModel.set('isEmptyQuery', true);
      originalModel.set('isRunning', false);
      defer.reject({
        message: 'No query to process.'
      });
      return defer.promise;
    }

    // for now we won't support multiple queries
    // buildQuery will return false it multiple queries
    // are selected
    if (!query) {
      originalModel.set('isRunning', false);
      defer.reject({
        message: 'Running multiple queries is not supported.'
      });

      return defer.promise;
    }

    finalQuery = query;
    finalQuery = this.bindQueryParams(finalQuery);
    finalQuery = this.prependGlobalSettings(finalQuery, job);
    job.set('forcedContent', finalQuery);

    if (shouldGetVisualExplain) {
      return this.getVisualExplainJson(job, originalModel);
    }

    return this.createJob(job, originalModel);
  },


  getVisualExplainJson: function (job, originalModel) {
    var self = this;
    var defer = Ember.RSVP.defer();
    var attempt = 3;

    var getResult = function() {
      self.get('results').getResultsJson(job).then(function (json) {
        defer.resolve(json);
      }, function (err) {
        if(err.status === 409 && attempt > 0) {
          attempt--;
          Ember.run.later(self, getResult, 3000); // Retry after 3 seconds
        } else {
          defer.reject(err);
        }
      });
    };

    job.save().then(function () {
      Ember.run.later(self, getResult, 1000);
    }, function (err) {
      defer.reject(err);
    });

    return defer.promise;
  },

  createJob: function (job, originalModel) {
    var defer = Ember.RSVP.defer(),
        self = this,
        openQueries = this.get('openQueries');

    var handleError = function (err) {
      self.set('jobSaveSucceeded');
      originalModel.set('isRunning', undefined);
      defer.reject(err);

      if(err.status == 401) {
          self.send('passwordLDAP', job, originalModel);
      }

    };

    job.save().then(function () {
      //convert tab for current model since the execution will create a new job, and navigate to the new job route.
      openQueries.convertTabToJob(originalModel, job).then(function () {
        self.get('jobProgressService').setupProgress(job);
        self.set('jobSaveSucceeded', true);

        //reset flag on the original model
        originalModel.set('isRunning', undefined);

        defer.resolve(job);
      }, function (err) {
        handleError(err);
      });
    }, function (err) {
      handleError(err);
    });

    return defer.promise;
  },

  prependGlobalSettings: function (query, job) {
    var jobGlobalSettings = job.get('globalSettings');
    var currentGlobalSettings = this.get('settingsService').getSettings();

    // remove old globals
    if (jobGlobalSettings) {
      query.replace(jobGlobalSettings, '');
    }

    job.set('globalSettings', currentGlobalSettings);
    query = currentGlobalSettings + query;

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
    queries = queries.filter(Boolean);

    var queriesLength = queries.length;

    queries = queries.map(function (q, index) {
      var newQuery = q.replace(/explain formatted|explain/gi, '');
      return newQuery;
    });

    var lastQuery = queries[queriesLength - 1];

    if(!Ember.isNone(lastQuery) && shouldExplain) {
      if (shouldGetVisualExplain) {
        lastQuery = constants.namingConventions.explainFormattedPrefix + lastQuery;
      } else {
        lastQuery = constants.namingConventions.explainPrefix + lastQuery;
      }
      queries[queriesLength - 1] = lastQuery;
    }

    if (queryComponents.files.length) {
      finalQuery += queryComponents.files.join("\n") + "\n\n";
    }

    if (queryComponents.udfs.length) {
      finalQuery += queryComponents.udfs.join("\n") + "\n\n";
    }

    finalQuery += queries.join(";");
    if(!finalQuery.trim().endsWith(';')){
      finalQuery = finalQuery.trim() + ";";
    }

    return finalQuery.trim();
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
           utils.isInteger(this.get('content.id')) &&
           this.get('jobSaveSucceeded');
  }.property('content', 'jobSaveSucceeded'),

  databasesOrModelChanged: function () {
    this.get('databaseService').setDatabaseByName(this.get('content.dataBase'));
  }.observes('databaseService.databases', 'content'),

  selectedDatabaseChanged: function () {
    this.set('content.dataBase', this.get('selectedDatabase.name'));
  }.observes('selectedDatabase'),

  modelChanged: function () {
    var self = this;
    var content = this.get('content');
    var openQueries = this.get('openQueries');

    this.set('jobSaveSucceeded', true);

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

    var file = path + ".csv";
    var url = this.container.lookup('adapter:application').buildURL();
    url +=  "/jobs/" + job.get('id') + "/results/csv/saveToHDFS";

    Ember.$.getJSON(url, {
        commence: true,
        file: file
    }).then(function (response) {
      self.pollSaveToHDFS(response);
    }, function (error) {
      self.get('notifyService').error(error);
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
      }, function (error) {
        self.get('notifyService').error(error);
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

  updateSessionStatus: function() {
    this.get('session').updateSessionStatus(this.get('model'));
  }.observes('model', 'model.status'),

  actions: {
    passwordLDAP: function(){
      var job = arguments[0],
            originalModel = arguments[1],
            self = this,
            defer = Ember.RSVP.defer();

        this.send('openModal', 'modal-save', {
          heading: "modals.authenticationLDAP.heading",
          text:"",
          type: "password",
          defer: defer
        });

        defer.promise.then(function (text) {
            // make a post call with the given ldap password.
            var password = text;
            var pathName = window.location.pathname;
            var pathNameArray = pathName.split("/");
            var hiveViewVersion = pathNameArray[3];
            var hiveViewName = pathNameArray[4];
            var ldapAuthURL = "/api/v1/views/HIVE/versions/"+ hiveViewVersion + "/instances/" + hiveViewName + "/jobs/auth";

            $.ajax({
                url: ldapAuthURL,
                type: 'post',
                headers: {'X-Requested-With': 'XMLHttpRequest', 'X-Requested-By': 'ambari'},
                contentType: 'application/json',
                data: JSON.stringify({ "password" : password}),
                success: function( data, textStatus, jQxhr ){

                  self.get('databaseService').getDatabases().then(function (databases) {
                    var selectedDatabase = self.get('databaseService.selectedDatabase.name') || 'default';
                    self.get('databaseService').setDatabaseByName( selectedDatabase);
                    return self.send('executeQuery', 'job', self.get('openQueries.currentQuery.fileContent') );
                  }).catch(function (error) {
                    self.get('notifyService').error( "Error in accessing databases." );
                  });

                },
                error: function( jqXhr, textStatus, errorThrown ){
                    console.log( "LDAP fail: " + errorThrown );
                    self.get('notifyService').error( "Wrong Credentials." );
                }
            });

          });
    },

    stopCurrentJob: function () {
      this.get('jobService').stopJob(this.get('model'));
    },

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
        var urlString = "%@/%@.csv";
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

    filesUploaded: (function(files) {
      var idCounter = 0;
      return function (files) {
        var self=this;
        var name = files[0].name;
        var i = name.indexOf(".");
        var title = name.substr(0, i);
        idCounter++;
        var defer = Ember.RSVP.defer()
        var reader = new FileReader();

        reader.onloadstart = function(e) {
          Ember.$("#uploadProgressModal").modal("show");
        }
        reader.onloadend = function(e) {
          defer.resolve(e.target.result);
        }
        reader.onerror = function(e) {
          self.get('notifyService').error("Upload failed");
          Ember.$("#uploadProgressModal").modal("hide");
        }
        reader.readAsText(files[0]);
        defer.promise.then(function(data) {
        var model = self.store.createRecord(constants.namingConventions.savedQuery, {
          dataBase: self.get('selectedDatabase.name'),
          title: title,
          id: 'fixture_upload' + idCounter
          });
        return Ember.RSVP.resolve(self.transitionToRoute(constants.namingConventions.subroutes.savedQuery, model)).then(function() {
          return data;
        });
        }). then(function(data) {
          self.set('openQueries.currentQuery.fileContent',data);
          Ember.$("#uploadProgressModal").modal("hide");
        });
        };
    }()),


    addQuery: (function () {
      var idCounter = 0;

      return function (workSheetName) {
        var model = this.store.createRecord(constants.namingConventions.savedQuery, {
          dataBase: this.get('selectedDatabase.name'),
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

      this.set('model.dataBase', this.get('selectedDatabase.name'));

      this.send('openModal', 'modal-save-query', {
        heading: 'modals.save.heading',
        message: 'modals.save.overwrite',
        text: this.get('content.title'),
        content: this.get('content'),
        defer: defer
      });

      defer.promise.then(function (result) {
        // we need to update the original model
        // because when this is executed
        // it sets the title from the original model
        self.set('model.title', result.get('text'));

        if (result.get('overwrite')) {
          self.get('openQueries').save(self.get('content'), null, true, result.get('text')).then(function () {
            self.get('notifyService').success(Ember.I18n.t('alerts.success.query.update'));
          });
        } else {
          self.get('openQueries').save(self.get('content'), null, false, result.get('text')).then(function (newId) {
            self.get('notifyService').success(Ember.I18n.t('alerts.success.query.save'));

            if (self.get('model.constructor.typeKey') !== constants.namingConventions.job) {
              self.transitionToRoute(constants.namingConventions.subroutes.savedQuery, newId);
            }
          });
        }
      });
    },

    executeQuery: function (referrer, query) {
      var self = this;

      var isExplainQuery = (self.get('openQueries.currentQuery.fileContent').toUpperCase().trim().indexOf(constants.namingConventions.explainPrefix) === 0);

      if(isExplainQuery){
        self.send('explainQuery');
        return;
      }

      var subroute;

      if (query) {
        this.set('openQueries.currentQuery.fileContent', query);
      }

      referrer = referrer || constants.jobReferrer.job;

      this._executeQuery(referrer).then(function (job) {
        if (job.get('status') !== constants.statuses.succeeded) {
          subroute = constants.namingConventions.subroutes.jobLogs;
        } else {
          subroute = constants.namingConventions.subroutes.jobResults;
        }

        self.get('openQueries').updateTabSubroute(job, subroute);
        self.get('notifyService').success(Ember.I18n.t('alerts.success.query.execution'));
        self.transitionToRoute(constants.namingConventions.subroutes.historyQuery, job.get('id'));
      }, function (error) {
        self.get('notifyService').error(error);
      });
    },

    explainQuery: function () {
      var self = this;

      this._executeQuery(constants.jobReferrer.explain, true).then(function (job) {
        self.get('openQueries').updateTabSubroute(job, constants.namingConventions.subroutes.jobExplain);

        self.transitionToRoute(constants.namingConventions.subroutes.historyQuery, job.get('id'));
      }, function (error) {
        self.get('notifyService').error(error);
      });
    },

    toggleDatabaseExplorerVisibility: function () {
      this.toggleProperty('isDatabaseExplorerVisible');
    },

    killSession: function() {
      var self = this;
      var model = this.get('model');

      this.get('session').killSession(model)
        .catch(function (response) {
          if ([200, 404].contains(response.status)) {
            model.set('sessionActive', false);
            self.notify.success(Ember.I18n.t('alerts.success.sessions.deleted'));
          } else {
            self.notify.error(response);
          }
        });
    }
  }
});
