import { Pipe, PipeTransform  } from '@angular/core';

@Pipe({
  name: 'range'
})
export class RangePipe implements PipeTransform {
  
  transform (_input: any, size: number = 0, start: number = 1, step: number = 1): any {
    
    const range: number[] = [];
    for (let length = 0; length < size; ++length) {
      range.push(start);
      start += step;
    }
    
    return range;
  }
}
