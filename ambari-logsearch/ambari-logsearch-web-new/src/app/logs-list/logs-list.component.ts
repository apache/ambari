/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {Component, OnInit} from '@angular/core';

@Component({
  selector: 'logs-list',
  templateUrl: './logs-list.component.html',
  styleUrls: ['./logs-list.component.less']
})
export class LogsListComponent implements OnInit {

  constructor() {
  }

  ngOnInit() {
  }

  // TODO implement loading logs from API
  private readonly logs = [
    {
      level: 'fatal',
      type: 'ambari_agent',
      time: '2017/01/31 18:00:00',
      content: 'Something went wrong.<br>Please restart ambari-agent.<br>See log file for details.'
    },
    {
      level: 'error',
      type: 'ambari_agent',
      time: '2017/01/31 18:00:00',
      content: 'Something went wrong.<br>Please restart ambari-agent.<br>See log file for details.'
    },
    {
      level: 'warn',
      type: 'ambari_agent',
      time: '2017/01/31 18:00:00',
      content: 'Something went wrong.<br>Please restart ambari-agent.<br>See log file for details.'
    },
    {
      level: 'info',
      type: 'ambari_agent',
      time: '2017/01/31 18:00:00',
      content: 'Something went wrong.<br>Please restart ambari-agent.<br>See log file for details.'
    },
    {
      level: 'debug',
      type: 'ambari_agent',
      time: '2017/01/31 18:00:00',
      content: 'Something went wrong.<br>Please restart ambari-agent.<br>See log file for details.'
    },
    {
      level: 'trace',
      type: 'ambari_agent',
      time: '2017/01/31 18:00:00',
      content: 'Something went wrong.<br>Please restart ambari-agent.<br>See log file for details.'
    },
    {
      level: 'unknown',
      type: 'ambari_agent',
      time: '2017/01/31 18:00:00',
      content: 'Something went wrong.<br>Please restart ambari-agent.<br>See log file for details.'
    }
  ];

}
