import { useEffect, useState } from 'react'
import { api } from '../api/client.js'

/**
 * Desvíos vigentes (la última auditoría de cada elemento y parámetro dio
 * DESVIO) y descarga de los planes RAML 2.0 para NetAct: el plan con el
 * valor objetivo y el backup con el valor vigente, para poder hacer rollback.
 */
export default function Reconfiguracion() {
  const [segmentos, setSegmentos] = useState([])
  const [idSegmento, setIdSegmento] = useState('')
  const [desvios, setDesvios] = useState([])
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(false)

  const consulta = idSegmento ? `?idSegmento=${idSegmento}` : ''

  const cargar = () => {
    setCargando(true)
    setError(null)
    api
      .get(`/api/auditorias/desvios${consulta}`)
      .then(setDesvios)
      .catch((e) => setError(e.message))
      .finally(() => setCargando(false))
  }

  useEffect(() => {
    api.get('/api/segmentos').then(setSegmentos).catch((e) => setError(e.message))
  }, [])

  useEffect(() => {
    cargar()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [idSegmento])

  const descargar = async (path, nombre) => {
    setError(null)
    try {
      await api.descargar(path, nombre)
    } catch (err) {
      setError(err.message)
    }
  }

  const sep = consulta ? '&' : '?'
  const reconfigurables = desvios.filter((d) => d.reconfigurable).length
  const pendientes = desvios.length - reconfigurables
  const nombreSegmento = (id) => segmentos.find((s) => s.idSegmento === id)?.nombre ?? id

  return (
    <div>
      <h1>Reconfiguración</h1>
      <p className="subtitle">
        Desvíos vigentes y su corrección en formato RAML 2.0 (plan de CM de NetAct). Siempre se generan dos
        archivos con los mismos objetos: el <strong>plan</strong> con el valor objetivo y el{' '}
        <strong>backup</strong> con el valor vigente, para hacer rollback si hace falta. Los desvíos sin valor
        objetivo no entran en el plan: quedan en <code>pendientes.txt</code> dentro del ZIP.
      </p>

      {error && <p className="error">{error}</p>}

      <div className="toolbar">
        <label>
          Segmento
          <select value={idSegmento} onChange={(e) => setIdSegmento(e.target.value)}>
            <option value="">Todos (un plan por segmento en el ZIP)</option>
            {segmentos.map((s) => (
              <option key={s.idSegmento} value={s.idSegmento}>
                {s.nombre}
              </option>
            ))}
          </select>
        </label>
        <button onClick={() => descargar(`/api/auditorias/reconfiguracion/zip${consulta}`, 'neipa-reconfiguracion.zip')}>
          Descargar plan + backup (ZIP)
        </button>
        <button
          className="secondary"
          onClick={() => descargar(`/api/auditorias/reconfiguracion${consulta}${sep}tipo=plan`, 'plan.xml')}
        >
          Solo plan XML
        </button>
        <button
          className="secondary"
          onClick={() => descargar(`/api/auditorias/reconfiguracion${consulta}${sep}tipo=backup`, 'backup.xml')}
        >
          Solo backup XML
        </button>
        <button className="secondary" onClick={cargar}>
          Actualizar
        </button>
      </div>

      <p className="muted">
        {cargando
          ? 'Cargando...'
          : `${desvios.length} desvíos vigentes: ${reconfigurables} van al plan, ${pendientes} pendientes (sin valor objetivo).`}
      </p>

      <table>
        <thead>
          <tr>
            <th>Elemento</th>
            <th>Segmento</th>
            <th>Objeto (distName)</th>
            <th>Parámetro</th>
            <th>Vigente (backup)</th>
            <th>Objetivo (plan)</th>
            <th>Regla</th>
          </tr>
        </thead>
        <tbody>
          {desvios.map((d) => (
            <tr key={d.idAuditoria}>
              <td>{d.elemento}</td>
              <td>{nombreSegmento(d.idSegmento)}</td>
              <td className="mono">
                {d.claseMo}
                <div className="muted">{d.distName}</div>
              </td>
              <td>{d.parametro}</td>
              <td>{d.valorVigente || <span className="muted">(vacío)</span>}</td>
              <td>
                {d.reconfigurable ? (
                  <span className="badge badge-conforme">{d.valorObjetivo}</span>
                ) : (
                  <span className="badge badge-desvio">pendiente</span>
                )}
              </td>
              <td>
                {d.valorEstandar}
                <div className="muted">{d.reglaAplicada}</div>
              </td>
            </tr>
          ))}
          {!cargando && desvios.length === 0 && (
            <tr>
              <td colSpan={7}>No hay desvíos vigentes. Auditá un segmento desde la pantalla Segmentos.</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
