import { EnterZoneSignature } from '../../operator/enterZone';
declare module 'rxjs/Observable' {
    interface Observable<T> {
        enterZone: EnterZoneSignature<T>;
    }
}
