import type { ReactNode } from 'react'
import { Link } from 'react-router'

interface PageHeaderProps {
  title: string
  subtitle?: ReactNode
  back?: { to: string; label: string }
  actions?: ReactNode
}

export function PageHeader({ title, subtitle, back, actions }: PageHeaderProps) {
  return (
    <div className="mb-6">
      {back && (
        <Link to={back.to} className="text-sm text-slate-500 hover:text-slate-900">
          ← {back.label}
        </Link>
      )}
      <div className="mt-1 flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
          {subtitle && <div className="mt-1 text-sm text-slate-500">{subtitle}</div>}
        </div>
        {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
      </div>
    </div>
  )
}
