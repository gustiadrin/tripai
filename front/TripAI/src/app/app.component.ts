import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ChatAssistant } from './components/chat-assistant/chat-assistant';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ChatAssistant],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent {
  title = 'GymAI';
}
