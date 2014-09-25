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
    'yarn.app.id': 'YARN Application ID'
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
  'tableView.filters.noItems' : 'No sliders to display',

  'configs.add_property': 'Add Property',
  'configs.add_property.invalid_name': 'Config name should consists only of letters, numbers, \'-\', \'_\', \'.\' and first character should be a letter.',
  'configs.add_property.name_exists': 'Config name already exists',
  'configs.enable.metrics': 'Enable Metrics',

  'slider.apps.title': 'Slider Apps',
  'slider.apps.create': 'Create App',
  'slider.apps.unavailable': 'HDFS or YARN services are currently unaccessible',
  'sliderApps.filters.info': '{0} of {1} sliders showing',

  'sliderApp.flex.invalid_counts': 'Instance counts should be integer and >= 0',

  'sliderApp.summary.go_to_nagios': 'Go to Nagios',
  'sliderApp.summary.go_to_ganglia': 'Go to Ganglia',
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
  'wizard.step1.header': 'Available Applications',
  'wizard.step1.description': 'Description',
  'wizard.step1.typeDescription': 'Deploys {0} cluster on YARN.',
  'wizard.step1.nameFormatError': 'App Name should consist only of letters, numbers, \'-\', \'_\' and first character should be a letter.',
  'wizard.step1.nameRepeatError': 'App with entered Name already exists.',
  'wizard.step1.noAppTypesError': 'No Slider Application packages have been installed on this server. Please contact your Ambari server administrator to install Slider Application packages into /var/lib/ambari-server/resources/apps/ folder.',
  'wizard.step2.name': 'Allocate Resources',
  'wizard.step2.header': ' application requires resources to be allocated on the cluster. Provide resource allocation requests for each component of the application below.',
  'wizard.step2.table.instances': 'Number of Instances',
  'wizard.step2.table.memory': 'YARN Memory (MB)',
  'wizard.step2.table.cpu': 'YARN	CPU	Cores',
  'wizard.step2.error.numbers': 'All fields should be filled. Only integer numbers allowed.',
  'wizard.step3.name': 'Configuration',
  'wizard.step3.header.beginning': 'Provide	configuration	details	for	',
  'wizard.step3.header.end': '	application',
  'wizard.step3.error': 'Only \"key\":\"value\" format allowed.',
  'wizard.step4.name': 'Deploy',
  'wizard.step4.appName': 'App Name',
  'wizard.step4.appType': 'App Type',

  'ajax.errorMessage': 'Error message',
  'ajax.apiInfo': 'received on {0} method for API: {1}',
  'ajax.statusCode': '{0} status code'
};
