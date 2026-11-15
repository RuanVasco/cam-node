import { TestBed } from '@angular/core/testing';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';
import { NEVER, of } from 'rxjs';

import { RingApi } from '../ring-api';
import { RingStream } from '../ring-stream';
import { RingHistory } from './ring-history';

describe('RingHistory', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RingHistory],
      providers: [
        { provide: RingApi, useValue: { fetchPage: () => of({ rings: [], totalElements: 0 }) } },
        { provide: RingStream, useValue: { connect: () => NEVER } },
      ],
    });
    const sanitizer = TestBed.inject(DomSanitizer);
    TestBed.inject(MatIconRegistry).addSvgIconLiteral(
      'notifications-off',
      sanitizer.bypassSecurityTrustHtml('<svg></svg>'),
    );
  });

  it('SPEC-002 #10: without rings shows the empty message', async () => {
    const fixture = TestBed.createComponent(RingHistory);

    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).toContain('Nenhum toque registrado ainda.');
  });
});
