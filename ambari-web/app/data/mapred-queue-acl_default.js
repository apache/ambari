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
module.exports = [
  {
    name: 'mapred.queue.default.acl-submit-job',
    value: ' ',
    description: 'Comma separated list of user and group names that are allowed' +
      'to submit jobs to the \'default\' queue. The user list and the group list' +
      'are separated by a blank. For e.g. user1,user2 group1,group2.' +
      'If set to the special value \'*\', it means all users are allowed to' +
      'submit jobs. If set to \' \'(i.e. space), no user will be allowed to submit' +
      'jobs.' +
      '\n' +
      'It is only used if authorization is enabled in Map/Reduce by setting the' +
      'configuration property mapred.acls.enabled to true.' +
      '\n' +
      'Irrespective of this ACL configuration, the user who started the cluster and' +
      'cluster administrators configured via' +
      'mapreduce.cluster.administrators can submit jobs.'
  },
  {
    name: 'mapred.queue.default.acl-administer-jobs',
    value: ' ',
    description: 'Comma separated list of user and group names that are allowed' +
      'to view job details, kill jobs or modify job\'s priority for all the jobs' +
      'in the \'default\' queue.'
  }
];

