import { deepEqual, unwrapDeep, wrapDeep } from './utils';
    

describe('CapitalizePipe', () => {
  
  it('Should return false', () => {
    expect(
    deepEqual({ x: 1 }, { x: 1, y: 2 })
    ).toBeFalsy();
  });

  it('Should return the input', () => {
    expect(unwrapDeep(1)).toEqual(1);
  });
   
});