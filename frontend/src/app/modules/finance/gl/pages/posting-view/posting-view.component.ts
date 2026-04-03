import {
  Component, OnInit, OnDestroy, inject, signal, effect, untracked,
  ChangeDetectionStrategy, ChangeDetectorRef, DestroyRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';

import { SharedModule } from 'src/app/theme/shared/shared.module';

import { ErpSectionComponent } from 'src/app/shared/components/erp-section/erp-section.component';
import { ErpActionBarComponent } from 'src/app/shared/components/erp-action-bar/erp-action-bar.component';
import { ErpEmptyStateComponent } from 'src/app/shared/components/erp-empty-state/erp-empty-state.component';
import { ErpPermissionDirective } from 'src/app/shared/directives/erp-permission.directive';
import { ErpNotificationService } from 'src/app/shared/services/erp-notification.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AuthenticationService } from 'src/app/core/services/authentication.service';

import { PostingFacade } from 'src/app/modules/finance/gl/facades/posting.facade';
import { PostingApiService } from 'src/app/modules/finance/gl/services/posting-api.service';
import { AccPostingMstDto, POSTING_STATUS } from 'src/app/modules/finance/gl/models/posting.model';
import {
  PostingConfirmActionDeps,
  confirmGenerateJournal
} from '../../helpers/posting-confirm-actions';

import { TranslateModule, TranslateService } from '@ngx-translate/core';

/**
 * PostingViewComponent – GL Posting Detail / View page.
 *
 * Read-only view showing posting header, details table,
 * and "Generate Journal" action for READY_FOR_GL postings.
 */
@Component({
  selector: 'app-posting-view',
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    TranslateModule,
    ErpSectionComponent,
    ErpActionBarComponent,
    ErpEmptyStateComponent,
    ErpPermissionDirective
  ],
  templateUrl: './posting-view.component.html',
  styleUrl: './posting-view.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [PostingFacade, PostingApiService]
})
export class PostingViewComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  private destroyRef = inject(DestroyRef);
  readonly translate = inject(TranslateService);
  readonly facade = inject(PostingFacade);
  private readonly notificationService = inject(ErpNotificationService);
  private readonly modalService = inject(NgbModal);
  private readonly authService = inject(AuthenticationService);

  readonly posting = signal<AccPostingMstDto | null>(null);
  readonly loading = signal<boolean>(true);

  get isReadyForGl(): boolean {
    return this.posting()?.status === POSTING_STATUS.READY_FOR_GL;
  }

  get isJournalCreated(): boolean {
    return this.posting()?.status === POSTING_STATUS.JOURNAL_CREATED;
  }

  get isReadyForPost(): boolean {
    return this.posting()?.status === POSTING_STATUS.READY_FOR_POST;
  }

  get isPosted(): boolean {
    return this.posting()?.status === POSTING_STATUS.POSTED;
  }

  get isCancelled(): boolean {
    return this.posting()?.status === POSTING_STATUS.CANCELLED;
  }

  get isReversed(): boolean {
    return this.posting()?.status === POSTING_STATUS.REVERSED;
  }

  private saveErrorEffect = effect(() => {
    const error = this.facade.saveError();
    if (error) {
      untracked(() => this.notificationService.error(this.translate.instant(error)));
    }
  });

  ngOnInit(): void {
    const postingId = this.route.snapshot.paramMap.get('id');
    if (postingId) {
      this.loadPosting(+postingId);
    }
  }

  private loadPosting(postingId: number): void {
    this.loading.set(true);
    this.facade.getPostingById(postingId, (posting) => {
      this.posting.set(posting);
      this.loading.set(false);
      this.cdr.markForCheck();
    });
  }

  // ── Actions ───────────────────────────────────────────────

  private get confirmDeps(): PostingConfirmActionDeps {
    return {
      modal: this.modalService,
      notify: this.notificationService,
      auth: this.authService,
      facade: this.facade
    };
  }

  onGenerateJournal(): void {
    const p = this.posting();
    if (!p) return;
    confirmGenerateJournal(this.confirmDeps, p, () => {
      this.loadPosting(p.postingId);
    });
  }

  onViewJournal(): void {
    const p = this.posting();
    if (p?.finJournalIdFk) {
      this.router.navigate(['/finance/gl/journals/view', p.finJournalIdFk]);
    }
  }

  goBack(): void {
    this.router.navigate(['/finance/gl/postings']);
  }

  ngOnDestroy(): void {
    this.facade.clearCurrentEntity();
  }
}
