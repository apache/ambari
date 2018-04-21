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
import {Injectable} from '@angular/core';
import {CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {RoutingUtilsService} from '@app/services/routing-utils.service';
import {ClustersService} from '@app/services/storage/clusters.service';
import {ShipperClusterServiceListService} from '@modules/shipper/services/shipper-cluster-service-list.service';

@Injectable()
export class ShipperGuard implements CanActivate {

  constructor (
    private routingUtilsService: RoutingUtilsService,
    private router: Router,
    private clustersStoreService: ClustersService,
    private shipperClusterServiceListService: ShipperClusterServiceListService
  ) {}

  getFirstCluster(): Observable<string> {
    return this.clustersStoreService.getAll().map((clusters: string[]) => Array.isArray(clusters) ? clusters[0] : clusters);
  }

  getFirstServiceForCluster(cluster: string): Observable<string> {
    return this.shipperClusterServiceListService.getServicesForCluster(cluster)
      .map((services: string[]) => Array.isArray(services) ? services[0] : services);
  }

  canActivate(
    next: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> | Promise<boolean> | boolean {
    const cluster: string = this.routingUtilsService.getParamFromActivatedRouteSnapshot(next, 'cluster');
    const service: string = this.routingUtilsService.getParamFromActivatedRouteSnapshot(next, 'service');
    (cluster ? Observable.of(cluster) : this.getFirstCluster()).first().subscribe((firstCluster: string) => {
      this.getFirstServiceForCluster(firstCluster).first().subscribe((firstService: string) => {
        this.router.navigate(['/shipper', firstCluster, firstService]);
      });
    });
    return !!(cluster && service);
  }
}
