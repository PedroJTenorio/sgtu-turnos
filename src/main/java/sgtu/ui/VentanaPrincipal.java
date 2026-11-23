package sgtu.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import sgtu.aplicacion.ResultadoOperacion;
import sgtu.aplicacion.ServicioTurnos;
import sgtu.dominio.EstadoTurno;
import sgtu.dominio.EstadoVentanilla;
import sgtu.dominio.OperacionInvalidaException;
import sgtu.dominio.Turno;
import sgtu.dominio.Ventanillas;
import sgtu.persistencia.PersistenciaException;

// Interfaz de escritorio (Swing). Solo presenta datos y delega toda regla de negocio (RNF-M01). Cubre RI-01, RI-02 y RI-03.
public class VentanaPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final String[] COLUMNAS_FILA = {"ID", "Registro"};
    private static final String[] COLUMNAS_HISTORIAL = {"ID", "Registro", "Estado", "Ventanilla"};
    private static final int COLUMNA_ESTADO_HISTORIAL = 2;

    // Acción de la interfaz que puede ser rechazada por el dominio o fallar al guardar.
    @FunctionalInterface
    private interface Accion {
        ResultadoOperacion ejecutar() throws OperacionInvalidaException, PersistenciaException;
    }

    private final transient ServicioTurnos servicio;
    private final JPanel panelVentanillas = new JPanel(new GridLayout(1, Ventanillas.IDS.size(), 8, 8));
    private final DefaultTableModel modeloFila = crearModelo(COLUMNAS_FILA);
    private final DefaultTableModel modeloHistorial = crearModelo(COLUMNAS_HISTORIAL);
    private final JTextField campoIdCancelar = new JTextField(8);
    private final JLabel etiquetaMensaje = new JLabel("Listo.");

    public VentanaPrincipal(ServicioTurnos servicio) {
        super("Sistema de Gestión de Turnos Universitarios");
        this.servicio = servicio;
        construirInterfaz();
        actualizarVistas();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 620);
        setLocationRelativeTo(null);
    }

    // Construcción de la interfaz
    private void construirInterfaz() {
        setLayout(new BorderLayout(8, 8));
        panelVentanillas.setBorder(BorderFactory.createTitledBorder("Ventanillas"));
        add(panelVentanillas, BorderLayout.NORTH);

        JPanel centro = new JPanel(new GridLayout(1, 2, 8, 8));
        centro.add(crearPanelTabla("Fila de espera (FIFO)", modeloFila, -1));
        centro.add(crearPanelTabla("Historial", modeloHistorial, COLUMNA_ESTADO_HISTORIAL));
        add(centro, BorderLayout.CENTER);

        JPanel sur = new JPanel(new BorderLayout());
        sur.add(crearPanelControles(), BorderLayout.NORTH);
        etiquetaMensaje.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        sur.add(etiquetaMensaje, BorderLayout.SOUTH);
        add(sur, BorderLayout.SOUTH);
    }

    private JPanel crearPanelControles() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton botonRegistrar = new JButton("Registrar turno");
        botonRegistrar.addActionListener(evento -> ejecutar(() -> servicio.registrarTurno()));
        JButton botonCancelar = new JButton("Cancelar turno");
        botonCancelar.addActionListener(evento -> cancelarTurno());
        JButton botonActualizar = new JButton("Actualizar vistas");
        botonActualizar.addActionListener(evento -> actualizarVistas());

        panel.add(botonRegistrar);
        panel.add(new JLabel("ID del turno a cancelar:"));
        panel.add(campoIdCancelar);
        panel.add(botonCancelar);
        panel.add(botonActualizar);
        return panel;
    }

    private JScrollPane crearPanelTabla(String titulo, DefaultTableModel modelo, int columnaEstado) {
        JTable tabla = new JTable(modelo);
        tabla.setEnabled(false);
        if (columnaEstado >= 0) {
            tabla.getColumnModel().getColumn(columnaEstado).setCellRenderer(new RenderizadorEstado());
            tabla.getColumnModel().getColumn(0).setPreferredWidth(50);
            tabla.getColumnModel().getColumn(1).setPreferredWidth(170);
            tabla.getColumnModel().getColumn(columnaEstado).setPreferredWidth(95);
            tabla.getColumnModel().getColumn(3).setPreferredWidth(75);
        }
        JScrollPane desplazamiento = new JScrollPane(tabla);
        desplazamiento.setBorder(BorderFactory.createTitledBorder(titulo));
        return desplazamiento;
    }

    private DefaultTableModel crearModelo(String[] columnas) {
        return new DefaultTableModel(columnas, 0);
    }

    private JPanel crearTarjetaVentanilla(EstadoVentanilla ventanilla) {
        JPanel tarjeta = new JPanel(new BorderLayout(4, 4));
        tarjeta.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JLabel titulo = new JLabel(ventanilla.id(), JLabel.CENTER);
        String textoEstado = "LIBRE";
        if (!ventanilla.estaLibre()) {
            textoEstado = "OCUPADA: " + ventanilla.turnoActual().getId() + " (En atención)";
        }
        JLabel estado = new JLabel(textoEstado, JLabel.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        JButton asignar = new JButton("Asignar siguiente");
        asignar.addActionListener(evento -> ejecutar(() -> servicio.asignarSiguiente(ventanilla.id())));
        JButton finalizar = new JButton("Finalizar");
        finalizar.addActionListener(evento -> ejecutar(() -> servicio.finalizarAtencion(ventanilla.id())));
        botones.add(asignar);
        botones.add(finalizar);

        tarjeta.add(titulo, BorderLayout.NORTH);
        tarjeta.add(estado, BorderLayout.CENTER);
        tarjeta.add(botones, BorderLayout.SOUTH);
        return tarjeta;
    }

    // Acciones

    // RNF-U04: pide confirmación explícita y, si se rechaza, no cambia nada.
    private void cancelarTurno() {
        String id = campoIdCancelar.getText();
        int respuesta = JOptionPane.showConfirmDialog(this,
                "¿Desea cancelar el turno '" + id.trim() + "'? Esta acción no se puede deshacer.",
                "Confirmar cancelación", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (respuesta != JOptionPane.YES_OPTION) {
            etiquetaMensaje.setText("Cancelación no confirmada: no se hicieron cambios.");
            return;
        }
        ejecutar(() -> servicio.cancelarTurno(id));
    }

    // Ejecuta una acción y muestra su resultado; así el manejo de errores no se repite.
    private void ejecutar(Accion accion) {
        try {
            ResultadoOperacion resultado = accion.ejecutar();
            etiquetaMensaje.setText("Correcto: " + resultado.mensaje());
            campoIdCancelar.setText("");
        } catch (OperacionInvalidaException e) {
            etiquetaMensaje.setText("Rechazado: " + e.getMessage());
            JOptionPane.showMessageDialog(this, e.getMessage(), "Operación rechazada", JOptionPane.WARNING_MESSAGE);
        } catch (PersistenciaException e) {
            etiquetaMensaje.setText("Error al guardar: " + e.getMessage());
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error de almacenamiento", JOptionPane.ERROR_MESSAGE);
        }
        actualizarVistas();
    }

    // Actualización de la presentación (RI-02)

    private void actualizarVistas() {
        panelVentanillas.removeAll();
        for (EstadoVentanilla ventanilla : servicio.consultarVentanillas()) {
            panelVentanillas.add(crearTarjetaVentanilla(ventanilla));
        }
        panelVentanillas.revalidate();
        panelVentanillas.repaint();

        modeloFila.setRowCount(0);
        for (Turno turno : servicio.consultarFila()) {
            modeloFila.addRow(new Object[] {turno.getId(), turno.getFechaHoraRegistro().format(FORMATO_FECHA)});
        }

        modeloHistorial.setRowCount(0);
        for (Turno turno : servicio.consultarHistorial()) {
            modeloHistorial.addRow(crearFilaHistorial(turno));
        }
    }

    private Object[] crearFilaHistorial(Turno turno) {
        String ventanilla = "—";
        if (turno.getVentanillaAsignada() != null) {
            ventanilla = turno.getVentanillaAsignada();
        }
        return new Object[] {turno.getId(), turno.getFechaHoraRegistro().format(FORMATO_FECHA),
                turno.getEstado().getEtiqueta(), ventanilla};
    }

    // RNF-U03: el estado se distingue por texto y además por color (el color nunca es lo único).
    private static class RenderizadorEstado extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                       boolean enfocada, int fila, int columna) {
            Component componente = super.getTableCellRendererComponent(tabla, valor, seleccionada,
                    enfocada, fila, columna);
            componente.setBackground(colorDe(String.valueOf(valor)));
            return componente;
        }

        private Color colorDe(String etiqueta) {
            if (etiqueta.equals(EstadoTurno.EN_ESPERA.getEtiqueta())) {
                return new Color(255, 245, 200);
            }
            if (etiqueta.equals(EstadoTurno.EN_ATENCION.getEtiqueta())) {
                return new Color(200, 225, 255);
            }
            if (etiqueta.equals(EstadoTurno.ATENDIDO.getEtiqueta())) {
                return new Color(210, 240, 210);
            }
            return new Color(235, 210, 210);
        }
    }
}