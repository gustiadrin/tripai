import {
  Component,
  ElementRef,
  ViewChild,
  inject,
  signal,
  effect,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MarkdownModule } from 'ngx-markdown';
import { RouterLink } from '@angular/router';
import { ChatService, ChatMessage } from '../../services/chat-service';
import { environment } from '../../../environments/environment';
import { ProfileService } from '../../services/profile-service';

@Component({
  selector: 'app-chat-assistant',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, MarkdownModule],
  templateUrl: './chat-assistant.html',
})
export class ChatAssistant {
  draft = signal('');
  form: FormGroup;
  chat = inject(ChatService);
  private profileService = inject(ProfileService);
  private fb = inject(FormBuilder);
  theme = signal<'light' | 'dark'>(
    (localStorage.getItem('gymai_theme') as 'light' | 'dark') || 'dark'
  );

  @ViewChild('messagesContainer')
  messagesContainer?: ElementRef<HTMLDivElement>;

  userNearBottom = signal(true);
  showScrollButton = signal(false);

  isTyping = signal(false);
  pdfGenerating = signal<'routine' | 'diet' | null>(null);

  private previousMessageCount = 0;

  // Touch handling para ocultar teclado con swipe down
  private touchStartY = 0;
  private touchCurrentY = 0;
  private isInputFocused = false;

  // Track messages by ID para evitar re-renders innecesarios
  trackByMessageId(index: number, message: ChatMessage): string {
    return message.id || `${index}-${message.timestamp}`;
  }

  private generateId(): string {
    return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }

  constructor() {
    this.form = this.fb.group({
      message: [''],
    });
    this.chat.loadMessagesOnce().then(() => {
      const profile = this.profileService.profile();

      const hasData =
        !!profile &&
        (profile.age != null ||
          profile.height != null ||
          profile.weight != null ||
          !!profile.goal ||
          !!profile.activity ||
          !!profile.medicalNotes);
      const current = this.chat.messages();

      if (!hasData) {
        // Caso sin perfil: mostrar saludo estándar
        const welcomeTextNoProfile =
          'Hola, soy GymAI. Cuéntame tu objetivo (adelgazar, ganar músculo, definir, mantener), tu experiencia en el gimnasio y cuántos días puedes entrenar, y te ayudaré con tus rutinas y dieta.';

        if (current.length === 0) {
          // No hay historial: creamos saludo inicial
          this.chat.messages.set([
            {
              id: this.generateId(),
              sender: 'bot',
              content: welcomeTextNoProfile,
              timestamp: new Date().toISOString(),
            },
          ]);
        } else if (
          current[0].sender === 'bot' &&
          current[0].content.startsWith('Hola, soy GymAI.')
        ) {
          // Ya había un saludo antiguo estándar: lo actualizamos por si cambia el texto
          const updated = [...current];
          updated[0] = {
            ...updated[0],
            content: welcomeTextNoProfile,
          };
          this.chat.messages.set(updated);
        }
      } else if (hasData && current.length === 0) {
        // Caso con perfil y conversación recién reseteada: no mostramos saludo, arrancamos
        // directamente una respuesta automática basada en el perfil.
        this.startConversationWithProfile(profile!);
      }
    });
    document.documentElement.setAttribute('data-theme', this.theme());

    // ✅ Solo scrollea si entró un mensaje nuevo
    effect(() => {
      const messages = this.chat.messages();
      const currentCount = messages.length;

      if (currentCount > this.previousMessageCount) {
        // Siempre hacer scroll hasta el final cuando entra un mensaje nuevo
        queueMicrotask(() => this.scrollToBottom(true));
      }

      this.previousMessageCount = currentCount;
    });
  }

  private buildCommandMessage(kind: 'routine' | 'diet'): string {
    const baseInstruction =
      kind === 'routine'
        ? 'Genera un plan de ENTRENAMIENTO detallado. IMPORTANTE: Estructura los días en una TABLA Markdown con las columnas: Día, Ejercicio, Series, Repeticiones. Usa encabezados claros (##) para las secciones principales.'
        : 'Genera un plan de DIETA detallado. IMPORTANTE: Estructura las comidas en una TABLA Markdown con las columnas: Comida, Opción 1, Opción 2, Calorías aprox. Usa encabezados claros (##) para las secciones principales.';

    const profile = this.profileService.profile();
    if (!profile) {
      return baseInstruction;
    }

    const parts: string[] = [];
    if (profile.age != null) parts.push(`edad: ${profile.age} años`);
    if (profile.height != null) parts.push(`altura: ${profile.height} cm`);
    if (profile.weight != null) parts.push(`peso: ${profile.weight} kg`);
    if (profile.goal) parts.push(`objetivo: ${profile.goal}`);
    if (profile.activity) parts.push(`actividad diaria: ${profile.activity}`);
    if (profile.medicalNotes)
      parts.push(`datos médicos: ${profile.medicalNotes}`);

    if (!parts.length) {
      return baseInstruction;
    }

    const profileText =
      'Perfil del usuario para entreno y dieta: ' + parts.join(', ') + '.';
    return profileText + '\n\nInstrucción para GymAI: ' + baseInstruction;
  }

