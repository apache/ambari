/**
 * @whatItDoes Provides a hook for centralized exception handling.
 *
 * @description
 *
 * The default implementation of `ErrorHandler` prints error messages to the `console`. To
 * intercept error handling, write a custom exception handler that replaces this default as
 * appropriate for your app.
 *
 * ### Example
 *
 * ```
 * class MyErrorHandler implements ErrorHandler {
 *   handleError(error) {
 *     // do something with the exception
 *   }
 * }
 *
 * @NgModule({
 *   providers: [{provide: ErrorHandler, useClass: MyErrorHandler}]
 * })
 * class MyModule {}
 * ```
 *
 * @stable
 */
export declare class ErrorHandler {
    constructor(
        /**
         * @deprecated since v4.0 parameter no longer has an effect, as ErrorHandler will never
         * rethrow.
         */
        deprecatedParameter?: boolean);
    handleError(error: any): void;
}
export declare function wrappedError(message: string, originalError: any): Error;
