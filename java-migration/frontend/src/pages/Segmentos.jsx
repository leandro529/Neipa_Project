import { useEffect, useState } from 'react'
import { api } from '../api/client.js'

const VACIO = { nombre: '', criterio: '', rc: '', filtroNombre: '' }

export default function Segmentos() {
  const [segmentos, setSegmentos] = useState([])
  const [form, setForm] = useState(VACIO)
  const [editandoId, setEditandoId] = useState(null)
  const [error, setError] = useState(null)
  const [auditando, setAuditando] = useState(null)

  const cargar = () => api.get('/api/segmentos').then(setSegmentos).catch((e) => setError(e.message))

  useEffect(() => {
    cargar()
  }, [])

  const guardar = async (e) => {
    e.preventDefault()
    setError(null)
    const payload = { ...form, rc: form.rc || null, filtroNombre: form.filtroNombre || null }
    try {
      if (editandoId) {
        await api.put(`/api/segmentos/${editandoId}`, payload)
      } else {
        await api.post('/api/segmentos', payload)
      }
      setForm(VACIO)
      setEditandoId(null)
      cargar()
    } catch (err) {
      setError(err.message)
    }
  }

  const editar = (s) => {
    setEditandoId(s.idSegmento)
    setForm({ nombre: s.nombre, criterio: s.criterio || '', rc: s.rc || '', filtroNombre: s.filtroNombre || '' })
  }

  const cancelar = () => {
    setEditandoId(null)
    setForm(VACIO)
  }

  const eliminar = async (id) => {
    if (!window.confirm('¿Eliminar este segmento?')) return
    try {
      await api.del(`/api/segmentos/${id}`)
      cargar()
    } catch (err) {
      setError(err.message)
    }
  }

  const auditar = async (id) => {
    setError(null)
    try {
      setAuditando(id)
      const r = await api.post(`/api/auditorias/segmentos/${id}`)
      if (r && typeof r === 'object' && 'comparaciones' in r) {
        window.alert(
          `Corrida ${r.idCorrida}: ${r.objetosEvaluados} objetos, ${r.comparaciones} comparaciones — ` +
            `${r.conformes} conformes, ${r.desvios} desvíos, ${r.exceptuados} exceptuados ` +
            `(${(r.duracionMs / 1000).toFixed(1)} s). Ver evolución en "Datos CM".`,
        )
      } else {
        window.alert('Auditoría iniciada para el segmento ' + id + '. Revisá el dashboard en unos segundos.')
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setAuditando(null)
    }
  }

  return (
    <div>
      <h1>Segmentos</h1>
      <p className="subtitle">
        Agrupación de elementos de red por tecnología, zona u otro criterio (RF09). Los segmentos con RC se
        auditan sobre los datos de CM importados (uno por RC se crea solo al importar el dump), y pueden
        filtrarse además por nombre del objeto.
      </p>

      {error && <p className="error">{error}</p>}

      <form className="form-card" onSubmit={guardar}>
        <div className="form-row">
          <label>
            Nombre
            <input required value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} />
          </label>
          <label>
            Criterio
            <input
              value={form.criterio}
              onChange={(e) => setForm({ ...form, criterio: e.target.value })}
              placeholder="ej: Zona Norte 4G"
            />
          </label>
          <label>
            RC del CM (PLMN_id)
            <input value={form.rc} onChange={(e) => setForm({ ...form, rc: e.target.value })} placeholder="ej: rc7 (vacío = manual)" />
          </label>
          <label>
            Filtro de nombre
            <input
              value={form.filtroNombre}
              onChange={(e) => setForm({ ...form, filtroNombre: e.target.value })}
              placeholder="ej: BOG* (opcional)"
            />
          </label>
        </div>
        <div className="form-actions">
          <button type="submit">{editandoId ? 'Guardar cambios' : 'Cargar segmento'}</button>
          {editandoId && (
            <button type="button" className="secondary" onClick={cancelar}>
              Cancelar
            </button>
          )}
        </div>
      </form>

      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Nombre</th>
            <th>Criterio</th>
            <th>CM</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {segmentos.map((s) => (
            <tr key={s.idSegmento}>
              <td>{s.idSegmento}</td>
              <td>{s.nombre}</td>
              <td>{s.criterio}</td>
              <td>{s.rc ? `${s.rc}${s.filtroNombre ? ' · ' + s.filtroNombre : ''}` : '—'}</td>
              <td className="row-actions">
                <button onClick={() => editar(s)}>Editar</button>
                <button className="secondary" disabled={auditando !== null} onClick={() => auditar(s.idSegmento)}>
                  {auditando === s.idSegmento ? 'Auditando…' : 'Auditar'}
                </button>
                <button className="danger" onClick={() => eliminar(s.idSegmento)}>
                  Eliminar
                </button>
              </td>
            </tr>
          ))}
          {segmentos.length === 0 && (
            <tr>
              <td colSpan={5}>No hay segmentos cargados todavía.</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
