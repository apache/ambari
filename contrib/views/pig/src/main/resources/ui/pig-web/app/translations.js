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

Ember.I18n.translations = {
  'common':{
    'create': 'Create',
    'add':"Add",
    'edit':"Edit",
    'name':"Name",
    'path':"Path",
    'owner':"Owner",
    'delete':"Delete",
    'created':"Created",
    'created':"Created",
    'history':"History",
    'clone':"Clone",
    'cancel':"Cancel",
    'arguments':"Arguments",
    'errorLog':"Stack Trace",
    'showErrorLog':"Show Stack Trace"
  },
  'scripts':{
    'scripts':"Scripts",
    'newscript': "New Script",
    'title': "Name",
    'path': "Script HDFS Location (optional)",
    'noScripts': "No scripts to display",
    'modal':{
      'create_script':'New Script',
      'script_title_placeholder': 'Script name',
      'file_path_placeholder':'Full path to script file',
      'file_path_hint':'Leave empty to create file automatically.',
      'file_path_hint':'Leave empty to create file automatically.',

      'error_empty_title':'Title can not be empty!',

      'confirm_delete':'Confirm Delete',
      'confirm_delete_massage':'Are you sure you want to delete {{title}} script?'
    },
    'alert':{
      'file_not_found':'File not found',
      'arg_present':'Argument already present',
      'file_exist_error':'File already exist',
      'script_saved':'{{title}} saved!',
      'script_created':'{{title}} created!',
      'script_deleted':'{{title}} deleted!',
      'create_failed':'Failed to create script!',
      'delete_failed':'Delete failed!',
      'save_error':'Error while saving script',
      'save_error_reason':'{{message}}',
    },
  },
  'editor':{
    'title_updated':'Title updated.',
    'pig_argument':'Pig argument',
    'pighelper':'PIG helper',
    'udfhelper':'UDF helper',
    'save':'Save',
    'execute':'Execute',
    'explain':'Explain',
    'syntax_check':'Syntax check'
  },
  'job':{
    'title': "Title",
    'name': "Name",
    'results':'Results',
    'logs':'Logs',
    'job_status':'Job status: ',
    'status':'Status',
    'started':'Date started',
    'noJobs': "No jobs to display",
    'alert':{
      'job_started' :'Job started!',
      'job_killed' :'{{title}} job killed!',
      'job_kill_error' :'Job kill failed!',
      'start_filed' :'Job failed to start!',
      'load_error' :'Error loading job. Reason: {{message}}',
      'stdout_error' :'Error loading STDOUT. \n Status: {{status}} Message: {{message}}',
      'stderr_error' :'Error loading STDERR. \n Status: {{status}} Message: {{message}}',
      'exit_error' :'Error loading EXITCODE. \n Status: {{status}} Message: {{message}}',
    },
    'job_results':{
      'stdout':'Stdout',
      'stderr':'Stderr',
      'exitcode':'Exit code',
      'stdout_loading':'Loading stdout...',
      'stderr_loading':'Loading stderr...',
      'exitcode_loading':'Loading exitcode...',
    },
  },
  'udfs':{
    'udfs':'UDFs',
    'tooltips':{
      'path':'Path of this script file on HDFS',
    },
    'noUDFs': "No UDFs to display",
    'alert':{
      'udf_created':'{{name}} created!',
      'udf_deleted':'{{name}} deleted!',
      'create_failed':'Failed to create UDF!',
      'delete_failed':'Delete failed!',
    },
    'modal':{
      'create_udf':'Create UDF',
      'udf_name':'UDF name',
      'hdfs_path':'HDFS path',
    }
  }
};
