package gestorfinanzas;

public class Categoria {
    private int id;
    private int tipoId;
    private String nombre;

    public Categoria() {}

    public Categoria(int id, int tipoId, String nombre) { this.id = id; this.tipoId = tipoId; this.nombre = nombre; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTipoId() { return tipoId; }
    public void setTipoId(int tipoId) { this.tipoId = tipoId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    @Override
    public String toString() { return nombre; }
}
