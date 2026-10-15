import { useEffect, useRef, useState } from 'react'
import { api } from '../api/client.js'

/** Clase del CM de un objeto gestionado: "LNCEL-1/SIB-0" -> SIB, vacío -> LNBTS (como CmNombres). */
export function claseDe(objetoGestionado) {
  const o = (objetoGestionado || '').trim()
  if (!o) return 'LNBTS'
  const ultimo = o.substring(o.lastIndexOf('/') + 1)
  const guion = ultimo.indexOf('-')
  return (guion > 0 ? ultimo.substring(0, guion) : ultimo).toUpperCase()
}

/** Texto de los valores permitidos de un parámetro del catálogo. */
export function permitido(p) {
  if (p.enumeracion) {
    const valores = p.enumeracion.split('|')
    return valores.slice(0, 12).join(', ') + (valores.length > 12 ? ` … (${valores.length})` : '')
  }
  if (p.minimoInterno !== null && p.minimoInterno !== undefined) {
    return `${p.minimoInterno} … ${p.maximoInterno}`
  }
  return p.rango ? p.rango.split('\n')[0] : '—'
}

const formulaUtil = (p) => p.formula && /[*/+]/.test(p.formula) && !p.formula.includes('* 1 / 1')

/** Ficha de un parámetro (o de una clase y un nombre) buscada en el catálogo. */
export function useFicha(clase, parametro) {
  const [fichas, setFichas] = useState([])
  useEffect(() => {
    if (!parametro) {
      setFichas([])
      return undefined
    }
    const t = setTimeout(() => {
      const q = new URLSearchParams({ parametro, objetoGestionado: clase || '' })
      api
        .get('/api/catalogo/ficha?' + q.toString())
        .then(setFichas)
        .catch(() => setFichas([]))
    }, 250)
    return () => clearTimeout(t)
  }, [clase, parametro])
  return fichas
}

/** Clases del catálogo (con ruta y objetos padre). */
export function useClases() {
  const [clases, setClases] = useState([])
  useEffect(() => {
    api.get('/api/catalogo/clases').then(setClases).catch(() => setClases([]))
  }, [])
  return clases
}

/** Objeto gestionado: escribir o elegir una clase del catálogo. */
export function SelectorClase({ valor, onChange, clases, id = 'clases-catalogo' }) {
  return (
    <>
      <input
        list={id}
        value={valor}
        onChange={(e) => onChange(e.target.value)}
        placeholder="LNCEL, SIB, LNCEL_FDD… (vacío = LNBTS)"
      />
      <datalist id={id}>
        {clases.map((c) => (
          <option key={c.clase} value={c.clase}>
            {c.ruta} · {c.parametros} parámetros
          </option>
        ))}
      </datalist>
    </>
  )
}

/**
 * Buscador de parámetros de una clase: al escribir muestra los del catálogo
 * con su nombre, valores permitidos y default. También acepta un nombre que
 * no esté en el catálogo (por ejemplo, los de la demo).
 */
