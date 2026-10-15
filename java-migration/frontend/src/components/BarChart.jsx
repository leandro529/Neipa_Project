/**
 * Barra horizontal simple para comparar magnitudes entre pocas categorias
 * (resultados de auditoria, elementos por fabricante). Cada barra ya lleva
 * su nombre y su valor como label directo, asi que no hace falta una
 * leyenda aparte para identificar las series (ver skill de dataviz,
 * "un legend box no hace falta cuando cada marca ya esta rotulada").
 */
export default function BarChart({ title, items, unit = '' }) {
  const max = Math.max(1, ...items.map((item) => item.value))

  return (
    <div className="chart-card">
      <h3>{title}</h3>
      {items.length === 0 && <p className="muted">Sin datos todavia.</p>}
      <div className="bars">
        {items.map((item) => (
          <div className="bar-row" key={item.label}>
            <span className="bar-label">{item.label}</span>
            <div className="bar-track">
              <div
                className="bar-fill"
                style={{ width: `${(item.value / max) * 100}%`, background: item.color }}
              />
            </div>
            <span className="bar-value">
              {item.value}
              {unit}
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}
