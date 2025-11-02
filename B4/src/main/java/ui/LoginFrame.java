package ui;

import javax.swing.*;
import services.AuthService;
import exceptions.ErrorConexionRedisException;
import exceptions.ErrorConexionMongoException;
import exceptions.ErrorConexionMySQLException;
import java.awt.*;
import java.awt.event.ActionEvent;

public class LoginFrame extends JFrame {
    private JTextField emailField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private String currentToken;

    public LoginFrame() {
        setTitle("Polyglot Persistence - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBackground(new Color(240, 240, 240));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Título
        JLabel titleLabel = new JLabel("Sensor Management System");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        // Email
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panel.add(new JLabel("Email:"), gbc);
        emailField = new JTextField(20);
        gbc.gridx = 1;
        panel.add(emailField, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Password:"), gbc);
        passwordField = new JPasswordField(20);
        gbc.gridx = 1;
        panel.add(passwordField, gbc);

        // Botones
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(240, 240, 240));

        loginButton = new JButton("Login");
        loginButton.addActionListener(this::handleLogin);
        buttonPanel.add(loginButton);

        registerButton = new JButton("Register");
        registerButton.addActionListener(this::handleRegister);
        buttonPanel.add(registerButton);

        panel.add(buttonPanel, gbc);

        add(panel);
    }

    private void handleLogin(ActionEvent e) {
        String email = emailField.getText();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String token = AuthService.getInstance().login(email, password);
            if (token != null) {
                currentToken = token;
                try {
                    // Iniciar simulación continua de mediciones (global)
                    services.AutoMedicionService.getInstance().start();
                    var usuario = AuthService.getInstance().validarToken(token);
                    if (usuario != null && usuario.getRol() != null) {
                        String rol = usuario.getRol().toUpperCase();
                        if ("ADMINISTRADOR".equals(rol)) {
                            AdminFrame admin = new AdminFrame(currentToken);
                            admin.setVisible(true);
                            this.dispose();
                            return;
                        } else if ("TECNICO".equals(rol)) {
                            TecnicoFrame tecnico = new TecnicoFrame(currentToken);
                            tecnico.setVisible(true);
                            this.dispose();
                            return;
                        }
                    }
                    openDashboard();
                } catch (ErrorConexionRedisException | ErrorConexionMySQLException ex) {
                    // Si falla la validación del token, procedemos al dashboard genérico
                    openDashboard();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (ErrorConexionMongoException | ErrorConexionRedisException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRegister(ActionEvent e) {
        RegisterDialog dialog = new RegisterDialog(this);
        dialog.setVisible(true);
    }

    private void openDashboard() {
        DashboardFrame dashboard = new DashboardFrame(currentToken);
        dashboard.setVisible(true);
        this.dispose();
    }
}
