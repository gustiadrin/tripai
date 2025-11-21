import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { ChatAssistant } from './components/chat-assistant/chat-assistant';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'chat', component: ChatAssistant },
  { path: '**', redirectTo: '' },
];
