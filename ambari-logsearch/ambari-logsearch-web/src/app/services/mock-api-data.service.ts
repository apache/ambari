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

import {URLSearchParams, Response, ResponseOptions} from '@angular/http';
import {InMemoryDbService, InMemoryBackendService, createErrorResponse} from 'angular-in-memory-web-api';
import {Observable} from 'rxjs/Observable';
import {Subscriber} from 'rxjs/Subscriber';
import * as moment from 'moment';
import {mockData} from '@app/mock-data';

export class mockBackendService extends InMemoryBackendService {
  getLocation(url: string): any {
    return super.getLocation(url);
  }
}

export class mockApiDataService implements InMemoryDbService {

  private readonly filterMap = {
    'api/v1/service/logs': {
      pathToCollection: 'logList',
      totalCountKey: 'totalCount',
      filters: {
        clusters: {
          key: 'cluster',
          isValuesList: true
        },
        mustBe: {
          key: 'type',
          isValuesList: true
        },
        level: {
          key: 'level',
          isValuesList: true
        },
        iMessage: {
          key: 'log_message',
          filterFunction: (value, filterValue) => value.toLowerCase().indexOf(filterValue.toLowerCase()) > -1
        },
        from: {
          key: 'logtime',
          filterFunction: (value, filterValue) => value >= moment(filterValue).valueOf()
        },
        to: {
          key: 'logtime',
          filterFunction: (value, filterValue) => value < moment(filterValue).valueOf()
        },
        hostList: {
          key: 'host',
          isValuesList: true
        }
      }
    },
    'api/v1/audit/logs': {
      pathToCollection: 'logList',
      totalCountKey: 'totalCount',
      filters: {
        clusters: {
          key: 'cluster',
          isValuesList: true
        },
        iMessage: {
          key: 'log_message',
          filterFunction: (value, filterValue) => value.toLowerCase().indexOf(filterValue.toLowerCase()) > -1
        },
        from: {
          key: 'evtTime',
          filterFunction: (value, filterValue) => value >= moment(filterValue).valueOf()
        },
        to: {
          key: 'evtTime',
          filterFunction: (value, filterValue) => value < moment(filterValue).valueOf()
        }
      }
    }
  };

  parseUrl(url: string): any {
    const urlLocation = mockBackendService.prototype.getLocation(url),
      query = urlLocation.search && new URLSearchParams(urlLocation.search.substr(1), {
          encodeKey: key => key,
          encodeValue: value => value
        }),
      splitUrl = urlLocation.pathname.substr(1).split('/'),
      urlPartsCount = splitUrl.length,
      collectionName = splitUrl[urlPartsCount - 1],
      base = splitUrl.slice(0, urlPartsCount - 1).join('/') + '/';
    return {
      base: base,
      collectionName: collectionName,
      query: query
    };
  }

  get(interceptorArgs: any): Observable<Response> {
    const query = interceptorArgs.requestInfo.query,
      path = interceptorArgs.requestInfo.base + interceptorArgs.requestInfo.collectionName,
      pathArray = path.split('/').filter(part => part !== '');
    if (query && query.paramsMap.has('static') && interceptorArgs.passThruBackend) {
      return interceptorArgs.passThruBackend.createConnection(interceptorArgs.requestInfo.req).response;
    } else {
      let is404 = false;
      const allData = pathArray.reduce((currentObject, currentKey, index, array) => {
        if (!currentObject && index < array.length - 1) {
          return {};
        } else if (currentObject.hasOwnProperty(currentKey)) {
          return currentObject[currentKey];
        } else {
          is404 = true;
          return {};
        }
      }, interceptorArgs.db);
      if (is404) {
        return new Observable<Response>((subscriber: Subscriber<Response>) => subscriber.error(new Response(createErrorResponse(
          interceptorArgs.requestInfo.req, 404, 'Not found'
        ))));
      } else {
        let filteredData;
        const filterMapItem = this.filterMap[path];
        if (query && filterMapItem) {
          filteredData = {};
          const pathToCollection = filterMapItem.pathToCollection,
            collection = allData[pathToCollection];
          let filteredCollection = collection.filter(item => {
            let result = true;
              query.paramsMap.forEach((value, key) => {
              const paramValue = decodeURIComponent(value[0]),
                paramFilter = filterMapItem.filters[key],
                paramValuesList = paramFilter && paramFilter.isValuesList && paramValue ? paramValue.split(',') : [],
                currentValue = paramFilter && item[paramFilter.key];
              if (paramFilter &&
                ((paramFilter.filterFunction && !paramFilter.filterFunction(currentValue, paramValue)) ||
                (!paramFilter.filterFunction && !paramFilter.isValuesList && currentValue !== paramValue) ||
                (!paramFilter.filterFunction && paramFilter.isValuesList && paramValuesList.indexOf(currentValue) === -1))) {
                result = false;
              }
            });
            return result;
          });
          if (query.paramsMap.has('sortBy') && query.paramsMap.has('sortType')) {
            const sortKey = query.paramsMap.get('sortBy')[0],
              sortType = query.paramsMap.get('sortType')[0];
            filteredCollection.sort((a, b) => {
              const itemA = a[sortKey],
                itemB = b[sortKey];
              let ascResult;
              if (itemA > itemB) {
                ascResult = 1;
              } else if (itemA < itemB) {
                ascResult = -1;
              } else {
                ascResult = 0;
              }
              return ascResult * Math.pow(-1, Number(sortType === 'desc'));
            });
          }
          if (filterMapItem.totalCountKey) {
            filteredData[filterMapItem.totalCountKey] = filteredCollection.length;
          }
          if (query && query.paramsMap.has('page') && query.paramsMap.has('pageSize')) {
            const page = parseInt(query.paramsMap.get('page')[0]),
              pageSize = parseInt(query.paramsMap.get('pageSize')[0]);
            filteredCollection = filteredCollection.slice(page * pageSize, (page + 1) * pageSize);
          }
          filteredData[pathToCollection] = filteredCollection;
        } else {
          filteredData = allData;
        }
        return new Observable<Response>((subscriber: Subscriber<Response>) => subscriber.next(new Response(new ResponseOptions({
          status: 200,
          body: filteredData
        }))));
      }
    }
  }

  post(interceptorArgs: any) {
    // TODO implement posting data to mock object except login call
    return this.get(interceptorArgs);
  }

  createDb() {
    return mockData;
  }
}
