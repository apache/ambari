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

import {TestBed, inject} from '@angular/core/testing';
import {StoreModule} from '@ngrx/store';
import {AppSettingsService, appSettings} from '@app/services/storage/app-settings.service';
import {AppStateService, appState} from '@app/services/storage/app-state.service';
import {ClustersService, clusters} from '@app/services/storage/clusters.service';
import {ComponentsService, components} from '@app/services/storage/components.service';
import {HostsService, hosts} from '@app/services/storage/hosts.service';
import {UtilsService} from '@app/services/utils.service';
import {HttpClientService} from '@app/services/http-client.service';
import {ListItem} from '@app/classes/list-item';
import {Node} from '@app/classes/models/node';

import {FilteringService} from './filtering.service';

describe('FilteringService', () => {
  beforeEach(() => {
    const httpClient = {
      get: () => {
        return {
          subscribe: () => {
          }
        }
      }
    };
    TestBed.configureTestingModule({
      imports: [
        StoreModule.provideStore({
          appSettings,
          appState,
          clusters,
          components,
          hosts
        })
      ],
      providers: [
        FilteringService,
        AppSettingsService,
        AppStateService,
        ClustersService,
        ComponentsService,
        HostsService,
        UtilsService,
        {
          provide: HttpClientService,
          useValue: httpClient
        }
      ]
    });
  });

  it('should create service', inject([FilteringService], (service: FilteringService) => {
    expect(service).toBeTruthy();
  }));

  describe('#getListItemFromString()', () => {
    it('should convert string to ListItem', inject([FilteringService], (service: FilteringService) => {
      const getListItemFromString: (name: string) => ListItem = service['getListItemFromString'];
      expect(getListItemFromString('customName')).toEqual({
        label: 'customName',
        value: 'customName'
      });
    }));
  });

  describe('#getListItemFromNode()', () => {
    it('should convert Node to ListItem', inject([FilteringService], (service: FilteringService) => {
      const getListItemFromNode: (node: Node) => ListItem = service['getListItemFromNode'];
      expect(getListItemFromNode({
        name: 'customName',
        value: '1',
        isParent: true,
        isRoot: true
      })).toEqual({
        label: 'customName (1)',
        value: 'customName'
      });
    }));
  });
});
