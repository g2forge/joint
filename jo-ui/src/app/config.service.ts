import { Injectable } from '@angular/core';

import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

export interface JointConfig {
	title: string;
	rewrites?: any;
}

@Injectable({
	providedIn: 'root'
})
export class ConfigService {
	constructor(
		private http: HttpClient
	) { }

	getConfig(): Observable<JointConfig> {
		return this.http.get<JointConfig>("assets/config.json").pipe(
			catchError((error: HttpErrorResponse) => {
				console.error(error);
				return of({ title: "Joint" });
			})
		);
	}
}
