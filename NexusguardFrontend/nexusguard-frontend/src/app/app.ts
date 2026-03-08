// import { Component, signal } from '@angular/core';
// import { RouterOutlet } from '@angular/router';

// @Component({
//   selector: 'app-root',
//   imports: [RouterOutlet],
//   templateUrl: './app.html',
//   styleUrl: './app.scss'
// })
// export class App {
//   protected readonly title = signal('nexusguard-frontend');
// }
import { Component } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink],
  template: `
    <nav class="navbar">
      <div class="nav-brand">
        🛡️ NexusGuard
      </div>
      <div class="nav-links">
        <a routerLink="/">Scan</a>
        <a routerLink="/history">History</a>
      </div>
    </nav>
    <router-outlet></router-outlet>
  `,
  styles: [`
    .navbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 32px;
      background: #1a1a2e;
      color: white;
    }
    .nav-brand {
      font-size: 22px;
      font-weight: bold;
      color: #00d4ff;
    }
    .nav-links a {
      color: white;
      text-decoration: none;
      margin-left: 24px;
      font-size: 15px;
    }
    .nav-links a:hover {
      color: #00d4ff;
    }
  `]
})
export class AppComponent {}