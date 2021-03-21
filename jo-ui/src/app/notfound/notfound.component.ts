import { Component, OnInit } from '@angular/core';

import { Router, NavigationEnd } from '@angular/router';

import { ToolbarComponent } from '../toolbar/toolbar.component';

@Component( {
    selector: 'app-notfound',
    templateUrl: './notfound.component.html',
    styleUrls: ['./notfound.component.scss']
} )
export class NotfoundComponent implements OnInit {
    url: string | null = null;

    constructor(
        private router: Router
    ) { }

    ngOnInit(): void {
        this.load( this.router.url );
        this.router.events.subscribe( event => {
            if ( event instanceof NavigationEnd ) this.load( event.url );
        } );
    }

    load( url: string ): void {
        this.url = url;
    }
}
