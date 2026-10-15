import { useEffect, useState } from 'react'
import { api } from '../api/client.js'
import {
  CartelParametro,
  permitido,
  FichaParametro,
  SelectorClase,
  SelectorParametro,
  claseDe,
  useClases,
  useFicha,
} from '../components/CatalogoUi.jsx'

const VACIO = {
  parametro: '',
  operador: 'RANGO',
  minimo: '',
  maximo: '',
  valorReferencia: '',
  unidad: '',
  idSegmento: '',
  prioridad: 0,
  objetoGestionado: '',
  valorObjetivo: '',
  descripcion: '',
  condiciones: [],
}

const CONDICION_VACIA = { origen: 'ATRIBUTO_ELEMENTO', campo: 'NOMBRE', operador: 'COINCIDE', valor: '' }

const OPCIONES_OPERADOR = [
  { valor: 'RANGO', etiqueta: 'Rango (mínimo – máximo)' },
  { valor: 'IGUAL', etiqueta: 'Igual a (=)' },
  { valor: 'DISTINTO', etiqueta: 'Distinto de (<>)' },
  { valor: 'MENOR_QUE', etiqueta: 'Menor que (<)' },
  { valor: 'MENOR_IGUAL', etiqueta: 'Menor o igual (<=)' },
  { valor: 'MAYOR_QUE', etiqueta: 'Mayor que (>)' },
  { valor: 'MAYOR_IGUAL', etiqueta: 'Mayor o igual (>=)' },
  { valor: 'EN_LISTA', etiqueta: 'En una lista (in)' },
  { valor: 'COINCIDE', etiqueta: 'Coincide con patrón (like)' },
]

const ATRIBUTOS = [
  { valor: 'NOMBRE', etiqueta: 'Nombre del elemento' },
  { valor: 'TECNOLOGIA', etiqueta: 'Tecnología' },
  { valor: 'FABRICANTE', etiqueta: 'Fabricante' },
]

const SIMBOLOS = {
  IGUAL: '=',
  DISTINTO: '<>',
  MENOR_QUE: '<',
  MENOR_IGUAL: '<=',
  MAYOR_QUE: '>',
  MAYOR_IGUAL: '>=',
  EN_LISTA: 'in',
  COINCIDE: 'like',
}

const placeholderReferencia = (operador) => {
  if (operador === 'EN_LISTA') return 'valores separados por coma, ej: FDD, TDD'
  if (operador === 'COINCIDE') return 'patrón con * o % como comodín, ej: *_URB_*'
  return 'ej: 500000, -6, fixed...'
}

const valorEsperado = (v) => {
  if (v.operador === 'RANGO' || !v.operador) return `${v.minimo} – ${v.maximo}`
  return `${SIMBOLOS[v.operador] ?? v.operador} ${v.valorReferencia ?? ''}`
}

