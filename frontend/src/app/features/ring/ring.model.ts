export interface Ring {
  readonly id: string;
  readonly ringedAt: Date;
  readonly photoUrl: string;
}

export interface RingPage {
  readonly rings: readonly Ring[];
  readonly totalElements: number;
}

export interface RingDay {
  readonly key: string;
  readonly label: string;
  readonly rings: readonly Ring[];
}

export type ConnectionStatus = 'connecting' | 'live' | 'reconnecting';

export type RingStreamEvent =
  | { readonly kind: 'open' }
  | { readonly kind: 'error' }
  | { readonly kind: 'ring'; readonly ring: Ring }
  | { readonly kind: 'removed'; readonly id: string };

/** The admin key was missing or wrong (SPEC-010 #7). */
export class InvalidAdminKeyError extends Error {
  constructor() {
    super('Invalid admin key');
    this.name = 'InvalidAdminKeyError';
  }
}
