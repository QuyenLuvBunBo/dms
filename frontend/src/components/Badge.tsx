import type { ReactNode } from 'react'

export type BadgeTone = 'neutral' | 'green' | 'amber' | 'red' | 'blue' | 'pink'

const TONES: Record<BadgeTone, string> = {
  neutral: 'bg-slate-100 text-slate-700',
  green: 'bg-emerald-50 text-emerald-700',
  amber: 'bg-amber-50 text-amber-800',
  red: 'bg-red-50 text-red-700',
  blue: 'bg-sky-50 text-sky-700',
  pink: 'bg-pink-50 text-pink-700',
}

export function Badge({ tone = 'neutral', children }: { tone?: BadgeTone; children: ReactNode }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${TONES[tone]}`}>
      {children}
    </span>
  )
}