export default function Configuraciones() {
  const [valores, setValores] = useState([])
  const [segmentos, setSegmentos] = useState([])
  const [form, setForm] = useState(VACIO)
  const [editandoId, setEditandoId] = useState(null)
  const [error, setError] = useState(null)
  const clases = useClases()
  const claseRegla = claseDe(form.objetoGestionado)
  const ficha = useFicha(form.objetoGestionado, form.parametro)
  // Objetos padre de la clase de la regla (para condiciones sobre, por ejemplo, un parametro del LNBTS).
  const infoClase = clases.find((c) => c.clase === claseRegla)
  const padres = infoClase ? infoClase.ancestros : claseRegla === 'LNBTS' ? [] : ['LNBTS']

  // Un parametro enumerado (o booleano) se compara por igualdad: en una regla nueva se propone IGUAL.
  useEffect(() => {
    if (!editandoId && ficha.length && ficha[0].enumeracion && form.operador === 'RANGO' && !form.minimo && !form.maximo) {
      setForm((f) => ({ ...f, operador: 'IGUAL' }))
    }
  }, [ficha])

  const cargar = () => api.get('/api/configuraciones').then(setValores).catch((e) => setError(e.message))

  useEffect(() => {
    cargar()
    api.get('/api/segmentos').then(setSegmentos).catch((e) => setError(e.message))
  }, [])

  const nombreSegmento = (id) => segmentos.find((s) => s.idSegmento === id)?.nombre ?? `segmento ${id}`

  const aplicaA = (v) => {
    const partes = []
    if (v.idSegmento) partes.push(`segmento ${nombreSegmento(v.idSegmento)}`)
    for (const c of v.condiciones || []) {
      const sujeto = c.origen === 'PARAMETRO' ? c.campo : c.campo.toLowerCase()
      partes.push(`${sujeto} ${SIMBOLOS[c.operador] ?? c.operador} ${c.valor}`)
    }
    return partes.length ? partes.join(' y ') : 'General (todos los elementos)'
  }

  const guardar = async (e) => {
    e.preventDefault()
    setError(null)
    const esRango = form.operador === 'RANGO'
    const payload = {
      parametro: form.parametro,
      operador: form.operador,
      minimo: esRango ? Number(form.minimo) : null,
      maximo: esRango ? Number(form.maximo) : null,
      valorReferencia: esRango ? null : form.valorReferencia,
      unidad: form.unidad || null,
      idSegmento: form.idSegmento ? Number(form.idSegmento) : null,
      prioridad: Number(form.prioridad) || 0,
      objetoGestionado: form.objetoGestionado || null,
      valorObjetivo: form.valorObjetivo || null,
      descripcion: form.descripcion || null,
      condiciones: form.condiciones.map(({ origen, campo, operador, valor }) => ({ origen, campo, operador, valor })),
    }
    try {
      if (editandoId) {
        await api.put(`/api/configuraciones/${editandoId}`, payload)
      } else {
        await api.post('/api/configuraciones', payload)
      }
      setForm(VACIO)
      setEditandoId(null)
      cargar()
    } catch (err) {
      setError(err.message)
    }
  }

  const editar = (v) => {
    setEditandoId(v.idValorEstandar)
    setForm({
      parametro: v.parametro,
      operador: v.operador || 'RANGO',
      minimo: v.minimo ?? '',
      maximo: v.maximo ?? '',
      valorReferencia: v.valorReferencia ?? '',
      unidad: v.unidad || '',
      idSegmento: v.idSegmento || '',
      prioridad: v.prioridad ?? 0,
      objetoGestionado: v.objetoGestionado || '',
      valorObjetivo: v.valorObjetivo || '',
      descripcion: v.descripcion || '',
      condiciones: (v.condiciones || []).map(({ origen, campo, operador, valor }) => ({ origen, campo, operador, valor })),
    })
  }

  const cancelar = () => {
    setEditandoId(null)
    setForm(VACIO)
  }

  const eliminar = async (id) => {
    if (!window.confirm('¿Eliminar esta regla? Si era la única del parámetro, las auditorías van a fallar hasta que se cargue otra.')) return
    try {
      await api.del(`/api/configuraciones/${id}`)
      cargar()
    } catch (err) {
      setError(err.message)
    }
  }

  const cambiarCondicion = (i, cambios) => {
    const condiciones = form.condiciones.map((c, j) => (j === i ? { ...c, ...cambios } : c))
    setForm({ ...form, condiciones })
  }

  const agregarCondicion = () => setForm({ ...form, condiciones: [...form.condiciones, { ...CONDICION_VACIA }] })

  const quitarCondicion = (i) => setForm({ ...form, condiciones: form.condiciones.filter((_, j) => j !== i) })

  return (
    <div>
      <h1>Configuraciones</h1>
      <p className="subtitle">
        Reglas contra las que se compara el valor vigente de cada parámetro (RF02, RF04). Un parámetro puede
        tener varias reglas: una general, otra para un segmento entero, otra que depende del nombre o la
        tecnología del elemento, o del valor de otro parámetro. Al auditar se usa la regla de mayor prioridad
        y, a igual prioridad, la más específica.
      </p>

      {error && <p className="error">{error}</p>}

      <form className="form-card" onSubmit={guardar}>
        <div className="form-row">
          <label>
            Objeto gestionado (clase del CM)
            <SelectorClase
              valor={form.objetoGestionado}
              clases={clases}
              onChange={(v) => setForm({ ...form, objetoGestionado: v })}
            />
          </label>
          <label style={{ flex: 2 }}>
            Parámetro
            <SelectorParametro
              required
              clase={claseRegla}
              valor={form.parametro}
              onChange={(v) => setForm({ ...form, parametro: v })}
            />
          </label>
        </div>

        <FichaParametro
          fichas={ficha}
          sinCatalogo={
            form.parametro
              ? `Sin ficha en el catálogo para ${claseRegla}.${form.parametro}: su valor no se valida.`
              : null
          }
        />

        <div className="form-row">
          <label>
            Operador
            <select value={form.operador} onChange={(e) => setForm({ ...form, operador: e.target.value })}>
              {OPCIONES_OPERADOR.map((o) => (
                <option key={o.valor} value={o.valor}>
                  {o.etiqueta}
                </option>
              ))}
            </select>
          </label>

          {form.operador === 'RANGO' ? (
            <>
              <label>
                Mínimo
                <input
                  required
                  type="number"
                  step="0.01"
                  value={form.minimo}
                  onChange={(e) => setForm({ ...form, minimo: e.target.value })}
                />
              </label>
              <label>
                Máximo
                <input
                  required
                  type="number"
                  step="0.01"
                  value={form.maximo}
                  onChange={(e) => setForm({ ...form, maximo: e.target.value })}
                />
              </label>
            </>
          ) : (
            <label>
              Valor de referencia
              <input
                required
                value={form.valorReferencia}
                onChange={(e) => setForm({ ...form, valorReferencia: e.target.value })}
                placeholder={ficha.length ? `permitido: ${permitido(ficha[0])}` : placeholderReferencia(form.operador)}
              />
            </label>
          )}

          <label>
            Unidad
            <input value={form.unidad} onChange={(e) => setForm({ ...form, unidad: e.target.value })} placeholder="dBm" />
          </label>
        </div>

        <div className="form-row">
          <label>
            Aplica al segmento
            <select value={form.idSegmento} onChange={(e) => setForm({ ...form, idSegmento: e.target.value })}>
              <option value="">Todos los segmentos</option>
              {segmentos.map((s) => (
                <option key={s.idSegmento} value={s.idSegmento}>
                  {s.nombre}
                </option>
              ))}
            </select>
          </label>
          <label>
            Prioridad
            <input
              type="number"
              step="1"
              value={form.prioridad}
              onChange={(e) => setForm({ ...form, prioridad: e.target.value })}
            />
          </label>
          <label>
            Valor objetivo (XML)
            <input
              value={form.valorObjetivo}
              onChange={(e) => setForm({ ...form, valorObjetivo: e.target.value })}
              placeholder="vacío = se deriva de la regla"
            />
          </label>
        </div>

        <div className="form-row">
          <label style={{ flex: 1 }}>
            Descripción
            <input value={form.descripcion} onChange={(e) => setForm({ ...form, descripcion: e.target.value })} />
          </label>
        </div>

        <h3 className="form-subtitle">Condiciones (se tienen que cumplir todas)</h3>
        {form.condiciones.length === 0 && (
          <p className="hint">Sin condiciones: la regla aplica a todos los elementos (del segmento elegido, si hay).</p>
        )}
        {form.condiciones.map((c, i) => (
          <CondicionFila
            key={i}
            condicion={c}
            claseRegla={claseRegla}
            padres={padres}
            onChange={(cambios) => cambiarCondicion(i, cambios)}
            onQuitar={() => quitarCondicion(i)}
          />
        ))}

        <div className="form-actions">
          <button type="button" className="secondary" onClick={agregarCondicion}>
            Agregar condición
          </button>
          <button type="submit">{editandoId ? 'Guardar cambios' : 'Cargar regla'}</button>
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
            <th>Parámetro</th>
            <th>Valor esperado</th>
            <th>Aplica a</th>
            <th>Prioridad</th>
            <th>MO / valor objetivo</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {valores.map((v) => (
            <tr key={v.idValorEstandar}>
              <td>{v.idValorEstandar}</td>
              <td>
                {v.parametro}
                {v.descripcion && <div className="muted">{v.descripcion}</div>}
              </td>
              <td>
                {valorEsperado(v)} {v.unidad}
              </td>
              <td>{aplicaA(v)}</td>
              <td>{v.prioridad ?? 0}</td>
              <td>
                {v.objetoGestionado || 'LNBTS'}
                {v.valorObjetivo && <div className="muted">objetivo: {v.valorObjetivo}</div>}
              </td>
              <td className="row-actions">
                <button onClick={() => editar(v)}>Editar</button>
                <button className="danger" onClick={() => eliminar(v.idValorEstandar)}>
                  Eliminar
                </button>
              </td>
            </tr>
          ))}
          {valores.length === 0 && (
            <tr>
              <td colSpan={7}>No hay configuraciones cargadas todavía.</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  )
}

/** Separa "LNBTS.actFoo" en objeto padre ("LNBTS") y parametro ("actFoo"); sin punto, es del mismo objeto. */
function partesDelCampo(campo) {
  const punto = (campo || '').indexOf('.')
  return punto > 0 ? { objeto: campo.substring(0, punto).toUpperCase(), parametro: campo.substring(punto + 1) } : { objeto: '', parametro: campo || '' }
}

/** Una condicion: dato del elemento, o parametro del mismo objeto o de un objeto padre (LNBTS, LNCEL…). */
function CondicionFila({ condicion: c, claseRegla, padres, onChange, onQuitar }) {
  const { objeto, parametro } = partesDelCampo(c.campo)
  const claseCondicion = objeto || claseRegla
  const fichas = useFicha(c.origen === 'PARAMETRO' ? claseCondicion : '', c.origen === 'PARAMETRO' ? parametro : '')
  const opcionesObjeto = [...padres]
  if (objeto && !opcionesObjeto.includes(objeto)) opcionesObjeto.push(objeto)
  const armar = (obj, param) => (obj ? `${obj}.${param}` : param)

  return (
    <div className="condicion">
      <div className="form-row condicion-row">
        <label>
          Según
          <select
            value={c.origen}
            onChange={(e) =>
              onChange({
                origen: e.target.value,
                campo: e.target.value === 'PARAMETRO' ? '' : 'NOMBRE',
                operador: e.target.value === 'PARAMETRO' ? 'IGUAL' : 'COINCIDE',
              })
            }
          >
            <option value="ATRIBUTO_ELEMENTO">Dato del elemento</option>
            <option value="PARAMETRO">Parámetro</option>
          </select>
        </label>
        {c.origen === 'PARAMETRO' ? (
          <>
            <label>
              Del objeto
              <select value={objeto} onChange={(e) => onChange({ campo: armar(e.target.value, parametro) })}>
                <option value="">el mismo ({claseRegla})</option>
                {opcionesObjeto.map((o) => (
                  <option key={o} value={o}>
                    padre: {o}
                  </option>
                ))}
              </select>
            </label>
            <label style={{ flex: 2 }}>
              Parámetro
              <SelectorParametro
                required
                clase={claseCondicion}
                valor={parametro}
                onChange={(v) => onChange({ campo: armar(objeto, v) })}
              />
            </label>
          </>
        ) : (
          <label>
            Dato
            <select value={c.campo} onChange={(e) => onChange({ campo: e.target.value })}>
              {ATRIBUTOS.map((a) => (
                <option key={a.valor} value={a.valor}>
                  {a.etiqueta}
                </option>
              ))}
            </select>
          </label>
        )}
        <label>
          Operador
          <select value={c.operador} onChange={(e) => onChange({ operador: e.target.value })}>
            {OPCIONES_OPERADOR.filter((o) => o.valor !== 'RANGO').map((o) => (
              <option key={o.valor} value={o.valor}>
                {o.etiqueta}
              </option>
            ))}
          </select>
        </label>
        <label>
          Valor
          <input
            required
            value={c.valor}
            onChange={(e) => onChange({ valor: e.target.value })}
            placeholder={fichas.length ? permitido(fichas[0]) : placeholderReferencia(c.operador)}
          />
        </label>
        <button type="button" className="danger" onClick={onQuitar}>
          Quitar
        </button>
      </div>
      {c.origen === 'PARAMETRO' && <CartelParametro fichas={fichas} />}
    </div>
  )
}
