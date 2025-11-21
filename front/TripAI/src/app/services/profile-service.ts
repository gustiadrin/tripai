import { Injectable, signal } from '@angular/core';

export type GymGoal = 'adelgazar' | 'ganar_masa' | 'definir' | 'mantener' | '';

export interface GymProfile {
  age: number | null;
  height: number | null; // cm
  weight: number | null; // kg
  goal: GymGoal;
  activity: string; // texto libre o etiqueta
  medicalNotes: string;
}

const STORAGE_KEY = 'gymai_profile';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  profile = signal<GymProfile | null>(null);

  constructor() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) {
        this.profile.set(JSON.parse(raw) as GymProfile);
      }
    } catch {}
  }

  saveProfile(p: GymProfile) {
    this.profile.set(p);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(p));
    } catch {}
  }

  clearProfile() {
    this.profile.set(null);
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {}
  }
}
