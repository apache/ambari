import { SelectSignature } from '../../operator/select';
declare module 'rxjs/Observable' {
    interface Observable<T> {
        select: SelectSignature<T>;
    }
}
