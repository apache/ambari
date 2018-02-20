import { NgModule }              from '@angular/core';
import { RouterModule, Routes }  from '@angular/router';
import {LogsContainerComponent} from "@app/components/logs-container/logs-container.component";
import {LoginFormComponent} from "@app/components/login-form/login-form.component";
import {AuthGuardService} from "@app/services/auth-guard.service";

const appRoutes: Routes = [
  { path: 'login', component: LoginFormComponent },
  { path: 'logs', component: LogsContainerComponent, canActivate: [AuthGuardService] },
  { path: '',   redirectTo: '/logs', pathMatch: 'full' },
  { path: '**', redirectTo: '/logs' }
];

@NgModule({
  imports: [
    RouterModule.forRoot(
      appRoutes,
      { enableTracing: true } // <-- debugging purposes only
    )
  ],
  exports: [
    RouterModule
  ]
})
export class AppRoutingModule {}
