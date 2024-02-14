import { CeilPipe } from './ceil.pipe';
        

describe('CeilPipe', () => {
    
    let pipe: CeilPipe;
    
    beforeEach(() => {
       pipe = new CeilPipe(); 
    });
    
    it('Should return 4', () => {
        
        expect(pipe.transform(3.4, 0)).toEqual(4);
    });
    
    it('Should return 1', () => {
        
        expect(pipe.transform(1, 0)).toEqual(1);
    });
    
    it('Should return 1', () => {
        
        expect(pipe.transform(0.65, 0)).toEqual(1);
    });
    
    it('Should return 1.5', () => {
       
       expect(pipe.transform(1.5, 1)).toEqual(1.5); 
    });
    
    
    it('Should return 1.55', () => {
       
       expect(pipe.transform(1.5444, 2)).toEqual(1.55); 
    });

    it('Should return 1.55 #2', () => {
       
       expect(pipe.transform(1.5444,'2')).toEqual(1.55); 
    });
});