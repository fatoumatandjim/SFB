import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConstrucPage } from './construc.page';

describe('ConstrucPage', () => {
  let component: ConstrucPage;
  let fixture: ComponentFixture<ConstrucPage>;

  beforeEach(() => {
    fixture = TestBed.createComponent(ConstrucPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
