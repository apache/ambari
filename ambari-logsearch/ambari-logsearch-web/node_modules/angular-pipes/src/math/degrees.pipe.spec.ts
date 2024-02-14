import { DegreesPipe } from './degrees.pipe';
        


describe('DegreesPipe', () => {
    
    let pipe: DegreesPipe;
    
    beforeEach(() => {
       pipe = new DegreesPipe(); 
    });
    
    it ('Should transfrom the radians to degrees', () => {
       const radians = Math.PI;
       expect(pipe.transform(radians)).toEqual(180);
    });

    it('Should return NaN', () => {
        expect(pipe.transform('a')).toEqual('NaN');
    });
});