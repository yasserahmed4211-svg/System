export interface UserDto {
  id: number;
  username: string;
  enabled: boolean;
  roles: string[];
  permissions: string[];
}

export interface AuthRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken?: string;
  expiresIn?: number;
  userId?: number;
  username?: string;
  roles?: string[];
  permissions?: string[];
  enabled?: boolean;
}

// Re-export shared ApiResponse – keeps existing imports working
export { ApiResponse } from 'src/app/shared/models/api-response.model';
