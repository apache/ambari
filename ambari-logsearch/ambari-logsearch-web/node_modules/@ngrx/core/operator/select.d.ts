import { Observable } from 'rxjs/Observable';
export interface SelectSignature<T> {
    <R>(...paths: string[]): Observable<R>;
    <R>(mapFn: (state: T) => R): Observable<R>;
}
export declare function select<T, R>(pathOrMapFn: any, ...paths: string[]): Observable<R>;
