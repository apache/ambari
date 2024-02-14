import { NgModule } from '@angular/core';

import {
    IsGreaterPipe,
    IsGreaterOrEqualPipe,
    IsLessPipe,
    IsLessOrEqualPipe,
    IsEqualPipe,
    IsNotEqualPipe,
    IsIdenticalPipe,
    IsNotIdenticalPipe,
} from './conditions.pipe';

import {
    IsNullPipe,
    IsNilPipe,
    IsUndefinedPipe,
    IsFunctionPipe,
    IsNumberPipe,
    IsStringPipe,
    IsArrayPipe,
    IsObjectPipe,
    IsDefinedPipe
} from './types.pipe';

@NgModule({
    declarations: [
        IsGreaterPipe,
        IsGreaterOrEqualPipe,
        IsLessPipe,
        IsLessOrEqualPipe,
        IsEqualPipe,
        IsNotEqualPipe,
        IsIdenticalPipe,
        IsNotIdenticalPipe,
        IsNilPipe,
        IsNullPipe,
        IsUndefinedPipe,
        IsFunctionPipe,
        IsNumberPipe,
        IsStringPipe,
        IsArrayPipe,
        IsObjectPipe,
        IsDefinedPipe
    ],
    exports: [
        IsGreaterPipe,
        IsGreaterOrEqualPipe,
        IsLessPipe,
        IsLessOrEqualPipe,
        IsEqualPipe,
        IsNotEqualPipe,
        IsIdenticalPipe,
        IsNotIdenticalPipe,
        IsNilPipe,
        IsNullPipe,
        IsUndefinedPipe,
        IsFunctionPipe,
        IsNumberPipe,
        IsStringPipe,
        IsArrayPipe,
        IsObjectPipe,
        IsDefinedPipe
    ]
})
export class NgBooleanPipesModule {}

export * from './conditions.pipe';
export * from './types.pipe';
