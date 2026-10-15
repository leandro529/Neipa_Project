package com.neipa.extraction;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.neipa.domain.ElementoDeRed;

/**
 * Extractor de parametros vigentes para elementos de red del fabricante NSN.
 *
 * En el repo original (vendor_utilities/NsnOperations.py) esta clase nunca
 * llego a implementarse: el metodo get_default_parameters() estaba
 * declarado pero sin cuerpo. Migrarla "tal cual" no es posible porque no
 * habia logica real que portar, asi que esta implementacion es una
 * extraccion simulada, pensada para poder probar de punta a punta el flujo
 * de auditoria (CU001) sin depender de una conexion real al OSS de NSN.
 *
 * Para reemplazarla por la extraccion real solo hace falta cambiar el
 * cuerpo de este metodo (por ejemplo, para consultar el OSS via su API o
 * leer un archivo de export), ya que el contrato (VendorExtractor) y el
 * resto del flujo de auditoria no cambian.
 *
 * Ampliacion (ver registro del proyecto): ademas de PotenciaTX_dBm (el
 * unico parametro que devolvia la version original), ahora tambien simula
 * AnguloTilt_grados (que ya estaba cargado en Configuraciones pero ningun
 * extractor auditaba todavia) y dos parametros reales tomados de las
 * auditorias que el usuario ya tenia cargadas fuera de este repo:
 * srvccDelayTimer (AUDIT-05_4G_SRVCC-DelayTimer, operador IGUAL) y
 * tacLimitGbrEmergency (AUDIT-22_4G_AJUSTES-UMBRALES-MINIMOS-LTAC, operador
 * MENOR_QUE). Asi el boton "Auditar" ejercita de punta a punta los
 * operadores nuevos de ValidadorRango, no solo el catalogo estatico de la
 * pantalla de Configuraciones.
 */
@Component
public class NsnVendorExtractor implements VendorExtractor {

    @Override
    public List<ParametroVigente> extraerParametrosVigentes(ElementoDeRed elemento) {
        // Valores simulados, pero deterministicos en funcion del nombre del
        // elemento, para que las pruebas sean reproducibles.
        int hash = Math.abs(elemento.getNombre().hashCode());

        // PotenciaTX_dBm: RANGO 30-46. Base 38, variacion -10..+9 (igual
        // que en la version original de esta migracion).
        double potencia = 38.0 + ((hash % 20) - 10);

        // AnguloTilt_grados: RANGO 0-15. Otro modulo del hash, para no
        // repetir siempre el mismo patron CONFORME/DESVIO que PotenciaTX_dBm.
        double tilt = 8.0 + (((hash / 7) % 20) - 10);
        if (tilt < 0) {
            tilt = 0;
        }

        // srvccDelayTimer: IGUAL a 4 (AUDIT-05). La mitad de los elementos
        // simulan el valor correcto, la otra mitad uno distinto, para ver
        // ambos resultados en la demo.
        int srvccDelayTimer = (hash % 2 == 0) ? 4 : 5;

        // tacLimitGbrEmergency: MENOR_QUE 500000 (AUDIT-22). Algunos
        // elementos simulan justo el limite (DESVIO, 500000 no es menor que
        // 500000) y el resto un valor por debajo (CONFORME).
        int tacLimitGbrEmergency = (hash % 3 == 0) ? 500000 : 480000 + (hash % 15000);

        List<ParametroVigente> parametros = new ArrayList<>();
        parametros.add(new ParametroVigente("PotenciaTX_dBm", String.format("%.0f", potencia)));
        parametros.add(new ParametroVigente("AnguloTilt_grados", String.format("%.0f", tilt)));
        parametros.add(new ParametroVigente("srvccDelayTimer", String.valueOf(srvccDelayTimer)));
        parametros.add(new ParametroVigente("tacLimitGbrEmergency", String.valueOf(tacLimitGbrEmergency)));
        return parametros;
    }
}