  generateRoutine() {
    const command = this.buildCommandMessage('routine');
    this.sendSpecialCommand(command, 'routine');
  }

  generateDiet() {
    const command = this.buildCommandMessage('diet');
    this.sendSpecialCommand(command, 'diet');
  }

  private async triggerPdfDownload(kind: 'routine' | 'diet') {
    try {
      const res = await fetch(`${environment.apiBaseUrl}/export/last-plan.pdf`);
      if (!res.ok) return;
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download =
        kind === 'routine' ? 'plan-rutina-gymai.pdf' : 'plan-dieta-gymai.pdf';
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } catch (e) {
      console.error('Error descargando PDF', e);
    } finally {
      this.pdfGenerating.set(null);
    }
  }

  private sendSpecialCommand(command: string, kind: 'routine' | 'diet') {
    const text = command.trim();
    if (!text) return;

    // Para las órdenes especiales (rutina/dieta) no mostramos el prompt ni la
    // respuesta en el chat: solo usamos la respuesta para generar el PDF.
    const messageForGemini = text;

    this.pdfGenerating.set(kind);

    this.chat.sendMessageStream(
      messageForGemini,
      () => {
        // ignoramos los chunks en pantalla, pero el backend sigue guardando
        // el último mensaje del bot para el PDF
      },
      () => {
        // cuando termina de llegar la respuesta, disparamos la descarga
        this.triggerPdfDownload(kind);
      },
      (err) => {
        console.error('Error en stream', err);
      }
    );
  }

  visibleMessages(): ChatMessage[] {
    return this.chat.messages().filter((m) => !this.isSystemCommandMessage(m));
  }

  private isSystemCommandMessage(m: ChatMessage): boolean {
    return (
      m.sender === 'user' &&
      m.content.startsWith('Perfil del usuario para entreno y dieta:')
    );
  }

  ngAfterViewInit() {
    const el = this.messagesContainer?.nativeElement;
    if (!el) return;

    el.addEventListener('scroll', () => {
      const distanceFromBottom =
        el.scrollHeight - el.scrollTop - el.clientHeight;
      const isNearBottom = distanceFromBottom < 100;
      this.userNearBottom.set(isNearBottom);
      this.showScrollButton.set(!isNearBottom);
    });
  }

  scrollToBottom(force = false) {
    const el = this.messagesContainer?.nativeElement;
    if (!el) return;

    el.scrollTo({
      top: el.scrollHeight,
      behavior: 'smooth',
    });
  }

  async send() {
    const rawValue = this.form.get('message')?.value ?? '';
    const text = (rawValue as string).trim();
    if (!text) return;

    // Marcar actividad para evitar polling innecesario
    this.chat.updateActivity();

    this.chat.messages.update((m: ChatMessage[]) => [
      ...m,
      {
        id: this.generateId(),
        sender: 'user',
        content: text,
        timestamp: new Date().toISOString(),
      },
    ]);

    this.draft.set('');
    this.form.reset({ message: '' });

    const profile = this.profileService.profile();
    let messageForGemini = text;

    if (profile) {
      const parts: string[] = [];
      if (profile.age != null) {
        parts.push(`edad: ${profile.age} años`);
      }
      if (profile.height != null) {
        parts.push(`altura: ${profile.height} cm`);
      }
      if (profile.weight != null) {
        parts.push(`peso: ${profile.weight} kg`);
      }
      if (profile.goal) {
        parts.push(`objetivo: ${profile.goal}`);
      }
      if (profile.activity) {
        parts.push(`actividad diaria: ${profile.activity}`);
      }
      if (profile.medicalNotes) {
        parts.push(`datos médicos: ${profile.medicalNotes}`);
      }

      if (parts.length) {
        const profileText =
          'Perfil del usuario para entreno y dieta: ' + parts.join(', ') + '.';
        messageForGemini = profileText + '\n\nMensaje del usuario: ' + text;
      }
    }

    const botIndex = this.chat.messages().length;
    this.chat.messages.update((m: ChatMessage[]) => [
      ...m,
      {
        id: this.generateId(),
        sender: 'bot',
        content: '',
        timestamp: new Date().toISOString(),
      },
    ]);

    const wordsQueue: string[] = [];
    let typingInterval: any | null = null;
    let fullReply = '';
    this.isTyping.set(true);

    this.chat.sendMessageStream(
      messageForGemini,
      (chunk) => {
        // Acumulamos el Markdown completo
        fullReply += chunk;

        // Versión plana: troceamos por palabras, quitando asteriscos
        const newWords = chunk
          .replace(/\*/g, '')
          .split(/\s+/)
          .map((w) => w.trim())
          .filter((w) => w.length > 0);
        if (newWords.length) {
          wordsQueue.push(...newWords);
        }

        if (!typingInterval && wordsQueue.length) {
          typingInterval = setInterval(() => {
            if (!wordsQueue.length) {
              if (typingInterval) {
                clearInterval(typingInterval);
                typingInterval = null;
              }
              return;
            }

            const nextWord = wordsQueue.shift()!;

            this.chat.messages.update((msgs: ChatMessage[]) => {
              const updated = [...msgs];
              const current = updated[botIndex];
              if (!current) return msgs;
              updated[botIndex] = {
                ...current,
                content:
                  current.content + (current.content ? ' ' : '') + nextWord,
              };
              return updated;
            });

            queueMicrotask(() => this.scrollToBottom());
          }, 60);
        }
      },
      () => {
        // Al terminar el stream, esperamos a que se vacíe la cola y luego
        // sustituimos por el Markdown completo
        const finish = () => {
          this.chat.messages.update((msgs: ChatMessage[]) => {
            const updated = [...msgs];
            const current = updated[botIndex];
            if (!current) return msgs;
            updated[botIndex] = {
              ...current,
              content: fullReply,
            };
            return updated;
          });

          this.isTyping.set(false);
        };

        if (!wordsQueue.length && !typingInterval) {
          finish();
        } else {
          const checkInterval = setInterval(() => {
            if (!wordsQueue.length && !typingInterval) {
              clearInterval(checkInterval);
              finish();
            }
          }, 50);
        }
      },
      (err) => {
        console.error('Error en stream', err);
        this.isTyping.set(false);
      }
    );
  }

