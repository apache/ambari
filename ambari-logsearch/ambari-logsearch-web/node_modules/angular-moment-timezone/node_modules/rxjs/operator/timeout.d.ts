import { IScheduler } from '../Scheduler';
import { Observable } from '../Observable';
/**
 * @param {number} due
 * @param {Scheduler} [scheduler]
 * @return {Observable<R>|WebSocketSubject<T>|Observable<T>}
 * @method timeout
 * @owner Observable
 */
export declare function timeout<T>(this: Observable<T>, due: number | Date, scheduler?: IScheduler): Observable<T>;
