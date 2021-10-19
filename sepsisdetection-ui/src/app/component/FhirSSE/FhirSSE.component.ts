import { HttpClient } from '@angular/common/http';
import { Component, NgZone, OnDestroy, OnInit } from '@angular/core';
import { Observable, Subscription } from 'rxjs';


@Component({
  selector: 'fhir-sse',
  templateUrl: './FhirSSE.component.html',
  styleUrls: ['./FhirSSE.component.css']
})
export class FhirSSEComponent implements OnInit, OnDestroy {
  title = 'client';
  message = '';
  messages: any[];
  sub: Subscription;
  fhirStreamingUrl = window['_env'].FHIR_SSE_STREAMING_URL+"/sse/event/fhir";

  constructor(private zone: NgZone, private http: HttpClient) {
  }

  getMessages(): Observable<any> {

    return Observable.create(
      observer => {

        let source = new EventSource(this.fhirStreamingUrl);
        source.onmessage = event => {
          this.zone.run(() => {
            observer.next(event.data)
          })
        }

        source.onerror = event => {
          this.zone.run(() => {
            observer.error(event)
          })
        }
      }
    )
  }

  ngOnInit(): void {
    this.messages = [];
    console.log("ngOnIt() about to register for SSE at: "+this.fhirStreamingUrl);
    this.sub = this.getMessages().subscribe({
      next: data => {
        //console.log(data);
        this.addMessage(data);
      },
      error: err => console.error(err)
    });
  }

  addMessage(msg: any) {
    this.messages = [...this.messages, msg];
  }

  ngOnDestroy(): void {
    console.log("ngOnDestroy() ... unsubscribing from: "+this.fhirStreamingUrl);
    this.sub && this.sub.unsubscribe();
  }

}