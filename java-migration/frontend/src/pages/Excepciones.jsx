import { useEffect, useState } from 'react'
import { api } from '../api/client.js'

const VACIO = {
  alcance: 'GLOBAL',
  idElemento: '',
  idSegmento: '',
  patronNombre: '',
  parametro: '',
  motivo: '',
  vigenteDesde: '',
  vigenteHasta: '',
  activa: true,
}

export default function Excepciones() {
  const [excepciones, setExcepciones] = useState([])
  const [segmentos, setSegmentos] = useState([])
  const [elementos, setElementos] = useState([])
  const [form, setForm] = useState(VACIO)
  const [editandoId, setEditandoId] = useState(null)
  const [error, setError] = useState(null)

  const cargar = () => api.get('/api/excepciones').then(setExcepciones).catch((e) => setError(e.message))

  useEffect(() => {
    cargar()
    api.get('/api/segmentos').then(setSegmentos).catch((e) => setError(e.message))
    api.get('/api/elementos').then(setElementos).catch((e) => setError(e.message))
  }, [])

  const guardar = async (e) => {
    e.preventDefault()
    setError(null)
    const payload = {
      ...form,
      idElemento: form.alcance === 'ELEMENTO' && form.idElemento ? Number(form.idElemento) : null,
      idSegmento: form.alcance === 'SEGMENTO' && form.idSegmento ? Number(form.idSegmento) : null,
      patronNombre: form.alcance === 'PATRON' ? form.patronNombre : null,
      vigenteDesde: form.vigenteDesde || null,
      vigenteHasta: form.vigenteHasta || null,
    }
    try {
      if (editandoId) {
        await api.put(`/api/excepciones/${editandoId}`, payload)
      } else {
        await api.post('/api/excepciones', payload)
      }
      setForm(VACIO)
      setEditandoId(null)
      cargar()
    } catch (err) {
      setError(err.message)
    }
  }

  const editar = (x) => {
    setEditandoId(x.idExcepcion)
    setForm({
      alcance: x.alcance,
      idElemento: x.idElemento || '',
      idSegmento: x.idSegmento || '',
      patronNombre: x.patronNombre || '',
      parametro: x.parametro,
      motivo: x.motivo || '',
      vigenteDesde: x.vigenteDesde ? x.vigenteDesde.slice(0, 16) : '',
      vigenteHasta: x.vigenteHasta ? x.vigenteHasta.slice(0, 16) : '',
      activa: x.activa,
    })
  }

  const cancelar = () => {
    setEditandoId(null)
    setForm(VACIO)
  }

  const eliminar = async (id) => {
    if (!window.confirm('¿Eliminar esta excepción?')) return
    try {
      await api.del(`/api/excepciones/${id}`)
      cargar()
    } catch (err) {
      setError(err.message)
    }
  }

  const desactivar = async (id) => {
    try {
      await api.put(`/api/excepciones/${id}/desactivar`)
      cargar()
    } catch (err) {
      setError(err.message)
    }
  }

  const referencia = (x) => {
    if (x.alcance === 'ELEMENTO') {
      return elementos.find((e) => e.idElemento === x.idElemento)?.nombre ?? `elemento ${x.idElemento}`
    }
    if (x.alcance === 'SEGMENTO') {
      return segmentos.find((s) => s.idSegmento === x.idSegmento)?.nombre ?? `segmento ${x.idSegmento}`
    }
    if (x.alcance === 'PATRON') {
      return `nombre como "${x.patronNombre}"`
    }
    return 'todo el sistema'
  }

  return (
    <div>
      <h1>Excepciones</h1>
      <p className="subtitle">
        RF05-08: evitan que un parámetro fuera de rango se marque como desvío para un elemento puntual, los
        elementos cuyo nombre coincide con un patrón (ej. <code>*_test_*</code>), un segmento entero, o todo el
        sistema, mientras la excepción esté vigente y activa. Con parámetro <code>*</code> cubre todos los
        parámetros.
      </p>

      {error && <p className="error">{error}</p>}

      <form className="form-card" onSubmit={guardar}>
        <div className="form-row">
          <label>
            Alcance
            <select value={form.alcance} onChange={(e) => setForm({ ...form, alcance: e.target.value })}>
              <option value="GLOBAL">Global (todo el sistema)</option>
              <option value="SEGMENTO">Segmento</option>
              <option value="ELEMENTO">Elemento puntual</option>
              <option value="PATRON">Patrón de nombre</option>
            </select>
          </label>

          {form.alcance === 'PATRON' && (
            <label>
              Patrón de nombre
              <input
                required
                value={form.patronNombre}
                onChange={(e) => setForm({ ...form, patronNombre: e.target.value })}
                placeholder="ej: *_test_*  (* = cualquier texto, ? = un caracter)"
              />
            </label>
          )}

          {form.alcance === 'SEGMENTO' && (
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
          )}

          {form.alcance === 'ELEMENTO' && (
            <label>
              Elemento
              <select required value={form.idElemento} onChange={(e) => setForm({ ...form, idElemento: e.target.value })}>
                <option value="">Seleccionar...</option>
                {elementos.map((el) => (
                  <option key={el.idElemento} value={el.idElemento}>
                    {el.nombre}
                  </option>
                ))}
              </select>
            </label>
          )}

          <label>
            Parámetro
            <input
              required
              value={form.parametro}
              onChange={(e) => setForm({ ...form, parametro: e.target.value })}
              placeholder="ej: PotenciaTX_dBm, o * para todos"
            />
          </label>
        </div>

        <div className="form-row">
          <label>
            Motivo
            <input value={form.motivo} onChange={(e) => setForm({ ...form, motivo: e.target.value })} />
          </label>
          <label>
            Vigente desde
            <input
              type="datetime-local"
              value={form.vigenteDesde}
              onChange={(e) => setForm({ ...form, vigenteDesde: e.target.value })}
            />
          </label>
          <label>
            Vigente hasta
            <input
              type="datetime-local"
              value={form.vigenteHasta}
              onChange={(e) => setForm({ ...form, vigenteHasta: e.target.value })}
            />
          </label>
          <label>
            Activa
            <select
              value={form.activa ? 'true' : 'false'}
              onChange={(e) => setForm({ ...form, activa: e.target.value === 'true' })}
            >
              <option value="true">Sí</option>
              <option value="false">No</option>
            </select>
          </label>
        </div>

        <div className="form-actions">
          <button type="submit">{editandoId ? 'Guardar cambios' : 'Cargar excepción'}</button>
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
            <th>Alcance</th>
            <th>Referencia</th>
            <th>Parámetro</th>
            <th>Vigencia</th>
            <th>Activa</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {excepciones.map((x) => (
            <tr key={x.idExcepcion}>
              <td>{x.idExcepcion}</td>
              <td>{x.alcance}</td>
              <td>{referencia(x)}</td>
              <td>{x.parametro}</td>
              <td>
                {x.vigenteDesde ? new Date(x.vigenteDesde).toLocaleDateString('es-AR') : 'sin límite'} —{' '}
                {x.vigenteHasta ? new Date(x.vigenteHasta).toLocaleDateString('es-AR') : 'sin límite'}
              </td>
              <td>{x.activa ? 'Sí' : 'No'}</td>
              <td className="row-actions">
                <button onClick={() => editar(x)}>Editar</button>
                {x.activa && (
                  <button className="secondary" onClick={() => desactivar(x.idExcepcion)}>
                    Desactivar
                  </button>
                )}
                <button className="danger" onClick={() => eliminar(x.idExcepcion)}>
                  Eliminar
                </button>
              </td>
            </tr>
          ))}
          {excepciones.length === 0 && (
            <tr>
              <td colSpan={7}>No hay excepciones cargadas todavía.</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
