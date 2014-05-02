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

App.JobResultsView = Em.View.extend({
    templateName: 'pig/jobResults',
    outputView: Em.View.extend({
      classNames: ['panel panel-default panel-results'],
      templateName: 'pig/jobResultsOutput',
      didInsertElement:function () {
          $('#btn-stdout').button({'loadingText':Em.I18n.t('job.job_results.stdout_loading')});
          $('#btn-stderr').button({'loadingText':Em.I18n.t('job.job_results.stderr_loading')});
          $('#btn-exitcode').button({'loadingText':Em.I18n.t('job.job_results.exitcode_loading')});
      },
      actions:{
        getOutput:function (item) {
          var self = this;
          var controller = this.get('controller');
          output = controller.get(item); 
          if (!output) {
            return;
          };
          $(this.get('element')).find('.btn').removeClass('active');
          $('#btn-'+item).button('loading');
          this.set('isLoadingOutput',true);
          output.then(function (result) {
            $('#btn-'+item).button('reset').addClass('active');
            self.set('isLoadingOutput',false);
            self.set('activeOutput',result.fileContent);
          })
        }
      },
      isLoadingOutput:false,
      activeOutput:'Select output.'
    })
});
