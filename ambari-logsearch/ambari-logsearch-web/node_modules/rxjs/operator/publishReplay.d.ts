import { Observable } from '../Observable';
import { IScheduler } from '../Scheduler';
import { ConnectableObservable } from '../observable/ConnectableObservable';
/**
 * @param bufferSize
 * @param windowTime
 * @param scheduler
 * @return {ConnectableObservable<T>}
 * @method publishReplay
 * @owner Observable
 */
export declare function publishReplay<T>(this: Observable<T>, bufferSize?: number, windowTime?: number, scheduler?: IScheduler): ConnectableObservable<T>;
