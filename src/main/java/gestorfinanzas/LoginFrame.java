package gestorfinanzas;

import javax.swing.*;
import java.awt.*;
import javax.swing.text.*;
import java.util.regex.Pattern;

public class LoginFrame extends JFrame {
    private FinanzasManager manager;
    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginFrame() {
        manager = new FinanzasManager();
        initComponents();
        setVisible(true);
    }

    private void initComponents() {
        setTitle("Inicio de Sesión - Gestor de Finanzas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 350);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Título
        JLabel titleLabel = new JLabel("GESTOR DE FINANZAS PERSONALES", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(new Color(0, 102, 204));

        // Formulario
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Iniciar Sesión"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Usuario
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Usuario:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0; // permitir que el campo ocupe más espacio horizontal
        usernameField = new JTextField(20);
        usernameField.setPreferredSize(new Dimension(260, 28));
        usernameField.setMaximumSize(new Dimension(Short.MAX_VALUE, 28));
        formPanel.add(usernameField, gbc);
        gbc.weightx = 0.0;

        // Contraseña
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Contraseña:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        passwordField = new JPasswordField(20);
        passwordField.setPreferredSize(new Dimension(260, 28));
        passwordField.setMaximumSize(new Dimension(Short.MAX_VALUE, 28));
        formPanel.add(passwordField, gbc);
        gbc.weightx = 0.0;

        // Botones (Iniciar, Registrarse)
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        
        JButton loginBtn = new JButton("Iniciar Sesión");
        loginBtn.setBackground(new Color(76, 175, 80));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFont(new Font("Arial", Font.BOLD, 14));
        loginBtn.setPreferredSize(new Dimension(140, 35));
        loginBtn.addActionListener(e -> iniciarSesion());
        
        JButton registerBtn = new JButton("Registrarse");
        registerBtn.setBackground(new Color(33, 150, 243));
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setFont(new Font("Arial", Font.BOLD, 14));
        registerBtn.setPreferredSize(new Dimension(140, 35));
        registerBtn.addActionListener(e -> mostrarRegistro());

        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonsPanel.add(loginBtn);
        buttonsPanel.add(Box.createVerticalStrut(10));
        buttonsPanel.add(registerBtn);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        formPanel.add(buttonsPanel, gbc);


        // Agregar componentes
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);

        // eventos
        usernameField.addActionListener(e -> iniciarSesion());
        passwordField.addActionListener(e -> iniciarSesion());

        add(mainPanel);
    }

    private void iniciarSesion() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Complete todos los campos", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (manager.login(username, password)) {
            JOptionPane.showMessageDialog(this, 
                "¡Bienvenido, " + manager.getUsuarioActual().getNombre() + "!",
                "Inicio exitoso", 
                JOptionPane.INFORMATION_MESSAGE);
            
            new MainFrame(manager);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, 
                "Usuario o contraseña incorrectos", 
                "Error de autenticación", 
                JOptionPane.ERROR_MESSAGE);
            passwordField.setText("");
        }
    }

    // Diagolo de registro de usuario
    private void mostrarRegistro() {
        JDialog dialog = new JDialog(this, "Registro de Usuario", true);
        dialog.setSize(420, 480);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Campos
        JTextField userField = new JTextField(15);
        JPasswordField passField = new JPasswordField(15);
        JPasswordField confirmField = new JPasswordField(15);
        JTextField nameField = new JTextField(15);
        JTextField apellidoField = new JTextField(15);

        // Restringir nombre y apellido a letras y espacios
        DocumentFilter nameFilter = new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (string == null) return;
                String filtered = string.replaceAll("[^\\p{L} ]", "");
                if (!filtered.isEmpty()) super.insertString(fb, offset, filtered, attr);
            }
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text == null) return;
                String filtered = text.replaceAll("[^\\p{L} ]", "");
                if (!filtered.isEmpty()) super.replace(fb, offset, length, filtered, attrs);
                else if (length > 0) super.remove(fb, offset, length);
            }
            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                super.remove(fb, offset, length);
            }
        };
        ((AbstractDocument) nameField.getDocument()).setDocumentFilter(nameFilter);
        ((AbstractDocument) apellidoField.getDocument()).setDocumentFilter(nameFilter);
        JTextField cedulaField = new JTextField(15);
        JTextField emailField = new JTextField(15);
        JComboBox<String> tipoCedulaCombo = new JComboBox<>(new String[]{"V", "E"});

        // Placeholder example (eight 1s) for cedula
        final String CEDULA_PLACEHOLDER = "11111111";
        cedulaField.setText(CEDULA_PLACEHOLDER);
        cedulaField.setForeground(Color.GRAY);
        cedulaField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (cedulaField.getText().equals(CEDULA_PLACEHOLDER)) {
                    cedulaField.setText("");
                    cedulaField.setForeground(Color.BLACK);
                }
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if (cedulaField.getText().trim().isEmpty()) {
                    cedulaField.setText(CEDULA_PLACEHOLDER);
                    cedulaField.setForeground(Color.GRAY);
                }
            }
        });

        // Restringir el campo cedula a solo dígitos y max 8 caracteres
        ((AbstractDocument) cedulaField.getDocument()).setDocumentFilter(new DocumentFilter() {
            private int MAX = 8;

            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (string == null) return;
                String filtered = string.replaceAll("\\D", "");
                int allowed = MAX - fb.getDocument().getLength();
                if (filtered.length() > allowed) filtered = filtered.substring(0, Math.max(0, allowed));
                if (!filtered.isEmpty()) super.insertString(fb, offset, filtered, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text == null) return;
                String filtered = text.replaceAll("\\D", "");
                int currentLen = fb.getDocument().getLength();
                int allowed = MAX - (currentLen - length);
                if (filtered.length() > allowed) filtered = filtered.substring(0, Math.max(0, allowed));
                if (!filtered.isEmpty()) super.replace(fb, offset, length, filtered, attrs);
                else if (length > 0) super.remove(fb, offset, length);
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                super.remove(fb, offset, length);
            }
        });

        int row = 0;
        
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Usuario:*"), gbc);
        gbc.gridx = 1;
        panel.add(userField, gbc);
        
        gbc.gridx = 0; gbc.gridy = ++row;
        panel.add(new JLabel("Contraseña:*"), gbc);
        gbc.gridx = 1;
        panel.add(passField, gbc);
        
        gbc.gridx = 0; gbc.gridy = ++row;
        panel.add(new JLabel("Confirmar:*"), gbc);
        gbc.gridx = 1;
        panel.add(confirmField, gbc);
        
        gbc.gridx = 0; gbc.gridy = ++row;
        panel.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1;
        panel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = ++row;
        panel.add(new JLabel("Apellido:"), gbc);
        gbc.gridx = 1;
        panel.add(apellidoField, gbc);

        gbc.gridx = 0; gbc.gridy = ++row;
        panel.add(new JLabel("Cédula:"), gbc);
        gbc.gridx = 1;
        // Panel para comboTipo + campo cedula
        JPanel cedulaPanel = new JPanel(new BorderLayout(6, 0));
        tipoCedulaCombo.setPreferredSize(new Dimension(60, cedulaField.getPreferredSize().height));
        cedulaPanel.add(tipoCedulaCombo, BorderLayout.WEST);
        cedulaPanel.add(cedulaField, BorderLayout.CENTER);
        panel.add(cedulaPanel, gbc);

        gbc.gridx = 0; gbc.gridy = ++row;
        panel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        panel.add(emailField, gbc);

        // Botones
        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton registrarBtn = new JButton("Registrar");
        JButton cancelarBtn = new JButton("Cancelar");
        
        registrarBtn.addActionListener(e -> {
            if (registrarUsuario(userField, passField, confirmField, nameField, apellidoField, tipoCedulaCombo, cedulaField, emailField)) {
                dialog.dispose();
            }
        });
        
        cancelarBtn.addActionListener(e -> dialog.dispose());
        
        btnPanel.add(registrarBtn);
        btnPanel.add(cancelarBtn);
        
        gbc.gridx = 0; gbc.gridy = ++row; gbc.gridwidth = 2;
        panel.add(btnPanel, gbc);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private boolean registrarUsuario(JTextField userField, JPasswordField passField,
                                    JPasswordField confirmField, JTextField nameField,
                                    JTextField apellidoField, JComboBox<String> tipoCedulaCombo, JTextField cedulaField, JTextField emailField) {
        
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());
        String confirm = new String(confirmField.getPassword());
        String nombre = nameField.getText().trim();
        String apellido = apellidoField.getText().trim();
        String cedula = cedulaField.getText().trim();
        String tipoCedula = tipoCedulaCombo.getSelectedItem() != null ? tipoCedulaCombo.getSelectedItem().toString() : "V";
        String email = emailField.getText().trim();

        // Validaciones
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Usuario y contraseña son obligatorios", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (apellido.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Apellido es obligatorio", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (cedula.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cédula es obligatoria", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // If placeholder is still present, treat as empty
        if (cedula.equals("11111111")) {
            JOptionPane.showMessageDialog(this, "Ingrese una cédula válida (ej: 11111111)", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Validate numeric and length 8
        String cedulaDigits = cedula.replaceAll("\\D", "");
        if (cedulaDigits.length() != 8) {
            JOptionPane.showMessageDialog(this, "La cédula debe tener exactamente 8 dígitos", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        String formattedCedula = tipoCedula + "-" + cedulaDigits;

        // Validar email si fue ingresado
        if (!email.isEmpty()) {
            Pattern emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
            if (!emailPattern.matcher(email).matches()) {
                JOptionPane.showMessageDialog(this, "Ingrese un correo válido (ej: usuario@ejemplo.com)", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        if (!password.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (password.length() < 4) {
            JOptionPane.showMessageDialog(this, "Contraseña mínima: 4 caracteres", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (manager.existeUsuario(username)) {
            JOptionPane.showMessageDialog(this, "El usuario ya existe", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (manager.registrarUsuario(username, password, nombre, apellido, formattedCedula, email)) {
            JOptionPane.showMessageDialog(this, 
                "¡Registro exitoso!\nAhora puede iniciar sesión.",
                "Éxito", 
                JOptionPane.INFORMATION_MESSAGE);
            return true;
        } else {
            JOptionPane.showMessageDialog(this, "Error en el registro", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public static void main(String[] args) {
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
                // ignore
            }
        }
        SwingUtilities.invokeLater(LoginFrame::new);
    }
}
