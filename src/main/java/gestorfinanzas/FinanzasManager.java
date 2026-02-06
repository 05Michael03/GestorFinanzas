package gestorfinanzas;

import java.util.List;

public class FinanzasManager {
    private DatabaseHelper dbHelper;
    private Usuario usuarioActual;

    public FinanzasManager() {
        dbHelper = new DatabaseHelper();
    }

    // ============ AUTENTICACIÓN ============
    public boolean login(String username, String password) {
        boolean autenticado = dbHelper.autenticarUsuario(username, password);
        if (autenticado) {
            usuarioActual = dbHelper.obtenerUsuarioActual();
        }
        return autenticado;
    }

    public boolean registrarUsuario(String username, String password, String nombre, String email) {
        boolean registrado = dbHelper.registrarUsuario(username, password, nombre, email);
        if (registrado) {
            return login(username, password);
        }
        return false;
    }

    public boolean existeUsuario(String username) {
        return dbHelper.existeUsuario(username);
    }

    public Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public void logout() {
        usuarioActual = null;
        dbHelper.setUsuarioIdActual(0);
    }

    public boolean isLoggedIn() {
        return usuarioActual != null;
    }

    // ============ FINANZAS ============
    public boolean agregarIngreso(double monto, String categoria, String descripcion) {
        throw new UnsupportedOperationException("Use agregarMovimiento con ids");
    }

    public boolean agregarGasto(double monto, String categoria, String descripcion) {
        throw new UnsupportedOperationException("Use agregarMovimiento con ids");
    }

    public boolean agregarMovimiento(double monto, int tipoId, int categoriaId, String descripcion) {
        validarSesion();
        Movimiento movimiento = new Movimiento(monto, tipoId, categoriaId, descripcion);
        return dbHelper.agregarMovimiento(movimiento);
    }

    public List<Movimiento> obtenerTodosMovimientos() {
        validarSesion();
        return dbHelper.obtenerMovimientos();
    }

    public double obtenerSaldoActual() {
        return usuarioActual != null ? dbHelper.obtenerSaldoTotal() : 0.0;
    }

    public double obtenerTotalIngresos() {
        return usuarioActual != null ? dbHelper.obtenerTotalPorTipo("ingreso") : 0.0;
    }

    public double obtenerTotalGastos() {
        return usuarioActual != null ? dbHelper.obtenerTotalPorTipo("gasto") : 0.0;
    }

    public boolean eliminarMovimiento(int id) {
        validarSesion();
        return dbHelper.eliminarMovimiento(id);
    }

    public List<String> obtenerCategoriasDisponibles() {
        return usuarioActual != null ? dbHelper.obtenerCategorias() : List.of();
    }

    public java.util.List<Tipo> obtenerTipos() {
        return usuarioActual != null ? dbHelper.obtenerTipos() : java.util.List.of();
    }

    public java.util.List<Categoria> obtenerCategoriasPorTipoId(int tipoId) {
        return usuarioActual != null ? dbHelper.obtenerCategoriasPorTipoId(tipoId) : java.util.List.of();
    }

    public boolean agregarCategoria(int tipoId, String nombre) {
        validarSesion();
        return dbHelper.agregarCategoria(tipoId, nombre);
    }

    private void validarSesion() {
        if (usuarioActual == null) {
            throw new IllegalStateException("No hay usuario logueado. Inicie sesión primero.");
        }
    }

    public void cerrarConexion() {
        dbHelper.close();
    }
}
