import { NgModule } from '@angular/core';
import { BrowserModule, Title } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { WikiComponent } from './wiki/wiki.component';
import { NotfoundComponent } from './notfound/notfound.component';
import { ToolbarComponent } from './toolbar/toolbar.component';
import { SafeHtmlPipe } from './safe-html.pipe';

import { HttpClientModule } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';


import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatToolbarModule } from '@angular/material/toolbar';

@NgModule( {
    declarations: [
        AppComponent,
        NotfoundComponent,
        SafeHtmlPipe,
        ToolbarComponent,
        WikiComponent
    ],
    imports: [
        BrowserModule,
        AppRoutingModule,

        HttpClientModule,
        BrowserAnimationsModule,
        FormsModule,
        ReactiveFormsModule,

        MatFormFieldModule,
        MatIconModule,
        MatInputModule,
        MatPaginatorModule,
        MatSortModule,
        MatTableModule,
        MatToolbarModule,
    ],
    providers: [
        Title
    ],
    bootstrap: [AppComponent]
} )
export class AppModule { }
