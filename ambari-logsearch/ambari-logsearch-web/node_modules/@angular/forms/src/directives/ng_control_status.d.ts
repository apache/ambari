import { AbstractControlDirective } from './abstract_control_directive';
import { ControlContainer } from './control_container';
import { NgControl } from './ng_control';
export declare class AbstractControlStatus {
    private _cd;
    constructor(cd: AbstractControlDirective);
    readonly ngClassUntouched: boolean;
    readonly ngClassTouched: boolean;
    readonly ngClassPristine: boolean;
    readonly ngClassDirty: boolean;
    readonly ngClassValid: boolean;
    readonly ngClassInvalid: boolean;
    readonly ngClassPending: boolean;
}
export declare const ngControlStatusHost: {
    '[class.ng-untouched]': string;
    '[class.ng-touched]': string;
    '[class.ng-pristine]': string;
    '[class.ng-dirty]': string;
    '[class.ng-valid]': string;
    '[class.ng-invalid]': string;
    '[class.ng-pending]': string;
};
/**
 * Directive automatically applied to Angular form controls that sets CSS classes
 * based on control status. The following classes are applied as the properties
 * become true:
 *
 * * ng-valid
 * * ng-invalid
 * * ng-pending
 * * ng-pristine
 * * ng-dirty
 * * ng-untouched
 * * ng-touched
 *
 * @stable
 */
export declare class NgControlStatus extends AbstractControlStatus {
    constructor(cd: NgControl);
}
/**
 * Directive automatically applied to Angular form groups that sets CSS classes
 * based on control status (valid/invalid/dirty/etc).
 *
 * @stable
 */
export declare class NgControlStatusGroup extends AbstractControlStatus {
    constructor(cd: ControlContainer);
}
