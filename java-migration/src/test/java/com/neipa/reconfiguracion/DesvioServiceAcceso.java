package com.neipa.reconfiguracion;

import com.neipa.domain.Auditoria;

/** Acceso de test al metodo package-private DesvioService.deCm. */
public final class DesvioServiceAcceso {

    private DesvioServiceAcceso() {
    }

    public static Desvio deCm(Auditoria a, Long idSegmento) {
        return DesvioService.deCm(a, idSegmento);
    }
}
