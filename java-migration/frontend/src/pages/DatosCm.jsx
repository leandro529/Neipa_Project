import { Fragment, useEffect, useState } from 'react'
import { api } from '../api/client.js'
import ConexionesCm from './ConexionesCm.jsx'

const fmt = (n) => (n === null || n === undefined ? '—' : Number(n).toLocaleString('es-AR'))

/**
 * Datos de CM de NetAct: importación del dump SQLite (inventario LNBTS/LNCEL y
 * los parámetros que tienen regla) y evolución de las corridas de auditoría.
 */
export default function DatosCm() {
  const [archivos, setArchivos] = useState([])
  const [archivo, setArchivo] = useState('')
  const [conexiones, setConexiones] = useState([])
  const [importaciones, setImportaciones] = useState([])
  const [objetos, setObjetos] = useState([])
  const [corridas, setCorridas] = useState([])
  const [segmentos, setSegmentos] = useState([])
  const [resumen, setResumen] = useState({})
  const [error, setError] = useState(null)

  const cargar = () => {
    api.get('/api/cm/importaciones').then(setImportaciones).catch((e) => setError(e.message))
    api.get('/api/cm/objetos/resumen').then(setObjetos).catch((e) => setError(e.message))
    api.get('/api/cm/corridas').then(setCorridas).catch((e) => setError(e.message))
    api.get('/api/segmentos').then(setSegmentos).catch((e) => setError(e.message))
  }

  useEffect(() => {
    api
      .get('/api/cm/archivos')
      .then((lista) => {
        setArchivos(lista)
        if (lista.length) setArchivo('archivo:' + lista[0])
      })
      .catch((e) => setError(e.message))
    cargar()
  }, [])

  const enCurso = importaciones.some((i) => i.estado === 'EN_CURSO')
  useEffect(() => {
    if (!enCurso) return undefined
    const t = setInterval(cargar, 4000)
    return () => clearInterval(t)
  }, [enCurso])

  const importar = async () => {
    setError(null)
    try {
      const cuerpo = archivo.startsWith('conexion:')
        ? { idConexion: Number(archivo.substring('conexion:'.length)) }
        : { archivo: archivo.substring('archivo:'.length) }
      await api.post('/api/cm/importaciones', cuerpo)
      cargar()
    } catch (err) {
      setError(err.message)
    }
  }

  const verResumen = async (id) => {
    if (resumen[id]) {
      const copia = { ...resumen }
      delete copia[id]
      setResumen(copia)
      return
    }
    try {
      const filas = await api.get(`/api/cm/corridas/${id}/resumen`)
      setResumen({ ...resumen, [id]: filas })
    } catch (err) {
      setError(err.message)
    }
  }

  const nombreSegmento = (id) => segmentos.find((s) => s.idSegmento === id)?.nombre ?? id
  const maxDesvios = Math.max(1, ...corridas.map((c) => c.desvios))
  const porClase = objetos.reduce((acc, o) => {
    acc[o.clase] = acc[o.clase] || {}
    acc[o.clase][o.rc] = o.objetos
    return acc
  }, {})
  const rcs = [...new Set(objetos.map((o) => o.rc))].sort()

  return (
    <div>
      <h1>Datos CM</h1>
      <p className="subtitle">
        Dump SQLite de NetAct (carpeta <code>external_data</code> del repositorio) o una base de datos con la misma
        estructura (MySQL/MariaDB, PostgreSQL, Oracle o SQL Server; ver Conexiones más abajo). Se importa el inventario de LNBTS
        y LNCEL y, de cada clase que usan las reglas, solo los parámetros con regla; no se importan las vecindades
        (LNREL, LNADJ). Si agregás una regla de un parámetro nuevo, volvé a importar el dump. Cada auditoría de un
        segmento con RC es una corrida: se guardan solo los desvíos y los exceptuados, y los totales.
      </p>

      {error && <p className="error">{error}</p>}

      <div className="form-card">
        <div className="toolbar">
          <label>
            Origen
            <select value={archivo} onChange={(e) => setArchivo(e.target.value)}>
              {archivos.length === 0 && conexiones.length === 0 && (
                <option value="">(no hay dumps en la carpeta ni conexiones)</option>
              )}
              {archivos.length > 0 && (
                <optgroup label="Dump SQLite (carpeta external_data)">
                  {archivos.map((a) => (
                    <option key={a} value={'archivo:' + a}>
                      {a}
                    </option>
                  ))}
                </optgroup>
              )}
              {conexiones.length > 0 && (
                <optgroup label="Conexión a base de datos">
                  {conexiones.map((c) => (
                    <option key={c.idConexion} value={'conexion:' + c.idConexion}>
                      {c.nombre} ({c.motor})
                    </option>
                  ))}
                </optgroup>
              )}
            </select>
          </label>
          <button disabled={!archivo || enCurso} onClick={importar}>
            {enCurso ? 'Importando…' : 'Importar'}
          </button>
          <button className="secondary" onClick={cargar}>
            Actualizar
          </button>
        </div>
      </div>

      <ConexionesCm onCambio={setConexiones} />

      <h3 className="form-subtitle">Importaciones</h3>
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Origen</th>
            <th>Fecha del dump</th>
            <th>Estado</th>
            <th>Objetos</th>
            <th>Valores</th>
            <th>Avisos</th>
          </tr>
        </thead>
        <tbody>
          {importaciones.map((i) => (
            <tr key={i.id_importacion}>
              <td>{i.id_importacion}</td>
              <td className="mono">{i.archivo}</td>
              <td>{i.fecha_dump || '—'}</td>
              <td>
                <span className={'badge ' + (i.estado === 'OK' ? 'badge-conforme' : i.estado === 'ERROR' ? 'badge-desvio' : 'badge-exceptuado')}>
                  {i.estado}
                </span>
              </td>
              <td>{fmt(i.objetos)}</td>
              <td>{fmt(i.valores)}</td>
              <td className="muted" style={{ whiteSpace: 'pre-wrap', maxWidth: 420 }}>
                {i.mensaje}
              </td>
            </tr>
          ))}
          {importaciones.length === 0 && (
            <tr>
              <td colSpan={7}>Todavía no se importó ningún dump.</td>
            </tr>
          )}
        </tbody>
      </table>

      {objetos.length > 0 && (
        <>
          <h3 className="form-subtitle">Objetos importados por clase y RC</h3>
          <table>
            <thead>
              <tr>
                <th>Clase</th>
                {rcs.map((rc) => (
                  <th key={rc}>{rc}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {Object.keys(porClase).map((clase) => (
                <tr key={clase}>
                  <td>{clase}</td>
                  {rcs.map((rc) => (
                    <td key={rc}>{fmt(porClase[clase][rc])}</td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}

      <h3 className="form-subtitle">Corridas de auditoría</h3>
      <p className="hint">La barra muestra los desvíos de cada corrida: a medida que se aplican los planes de reconfiguración, debería bajar.</p>
      <table>
        <thead>
          <tr>
            <th>Corrida</th>
            <th>Fecha</th>
            <th>Segmento</th>
            <th>Objetos</th>
            <th>Comparaciones</th>
            <th>Conformes</th>
            <th>Desvíos</th>
            <th>Exceptuados</th>
            <th>Conformidad</th>
            <th>Duración</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {corridas
            .slice()
            .reverse()
            .map((c) => (
              <Fragment key={c.idCorrida}>
                <tr>
                  <td>{c.idCorrida}</td>
                  <td>{c.fecha ? new Date(c.fecha).toLocaleString('es-AR') : '—'}</td>
                  <td>{nombreSegmento(c.idSegmento)}</td>
                  <td>{fmt(c.objetosEvaluados)}</td>
                  <td>{fmt(c.comparaciones)}</td>
                  <td>{fmt(c.conformes)}</td>
                  <td>
                    {fmt(c.desvios)}
                    <div className="bar-track" style={{ marginTop: 4 }}>
                      <div className="bar-fill" style={{ width: `${(c.desvios * 100) / maxDesvios}%` }} />
                    </div>
                  </td>
                  <td>{fmt(c.exceptuados)}</td>
                  <td>{c.tasaConformidadPct.toFixed(1)} %</td>
                  <td>{(c.duracionMs / 1000).toFixed(1)} s</td>
                  <td>
                    <button className="secondary" onClick={() => verResumen(c.idCorrida)}>
                      {resumen[c.idCorrida] ? 'Ocultar' : 'Por regla'}
                    </button>
                  </td>
                </tr>
                {resumen[c.idCorrida] && (
                  <tr>
                    <td colSpan={11}>
                      <table>
                        <thead>
                          <tr>
                            <th>Regla</th>
                            <th>Clase</th>
                            <th>Parámetro</th>
                            <th>Conformes</th>
                            <th>Desvíos</th>
                            <th>Exceptuados</th>
                          </tr>
                        </thead>
                        <tbody>
                          {resumen[c.idCorrida].map((r) => (
                            <tr key={r.idValorEstandar}>
                              <td>{r.idValorEstandar}</td>
                              <td>{r.clase}</td>
                              <td>{r.parametro}</td>
                              <td>{fmt(r.conformes)}</td>
                              <td>{fmt(r.desvios)}</td>
                              <td>{fmt(r.exceptuados)}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </td>
                  </tr>
                )}
              </Fragment>
            ))}
          {corridas.length === 0 && (
            <tr>
              <td colSpan={11}>Todavía no hay corridas. Auditá un segmento con RC desde la pantalla Segmentos.</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
