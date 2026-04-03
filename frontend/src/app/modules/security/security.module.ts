import { NgModule } from '@angular/core';

import { SecurityRoutingModule } from './security-routing.module';
import { PagesApiService } from './pages-registry/services/pages-api.service';
import { PagesFacade } from './pages-registry/facades/pages.facade';

@NgModule({
  imports: [SecurityRoutingModule],
  providers: [PagesApiService, PagesFacade]
})
export class SecurityModule {}
