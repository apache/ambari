import { Operator } from 'rxjs/Operator';
import { Subscriber } from 'rxjs/Subscriber';
import { Observable } from 'rxjs/Observable';
export interface EnterZoneSignature<T> {
    (zone: {
        run: (fn: any) => any;
    }): Observable<T>;
}
export declare function enterZone<T>(zone: {
    run: (fn: any) => any;
}): Observable<T>;
export declare class EnterZoneOperator<T> implements Operator<T, T> {
    private _zone;
    constructor(_zone: {
        run: (fn: any) => any;
    });
    call(subscriber: Subscriber<T>, source: any): any;
}
