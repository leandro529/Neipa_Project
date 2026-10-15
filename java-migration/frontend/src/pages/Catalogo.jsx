import { Fragment, useEffect, useState } from 'react'
import { api } from '../api/client.js'
import { permitido } from '../components/CatalogoUi.jsx'

/**
 * Catálogo de parámetros de Nokia (diccionarios LTE / SBTS / 5G convertidos a
 * CSV). Sirve para consultar un parámetro y para validar las reglas.
 */
export default function Catalogo() {
  const [archivos, setArchivos] = useState([])
  const [archivo, setArchivo] = useState('')
  const [resumen, setResumen] = useState([])
  const [texto, setTexto] = useState('')
  const [clase, setClase] = useState('')
  const [resultados, setResultados] = useState([])
  const [abierto, setAbierto] = useState(null)
  const [error, setError] = useState(null)
  const [mensaje, setMensaje] = useState(null)
  const [importando, setImportando] = useState(false)

  const cargarResumen = () => api.get('/api/catalogo/resumen').then(setResumen).catch((e) => setError(e.message))

  useEffect(() => {
    api
      .get('/api/catalogo/archivos')
      .then((lista) => {
        setArchivos(lista)
        const nokia = lista.find((a) => a.startsWith('catalogo')) || lista[0]
        if (nokia) setArchivo(nokia)
      })
      .catch((e) => setError(e.message))
    cargarResumen()
  }, [])

  const importar = async () => {
    setError(null)
    setMensaje(null)
    setImportando(true)
    try {
      const r = await api.post('/api/catalogo/importaciones', { archivo })
      setMensaje(`Catálogo importado: ${Number(r.parametros).toLocaleString('es-AR')} parámetros.`)
      cargarResumen()
    } catch (err) {
      setError(err.message)
    } finally {
      setImportando(false)
    }
  }

  const buscar = async (e) => {
    if (e) e.preventDefault()
    setError(null)
    const q = new URLSearchParams()
    if (texto) q.set('texto', texto)
    if (clase) q.set('clase', clase)
    q.set('limite', '200')
    try {
      setResultados(await api.get('/api/catalogo?' + q.toString()))
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div>
      <h1>Catálogo de parámetros</h1>
      <p className="subtitle">
        Diccionarios de parámetros de Nokia (LTE, SBTS, 5G) convertidos a CSV con{' '}
        <code>herramientas/convertir_diccionarios_nokia.py</code> y guardados en <code>external_data</code>. Al guardar
        una regla, su valor se valida contra el rango o la enumeración del parámetro (en el valor interno que guarda el
        CM, por ejemplo pMax 430 = 43 dBm). Si hay varias releases, alcanza con que una lo acepte.
      </p>

      {error && <p className="error">{error}</p>}
      {mensaje && <p className="ok">{mensaje}</p>}

      <div className="form-card">
        <div className="toolbar">
          <label>
            Archivo
            <select value={archivo} onChange={(e) => setArchivo(e.target.value)}>
              {archivos.length === 0 && <option value="">(no hay .csv en la carpeta)</option>}
              {archivos.map((a) => (
                <option key={a} value={a}>
                  {a}
                </option>
              ))}
            </select>
          </label>
          <button disabled={!archivo || importando} onClick={importar}>
            {importando ? 'Importando…' : 'Importar catálogo'}
          </button>
        </div>
        <table>
          <thead>
            <tr>
              <th>Fuente</th>
              <th>Release</th>
              <th className="num">Parámetros</th>
              <th className="num">Clases</th>
            </tr>
          </thead>
          <tbody>
            {resumen.length === 0 && (
              <tr>
                <td colSpan={4} className="muted">
                  El catálogo está vacío.
                </td>
              </tr>
            )}
            {resumen.map((r) => (
              <tr key={r.fuente}>
                <td className="mono">{r.fuente}</td>
                <td>{r.releaseDoc}</td>
                <td className="num">{Number(r.parametros).toLocaleString('es-AR')}</td>
                <td className="num">{r.clases}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <form className="form-card" onSubmit={buscar}>
        <div className="toolbar">
          <label>
            Parámetro o nombre
            <input value={texto} onChange={(e) => setTexto(e.target.value)} placeholder="n310Qci1, pMax, DRX…" />
          </label>
          <label>
            Clase
            <input value={clase} onChange={(e) => setClase(e.target.value)} placeholder="LNCEL, SIB, NRCELL…" />
          </label>
          <button type="submit">Buscar</button>
        </div>
      </form>

      {resultados.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Clase</th>
              <th>Parámetro</th>
              <th>Nombre</th>
              <th>Valores</th>
              <th>Default</th>
              <th>Modificación</th>
              <th>Fuente</th>
            </tr>
          </thead>
          <tbody>
            {resultados.map((p) => (
              <Fragment key={p.idCatalogo}>
                <tr className="clickable" onClick={() => setAbierto(abierto === p.idCatalogo ? null : p.idCatalogo)}>
                  <td>{p.clase}</td>
                  <td className="mono">{p.parametro}</td>
                  <td>{p.nombre}</td>
                  <td className="mono">{permitido(p)}</td>
                  <td className="mono">{p.defaultValor || '—'}</td>
                  <td className={p.modificacion && p.modificacion.includes('locking') ? 'texto-alerta' : ''}>
                    {p.modificacion}
                  </td>
                  <td className="muted">{p.fuente}</td>
                </tr>
                {abierto === p.idCatalogo && (
                  <tr>
                    <td colSpan={7} style={{ whiteSpace: 'pre-wrap' }}>
                      <p className="muted mono">{p.rutaClase}</p>
                      <p>{p.descripcion}</p>
                      {p.rango && (
                        <p>
                          <strong>Rango:</strong> {p.rango}
                          {p.formula ? ` · ${p.formula}` : ''}
                        </p>
                      )}
                      {p.relacionados && (
                        <p>
                          <strong>Parámetros relacionados:</strong>
                          {'\n'}
                          {p.relacionados}
                        </p>
                      )}
                      {p.relaciones && (
                        <p>
                          <strong>Relaciones:</strong>
                          {'\n'}
                          {p.relaciones}
                        </p>
                      )}
                      {p.funciones && (
                        <p>
                          <strong>Funciones:</strong> {p.funciones}
                        </p>
                      )}
                    </td>
                  </tr>
                )}
              </Fragment>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
