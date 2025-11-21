import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProfileService, GymProfile } from '../../services/profile-service';
import { ChatService } from '../../services/chat-service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './home.component.html',
})
export class HomeComponent {
  private profileService = inject(ProfileService);
  private router = inject(Router);
  private chatService = inject(ChatService);

  theme = signal<'light' | 'dark'>(
    (localStorage.getItem('tripai_theme') as 'light' | 'dark') || 'dark'
  );

  model: GymProfile = {
    age: null,
    height: null,
    weight: null,
    goal: '',
    activity: '',
    medicalNotes: '',
  };

  saved = false;

  constructor() {
    const existing = this.profileService.profile();
    if (existing) {
      this.model = { ...existing };
    }

    document.documentElement.setAttribute('data-theme', this.theme());
  }

  async save() {
    const p = this.model;
    const hasData =
      p.age != null ||
      p.height != null ||
      p.weight != null ||
      !!p.goal ||
      !!p.activity ||
      !!p.medicalNotes;

    if (hasData) {
      this.profileService.saveProfile(this.model);
    } else {
      this.profileService.clearProfile();
    }

    this.saved = true;
    setTimeout(() => (this.saved = false), 2000);

    // Reiniciar conversación completa antes de ir al chat
    await this.chatService.resetConversation();

    this.router.navigate(['/chat']);
  }

  toggleTheme() {
    const next = this.theme() === 'dark' ? 'light' : 'dark';
    this.theme.set(next);
    localStorage.setItem('tripai_theme', next);
    document.documentElement.setAttribute('data-theme', next);
  }
}
