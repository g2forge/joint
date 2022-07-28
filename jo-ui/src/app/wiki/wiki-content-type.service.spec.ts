import { TestBed } from '@angular/core/testing';

import { WikiContentTypeService } from './wiki-content-type.service';

describe('WikiContentTypeService', () => {
  let service: WikiContentTypeService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(WikiContentTypeService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
