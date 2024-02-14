import { LeaveZoneSignature } from '../../operator/leaveZone';
declare module 'rxjs/Observable' {
    interface Observable<T> {
        leaveZone: LeaveZoneSignature<T>;
    }
}
