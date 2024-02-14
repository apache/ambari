export declare function devModeEqual(a: any, b: any): boolean;
/**
 * Indicates that the result of a {@link Pipe} transformation has changed even though the
 * reference
 * has not changed.
 *
 * The wrapped value will be unwrapped by change detection, and the unwrapped value will be stored.
 *
 * Example:
 *
 * ```
 * if (this._latestValue === this._latestReturnedValue) {
 *    return this._latestReturnedValue;
 *  } else {
 *    this._latestReturnedValue = this._latestValue;
 *    return WrappedValue.wrap(this._latestValue); // this will force update
 *  }
 * ```
 * @stable
 */
export declare class WrappedValue {
    wrapped: any;
    constructor(wrapped: any);
    static wrap(value: any): WrappedValue;
}
/**
 * Helper class for unwrapping WrappedValue s
 */
export declare class ValueUnwrapper {
    hasWrappedValue: boolean;
    unwrap(value: any): any;
    reset(): void;
}
/**
 * Represents a basic change from a previous to a new value.
 * @stable
 */
export declare class SimpleChange {
    previousValue: any;
    currentValue: any;
    firstChange: boolean;
    constructor(previousValue: any, currentValue: any, firstChange: boolean);
    /**
     * Check whether the new value is the first value assigned.
     */
    isFirstChange(): boolean;
}
export declare function isListLikeIterable(obj: any): boolean;
export declare function areIterablesEqual(a: any, b: any, comparator: (a: any, b: any) => boolean): boolean;
export declare function iterateListLike(obj: any, fn: (p: any) => any): void;
export declare function isJsObject(o: any): boolean;
