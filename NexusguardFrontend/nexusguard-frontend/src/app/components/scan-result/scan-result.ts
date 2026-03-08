import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { Scan } from '../../services/scan';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-scan-result',
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './scan-result.html',
  styleUrl: './scan-result.scss',
  standalone: true,
})
export class ScanResult implements OnInit {
  scanResult: any = null;
  loading: boolean = true;
  errorMessage: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private scanService: Scan,
    private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    const scanId = this.route.snapshot
                       .paramMap.get('scanId');

    console.log('Scan ID from URL:', scanId);

    if (scanId) {
      this.scanService
          .getScanById(Number(scanId))
          .subscribe({
            next: (data) => {
              console.log('Scan data received:', data);
              this.scanResult = data;
              this.loading = false;
              this.cdr.markForCheck();
              this.cdr.detectChanges();
            },
            error: (err) => {
              console.error('API Error:', err);
              this.errorMessage =
                'Failed to load scan results: '
                + err.message;
              this.loading = false;
              this.cdr.markForCheck();
              this.cdr.detectChanges();
            },
            complete: () => {
              console.log('Request completed');
            }
          });
    } else {
      console.warn('No scanId found in URL');
      this.errorMessage = 'No scan ID provided';
      this.loading = false;
      this.cdr.markForCheck();
    }
  }

  getSeverityColor(severity: string): string {
    switch (severity?.toUpperCase()) {
      case 'CRITICAL': return '#e53e3e';
      case 'HIGH':     return '#dd6b20';
      case 'MEDIUM':   return '#d69e2e';
      case 'LOW':      return '#38a169';
      default:         return '#718096';
    }
  }

  getSeverityBg(severity: string): string {
    switch (severity?.toUpperCase()) {
      case 'CRITICAL': return '#fff5f5';
      case 'HIGH':     return '#fffaf0';
      case 'MEDIUM':   return '#fffff0';
      case 'LOW':      return '#f0fff4';
      default:         return '#f7fafc';
    }
  }

  getSeverityIcon(severity: string): string {
    switch (severity?.toUpperCase()) {
      case 'CRITICAL': return '🔴';
      case 'HIGH':     return '🟠';
      case 'MEDIUM':   return '🟡';
      case 'LOW':      return '🟢';
      default:         return '⚪';
    }
  }

  getPriorityIcon(priority: string): string {
    switch (priority?.toUpperCase()) {
      case 'IMMEDIATE': return '🚨';
      case 'HIGH':      return '⚠️';
      case 'MEDIUM':    return '📌';
      case 'LOW':       return '📎';
      default:          return '📌';
    }
  }

  getRiskColor(riskScore: number): string {
    if (riskScore >= 9) return '#e53e3e';
    if (riskScore >= 7) return '#dd6b20';
    if (riskScore >= 4) return '#d69e2e';
    return '#38a169';
  }

  goBack() {
    this.router.navigate(['/']);
  }

  goToHistory() {
    this.router.navigate(['/history']);
  }
}