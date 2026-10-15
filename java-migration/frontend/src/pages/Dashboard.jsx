import { useEffect, useState } from 'react'
import { api } from '../api/client.js'
import BarChart from '../components/BarChart.jsx'

const COLOR_RESULTADO = {
  conforme: 'var(--status-good)',
  desvio: 'var(--status-critical)',
  exceptuado: 'var(--status-warning)',
}

const COLOR_CATEGORICO = ['var(--series-1)', 'var(--series-2)', 'var(--series-3)']

export default function Dashboard() {
  const [resumen, setResumen] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    api
      .get('/api/dashboard/resumen')
      .then(setResumen)
      .catch((e) => setError(e.message))
      .finally(() => setCargando(false))
  }, [])

  if (cargando) {
    return <p className="muted">Cargando dashboard...</p>
  }
  if (error) {
    return <p className="error">No se pudo cargar el dashboard: {error}</p>
  }
  if (!resumen) {
    return null
  }

  const resultados = [
    { label: 'Conforme', value: resumen.auditoriasConformes, color: COLOR_RESULTADO.conforme },
    { label: 'Desvío', value: resumen.auditoriasDesvio, color: COLOR_RESULTADO.desvio },
    { label: 'Exceptuado', value: resumen.auditoriasExceptuadas, color: COLOR_RESULTADO.exceptuado },
  ]

  const fabricantes = Object.entries(resumen.elementosPorFabricante || {}).map(([label, value], i) => ({
    label,
    value,
    color: COLOR_CATEGORICO[i % COLOR_CATEGORICO.length],
  }))

  const recientes = resumen.auditoriasRecientes || []

  return (
    <div>
      <h1>Dashboard de control</h1>
      <p className="subtitle">Estado de auditoría, excepciones y cambios masivos de NEIPA.</p>

      <div className="kpi-grid">
        <div className="kpi-tile">
          <span className="kpi-label">Segmentos</span>
          <span className="kpi-value">{resumen.totalSegmentos}</span>
        </div>
        <div className="kpi-tile">
          <span className="kpi-label">Elementos de red</span>
          <span className="kpi-value">{resumen.totalElementos}</span>
        </div>
        <div className="kpi-tile">
          <span className="kpi-label">Auditorías totales</span>
          <span className="kpi-value">{resumen.totalAuditorias}</span>
        </div>
        <div className="kpi-tile">
          <span className="kpi-label">Tasa de conformidad</span>
          <span className="kpi-value">{resumen.tasaConformidadPct.toFixed(1)}%</span>
        </div>
        <div className="kpi-tile">
          <span className="kpi-label">Excepciones activas</span>
          <span className="kpi-value">{resumen.excepcionesActivas}</span>
        </div>
      </div>

      <div className="chart-grid">
        <BarChart title="Resultados de auditoría" items={resultados} />
        <BarChart title="Elementos por fabricante" items={fabricantes} />
      </div>

      <div className="table-card">
        <h3>Auditorías recientes</h3>
        <table>
          <thead>
            <tr>
              <th>Fecha</th>
              <th>Elemento</th>
              <th>Parámetro</th>
              <th>Vigente</th>
              <th>Estándar</th>
              <th>Resultado</th>
            </tr>
          </thead>
          <tbody>
            {recientes.map((a) => (
              <tr key={a.idAuditoria}>
                <td>{a.fecha ? new Date(a.fecha).toLocaleString('es-AR') : '-'}</td>
                <td>{a.idElemento}</td>
                <td>{a.parametro}</td>
                <td>{a.valorVigente}</td>
                <td>{a.valorEstandar}</td>
                <td>
                  <span className={`badge badge-${a.resultado}`}>{a.resultado}</span>
                </td>
              </tr>
            ))}
            {recientes.length === 0 && (
              <tr>
                <td colSpan={6}>Todavía no se corrió ninguna auditoría (pantalla "Segmentos").</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
