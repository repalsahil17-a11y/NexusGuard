import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ScanResult } from './scan-result';

describe('ScanResult', () => {
  let component: ScanResult;
  let fixture: ComponentFixture<ScanResult>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ScanResult],
    }).compileComponents();

    fixture = TestBed.createComponent(ScanResult);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
