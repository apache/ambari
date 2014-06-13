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

App.ApplicationAdapter = DS.RESTAdapter.extend({
  namespace: App.getNamespaceUrl(),
  headers: {
   'X-Requested-By': 'ambari'
  }
});

App.FileAdapter = App.ApplicationAdapter.extend({
  pathForType: function() {
    return 'resources/file';
  },
});

App.FileSerializer = DS.RESTSerializer.extend({
  primaryKey:'filePath',
});

App.IsodateTransform = DS.Transform.extend({  
  deserialize: function (serialized) {
    if (serialized) {
      return moment.unix(serialized).toDate();
    }
    return serialized;
  },
  serialize: function (deserialized) {
    if (deserialized) {
      return moment(deserialized).format('X');
    }
    return deserialized;
  }
});

Ember.Handlebars.registerBoundHelper('showDate', function(date,format) {
  return moment(date).format(format)
});

Em.TextField.reopen(Em.I18n.TranslateableAttributes)

//////////////////////////////////
// Templates
//////////////////////////////////

require('translations');

require('templates/application');
require('templates/index');
require('templates/pig/loading');

require('templates/pig');
require('templates/pig/index');
require('templates/pig/scriptList');
require('templates/pig/scriptEdit');
require('templates/pig/scriptEditIndex');
require('templates/pig/scriptResults');
require('templates/pig/scriptResultsNav');
require('templates/pig/job');
require('templates/pig/jobEdit');
require('templates/pig/jobStatus');
require('templates/pig/jobResults');
require('templates/pig/jobResultsOutput');
require('templates/pig/history');
require('templates/pig/udfs');
require('templates/pig/errorLog');

require('templates/pig/util/script-nav');
require('templates/pig/util/alert');
require('templates/pig/util/alert-content');
require('templates/pig/util/pigHelper');
require('templates/pig/modal/confirmdelete');
require('templates/pig/modal/createUdf');
require('templates/pig/modal/modalLayout');
require('templates/pig/modal/createScript');

require('templates/splash');

//////////////////////////////////
// Models
//////////////////////////////////

require('models/pig_script');
require('models/pig_job');
require('models/file');
require('models/udf');

/////////////////////////////////
// Controllers
/////////////////////////////////

require('controllers/pig');
require('controllers/poll');
require('controllers/edit');
require('controllers/pigScriptEdit');
require('controllers/pigScriptList');
require('controllers/pigScriptEditResults');
require('controllers/pigUdfs');
require('controllers/pigHistory');
require('controllers/pigJob');
require('controllers/jobResults');
require('controllers/splash');
require('controllers/errorLog');
require('controllers/util/pigUtilAlert');
require('controllers/modal/pigModal');

/////////////////////////////////
// Views
/////////////////////////////////

require('views/pig');
require('views/pig/scriptList');
require('views/pig/scriptEdit');
require('views/pig/scriptResults');
require('views/pig/scriptResultsNav');
require('views/pig/pigHistory');
require('views/pig/pigUdfs');
require('views/pig/pigJob');
require('views/pig/jobResults');
require('views/pig/modal/pigModal');
require('views/pig/modal/confirmDelete');
require('views/pig/modal/createUdf');
require('views/pig/modal/createScript');
require('views/pig/util/pigUtilAlert');

/////////////////////////////////
// Routes
/////////////////////////////////

require('routes/pig');
require('routes/pigHistory');
require('routes/pigIndex');
require('routes/pigScriptEdit');
require('routes/pigScriptEditIndex');
require('routes/pigScriptEditResults');
require('routes/pigScriptList');
require('routes/pigUdfs');
require('routes/pigJob');
require('routes/jobResults');
require('routes/splash');

/////////////////////////////////
// Router
/////////////////////////////////

require('router');
