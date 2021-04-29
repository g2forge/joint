import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { WikiComponent } from './wiki.component';
import { SafeHtmlPipe } from '../safe-html.pipe';

describe('WikiComponent', () => {
	let component: WikiComponent;
	let fixture: ComponentFixture<WikiComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [WikiComponent, SafeHtmlPipe],
			imports: [RouterTestingModule, HttpClientTestingModule]
		}).compileComponents();
	});

	beforeEach(() => {
		fixture = TestBed.createComponent(WikiComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	it('should recognize http', () => {
		expect(component.hasScheme("http://example.com/")).toBeTrue();
	});

	it('should recognize https', () => {
		expect(component.hasScheme("https://example.com/")).toBeTrue();
	});

	it('should recognize mailto', () => {
		expect(component.hasScheme("mailto:user@example.com")).toBeTrue();
	});

	it('should recognize non-scheme urls', () => {
		expect(component.hasScheme("dir/file.html")).toBeFalse();
	});
});
