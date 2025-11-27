import { Injectable, signal, effect } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom, interval, switchMap } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ChatMessage {
  sender: 'user' | 'bot';
  content: string;
  timestamp?: string;
}

@Injectable({ providedIn: 'root' })
export class ChatService {
  private base = environment.apiBaseUrl;
  private storageKey = 'gymai_messages';
  messages = signal<ChatMessage[]>([]);
  updateActivity!: () => void;

  constructor(private http: HttpClient) {
    // Cargar historial desde localStorage al iniciar (si existe)
    try {
      const raw = localStorage.getItem(this.storageKey);
      if (raw) {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed)) {
          this.messages.set(parsed as ChatMessage[]);
        }
      }
    } catch {}

    // Guardar automáticamente en localStorage cuando cambie la señal
    effect(() => {
      const current = this.messages();
      try {
        localStorage.setItem(this.storageKey, JSON.stringify(current));
      } catch {}
    });
    // Polling más suave: cada 5s y solo si no hay actividad reciente
    let lastActivity = Date.now();

    interval(5000)
      .pipe(
        switchMap(() => {
          // Solo hacer polling si no ha habido actividad en los últimos 3 segundos
          if (Date.now() - lastActivity < 3000) {
            return [];
          }
          return this.http.get<ChatMessage[]>(`${this.base}/messages`);
        })
      )
      .subscribe({
        next: (msgs) => {
          if (!Array.isArray(msgs) || msgs.length === 0) return;

          const withTs = msgs
            .filter(
              (m) =>
                !(
                  m.sender === 'user' &&
                  m.content ===
                    'Empieza con un primer análisis y plan inicial basado en este perfil.'
                )
            )
            .map((m) => ({
              ...m,
              timestamp: m.timestamp || new Date().toISOString(),
            }));

          const current = this.messages();

          // Solo actualizar si hay cambios reales
          if (JSON.stringify(current) !== JSON.stringify(withTs)) {
            const hasLocalWelcome =
              current.length > 0 &&
              current[0].sender === 'bot' &&
              (current[0].content.startsWith('Hola, soy GymAI.') ||
                current[0].content.startsWith(
                  'Basándome en los datos que me has dado'
                ));

            if (hasLocalWelcome && withTs.length > 0) {
              this.messages.set([current[0], ...withTs]);
            } else if (!hasLocalWelcome) {
              this.messages.set(withTs);
            }
          }
        },
        error: () => {},
      });

    // Actualizar timestamp de actividad cuando se envían mensajes
    this.updateActivity = () => {
      lastActivity = Date.now();
    };
  }

  sendMessageStream(
    text: string,
    onChunk: (chunk: string) => void,
    onDone?: () => void,
    onError?: (err: any) => void
  ): void {
    const url = `${this.base}/chat/stream?message=${encodeURIComponent(text)}`;
    const es = new EventSource(url);

    es.onmessage = (event) => {
      const chunk = event.data as string;
      if (chunk) {
        onChunk(chunk);
      }
    };

    es.onerror = (err) => {
      es.close();
      // Muchos navegadores disparan 'error' cuando el servidor cierra la conexión SSE.
      // Si el EventSource ya está cerrado, lo tratamos como finalización normal.
      if ((es as EventSource).readyState === EventSource.CLOSED) {
        if (onDone) {
          onDone();
        }
        return;
      }

      if (onError) {
        onError(err);
      }
      if (onDone) {
        onDone();
      }
    };
  }

  async resetConversation(): Promise<void> {
    try {
      await firstValueFrom(
        this.http.post<void>(`${this.base}/messages/reset`, {})
      );
    } catch {
      // incluso si falla el backend, limpiamos el estado local para evitar confusión
    }

    this.messages.set([]);
    try {
      localStorage.removeItem(this.storageKey);
    } catch {}
  }

  async sendMessage(text: string) {
    const body = { message: text };
    const res: any = await firstValueFrom(
      this.http.post(`${this.base}/chat`, body)
    );
    // respuesta inmediata ya guardada en back; opcional: actualizar UI inmediatamente
    return res?.reply;
  }

  async loadMessagesOnce() {
    const msgs = await firstValueFrom(
      this.http.get<ChatMessage[]>(`${this.base}/messages`)
    );
    const withTs = (msgs || [])
      .filter(
        (m) =>
          !(
            m.sender === 'user' &&
            m.content ===
              'Empieza con un primer análisis y plan inicial basado en este perfil.'
          )
      )
      .map((m) => {
        let content = m.content;
        if (m.sender === 'user') {
          const marker = 'Mensaje del usuario:';
          const idx = content.indexOf(marker);
          if (idx !== -1) {
            content = content.substring(idx + marker.length).trim();
          }
        }

        return {
          ...m,
          content,
          timestamp: m.timestamp || new Date().toISOString(),
        };
      });
    this.messages.set(withTs);
  }
}
