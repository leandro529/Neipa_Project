package com.neipa.cm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** La factory devuelve la subclase de cada motor y cada una se comporta segun su motor. */
class JdbcCmFuenteFactoryTest {

    private FakeCmFuente datos;

    @BeforeEach
    void preparar() {
        datos = new FakeCmFuente();
        datos.tabla("LNCEL", "distName", "n310Qci1");
        datos.fila("LNCEL", "PLMN-PLMN/MRBTS-1/LNBTS-1/LNCEL-1", 8);
    }

    private BaseCmFicticia base(String esquema) {
        return new BaseCmFicticia(datos, x -> x, esquema);
    }

    @Test
    void creaLaSubclaseQueCorrespondeACadaMotor() {
        assertTrue(JdbcCmFuenteFactory.crear(base(null).conexion(), MotorCm.SQLITE, null) instanceof SqliteCmFuente);
        assertTrue(JdbcCmFuenteFactory.crear(base("cm").conexion(), MotorCm.MYSQL, null) instanceof MySqlCmFuente);
        assertTrue(JdbcCmFuenteFactory.crear(base("cm").conexion(), MotorCm.POSTGRESQL, null) instanceof PostgresCmFuente);
        assertTrue(JdbcCmFuenteFactory.crear(base("CM").conexion(), MotorCm.ORACLE, null) instanceof OracleCmFuente);
        assertTrue(JdbcCmFuenteFactory.crear(base("dbo").conexion(), MotorCm.SQLSERVER, null) instanceof SqlServerCmFuente);
        assertThrows(IllegalArgumentException.class, () -> JdbcCmFuenteFactory.crear(base(null).conexion(), null, null));
    }

    @Test
    void cadaSubclaseAplicaLoPropioDeSuMotor() {
        BaseCmFicticia pg = base("cm");
        try (CmFuente f = JdbcCmFuenteFactory.crear(pg.conexion(), MotorCm.POSTGRESQL, null)) {
            f.recorrer("LNCEL", Arrays.asList("distName"), fila -> { });
        }
        assertFalse(pg.autoCommit, "PostgreSQL apaga el autocommit para leer por cursor");
        assertEquals(Integer.valueOf(5000), pg.fetchSizes.get(0));

        BaseCmFicticia mysql = base("cm_demo");
        try (CmFuente f = JdbcCmFuenteFactory.crear(mysql.conexion(), MotorCm.MYSQL, null)) {
            f.recorrer("LNCEL", Arrays.asList("distName"), fila -> { });
        }
        assertTrue(mysql.autoCommit);
        assertEquals(Integer.valueOf(Integer.MIN_VALUE), mysql.fetchSizes.get(0));
        assertEquals("SELECT `distName` FROM `cm_demo`.`LNCEL`", mysql.consultas.get(0));

        BaseCmFicticia dump = base(null);
        dump.metadataFalla = true;
        try (CmFuente f = JdbcCmFuenteFactory.crear(dump.conexion(), MotorCm.SQLITE, "ignorado")) {
            f.recorrer("LNCEL", Arrays.asList("distName"), fila -> { });
        }
        assertTrue(dump.consultas.contains("SELECT \"distName\" FROM \"LNCEL\""), dump.consultas.toString());
    }
}
