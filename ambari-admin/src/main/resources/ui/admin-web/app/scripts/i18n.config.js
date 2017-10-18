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
'use strict';

angular.module('ambariAdminConsole')
.config(['$translateProvider', function($translateProvider) {
  $translateProvider.translations('en',{
    'CLUSTER.ADMINISTRATOR': 'Operator',
    'CLUSTER.USER': 'Read-Only',
    'VIEW.USER': 'Use',

    'common': {
      'ambari': 'Ambari',
      'apacheAmbari': 'Apache Ambari',
      'about': 'About',
      'version': 'Version',
      'signOut': 'Sign out',
      'register':'Register',
      'clusters': 'Clusters',
      'views': 'Views',
      'viewUrls': 'View URLs',
      'roles': 'Roles',
      'users': 'Users',
      'groups': 'Groups',
      'versions': 'Versions',
      'stack': 'Stack',
      'details': 'Details',
      'goToDashboard': 'Go to Dashboard',
      'noClusters': 'No Clusters',
      'noViews': 'No Views',
      'view': 'View',
      'displayLabel': 'Display label',
      'search': 'Search',
      'name': 'Name',
      'any': 'Any',
      'none': 'None',
      'type': 'Type',
      'add': 'Add {{term}}',
      'delete': 'Delete {{term}}',
      'deregisterCluster': 'Deregister Cluster',
      'cannotDelete': 'Cannot Delete {{term}}',
      'privileges': 'Privileges',
      'cluster': 'Cluster',
      'remoteClusters': 'Remote Clusters',
      'services':'Services',
      'clusterRole': 'Cluster Role',
      'viewPermissions': 'View Permissions',
      'getInvolved': 'Get involved!',
      'license': 'Licensed under the Apache License, Version 2.0',
      'tableFilterMessage': '{{showed}} of {{total}} {{term}} showing',
      'yes': 'Yes',
      'no': 'No',
      'renameCluster': 'Rename Cluster',
      'renameClusterTip': 'Only alpha-numeric characters, up to 80 characters',
      'clusterCreationInProgress': 'Cluster creation in progress...',
      'userGroupManagement': 'User + Group Management',
      'all': 'All',
      'group': 'Group',
      'user': 'User',
      'settings': 'Settings',
      'authentication': 'Authentication',
      'deleteConfirmation': 'Are you sure you want to delete {{instanceType}} {{instanceName}}?',
      'remoteClusterDelConfirmation':'Are you sure you want to delete {{instanceType}} {{instanceName}}? This operation cannot be undone.',
      'messageInstanceAffected':'The following View Instances are using this Remote Cluster for configuration, and will need to be reconfigured: {{viewInstance}}',
      'local': 'Local',
      'pam': 'PAM',
      'ldap': 'LDAP',
      'jwt': 'JWT',
      'warning': 'Warning',
      'filterInfo': '{{showed}} of {{total}} {{term}} showing',
      'usersGroups': 'Users/Groups',
      'enabled': 'Enabled',
      'disabled': 'Disabled',
      'NA': 'n/a',
      'blockViewLabel': 'BLOCK',
      'listViewLabel': 'LIST',
      'rbac': 'Role Based Access Control',
      'important': 'Important',
      'undo': 'Undo',
      'fromGroupMark': '(from group)',
      'hidden' : 'Hidden',

      'clusterNameChangeConfirmation': {
        'title': 'Confirm Cluster Name Change',
        'message': 'Are you sure you want to change the cluster name to {{clusterName}}?'
      },

      'loginActivities': {
        'loginActivities':'Login Activities',
        'loginMessage': 'Login Message',
        'loginMessage.placeholder': 'Please enter login message',
        'buttonText.placeholder': 'Please enter button text',
        'homeDirectory': 'Home Directory',
        'notEmpty': 'These field cannot be empty',
        'saveError': 'Save error',
        'message': 'Message',
        'buttonText': 'Button',
        'status': 'Status',
        'status.disabled': 'Disabled',
        'homeDirectory.alert': 'Many Ambari Views store user preferences in the logged in user\'s / user directory in HDFS. Optionally, Ambari can auto-create these directories for users on login.',
        'homeDirectory.autoCreate': 'Auto-Create HDFS user directories',
        'homeDirectory.header': 'User Directory Creation Options',
        'homeDirectory.template': 'User Directory creation template',
        'homeDirectory.group': 'Default Group',
        'homeDirectory.permissions': 'Permissions'
      },

      'controls': {
        'cancel': 'Cancel',
        'close': 'Close',
        'ok': 'OK',
        'save': 'Save',
        'clearFilters': 'clear filters',
        'confirmChange': 'Confirm Change',
        'discard': 'Discard',
        'remove': 'Remove',
        'update':'Update',
        'checkAll': 'Check All',
        'clearAll': 'Clear All'
      },

      'alerts': {
        'fieldRequired': 'Field required!',
        'fieldIsRequired': 'This field is required.',
        'noSpecialChars': 'Must not contain special characters!',
        'nothingToDisplay': 'No {{term}} to display.',
        'noRemoteClusterDisplay':'No Remote Clusters to display.',
        'noPrivileges': 'No {{term}} privileges',
        'noPrivilegesDescription': 'This {{term}} does not have any privileges.',
        'timeOut': 'You will be automatically logged out in <b>{{time}}</b> seconds due to inactivity.',
        'isInvalid': '{{term}} Invalid.',
        'cannotSavePermissions': 'Cannot save permissions',
        'cannotLoadPrivileges': 'Cannot load privileges',
        'cannotLoadClusterStatus': 'Cannot load cluster status',
        'clusterRenamed': 'The cluster has been renamed to {{clusterName}}.',
        'remoteClusterRegistered': 'The cluster has been registered as {{clusterName}}.',
        'cannotRenameCluster': 'Cannot rename cluster to {{clusterName}}',
        'minimumTwoChars': 'Minimum length is 2 characters.',
        'maxTwentyFiveChars': 'Maximum length is 25 characters.',
        'onlyText': 'Only lowercase alphanumeric characters are allowed.',
        'onlyAnScore': 'Invalid input, only alphanumerics allowed eg: My_default_view',
        'passwordRequired':'Password Required',
        'unsavedChanges': 'You have unsaved changes. Save changes or discard?'
      }
    },

    'main': {
      'title': 'Welcome to Apache Ambari',
      'noClusterDescription': 'Provision a cluster, manage who can access the cluster, and customize views for Ambari users.',
      'hasClusterDescription': 'Monitor your cluster resources, manage who can access the cluster, and customize views for Ambari users.',
      'autoLogOut': 'Automatic Logout',

      'operateCluster': {
        'title': 'Operate Your Cluster',
        'description': 'Manage the configuration of your cluster and monitor the health of your services',
        'manageRoles': 'Manage Roles'
      },

      'createCluster': {
        'title': 'Create a Cluster',
        'description': 'Use the Install Wizard to select services and configure your cluster',
        'launchInstallWizard': 'Launch Install Wizard'
      },

      'manageUsersAndGroups': {
        'title': 'Manage Users + Groups',
        'description': 'Manage the users and groups that can access Ambari'
      },

      'deployViews': {
        'title': 'Deploy Views',
        'description': 'Create view instances and grant permissions'
      },

      'controls': {
        'remainLoggedIn': 'Remain Logged In',
        'logOut': 'Log Out Now'
      }
    },

    'views': {
      'instance': 'Instance',
      'viewInstance': 'View Instance',
      'create': 'Create Instance',
      'createViewInstance': 'Create View Instance',
      'edit': 'Edit',
      'viewName': 'View Name',
      'instances': 'Instances',
      'instanceName': 'Instance Name',
      'instanceId': 'Instance ID',
      'displayName': 'Display Name',
      'settings': 'Settings',
      'advanced': 'Advanced',
      'visible': 'Visible',
      'description': 'Description',
      'shortUrl':'Short URL',
      'instanceDescription': 'Instance Description',
      'clusterConfiguration': 'Cluster Configuration',
      'localCluster': 'Local Cluster',
      'remoteCluster': 'Remote Cluster',
      'registerRemoteCluster' : 'Register Remote Cluster',
      'clusterName': 'Cluster Name',
      'custom': 'Custom',
      'icon': 'Icon',
      'icon64': 'Icon64',
      'permissions': 'Permissions',
      'permission': 'Permission',
      'grantUsers': 'Grant permission to these users',
      'grantGroups': 'Grant permission to these groups',
      'configuration': 'Configuration',
      'goToInstance': 'Go to instance',
      'pending': 'Pending...',
      'deploying': 'Deploying...',
      'properties': 'properties',
      'urlDelete':'Delete URL',

      'clusterPermissions': {
        'label': 'Local Cluster Permissions',
        'clusteradministrator': 'Cluster Administrator',
        'clusteroperator': 'Cluster Operator',
        'clusteruser': 'Cluster User',
        'serviceadministrator': 'Service Administrator',
        'serviceoperator': 'Service Operator',
        'infoMessage': 'Grant <strong>Use</strong> permission for the following <strong>{{cluster}}</strong> Roles:',
        'nonLocalClusterMessage': 'The ability to inherit view <strong>Use</strong> permission based on Cluster Roles is only available when using a Local Cluster configuration.'
      },

      'alerts': {
        'noSpecialChars': 'Must not contain any special characters.',
        'noSpecialCharsOrSpaces': 'Must not contain any special characters or spaces.',
        'instanceExists': 'Instance with this name already exists.',
        'notDefined': 'There are no {{term}} defined for this view.',
        'cannotEditInstance': 'Cannot Edit Static Instances',
        'cannotDeleteStaticInstance': 'Cannot Delete Static Instances',
        'deployError': 'Error deploying. Check Ambari Server log.',
        'unableToCreate': 'Unable to create view instances',
        'cannotUseOption': 'This view cannot use this option',
        'unableToResetErrorMessage': 'Unable to reset error message for prop: {{key}}',
        'instanceCreated': 'Created View Instance {{instanceName}}',
        'unableToParseError': 'Unable to parse error message: {{message}}',
        'cannotCreateInstance': 'Cannot create instance',
        'cannotLoadInstanceInfo': 'Cannot load instance info',
        'cannotLoadPermissions': 'Cannot load permissions',
        'cannotSaveSettings': 'Cannot save settings',
        'cannotSaveProperties': 'Cannot save properties',
        'cannotDeleteInstance': 'Cannot delete instance',
        'cannotLoadViews': 'Cannot load views',
        'cannotLoadViewUrls': 'Cannot load view URLs',
        'cannotLoadViewUrl': 'Cannot load view URL',
        'savedRemoteClusterInformation':'Remote cluster information is saved.',
        'credentialsUpdated':'Credentials Updated.'
      }
    },

    'urls':{
      'name':'Name',
      'url':'URL',
      'viewUrls':'View URLs',
      'createNewUrl':'Create New URL',
      'create':'Create',
      'edit':'Edit',
      'view':'View',
      'viewInstance':'Instance',
      'step1':'Create URL',
      'step2':'Select instance',
      'step3':'Assign URL',
      'noUrlsToDisplay':'No URLs to display.',
      'noViewInstances':'No view instances',
      'none':'None',
      'change':'Change',
      'urlCreated':'Created short URL <a href="{{siteRoot}}#/main/view/{{viewName}}/{{shortUrl}}">{{urlName}}</a>',
      'urlUpdated':'Updated short URL <a href="{{siteRoot}}#/main/view/{{viewName}}/{{shortUrl}}">{{urlName}}</a>'
    },

    'clusters': {
      'switchToList': 'Switch&nbsp;to&nbsp;list&nbsp;view',
      'switchToBlock': 'Switch&nbsp;to&nbsp;block&nbsp;view',
      'role': 'Role',
      'assignRoles': 'Assign roles to these {{term}}',

      'alerts': {
        'cannotLoadClusterData': 'Cannot load cluster data'
      }
    },

    'groups': {
      'createLocal': 'Create Local Group',
      'name': 'Group name',
      'members': 'Members',
      'membersPlural': '{{n}} member{{n == 1 ? "" : "s"}}',

      'alerts': {
        'onlySimpleChars': 'Must contain only simple characters.',
        'groupCreated': 'Created group <a href="#/groups/{{groupName}}/edit">{{groupName}}</a>',
        'groupCreationError': 'Group creation error',
        'cannotUpdateGroupMembers': 'Cannot update group members',
        'getGroupsListError': 'Get groups list error'
      }
    },

    'users': {
      'username': 'Username',
      'userName': 'User name',
      'admin': 'Admin',
      'ambariAdmin': 'Ambari Admin',
      'ambariClusterURL':'Ambari Cluster URL',
      'changePassword': 'Change Password',
      'updateCredentials':'Update Credentials',
      'changePasswordFor': 'Change Password for {{userName}}',
      'yourPassword': 'Your Password',
      'newPassword': 'New User Password',
      'newPasswordConfirmation': 'New User Password Confirmation',
      'create': 'Create Local User',
      'active': 'Active',
      'inactive': 'Inactive',
      'status': 'Status',
      'password': 'Password',
      'passwordConfirmation': 'Password сonfirmation',
      'userIsAdmin': 'This user is an Ambari Admin and has all privileges.',
      'showAll': 'Show all users',
      'showAdmin': 'Show only admin users',
      'groupMembership': 'Group Membership',
      'userNameTip': 'Maximum length is 80 characters. \\, &, |, <, >, ` are not allowed.',

      'changeStatusConfirmation': {
        'title': 'Change Status',
        'message': 'Are you sure you want to change status for user "{{userName}}" to {{status}}?'
      },

      'changePrivilegeConfirmation': {
        'title': 'Change Admin Privilege',
        'message': 'Are you sure you want to {{action}} Admin privilege to user "{{userName}}"?'
      },

      'roles': {
        'clusterUser': 'Cluster User',
        'clusterAdministrator': 'Cluster Administrator',
        'clusterOperator': 'Cluster Operator',
        'serviceAdministrator': 'Service Administrator',
        'serviceOperator': 'Service Operator',
        'ambariAdmin': 'Ambari Administrator',
        'viewUser': 'View User',
        'none': 'None',
        'oneRolePerUserOrGroup': 'Only 1 role allowed per user or group',
        'permissionLevel': '{{level}}-level Permissions'
      },

      'alerts': {
        'passwordRequired': 'Password required',
        'wrongPassword': 'Password must match!',
        'usernameRequired':'Username Required',
        'cannotChange': 'Cannot Change {{term}}',
        'userCreated': 'Created user <a href="#/users/{{encUserName}}">{{userName}}</a>',
        'userCreationError': 'User creation error',
        'removeUserError': 'Removing from group error',
        'cannotAddUser': 'Cannot add user to group',
        'passwordChanged': 'Password changed.',
        'cannotChangePassword': 'Cannot change password',
        'roleChanged': '{{name}} changed to {{role}}',
        'roleChangedToNone': '{{user_name}}\'s explicit privilege has been changed to \'NONE\'. Any privilege now seen for this user comes through its Group(s).',
        'usersEffectivePrivilege': '{{user_name}}\'s effective privilege through its Group(s) is higher than your selected privilege.'
      }
    },

    'versions': {
      'current': 'Current',
      'addVersion': 'Add Version',
      'defaultVersion': '(Default Version Definition)',
      'inUse': 'In Use',
      'installed': 'Installed',
      'usePublic': "Use Public Repository",
      'networkIssues': {
        'networkLost': "Why is this disabled?",
        'publicDisabledHeader': "Public Repository Option Disabled",
        'publicRepoDisabledMsg': 'Ambari does not have access to the Internet and cannot use the Public Repository for installing the software. Your Options:',
        'publicRepoDisabledMsg1': 'Configure your hosts for access to the Internet.',
        'publicRepoDisabledMsg2': 'If you are using an Internet Proxy, refer to the Ambari Documentation on how to configure Ambari to use the Internet Proxy.',
        'publicRepoDisabledMsg3': 'Use the Local Repositoy option.'
      },
      'selectVersion': "Select Version",
      'selectVersionEmpty': "No other repositories",
      'useLocal': "Use Local Repository",
      'uploadFile': 'Upload Version Definition File',
      'enterURL': 'Version Definition File URL',
      'defaultURL': 'https://',
      'readInfo': 'Read Version Info',
      'browse': 'Browse',
      'installOn': 'Install on...',
      'register': {
        'title': 'Register Version',
        'error': {
          'header': 'Unable to Register',
          'body': 'You are attempting to register a version with a Base URL that is already in use with an existing registered version. You *must* review your Base URLs and confirm they are unique for the version you are trying to register.'
        }
      },
      'deregister': 'Deregister Version',
      'deregisterConfirmation': 'Are you sure you want to deregister version <strong>{{versionName}}</strong> ?',
      'placeholder': 'Version Number (0.0)',
      'repos': 'Repositories',
      'os': 'OS',
      'baseURL': 'Base URL',
      'skipValidation': 'Skip Repository Base URL validation (Advanced)',
      'noVersions': 'Select version to display details.',
      'patch': 'Patch',
      'maint': 'Maint',
      'introduction': 'To register a new version in Ambari, provide a Version Definition File, confirm the software repository information and save the version.',
      'contents': {
        'title': 'Contents',
        'empty': 'No contents to display'
      },
      'details': {
        'stackName': 'Stack Name',
        'displayName': 'Display Name',
        'version': 'Version',
        'actualVersion': 'Actual Version',
        'releaseNotes': 'Release Notes'
      },
      'repository': {
        'placeholder': 'Enter Base URL or remove this OS'
      },
      'useRedhatSatellite': {
        'title': 'Use RedHat Satellite/Spacewalk',
        'warning': 'By selecting to <b>"Use RedHat Satellite/Spacewalk"</b> for the software repositories, ' +
        'you are responsible for configuring the repository channel in Satellite/Spacewalk and confirming the repositories for the selected <b>stack version</b> are available on the hosts in the cluster. ' +
        'Refer to the Ambari documentation for more information.',
        'disabledMsg': 'Use of RedHat Satellite/Spacewalk is not available when using Public Repositories'
      },
      'changeBaseURLConfirmation': {
        'title': 'Confirm Base URL Change',
        'message': 'You are about to change repository Base URLs that are already in use. Please confirm that you intend to make this change and that the new Base URLs point to the same exact Stack version and build'
      },

      'alerts': {
        'baseURLs': 'Provide Base URLs for the Operating Systems you are configuring.',
        'validationFailed': 'Some of the repositories failed validation. Make changes to the base url or skip validation if you are sure that urls are correct',
        'skipValidationWarning': '<b>Warning:</b> This is for advanced users only. Use this option if you want to skip validation for Repository Base URLs.',
        'useRedhatSatelliteWarning': 'Disable distributed repositories and use RedHat Satellite/Spacewalk channels instead',
        'filterListError': 'Fetch stack version filter list error',
        'versionCreated': 'Created version <a href="#/stackVersions/{{stackName}}/{{versionName}}/edit">{{stackName}}-{{versionName}}</a>',
        'versionCreationError': 'Version creation error',
        'allOsAdded': 'All Operating Systems have been added',
        'osListError': 'getSupportedOSList error',
        'readVersionInfoError': 'Version Definition read error',
        'versionEdited': 'Edited version <a href="#/stackVersions/{{stackName}}/{{versionName}}/edit">{{displayName}}</a>',
        'versionUpdateError': 'Version update error',
        'versionDeleteError': 'Version delete error'
      }
    },

    'authentication': {
      'description': 'Ambari supports authenticating against local Ambari users created and stored in the Ambari Database, or authenticating against a LDAP server:',
      'ldap': 'LDAP Authentication',
      'on': 'On',
      'off': 'Off',

      'connectivity': {
        'title': 'LDAP Connectivity Configuration',
        'host': 'LDAP Server Host',
        'port': 'LDAP Server Port',
        'ssl': 'Use SSL?',
        'trustStore': {
          'label': 'Trust Store',
          'options': {
            'default': 'JDK Default',
            'custom': 'Custom'
          }
        },
        'trustStorePath': 'Trust Store Path',
        'trustStoreType': {
          'label': 'Trust Store Type',
          'options': {
            'jks': 'JKS',
            'jceks': 'JCEKS',
            'pkcs12': 'PKCS12'
          }
        },
        'trustStorePassword': 'Trust Store Password',
        'dn': 'Bind DN',
        'bindPassword': 'Bind Password',

        'controls': {
          'testConnection': 'Test Connection'
        }
      },

      'attributes': {
        'title': 'LDAP Attribute Configuration',
        'detection': {
          'label': 'Identifying the proper attributes to be used when authenticating and looking up users and groups can be specified manually, or automatically detected. Please choose:',
          'options': {
            'manual': 'Define Attributes Manually',
            'auto': 'Auto-Detect Attributes'
          }
        },
        'userSearch': 'User Search Base',
        'groupSearch': 'Group Search Base',
        'detected': 'The following attributes were detected, please review and Test Attributes to ensure their accuracy.',
        'userObjClass': 'User Object Class',
        'userNameAttr': 'User Name Attribute',
        'groupObjClass': 'Group Object Class',
        'groupNameAttr': 'Group Name Attribute',
        'groupMemberAttr': 'Group Member Attribute',
        'distinguishedNameAttr': 'Distinguished Name Attribute',
        'test': {
          'description': 'To quickly test the chosen attributes click the button below. During this process you can specify a test user name and password and Ambari will attempt to authenticate and retrieve group membership information',
          'username': 'Test Username',
          'password': 'Test Password'
        },
        'groupsList': 'List of Groups',

        'controls': {
          'autoDetect': 'Perform Auto-Detection',
          'testAttrs': 'Test Attributes'
        },

        'alerts': {
          'successfulAuth': 'Successful Authentication'
        }
      },

      'controls': {
        'test': 'Test'
      }
    }
  });

  $translateProvider.preferredLanguage('en');
}]);
