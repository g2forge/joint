import { DomSanitizer, SafeHtml } from '@angular/platform-browser'
import { Pipe, PipeTransform } from '@angular/core';

@Pipe( { name: 'safeHtml' } )
export class SafeHtmlPipe implements PipeTransform {
    constructor( private sanitizer: DomSanitizer | null ) { }

    transform( value: string ): SafeHtml {
        return this.sanitizer == null ? value : this.sanitizer.bypassSecurityTrustHtml( value );
    }
}
