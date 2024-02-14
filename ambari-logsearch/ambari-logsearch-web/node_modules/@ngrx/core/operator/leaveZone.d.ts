import { Operator } from 'rxjs/Operator';
import { Subscriber } from 'rxjs/Subscriber';
import { Observable } from 'rxjs/Observable';
export declare function leaveZone<T>(zone: {
    runOutsideAngular: (fn: any) => any;
}): Observable<T>;
export interface LeaveZoneSignature<T> {
    (zone: {
        runOutsideAngular: (fn: any) => any;
    }): Observable<T>;
}
export declare class LeaveZoneOperator<T> implements Operator<T, T> {
    private _zone;
    constructor(_zone: {
        runOutsideAngular: (fn: any) => any;
    });
    call(subscriber: Subscriber<T>, source: any): any;
}
