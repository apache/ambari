import { InjectionToken } from './di';
/**
 * A function that will be executed when an application is initialized.
 * @experimental
 */
export declare const APP_INITIALIZER: InjectionToken<(() => void)[]>;
/**
 * A class that reflects the state of running {@link APP_INITIALIZER}s.
 *
 * @experimental
 */
export declare class ApplicationInitStatus {
    private appInits;
    private resolve;
    private reject;
    private initialized;
    private _donePromise;
    private _done;
    constructor(appInits: (() => any)[]);
    readonly done: boolean;
    readonly donePromise: Promise<any>;
}
