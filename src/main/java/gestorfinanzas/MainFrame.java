package gestorfinanzas;

import javax.swing.*;
import javax.swing.table.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import java.awt.*;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainFrame extends JFrame {
    private FinanzasManager manager;
    private JLabel saldoLabel, usuarioLabel;
    private JPanel mainPanel;
    private DefaultTableModel tableModel;
    private JTable movimientosTable;
    private JTextField montoField;
    private JTextField fechaField;
    private JComboBox<Object> categoriaCombo;
    private JComboBox<Object> tipoCombo;
    private JTextArea descripcionArea;
    private JLabel totalIngresosLabel, totalGastosLabel;
    // filtros
    private JComboBox<Object> tipoFilterCombo;
    private JComboBox<Object> categoriaFilterCombo;
    private JTextField fromDateField, toDateField;
    private JButton aplicarFiltroBtn, limpiarFiltroBtn, generarGraficoBtn;
    private java.util.List<Movimiento> movimientosCache = java.util.List.of();
    private boolean filtrosInicializados = false;

    public MainFrame(FinanzasManager manager) {
        // Intentar aplicar Nimbus antes de inicializar componentes, con fallback al LAF del sistema
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        this.manager = manager;
        initComponents();
        // Ensure DB connection is closed when window is closed
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    manager.cerrarConexion();
                } catch (Exception ex) {
                    // ignore
                }
            }
        });
        cargarDatos();
        setVisible(true);
    }

    private void initComponents() {
        setTitle("Gestor de Finanzas Personales");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);

        this.mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top
        JPanel topPanel = new JPanel(new BorderLayout());
        Usuario usuario = manager.getUsuarioActual();
        usuarioLabel = new JLabel("Usuario: " + (usuario != null ? usuario.getNombre() + " (" + usuario.getUsername() + ")" : ""));
        usuarioLabel.setFont(new Font("Arial", Font.BOLD, 14));

        JButton logoutBtn = new JButton("Cerrar Sesión");
        logoutBtn.addActionListener(e -> cerrarSesion());
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        userPanel.add(usuarioLabel);
        userPanel.add(logoutBtn);

        JPanel saldoPanel = new JPanel();
        saldoPanel.setBorder(BorderFactory.createTitledBorder("Saldo Actual"));
        saldoLabel = new JLabel("$0.00", SwingConstants.CENTER);
        saldoLabel.setFont(new Font("Arial", Font.BOLD, 36));
        saldoPanel.add(saldoLabel);

        topPanel.add(userPanel, BorderLayout.WEST);
        topPanel.add(saldoPanel, BorderLayout.EAST);

        // --- Panel de filtros (aparece bajo el topPanel)
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        tipoFilterCombo = new JComboBox<>();
        categoriaFilterCombo = new JComboBox<>();
        fromDateField = new JTextField(10);
        fromDateField.setToolTipText("YYYY-MM-DD");
        toDateField = new JTextField(10);
        toDateField.setToolTipText("YYYY-MM-DD");
        aplicarFiltroBtn = new JButton("Aplicar filtro");
        limpiarFiltroBtn = new JButton("Limpiar filtro");
        generarGraficoBtn = new JButton("Generar gráfica");

        filterPanel.add(new JLabel("Tipo:"));
        filterPanel.add(tipoFilterCombo);
        filterPanel.add(new JLabel("Categoría:"));
        filterPanel.add(categoriaFilterCombo);
        filterPanel.add(new JLabel("Fecha desde:"));
        filterPanel.add(fromDateField);
        filterPanel.add(new JLabel("Fecha hasta:"));
        filterPanel.add(toDateField);
        filterPanel.add(aplicarFiltroBtn);
        filterPanel.add(limpiarFiltroBtn);
        filterPanel.add(generarGraficoBtn);

        // Container to stack topPanel + filters
        JPanel northContainer = new JPanel(new BorderLayout());
        northContainer.add(topPanel, BorderLayout.NORTH);
        northContainer.add(filterPanel, BorderLayout.SOUTH);

        // Center - tabla
        String[] columnas = {"ID", "Fecha", "Tipo", "Categoría", "Monto", "Descripción"};
        tableModel = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        movimientosTable = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(movimientosTable);
        try {
            if (movimientosTable.getColumnModel().getColumnCount() > 0) {
                movimientosTable.removeColumn(movimientosTable.getColumnModel().getColumn(0));
            }
        } catch (Exception ignore) {
        }

        // Renderer para colorear montos: ingresos en verde, gastos en rojo
        try {
            TableColumn montoCol = null;
            for (int i = 0; i < movimientosTable.getColumnModel().getColumnCount(); i++) {
                TableColumn c = movimientosTable.getColumnModel().getColumn(i);
                if ("Monto".equals(c.getHeaderValue())) { montoCol = c; break; }
            }
            if (montoCol != null) {
                montoCol.setCellRenderer(new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        int modelRow = table.convertRowIndexToModel(row);
                        Object tipoVal = table.getModel().getValueAt(modelRow, 2); // columna 'Tipo' en el modelo
                        String tipo = tipoVal != null ? tipoVal.toString() : "";
                        if (tipo.equalsIgnoreCase("ingreso")) {
                            comp.setForeground(new Color(0, 128, 0));
                        } else {
                            comp.setForeground(Color.RED);
                        }
                        if (value instanceof Number) {
                            setHorizontalAlignment(SwingConstants.RIGHT);
                            setText(String.format("$%.2f", ((Number) value).doubleValue()));
                        } else if (value != null) {
                            setHorizontalAlignment(SwingConstants.RIGHT);
                            setText(value.toString());
                        }
                        return comp;
                    }
                });
            }
        } catch (Exception ignore) {}

        // Right - formulario
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setPreferredSize(new Dimension(340, 0));
        rightPanel.setBorder(BorderFactory.createTitledBorder("Nuevo Movimiento"));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Monto:"), gbc);
        gbc.gridx = 1;
        montoField = new JTextField();
        form.add(montoField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Fecha (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        fechaField = new JTextField();
        fechaField.setToolTipText("Formato: YYYY-MM-DD. Dejar vacío = hoy");
        form.add(fechaField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        form.add(new JLabel("Tipo:"), gbc);
        gbc.gridx = 1;
        tipoCombo = new JComboBox<>();
        form.add(tipoCombo, gbc);
        tipoCombo.addActionListener(e -> actualizarCategoriasSegunTipo());

        gbc.gridx = 0; gbc.gridy = 3;
        form.add(new JLabel("Categoría:"), gbc);
        gbc.gridx = 1;
        JPanel categoriaPanel = new JPanel(new BorderLayout(4, 0));
        categoriaCombo = new JComboBox<>();
        categoriaPanel.add(categoriaCombo, BorderLayout.CENTER);
        JButton addCategoriaBtn = new JButton("+");
        addCategoriaBtn.setMargin(new Insets(2, 6, 2, 6));
        addCategoriaBtn.addActionListener(e -> mostrarDialogoNuevaCategoria());
        categoriaPanel.add(addCategoriaBtn, BorderLayout.EAST);
        form.add(categoriaPanel, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        form.add(new JLabel("Descripción:"), gbc);
        gbc.gridx = 1;
        descripcionArea = new JTextArea(8, 25);
        descripcionArea.setLineWrap(true);
        descripcionArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descripcionArea);
        descScroll.setPreferredSize(new Dimension(260, 140));
        form.add(descScroll, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        JPanel btns = new JPanel(new FlowLayout());
        JButton addBtn = new JButton("Agregar");
        addBtn.addActionListener(e -> agregarMovimiento());
        JButton delBtn = new JButton("Eliminar seleccionado");
        delBtn.addActionListener(e -> eliminarSeleccionado());
        btns.add(addBtn);
        btns.add(delBtn);
        form.add(btns, gbc);

        rightPanel.add(form, BorderLayout.NORTH);

        // Panel de estadísticas
        JPanel statsPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        totalIngresosLabel = new JLabel("Total Ingresos: $0.00");
        totalGastosLabel = new JLabel("Total Gastos: $0.00");
        statsPanel.add(totalIngresosLabel);
        statsPanel.add(totalGastosLabel);
        rightPanel.add(statsPanel, BorderLayout.CENTER);

        // Botón adicional de gráfica en panel derecho (asegura visibilidad)
        JButton chartBtnRight = new JButton("Generar gráfica");
        chartBtnRight.addActionListener(e -> generarGrafico());
        JPanel chartBtnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        chartBtnPanel.add(chartBtnRight);
        rightPanel.add(chartBtnPanel, BorderLayout.SOUTH);

        mainPanel.add(northContainer, BorderLayout.NORTH);
        mainPanel.add(tableScroll, BorderLayout.CENTER);
        mainPanel.add(rightPanel, BorderLayout.EAST);

        add(mainPanel);
    }

    private void cargarDatos() {
        // Saldo
        saldoLabel.setText(String.format("$%.2f", manager.obtenerSaldoActual()));

        // Tipos y categorías
        tipoCombo.removeAllItems();
        java.util.List<Tipo> tipos = manager.obtenerTipos();
        if (tipos != null) {
            for (Tipo t : tipos) tipoCombo.addItem(t);
        }
        if (tipoCombo.getItemCount() > 0) tipoCombo.setSelectedIndex(0);
        actualizarCategoriasSegunTipo();

        // Movimientos
        tableModel.setRowCount(0);
        List<Movimiento> movimientos = manager.obtenerTodosMovimientos();
        // cache para filtros
        movimientosCache = movimientos != null ? movimientos : java.util.List.of();
        for (Movimiento m : movimientos) {
            String tipoNombre = m.getTipoNombre() != null ? m.getTipoNombre() : "";
            String categoriaNombre = m.getCategoriaNombre() != null ? m.getCategoriaNombre() : "";
            tableModel.addRow(new Object[]{m.getId(), m.getFecha(), tipoNombre, categoriaNombre, m.getMonto(), m.getDescripcion()});
        }
        
        // Calcular estadísticas
        double totalIngresos = 0;
        double totalGastos = 0;
        for (Movimiento m : movimientos) {
            String tipoNombre = m.getTipoNombre();
            if (tipoNombre != null && tipoNombre.equalsIgnoreCase("ingreso")) {
                totalIngresos += m.getMonto();
            } else {
                totalGastos += m.getMonto();
            }
        }
        
        // Actualizar estadísticas en el panel
        if (totalIngresosLabel != null && totalGastosLabel != null) {
            totalIngresosLabel.setText(String.format("Total Ingresos: $%.2f", totalIngresos));
            totalGastosLabel.setText(String.format("Total Gastos: $%.2f", totalGastos));
            totalIngresosLabel.setForeground(new Color(0, 128, 0));
            totalGastosLabel.setForeground(Color.RED);
        }

        // Inicializar/actualizar filtros (no volverá a anexar listeners si ya están)
        inicializarFiltros();
    }

    private void agregarMovimiento() {
        try {
            double monto = Double.parseDouble(montoField.getText().trim());
            // Fecha: si está vacío -> hoy; si no, parsear y validar que no sea futura
            LocalDate fecha;
            String fechaTxt = fechaField.getText().trim();
            if (fechaTxt.isEmpty()) {
                fecha = LocalDate.now();
            } else {
                try {
                    fecha = LocalDate.parse(fechaTxt);
                } catch (DateTimeParseException ex) {
                    JOptionPane.showMessageDialog(this, "Formato de fecha inválido. Use YYYY-MM-DD.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (fecha.isAfter(LocalDate.now())) {
                    JOptionPane.showMessageDialog(this, "La fecha no puede ser posterior a la fecha actual.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            Object tipoObj = tipoCombo.getSelectedItem();
            Object catObj = categoriaCombo.getSelectedItem();
            if (tipoObj == null) throw new IllegalStateException("Seleccione un tipo");
            if (catObj == null) throw new IllegalStateException("Seleccione una categoría");
            int tipoId = (tipoObj instanceof Tipo) ? ((Tipo) tipoObj).getId() : -1;
            int categoriaId = -1;
            // If the category is a Categoria object, use its id; if it's a String, try to resolve to a Categoria in DB
            if (catObj instanceof Categoria) {
                categoriaId = ((Categoria) catObj).getId();
            } else if (catObj instanceof String) {
                // try to resolve by name
                java.util.List<Categoria> posibles = manager.obtenerCategoriasPorTipoId(tipoId);
                String nombre = ((String) catObj).trim();
                for (Categoria c : posibles) {
                    if (c.getNombre().equalsIgnoreCase(nombre)) { categoriaId = c.getId(); break; }
                }
                if (categoriaId == -1) throw new IllegalStateException("La categoría seleccionada no existe en la base de datos. Seleccione una existente.");
            }

            String descripcion = descripcionArea.getText().trim();

            boolean ok = manager.agregarMovimiento(monto, tipoId, categoriaId, descripcion, fecha);

            if (!ok) {
                JOptionPane.showMessageDialog(this, "No se pudo guardar el movimiento en la base de datos. Revisa la consola para detalles.", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                montoField.setText("");
                fechaField.setText("");
                descripcionArea.setText("");
                cargarDatos();
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Monto inválido", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void inicializarFiltros() {
        // siempre refrescar items (por si se añaden tipos/categorías)
        tipoFilterCombo.removeAllItems();
        java.util.List<Tipo> tipos = manager.obtenerTipos();
        tipoFilterCombo.addItem("-- Todos --");
        if (tipos != null) for (Tipo t : tipos) tipoFilterCombo.addItem(t);

        // actualizar categorías según la selección actual
        actualizarCategoriasFiltroSegunTipo();

        // agregar listeners solo la primera vez para evitar duplicados
        if (!filtrosInicializados) {
            tipoFilterCombo.addActionListener(e -> actualizarCategoriasFiltroSegunTipo());
            aplicarFiltroBtn.addActionListener(e -> aplicarFiltro());
            limpiarFiltroBtn.addActionListener(e -> {
                tipoFilterCombo.setSelectedIndex(0);
                categoriaFilterCombo.removeAllItems();
                fromDateField.setText("");
                toDateField.setText("");
                cargarDatos();
            });
            generarGraficoBtn.addActionListener(e -> generarGrafico());
            filtrosInicializados = true;
        }
    }

    private void actualizarCategoriasFiltroSegunTipo() {
        categoriaFilterCombo.removeAllItems();
        Object sel = tipoFilterCombo.getSelectedItem();
        if (sel instanceof Tipo) {
            int tipoId = ((Tipo) sel).getId();
            java.util.List<Categoria> cats = manager.obtenerCategoriasPorTipoId(tipoId);
            if (cats == null || cats.isEmpty()) {
                categoriaFilterCombo.addItem("-- Todas --");
            } else {
                categoriaFilterCombo.addItem("-- Todas --");
                for (Categoria c : cats) categoriaFilterCombo.addItem(c);
            }
        } else {
            categoriaFilterCombo.addItem("-- Todas --");
        }
    }

    private void aplicarFiltro() {
        java.util.List<Movimiento> todos = movimientosCache;
        java.util.List<Movimiento> filtrados = new java.util.ArrayList<>();

        Object tipoSel = tipoFilterCombo.getSelectedItem();
        Integer tipoIdSel = null;
        if (tipoSel instanceof Tipo) tipoIdSel = ((Tipo) tipoSel).getId();

        Object catSel = categoriaFilterCombo.getSelectedItem();
        Integer catIdSel = null;
        if (catSel instanceof Categoria) catIdSel = ((Categoria) catSel).getId();

        // parsear fechas de filtro (si las hay)
        LocalDate desde = null, hasta = null;
        try {
            String txtDesde = fromDateField.getText().trim();
            String txtHasta = toDateField.getText().trim();
            if (!txtDesde.isEmpty()) desde = LocalDate.parse(txtDesde);
            if (!txtHasta.isEmpty()) hasta = LocalDate.parse(txtHasta);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Formato de fecha inválido en filtros. Use YYYY-MM-DD.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            JOptionPane.showMessageDialog(this, "La fecha 'desde' no puede ser posterior a la fecha 'hasta'.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        for (Movimiento m : todos) {
            if (tipoIdSel != null && tipoIdSel != m.getTipoId()) continue;
            if (catIdSel != null && catIdSel != m.getCategoriaId()) continue;
            LocalDate f = m.getFecha();
            if (desde != null && (f == null || f.isBefore(desde))) continue;
            if (hasta != null && (f == null || f.isAfter(hasta))) continue;
            filtrados.add(m);
        }

        // actualizar tabla con filtrados
        tableModel.setRowCount(0);
        for (Movimiento m : filtrados) {
            String tipoNombre = m.getTipoNombre() != null ? m.getTipoNombre() : "";
            String categoriaNombre = m.getCategoriaNombre() != null ? m.getCategoriaNombre() : "";
            tableModel.addRow(new Object[]{m.getId(), m.getFecha(), tipoNombre, categoriaNombre, m.getMonto(), m.getDescripcion()});
        }

        // recalcular estadísticas filtradas
        double totalIngresos = 0, totalGastos = 0;
        for (Movimiento m : filtrados) {
            String tipoNombre = m.getTipoNombre();
            if (tipoNombre != null && tipoNombre.equalsIgnoreCase("ingreso")) totalIngresos += m.getMonto(); else totalGastos += m.getMonto();
        }
        totalIngresosLabel.setText(String.format("Total Ingresos: $%.2f", totalIngresos));
        totalGastosLabel.setText(String.format("Total Gastos: $%.2f", totalGastos));
        totalIngresosLabel.setForeground(new Color(0, 128, 0));
        totalGastosLabel.setForeground(Color.RED);
    }

    private void generarGrafico() {
        // Use currently displayed rows as filtered dataset and create two separate charts
        java.util.Map<String, Double> ingresosPorCategoria = new java.util.HashMap<>();
        java.util.Map<String, Double> gastosPorCategoria = new java.util.HashMap<>();

        for (int r = 0; r < tableModel.getRowCount(); r++) {
            String tipo = (tableModel.getValueAt(r, 2) != null) ? tableModel.getValueAt(r, 2).toString() : "";
            String categoria = (tableModel.getValueAt(r, 3) != null) ? tableModel.getValueAt(r, 3).toString() : "Sin categoría";
            double monto = 0.0;
            Object montoObj = tableModel.getValueAt(r, 4);
            if (montoObj instanceof Number) monto = ((Number) montoObj).doubleValue(); else { try { monto = Double.parseDouble(montoObj.toString()); } catch (Exception ignored) {} }

            if (tipo.equalsIgnoreCase("ingreso")) {
                ingresosPorCategoria.put(categoria, ingresosPorCategoria.getOrDefault(categoria, 0.0) + monto);
            } else {
                gastosPorCategoria.put(categoria, gastosPorCategoria.getOrDefault(categoria, 0.0) + monto);
            }
        }

        if (ingresosPorCategoria.isEmpty() && gastosPorCategoria.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay datos para graficar.", "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        DefaultCategoryDataset ingresosDataset = new DefaultCategoryDataset();
        for (java.util.Map.Entry<String, Double> e : ingresosPorCategoria.entrySet()) {
            ingresosDataset.addValue(e.getValue(), "Ingresos", e.getKey());
        }

        DefaultCategoryDataset gastosDataset = new DefaultCategoryDataset();
        for (java.util.Map.Entry<String, Double> e : gastosPorCategoria.entrySet()) {
            gastosDataset.addValue(e.getValue(), "Gastos", e.getKey());
        }

        JFreeChart ingresosChart = ChartFactory.createBarChart("Ingresos por Categoría", "Categoría", "Monto", ingresosDataset);
        JFreeChart gastosChart = ChartFactory.createBarChart("Gastos por Categoría", "Categoría", "Monto", gastosDataset);

        // Aplicar colores consistentes: ingresos = verde, gastos = rojo
        try {
            CategoryPlot p1 = ingresosChart.getCategoryPlot();
            BarRenderer r1 = (BarRenderer) p1.getRenderer();
            r1.setSeriesPaint(0, new Color(0, 128, 0));
        } catch (Exception ignore) {}
        try {
            CategoryPlot p2 = gastosChart.getCategoryPlot();
            BarRenderer r2 = (BarRenderer) p2.getRenderer();
            r2.setSeriesPaint(0, Color.RED);
        } catch (Exception ignore) {}

        ChartPanel cp1 = new ChartPanel(ingresosChart);
        ChartPanel cp2 = new ChartPanel(gastosChart);

        JPanel content = new JPanel(new GridLayout(1, 2));
        content.add(cp1);
        content.add(cp2);

        JDialog dlg = new JDialog(this, "Gráficas: Ingresos y Gastos", true);
        dlg.setSize(1100, 600);
        dlg.setLocationRelativeTo(this);
        dlg.setContentPane(content);
        dlg.setVisible(true);
    }

    private void actualizarCategoriasSegunTipo() {
        categoriaCombo.removeAllItems();
        Object tipoObj = tipoCombo.getSelectedItem();
        if (tipoObj instanceof Tipo) {
            int tipoId = ((Tipo) tipoObj).getId();
            java.util.List<Categoria> cats = manager.obtenerCategoriasPorTipoId(tipoId);
            if (cats == null || cats.isEmpty()) {
                // fallback defaults
                if ("ingreso".equalsIgnoreCase(tipoObj.toString())) {
                    String[] ingresos = {"Salario", "Inversión", "Regalo", "Otros"};
                    for (String c : ingresos) categoriaCombo.addItem(c);
                } else {
                    String[] gastos = {"Comida", "Transporte", "Ocio", "Salud", "Vivienda", "Otros"};
                    for (String c : gastos) categoriaCombo.addItem(c);
                }
            } else {
                for (Categoria c : cats) categoriaCombo.addItem(c);
            }
        }
    }

    private void mostrarDialogoNuevaCategoria() {
        JDialog dlg = new JDialog(this, "Nueva Categoría", true);
        dlg.setSize(400, 160);
        dlg.setLocationRelativeTo(this);
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        p.add(new JLabel("Tipo:"), gbc);
        gbc.gridx = 1;
        JComboBox<Tipo> tiposBox = new JComboBox<>();
        java.util.List<Tipo> tipos = manager.obtenerTipos();
        if (tipos != null) for (Tipo t: tipos) tiposBox.addItem(t);
        p.add(tiposBox, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        p.add(new JLabel("Categoria:"), gbc);
        gbc.gridx = 1;
        JTextField nombreField = new JTextField();
        p.add(nombreField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton save = new JButton("Guardar");
        JButton cancel = new JButton("Cancelar");
        btns.add(save); btns.add(cancel);
        p.add(btns, gbc);

        save.addActionListener(e -> {
            Tipo seleccionado = (Tipo) tiposBox.getSelectedItem();
            String nombre = nombreField.getText().trim();
            if (seleccionado == null) {
                JOptionPane.showMessageDialog(dlg, "Seleccione un tipo", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (nombre.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Ingrese un nombre para la categoría", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            boolean ok = manager.agregarCategoria(seleccionado.getId(), nombre);
            if (!ok) {
                JOptionPane.showMessageDialog(dlg, "No se pudo crear la categoría (quizá ya existe)", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            dlg.dispose();
            // refrescar categorías y seleccionar la recién creada
            actualizarCategoriasSegunTipo();
            // seleccionar por nombre si está presente
            for (int i=0;i<categoriaCombo.getItemCount();i++) {
                Object it = categoriaCombo.getItemAt(i);
                if (it instanceof Categoria && ((Categoria) it).getNombre().equalsIgnoreCase(nombre)) {
                    categoriaCombo.setSelectedIndex(i);
                    break;
                } else if (it instanceof String && ((String) it).equalsIgnoreCase(nombre)) {
                    categoriaCombo.setSelectedIndex(i);
                    break;
                }
            }
        });

        cancel.addActionListener(e -> dlg.dispose());

        dlg.setContentPane(p);
        dlg.setVisible(true);
    }

    private void eliminarSeleccionado() {
        int viewRow = movimientosTable.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un movimiento", "Atención", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = movimientosTable.convertRowIndexToModel(viewRow);
        int id = (int) tableModel.getValueAt(modelRow, 0);
        int resp = JOptionPane.showConfirmDialog(this, "¿Eliminar movimiento seleccionado?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (resp == JOptionPane.YES_OPTION) {
            if (manager.eliminarMovimiento(id)) {
                cargarDatos();
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo eliminar el movimiento", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void cerrarSesion() {
        manager.logout();
        new LoginFrame();
        dispose();
    }
}
