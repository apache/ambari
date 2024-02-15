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
import {Component, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Response} from '@angular/http';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/operator/skipWhile';

import {NotificationService, NotificationType} from '@modules/shared/services/notification.service';
import {CanComponentDeactivate} from '@modules/shared/services/can-deactivate-guard.service';

import {ShipperCluster} from '../../models/shipper-cluster.type';
import {ShipperClusterService} from '../../models/shipper-cluster-service.type';
import {ShipperConfigurationService} from '../../services/shipper-configuration.service';
import {ShipperClusterServiceListService} from '../../services/shipper-cluster-service-list.service';
import {
  ShipperServiceConfigurationFormComponent
} from '@modules/shipper/components/shipper-service-configuration-form/shipper-service-configuration-form.component';
import {TranslateService} from '@ngx-translate/core';
import {ClusterSelectionService} from '@app/services/storage/cluster-selection.service';
import {Subscription} from 'rxjs/Subscription';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { FormGroup } from '@angular/forms';

@Component({
  selector: 'shipper-configuration',
  templateUrl: './shipper-configuration.component.html',
  styleUrls: ['./shipper-configuration.component.less']
})
export class ShipperConfigurationComponent implements CanComponentDeactivate, OnInit, OnDestroy {

  static clusterSelectionStoreKey = 'shipper';

  @Input()
  routerPath: string[] = ['/shipper'];

  @ViewChild(ShipperServiceConfigurationFormComponent)
  configurationFormRef: ShipperServiceConfigurationFormComponent;

  private requestInProgress$: BehaviorSubject<boolean> = new BehaviorSubject(false);

  private clusterName$: Observable<ShipperClusterService> = this.activatedRoute.params.map(params => params.cluster);
  private serviceName$: Observable<ShipperClusterService> = this.activatedRoute.params.map(params => params.service);

  private serviceNamesList$: Observable<ShipperClusterService[]> = this.clusterName$.switchMap((cluster: ShipperCluster) => {
    return cluster ? this.shipperClusterServiceListService.getServicesForCluster(cluster) : Observable.of(undefined);
  });

  private configuration$: Observable<{[key: string]: any}> = Observable.combineLatest(
      this.clusterName$,
      this.serviceName$
    ).switchMap(([clusterName, serviceName]: [ShipperCluster, ShipperClusterService]) => {
      return clusterName && serviceName ?
        this.shipperConfigurationService.loadConfiguration(clusterName, serviceName) : Observable.of(undefined);
    });

  private subscriptions: Subscription[] = [];

  validationResponse: {[key: string]: any};

  constructor(
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private shipperClusterServiceListService: ShipperClusterServiceListService,
    private shipperConfigurationService: ShipperConfigurationService,
    private notificationService: NotificationService,
    private translate: TranslateService,
    private clusterSelectionStoreService: ClusterSelectionService
  ) { }

  ngOnInit() {
    this.subscriptions.push(
      this.clusterSelectionStoreService
        .getParameter(ShipperConfigurationComponent.clusterSelectionStoreKey)
        .subscribe(this.onClusterSelectionChanged)
    );
  }

  ngOnDestroy() {
    if (this.subscriptions) {
      this.subscriptions.forEach((subscription: Subscription) => subscription.unsubscribe());
    }
  }

  private getPathMapForClusterFirstService(cluster: ShipperCluster): Observable<string[]> {
    return this.shipperClusterServiceListService.getServicesForCluster(cluster)
      .switchMap((serviceNamesList: ShipperClusterService[]) => {
        return Observable.of(this.getRouterLink([cluster, serviceNamesList[0]]));
      });
  }

  onClusterSelectionChanged = (selection: ShipperCluster): void => {
    let clusterName: ShipperCluster = selection;
    if (Array.isArray(clusterName)) {
      clusterName = clusterName.shift();
    }
    if (clusterName) {
      this.clusterName$.first().subscribe((currentClusterName: ShipperCluster) => {
        if (currentClusterName !== clusterName) {
          this.getPathMapForClusterFirstService(clusterName).first().subscribe((path: string[]) => this.router.navigate(path));
        }
      });
    }
  }

  private getRouterLink(path: string | string[]): string[] {
    return [...this.routerPath, ...(Array.isArray(path) ? path : [path])];
  }

  getResponseHandler(cmd: string, type: string, form?: FormGroup) {
    const msgVariables = form.getRawValue();
    return (response: Response) => {
      const result = response.json();
      // @ToDo change the backend response status to some error code if the configuration is not valid and don't use the .message prop
      const resultType = response ? (response.ok && !result.errorMessage ? NotificationType.SUCCESS : NotificationType.ERROR) : type;
      const translateParams = {errorMessage: (result && result.message) || '', ...msgVariables, ...result};
      const title = this.translate.instant(`shipperConfiguration.action.${cmd}.title`, translateParams);
      const message = this.translate.instant(`shipperConfiguration.action.${cmd}.${resultType}.message`, translateParams);
      this.notificationService.addNotification({type: resultType, title, message});
      if (resultType !== NotificationType.ERROR) {
        form.markAsPristine();
      }
      this.requestInProgress$.next(false);
    };
  }

  onConfigurationFormSubmit(configurationForm: FormGroup): void {
    const rawValue = configurationForm.getRawValue();
    this.serviceNamesList$.first().subscribe((services: ShipperClusterService[]) => {
      const cmd: string = services.indexOf(rawValue.serviceName) > -1 ? 'update' : 'add';
      this.requestInProgress$.next(true);
      this.shipperConfigurationService[`${cmd}Configuration`]({
        cluster: rawValue.clusterName,
        service: rawValue.serviceName,
        configuration: JSON.parse(rawValue.configuration)
      }).subscribe(
        this.getResponseHandler(cmd, NotificationType.SUCCESS, configurationForm),
        this.getResponseHandler(cmd, NotificationType.ERROR, configurationForm)
      );
    });
  }

  private setValidationResult = (result: {[key: string]: any}) => {
    this.validationResponse = result;
  }

  onValidationFormSubmit(validationForm: FormGroup): void {
    this.validationResponse = null;
    const rawValue = validationForm.getRawValue();
    const request$: Observable<Response> = this.shipperConfigurationService.testConfiguration(rawValue);
    request$.subscribe(
      this.getResponseHandler('validate', NotificationType.SUCCESS, validationForm),
      this.getResponseHandler('validate', NotificationType.ERROR, validationForm)
    );
    request$
      .filter((response: Response): boolean => response.ok)
      .map((response: Response) => response.json())
      // @ToDo change the backend response status to some error code if the configuration is not valid and don't use the .errorMessage prop
      .filter(result => result.errorMessage === undefined)
      .subscribe(this.setValidationResult);
  }

  canDeactivate() {
    return this.configurationFormRef.canDeactivate();
  }

}
