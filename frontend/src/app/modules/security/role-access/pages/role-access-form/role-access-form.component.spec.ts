import { TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA, computed, signal } from '@angular/core';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { Observable } from 'rxjs';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';

import { RoleAccessFormComponent } from './role-access-form.component';
import { RoleAccessFacade } from '../../facades/role-access.facade';
import { ActivePageDto, RoleDto, RolePagePermissionDto } from '../../models/role-access.model';

class MockRoleAccessFacade {
  private readonly rolePagesSig = signal<RolePagePermissionDto[]>([]);
  private readonly selectedRoleSig = signal<RoleDto | null>(null);
  private readonly rolesSig = signal<RoleDto[]>([]);
  private readonly activePagesSig = signal<ActivePageDto[]>([]);
  private readonly loadingSig = signal<boolean>(false);
  private readonly savingSig = signal<boolean>(false);
  private readonly saveErrorSig = signal<string | null>(null);

  readonly rolePages = computed(() => this.rolePagesSig());
  readonly selectedRole = computed(() => this.selectedRoleSig());
  readonly roles = computed(() => this.rolesSig());
  readonly activePages = computed(() => this.activePagesSig());
  readonly loading = computed(() => this.loadingSig());
  readonly saving = computed(() => this.savingSig());
  readonly saveError = computed(() => this.saveErrorSig());

  loadActivePages = jasmine.createSpy('loadActivePages');
  loadRoleDetails = jasmine.createSpy('loadRoleDetails');
  loadRolePages = jasmine.createSpy('loadRolePages');
  loadRoles = jasmine.createSpy('loadRoles');

  setFilters = jasmine.createSpy('setFilters');
  setSize = jasmine.createSpy('setSize');
  setPage = jasmine.createSpy('setPage');

  // helper for tests
  setRolePages(pages: RolePagePermissionDto[]): void {
    this.rolePagesSig.set(pages);
  }
}

class FakeTranslateLoader implements TranslateLoader {
  getTranslation(_lang: string): Observable<Record<string, string>> {
    return of({});
  }
}

describe('RoleAccessFormComponent', () => {
  it('renders role pages on initial load (edit mode)', async () => {
    await TestBed.configureTestingModule({
      imports: [
        RoleAccessFormComponent,
        TranslateModule.forRoot({
          loader: { provide: TranslateLoader, useClass: FakeTranslateLoader }
        })
      ],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of(convertToParamMap({ roleId: '5' }))
          }
        }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .overrideComponent(RoleAccessFormComponent, {
        set: {
          providers: [{ provide: RoleAccessFacade, useClass: MockRoleAccessFacade }]
        }
      })
      .compileComponents();

    const fixture = TestBed.createComponent(RoleAccessFormComponent);
    const component = fixture.componentInstance;
    const facade = fixture.debugElement.injector.get(RoleAccessFacade) as unknown as MockRoleAccessFacade;

    fixture.detectChanges();

    expect(facade.loadActivePages).toHaveBeenCalled();
    expect(facade.loadRoleDetails).toHaveBeenCalledWith(5);
    expect(facade.loadRolePages).toHaveBeenCalledWith(5);

    facade.setRolePages([{ pageCode: 'USER', create: true, update: false, delete: true }]);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('USER');

    // remove page => UI updates (regression for OnPush + missing markForCheck)
    facade.setRolePages([]);
    fixture.detectChanges();

    const text2 = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text2).not.toContain('USER');
    expect(component.permissions.length).toBe(0);
  });
});
