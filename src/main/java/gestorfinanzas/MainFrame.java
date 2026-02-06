package gestorfinanzas;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class MainFrame extends JFrame {
    private FinanzasManager manager;
    private JLabel saldoLabel, usuarioLabel;
    private JPanel mainPanel;
    private DefaultTableModel tableModel;
    private JTable movimientosTable;
    private JTextField montoField;
    private JComboBox<Object> categoriaCombo;
    private JComboBox<Object> tipoCombo;
    private JTextArea descripcionArea;
    private JLabel totalIngresosLabel, totalGastosLabel;

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
        form.add(new JLabel("Tipo:"), gbc);
        gbc.gridx = 1;
        tipoCombo = new JComboBox<>();
        form.add(tipoCombo, gbc);
        tipoCombo.addActionListener(e -> actualizarCategoriasSegunTipo());

        gbc.gridx = 0; gbc.gridy = 2;
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

        gbc.gridx = 0; gbc.gridy = 3;
        form.add(new JLabel("Descripción:"), gbc);
        gbc.gridx = 1;
        descripcionArea = new JTextArea(4, 15);
        form.add(new JScrollPane(descripcionArea), gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
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

        mainPanel.add(topPanel, BorderLayout.NORTH);
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
        }
    }

    private void agregarMovimiento() {
        try {
            double monto = Double.parseDouble(montoField.getText().trim());
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

            boolean ok = manager.agregarMovimiento(monto, tipoId, categoriaId, descripcion);

            if (!ok) {
                JOptionPane.showMessageDialog(this, "No se pudo guardar el movimiento en la base de datos. Revisa la consola para detalles.", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                montoField.setText("");
                descripcionArea.setText("");
                cargarDatos();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Monto inválido", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
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
