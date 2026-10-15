import { useEffect, useState } from 'react'
import { api } from '../api/client.js'

const VACIO = { nombre: '', tecnologia: '', fabricante: 'NSN', idSegmento: '', distName: '' }
const FABRICANTES = ['NSN', 'HUAWEI', 'ERICSSON']

export default function Elementos() {
  const [elementos, setElementos] = useState([])
  const [segmentos, setSegmentos] = useState([])
  const [form, setForm] = useState(VACIO)
  const [editandoId, setEditandoId] = useState(null)
  const [error, setError] = useState(null)
  const [filtro, setFiltro] = useState('')

  const cargar = () => api.get('/api/elementos').then(setElementos).catch((e) => setError(e.message))

  useEffect(() => {
    cargar()
    api.get('/api/segmentos').then(setSegmentos).catch((e) => setError(e.message))
  }, [])

  const guardar = async (e) => {
    e.preventDefault()
    setError(null)
    const payload = {
      ...form,
      idSegmento: form.idSegmento ? Number(form.idSegmento) : null,
      distName: form.distName || null,
    }
    try {
      if (editandoId) {
        await api.put(`/api/elementos/${editandoId}`, payload)
      } else {
        await api.post('/api/elementos', payload)
      }
      setForm(VACIO)
      setEditandoId(null)
      cargar()
    } catch (err) {
      setError(err.message)
    }
  }

  const editar = (el) => {
    setEditandoId(el.idElemento)
    setForm({
      nombre: el.nombre,
      tecnologia: el.tecnologia || '',
      fabricante: el.fabricante || 'NSN',
      idSegmento: el.idSegmento || '',
      distName: el.distName || '',
    })
  }

  const cancelar = () => {
    setEditandoId(null)
    setForm(VACIO)
  }

  const eliminar = async (id) => {
    if (!window.confirm('¿Eliminar este elemento de red?')) return
    try {
      await api.del(`/api/elementos/${id}`)
      cargar()
    } catch (err) {
      setError(err.message)
    }
  }

  const MAX_FILAS = 300
  const filtrados = filtro
    ? elementos.filter((el) =>
        `${el.nombre} ${el.distName || ''}`.toLowerCase().includes(filtro.toLowerCase()),
      )
    : elementos
  const visibles = filtrados.slice(0, MAX_FILAS)

  const nombreSegmento = (id) => segmentos.find((s) => s.idSegmento === id)?.nombre ?? id

  return (
    <div>
      <h1>Elementos de red</h1>
      <p className="subtitle">Sitios, celdas, core y antenas que se auditan por segmento (CU001).</p>

      {error && <p className="error">{error}</p>}

      <form className="form-card" onSubmit={guardar}>
        <div className="form-row">
          <label>
            Nombre
            <input required value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} />
          </label>
          <label>
            Tecnología
            <input
              value={form.tecnologia}
              onChange={(e) => setForm({ ...form, tecnologia: e.target.value })}
              placeholder="ej: 4G, 5G"
            />
          </label>
          <label>
            Fabricante
            <select value={form.fabricante} onChange={(e) => setForm({ ...form, fabricante: e.target.value })}>
              {FABRICANTES.map((f) => (
                <option key={f} value={f}>
                  {f}
                </option>
              ))}
            </select>
          </label>
          <label>
            Segmento
            <select required value={form.idSegmento} onChange={(e) => setForm({ ...form, idSegmento: e.target.value })}>
              <option value="">Seleccionar...</option>
              {segmentos.map((s) => (
                <option key={s.idSegmento} value={s.idSegmento}>
                  {s.nombre}
                </option>
              ))}
            </select>
          </label>
        </div>
        <div className="form-row">
          <label style={{ flex: 1 }}>
            distName en el OSS
            <input
              value={form.distName}
              onChange={(e) => setForm({ ...form, distName: e.target.value })}
              placeholder="ej: PLMN-PLMN/MRBTS-9001/LNBTS-9001 (vacío = se arma con el id)"
            />
          </label>
        </div>
        <p className="hint">
          Solo el fabricante NSN tiene extracción implementada por ahora; Huawei y Ericsson quedan como
          puntos de extensión (RF21) y hoy fallan al auditar.
        </p>
        <div className="form-actions">
          <button type="submit">{editandoId ? 'Guardar cambios' : 'Cargar elemento'}</button>
          {editandoId && (
            <button type="button" className="secondary" onClick={cancelar}>
              Cancelar
            </button>
          )}
        </div>
      </form>

      <div className="toolbar">
        <label>
          Buscar
          <input value={filtro} onChange={(e) => setFiltro(e.target.value)} placeholder="nombre o distName" />
        </label>
        <span className="muted">
          {filtrados.length} elementos{filtrados.length > MAX_FILAS ? ` (se muestran los primeros ${MAX_FILAS})` : ''}
        </span>
      </div>

      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Nombre</th>
            <th>Tecnología</th>
            <th>Fabricante</th>
            <th>Segmento</th>
            <th>distName</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {visibles.map((el) => (
            <tr key={el.idElemento}>
              <td>{el.idElemento}</td>
              <td>{el.nombre}</td>
              <td>{el.tecnologia}</td>
              <td>{el.fabricante}</td>
              <td>{nombreSegmento(el.idSegmento)}</td>
              <td className="mono">{el.distName || `PLMN-PLMN/MRBTS-${el.idElemento}/LNBTS-${el.idElemento}`}</td>
              <td className="row-actions">
                <button onClick={() => editar(el)}>Editar</button>
                <button className="danger" onClick={() => eliminar(el.idElemento)}>
                  Eliminar
                </button>
              </td>
            </tr>
          ))}
          {elementos.length === 0 && (
            <tr>
              <td colSpan={7}>No hay elementos de red cargados todavía.</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
