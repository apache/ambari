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

import {ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges} from '@angular/core';
import {AbstractControl, FormBuilder, FormGroup, ValidatorFn, Validators} from '@angular/forms';
import {Observable} from 'rxjs/Observable';
import {Subject} from 'rxjs/Subject';
import {Observer} from 'rxjs/Observer';
import 'rxjs/add/operator/startWith';

import {CanComponentDeactivate} from '@modules/shared/services/can-deactivate-guard.service';

import {UtilsService} from '@app/services/utils.service';
import {ShipperClusterServiceConfigurationModel} from '../../models/shipper-cluster-service-configuration.model';
import {ShipperCluster} from '../../models/shipper-cluster.type';
import {ShipperClusterService} from '../../models/shipper-cluster-service.type';
import {ShipperClusterServiceConfigurationInterface} from '../../models/shipper-cluster-service-configuration.interface';
import {ShipperConfigurationModel} from '../../models/shipper-configuration.model';
import * as formValidators from '../../directives/validator.directive';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {Subscription} from 'rxjs/Subscription';
import {ShipperClusterServiceValidationModel} from '@modules/shipper/models/shipper-cluster-service-validation.model';

@Component({
  selector: 'shipper-configuration-form',
  templateUrl: './shipper-service-configuration-form.component.html',
  styleUrls: ['./shipper-service-configuration-form.component.less']
})
export class ShipperServiceConfigurationFormComponent implements OnInit, OnDestroy, OnChanges, CanComponentDeactivate {

  private configurationForm: FormGroup;
  private validatorForm: FormGroup;

  @Input()
  clusterName: ShipperCluster;

  @Input()
  serviceName: ShipperClusterService;

  @Input()
  configuration: ShipperClusterServiceConfigurationInterface;

  @Input()
  existingServiceNames: Observable<ShipperClusterService[]> | ShipperClusterService[];

  @Input()
  validationResponse: {[key: string]: any};

  @Output()
  configurationSubmit: EventEmitter<ShipperClusterServiceConfigurationModel> = new EventEmitter<ShipperClusterServiceConfigurationModel>();

  @Output()
  validationSubmit: EventEmitter<ShipperClusterServiceValidationModel> = new EventEmitter<ShipperClusterServiceValidationModel>();

  private configurationComponents$: Observable<string[]>;

  private isLeavingDirtyForm = false;

  private get clusterNameField(): AbstractControl {
    return this.configurationForm.get('clusterName');
  }

  private get serviceNameField(): AbstractControl {
    return this.configurationForm.get('serviceName');
  }

  private get configurationField(): AbstractControl {
    return this.configurationForm.get('configuration');
  }

  private get componentNameField(): AbstractControl {
    return this.validatorForm.get('componentName');
  }

  private get sampleDataField(): AbstractControl {
    return this.validatorForm.get('sampleData');
  }

  private canDeactivateModalResult: Subject<boolean> = new Subject<boolean>();

  private canDeactivateObservable$: Observable<boolean> = Observable.create((observer: Observer<boolean>)  => {
    this.canDeactivateModalResult.subscribe((result: boolean) => {
      observer.next(result);
    });
  });

  private serviceNamesListSubject: BehaviorSubject<ShipperClusterService[]> = new BehaviorSubject<ShipperClusterService[]>([]);

  private subscriptions: Subscription[] = [];

  constructor(
    private utilsService: UtilsService,
    private formBuilder: FormBuilder,
    private changeDetectorRef: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.createForms();
    this.configurationComponents$ = this.configurationForm.controls.configuration.valueChanges.map((newValue: string): string[] => {
      let components: string[];
      try {
        const inputs: {[key: string]: any}[] = (newValue ? JSON.parse(newValue) : {}).input;
        components = inputs && inputs.length ? inputs.map(input => input.type) : [];
      } catch (error) {
        components = [];
      }
      return components;
    }).startWith([]);
    if (this.existingServiceNames instanceof Observable) {
      this.subscriptions.push(
        this.existingServiceNames.subscribe((serviceNames: ShipperClusterService[]) => {
          this.serviceNamesListSubject.next(serviceNames);
        })
      );
    } else {
      this.serviceNamesListSubject.next(this.existingServiceNames);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (this.configurationForm) {
      Object.keys(changes).forEach((controlName: string) => {
        if (this.configurationForm.controls[controlName]) {
          let value: any = changes[controlName].currentValue;
          if (controlName === 'configuration') {
            value = value || new ShipperConfigurationModel();
            if (!(value instanceof String)) {
              value = this.getConfigurationAsString(value);
            }
          }
          if (this.configurationForm.controls[controlName].value !== value) {
            this.configurationForm.controls[controlName].setValue(value);
            this.configurationForm.markAsPristine();
          }
        }
      });
    }
    if (this.validatorForm && changes.clusterName && this.validatorForm.controls.clusterName.value !== changes.clusterName.currentValue) {
      this.validatorForm.controls.clusterName.setValue(changes.clusterName.currentValue);
      this.validatorForm.markAsPristine();
    }
  }

  ngOnDestroy() {
    if (this.subscriptions) {
      this.subscriptions.forEach(subscription => subscription.unsubscribe());
    }
  }

  leaveDirtyFormConfirmed = () => {
    this.canDeactivateModalResult.next(true);
    this.isLeavingDirtyForm = false;
  }

  leaveDirtyFormCancelled = () => {
    this.canDeactivateModalResult.next(false);
    this.isLeavingDirtyForm = false;
  }

  canDeactivate(): Observable<boolean> {
    if (this.configurationForm.pristine) {
      return Observable.of(true);
    }
    this.isLeavingDirtyForm = true;
    return this.canDeactivateObservable$;
  }

  getConfigurationAsString(configuration: ShipperClusterServiceConfigurationInterface): string {
    return configuration ? JSON.stringify(configuration, null, 4) : '';
  }

  createForms(): void {
    const configuration: ShipperClusterServiceConfigurationInterface = this.configuration || (
      this.serviceName ? this.configuration : new ShipperConfigurationModel()
    );
    const configurationFormValidators: ValidatorFn[] = [Validators.required];
    if (!this.serviceName) {
      configurationFormValidators.push(formValidators.uniqueServiceNameValidator(this.serviceNamesListSubject));
    }
    this.configurationForm = this.formBuilder.group({
      clusterName: this.formBuilder.control(this.clusterName, Validators.required),
      serviceName: this.formBuilder.control(
        this.serviceName,
        configurationFormValidators
      ),
      configuration: this.formBuilder.control(
        this.getConfigurationAsString(configuration),
        [Validators.required, formValidators.configurationValidator()]
      )
    });

    this.validatorForm = this.formBuilder.group({
      clusterName: this.formBuilder.control(this.clusterName, Validators.required),
      componentName: this.formBuilder.control('', Validators.required),
      sampleData: this.formBuilder.control('', Validators.required)
    });
  }

  onConfigurationSubmit(configurationForm: FormGroup): void {
    if (this.configurationForm.valid) {
      const rawValue = this.configurationForm.getRawValue();
      const serviceName: string = Array.isArray(rawValue.serviceName) ? rawValue.serviceName.shift().value : rawValue.serviceName;
      this.configurationSubmit.emit({
        cluster: rawValue.clusterName,
        service: serviceName,
        configuration: JSON.parse(rawValue.configuration)
      });
    }
  }

  onValidationSubmit(): void {
    if (this.validatorForm) {
      this.validationSubmit.emit({
        configuration: this.configurationForm.controls.configuration.value,
        ...this.validatorForm.getRawValue()
      });
    }
  }

}
