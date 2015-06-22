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


Em.I18n.translations = {

  'ok': 'OK',
  'yes': 'Yes',
  'no': 'No',
  'any': 'Any',

  'common' : {
    'add': 'Add',
    'show': 'Show',
    'actions': 'Actions',
    'cancel': 'Cancel',
    'name': "Name",
    'back': "Back",
    'delete': 'Delete',
    'destroy': 'Destroy',
    'value': "Value",
    'next': "Next",
    'quickLinks': "Quick Links",
    'summary': 'Summary',
    'configs': 'Configs',
    'metrics': 'Metrics',
    'confirmation': 'Confirmation',
    'configuration': 'Configuration',
    'finish': 'Finish',
    'components': 'Components',
    'type': 'Type',
    'status': 'Status',
    'started': 'Started',
    'finished': 'Finished',
    'diagnostics': 'Diagnostics',
    'description': 'Description',
    'warning': 'Warning',
    'key': 'Key',
    'remove': 'Remove',
    'send': 'Send',
    'error': 'Error',
    'yarn.app.id': 'YARN Application ID',
    'frequency': 'Frequency',
    'minutes': 'Minutes',
    'seconds': 'Seconds',
    'save': 'Save'
  },

  'form': {
    'placeholder': {
      'optional': '(optional)',
      'step1.name': 'Enter Name (required)',
      'include.file.patterns': '.*log',
      'exclude.file.patterns': '.*zip',
      'frequency': '3600'
    }
  },

  'error.config_is_empty': 'Config <strong>{0}</strong> should not be empty',
  'error.config_is_empty_2': 'Config <strong>{0}</strong> should not be empty, because <strong>{1}</strong> is set to "true"',

  'popup.confirmation.commonHeader': 'Confirmation',
  'question.sure':'Are you sure?',

  'tableView.filters.all': 'All',
  'tableView.filters.filtered': 'Filtered',
  'tableView.filters.clearFilters': 'Clear filters',
  'tableView.filters.paginationInfo': '{0} - {1} of {2}',
  'tableView.filters.clearAllFilters': 'clear filters',
  'tableView.filters.showAll': 'Show All',
  'tableView.filters.clearSelection': 'clear selection All',
  'tableView.filters.noItems' : 'No slider apps to display',

  'configs.add_property': 'Add Property',
  'configs.add_property.invalid_name': 'Config name should consists only of letters, numbers, \'-\', \'_\', \'.\' and first character should be a letter.',
  'configs.add_property.name_exists': 'Config name already exists',
  'configs.enable.metrics': 'Enable Metrics',

  'slider.apps.title': 'Slider Apps',
  'slider.apps.create': 'Create App',
  'slider.apps.unavailable': 'Unable to get list of Slider Apps due to issues below. Possible reasons include incorrect or invalid view parameters. Please contact administrator for setting up proper view parameters and verifying necessary services are working.',
  'slider.apps.undefined.issue': 'Undefined issue',
  'sliderApps.filters.info': '{0} of {1} slider apps showing',
  'slider.apps.no.description.available': 'No Description Available',
  'slider.apps.no.applications.available': 'No Applications Available',

  'sliderApp.flex.invalid_counts': 'Instance counts should be integer and >= 0',
  'sliderApp.flex.message': 'Update the number of desired instances for each component of this application',

  'sliderApp.summary.go_to_nagios': 'Go to Nagios',
  'sliderApp.summary.no.components': 'No components are currently running',

  'sliderApp.alerts.no.status': 'No component statuses are currently available',
  'sliderApp.alerts.OK.timePrefixShort': 'OK',
  'sliderApp.alerts.WARN.timePrefixShort': 'WARN',
  'sliderApp.alerts.CRIT.timePrefixShort': 'CRIT',
  'sliderApp.alerts.MAINT.timePrefixShort': 'MAINT',
  'sliderApp.alerts.UNKNOWN.timePrefixShort': 'UNKNOWN',
  'sliderApp.alerts.OK.timePrefix': 'OK for {0}',
  'sliderApp.alerts.WARN.timePrefix': 'WARN for {0}',
  'sliderApp.alerts.CRIT.timePrefix': 'CRIT for {0}',
  'sliderApp.alerts.MAINT.timePrefix': 'MAINT for {0}',
  'sliderApp.alerts.UNKNOWN.timePrefix': 'UNKNOWN for {0}',
  'sliderApp.alerts.lastCheck': 'Last Checked {0}',
  'sliderApp.alerts.brLastCheck': "\nLast Checked {0}",
  'sliderApp.alerts.occurredOn': 'Occurred on {0}, {1}',

  'sliderApp.destroy.confirm.title': 'Destroy Slider App',
  'sliderApp.destroy.confirm.body': 'Destroying a Slider App could result in data loss if not property performed. Make sure you have backed up data handled by the application.',
  'sliderApp.destroy.confirm.body2': 'Are you sure you want to destroy Slider App <em>{0}</em>?',

  'wizard.name': 'Create App',
  'wizard.step1.name': 'Select Type',
  'wizard.step1.header': 'Select Application',
  'wizard.step1.appTypes': 'Application Types',
  'wizard.step1.description': 'Description',
  'wizard.step1.schedulerOptions.label': 'Scheduler Options',
  'wizard.step1.schedulerOptions.queueName': 'Queue Name',
  'wizard.step1.yarnLabels.label': 'YARN Labels',
  'wizard.step1.yarnLabels.options.anyHost': 'Any Host',
  'wizard.step1.yarnLabels.options.nonLabeledHost': 'Non-Labeled Host',
  'wizard.step1.yarnLabels.options.specifyLabel': 'Specify Label',
  'wizard.step1.logAggregation.label': 'Log Aggregation',
  'wizard.step1.logAggregation.filePatterns.include': 'Include File Patterns',
  'wizard.step1.logAggregation.filePatterns.exclude': 'Exclude File Patterns',
  'wizard.step1.typeDescription': 'Deploys {0} cluster on YARN.',
  'wizard.step1.nameFormatError': 'App Name should consist only of lower case letters, numbers, \'-\', and \'_\'. Also, first character should be a lower case letter.',
  'wizard.step1.nameRepeatError': 'App with entered Name already exists.',
  'wizard.step1.validateAppNameError': 'Application with name \'{0}\' already exists',
  'wizard.step1.noAppTypesError': 'No Slider Application packages have been installed on this server. Please contact your Ambari server administrator to install Slider Application packages into /var/lib/ambari-server/resources/apps/ folder and restart Ambari server.',
  'wizard.step1.frequencyError': 'Frequency value should be numeric',
  'wizard.step1.enable2wayssl': 'Enable Two-Way SSL',
  'wizard.step2.name': 'Allocate Resources',
  'wizard.step2.header': ' application requires resources to be allocated on the cluster. Provide resource allocation requests for each component of the application below.',
  'wizard.step2.table.instances': 'Instances',
  'wizard.step2.table.memory': 'Memory (MB)',
  'wizard.step2.table.cpu': 'CPU	Cores',
  'wizard.step2.table.yarnLabels': 'YARN Labels',
  'wizard.step2.table.popoverCheckbox': 'Check box to enable YARN labels on component',
  'wizard.step2.table.popoverLabel': 'Provide YARN label to make component run on labeled hosts. Empty value would make component run on non-labeled hosts.',
  'wizard.step2.error.numbers': 'All fields should be filled. Only integer numbers allowed.',
  'wizard.step3.name': 'Configuration',
  'wizard.step3.header.beginning': 'Provide	configuration	details	for	',
  'wizard.step3.header.end': '	application',
  'wizard.step3.error': 'Only \"key\":\"value\" format allowed.',
  'wizard.step4.name': 'Deploy',
  'wizard.step4.appName': 'App Name',
  'wizard.step4.appType': 'App Type',
  'wizard.step4.2waysslEnabled': 'Two-Way SSL Enabled',

  'ajax.errorMessage': 'Error message',
  'ajax.apiInfo': 'received on {0} method for API: {1}',
  'ajax.statusCode': '{0} status code'
};
