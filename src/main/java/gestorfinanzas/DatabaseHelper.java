package gestorfinanzas;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.io.*;

public class DatabaseHelper {
    private String dbUrl;
    private Connection connection;
    private int usuarioIdActual;

    public DatabaseHelper() {
        String dbPath = System.getProperty("db.path");
        if (dbPath == null || dbPath.isBlank()) dbPath = System.getenv("DB_PATH");
        if (dbPath == null || dbPath.isBlank()) dbPath = "finanzas";

        if (dbPath.startsWith("jdbc:")) {
            this.dbUrl = dbPath;
        } else {
            this.dbUrl = "jdbc:mysql://127.0.0.1:3306/" + dbPath + "?user=root&password=&serverTimezone=UTC";
        }

        connect();

        File sqlFile = new File("finanzas.sql");
        if (sqlFile.exists()) {
            importarSqlSiExiste(null);
        }
    }

    private void connect() {
        try {
            connection = DriverManager.getConnection(dbUrl);
        } catch (SQLException e) {
            System.err.println("Error al conectar: " + e.getMessage());
        }
    }

    private void importarSqlSiExiste(String dbPath) {
        File sqlFile = new File("finanzas.sql");
        if (!sqlFile.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(sqlFile));
             Statement stmt = connection.createStatement()) {

            StringBuilder current = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) continue;

