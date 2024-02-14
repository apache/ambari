/**
 * The module that includes all the basic Angular directives like {@link NgIf}, {@link NgForOf}, ...
 *
 * @stable
 */
export declare class CommonModule {
}
/**
 * I18N pipes are being changed to move away from using the JS Intl API.
 *
 * The former pipes relying on the Intl API will be moved to this module while the `CommonModule`
 * will contain the new pipes that do not rely on Intl.
 *
 * As a first step this module is created empty to ease the migration.
 *
 * see https://github.com/angular/angular/pull/18284
 *
 * @deprecated from v5
 */
export declare class DeprecatedI18NPipesModule {
}
