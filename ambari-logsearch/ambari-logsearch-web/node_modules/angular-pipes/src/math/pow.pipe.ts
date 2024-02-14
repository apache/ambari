import { Pipe, PipeTransform  } from '@angular/core';
import { isNumberFinite } from '../utils/utils';

@Pipe({
  name: 'pow'
})
export class PowPipe implements PipeTransform {
  
  transform (input: any, power: number = 2): any {
    
    if (!isNumberFinite(input)) {
      return 'NaN';
    }
    
    return Math.pow(input, power);
  }
}