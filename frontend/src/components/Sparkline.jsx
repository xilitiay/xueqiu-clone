export default function Sparkline({ data, width = 90, height = 30, fluid = false }) {
  if (!data || data.length < 2) return <svg className="sparkline" />
  const min = Math.min(...data)
  const max = Math.max(...data)
  const span = max - min || 1
  const stepX = width / (data.length - 1)
  const points = data.map((v, i) => {
    const x = i * stepX
    const y = height - ((v - min) / span) * (height - 4) - 2
    return `${x.toFixed(1)},${y.toFixed(1)}`
  }).join(' ')

  const up = data[data.length - 1] >= data[0]
  const color = up ? '#e64340' : '#1aad19'

  return (
    <svg className="sparkline" viewBox={`0 0 ${width} ${height}`} preserveAspectRatio="none"
         style={{ width: fluid ? '100%' : width, height, display: 'block' }}>
      <polyline points={points} fill="none" stroke={color} strokeWidth="1.5" />
    </svg>
  )
}
