import { Pipe, PipeTransform  } from '@angular/core';
import { createRound, isString } from '../utils/utils';

@Pipe({
  name: 'floor'
})
export class FloorPipe implements PipeTransform {
  
  transform (value: any, precision: any = 0): any {
    
    if (isString(precision)) {
      precision = parseInt(precision);
    }
    
    return createRound('floor')(value, precision);
  }
}