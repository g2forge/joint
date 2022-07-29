import { Injectable } from '@angular/core';

import { HttpClient, HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { of, Observable } from 'rxjs';
import { catchError, map, shareReplay } from 'rxjs/operators';

@Injectable( {
    providedIn: 'root'
} )
export class WikiContentTypeService {
    private cache: Map<string, Observable<string | null>> = new Map();

    constructor(
        private http: HttpClient
    ) { }

    getContentType( url: string | null ): Observable<string | null> {
        if ( url == null ) return of( null );

        var retVal = this.cache.get( url );
        if ( retVal !== undefined ) return retVal;

        var contentType = this.http.head( url, { observe: 'response' } ).pipe(
            catchError( ( error: HttpErrorResponse ) => {
                console.log( error );
                return of( null );
            } ),
            map( ( response: HttpResponse<Object> | null ) => {
                if ( response == null ) return null;

                var contentType = response.headers.get( "Content-Type" );
                if ( contentType == null ) return null;

                var semiColon = contentType.indexOf( ';' );
                return semiColon >= 0 ? contentType.substring( 0, semiColon ).trim() : contentType;
            } ),
            shareReplay( 1 )
        );
        this.cache.set( url, contentType );
        return contentType;
    }
}
