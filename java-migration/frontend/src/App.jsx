import { NavLink, Route, Routes } from 'react-router-dom'
import Dashboard from './pages/Dashboard.jsx'
import Segmentos from './pages/Segmentos.jsx'
import Elementos from './pages/Elementos.jsx'
import Configuraciones from './pages/Configuraciones.jsx'
import Excepciones from './pages/Excepciones.jsx'
import Reconfiguracion from './pages/Reconfiguracion.jsx'
import DatosCm from './pages/DatosCm.jsx'
import Catalogo from './pages/Catalogo.jsx'

const ENLACES = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/segmentos', label: 'Segmentos' },
  { to: '/elementos', label: 'Elementos de red' },
  { to: '/configuraciones', label: 'Configuraciones' },
  { to: '/excepciones', label: 'Excepciones' },
  { to: '/reconfiguracion', label: 'Reconfiguración' },
  { to: '/cm', label: 'Datos CM' },
  { to: '/catalogo', label: 'Catálogo' },
]

export default function App() {
  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">N</span>
          <div>
            <strong>NEIPA</strong>
            <div className="brand-sub">Network Element Integrity &amp; Parameter Auditor</div>
          </div>
        </div>
        <nav>
          {ENLACES.map((enlace) => (
            <NavLink
              key={enlace.to}
              to={enlace.to}
              end={enlace.end}
              className={({ isActive }) => 'nav-link' + (isActive ? ' active' : '')}
            >
              {enlace.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <main className="content">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/segmentos" element={<Segmentos />} />
          <Route path="/elementos" element={<Elementos />} />
          <Route path="/configuraciones" element={<Configuraciones />} />
          <Route path="/excepciones" element={<Excepciones />} />
          <Route path="/reconfiguracion" element={<Reconfiguracion />} />
          <Route path="/cm" element={<DatosCm />} />
          <Route path="/catalogo" element={<Catalogo />} />
        </Routes>
      </main>
    </div>
  )
}