export function SelectorParametro({ clase, valor, onChange, placeholder, required }) {
  const [abierto, setAbierto] = useState(false)
  const [opciones, setOpciones] = useState([])
  const [activo, setActivo] = useState(-1)
  const caja = useRef(null)

  useEffect(() => {
    if (!abierto) return undefined
    const t = setTimeout(() => {
      const q = new URLSearchParams({ clase: clase || 'LNBTS', limite: '60' })
      if (valor) q.set('texto', valor)
      api
        .get('/api/catalogo?' + q.toString())
        .then((lista) => {
          const vistos = new Set()
          setOpciones(lista.filter((p) => !vistos.has(p.parametro) && vistos.add(p.parametro)).slice(0, 25))
          setActivo(-1)
        })
        .catch(() => setOpciones([]))
    }, 200)
    return () => clearTimeout(t)
  }, [clase, valor, abierto])

  useEffect(() => {
    const cerrar = (e) => {
      if (caja.current && !caja.current.contains(e.target)) setAbierto(false)
    }
    document.addEventListener('mousedown', cerrar)
    return () => document.removeEventListener('mousedown', cerrar)
  }, [])

  const elegir = (p) => {
    onChange(p.parametro)
    setAbierto(false)
  }

  const teclas = (e) => {
    if (!abierto || opciones.length === 0) return
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setActivo((a) => Math.min(opciones.length - 1, a + 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setActivo((a) => Math.max(0, a - 1))
    } else if (e.key === 'Enter' && activo >= 0) {
      e.preventDefault()
      elegir(opciones[activo])
    } else if (e.key === 'Escape') {
      setAbierto(false)
    }
  }

  return (
    <div className="selector" ref={caja}>
      <input
        required={required}
        value={valor}
        onChange={(e) => {
          onChange(e.target.value)
          setAbierto(true)
        }}
        onFocus={() => setAbierto(true)}
        onKeyDown={teclas}
        placeholder={placeholder || `buscar un parámetro de ${clase || 'LNBTS'}`}
        autoComplete="off"
        role="combobox"
        aria-expanded={abierto}
      />
      {abierto && opciones.length > 0 && (
        <ul className="selector-lista" role="listbox">
          {opciones.map((p, i) => (
            <li
              key={p.parametro}
              role="option"
              aria-selected={i === activo}
              className={i === activo ? 'activo' : ''}
              onMouseDown={(e) => {
                e.preventDefault()
                elegir(p)
              }}
            >
              <span className="mono">{p.parametro}</span>
              <span className="muted"> · {p.nombre}</span>
              <div className="selector-valores">
                {permitido(p)}
                {p.unidad ? ` ${p.unidad}` : ''}
                {p.defaultValor ? ` · default ${p.defaultValor}` : ''}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

/** Cartel corto con default y valores permitidos (para una condición). */
export function CartelParametro({ fichas }) {
  if (!fichas || fichas.length === 0) return null
  const p = fichas[0]
  return (
    <div className="cartel">
      <span className="mono">
        {p.clase}.{p.parametro}
      </span>{' '}
      · permitido: <span className="mono">{permitido(p)}</span>
      {p.defaultValor ? (
        <>
          {' '}
          · default: <span className="mono">{p.defaultValor}</span>
        </>
      ) : null}
    </div>
  )
}

/** Ficha del parámetro con el default y el rango; "Ver detalle" muestra todo lo del diccionario. */
export function FichaParametro({ fichas, sinCatalogo }) {
  const [detalle, setDetalle] = useState(false)
  if (!fichas || fichas.length === 0) {
    return sinCatalogo ? <p className="hint">{sinCatalogo}</p> : null
  }
  const p = fichas[0]
  const bloqueo = p.modificacion && p.modificacion.toLowerCase().includes('locking')
  return (
    <div className="ficha">
      <div className="ficha-titulo">
        <strong className="mono">
          {p.clase}.{p.parametro}
        </strong>{' '}
        — {p.nombre}
      </div>
      <div className="ficha-datos">
        <div>
          <span className="ficha-etiqueta">Valor por default</span>
          <span className="mono">{p.defaultValor || '—'}</span>
          {p.defaultInterno && p.defaultInterno !== p.defaultValor && (
            <span className="muted"> (en el CM: {p.defaultInterno})</span>
          )}
        </div>
        <div>
          <span className="ficha-etiqueta">Permitido (valor del CM)</span>
          <span className="mono">{permitido(p)}</span>
          {p.unidad ? ` ${p.unidad}` : ''}
        </div>
        {p.rango && !p.enumeracion && (
          <div>
            <span className="ficha-etiqueta">Rango del documento</span>
            {p.rango.split('\n')[0]}
            {formulaUtil(p) ? <span className="muted"> · {p.formula}</span> : null}
          </div>
        )}
        <div>
          <span className="ficha-etiqueta">Modificación</span>
          <span className={bloqueo ? 'texto-alerta' : ''}>{p.modificacion || '—'}</span>
        </div>
      </div>
      {fichas.length > 1 && (
        <div className="muted">
          También en: {fichas.slice(1).map((f) => `${f.fuente} (${permitido(f)})`).join(' · ')}
        </div>
      )}
      <button type="button" className="link" onClick={() => setDetalle(!detalle)}>
        {detalle ? 'Ocultar detalle' : 'Ver detalle del parámetro'}
      </button>
      {detalle && (
        <div className="ficha-detalle">
          <p className="muted mono">
            {p.rutaClase} · {p.tipo}
            {p.tecnologia ? ` · ${p.tecnologia}` : ''} · {p.fuente}
          </p>
          {p.descripcion && <p>{p.descripcion}</p>}
          {p.enumeracion && (
            <p>
              <strong>Valores:</strong> {p.enumeracion.split('|').join(', ')}
            </p>
          )}
          {p.formula && (
            <p>
              <strong>Valor interno:</strong> {p.formula}
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
        </div>
      )}
    </div>
  )
}
