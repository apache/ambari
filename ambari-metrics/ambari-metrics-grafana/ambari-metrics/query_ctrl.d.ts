/// <reference path="../../../../../public/app/headers/common.d.ts" />
import { QueryCtrl } from 'app/plugins/sdk';
export declare class AmbariMetricsQueryCtrl extends QueryCtrl {
    static templateUrl: string;
    aggregators: any;
    aggregator: any;
    errors: any;
    precisions: any;
    transforms: any;
    transform: any;
    precisionInit: any;
    suggestMetrics: any;
    suggestApps: any;
    suggestHosts: any;
    /** @ngInject **/
    constructor($scope: any, $injector: any);
    targetBlur(): void;
    getTextValues(metricFindResult: any): any;
    getCollapsedText(): string;
    validateTarget(): any;
}
