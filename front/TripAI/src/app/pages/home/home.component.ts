import { Component, inject, signal } from '@angular/core';
import { Meta } from '@angular/platform-browser';
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
  private meta = inject(Meta);

  theme = signal<'light' | 'dark'>(
    (localStorage.getItem('gymai_theme') as 'light' | 'dark') || 'dark'
  );

  model: GymProfile = {
    age: null,
    height: null,
    weight: null,
    goal: '',
    activity: '',
    medicalNotes: '',
  };

  isSubmitting = signal(false);

  // Touch handling para ocultar teclado con swipe down
  private touchStartY = 0;
  private touchCurrentY = 0;
  private isInputFocused = false;

  constructor() {
    const existing = this.profileService.profile();
    if (existing) {
      this.model = { ...existing };
    }

    // Restaurar tema al iniciar
    const currentTheme = this.theme();
    document.documentElement.setAttribute('data-theme', currentTheme);
    this.updateMetaThemeColor(currentTheme);
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
      this.isSubmitting.set(true);

      // Simular un pequeño retardo para feedback visual si es muy rápido, 
      // o simplemente proceder (aquí es síncrono localStorage, pero el reset es async)
      this.profileService.saveProfile(this.model);

      try {
        // Reiniciar conversación completa antes de ir al chat
        await this.chatService.resetConversation();
        this.router.navigate(['/chat']);
      } catch (error) {
        console.error('Error resetting conversation', error);
        this.isSubmitting.set(false);
      }
    } else {
      this.profileService.clearProfile();
      this.isSubmitting.set(true);
      await this.chatService.resetConversation();
      this.router.navigate(['/chat']);
    }
  }

  toggleTheme() {
    const next = this.theme() === 'dark' ? 'light' : 'dark';
    this.theme.set(next);
    localStorage.setItem('gymai_theme', next);

    document.documentElement.setAttribute('data-theme', next);
    this.updateMetaThemeColor(next);
  }

  private updateMetaThemeColor(theme: 'light' | 'dark') {
    const color = theme === 'dark' ? '#1B232B' : '#ffffff';

    // Remove all existing theme-color tags (including those with media queries)
    // to prevent conflicts and forcefully override system preference
    const existingTags = document.querySelectorAll('meta[name="theme-color"]');
    existingTags.forEach(tag => tag.remove());

    // Add a single, definitive tag
    this.meta.addTag({ name: 'theme-color', content: color });
  }

  // ===== Swipe down para ocultar teclado =====

  onTouchStart(event: TouchEvent) {
    this.touchStartY = event.touches[0].clientY;
    this.touchCurrentY = this.touchStartY;
    this.isInputFocused =
      document.activeElement instanceof HTMLInputElement ||
      document.activeElement instanceof HTMLTextAreaElement;
  }

  onTouchMove(event: TouchEvent) {
    this.touchCurrentY = event.touches[0].clientY;
  }

  onTouchEnd() {
    const swipeDistance = this.touchCurrentY - this.touchStartY;
    const minSwipeDistance = 50;

    if (swipeDistance > minSwipeDistance && this.isInputFocused) {
      if (document.activeElement instanceof HTMLElement) {
        document.activeElement.blur();
      }
    }

    this.touchStartY = 0;
    this.touchCurrentY = 0;
    this.isInputFocused = false;
  }
}
