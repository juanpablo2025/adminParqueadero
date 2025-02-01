package org.example;


import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;



public class Main {
    private static final int PRECIO_POR_HORA = 1200;
    private static final int INCREMENTO_1H15M = 600;
    private static final int PRECIO_2HORAS = 2400;
    private static final int PRECIO_MAXIMO_5HORAS = 5000;

    private static final String USER_HOME = System.getProperty("user.home");
    private static final String EXCEL_FILE_PATH = USER_HOME + "\\Downloads\\Parqueadero\\parqueadero.xlsx";
    private static final String MENSUALIDADES_FILE_PATH = USER_HOME + "\\Downloads\\Parqueadero\\mensualidades.xlsx";
    private static JFrame frame;
    private static JTable table;
    private static DefaultTableModel tableModel;

    public static void main(String[] args) {

            // Configuración inicial de la ventana principal
            frame = new JFrame("Sistema de Parqueadero");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLayout(new BorderLayout());

            // Crear los botones para el menú principal
            JPanel buttonPanel = new JPanel();
            JButton btnRegistrarEntrada = new JButton("Registrar Entrada");
            JButton btnRegistrarSalida = new JButton("Registrar Salida");
            JButton btnPagarMensualidad = new JButton("Pagar Mensualidad");
            JButton btnSalir = new JButton("Salir");

            buttonPanel.add(btnRegistrarEntrada);
            buttonPanel.add(btnRegistrarSalida);
            buttonPanel.add(btnPagarMensualidad);
            buttonPanel.add(btnSalir);

            frame.add(buttonPanel, BorderLayout.NORTH);

            // Crear la tabla para mostrar la información
            tableModel = new DefaultTableModel(new String[]{"Placa", "Fecha Entrada", "Fecha Salida", "Posición Cascos", "Valor", "Realizó"}, 0);
            table = new JTable(tableModel);
            JScrollPane scrollPane = new JScrollPane(table);
            frame.add(scrollPane, BorderLayout.CENTER);

            // Agregar acciones a los botones
            btnRegistrarEntrada.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    registrarEntrada();
                }
            });

            btnRegistrarSalida.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    registrarSalida();
                }
            });

            btnPagarMensualidad.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    pagarMensualidad();
                }
            });

            btnSalir.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            });

            // Mostrar la ventana principal
            frame.setVisible(true);
        }

        private static void registrarEntrada() {
            // Crear un nuevo diálogo para el registro de entrada
            JDialog dialog = new JDialog(frame, "Registrar Entrada", true);
            dialog.setSize(400, 300);
            dialog.setLayout(new GridLayout(4, 2));

            JLabel lblPlaca = new JLabel("Placa:");
            JTextField txtPlaca = new JTextField();

            JLabel lblPosicionCascos = new JLabel("Posición Cascos:");
            JTextField txtPosicionCascos = new JTextField();

            JButton btnGuardar = new JButton("Guardar");
            JButton btnCancelar = new JButton("Cancelar");

            dialog.add(lblPlaca);
            dialog.add(txtPlaca);
            dialog.add(lblPosicionCascos);
            dialog.add(txtPosicionCascos);
            dialog.add(btnGuardar);
            dialog.add(btnCancelar);

            btnGuardar.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String placa = txtPlaca.getText();
                    String posicionCascos = txtPosicionCascos.getText();
                    String fechaEntrada = obtenerFechaHoraActual();

                    // Agregar a la tabla
                    tableModel.addRow(new Object[]{placa, fechaEntrada, "", posicionCascos, "", ""});
                    dialog.dispose();
                }
            });

            btnCancelar.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                }
            });

            dialog.setVisible(true);
        }

        private static void registrarSalida() {
            // Similar al registro de entrada pero calculando el costo
            JDialog dialog = new JDialog(frame, "Registrar Salida", true);
            dialog.setSize(400, 300);
            dialog.setLayout(new GridLayout(3, 2));

            JLabel lblPlaca = new JLabel("Placa:");
            JTextField txtPlaca = new JTextField();

            JButton btnGuardar = new JButton("Guardar");
            JButton btnCancelar = new JButton("Cancelar");

            dialog.add(lblPlaca);
            dialog.add(txtPlaca);
            dialog.add(btnGuardar);
            dialog.add(btnCancelar);

            btnGuardar.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String placa = txtPlaca.getText();
                    String fechaSalida = obtenerFechaHoraActual();

                    // Actualizar tabla con fecha de salida
                    for (int i = 0; i < tableModel.getRowCount(); i++) {
                        if (tableModel.getValueAt(i, 0).equals(placa)) {
                            tableModel.setValueAt(fechaSalida, i, 2);
                            tableModel.setValueAt("Calculado", i, 4); // Ejemplo de valor
                            break;
                        }
                    }
                    dialog.dispose();
                }
            });

            btnCancelar.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                }
            });

            dialog.setVisible(true);
        }

        private static void pagarMensualidad() {
            // Implementación similar para mensualidades
            JDialog dialog = new JDialog(frame, "Pagar Mensualidad", true);
            dialog.setSize(400, 200);
            dialog.setLayout(new GridLayout(2, 2));

            JLabel lblPlaca = new JLabel("Placa:");
            JTextField txtPlaca = new JTextField();

            JButton btnPagar = new JButton("Pagar");
            JButton btnCancelar = new JButton("Cancelar");

            dialog.add(lblPlaca);
            dialog.add(txtPlaca);
            dialog.add(btnPagar);
            dialog.add(btnCancelar);

            btnPagar.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String placa = txtPlaca.getText();
                    // Registrar el pago en la tabla
                    tableModel.addRow(new Object[]{placa, obtenerFechaHoraActual(), "", "", "", "Mensualidad"});
                    dialog.dispose();
                }
            });

            btnCancelar.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                }
            });

            dialog.setVisible(true);
        }

        private static String obtenerFechaHoraActual() {
            // Método para obtener la fecha y hora actual en formato String
            return java.time.LocalDateTime.now().toString();
        }
    }
















