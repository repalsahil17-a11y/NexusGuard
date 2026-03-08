import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Scan } from '../../services/scan';
import { Router } from '@angular/router';

@Component({
  selector: 'app-scan-history',
  imports: [CommonModule, DatePipe],
  templateUrl: './scan-history.html',
  styleUrl: './scan-history.scss',
  standalone: true,
})
export class ScanHistory implements OnInit {

  scans: any[] = [];
  loading: boolean = true;
  errorMessage: string = '';

  constructor(
    private scanService: Scan,
    private router: Router,
    private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    this.scanService.getScanHistory()
        .subscribe({
          next: (data) => {
            console.log('Scan history received:', data);
            this.scans = data;
            this.loading = false;
            this.cdr.markForCheck();
            this.cdr.detectChanges();
          },
          error: (err) => {
            console.error('Error loading history:', err);
            this.errorMessage =
              'Failed to load scan history';
            this.loading = false;
            this.cdr.markForCheck();
            this.cdr.detectChanges();
          }
        });
  }

  // Navigate to scan details
  viewScan(scanId: number) {
    this.router.navigate(['/result', scanId]);
  }

  // Get status color
  getStatusColor(status: string): string {
    switch (status?.toUpperCase()) {
      case 'COMPLETED': return '#38a169';
      case 'IN_PROGRESS': return '#d69e2e';
      case 'FAILED': return '#e53e3e';
      default: return '#718096';
    }
  }

  // Get overall risk level
  getRiskLevel(scan: any): string {
    if (scan.criticalCount > 0) return 'CRITICAL';
    if (scan.highCount > 0) return 'HIGH';
    if (scan.mediumCount > 0) return 'MEDIUM';
    if (scan.lowCount > 0) return 'LOW';
    return 'SAFE';
  }

  // Get risk color
  getRiskColor(scan: any): string {
    const risk = this.getRiskLevel(scan);
    switch (risk) {
      case 'CRITICAL': return '#e53e3e';
      case 'HIGH':     return '#dd6b20';
      case 'MEDIUM':   return '#d69e2e';
      case 'LOW':      return '#38a169';
      case 'SAFE':     return '#38a169';
      default:         return '#718096';
    }
  }

  goToScan() {
    this.router.navigate(['/']);
  }
}
