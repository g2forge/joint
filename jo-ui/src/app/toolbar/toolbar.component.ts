import { Component, OnInit } from '@angular/core';

import { Title } from '@angular/platform-browser';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';

interface JointConfig {
    title: string;
}

@Component( {
    selector: 'app-toolbar',
    templateUrl: './toolbar.component.html',
    styleUrls: ['./toolbar.component.scss']
} )
export class ToolbarComponent implements OnInit {
    config: JointConfig | null = null;

    constructor(
        private http: HttpClient,
        private titleService: Title
    ) { }

    ngOnInit(): void {
        this.http.get<JointConfig>( "assets/config.json" ).pipe(
            catchError( ( error: HttpErrorResponse ) => {
                return of( { title: "Joint" } );
            } )
        ).subscribe( ( data: JointConfig ) => {
            this.config = data;
            if ( this.config != null ) this.titleService.setTitle( this.config!.title );
        } );
    }
}
