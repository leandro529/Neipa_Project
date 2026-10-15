const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

/**
 * Envoltorio minimo sobre fetch: agrega el host del backend, serializa el
 * body a JSON, y convierte el ErrorResponse que devuelve
 * GlobalExceptionHandler en un Error con el mensaje legible, en vez de
 * dejar que cada pantalla tenga que parsear la respuesta.
 */
async function request(path, options = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options,
  })

  if (!res.ok) {
    let mensaje = `Error ${res.status} al llamar ${path}`
    try {
      const cuerpo = await res.json()
      if (cuerpo && cuerpo.mensaje) {
        mensaje = cuerpo.mensaje
      }
    } catch (_ignored) {
      // El backend no devolvio JSON (por ejemplo, un 500 sin manejar).
    }
    throw new Error(mensaje)
  }

  if (res.status === 204) {
    return null
  }
  // Algunas respuestas son texto (ej. "Auditoria iniciada ..."): no todo es JSON.
  const texto = await res.text()
  try {
    return texto ? JSON.parse(texto) : null
  } catch (_noEsJson) {
    return texto
  }
}

/**
 * Descarga un archivo (XML/ZIP) del backend y lo guarda con el nombre que
 * manda en Content-Disposition.
 */
async function descargar(path, nombrePorDefecto) {
  const res = await fetch(`${API_BASE}${path}`)
  if (!res.ok) {
    let mensaje = `Error ${res.status} al descargar ${path}`
    try {
      const cuerpo = await res.json()
      if (cuerpo && cuerpo.mensaje) mensaje = cuerpo.mensaje
    } catch (_ignored) {
      // sin cuerpo JSON
    }
    throw new Error(mensaje)
  }
  const disposicion = res.headers.get('Content-Disposition') || ''
  const coincidencia = disposicion.match(/filename="?([^"]+)"?/)
  const nombre = coincidencia ? coincidencia[1] : nombrePorDefecto
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = nombre
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

export const api = {
  descargar,
  get: (path) => request(path),
  post: (path, body) => request(path, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) }),
  put: (path, body) => request(path, { method: 'PUT', body: JSON.stringify(body) }),
  del: (path) => request(path, { method: 'DELETE' }),
}
