export interface TrashedRing {
  readonly id: string;
  readonly ringedAt: Date;
  readonly removedAt: Date;
  readonly photoUrl: string;
}

export interface TrashPage {
  readonly rings: readonly TrashedRing[];
  readonly totalElements: number;
}

export type TrashState = 'loading' | 'locked' | 'failed' | 'ready';

/** The admin key was missing or wrong (SPEC-010 #7). */
export class InvalidAdminKeyError extends Error {
  constructor() {
    super('Invalid admin key');
    this.name = 'InvalidAdminKeyError';
  }
}
