import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Title } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';

import { ToolbarComponent } from './toolbar.component';

class MockTitle {
    setTitle( title: string ): void { }
}

describe( 'ToolbarComponent', () => {
    let component: ToolbarComponent;
    let fixture: ComponentFixture<ToolbarComponent>;

    beforeEach( async () => {
        let title = new MockTitle();
        await TestBed.configureTestingModule( {
            declarations: [ToolbarComponent],
            imports: [RouterTestingModule, HttpClientTestingModule],
            providers: [{ provide: Title, useValue: title }]
        } ).compileComponents();
    } );

    beforeEach( () => {
        fixture = TestBed.createComponent( ToolbarComponent );
        component = fixture.componentInstance;
        fixture.detectChanges();
    } );

    it( 'should create', () => {
        expect( component ).toBeTruthy();
    } );
} );
