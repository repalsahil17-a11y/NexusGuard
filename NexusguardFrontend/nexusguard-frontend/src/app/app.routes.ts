import { Routes } from '@angular/router';
// import { UploadComponent } from './components/upload/upload.component';
// import { ScanResultComponent } from './components/scan-result/scan-result.component';
// import { ScanHistoryComponent } from './components/scan-history/scan-history.component';
import { Upload } from './components/upload/upload';
import { ScanResult } from './components/scan-result/scan-result';
import { ScanHistory } from './components/scan-history/scan-history';
export const routes: Routes = [
  { path: '', component: Upload },
  { path: 'result/:scanId', component:  ScanResult },
  { path: 'history', component:  ScanHistory }
];