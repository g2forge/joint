import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';

import { NotfoundComponent } from './notfound.component';
import { ToolbarComponent } from '../toolbar/toolbar.component';

describe( 'NotfoundComponent', () => {
    let component: NotfoundComponent;
    let fixture: ComponentFixture<NotfoundComponent>;
    let route: ActivatedRoute;

    beforeEach( async () => {
        await TestBed.configureTestingModule( {
            declarations: [
                ToolbarComponent,
                NotfoundComponent
            ],
            imports: [RouterTestingModule, HttpClientTestingModule]
        } ).compileComponents();
    } );

    beforeEach( () => {
        fixture = TestBed.createComponent( NotfoundComponent );
        component = fixture.componentInstance;
        fixture.detectChanges();
    } );

    it( 'should create', () => {
        expect( component ).toBeTruthy();
    } );
} );
