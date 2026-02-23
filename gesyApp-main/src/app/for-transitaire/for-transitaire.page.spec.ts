import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ForTransitairePage } from './for-transitaire.page';

describe('ForTransitairePage', () => {
  let component: ForTransitairePage;
  let fixture: ComponentFixture<ForTransitairePage>;

  beforeEach(() => {
    fixture = TestBed.createComponent(ForTransitairePage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
