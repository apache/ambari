import { Observable } from '../Observable';
import { IScheduler } from '../Scheduler';
/**
 * @param scheduler
 * @return {Observable<Timestamp<any>>|WebSocketSubject<T>|Observable<T>}
 * @method timestamp
 * @owner Observable
 */
export declare function timestamp<T>(this: Observable<T>, scheduler?: IScheduler): Observable<Timestamp<T>>;
export declare class Timestamp<T> {
    value: T;
    timestamp: number;
    constructor(value: T, timestamp: number);
}
