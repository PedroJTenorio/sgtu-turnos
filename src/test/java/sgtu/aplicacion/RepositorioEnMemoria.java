package sgtu.aplicacion;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import sgtu.dominio.Turno;
import sgtu.persistencia.PersistenciaException;
import sgtu.persistencia.IRepositorioTurnos;


//Repositorio para pruebas exclusivamente
//Sigue el contrato de la interfaz definida: Guarda en memoria, cuenta los datos guardados
// y se puede configurar para fallar para simular un fallo de escritura

class RepositorioEnMemoria implements IRepositorioTurnos {

    private List<Turno> guardados = new ArrayList<>();
    private boolean fallarAlGuardar;
    private boolean fallarAlCargar;
    private int vecesGuardado;

    void configurarFalloAlGuardar(boolean fallar) {
        this.fallarAlGuardar = fallar;
    }

    void configurarFalloAlCargar(boolean fallar) {
        this.fallarAlCargar = fallar;
    }

    void precargar(List<Turno> turnos) {
        this.guardados = clonarTurnos(turnos);
    }

    int getVecesGuardado() {
        return vecesGuardado;
    }

    List<Turno> getGuardados() {
        return clonarTurnos(guardados);
    }

    @Override
    public List<Turno> cargar() throws PersistenciaException {
        if (fallarAlCargar) {
            throw new PersistenciaException("Lectura imposible (simulada).");
        }
        return clonarTurnos(guardados);
    }

    @Override
    public void guardar(List<Turno> turnos) throws PersistenciaException {
        if (fallarAlGuardar) {
            throw new PersistenciaException("Escritura imposible (simulada).");
        }
        guardados = clonarTurnos(turnos);
        //vecesGuardado ahora ya se incrementa luego de la verificacion
        vecesGuardado++;
    }
    //metodo auxiliar para resolver el problema de las copias superficiales
    private List<Turno> clonarTurnos(List<Turno> origen) {
        if (origen == null) {
            return new ArrayList<>();
        }
        return origen.stream()
                //usando la sugerencia de la revision
                .map(Turno::copiar)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}