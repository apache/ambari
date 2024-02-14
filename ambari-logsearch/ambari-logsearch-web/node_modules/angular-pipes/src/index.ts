import { NgModule } from '@angular/core';

import { NgAggregatePipesModule } from './aggregate';
import { NgArrayPipesModule } from './array';
import { NgBooleanPipesModule } from './boolean';
import { NgMathPipesModule } from './math';
import { NgObjectPipesModule } from './object';
import { NgStringPipesModule } from './string';


@NgModule({
  exports: [
    NgArrayPipesModule,
    NgMathPipesModule,
    NgBooleanPipesModule,
    NgStringPipesModule,
    NgObjectPipesModule,
    NgAggregatePipesModule
  ]
})
export class NgPipesModule {}

// Export individual pipes and modules
export * from './aggregate';
export * from './array';
export * from './boolean';
export * from './math';
export * from './object';
export * from './string';
