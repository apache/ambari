import { Injectable } from '@angular/core';
import {Response} from '@angular/http';

import { Observable } from 'rxjs/Observable';

import { HttpClientService } from '@app/services/http-client.service';
import { Filter } from '@app/classes/models/filter';

@Injectable()
export class LogIndexFilterService {

  constructor(private httpClient: HttpClientService) { }

  getFilterByClusterName(clusterName: string): Observable<Filter> {
    return this.httpClient.get('logIndexFilters', null, {
      clusterName
    }).map((response: Response): Filter => {
      const filters: Filter = response.json() && response.json().filter;
      return filters;
    });
  }

}
