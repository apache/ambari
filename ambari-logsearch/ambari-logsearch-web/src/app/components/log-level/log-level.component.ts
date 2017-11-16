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
import {Component, Input} from '@angular/core';

/**
 * This is a simple UI component to display the log message. The goal is to be able to show one line and be collapsile
 * to show the full log message with new lines.
 * @class LogMessageComponent
 */
@Component({
  selector: 'log-level',
  templateUrl: './log-level.component.html',
  styleUrls: []
})
export class LogLevelComponent {

  /**
   * This is the log entry object
   * @type {object}
   */
  @Input()
  logEntry: any;

  private classMap: object = {
    warn: 'fa-exclamation-triangle',
    fatal: 'fa-exclamation-circle',
    error: 'fa-exclamation-circle',
    info: 'fa-info-circle',
    debug: 'fa-bug',
    trace: 'fa-random',
    unknown: 'fa-question-circle'
  };

  get cssClass() {
    return this.classMap[((this.logEntry && this.logEntry.level) || 'unknown').toLowerCase()];
  }

}
