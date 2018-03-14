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

import {AbstractControl, ValidatorFn} from '@angular/forms';
import {ShipperClusterService} from '@modules/shipper/models/shipper-cluster-service.type';
import {ValidationErrors} from '@angular/forms/src/directives/validators';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';

export function configurationValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    try {
      const json: {[key: string]: any} = JSON.parse(control.value);
      return null;
    } catch (error) {
      return {
        invalidJSON: {value: control.value}
      };
    }
  };
}

export function uniqueServiceNameValidator(
  serviceNames: BehaviorSubject<ShipperClusterService[]>
): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const services: ShipperClusterService[] = serviceNames.getValue();
    return services && services.indexOf(control.value) > -1 ? {
      serviceNameExists: {value: control.value}
    } : null;
  };
}

