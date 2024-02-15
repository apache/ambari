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

import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { Observable } from 'rxjs/Observable';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { Subscription } from 'rxjs/Subscription';

import { LogsContainerService } from '@app/services/logs-container.service';
import { HistoryManagerService } from '@app/services/history-manager.service';
import { UserSettingsService } from '@app/services/user-settings.service';
import { ListItem } from '@app/classes/list-item';
import { ClustersService } from '@app/services/storage/clusters.service';
import { UtilsService } from '@app/services/utils.service';

@Component({
  selector: 'action-menu',
  templateUrl: './action-menu.component.html',
  styleUrls: ['./action-menu.component.less']
})
export class ActionMenuComponent  implements OnInit, OnDestroy {

  isLogIndexFilterDisplayed$: Observable<boolean> = this.route.queryParams
    .map((params) => {
      return params;
    })
    .map((params): boolean => /^(show|yes|true|1)$/.test(params.logIndexFilterSettings))
    .distinctUntilChanged();

  settingsForm: FormGroup = this.settings.settingsFormGroup;

  isModalSubmitDisabled = true;

  clustersListItems$: Observable<ListItem[]> = this.clustersService.getAll()
    .map((clusterNames: string[]): ListItem[] => clusterNames.map(this.utilsService.getListItemFromString))
    .map((clusters: ListItem[]) => {
      if (clusters.length && !clusters.some((item: ListItem) => item.isChecked)) {
        clusters[0].isChecked = true;
      }
      return clusters;
    });

  selectedClusterName$: BehaviorSubject<string> = new BehaviorSubject('');

  subscriptions: Subscription[] = [];

  constructor(
    private logsContainer: LogsContainerService,
    private historyManager: HistoryManagerService,
    private settings: UserSettingsService,
    private route: ActivatedRoute,
    private router: Router,
    private clustersService: ClustersService,
    private utilsService: UtilsService
  ) {
  }

  ngOnInit() {
    this.subscriptions.push(
      this.selectedClusterName$.subscribe(
        (clusterName: string) => this.setModalSubmitDisabled(!(!!clusterName))
      )
    );
  }

  ngOnDestroy() {
    this.subscriptions.forEach((subscription: Subscription) => subscription.unsubscribe());
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

  onSelectCluster(cluster: string) {
    this.selectedClusterName$.next(cluster);
  }

  openLogIndexFilter(): void {
    this.router.navigate(['.'], {
      queryParamsHandling: 'merge',
      queryParams: {logIndexFilterSettings: 'show'},
      relativeTo: this.route.root.firstChild
    });
  }

  closeLogIndexFilter(): void {
    this.route.queryParams.first().subscribe((queryParams) => {
      const {logIndexFilterSettings, ...params} = queryParams;
      this.router.navigate(['.'], {
        queryParams: params,
        relativeTo: this.route.root.firstChild
      });
    });
  }

  saveLogIndexFilter(): void {
    this.closeLogIndexFilter();
    this.settings.saveIndexFilterConfig();
  }

  startCapture(): void {
    this.logsContainer.startCaptureTimer();
  }

  stopCapture(): void {
    this.logsContainer.stopCaptureTimer();
  }

}
