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
import {FilteringService} from '@app/services/filtering.service';
import {HttpClientService} from '@app/services/http-client.service';
import {ClustersService} from '@app/services/storage/clusters.service';
import {ComponentsService} from '@app/services/storage/components.service';

@Component({
  selector: 'filters-panel',
  templateUrl: './filters-panel.component.html',
  styleUrls: ['./filters-panel.component.less']
})
export class FiltersPanelComponent {

  constructor(private filtering: FilteringService, private httpClient: HttpClientService, private clustersStorage: ClustersService, private componentsStorage: ComponentsService) {
    this.loadClusters();
    this.loadComponents();
  }

  get filters(): any {
    return this.filtering.filters;
  }

  private loadClusters(): void {
    this.httpClient.get('clusters').subscribe(response => {
      const clusterNames = response.json();
      if (clusterNames) {
        this.clustersStorage.addInstances(clusterNames);
      }
    });
  }

  private loadComponents(): void {
    this.httpClient.get('components').subscribe(response => {
      const jsonResponse = response.json(),
        components = jsonResponse && jsonResponse.groupList;
      if (components) {
        const componentNames = components.map(component => component.type);
        this.componentsStorage.addInstances(componentNames);
      }
    });
  }

  get filtersForm(): FormGroup {
    return this.filtering.filtersForm;
  }

}
