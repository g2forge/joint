import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';

import { ToolbarComponent } from './toolbar.component';

import { ConfigService, JointConfig } from '../config.service';
import { Observable, of } from 'rxjs';

class MockTitle {
	setTitle(title: string): void { }
}

class MockConfigService {
	getConfig(): Observable<JointConfig> {
		return of({ "title": "Test Title" })
	}
}

describe('ToolbarComponent', () => {
	let component: ToolbarComponent;
	let fixture: ComponentFixture<ToolbarComponent>;

	beforeEach(async () => {
		let title = new MockTitle();
		let configService = new MockConfigService();
		await TestBed.configureTestingModule({
			declarations: [ToolbarComponent],
			imports: [RouterTestingModule],
			providers: [{ provide: Title, useValue: title }, { provide: ConfigService, useValue: configService }]
		}).compileComponents();
	});

	beforeEach(() => {
		fixture = TestBed.createComponent(ToolbarComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});
});
