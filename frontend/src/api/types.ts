export const ROLES = ['STUDENT', 'ADMIN', 'TECHNICIAN', 'ACCOUNTANT', 'AFFAIRS'] as const

export type Role = (typeof ROLES)[number]

export const ROLE_LABELS: Record<Role, string> = {
  STUDENT: 'Student',
  ADMIN: 'Administrator',
  TECHNICIAN: 'Technician',
  ACCOUNTANT: 'Accountant',
  AFFAIRS: 'Student affairs',
}

/** GET /api/me and the login response. */
export interface Me {
  id: number
  username: string
  fullName: string
  role: Role
}

export interface FieldError {
  field: string
  message: string
}

/** RFC 7807 body returned by the API for every error. */
export interface ProblemDetail {
  type?: string
  title?: string
  status: number
  detail?: string
  instance?: string
  errors?: FieldError[]
  /** Business rule id (BR-xx) on 422 responses. */
  rule?: string
}
