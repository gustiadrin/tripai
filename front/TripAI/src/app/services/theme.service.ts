import { Injectable, signal, effect, inject } from '@angular/core';
import { Meta } from '@angular/platform-browser';

export type Theme = 'light' | 'dark';

@Injectable({
    providedIn: 'root'
})
export class ThemeService {
    private meta = inject(Meta);

    // Dracula base-100 is #282a36, Light is #ffffff
    private readonly colors = {
        light: '#ffffff',
        dark: '#282a36'
    };

    theme = signal<Theme>(this.getInitialTheme());

    constructor() {
        effect(() => {
            const current = this.theme();
            this.applyTheme(current);
        });
    }

    toggleTheme() {
        this.theme.update(t => t === 'dark' ? 'light' : 'dark');
    }

    private getInitialTheme(): Theme {
        const saved = localStorage.getItem('gymai_theme') as Theme;
        if (saved && (saved === 'light' || saved === 'dark')) {
            return saved;
        }
        return 'dark'; // Default to dark (Dracula)
    }

    private applyTheme(theme: Theme) {
        // 1. Update HTML attribute for DaisyUI
        document.documentElement.setAttribute('data-theme', theme);

        // 2. Persist
        localStorage.setItem('gymai_theme', theme);

        // 3. Update Meta Theme Color
        const color = this.colors[theme];
        this.meta.updateTag({ name: 'theme-color', content: color });
        this.meta.updateTag({ name: 'apple-mobile-web-app-status-bar-style', content: 'default' });
    }
}
