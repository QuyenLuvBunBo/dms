export function PlaceholderPage({ title, phase }: { title: string; phase: number }) {
  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
      <p className="mt-2 text-sm text-slate-500">Coming in Phase {phase}.</p>
    </div>
  )
}
