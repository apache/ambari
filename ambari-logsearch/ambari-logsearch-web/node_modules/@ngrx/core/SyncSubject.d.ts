import { ReplaySubject } from 'rxjs/ReplaySubject';
export declare class SyncSubject<T> extends ReplaySubject<T> {
    constructor(value: T);
}
