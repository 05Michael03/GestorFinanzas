package gestorfinanzas;

import java.time.LocalDate;

public class Movimiento {
    private int id;
    private double monto;
    private int tipoId;
    private int categoriaId;
    private String tipoNombre;
    private String categoriaNombre;
    private LocalDate fecha;
    private String descripcion;

    public Movimiento() {
        this.fecha = LocalDate.now();
    }

    // For creation: provide monto, tipoId, categoriaId, descripcion
    public Movimiento(double monto, int tipoId, int categoriaId, String descripcion) {
        this.monto = monto;
        this.tipoId = tipoId;
        this.categoriaId = categoriaId;
        this.descripcion = descripcion;
        this.fecha = LocalDate.now();
    }

    // For display: allow setting names
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public int getTipoId() { return tipoId; }
    public void setTipoId(int tipoId) { this.tipoId = tipoId; }

    public int getCategoriaId() { return categoriaId; }
    public void setCategoriaId(int categoriaId) { this.categoriaId = categoriaId; }

    public String getTipoNombre() { return tipoNombre; }
    public void setTipoNombre(String tipoNombre) { this.tipoNombre = tipoNombre; }

    public String getCategoriaNombre() { return categoriaNombre; }
    public void setCategoriaNombre(String categoriaNombre) { this.categoriaNombre = categoriaNombre; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    @Override
    public String toString() {
        return String.format("Movimiento{id=%d, monto=%.2f, tipoId=%d, categoriaId=%d, fecha=%s}",
                id, monto, tipoId, categoriaId, fecha);
    }
}
