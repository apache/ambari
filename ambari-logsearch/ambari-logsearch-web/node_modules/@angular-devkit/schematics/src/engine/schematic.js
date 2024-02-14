"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
const Observable_1 = require("rxjs/Observable");
require("rxjs/add/observable/of");
require("rxjs/add/operator/concatMap");
const exception_1 = require("../exception/exception");
class InvalidSchematicsNameException extends exception_1.BaseException {
    constructor(name) {
        super(`Schematics has invalid name: "${name}".`);
    }
}
exports.InvalidSchematicsNameException = InvalidSchematicsNameException;
class SchematicImpl {
    constructor(_description, _factory, // tslint:disable-line:no-any
        _collection, _engine) {
        this._description = _description;
        this._factory = _factory;
        this._collection = _collection;
        this._engine = _engine;
        if (!_description.name.match(/^[-_.a-zA-Z0-9]+$/)) {
            throw new InvalidSchematicsNameException(_description.name);
        }
    }
    get description() { return this._description; }
    get collection() { return this._collection; }
    call(options, host) {
        const context = {
            engine: this._engine,
            schematic: this,
            strategy: this._engine.defaultMergeStrategy,
        };
        const transformedOptions = this._engine.transformOptions(this, options);
        return host.concatMap(tree => {
            const result = this._factory(transformedOptions)(tree, context);
            if (result instanceof Observable_1.Observable) {
                return result;
            }
            else {
                return Observable_1.Observable.of(result);
            }
        });
    }
}
exports.SchematicImpl = SchematicImpl;
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoic2NoZW1hdGljLmpzIiwic291cmNlUm9vdCI6Ii9Vc2Vycy9oYW5zbC9Tb3VyY2VzL2RldmtpdC8iLCJzb3VyY2VzIjpbInBhY2thZ2VzL2FuZ3VsYXJfZGV2a2l0L3NjaGVtYXRpY3Mvc3JjL2VuZ2luZS9zY2hlbWF0aWMudHMiXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6Ijs7QUFBQTs7Ozs7O0dBTUc7QUFDSCxnREFBNkM7QUFDN0Msa0NBQWdDO0FBQ2hDLHVDQUFxQztBQUNyQyxzREFBdUQ7QUFZdkQsb0NBQTRDLFNBQVEseUJBQWE7SUFDL0QsWUFBWSxJQUFZO1FBQ3RCLEtBQUssQ0FBQyxpQ0FBaUMsSUFBSSxJQUFJLENBQUMsQ0FBQztJQUNuRCxDQUFDO0NBQ0Y7QUFKRCx3RUFJQztBQUdEO0lBR0UsWUFBb0IsWUFBMkQsRUFDM0QsUUFBMEIsRUFBRyw2QkFBNkI7UUFDMUQsV0FBZ0QsRUFDaEQsT0FBd0M7UUFIeEMsaUJBQVksR0FBWixZQUFZLENBQStDO1FBQzNELGFBQVEsR0FBUixRQUFRLENBQWtCO1FBQzFCLGdCQUFXLEdBQVgsV0FBVyxDQUFxQztRQUNoRCxZQUFPLEdBQVAsT0FBTyxDQUFpQztRQUMxRCxFQUFFLENBQUMsQ0FBQyxDQUFDLFlBQVksQ0FBQyxJQUFJLENBQUMsS0FBSyxDQUFDLG1CQUFtQixDQUFDLENBQUMsQ0FBQyxDQUFDO1lBQ2xELE1BQU0sSUFBSSw4QkFBOEIsQ0FBQyxZQUFZLENBQUMsSUFBSSxDQUFDLENBQUM7UUFDOUQsQ0FBQztJQUNILENBQUM7SUFFRCxJQUFJLFdBQVcsS0FBSyxNQUFNLENBQUMsSUFBSSxDQUFDLFlBQVksQ0FBQyxDQUFDLENBQUM7SUFDL0MsSUFBSSxVQUFVLEtBQUssTUFBTSxDQUFDLElBQUksQ0FBQyxXQUFXLENBQUMsQ0FBQyxDQUFDO0lBRTdDLElBQUksQ0FBeUIsT0FBZ0IsRUFBRSxJQUFzQjtRQUNuRSxNQUFNLE9BQU8sR0FBbUQ7WUFDOUQsTUFBTSxFQUFFLElBQUksQ0FBQyxPQUFPO1lBQ3BCLFNBQVMsRUFBRSxJQUFJO1lBQ2YsUUFBUSxFQUFFLElBQUksQ0FBQyxPQUFPLENBQUMsb0JBQW9CO1NBQzVDLENBQUM7UUFDRixNQUFNLGtCQUFrQixHQUFHLElBQUksQ0FBQyxPQUFPLENBQUMsZ0JBQWdCLENBQUMsSUFBSSxFQUFFLE9BQU8sQ0FBQyxDQUFDO1FBRXhFLE1BQU0sQ0FBQyxJQUFJLENBQUMsU0FBUyxDQUFDLElBQUk7WUFDeEIsTUFBTSxNQUFNLEdBQUcsSUFBSSxDQUFDLFFBQVEsQ0FBQyxrQkFBa0IsQ0FBQyxDQUFDLElBQUksRUFBRSxPQUFPLENBQUMsQ0FBQztZQUNoRSxFQUFFLENBQUMsQ0FBQyxNQUFNLFlBQVksdUJBQVUsQ0FBQyxDQUFDLENBQUM7Z0JBQ2pDLE1BQU0sQ0FBQyxNQUFNLENBQUM7WUFDaEIsQ0FBQztZQUFDLElBQUksQ0FBQyxDQUFDO2dCQUNOLE1BQU0sQ0FBQyx1QkFBVSxDQUFDLEVBQUUsQ0FBQyxNQUFNLENBQUMsQ0FBQztZQUMvQixDQUFDO1FBQ0gsQ0FBQyxDQUFDLENBQUM7SUFDTCxDQUFDO0NBQ0Y7QUFoQ0Qsc0NBZ0NDIiwic291cmNlc0NvbnRlbnQiOlsiLyoqXG4gKiBAbGljZW5zZVxuICogQ29weXJpZ2h0IEdvb2dsZSBJbmMuIEFsbCBSaWdodHMgUmVzZXJ2ZWQuXG4gKlxuICogVXNlIG9mIHRoaXMgc291cmNlIGNvZGUgaXMgZ292ZXJuZWQgYnkgYW4gTUlULXN0eWxlIGxpY2Vuc2UgdGhhdCBjYW4gYmVcbiAqIGZvdW5kIGluIHRoZSBMSUNFTlNFIGZpbGUgYXQgaHR0cHM6Ly9hbmd1bGFyLmlvL2xpY2Vuc2VcbiAqL1xuaW1wb3J0IHsgT2JzZXJ2YWJsZSB9IGZyb20gJ3J4anMvT2JzZXJ2YWJsZSc7XG5pbXBvcnQgJ3J4anMvYWRkL29ic2VydmFibGUvb2YnO1xuaW1wb3J0ICdyeGpzL2FkZC9vcGVyYXRvci9jb25jYXRNYXAnO1xuaW1wb3J0IHsgQmFzZUV4Y2VwdGlvbiB9IGZyb20gJy4uL2V4Y2VwdGlvbi9leGNlcHRpb24nO1xuaW1wb3J0IHsgVHJlZSB9IGZyb20gJy4uL3RyZWUvaW50ZXJmYWNlJztcbmltcG9ydCB7XG4gIENvbGxlY3Rpb24sXG4gIEVuZ2luZSxcbiAgUnVsZUZhY3RvcnksXG4gIFNjaGVtYXRpYyxcbiAgU2NoZW1hdGljRGVzY3JpcHRpb24sXG4gIFR5cGVkU2NoZW1hdGljQ29udGV4dCxcbn0gZnJvbSAnLi9pbnRlcmZhY2UnO1xuXG5cbmV4cG9ydCBjbGFzcyBJbnZhbGlkU2NoZW1hdGljc05hbWVFeGNlcHRpb24gZXh0ZW5kcyBCYXNlRXhjZXB0aW9uIHtcbiAgY29uc3RydWN0b3IobmFtZTogc3RyaW5nKSB7XG4gICAgc3VwZXIoYFNjaGVtYXRpY3MgaGFzIGludmFsaWQgbmFtZTogXCIke25hbWV9XCIuYCk7XG4gIH1cbn1cblxuXG5leHBvcnQgY2xhc3MgU2NoZW1hdGljSW1wbDxDb2xsZWN0aW9uVCBleHRlbmRzIG9iamVjdCwgU2NoZW1hdGljVCBleHRlbmRzIG9iamVjdD5cbiAgICBpbXBsZW1lbnRzIFNjaGVtYXRpYzxDb2xsZWN0aW9uVCwgU2NoZW1hdGljVD4ge1xuXG4gIGNvbnN0cnVjdG9yKHByaXZhdGUgX2Rlc2NyaXB0aW9uOiBTY2hlbWF0aWNEZXNjcmlwdGlvbjxDb2xsZWN0aW9uVCwgU2NoZW1hdGljVD4sXG4gICAgICAgICAgICAgIHByaXZhdGUgX2ZhY3Rvcnk6IFJ1bGVGYWN0b3J5PGFueT4sICAvLyB0c2xpbnQ6ZGlzYWJsZS1saW5lOm5vLWFueVxuICAgICAgICAgICAgICBwcml2YXRlIF9jb2xsZWN0aW9uOiBDb2xsZWN0aW9uPENvbGxlY3Rpb25ULCBTY2hlbWF0aWNUPixcbiAgICAgICAgICAgICAgcHJpdmF0ZSBfZW5naW5lOiBFbmdpbmU8Q29sbGVjdGlvblQsIFNjaGVtYXRpY1Q+KSB7XG4gICAgaWYgKCFfZGVzY3JpcHRpb24ubmFtZS5tYXRjaCgvXlstXy5hLXpBLVowLTldKyQvKSkge1xuICAgICAgdGhyb3cgbmV3IEludmFsaWRTY2hlbWF0aWNzTmFtZUV4Y2VwdGlvbihfZGVzY3JpcHRpb24ubmFtZSk7XG4gICAgfVxuICB9XG5cbiAgZ2V0IGRlc2NyaXB0aW9uKCkgeyByZXR1cm4gdGhpcy5fZGVzY3JpcHRpb247IH1cbiAgZ2V0IGNvbGxlY3Rpb24oKSB7IHJldHVybiB0aGlzLl9jb2xsZWN0aW9uOyB9XG5cbiAgY2FsbDxPcHRpb25UIGV4dGVuZHMgb2JqZWN0PihvcHRpb25zOiBPcHRpb25ULCBob3N0OiBPYnNlcnZhYmxlPFRyZWU+KTogT2JzZXJ2YWJsZTxUcmVlPiB7XG4gICAgY29uc3QgY29udGV4dDogVHlwZWRTY2hlbWF0aWNDb250ZXh0PENvbGxlY3Rpb25ULCBTY2hlbWF0aWNUPiA9IHtcbiAgICAgIGVuZ2luZTogdGhpcy5fZW5naW5lLFxuICAgICAgc2NoZW1hdGljOiB0aGlzLFxuICAgICAgc3RyYXRlZ3k6IHRoaXMuX2VuZ2luZS5kZWZhdWx0TWVyZ2VTdHJhdGVneSxcbiAgICB9O1xuICAgIGNvbnN0IHRyYW5zZm9ybWVkT3B0aW9ucyA9IHRoaXMuX2VuZ2luZS50cmFuc2Zvcm1PcHRpb25zKHRoaXMsIG9wdGlvbnMpO1xuXG4gICAgcmV0dXJuIGhvc3QuY29uY2F0TWFwKHRyZWUgPT4ge1xuICAgICAgY29uc3QgcmVzdWx0ID0gdGhpcy5fZmFjdG9yeSh0cmFuc2Zvcm1lZE9wdGlvbnMpKHRyZWUsIGNvbnRleHQpO1xuICAgICAgaWYgKHJlc3VsdCBpbnN0YW5jZW9mIE9ic2VydmFibGUpIHtcbiAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICAgIH0gZWxzZSB7XG4gICAgICAgIHJldHVybiBPYnNlcnZhYmxlLm9mKHJlc3VsdCk7XG4gICAgICB9XG4gICAgfSk7XG4gIH1cbn1cbiJdfQ==