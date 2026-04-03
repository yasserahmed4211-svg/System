import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from 'src/environments/environment';
import { BaseApiService } from 'src/app/shared/base/base-api.service';
import {
  UserDto,
  PageUserDto,
  SearchRequest,
  CreateUserRequest,
  UpdateUserRequest,
  PageRoleDto
} from 'src/app/modules/security/user-management/models/user.model';

/**
 * UserApiService
 *
 * NOTE: Authorization header is injected globally by AuthInterceptor.
 *       The old `token` parameter is kept in method signatures for backward
 *       compatibility but is no longer used.
 */
@Injectable()
export class UserApiService extends BaseApiService {
  private readonly baseUrl = `${environment.authApiUrl}/api/users`;
  private readonly rolesUrl = `${environment.authApiUrl}/api/roles`;

  searchUsers(request: SearchRequest, _token?: string): Observable<PageUserDto> {
    return this.doPost<PageUserDto>(`${this.baseUrl}/search`, request);
  }

  createUser(request: CreateUserRequest, _token?: string): Observable<UserDto> {
    return this.doPost<UserDto>(this.baseUrl, request);
  }

  updateUser(userId: number, request: UpdateUserRequest, _token?: string): Observable<UserDto> {
    return this.doPut<UserDto>(`${this.baseUrl}/${userId}`, request);
  }

  deleteUser(userId: number, _token?: string): Observable<void> {
    return this.doDelete(`${this.baseUrl}/${userId}`);
  }

  getRoles(_token?: string, size: number = 50): Observable<PageRoleDto> {
    return this.doPost<PageRoleDto>(`${this.rolesUrl}/search`, {
      filters: [],
      sorts: [{ field: 'roleName', direction: 'ASC' }],
      page: 0,
      size: Math.min(size, 50)
    });
  }
}
