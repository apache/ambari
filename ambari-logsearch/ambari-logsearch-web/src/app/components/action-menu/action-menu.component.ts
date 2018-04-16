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

import {Component} from '@angular/core';
import {FormGroup} from '@angular/forms';
import {LogsContainerService} from '@app/services/logs-container.service';
import {HistoryManagerService} from '@app/services/history-manager.service';
import {UserSettingsService} from '@app/services/user-settings.service';
import {ListItem} from '@app/classes/list-item';

@Component({
  selector: 'action-menu',
  templateUrl: './action-menu.component.html',
  styleUrls: ['./action-menu.component.less']
})
export class ActionMenuComponent {

  isLogIndexFilterDisplayed: boolean = false;

  settingsForm: FormGroup = this.settings.settingsFormGroup;

  isModalSubmitDisabled: boolean = true;

  constructor(
    private logsContainer: LogsContainerService, private historyManager: HistoryManagerService,
    private settings: UserSettingsService
  ) {
  }

  get undoItems(): ListItem[] {
    return this.historyManager.undoItems;
  }

  get redoItems(): ListItem[] {
    return this.historyManager.redoItems;
  }

  get historyItems(): ListItem[] {
    return this.historyManager.activeHistory;
  }

  get captureSeconds(): number {
    return this.logsContainer.captureSeconds;
  }

  setModalSubmitDisabled(isDisabled: boolean): void {
    this.isModalSubmitDisabled = isDisabled;
  }

  undoLatest(): void {
    if (this.undoItems.length) {
      this.historyManager.undo(this.undoItems[0]);
    }
  }

  redoLatest(): void {
    if (this.redoItems.length) {
      this.historyManager.redo(this.redoItems[0]);
    }
  }

  undo(item: ListItem): void {
    this.historyManager.undo(item);
  }

  redo(item: ListItem): void {
    this.historyManager.redo(item);
  }

  refresh(): void {
    this.logsContainer.loadLogs();
  }

  openLogIndexFilter(): void {
    this.isLogIndexFilterDisplayed = true;
  }

  closeLogIndexFilter(): void {
    this.isLogIndexFilterDisplayed = false;
  }

  saveLogIndexFilter(): void {
    this.isLogIndexFilterDisplayed = false;
    this.settings.saveIndexFilterConfig();
  }

  startCapture(): void {
    this.logsContainer.startCaptureTimer();
  }

  stopCapture(): void {
    this.logsContainer.stopCaptureTimer();
  }

}
