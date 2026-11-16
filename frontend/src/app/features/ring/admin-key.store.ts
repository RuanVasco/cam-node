import { Injectable } from '@angular/core';

const STORAGE_KEY = 'cam-node.admin-key';

/**
 * Rule: SPEC-010 #7. Session storage belongs to one tab and is cleared when the tab closes, which is exactly how
 * long the spec lets the key be kept. Temporary until login exists (SPEC-004).
 *
 * Storage can be unavailable (privacy modes, blocked site data); the key then lives in memory for this page only.
 */
@Injectable({ providedIn: 'root' })
export class AdminKeyStore {
  private inMemory: string | null = null;

  get(): string | null {
    try {
      return sessionStorage.getItem(STORAGE_KEY) ?? this.inMemory;
    } catch {
      return this.inMemory;
    }
  }

  set(key: string): void {
    this.inMemory = key;
    try {
      sessionStorage.setItem(STORAGE_KEY, key);
    } catch {
      // Kept in memory only.
    }
  }

  clear(): void {
    this.inMemory = null;
    try {
      sessionStorage.removeItem(STORAGE_KEY);
    } catch {
      // Nothing stored to clear.
    }
  }
}
