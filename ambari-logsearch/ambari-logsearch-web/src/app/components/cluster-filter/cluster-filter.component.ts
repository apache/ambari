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
import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import 'rxjs/add/operator/switchMap';
import {Observable} from 'rxjs/Observable';
import {ActivatedRouteSnapshot, Router, RoutesRecognized} from '@angular/router';
import {Subscription} from 'rxjs/Subscription';

import {ClustersService} from '@app/services/storage/clusters.service';
import {UtilsService} from '@app/services/utils.service';
import {ListItem} from '@app/classes/list-item';
import {ClusterSelectionService} from '@app/services/storage/cluster-selection.service';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {FilterDropdownComponent} from '@modules/shared/components/filter-dropdown/filter-dropdown.component';
import {RoutingUtilsService} from '@app/services/routing-utils.service';

@Component({
  selector: 'cluster-filter',
  templateUrl: './cluster-filter.component.html',
  styleUrls: ['./cluster-filter.component.less']
})
export class ClusterFilterComponent implements OnInit, OnDestroy {

  @ViewChild('filterDropdown', {
    read: FilterDropdownComponent
  })
  filterDropdown: FilterDropdownComponent;

  private clustersAsListItems$: Observable<ListItem[]>;

  private clusterSelectionStoreKey: BehaviorSubject<string> = new BehaviorSubject('');
  private readonly defaultUseMultiSelection = true;
  private useMultiSelection: BehaviorSubject<boolean> = new BehaviorSubject(false);

  private subscriptions: Subscription[] = [];

  constructor(
    private clusterStoreService: ClustersService,
    private utilsService: UtilsService,
    private router: Router,
    private clusterSelectionStoreService: ClusterSelectionService,
    private routingUtilsService: RoutingUtilsService
  ) { }

  ngOnInit() {
    this.subscriptions.push(
      this.router.events.filter(routes => routes instanceof RoutesRecognized).subscribe(this.onRecognizedActivatedRouteData)
    );
    this.subscriptions.push(
      this.clusterSelectionStoreKey.subscribe(this.onClusterSelectionStoreKeyChange)
    );
    this.actualizeDropdownSelectionByActivatedRouteSnapshot(this.router.routerState.root.snapshot);
  }

  ngOnDestroy() {
    if (this.subscriptions) {
      this.subscriptions.forEach((subscription: Subscription) => subscription.unsubscribe());
    }
  }

  private getClusterSelectionStoreKeyFromActivatedRouteSnapshot(routeSnapshot: ActivatedRouteSnapshot): string {
    return this.routingUtilsService.getDataFromActivatedRouteSnapshot(routeSnapshot, 'clusterSelectionStoreKey')
      || (routeSnapshot.firstChild && routeSnapshot.firstChild.url[0].path);
  }

  private setClusterSelectionStoreKeyFromActivatedRouteSnapshot(routeSnapshot: ActivatedRouteSnapshot): void {
    const clusterSelectionStoreKey: string = this.getClusterSelectionStoreKeyFromActivatedRouteSnapshot(routeSnapshot);
    if (clusterSelectionStoreKey !== this.clusterSelectionStoreKey.getValue()) {
      this.clusterSelectionStoreKey.next(clusterSelectionStoreKey);
    }
  }

  private setUseMultiSelectionFromActivatedRouteSnapshot(routeSnapshot: ActivatedRouteSnapshot): void {
    const multiClusterFilter: boolean | null = this.routingUtilsService.getDataFromActivatedRouteSnapshot(
      routeSnapshot, 'multiClusterFilter'
    );
    if (this.useMultiSelection.getValue() !== multiClusterFilter) {
      this.useMultiSelection.next(
        typeof multiClusterFilter === 'boolean' ? multiClusterFilter : this.defaultUseMultiSelection
      );
    }
  }

  private setDropdownSelectionByActivatedRouteSnapshot(routeSnapshot: ActivatedRouteSnapshot): void {
    let clusterSelection = this.routingUtilsService.getParamFromActivatedRouteSnapshot(routeSnapshot, 'cluster');
    if (clusterSelection) {
      clusterSelection = this.useMultiSelection.getValue() ? clusterSelection.split(/,|;/) : clusterSelection;
      this.filterDropdown.updateSelection(Object.assign(this.utilsService.getListItemFromString(clusterSelection), {
        isChecked: true
      }));
    }
  }

  private actualizeDropdownSelectionByActivatedRouteSnapshot(routeSnapshot: ActivatedRouteSnapshot): void {
    this.setClusterSelectionStoreKeyFromActivatedRouteSnapshot(routeSnapshot);
    this.setUseMultiSelectionFromActivatedRouteSnapshot(routeSnapshot);
    this.setDropdownSelectionByActivatedRouteSnapshot(routeSnapshot);
  }

  private onRecognizedActivatedRouteData = (routes: RoutesRecognized): void => {
    this.actualizeDropdownSelectionByActivatedRouteSnapshot(routes.state.root);
  }

  onDropDownSelectionChanged = (values): void => {
    this.setSelection(values);
  }

  private setSelection = (values): void => {
    this.clusterSelectionStoreService.getParameter(this.clusterSelectionStoreKey.getValue()).first()
      .subscribe(currentCluster => {
        if (!this.utilsService.isEqual(currentCluster, values)) {
          this.clusterSelectionStoreService.setParameter(this.clusterSelectionStoreKey.getValue(), values);
        }
      });
  }

  private setListItems(selectionStoreKey: string): void {
    this.clustersAsListItems$ = Observable.combineLatest(
      this.clusterSelectionStoreService.getParameter(selectionStoreKey),
      this.clusterStoreService.getAll()
    ).map(([selections, clusters]) => {
      const selectedClusters = selections ? (Array.isArray(selections) ? selections : [selections]) : selections;
      return clusters.map((cluster) => Object.assign(this.utilsService.getListItemFromString(cluster), {
          isChecked: selectedClusters && selectedClusters.indexOf(cluster) > -1
        })
      );
    }).startWith([]);
  }

  private onClusterSelectionStoreKeyChange = (selectionStoreKey: string): void => {
    if (selectionStoreKey) {
      this.setListItems(selectionStoreKey);
    }
  }

}
