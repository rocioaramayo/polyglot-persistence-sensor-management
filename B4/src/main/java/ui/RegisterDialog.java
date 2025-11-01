package ui;

import services.AuthService;
import exceptions.ErrorConexionMongoException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class RegisterDialog extends JDialog {
    private JTextField nombreField;
    private JTextField apellidoField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JTextField telefonoField;
    private JTextField direccionField;

    public RegisterDialog(Frame owner) {
        super(owner, "Registrar Usuario", true);
        setSize(400, 360);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Nombre
        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Nombre:"), gbc);
        nombreField = new JTextField(20);
        gbc.gridx = 1; panel.add(nombreField, gbc);

        // Apellido
        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Apellido:"), gbc);
        apellidoField = new JTextField(20);
        gbc.gridx = 1; panel.add(apellidoField, gbc);

        // Email
        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("Email:"), gbc);
        emailField = new JTextField(20);
        gbc.gridx = 1; panel.add(emailField, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 3; panel.add(new JLabel("Contraseña:"), gbc);
        passwordField = new JPasswordField(20);
        gbc.gridx = 1; panel.add(passwordField, gbc);

        // Telefono
        gbc.gridx = 0; gbc.gridy = 4; panel.add(new JLabel("Telefono:"), gbc);
        telefonoField = new JTextField(20);
        gbc.gridx = 1; panel.add(telefonoField, gbc);

        // Direccion
        gbc.gridx = 0; gbc.gridy = 5; panel.add(new JLabel("Direccion:"), gbc);
        direccionField = new JTextField(20);
        gbc.gridx = 1; panel.add(direccionField, gbc);

        // Botones
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel();
        JButton cancelarBtn = new JButton("Cancelar");
        cancelarBtn.addActionListener(e -> dispose());
        JButton registrarBtn = new JButton("Registrar");
        registrarBtn.addActionListener(this::handleRegistrar);
        btnPanel.add(cancelarBtn);
        btnPanel.add(registrarBtn);
        panel.add(btnPanel, gbc);

        add(panel);
    }

    private void handleRegistrar(ActionEvent e) {
        String nombre = nombreField.getText().trim();
        String apellido = apellidoField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String rol = "USUARIO"; // Rol fijo para registro desde UI
        String telefono = telefonoField.getText();
        String direccion = direccionField.getText();

        if (nombre.isEmpty() || apellido.isEmpty() || email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor completa todos los campos.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            AuthService.getInstance().registrar(nombre, apellido, email, password, rol, telefono.trim(), direccion.trim());
            JOptionPane.showMessageDialog(this, "¡Registro exitoso! Ahora puedes iniciar sesión.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (ErrorConexionMongoException ex) {
            JOptionPane.showMessageDialog(this, "Error al registrar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
