package sgtu.ui;

import java.awt.Color;
import java.awt.Component;
import java.util.Map;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import sgtu.dominio.EstadoTurno;

/**
 * RNF-U03: el estado se distingue por texto y además por color (el color nunca es lo único).
 * Un texto que no corresponda a ningún estado conserva el color normal de la tabla.
 */
final class RenderizadorEstadoTurno extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    private static final Map<String, Color> COLORES = Map.of(
            EstadoTurno.EN_ESPERA.getEtiqueta(), new Color(255, 245, 200),
            EstadoTurno.EN_ATENCION.getEtiqueta(), new Color(200, 225, 255),
            EstadoTurno.ATENDIDO.getEtiqueta(), new Color(210, 240, 210),
            EstadoTurno.CANCELADO.getEtiqueta(), new Color(235, 210, 210));

    @Override
    public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                   boolean enfocada, int fila, int columna) {
        Component componente = super.getTableCellRendererComponent(tabla, valor, seleccionada,
                enfocada, fila, columna);
        Color color = COLORES.get(String.valueOf(valor));
        if (color != null) {
            componente.setBackground(color);
        }
        return componente;
    }
}