  private startConversationWithProfile(
    profile: ReturnType<ProfileService['profile']>
  ) {
    const p = profile;
    if (!p) {
      return;
    }

    const parts: string[] = [];
    if (p.age != null) {
      parts.push(`edad: ${p.age} años`);
    }
    if (p.height != null) {
      parts.push(`altura: ${p.height} cm`);
    }
    if (p.weight != null) {
      parts.push(`peso: ${p.weight} kg`);
    }
    if (p.goal) {
      parts.push(`objetivo: ${p.goal}`);
    }
    if (p.activity) {
      parts.push(`actividad diaria: ${p.activity}`);
    }
    if (p.medicalNotes) {
      parts.push(`datos médicos: ${p.medicalNotes}`);
    }

    if (!parts.length) {
      return;
    }

    const autoUserText =
      'Empieza con un primer análisis y plan inicial basado en este perfil.';

    const profileText =
      'Perfil del usuario para entreno y dieta: ' + parts.join(', ') + '.';

    const messageForGemini =
      profileText + '\n\nMensaje del usuario: ' + autoUserText;

    const botIndex = this.chat.messages().length;
    this.chat.messages.update((m: ChatMessage[]) => [
      ...m,
      {
        id: this.generateId(),
        sender: 'bot',
        content: '',
        timestamp: new Date().toISOString(),
      },
    ]);

    let fullReply = '';
    this.isTyping.set(true);

    this.chat.sendMessageStream(
      messageForGemini,
      (chunk) => {
        // Acumulamos el Markdown completo
        fullReply += chunk;

        // Versión "plana" para mostrar mientras escribe (sin asteriscos)
        const displayChunk = chunk.replace(/\*/g, '');

        this.chat.messages.update((msgs: ChatMessage[]) => {
          const updated = [...msgs];
          const current = updated[botIndex];
          if (!current) return msgs;
          updated[botIndex] = {
            ...current,
            content: current.content + displayChunk,
          };
          return updated;
        });

        queueMicrotask(() => this.scrollToBottom());
      },
      () => {
        // Cuando termina el stream, sustituimos por el Markdown completo
        this.chat.messages.update((msgs: ChatMessage[]) => {
          const updated = [...msgs];
          const current = updated[botIndex];
          if (!current) return msgs;
          updated[botIndex] = {
            ...current,
            content: fullReply,
          };
          return updated;
        });

        this.isTyping.set(false);
      },
      (err) => {
        console.error('Error en stream', err);
        this.isTyping.set(false);
      }
    );
  }

  toggleTheme() {
    const next = this.theme() === 'dark' ? 'light' : 'dark';
    this.theme.set(next);
    localStorage.setItem('gymai_theme', next);
    document.documentElement.setAttribute('data-theme', next);
  }

  // ===== Swipe down para ocultar teclado (como ChatGPT) =====

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
    const minSwipeDistance = 50; // píxeles mínimos para considerar swipe

    // Si el usuario hizo swipe hacia abajo y hay un input enfocado, ocultar teclado
    if (swipeDistance > minSwipeDistance && this.isInputFocused) {
      this.hideKeyboard();
    }

    // Reset
    this.touchStartY = 0;
    this.touchCurrentY = 0;
    this.isInputFocused = false;
  }

  private hideKeyboard() {
    // Quitar focus del elemento activo para ocultar el teclado
    if (document.activeElement instanceof HTMLElement) {
      document.activeElement.blur();
    }
  }
}
