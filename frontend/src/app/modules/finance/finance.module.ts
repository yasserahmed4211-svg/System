import { NgModule } from '@angular/core';

import { FinanceRoutingModule } from './finance-routing.module';
import { GlFacade } from './gl/facades/gl.facade';
import { JournalFacade } from './gl/facades/journal.facade';

@NgModule({
  imports: [FinanceRoutingModule],
  providers: [GlFacade, JournalFacade]
})
export class FinanceModule {}
