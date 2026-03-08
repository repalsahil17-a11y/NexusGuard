import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
@Injectable({
  providedIn: 'root'
})
export class Scan {

  private apiUrl = 'http://localhost:8080/api/scan';

  constructor(private http: HttpClient) {}

  // Upload pom.xml and start scan
  uploadPom(
    file: File,
    projectName: string): Observable<any> {

    const formData = new FormData();
    formData.append('file', file);
    formData.append('projectName', projectName);

    return this.http.post(
      `${this.apiUrl}/upload`, formData);
  }

  // Get all past scans
  getScanHistory(): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.apiUrl}/history`);
  }

  // Get one scan by ID
  getScanById(scanId: number): Observable<any> {
    return this.http.get(
      `${this.apiUrl}/${scanId}`);
  }
}