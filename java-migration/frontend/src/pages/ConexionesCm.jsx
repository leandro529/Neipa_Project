import { useEffect, useState } from 'react'
import { api } from '../api/client.js'

const VACIA = { nombre: '', motor: 'POSTGRESQL', url: '', usuario: '', contrasena: '', esquema: '' }

/**
 * Conexiones directas a bases con la estructura del CM de NetAct (NSN):
 * MySQL/MariaDB, PostgreSQL, Oracle o SQL Server. La contraseña se envía al
 * guardar y el backend la guarda cifrada; nunca vuelve en las respuestas.
 */
export default function ConexionesCm({ onCambio }) {
  const [conexiones, setConexiones] = useState([])
  const [motores, setMotores] = useState([])
  const [form, setForm] = useState(VACIA)
  const [editando, setEditando] = useState(null)
  const [tieneContrasena, setTieneContrasena] = useState(false)
  const [prueba, setPrueba] = useState(null)
  const [probando, setProbando] = useState(false)
  const [error, setError] = useState(null)

  const cargar = () =>
    api
      .get('/api/cm/conexiones')
      .then((lista) => {
        setConexiones(lista)
        if (onCambio) onCambio(lista)
      })
      .catch((e) => setError(e.message))

  useEffect(() => {
    api.get('/api/cm/motores').then(setMotores).catch((e) => setError(e.message))
    cargar()
  }, [])

  const ejemplo = (motor) => motores.find((m) => m.motor === motor)?.urlEjemplo || ''

  const cuerpo = () => ({
    idConexion: editando,
    nombre: form.nombre,
    motor: form.motor,
    url: form.url,
    usuario: form.usuario || null,
    contrasena: form.contrasena || null,
    esquema: form.esquema || null,
  })

  const guardar = async (e) => {
    e.preventDefault()
    setError(null)
    try {
      if (editando) await api.put(`/api/cm/conexiones/${editando}`, cuerpo())
      else await api.post('/api/cm/conexiones', cuerpo())
      cancelar()
      cargar()
    } catch (err) {
      setError(err.message)
    }
  }

  const probarFormulario = async () => {
    setError(null)
    setProbando(true)
    try {
      setPrueba(await api.post('/api/cm/conexiones/prueba', cuerpo()))
    } catch (err) {
      setError(err.message)
    } finally {
      setProbando(false)
    }
  }

  const probarGuardada = async (id) => {
    setError(null)
    setProbando(true)
    try {
      setPrueba(await api.post(`/api/cm/conexiones/${id}/prueba`))
      cargar()
    } catch (err) {
      setError(err.message)
    } finally {
      setProbando(false)
    }
  }

  const editar = (c) => {
    setEditando(c.idConexion)
    setTieneContrasena(c.tieneContrasena)
    setForm({ nombre: c.nombre, motor: c.motor, url: c.url, usuario: c.usuario || '', contrasena: '', esquema: c.esquema || '' })
    setPrueba(null)
  }

  const cancelar = () => {
    setEditando(null)
    setTieneContrasena(false)
    setForm(VACIA)
    setPrueba(null)
  }

  const borrar = async (c) => {
    if (!window.confirm(`¿Borrar la conexión ${c.nombre}?`)) return
    setError(null)
    try {
      await api.del(`/api/cm/conexiones/${c.idConexion}`)
      if (editando === c.idConexion) cancelar()
      cargar()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div>
      <h3 className="form-subtitle">Conexiones a bases de CM</h3>
      <p className="muted">
        Bases con la misma estructura que el dump de NetAct (una tabla por clase: LNBTS, LNCEL, SIB…, con distName,
        PLMN_id, LNBTS_id y una columna por parámetro). Se leen en solo lectura; conviene un usuario con permiso de
        SELECT únicamente. Si la base corre en esta PC y NEIPA en Docker, usá <code>host.docker.internal</code> como
        servidor.
      </p>
      {error && <p className="error">{error}</p>}

      <form className="form-card" onSubmit={guardar}>
        <div className="form-row">
          <label>
            Nombre
            <input value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} required />
          </label>
          <label>
            Motor
            <select
              value={form.motor}
              onChange={(e) => setForm({ ...form, motor: e.target.value, url: form.url || ejemplo(e.target.value) })}
            >
              {motores.map((m) => (
                <option key={m.motor} value={m.motor}>
                  {m.motor === 'MYSQL' ? 'MySQL / MariaDB' : m.motor === 'SQLSERVER' ? 'SQL Server' : m.motor === 'POSTGRESQL' ? 'PostgreSQL' : 'Oracle'}
                </option>
              ))}
            </select>
          </label>
          <label style={{ flex: 2 }}>
            URL JDBC
            <input
              className="mono"
              value={form.url}
              onChange={(e) => setForm({ ...form, url: e.target.value })}
              placeholder={ejemplo(form.motor)}
              required
            />
          </label>
        </div>
        <div className="form-row">
          <label>
            Usuario
            <input value={form.usuario} onChange={(e) => setForm({ ...form, usuario: e.target.value })} autoComplete="off" />
          </label>
          <label>
            Contraseña
            <input
              type="password"
              value={form.contrasena}
              onChange={(e) => setForm({ ...form, contrasena: e.target.value })}
              placeholder={tieneContrasena ? '(sin cambios)' : ''}
              autoComplete="new-password"
            />
          </label>
          <label>
            Esquema
            <input
              value={form.esquema}
              onChange={(e) => setForm({ ...form, esquema: e.target.value })}
              placeholder={form.motor === 'MYSQL' ? 'base (vacío = la de la URL)' : 'vacío = el del usuario'}
            />
          </label>
        </div>
        <div className="toolbar">
          <button type="submit">{editando ? 'Guardar cambios' : 'Agregar conexión'}</button>
          <button type="button" className="secondary" disabled={probando || !form.url} onClick={probarFormulario}>
            {probando ? 'Probando…' : 'Probar'}
          </button>
          {editando && (
            <button type="button" className="secondary" onClick={cancelar}>
              Cancelar
            </button>
          )}
        </div>
        {prueba && <p className={prueba.ok ? 'ok' : 'error'}>{prueba.mensaje} ({prueba.milisegundos} ms)</p>}
      </form>

      {conexiones.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Motor</th>
              <th>URL</th>
              <th>Usuario</th>
              <th>Esquema</th>
              <th>Última prueba</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {conexiones.map((c) => (
              <tr key={c.idConexion}>
                <td>{c.nombre}</td>
                <td>{c.motor}</td>
                <td className="mono">{c.url}</td>
                <td>
                  {c.usuario || '—'}{c.tieneContrasena ? ' · con contraseña' : ''}
                </td>
                <td>{c.esquema || '—'}</td>
                <td className="muted">{c.ultimoResultado || '—'}</td>
                <td style={{ whiteSpace: 'nowrap' }}>
                  <button className="secondary" disabled={probando} onClick={() => probarGuardada(c.idConexion)}>
                    Probar
                  </button>{' '}
                  <button className="secondary" onClick={() => editar(c)}>
                    Editar
                  </button>{' '}
                  <button className="secondary" onClick={() => borrar(c)}>
                    Borrar
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
