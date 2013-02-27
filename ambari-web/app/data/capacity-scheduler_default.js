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
    name: 'mapred.capacity-scheduler.maximum-system-jobs',
    value: 3000,
    description: 'Maximum number of jobs in the system which can be initialized, concurrently, by the CapacityScheduler.'
  },
  {
    name: 'mapred.capacity-scheduler.queue.default.capacity',
    value: 100,
    description: 'Percentage of the number of slots in the cluster that are to be available for jobs in this queue.'
  },
  {
    name: 'mapred.capacity-scheduler.queue.default.maximum-capacity',
    value: -1,
    description: 'maximum-capacity defines a limit beyond which a queue cannot use the capacity of the cluster.' +
      'This provides a means to limit how much excess capacity a queue can use. By default, there is no limit.' +
      'The maximum-capacity of a queue can only be greater than or equal to its minimum capacity.' +
      'Default value of -1 implies a queue can use complete capacity of the cluster.' +
      '\n' +
      'This property could be to curtail certain jobs which are long running in nature from occupying more than a' +
      'certain percentage of the cluster, which in the absence of pre-emption, could lead to capacity guarantees of ' +
      'other queues being affected.' +
      '\n' +
      'One important thing to note is that maximum-capacity is a percentage , so based on the cluster\'s capacity' +
      'the max capacity would change. So if large no of nodes or racks get added to the cluster , max Capacity in' +
      'absolute terms would increase accordingly.'
  },
  {
    name: 'mapred.capacity-scheduler.queue.default.supports-priority',
    value: false,
    description: 'If true, priorities of jobs will be taken into account in scheduling decisions.'
  },
  {
    name: 'mapred.capacity-scheduler.queue.default.minimum-user-limit-percent',
    value: 100,
    description: 'Each queue enforces a limit on the percentage of resources' +
      'allocated to a user at any given time, if there is competition for them.' +
      'This user limit can vary between a minimum and maximum value. The former' +
      'depends on the number of users who have submitted jobs, and the latter is' +
      'set to this property value.'
  },
  {
    name: 'mapred.capacity-scheduler.queue.default.user-limit-factor',
    value: 1,
    description: 'The multiple of the queue capacity which can be configured to' +
      'allow a single user to acquire more slots.'
  },
  {
    name: 'mapred.capacity-scheduler.queue.default.maximum-initialized-active-tasks',
    value: 200000,
    description: 'The maximum number of tasks, across all jobs in the queue,' +
      'which can be initialized concurrently. Once the queue\'s jobs exceed this' +
      'limit they will be queued on disk. '
  },
  {
    name: 'mapred.capacity-scheduler.queue.default.maximum-initialized-active-tasks-per-user',
    value: 100000,
    description: 'The maximum number of tasks per-user, across all the of the' +
      'user\'s jobs in the queue, which can be initialized concurrently. Once the' +
      'user\'s jobs exceed this limit they will be queued on disk.'
  },
  {
    name: 'mapred.capacity-scheduler.queue.default.init-accept-jobs-factor',
    value: 10,
    description: 'The multipe of (maximum-system-jobs * queue-capacity) used to' +
      'determine the number of jobs which are accepted by the scheduler.'
  },
  {
    name: 'mapred.capacity-scheduler.default-supports-priority',
    value: false,
    description: 'If true, priorities of jobs will be taken into' +
      'account in scheduling decisions by default in a job queue.'
  },
  {
    name: 'mapred.capacity-scheduler.default-minimum-user-limit-percent',
    value: 100,
    description: 'The percentage of the resources limited to a particular user' +
      'for the job queue at any given point of time by default.'
  },
  {
    name: 'mapred.capacity-scheduler.default-user-limit-factor',
    value: 1,
    description: 'The default multiple of queue-capacity which is used to' +
      'determine the amount of slots a single user can consume concurrently.'
  },
  {
    name: 'mapred.capacity-scheduler.default-user-limit-factor',
    value: 1,
    description: 'The default multiple of queue-capacity which is used to' +
      'determine the amount of slots a single user can consume concurrently.'
  },
  {
    name: 'mapred.capacity-scheduler.default-maximum-active-tasks-per-queue',
    value: 200000,
    description: 'The default maximum number of tasks, across all jobs in the' +
      'queue, which can be initialized concurrently. Once the queue\'s jobs exceed' +
      'this limit they will be queued on disk.'
  },
  {
    name: 'mapred.capacity-scheduler.default-maximum-active-tasks-per-user',
    value: 100000,
    description: 'The default maximum number of tasks per-user, across all the of' +
      'the user\'s jobs in the queue, which can be initialized concurrently. Once' +
      'the user\'s jobs exceed this limit they will be queued on disk.'
  },
  {
    name: 'mapred.capacity-scheduler.default-init-accept-jobs-factor',
    value: 10,
    description: 'The default multipe of (maximum-system-jobs * queue-capacity)' +
      'used to determine the number of jobs which are accepted by the scheduler.'
  },
  {
    name: 'mapred.capacity-scheduler.init-poll-interval',
    value: 5000,
    description: 'The amount of time in milliseconds which is used to poll' +
      'the job queues for jobs to initialize.'
  },
  {
    name: 'mapred.capacity-scheduler.init-worker-threads',
    value: 5,
    description: 'Number of worker threads which would be used by' +
      'Initialization poller to initialize jobs in a set of queue.' +
      'If number mentioned in property is equal to number of job queues' +
      'then a single thread would initialize jobs in a queue. If lesser' +
      'then a thread would get a set of queues assigned. If the number' +
      'is greater then number of threads would be equal to number of' +
      'job queues.'
  }
];