                current.append(line).append(' ');
                if (trimmed.endsWith(";")) {
                    String sql = current.toString();
                    int lastSemi = sql.lastIndexOf(';');
                    if (lastSemi >= 0) sql = sql.substring(0, lastSemi);
                    sql = sql.trim();
                    if (!sql.isEmpty()) {
                        try {
                            stmt.execute(sql);
                        } catch (SQLException ex) {
                            System.err.println("Error ejecutando statement de finanzas.sql: " + ex.getMessage());
                        }
                    }
                    current.setLength(0);
                }
            }

            String leftover = current.toString().trim();
            if (!leftover.isEmpty()) {
                try {
                    stmt.execute(leftover);
                } catch (SQLException ex) {
                    System.err.println("Error ejecutando statement de finanzas.sql: " + ex.getMessage());
                }
            }

        } catch (IOException | SQLException e) {
            System.err.println("Error al importar finanzas.sql: " + e.getMessage());
        }
    }


    // AUTENTICACIÓN 
    public boolean autenticarUsuario(String username, String password) {
        String sql = "SELECT id FROM usuarios WHERE username = ? AND password = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                usuarioIdActual = rs.getInt("id");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error en autenticación: " + e.getMessage());
        }
        return false;
    }

    public boolean registrarUsuario(String username, String password, String nombre, String email) {
        String sql = "INSERT INTO usuarios (username, password, nombre, email) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, nombre);
            pstmt.setString(4, email);
            int affected = pstmt.executeUpdate();
            if (affected == 0) return false;

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys != null && keys.next()) {
                    int nuevoId = keys.getInt(1);
                    crearSaldoParaUsuario(nuevoId);
                } else {
                    System.err.println("No se obtuvo el id generado para el nuevo usuario.");
                }
            }
            return true;
        } catch (SQLException e) {
            System.err.println("Error al registrar: " + e.getMessage());
            return false;
        }
    }

    

    private void crearSaldoParaUsuario(int usuarioId) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO saldo (usuario_id, saldo_total) VALUES (?, 0)")) {
            pstmt.setInt(1, usuarioId);
            pstmt.executeUpdate();
        }
    }

    public boolean existeUsuario(String username) {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE username = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("Error al verificar usuario: " + e.getMessage());
            return false;
        }
    }

    public Usuario obtenerUsuarioActual() {
        String sql = "SELECT * FROM usuarios WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, usuarioIdActual);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Usuario usuario = new Usuario();
                usuario.setId(rs.getInt("id"));
                usuario.setUsername(rs.getString("username"));
                usuario.setNombre(rs.getString("nombre"));
                usuario.setEmail(rs.getString("email"));
                usuario.setFechaRegistro(rs.getString("fecha_registro"));
                return usuario;
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener usuario: " + e.getMessage());
        }
        return null;
    }

    public void setUsuarioIdActual(int id) {
        this.usuarioIdActual = id;
    }

    public int getUsuarioIdActual() {
        return usuarioIdActual;
    }

    // MOVIMIENTOS
    public boolean agregarMovimiento(Movimiento movimiento) {
        String sql = "INSERT INTO movimientos(usuario_id, monto, tipo_id, categoria_id, fecha, descripcion) VALUES(?,?,?,?,?,?)";
        boolean previousAutoCommit = true;
        try {
            previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, usuarioIdActual);
                pstmt.setDouble(2, movimiento.getMonto());
                pstmt.setInt(3, movimiento.getTipoId());
                pstmt.setInt(4, movimiento.getCategoriaId());
                pstmt.setString(5, movimiento.getFecha().toString());
                pstmt.setString(6, movimiento.getDescripcion());
                pstmt.executeUpdate();
            }

            // actualizarSaldo by tipo id
            String tipoNombre = obtenerNombreTipoPorId(movimiento.getTipoId());
            actualizarSaldoPorNombre(tipoNombre, movimiento.getMonto());

            connection.commit();
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                System.err.println("Error al hacer rollback: " + ex.getMessage());
            }
            System.err.println("Error al agregar movimiento: " + e.getMessage());
            return false;
        } finally {
            try {
                connection.setAutoCommit(previousAutoCommit);
            } catch (SQLException ex) {
                System.err.println("Error restaurando autoCommit: " + ex.getMessage());
            }
        }
    }

    public List<Movimiento> obtenerMovimientos() {
        List<Movimiento> movimientos = new ArrayList<>();
        String sql = "SELECT m.*, t.tipo AS tipo_nombre, c.categoria AS categoria_nombre FROM movimientos m " +
                     "LEFT JOIN tipo t ON m.tipo_id = t.id LEFT JOIN categoria c ON m.categoria_id = c.id " +
                     "WHERE m.usuario_id = ? ORDER BY fecha DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, usuarioIdActual);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Movimiento movimiento = new Movimiento();
                movimiento.setId(rs.getInt("id"));
                movimiento.setMonto(rs.getDouble("monto"));
                movimiento.setTipoId(rs.getInt("tipo_id"));
                movimiento.setCategoriaId(rs.getInt("categoria_id"));
                movimiento.setTipoNombre(rs.getString("tipo_nombre"));
                movimiento.setCategoriaNombre(rs.getString("categoria_nombre"));
                movimiento.setFecha(LocalDate.parse(rs.getString("fecha")));
                movimiento.setDescripcion(rs.getString("descripcion"));
                movimientos.add(movimiento);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener movimientos: " + e.getMessage());
        }
        return movimientos;
    }

    public boolean eliminarMovimiento(int id) {
        Movimiento movimiento = obtenerMovimientoPorId(id);
        if (movimiento == null) return false;
        
        String sql = "DELETE FROM movimientos WHERE id = ? AND usuario_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setInt(2, usuarioIdActual);
            int rows = pstmt.executeUpdate();
            
            if (rows > 0) {
                // Revert saldo based on tipoNombre
                String tipoNombre = movimiento.getTipoNombre();
                if (tipoNombre != null && tipoNombre.equalsIgnoreCase("Ingreso")) {
                    actualizarSaldoPorNombre("gasto", movimiento.getMonto());
                } else {
                    actualizarSaldoPorNombre("ingreso", movimiento.getMonto());
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error al eliminar: " + e.getMessage());
        }
        return false;
    }

    private Movimiento obtenerMovimientoPorId(int id) {
        String sql = "SELECT m.*, t.tipo AS tipo_nombre, c.categoria AS categoria_nombre FROM movimientos m " +
                     "LEFT JOIN tipo t ON m.tipo_id = t.id LEFT JOIN categoria c ON m.categoria_id = c.id " +
                     "WHERE m.id = ? AND m.usuario_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setInt(2, usuarioIdActual);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Movimiento movimiento = new Movimiento();
                movimiento.setId(rs.getInt("id"));
                movimiento.setMonto(rs.getDouble("monto"));
                movimiento.setTipoId(rs.getInt("tipo_id"));
                movimiento.setCategoriaId(rs.getInt("categoria_id"));
                movimiento.setTipoNombre(rs.getString("tipo_nombre"));
                movimiento.setCategoriaNombre(rs.getString("categoria_nombre"));
                movimiento.setFecha(LocalDate.parse(rs.getString("fecha")));
                movimiento.setDescripcion(rs.getString("descripcion"));
                return movimiento;
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener movimiento: " + e.getMessage());
        }
        return null;
    }

    private void actualizarSaldoPorNombre(String tipoNombre, double monto) {
        String tipoLower = tipoNombre == null ? "gasto" : tipoNombre.toLowerCase();
        String signo = tipoLower.contains("ingreso") ? "+" : "-";
        String sql = "UPDATE saldo SET saldo_total = saldo_total " + signo + " ?, fecha_actualizacion = CURRENT_TIMESTAMP WHERE usuario_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setDouble(1, monto);
            pstmt.setInt(2, usuarioIdActual);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al actualizar saldo: " + e.getMessage());
        }
    }

    public double obtenerSaldoTotal() {
        String sql = "SELECT saldo_total FROM saldo WHERE usuario_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, usuarioIdActual);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getDouble("saldo_total") : 0.0;
        } catch (SQLException e) {
            System.err.println("Error al obtener saldo: " + e.getMessage());
            return 0.0;
        }
    }

    public List<String> obtenerCategorias() {
        List<String> categorias = new ArrayList<>();
        String sql = "SELECT DISTINCT categoria FROM movimientos WHERE usuario_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, usuarioIdActual);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                categorias.add(rs.getString("categoria"));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener categorías: " + e.getMessage());
        }
        
        if (categorias.isEmpty()) {
            categorias.add("Comida");
            categorias.add("Transporte");
            categorias.add("Ocio");
            categorias.add("Salario");
            categorias.add("Otros");
        }
        return categorias;
    }

    public List<String> obtenerCategoriasPorTipo(String tipo) {
        java.util.LinkedHashSet<String> categoriasSet = new java.util.LinkedHashSet<>();

        // Categorias
        if ("ingreso".equalsIgnoreCase(tipo)) {
            categoriasSet.add("Salario");
            categoriasSet.add("Inversión");
            categoriasSet.add("Regalo");
            categoriasSet.add("Otros");
        } else {
            categoriasSet.add("Comida");
            categoriasSet.add("Transporte");
            categoriasSet.add("Ocio");
            categoriasSet.add("Salud");
            categoriasSet.add("Vivienda");
            categoriasSet.add("Otros");
        }

        String sql = "SELECT DISTINCT categoria FROM movimientos WHERE tipo = ? AND usuario_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, tipo);
            pstmt.setInt(2, usuarioIdActual);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String c = rs.getString("categoria");
                if (c != null) {
                    c = c.trim();
                    if (!c.isEmpty()) {
                        boolean exists = false;
                        for (String existing : categoriasSet) {
                            if (existing.equalsIgnoreCase(c)) { exists = true; break; }
                        }
                        if (!exists) categoriasSet.add(c);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener categorías por tipo: " + e.getMessage());
        }

        return new ArrayList<>(categoriasSet);
    }

    public double obtenerTotalPorTipo(String tipo) {
        // Deprecated: use obtenerTotalPorTipoId
        return 0.0;
    }

    public double obtenerTotalPorTipoId(int tipoId) {
        String sql = "SELECT SUM(monto) as total FROM movimientos WHERE tipo_id = ? AND usuario_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, tipoId);
            pstmt.setInt(2, usuarioIdActual);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getDouble("total") : 0.0;
        } catch (SQLException e) {
            System.err.println("Error al obtener total: " + e.getMessage());
            return 0.0;
        }
    }

    public String obtenerNombreTipoPorId(int tipoId) {
        String sql = "SELECT tipo FROM tipo WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, tipoId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("tipo");
        } catch (SQLException e) {
            System.err.println("Error al obtener nombre de tipo: " + e.getMessage());
        }
        return null;
    }

    public List<Tipo> obtenerTipos() {
        List<Tipo> tipos = new ArrayList<>();
        String sql = "SELECT id, tipo FROM tipo ORDER BY id ASC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                tipos.add(new Tipo(rs.getInt("id"), rs.getString("tipo")));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener tipos: " + e.getMessage());
        }
        return tipos;
    }

    public List<Categoria> obtenerCategoriasPorTipoId(int tipoId) {
        List<Categoria> cats = new ArrayList<>();
        String sql = "SELECT id, categoria FROM categoria WHERE tipo_id = ? ORDER BY id ASC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, tipoId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                cats.add(new Categoria(rs.getInt("id"), tipoId, rs.getString("categoria")));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener categorias por tipo: " + e.getMessage());
        }
        return cats;
    }

    public boolean agregarCategoria(int tipoId, String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return false;
        String lower = nombre.trim().toLowerCase();
        String check = "SELECT id FROM categoria WHERE tipo_id = ? AND LOWER(categoria) = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(check)) {
            pstmt.setInt(1, tipoId);
            pstmt.setString(2, lower);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                // ya existe
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar categoría existente: " + e.getMessage());
            return false;
        }

        String sql = "INSERT INTO categoria (tipo_id, categoria) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, tipoId);
            pstmt.setString(2, nombre.trim());
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            System.err.println("Error al agregar categoría: " + e.getMessage());
            return false;
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error al cerrar conexión: " + e.getMessage());
        }
    }
}
